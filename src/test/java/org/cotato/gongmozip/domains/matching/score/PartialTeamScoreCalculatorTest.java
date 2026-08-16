package org.cotato.gongmozip.domains.matching.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cotato.gongmozip.domains.matching.support.MatchingCandidateFixture.candidate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.score.PartialTeamScoreCalculator.PartialScore;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PartialTeamScoreCalculatorTest {

    private final PartialTeamScoreCalculator calculator = new PartialTeamScoreCalculator(new SimilarityScorer());

    @Test
    @DisplayName("유사한 응답의 후보가 상이한 응답의 후보보다 높은 점수를 받는다")
    void similarCandidateOutranksDifferent() {
        MatchingCandidate anchor = candidate(1, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.A, "3.00");
        MatchingCandidate similar = candidate(2, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.A, "3.00");
        MatchingCandidate different = candidate(3, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.A, "5.00");

        PartialScore similarScore = calculator.evaluateWithCandidate(List.of(anchor), similar);
        PartialScore differentScore = calculator.evaluateWithCandidate(List.of(anchor), different);

        assertThat(similarScore.compareTo(differentScore)).isPositive();
    }

    @Test
    @DisplayName("현재 팀에 WANTS가 있으면 WANTS 후보를 감점한다")
    void wantsPileUpIsPenalized() {
        MatchingCandidate anchor = candidate(1, LeaderPreference.WANTS, false, ExtroversionType.A, "3.00");
        MatchingCandidate anotherWants = candidate(2, LeaderPreference.WANTS, false, ExtroversionType.A, "3.00");

        PartialScore score = calculator.evaluateWithCandidate(List.of(anchor), anotherWants);

        assertThat(score.wantsPenalty()).isEqualTo(1);
    }

    @Test
    @DisplayName("현재 팀에 WANTS가 없으면 WANTS 후보를 감점하지 않는다")
    void firstWantsIsNotPenalized() {
        MatchingCandidate anchor = candidate(1, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.A, "3.00");
        MatchingCandidate wantsCandidate = candidate(2, LeaderPreference.WANTS, false, ExtroversionType.A, "3.00");

        PartialScore score = calculator.evaluateWithCandidate(List.of(anchor), wantsCandidate);

        assertThat(score.wantsPenalty()).isZero();
    }

    @Test
    @DisplayName("재배정 대상자 후보는 가산점을 받는다")
    void reassignmentCandidateReceivesBonus() {
        MatchingCandidate anchor = candidate(1, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.A, "3.00");
        MatchingCandidate reassigned = candidate(2, LeaderPreference.DOES_NOT_WANT, true, ExtroversionType.A, "3.00");

        PartialScore score = calculator.evaluateWithCandidate(List.of(anchor), reassigned);

        assertThat(score.reassignmentBonus()).isEqualTo(1);
    }

    @Test
    @DisplayName("유사도가 같으면 이른 신청 시각의 후보가 더 큰 점수로 정렬된다")
    void tieBreaksByEarlierAppliedAt() {
        // 유사도·페널티·보너스가 모두 같은 두 점수를 직접 만들어 tie-break 비교만 검증한다.
        PartialScore early = new PartialScore(new BigDecimal("40.00"), 0, 0, LocalDateTime.of(2026, 8, 2, 12, 0), 100L);
        PartialScore late = new PartialScore(new BigDecimal("40.00"), 0, 0, LocalDateTime.of(2026, 8, 2, 13, 0), 100L);

        assertThat(early.compareTo(late)).isPositive();
    }

    @Test
    @DisplayName("유사도와 신청 시각이 같으면 작은 신청 ID의 후보가 더 큰 점수로 정렬된다")
    void tieBreaksByLowerApplicationId() {
        LocalDateTime appliedAt = LocalDateTime.of(2026, 8, 2, 12, 0);
        PartialScore lower = new PartialScore(new BigDecimal("40.00"), 0, 0, appliedAt, 100L);
        PartialScore higher = new PartialScore(new BigDecimal("40.00"), 0, 0, appliedAt, 200L);

        assertThat(lower.compareTo(higher)).isPositive();
    }

    @Test
    @DisplayName("빈 현재 팀은 예외를 던진다")
    void emptyCurrentIsRejected() {
        MatchingCandidate cand = candidate(1);

        assertThatThrownBy(() -> calculator.evaluateWithCandidate(List.of(), cand))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("네 명 이상의 현재 팀은 예외를 던진다")
    void overfullCurrentIsRejected() {
        List<MatchingCandidate> current = List.of(candidate(1), candidate(2), candidate(3), candidate(4));

        assertThatThrownBy(() -> calculator.evaluateWithCandidate(current, candidate(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("현재 팀에 이미 포함된 후보는 예외를 던진다")
    void duplicateCandidateIsRejected() {
        MatchingCandidate anchor = candidate(1);

        assertThatThrownBy(() -> calculator.evaluateWithCandidate(List.of(anchor), anchor))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
