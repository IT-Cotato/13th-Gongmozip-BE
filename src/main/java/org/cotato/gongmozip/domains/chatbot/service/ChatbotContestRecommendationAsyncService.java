package org.cotato.gongmozip.domains.chatbot.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    // 선호 카테고리 안에 열린 공모전이 적어(카테고리 자체가 희소한 경우) 추천이 0~1개로 끝나는
    // 팀이 실제로 있었다(2026-08-18 확인). 후보 리스트가 아예 텅 비거나 1개뿐이면 팀원 입장에서
    // "투표할 게 없다"는 인상을 주므로, 부족하면 다른 카테고리에서라도 채워 최소 2개를 보장한다.
    private static final int MIN_CONTEST_RECOMMENDATIONS = 2;

    private final ContestRepository contestRepository;
    private final AiClient aiClient;
    private final ChatbotContestRecommendationTxService txService;

    @Async("aiSummaryExecutor")
    public void recommendContestsAsync(Long teamId, InterestCategory preferredCategory, boolean autoAssignedLeader) {
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
            recommendedContestIds = new ArrayList<>(aiClient.recommendContests(
                    preferredCategory,
                    openContests.stream().map(Contest::getContestId).toList()));
        } catch (Exception e) {
            log.error("공모전 추천 AI 호출 실패 - teamId: {}", teamId, e);
            txService.announcePlainPrompt(teamId, autoAssignedLeader);
            return;
        }

        List<Contest> candidatePool = openContests;
        if (recommendedContestIds.size() < MIN_CONTEST_RECOMMENDATIONS) {
            candidatePool = topUpAcrossCategories(teamId, preferredCategory, openContests, recommendedContestIds);
        }

        if (recommendedContestIds.isEmpty()) {
            txService.announcePlainPrompt(teamId, autoAssignedLeader);
        } else {
            txService.registerCandidatesAndAnnounce(teamId, candidatePool, recommendedContestIds, autoAssignedLeader);
        }
    }

    // 선호 카테고리 풀만으로 MIN_CONTEST_RECOMMENDATIONS를 못 채우면, 카테고리 제한 없이(null)
    // 마감 임박 안 된 순으로 같은 풀 사이즈만큼 다시 조회해서 아직 안 뽑힌 공모전으로 부족분을
    // 채운다. 그래도 전체 공모전 자체가 부족하면(이론상 드묾) 채울 수 있는 만큼만 채운다.
    //
    // 반환하는 풀은 contestId 기준으로 중복 제거해야 한다 — 카테고리 제한 없이 다시 조회한
    // fallbackPool엔 openContests와 같은 공모전(id)이 다른 객체 인스턴스로 다시 담겨 올 수 있고,
    // Contest는 equals/hashCode를 재정의하지 않아 인스턴스 기준으로는 중복이 안 걸러진다 — 이
    // 상태로 registerCandidatesAndAnnounce에 넘기면 그쪽의 Collectors.toMap(contestId 키)이
    // "Duplicate key" 예외를 던진다.
    private List<Contest> topUpAcrossCategories(
            Long teamId,
            InterestCategory preferredCategory,
            List<Contest> openContests,
            List<Long> recommendedContestIds) {
        List<Contest> fallbackPool = contestRepository
                .findAllWithFilterAndDeadlineDesc(
                        null, null, "OPEN", LocalDateTime.now(), PageRequest.of(0, CONTEST_RECOMMENDATION_POOL_SIZE))
                .getContent();

        Map<Long, Contest> poolById = new LinkedHashMap<>();
        openContests.forEach(contest -> poolById.put(contest.getContestId(), contest));

        boolean usedFallback = false;
        for (Contest contest : fallbackPool) {
            if (recommendedContestIds.size() >= MIN_CONTEST_RECOMMENDATIONS) {
                break;
            }
            if (recommendedContestIds.contains(contest.getContestId())) {
                continue;
            }
            recommendedContestIds.add(contest.getContestId());
            poolById.put(contest.getContestId(), contest);
            usedFallback = true;
        }

        if (usedFallback) {
            log.info("선호 카테고리({}) 풀만으로 최소 추천 수를 못 채워 다른 카테고리에서 보충함 - teamId: {}", preferredCategory, teamId);
        }
        return List.copyOf(poolById.values());
    }
}
