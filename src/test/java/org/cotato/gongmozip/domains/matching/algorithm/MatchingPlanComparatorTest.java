package org.cotato.gongmozip.domains.matching.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cotato.gongmozip.domains.matching.support.MatchingCandidateFixture.candidate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchedTeam;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.TeamCompatibilityScore;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingAlgorithmType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchingPlanComparatorTest {

    private final MatchingPlanComparator comparator = new MatchingPlanComparator();

    @Test
    @DisplayName("재배정 대상자 배정 수는 전체 팀 평균 궁합보다 먼저 비교한다")
    void reassignmentCountPrecedesAverageScore() {
        MatchingCandidate reassignment = candidate(5, LeaderPreference.DOES_NOT_WANT, true, ExtroversionType.A, "3.00");
        MatchingPlan withReassignment = plan(List.of(candidate(1), candidate(2), candidate(3), reassignment), 60);
        MatchingPlan higherAverage = plan(List.of(candidate(1), candidate(2), candidate(3), candidate(4)), 100);

        assertThat(comparator.compare(withReassignment, higherAverage)).isPositive();
    }

    @Test
    @DisplayName("전체 팀 평균 궁합은 팀장 희망자 배정 수보다 먼저 비교한다")
    void averageScorePrecedesWantsCount() {
        MatchingCandidate wants = candidate(4, LeaderPreference.WANTS, false, ExtroversionType.A, "3.00");
        MatchingPlan withWants = plan(List.of(candidate(1), candidate(2), candidate(3), wants), 70);
        MatchingPlan higherAverage = plan(List.of(candidate(5), candidate(6), candidate(7), candidate(8)), 80);

        assertThat(comparator.compare(higherAverage, withWants)).isPositive();
    }

    private MatchingPlan plan(List<MatchingCandidate> candidates, int total) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        TeamCompatibilityScore score = new TeamCompatibilityScore(
                zero,
                zero,
                zero,
                zero,
                zero,
                zero,
                zero,
                zero,
                BigDecimal.valueOf(total).setScale(2));
        return MatchingPlan.create(
                List.of(new MatchedTeam(candidates, score)),
                List.of(),
                MatchingAlgorithmType.BRUTE_FORCE,
                1L,
                Duration.ZERO,
                0,
                0,
                0,
                false);
    }
}
