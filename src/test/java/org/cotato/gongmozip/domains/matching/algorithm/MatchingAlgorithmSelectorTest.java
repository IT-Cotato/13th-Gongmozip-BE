package org.cotato.gongmozip.domains.matching.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cotato.gongmozip.domains.matching.support.MatchingCandidateFixture.candidate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.LongStream;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolInput;
import org.cotato.gongmozip.domains.matching.config.MatchingAlgorithmProperties;
import org.cotato.gongmozip.domains.matching.enums.MatchingAlgorithmType;
import org.cotato.gongmozip.domains.matching.score.PartialTeamScoreCalculator;
import org.cotato.gongmozip.domains.matching.score.SimilarityScorer;
import org.cotato.gongmozip.domains.matching.score.TeamCompatibilityCalculator;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchingAlgorithmSelectorTest {

    private static final Instant NOW = Instant.parse("2026-08-02T05:00:00Z");

    private MatchingAlgorithmSelector selector;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        MatchingPlanComparator comparator = new MatchingPlanComparator();
        SimilarityScorer similarityScorer = new SimilarityScorer();
        TeamCompatibilityCalculator calculator = new TeamCompatibilityCalculator(similarityScorer);
        PartialTeamScoreCalculator partialCalculator = new PartialTeamScoreCalculator(similarityScorer);
        MatchingAlgorithmProperties properties = new MatchingAlgorithmProperties();
        properties.setGreedyRestartCount(2);
        var bruteForce = new BruteForceMatchingAlgorithm(calculator, comparator, clock);
        var greedy =
                new MultiStartGreedyMatchingAlgorithm(calculator, partialCalculator, comparator, properties, clock);
        selector = new MatchingAlgorithmSelector(bruteForce, greedy, properties, clock);
    }

    @Test
    @DisplayName("병합 완료 후 유효 풀 인원이 16명 이하면 Brute Force를 선택한다")
    void selectsAlgorithmByEffectivePoolSize() {
        assertThat(selector.initiallySelectedAlgorithm(2)).isEqualTo(MatchingAlgorithmType.NONE);
        assertThat(selector.initiallySelectedAlgorithm(3)).isEqualTo(MatchingAlgorithmType.BRUTE_FORCE);
        assertThat(selector.initiallySelectedAlgorithm(16)).isEqualTo(MatchingAlgorithmType.BRUTE_FORCE);
        assertThat(selector.initiallySelectedAlgorithm(17)).isEqualTo(MatchingAlgorithmType.MULTI_START_GREEDY);
    }

    @Test
    @DisplayName("Brute Force가 시간 초과되면 부분 결과를 버리고 전체 입력으로 Greedy를 실행한다")
    void fallsBackToGreedyWithWholeInput() {
        List<MatchingCandidate> candidates =
                LongStream.rangeClosed(1, 7).mapToObj(id -> candidate(id)).toList();
        MatchingPoolInput input =
                new MatchingPoolInput(LocalDate.of(2026, 8, 2), InterestCategory.IT_AI_TECH, 1, candidates, 77L, NOW);

        var plan = selector.match(input);

        assertThat(plan.selectedAlgorithm()).isEqualTo(MatchingAlgorithmType.MULTI_START_GREEDY);
        assertThat(plan.fallbackOccurred()).isTrue();
        assertThat(plan.assignedCount()).isEqualTo(7);
    }
}
