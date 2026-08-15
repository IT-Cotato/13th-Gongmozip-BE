package org.cotato.gongmozip.domains.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestCandidate;
import org.cotato.gongmozip.domains.contest.repository.ContestCandidateRepository;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamRole;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ChatbotContestRecommendationAsyncService}가 AI 응답을 받아온 뒤의 DB 반영/카드 발행을
 * 전담한다. 비동기 스레드에서 새로 호출되므로(원래 상태 전이 트랜잭션과 무관) 메서드마다 독립된
 * 트랜잭션에서 시작한다.
 */
@Service
@RequiredArgsConstructor
public class ChatbotContestRecommendationTxService {

    private static final String CONTEST_SELECTION_PROMPT = "팀장 선출까지 마쳤으면, 팀원들과 함께 나갈 공모전을 후보로 추가하고 투표해보세요!";

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ContestCandidateRepository contestCandidateRepository;
    private final ChatService chatService;
    // 이 프로젝트에는 Spring이 자동 구성한 ObjectMapper 빈이 없어 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void announcePlainPrompt(Long teamId) {
        Team team = findTeam(teamId);
        chatService.postChatbotMessage(team, CONTEST_SELECTION_PROMPT);
    }

    @Transactional
    public void registerCandidatesAndAnnounce(
            Long teamId, List<Contest> openContests, List<Long> recommendedContestIds) {
        Team team = findTeam(teamId);
        registerRecommendedCandidates(team, openContests, recommendedContestIds);
        chatService.postChatbotCardMessage(
                team,
                MessageType.CONTEST_RECOMMEND_CARD,
                CONTEST_SELECTION_PROMPT,
                toIdsMetadata(recommendedContestIds));
    }

    // AI가 추천한 공모전은 정보성 표시로 끝나지 않고 바로 투표 가능한 후보로 등록돼야 한다
    // (Figma "공모전 후보 리스트" 화면이 추천 목록을 이미 후보로 전제하고 있음, 2026-08-05
    // 커버리지 점검 중 발견). 후보를 등록한 사람(addedByTeamMember)은 nullable=false라
    // 이 시점에 이미 확정된 팀장으로 채운다 — advanceToContestSelecting은 항상 팀장 확정
    // 직후에만 호출되므로 팀장이 없는 경우는 이론상 없다.
    private void registerRecommendedCandidates(
            Team team, List<Contest> openContests, List<Long> recommendedContestIds) {
        TeamMember leader =
                teamMemberRepository.findByTeamIdAndStatus(team.getTeamId(), TeamMemberStatus.ACTIVE).stream()
                        .filter(teamMember -> teamMember.getRole() == TeamRole.LEADER)
                        .findFirst()
                        .orElse(null);
        if (leader == null) {
            return;
        }

        Map<Long, Contest> contestById =
                openContests.stream().collect(Collectors.toMap(Contest::getContestId, contest -> contest));
        for (Long contestId : recommendedContestIds) {
            Contest contest = contestById.get(contestId);
            if (contest == null
                    || contestCandidateRepository.existsByTeam_TeamIdAndContest_ContestId(
                            team.getTeamId(), contestId)) {
                continue;
            }
            contestCandidateRepository.save(ContestCandidate.builder()
                    .team(team)
                    .contest(contest)
                    .addedByTeamMember(leader)
                    .build());
        }
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
    }

    private String toIdsMetadata(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(Map.of("contestIds", ids));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 추천 메타데이터 직렬화에 실패했습니다.", e);
        }
    }
}
