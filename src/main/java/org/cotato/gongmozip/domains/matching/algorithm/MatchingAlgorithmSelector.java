package org.cotato.gongmozip.domains.matching.algorithm;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.algorithm.exception.MatchingSearchTimeoutException;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolInput;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;
import org.cotato.gongmozip.domains.matching.config.MatchingAlgorithmProperties;
import org.cotato.gongmozip.domains.matching.enums.MatchingAlgorithmType;
import org.springframework.stereotype.Component;

/**
 * 풀 크기와 남은 실행시간에 따라 완전탐색과 Greedy를 선택하는 정책을 알고리즘 구현에서 분리하기 위해 만들었다.
 * 작은 풀의 완전탐색이 제한시간을 넘기면 동일한 전체 입력을 Greedy로 다시 계산하고 두 탐색의 실행 지표를 합쳐 반환한다.
 */
@Component
@RequiredArgsConstructor
public class MatchingAlgorithmSelector {

    private final BruteForceMatchingAlgorithm bruteForce;
    private final MultiStartGreedyMatchingAlgorithm greedy;
    private final MatchingAlgorithmProperties properties;
    private final Clock clock;

    public MatchingPlan match(MatchingPoolInput input) {
        // 3인 팀조차 만들 수 없는 풀은 모든 신청자를 미배정한 정상 결과로 처리한다.
        if (input.teamSizes().isEmpty()) {
            return MatchingPlan.create(
                    java.util.List.of(),
                    input.candidates(),
                    MatchingAlgorithmType.NONE,
                    input.seed(),
                    Duration.ZERO,
                    0,
                    0,
                    0,
                    false);
        }
        if (input.candidates().size() > properties.getBruteforceMaxPoolSize()) {
            return greedy.match(withGreedyDeadline(input));
        }

        // 작은 풀은 최적해를 우선 탐색하되, 시간 초과 시 부분 결과를 버리고 Greedy로 안전하게 전환한다.
        Instant startedAt = clock.instant();
        try {
            return bruteForce.match(input);
        } catch (MatchingSearchTimeoutException timeout) {
            MatchingPlan fallback = greedy.match(withGreedyDeadline(input));
            return fallback.withFallbackMetrics(
                    Duration.between(startedAt, clock.instant()),
                    timeout.getEvaluatedCombinationCount(),
                    timeout.getExploredBranchCount());
        }
    }

    private MatchingPoolInput withGreedyDeadline(MatchingPoolInput input) {
        // 완전탐색에 남겨 둔 fallbackReserve를 Greedy 실행시간으로 돌려준다.
        return new MatchingPoolInput(
                input.applicationDate(),
                input.category(),
                input.poolOrdinal(),
                input.candidates(),
                input.teamSizes(),
                input.seed(),
                input.searchDeadline().plus(properties.getFallbackReserve()));
    }

    public MatchingAlgorithmType initiallySelectedAlgorithm(int poolSize) {
        // 배치 시작 시점의 예상 알고리즘을 기록하기 위한 값이며 실제 최종 알고리즘은 fallback에 따라 달라질 수 있다.
        if (poolSize < 3) return MatchingAlgorithmType.NONE;
        return poolSize <= properties.getBruteforceMaxPoolSize()
                ? MatchingAlgorithmType.BRUTE_FORCE
                : MatchingAlgorithmType.MULTI_START_GREEDY;
    }
}
