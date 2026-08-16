package org.cotato.gongmozip.domains.matching.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimilarityScorerTest {

    private final SimilarityScorer scorer = new SimilarityScorer();

    @Test
    @DisplayName("모두 같은 값이면 분산 0으로 만점을 준다")
    void identicalValuesGiveMaximumScore() {
        List<BigDecimal> values = List.of(new BigDecimal("3.00"), new BigDecimal("3.00"), new BigDecimal("3.00"));

        assertThat(scorer.score(values, 10)).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("최대 분산이면 0점을 준다")
    void maximumVarianceGivesZero() {
        List<BigDecimal> values =
                List.of(new BigDecimal("1.00"), new BigDecimal("1.00"), new BigDecimal("5.00"), new BigDecimal("5.00"));

        assertThat(scorer.score(values, 20)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("2명 입력도 계산할 수 있다")
    void twoValueInputIsSupported() {
        // 미완성 팀 후보 순위 계산이 이 케이스에 의존한다.
        List<BigDecimal> values = List.of(new BigDecimal("2.00"), new BigDecimal("4.00"));

        BigDecimal score = scorer.score(values, 10);

        // 분산 1, 정규화 1/4 → (1 - 0.25) * 10 = 7.50
        assertThat(score).isEqualByComparingTo("7.50");
    }

    @Test
    @DisplayName("값이 하나뿐이면 예외를 던진다")
    void singleValueIsRejected() {
        assertThatThrownBy(() -> scorer.score(List.of(new BigDecimal("3.00")), 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 리스트는 예외를 던진다")
    void nullListIsRejected() {
        assertThatThrownBy(() -> scorer.score(null, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("1~5 범위를 벗어난 값은 예외를 던진다")
    void outOfRangeValueIsRejected() {
        List<BigDecimal> values = List.of(new BigDecimal("0.99"), new BigDecimal("3.00"));

        assertThatThrownBy(() -> scorer.score(values, 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
