package org.cotato.gongmozip.domains.chatbot.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.repository.ContestRepository;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.global.ai.AiClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * {@code ChatbotOrchestrationService#advanceToContestSelecting}가 팀 상태를 CONTEST_SELECTING으로
 * 커밋한 뒤에 트리거하는 AI 공모전 추천 호출 전담 서비스. AI Gateway 응답을 기다리는 동안(최대 20초,
 * {@code AiGatewayClient}) 상태 전이 트랜잭션이나 스케줄러 스레드를 붙잡지 않도록, DB 트랜잭션 밖에서
 * {@code aiSummaryExecutor}(이미 프로젝트 AI 요약에 쓰이던 풀) 위에서 비동기로 실행한다
 * (docs/decisions/07-scheduler.md 확장 논의).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotContestRecommendationAsyncService {

    private static final int CONTEST_RECOMMENDATION_POOL_SIZE = 10;

    private final ContestRepository contestRepository;
    private final AiClient aiClient;
    private final ChatbotContestRecommendationTxService txService;

    @Async("aiSummaryExecutor")
    public void recommendContestsAsync(Long teamId, InterestCategory preferredCategory) {
        // 마감이 가장 많이 남은 순서대로 추천한다(팀이 막 꾸려진 시점이라 준비 기간이 넉넉한
        // 공모전을 우선 보여주는 편이 낫다는 판단, 2026-08-05).
        List<Contest> openContests = contestRepository
                .findAllWithFilterAndDeadlineDesc(
                        null,
                        preferredCategory,
                        "OPEN",
                        LocalDateTime.now(),
                        PageRequest.of(0, CONTEST_RECOMMENDATION_POOL_SIZE))
                .getContent();

        List<Long> recommendedContestIds;
        try {
            recommendedContestIds = aiClient.recommendContests(
                    preferredCategory,
                    openContests.stream().map(Contest::getContestId).toList());
        } catch (Exception e) {
            log.error("공모전 추천 AI 호출 실패 - teamId: {}", teamId, e);
            txService.announcePlainPrompt(teamId);
            return;
        }

        if (recommendedContestIds.isEmpty()) {
            txService.announcePlainPrompt(teamId);
        } else {
            txService.registerCandidatesAndAnnounce(teamId, openContests, recommendedContestIds);
        }
    }
}
