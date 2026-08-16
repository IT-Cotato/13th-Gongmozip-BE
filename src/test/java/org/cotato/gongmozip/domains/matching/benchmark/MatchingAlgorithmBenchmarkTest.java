package org.cotato.gongmozip.domains.matching.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cotato.gongmozip.domains.matching.support.MatchingCandidateFixture.candidate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import org.cotato.gongmozip.domains.matching.algorithm.BruteForceMatchingAlgorithm;
import org.cotato.gongmozip.domains.matching.algorithm.MatchingPlanComparator;
import org.cotato.gongmozip.domains.matching.algorithm.MultiStartGreedyMatchingAlgorithm;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolInput;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;
import org.cotato.gongmozip.domains.matching.config.MatchingAlgorithmProperties;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.score.SimilarityScorer;
import org.cotato.gongmozip.domains.matching.score.TeamCompatibilityCalculator;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("matching-benchmark")
class MatchingAlgorithmBenchmarkTest {

    /**
     * 작은 풀에서는 Brute Force가 찾은 최적해와 Greedy의 근사해를 직접 비교한다.
     *
     * <p>Brute Force는 가능한 팀 조합을 모두 탐색하므로 풀 인원이 커지면 연산량이 급격히 증가한다. 따라서 운영 정책상 Brute Force를
     * 사용하는 최대 크기인 12명까지만 비교하는 것이 안전하다. 12명을 초과하는 풀의 처리 시간은 아래의 Greedy 전용 구간에서 측정한다.
     *
     * <p>주의: 첫 번째 반복 목록에 100명처럼 큰 값을 넣으면 Brute Force도 함께 실행된다. 가능한 모든 조합을 확인하는 Brute Force의
     * 특성상 현실적인 시간 안에 완료되기 어렵기 때문에, 큰 풀은 Greedy 전용 측정 목록에 넣는다.
     */
    @Test
    @DisplayName("Brute Force 대비 Greedy의 배정 정확도와 평균 궁합 손실률을 검증한다")
    void compareBruteForceAndGreedyAndMeasureLargeGreedyPools() {
        // 실제 운영 매칭 코드와 같은 계산기와 결과 비교기를 직접 사용한다.
        // DB 조회/저장, 네트워크, 외부 AI 호출 시간은 이 벤치마크에 포함되지 않는다.
        Clock clock = Clock.systemUTC();
        MatchingPlanComparator comparator = new MatchingPlanComparator();
        TeamCompatibilityCalculator calculator = new TeamCompatibilityCalculator(new SimilarityScorer());
        MatchingAlgorithmProperties properties = new MatchingAlgorithmProperties();

        // 무작위 앵커 순서를 50회 생성해 Greedy를 반복한다.
        // 이 값을 늘리면 더 좋은 조합을 찾을 가능성이 커지지만 실행 시간도 함께 증가한다.
        properties.setGreedyRestartCount(50);
        BruteForceMatchingAlgorithm bruteForce = new BruteForceMatchingAlgorithm(calculator, comparator, clock);
        MultiStartGreedyMatchingAlgorithm greedy =
                new MultiStartGreedyMatchingAlgorithm(calculator, comparator, properties, clock);

        // Greedy가 Brute Force의 최적 평균 궁합 점수에서 얼마나 손해를 보는지 풀별로 저장한다.
        List<BigDecimal> losses = new ArrayList<>();

        // Brute Force와 Greedy를 모두 실행하는 품질 비교 구간이다.
        // 운영 기준은 12명 이하이므로 원칙적으로 3~12만 둔다.
        // 100을 이 목록에 두면 Brute Force까지 100명으로 실행되므로 사실상 완료 시간을 측정하기 어렵다.
        for (int poolSize : List.of(3, 4, 5, 6, 7, 8, 9, 10, 11, 12)) {
            // 벤치마크는 완료까지 걸리는 시간을 측정하는 목적이므로 알고리즘 마감 시간을 제한하지 않는다.
            MatchingPoolInput input = input(poolSize, Instant.MAX);

            // Brute Force 결과를 최적해로 보고 Greedy 결과의 배정 수와 평균 점수를 비교한다.
            MatchingPlan optimum = bruteForce.match(input);
            MatchingPlan approximate = greedy.match(input);

            // Greedy도 최적해와 동일한 수의 전체 인원 및 재배정 대상자를 배정해야 한다.
            assertThat(approximate.assignedCount()).isEqualTo(optimum.assignedCount());
            assertThat(approximate.assignedReassignmentCount()).isEqualTo(optimum.assignedReassignmentCount());

            // 손실률 = (최적 평균점수 - Greedy 평균점수) / 최적 평균점수 * 100
            BigDecimal loss = lossRate(optimum.averageTeamScore(), approximate.averageTeamScore());
            losses.add(loss);
            System.out.printf(
                    "매칭 벤치마크 | 풀 인원=%d명 | 브루트포스=%dms | 그리디=%dms | " + "최적 평균점수=%s | 그리디 평균점수=%s | 손실률=%s%%%n",
                    poolSize,
                    optimum.elapsedTime().toMillis(),
                    approximate.elapsedTime().toMillis(),
                    optimum.averageTeamScore(),
                    approximate.averageTeamScore(),
                    loss);
        }

        // 12명을 초과하면 운영 코드가 Greedy를 선택하므로, 큰 풀은 Brute Force와 비교하지 않고 완료까지 걸린 시간만 측정한다.
        // 이 구간에서는 Greedy만 실행하므로 설정된 반복을 모두 수행한 실제 순수 계산 시간을 확인할 수 있다.
        for (int poolSize : List.of(13, 14, 15, 16, 20)) {
            MatchingPlan approximate = greedy.match(input(poolSize, Instant.MAX));

            // 3명 또는 4명 팀으로 모든 인원을 배정할 수 있는 크기이므로 전원 배정되었는지 검증한다.
            assertThat(approximate.assignedCount()).isEqualTo(poolSize);
            System.out.printf(
                    "매칭 벤치마크 | 풀 인원=%d명 | 알고리즘=그리디 | 실행시간=%dms | 평균점수=%s%n",
                    poolSize, approximate.elapsedTime().toMillis(), approximate.averageTeamScore());
        }

        // 모든 품질 비교 결과를 오름차순으로 정렬한 뒤 95백분위 손실률과 최대 손실률을 계산한다.
        losses.sort(Comparator.naturalOrder());
        int p95Index = Math.max(0, (int) Math.ceil(losses.size() * 0.95) - 1);
        BigDecimal p95 = losses.get(p95Index);
        BigDecimal maximum = losses.getLast();

        // Greedy 품질 허용 기준: 손실률 p95는 1% 이하, 최악의 경우에도 3% 이하여야 한다.
        assertThat(p95).isLessThanOrEqualTo(new BigDecimal("1.00"));
        assertThat(maximum).isLessThanOrEqualTo(new BigDecimal("3.00"));
    }

    /**
     * 카테고리 6개와 역량 그룹 4개가 모두 만들어진 최댓값인 24개 풀을 순서대로 처리했을 때의 순수 계산 시간을 측정한다.
     *
     * <p>각 풀은 Brute Force 적용 상한인 12명으로 만든다. 이 테스트 역시 DB와 외부 통신 시간은 포함하지 않는다.
     */
    @Test
    @DisplayName("최대 24개 유효 풀을 순차 처리하는 합성 배치 시간을 측정한다")
    void measureTwentyFourEffectivePools() {
        Clock clock = Clock.systemUTC();
        MatchingPlanComparator comparator = new MatchingPlanComparator();
        TeamCompatibilityCalculator calculator = new TeamCompatibilityCalculator(new SimilarityScorer());
        BruteForceMatchingAlgorithm bruteForce = new BruteForceMatchingAlgorithm(calculator, comparator, clock);

        long startedAt = System.nanoTime();

        // 24개의 서로 독립적인 12명 풀을 실제 Brute Force 알고리즘으로 순차 계산한다.
        // dataVariant에 풀 번호를 전달해 후보 성향과 랜덤 시드가 완전히 같은 입력을 24번 재사용하지 않도록 한다.
        for (int pool = 0; pool < 24; pool++) {
            // 0번 변형은 위의 3~12명 품질 비교에서 이미 사용하므로 1번부터 시작한다.
            MatchingPlan result = bruteForce.match(input(12, Instant.MAX, pool + 1));
            assertThat(result.assignedCount()).isEqualTo(12);
        }
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        System.out.printf("매칭 벤치마크 | 유효 풀=24개 | 전체 실행시간=%dms%n", elapsedMillis);
    }

    /** 주어진 인원수만큼 재현 가능한 가상 후보자를 생성해 알고리즘 입력을 만든다. */
    private MatchingPoolInput input(int size, Instant deadline) {
        return input(size, deadline, 0);
    }

    /**
     * 같은 인원수의 여러 풀을 측정할 때도 동일한 후보 조합을 반복하지 않도록 dataVariant만큼 후보 특성과 시드를 변경한다.
     *
     * <p>dataVariant가 같으면 언제 실행해도 같은 입력이 만들어지므로 성능 결과를 다시 비교할 수 있다.
     */
    private MatchingPoolInput input(int size, Instant deadline, int dataVariant) {
        List<MatchingCandidate> candidates = LongStream.rangeClosed(1, size)
                .mapToObj(id -> candidate(
                        id,
                        // 풀마다 팀장 희망자의 위치가 달라지도록 만든다.
                        (id + dataVariant) % 5 == 0 ? LeaderPreference.WANTS : LeaderPreference.DOES_NOT_WANT,
                        // 풀마다 재배정 대상자 두 명의 위치가 달라지도록 만든다.
                        id == (dataVariant % size) + 1 || id == ((dataVariant + 5) % size) + 1,
                        // 풀마다 I/A/E 외향성 배치 순서가 달라지도록 만든다.
                        ExtroversionType.values()[(int) ((id * 7 + dataVariant) % 3)],
                        // 풀마다 1.00~5.00 사이의 응답 점수 배치가 달라지도록 만든다.
                        ((id * 11 + dataVariant) % 5 + 1) + ".00"))
                .toList();

        // 날짜, 카테고리, 풀 번호, 후보 목록, 무작위 시드, 알고리즘 마감 시각을 묶는다.
        // 같은 size와 dataVariant를 사용하면 같은 시드가 만들어져 반복 실행 결과를 비교하기 쉽다.
        // poolOrdinal은 역량 그룹 번호이므로 dataVariant와 분리해 반드시 1~4 범위로 순환시킨다.
        int poolOrdinal = dataVariant % 4 + 1;
        return new MatchingPoolInput(
                LocalDate.of(2026, 8, 2),
                InterestCategory.IT_AI_TECH,
                poolOrdinal,
                candidates,
                20260802L + size * 100L + dataVariant,
                deadline);
    }

    /** Brute Force 최적 평균점수를 기준으로 Greedy 평균점수의 손실 비율을 백분율로 계산한다. */
    private BigDecimal lossRate(BigDecimal optimum, BigDecimal approximate) {
        // 최적 점수가 0이면 0으로 나눌 수 없으므로 손실도 0%로 처리한다.
        if (optimum.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        // Greedy 점수가 우연히 최적 점수보다 높게 계산되더라도 음수 손실률을 만들지 않는다.
        return optimum.subtract(approximate)
                .max(BigDecimal.ZERO)
                .divide(optimum, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
