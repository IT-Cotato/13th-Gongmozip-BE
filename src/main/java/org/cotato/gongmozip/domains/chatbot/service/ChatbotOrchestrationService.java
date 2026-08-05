package org.cotato.gongmozip.domains.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.repository.ContestRepository;
import org.cotato.gongmozip.domains.team.converter.TeamConverter;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.LeaderCandidacyStatus;
import org.cotato.gongmozip.domains.team.enums.LeaderSelectionMode;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamRole;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.cotato.gongmozip.global.ai.dto.LeaderCandidateSnapshot;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Team.status 상태머신을 전이시키는 챗봇 오케스트레이션 엔진 (docs/decisions/01-team.md).
 * Phase 4에서 MATCHED -> GREETING -> LEADER_SELECTING을, Phase 6에서
 * LEADER_DECIDED -> CONTEST_SELECTING, CONTEST_DECIDED -> IN_PROGRESS 전이를 추가했다.
 * Phase 8에서 팀장 후보/공모전 AI 추천을 카드 메시지에 연결했다 (docs/decisions/08-ai.md).
 */
@Service
@RequiredArgsConstructor
public class ChatbotOrchestrationService {

    private static final String GREETING_PROMPT =
            "안녕하세요. 저는 팀 운영을 도와주는 AI 챗봇이에요. 팀 매칭이 완료되었어요. 각자 간단한 자기소개와 인사를 나눠볼까요?";
    private static final String LEADER_SELECTION_PROMPT = "모두 인사를 마쳤네요! 이제 팀장을 선출해볼게요. 팀장이 되고 싶은 분은 투표해주세요.";
    private static final String CONTEST_SELECTION_PROMPT = "팀장 선출까지 마쳤으면, 팀원들과 함께 나갈 공모전을 후보로 추가하고 투표해보세요!";
    private static final String IN_PROGRESS_PROMPT = "공모전이 정해졌어요! 이제 팀원들과 함께 준비를 시작해보세요. 언제든 저의 도움이 필요하면 태그해주세요.\n\n"
            + "활용 예시\n@챗봇 우리 역할 분담 추천해줘\n@챗봇 우리 타임라인 추천해줘";
    private static final String REVIEW_COMPLETE_PROMPT = "모든 팀원이 서로에게 리뷰를 남겼어요. 수고 많으셨어요! 팀 프로젝트가 여기서 마무리됩니다.";
    // 팀장 여부 투표/팀장 투표 시작(=LEADER_SELECTING 진입) 후 이 시간 안에 결과가 나지 않으면
    // 스케줄러가 강제로 확정한다(GREETING 타임아웃과 동일한 정책, docs/decisions/02-leader-election.md).
    private static final int LEADER_SELECTION_TIMEOUT_HOURS = 2;
    private static final int CONTEST_RECOMMENDATION_POOL_SIZE = 10;
    private static final String CHATBOT_MENTION_PREFIX = "@챗봇";

    private final ChatService chatService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ContestRepository contestRepository;
    private final AiClient aiClient;
    // 이 프로젝트에는 Spring이 자동 구성한 ObjectMapper 빈이 없어 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 팀 생성 직후 호출되어 인사 유도 단계를 시작한다. AUTO_ASSIGNED(매칭 신청 시점 팀장 희망
     * "네" 1명)면 인사 메시지 자체에 팀장 안내를 포함시킨다 (docs/decisions/01-team.md 참고).
     */
    @Transactional
    public void startGreeting(Team team) {
        team.advanceStatus(TeamStatus.GREETING);
        chatService.postChatbotMessage(team, greetingPromptFor(team));
    }

    private String greetingPromptFor(Team team) {
        if (team.getLeaderSelectionMode() != LeaderSelectionMode.AUTO_ASSIGNED) {
            return GREETING_PROMPT;
        }
        List<TeamMember> activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(team.getTeamId(), TeamMemberStatus.ACTIVE);
        return activeMembers.stream()
                .filter(teamMember -> teamMember.getRole() == TeamRole.LEADER)
                .findFirst()
                .map(leader -> GREETING_PROMPT + "\n\n" + leader.getProfile().getNickname()
                        + "님이 매칭 시점에 팀장 참여를 희망하셔서 팀장으로 확정되었어요!")
                .orElse(GREETING_PROMPT);
    }

    /**
     * 팀원이 메시지를 보낼 때마다 호출된다. GREETING 단계에서만 동작하며, 해당 팀원의 첫 메시지를
     * 인사로 간주해 기록하고, 활성 팀원 전원이 인사를 마치면 팀장 선출 단계로 전이시킨다.
     */
    @Transactional
    public void recordGreetingAndAdvance(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.GREETING) {
            return;
        }

        TeamMember sender = teamMemberRepository
                .findByTeam_TeamIdAndMember_MemberId(teamId, memberId)
                .orElse(null);
        if (sender == null || sender.getGreetedAt() != null) {
            return;
        }
        sender.markGreeted(LocalDateTime.now());

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        boolean allGreeted = activeMembers.stream().allMatch(teamMember -> teamMember.getGreetedAt() != null);
        if (allGreeted) {
            advanceAfterGreeting(team, activeMembers);
        }
    }

    /**
     * GREETING 시작(=팀 생성) 후 2시간이 지나도록 인사를 마치지 않은 팀원이 있으면 스케줄러가
     * 호출해 강제로 다음 단계로 넘긴다 (기능명세서 5.1.3.1 팀 인사 유도 E1). 정상적으로 전원이
     * 인사를 마쳐 이미 다음 단계로 넘어간 팀이면 아무 것도 하지 않는다.
     */
    @Transactional
    public void forceAdvanceGreetingIfDue(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.GREETING) {
            return;
        }

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        advanceAfterGreeting(team, activeMembers);
    }

    /**
     * 전원 인사가 끝난 뒤 leaderSelectionMode에 따라 분기한다
     * (docs/decisions/02-leader-election.md 케이스①②③).
     * - AUTO_ASSIGNED(①): LEADER_SELECTING을 건너뛰고 바로 팀장 확정 안내 후 공모전 단계로.
     * - CANDIDATE_VOTE(③): 팀장 여부 투표 없이 사전 후보 전원을 바로 후보 등록하고 투표 카드 발행.
     * - OPEN_NOMINATION(②): 기존과 동일하게 AI 추천 2명을 담은 팀장 여부 투표 카드 발행.
     */
    private void advanceAfterGreeting(Team team, List<TeamMember> activeMembers) {
        if (team.getLeaderSelectionMode() == LeaderSelectionMode.AUTO_ASSIGNED) {
            TeamMember leader = activeMembers.stream()
                    .filter(teamMember -> teamMember.getRole() == TeamRole.LEADER)
                    .findFirst()
                    .orElseThrow(() -> new TeamException(TeamErrorCode.INVALID_TEAM_STATUS));
            team.advanceStatus(TeamStatus.LEADER_DECIDED);
            chatService.postChatbotCardMessage(
                    team,
                    MessageType.LEADER_RESULT_CARD,
                    leader.getProfile().getNickname() + "님이 팀장으로 확정되었어요! 이제 공모전을 골라볼까요?",
                    toIdMetadata("leaderTeamMemberId", leader.getTeamMemberId()));
            advanceToContestSelecting(team);
            return;
        }

        team.advanceStatus(TeamStatus.LEADER_SELECTING);
        team.scheduleLeaderSelectionDeadline(LocalDateTime.now().plusHours(LEADER_SELECTION_TIMEOUT_HOURS));
        if (team.getLeaderSelectionMode() == LeaderSelectionMode.CANDIDATE_VOTE) {
            postCandidateVoteCard(activeMembers, team);
        } else {
            postLeaderNominationCard(team, activeMembers);
        }
    }

    // 사전 후보(매칭 신청 시점 팀장 희망 "네")가 2명 이상이면 "팀장 여부 투표" 단계 없이 바로
    // 후보 전원을 대상으로 투표 카드를 발행한다. 비후보 팀원의 leaderCandidacy도 함께 확정해야
    // LeaderElectionService.castVote()의 "전원 응답 완료" 선행조건을 만족한다.
    private void postCandidateVoteCard(List<TeamMember> activeMembers, Team team) {
        activeMembers.forEach(teamMember -> teamMember.updateLeaderCandidacy(
                teamMember.isPreLeaderCandidate() ? LeaderCandidacyStatus.WANTS : LeaderCandidacyStatus.DOES_NOT_WANT));
        List<Long> candidateIds = activeMembers.stream()
                .filter(TeamMember::isPreLeaderCandidate)
                .map(TeamMember::getTeamMemberId)
                .toList();
        chatService.postChatbotCardMessage(
                team,
                MessageType.LEADER_VOTE_CARD,
                "팀장 희망자가 여러 명이에요. 팀장이 되면 좋을 것 같은 팀원에게 투표해주세요!",
                toIdsMetadata("candidateTeamMemberIds", candidateIds));
    }

    private void postLeaderNominationCard(Team team, List<TeamMember> activeMembers) {
        List<LeaderCandidateSnapshot> snapshots = activeMembers.stream()
                .map(TeamConverter::toLeaderCandidateSnapshot)
                .toList();
        List<Long> recommendedIds = aiClient.recommendLeaderCandidates(team.getTeamId(), snapshots);
        String recommendedNames = activeMembers.stream()
                .filter(member -> recommendedIds.contains(member.getTeamMemberId()))
                .map(member -> member.getProfile().getNickname())
                .collect(Collectors.joining(", "));
        String content = recommendedNames.isBlank()
                ? LEADER_SELECTION_PROMPT
                : LEADER_SELECTION_PROMPT + "\n\nAI 추천: " + recommendedNames + "님이 팀장으로 잘 어울릴 것 같아요!";

        chatService.postChatbotCardMessage(
                team,
                MessageType.LEADER_NOMINATION_CARD,
                content,
                toIdsMetadata("aiRecommendedTeamMemberIds", recommendedIds));
    }

    /**
     * 팀장이 확정된 직후(LeaderElectionService) 호출되어 공모전 선정 단계를 시작한다.
     * 공모전 후보/투표 마감을 오늘 오후 11시로 세팅한다 (docs/decisions/04-contest-voting.md).
     */
    @Transactional
    public void advanceToContestSelecting(Team team) {
        team.advanceStatus(TeamStatus.CONTEST_SELECTING);
        team.scheduleContestCandidateDeadline(LocalDateTime.now().toLocalDate().atTime(23, 0));

        List<Contest> openContests = contestRepository
                .findAllWithFilterAndDeadlineAsc(
                        null,
                        team.getPreferredCategory(),
                        "OPEN",
                        LocalDateTime.now(),
                        PageRequest.of(0, CONTEST_RECOMMENDATION_POOL_SIZE))
                .getContent();
        List<Long> recommendedContestIds = aiClient.recommendContests(
                team.getPreferredCategory(),
                openContests.stream().map(Contest::getContestId).toList());

        if (recommendedContestIds.isEmpty()) {
            chatService.postChatbotMessage(team, CONTEST_SELECTION_PROMPT);
        } else {
            chatService.postChatbotCardMessage(
                    team,
                    MessageType.CONTEST_RECOMMEND_CARD,
                    CONTEST_SELECTION_PROMPT,
                    toIdsMetadata("contestIds", recommendedContestIds));
        }
    }

    /** 공모전이 확정된 직후(ContestVotingService) 호출되어 진행 단계로 전이시킨다. */
    @Transactional
    public void advanceToInProgress(Team team) {
        team.advanceStatus(TeamStatus.IN_PROGRESS);
        chatService.postChatbotMessage(team, IN_PROGRESS_PROMPT);
    }

    /** 활성 팀원 전원이 서로에 대한 리뷰를 다 썼을 때(ReviewService) 호출되어 리뷰 단계를 마무리한다. */
    @Transactional
    public void completeReview(Team team) {
        team.advanceStatus(TeamStatus.COMPLETED);
        chatService.postChatbotMessage(team, REVIEW_COMPLETE_PROMPT);
    }

    /**
     * 팀원이 메시지를 보낼 때마다 호출된다. 메시지가 "@챗봇"으로 시작하면 자유 질의로 간주해
     * AI 답변을 채팅에 남긴다. 챗봇이 꺼져있으면(Team.chatbotEnabled=false) 응답하지 않는다.
     */
    @Transactional
    public void respondToMentionIfAny(Long teamId, String content) {
        if (content == null || !content.trim().startsWith(CHATBOT_MENTION_PREFIX)) {
            return;
        }

        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (!team.isChatbotEnabled()) {
            return;
        }

        String question =
                content.trim().substring(CHATBOT_MENTION_PREFIX.length()).trim();
        String answer = aiClient.answerTeamQuestion(question);
        chatService.postChatbotMessage(team, answer);
    }

    private String toIdsMetadata(String key, List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(Map.of(key, ids));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 추천 메타데이터 직렬화에 실패했습니다.", e);
        }
    }

    // LeaderElectionService.toLeaderResultMetadata와 동일한 JSON 형태({key: 단일 id})를 내야
    // 프론트가 LEADER_RESULT_CARD를 어느 경로로 받았든 같은 방식으로 파싱할 수 있다.
    private String toIdMetadata(String key, Long id) {
        try {
            return objectMapper.writeValueAsString(Map.of(key, id));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("팀장 결과 메타데이터 직렬화에 실패했습니다.", e);
        }
    }
}
