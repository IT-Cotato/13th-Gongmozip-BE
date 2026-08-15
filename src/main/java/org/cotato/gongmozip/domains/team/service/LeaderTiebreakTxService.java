package org.cotato.gongmozip.domains.team.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link LeaderTiebreakAsyncService}가 AI 응답을 받아온 뒤의 DB 반영/카드 발행을 전담한다.
 * 비동기 스레드에서 새로 호출되므로(원래 투표 트랜잭션과 무관) 새 트랜잭션에서 시작한다.
 */
@Service
@RequiredArgsConstructor
public class LeaderTiebreakTxService {

    // LeaderElectionService.LEADER_VOTE_TIMEOUT_HOURS와 동일한 값 — 동률 재투표 카드를 발행할 때
    // 그 시점부터 8시간을 새로 잡는다(2026-08-15, 후보 등록 마감과 분리하면서 결정).
    private static final int LEADER_VOTE_TIMEOUT_HOURS = 8;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ChatService chatService;
    private final ChatbotOrchestrationService chatbotOrchestrationService;
    // 이 프로젝트에는 Spring이 자동 구성한 ObjectMapper 빈이 없어 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void applyTiebreakResult(Long teamId, int round, List<Long> topCandidateIds, Long aiRecommendedId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        // 비동기로 넘어오는 사이 팀이 이미 다른 경로로 확정됐을 수 있다(예: 동시에 나간 팀원
        // 재확인 등) — 그 경우 여기서 더 손대지 않는다.
        if (team.getStatus() != TeamStatus.LEADER_SELECTING) {
            return;
        }

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        TeamMember recommended = activeMembers.stream()
                .filter(tm -> tm.getTeamMemberId().equals(aiRecommendedId))
                .findFirst()
                .orElse(null);

        // 재투표(2라운드 이상)도 또 동률이면 더 이상 재투표를 반복하지 않고 AI 추천 후보로
        // 바로 확정한다 (카톡 스펙: "둘이 또 동률일 경우에는 '추천 수락하기'로 진행한다").
        if (round >= 2 && recommended != null) {
            assignLeader(
                    team,
                    recommended,
                    "재투표도 동률이 발생해 AI 추천에 따라 " + recommended.getProfile().getNickname() + "님이 팀장으로 확정되었습니다.");
            return;
        }

        String recommendedName =
                recommended == null ? null : recommended.getProfile().getNickname();
        String content = recommendedName == null
                ? "동률이 발생했어요. 동률이었던 팀원들끼리 재투표를 진행할게요."
                : "동률이 발생했어요. AI가 보기엔 " + recommendedName + "님이 팀장으로 잘 어울릴 것 같아요. 추천을 수락하거나, 동률이었던 팀원들끼리 재투표를 진행해주세요.";

        // 재투표 라운드도 독립된 8시간 마감을 새로 받는다(직전 라운드에서 남은 시간을 그대로
        // 물려받지 않음, 2026-08-15 결정).
        team.scheduleLeaderVoteDeadline(LocalDateTime.now().plusHours(LEADER_VOTE_TIMEOUT_HOURS));
        chatService.postChatbotCardMessage(
                team, MessageType.LEADER_VOTE_CARD, content, toTiebreakMetadata(topCandidateIds, aiRecommendedId));
    }

    private void assignLeader(Team team, TeamMember leader, String announcement) {
        leader.assignAsLeader();
        team.advanceStatus(TeamStatus.LEADER_DECIDED);
        chatService.postChatbotCardMessage(
                team, MessageType.LEADER_RESULT_CARD, announcement, toLeaderResultMetadata(leader.getTeamMemberId()));
        chatbotOrchestrationService.advanceToContestSelecting(team);
    }

    private String toLeaderResultMetadata(Long leaderTeamMemberId) {
        try {
            return objectMapper.writeValueAsString(Map.of("leaderTeamMemberId", leaderTeamMemberId));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("팀장 결과 메타데이터 직렬화에 실패했습니다.", e);
        }
    }

    private String toTiebreakMetadata(List<Long> candidateTeamMemberIds, Long aiRecommendedTeamMemberId) {
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("candidateTeamMemberIds", candidateTeamMemberIds);
            metadata.put("aiRecommendedTeamMemberId", aiRecommendedTeamMemberId);
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("팀장 후보 메타데이터 직렬화에 실패했습니다.", e);
        }
    }
}
