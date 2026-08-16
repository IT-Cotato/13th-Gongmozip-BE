package org.cotato.gongmozip.domains.matching.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cotato.gongmozip.domains.matching.support.MatchingCandidateFixture.candidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamCompatibilityCalculatorTest {

    private final TeamCompatibilityCalculator calculator = new TeamCompatibilityCalculator(new SimilarityScorer());

    @Test
    @DisplayName("4인 이상적 조합은 총점 100점을 받는다")
    void idealFourPersonTeamScoresOneHundred() {
        List<MatchingCandidate> team = List.of(
                candidate(1, LeaderPreference.WANTS, false, ExtroversionType.E, "3.00"),
                candidate(2, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.A, "3.00"),
                candidate(3, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.A, "3.00"),
                candidate(4, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.I, "3.00"));

        var score = calculator.calculate(team);

        assertThat(score.leaderHarmonyScore()).isEqualByComparingTo("10.00");
        assertThat(score.extroversionComplementScore()).isEqualByComparingTo("20.00");
        assertThat(score.totalScore()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("3인 이상적 조합도 총점 100점을 받는다")
    void idealThreePersonTeamScoresOneHundred() {
        List<MatchingCandidate> team = List.of(
                candidate(1, LeaderPreference.WANTS, false, ExtroversionType.E, "3.00"),
                candidate(2, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.A, "3.00"),
                candidate(3, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.I, "3.00"));

        assertThat(calculator.calculate(team).totalScore()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("1·1·5·5의 모집단 분산은 유사성 점수를 0점으로 만든다")
    void maximumPopulationVarianceScoresZeroSimilarity() {
        List<MatchingCandidate> team = List.of(
                candidate(1, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.E, "1.00"),
                candidate(2, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.E, "1.00"),
                candidate(3, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.E, "5.00"),
                candidate(4, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.E, "5.00"));

        var score = calculator.calculate(team);

        assertThat(score.goalSimilarityScore()).isEqualByComparingTo("0.00");
        assertThat(score.agreeablenessSimilarityScore()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("4인 리더 배점표의 모든 경우를 그대로 계산한다")
    void allFourPersonLeaderCases() {
        int[][] cases = {
            {1, 0, 10}, {1, 3, 10}, {0, 4, 8}, {0, 3, 7}, {0, 2, 6},
            {0, 1, 5}, {0, 0, 0}, {2, 0, 7}, {3, 0, 3}, {4, 0, 0}
        };

        for (int[] testCase : cases) {
            assertThat(calculator
                            .calculate(teamWithLeaders(4, testCase[0], testCase[1]))
                            .leaderHarmonyScore())
                    .as("WANTS=%d NEUTRAL=%d", testCase[0], testCase[1])
                    .isEqualByComparingTo(Integer.toString(testCase[2]));
        }
    }

    @Test
    @DisplayName("3인 리더 배점표의 모든 경우를 그대로 계산한다")
    void allThreePersonLeaderCases() {
        int[][] cases = {{1, 0, 10}, {1, 2, 10}, {0, 3, 8}, {0, 2, 7}, {0, 1, 5}, {0, 0, 0}, {2, 0, 5}, {3, 0, 0}};

        for (int[] testCase : cases) {
            assertThat(calculator
                            .calculate(teamWithLeaders(3, testCase[0], testCase[1]))
                            .leaderHarmonyScore())
                    .as("WANTS=%d NEUTRAL=%d", testCase[0], testCase[1])
                    .isEqualByComparingTo(Integer.toString(testCase[2]));
        }
    }

    @Test
    @DisplayName("4인 외향성 15개 분포를 확정표대로 계산한다")
    void allFourPersonExtroversionDistributions() {
        int[][] cases = {
            {1, 2, 1, 20}, {1, 1, 2, 18}, {2, 1, 1, 18}, {0, 4, 0, 17}, {1, 3, 0, 16},
            {0, 3, 1, 16}, {2, 0, 2, 15}, {1, 0, 3, 14}, {2, 2, 0, 13}, {0, 2, 2, 13},
            {3, 1, 0, 10}, {3, 0, 1, 10}, {0, 1, 3, 9}, {4, 0, 0, 5}, {0, 0, 4, 5}
        };
        assertExtroversionCases(cases);
    }

    @Test
    @DisplayName("3인 외향성 10개 분포를 확정표대로 계산한다")
    void allThreePersonExtroversionDistributions() {
        int[][] cases = {
            {1, 1, 1, 20}, {0, 3, 0, 18}, {1, 2, 0, 16}, {0, 2, 1, 15}, {1, 0, 2, 14},
            {2, 1, 0, 11}, {0, 1, 2, 10}, {2, 0, 1, 8}, {0, 0, 3, 5}, {3, 0, 0, 3}
        };
        assertExtroversionCases(cases);
    }

    @Test
    @DisplayName("후보 순서를 바꿔도 모든 궁합 점수는 같다")
    void candidateOrderDoesNotChangeScore() {
        List<MatchingCandidate> original = new ArrayList<>(List.of(
                candidate(1, LeaderPreference.WANTS, false, ExtroversionType.E, "1.50"),
                candidate(2, LeaderPreference.NEUTRAL, false, ExtroversionType.A, "2.50"),
                candidate(3, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.A, "3.50"),
                candidate(4, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.I, "4.50")));
        var expected = calculator.calculate(original);
        Collections.reverse(original);

        assertThat(calculator.calculate(original)).isEqualTo(expected);
    }

    private void assertExtroversionCases(int[][] cases) {
        for (int[] testCase : cases) {
            List<MatchingCandidate> team = teamWithDistribution(testCase[0], testCase[1], testCase[2]);
            assertThat(calculator.calculate(team).extroversionComplementScore())
                    .as("E%d A%d I%d", testCase[0], testCase[1], testCase[2])
                    .isEqualByComparingTo(Integer.toString(testCase[3]));
        }
    }

    private List<MatchingCandidate> teamWithLeaders(int size, int wants, int neutral) {
        return IntStream.range(0, size)
                .mapToObj(index -> candidate(
                        index + 1L,
                        index < wants
                                ? LeaderPreference.WANTS
                                : index < wants + neutral ? LeaderPreference.NEUTRAL : LeaderPreference.DOES_NOT_WANT,
                        false,
                        ExtroversionType.A,
                        "3.00"))
                .toList();
    }

    private List<MatchingCandidate> teamWithDistribution(int e, int a, int i) {
        List<ExtroversionType> types = new ArrayList<>();
        IntStream.range(0, e).forEach(ignored -> types.add(ExtroversionType.E));
        IntStream.range(0, a).forEach(ignored -> types.add(ExtroversionType.A));
        IntStream.range(0, i).forEach(ignored -> types.add(ExtroversionType.I));
        return IntStream.range(0, types.size())
                .mapToObj(
                        index -> candidate(index + 1L, LeaderPreference.DOES_NOT_WANT, false, types.get(index), "3.00"))
                .toList();
    }
}
