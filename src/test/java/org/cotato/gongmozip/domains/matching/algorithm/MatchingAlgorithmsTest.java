package org.cotato.gongmozip.domains.matching.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cotato.gongmozip.domains.matching.support.MatchingCandidateFixture.candidate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.LongStream;
import org.cotato.gongmozip.domains.matching.algorithm.exception.MatchingSearchTimeoutException;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolInput;
import org.cotato.gongmozip.domains.matching.config.MatchingAlgorithmProperties;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.score.TeamCompatibilityCalculator;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchingAlgorithmsTest {

    private static final Instant NOW = Instant.parse("2026-08-02T05:00:00Z");

    private BruteForceMatchingAlgorithm bruteForce;
    private MultiStartGreedyMatchingAlgorithm greedy;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        MatchingPlanComparator comparator = new MatchingPlanComparator();
        TeamCompatibilityCalculator calculator = new TeamCompatibilityCalculator();
        MatchingAlgorithmProperties properties = new MatchingAlgorithmProperties();
        properties.setGreedyRestartCount(5);
        bruteForce = new BruteForceMatchingAlgorithm(calculator, comparator, clock);
        greedy = new MultiStartGreedyMatchingAlgorithm(calculator, comparator, properties, clock);
    }

    @Test
    @DisplayName("Brute Force는 8명을 정확히 4명씩 두 팀으로 배정한다")
    void bruteForceBuildsExactFourPersonTeams() {
        var plan = bruteForce.match(input(candidates(8), NOW.plusSeconds(30), 77L));

        assertThat(plan.teams()).hasSize(2);
        assertThat(plan.teams())
                .allSatisfy(team -> assertThat(team.candidates()).hasSize(4));
        assertThat(plan.unassignedCandidates()).isEmpty();
        assertThat(plan.assignedCount()).isEqualTo(8);
    }

    @Test
    @DisplayName("7명은 4인 팀 하나와 3인 팀 하나로 모두 배정한다")
    void supportsMixedFourAndThreePersonTeams() {
        var plan = bruteForce.match(input(candidates(7), NOW.plusSeconds(30), 99L));

        assertThat(plan.teams()).extracting(team -> team.candidates().size()).containsExactlyInAnyOrder(4, 3);
        assertThat(plan.assignedCount()).isEqualTo(7);
        assertThat(plan.unassignedCandidates()).isEmpty();
    }

    @Test
    @DisplayName("5명은 4인 팀을 우선 만들고 한 명을 미배정한다")
    void prefersFourPersonTeamWhenAssignedCountTies() {
        var plan = bruteForce.match(input(candidates(5), NOW.plusSeconds(30), 100L));

        assertThat(plan.teams()).singleElement().satisfies(team -> assertThat(team.candidates())
                .hasSize(4));
        assertThat(plan.unassignedCandidates()).hasSize(1);
    }

    @Test
    @DisplayName("Brute Force는 평균 궁합보다 재배정 대상자 배정을 우선한다")
    void bruteForceAssignsReassignmentCandidateBeforeAverageTieBreak() {
        List<MatchingCandidate> candidates = List.of(
                candidate(1),
                candidate(2),
                candidate(3),
                candidate(4),
                candidate(5, LeaderPreference.DOES_NOT_WANT, true, ExtroversionType.A, "3.00"));

        var plan = bruteForce.match(input(candidates, NOW.plusSeconds(30), 11L));

        assertThat(plan.assignedReassignmentCount()).isEqualTo(1);
        assertThat(plan.unassignedCandidates()).noneMatch(MatchingCandidate::reassignmentPriority);
    }

    @Test
    @DisplayName("Brute Force가 제한시간에 도달하면 부분 결과 대신 시간초과 신호를 반환한다")
    void bruteForceReturnsTimeoutSignalInsteadOfPartialResult() {
        assertThatThrownBy(() -> bruteForce.match(input(candidates(8), NOW, 1L)))
                .isInstanceOf(MatchingSearchTimeoutException.class);
    }

    @Test
    @DisplayName("Multi-start Greedy는 같은 입력과 시드에 항상 같은 결과를 반환한다")
    void greedyIsReproducibleForSameInputAndSeed() {
        MatchingPoolInput input = input(candidates(9), NOW.plusSeconds(30), 12345L);

        var first = greedy.match(input);
        var second = greedy.match(input);

        assertThat(first.canonicalApplicationOrder()).isEqualTo(second.canonicalApplicationOrder());
        assertThat(first.averageTeamScore()).isEqualByComparingTo(second.averageTeamScore());
        assertThat(first.assignedCount()).isEqualTo(9);
        assertThat(first.teams())
                .allSatisfy(team -> assertThat(team.candidates()).hasSize(3));
        assertThat(first.unassignedCandidates()).isEmpty();
    }

    private MatchingPoolInput input(List<MatchingCandidate> candidates, Instant deadline, long seed) {
        return new MatchingPoolInput(
                LocalDate.of(2026, 8, 2), InterestCategory.IT_AI_TECH, 2, candidates, seed, deadline);
    }

    private List<MatchingCandidate> candidates(int count) {
        return LongStream.rangeClosed(1, count)
                .mapToObj(id -> {
                    ExtroversionType type = ExtroversionType.values()[(int) ((id - 1) % 3)];
                    LeaderPreference leader = id % 4 == 0 ? LeaderPreference.WANTS : LeaderPreference.DOES_NOT_WANT;
                    String score = Integer.toString((int) ((id - 1) % 5) + 1) + ".00";
                    return candidate(id, leader, id == count, type, score);
                })
                .toList();
    }
}
