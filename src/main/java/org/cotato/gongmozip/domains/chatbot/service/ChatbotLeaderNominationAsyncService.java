package org.cotato.gongmozip.domains.chatbot.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.team.converter.TeamConverter;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.cotato.gongmozip.global.ai.dto.LeaderCandidateSnapshot;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * {@code ChatbotOrchestrationService#advanceAfterGreeting}(OPEN_NOMINATION 분기)이 팀 상태를
 * LEADER_SELECTING으로 커밋한 뒤에 트리거하는 AI 팀장 후보 추천 호출 전담 서비스.
 * {@link ChatbotContestRecommendationAsyncService}와 동일한 이유로 트랜잭션/스케줄러 스레드 밖에서
 * {@code aiSummaryExecutor}에서 비동기로 실행한다(스레드풀 점유 이슈 점검, 2026-08-15).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotLeaderNominationAsyncService {

    private final TeamMemberRepository teamMemberRepository;
    private final AiClient aiClient;
    private final ChatbotLeaderNominationTxService txService;

    @Async("aiSummaryExecutor")
    public void recommendLeaderNomineesAsync(Long teamId) {
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        List<LeaderCandidateSnapshot> snapshots = activeMembers.stream()
                .map(TeamConverter::toLeaderCandidateSnapshot)
                .toList();

        List<Long> recommendedIds;
        try {
            recommendedIds = aiClient.recommendLeaderCandidates(teamId, snapshots);
        } catch (Exception e) {
            log.error("팀장 후보 추천 AI 호출 실패 - teamId: {}", teamId, e);
            recommendedIds = List.of();
        }

        txService.announceLeaderNomination(teamId, recommendedIds);
    }
}
