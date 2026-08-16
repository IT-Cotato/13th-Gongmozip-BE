package org.cotato.gongmozip.domains.matching.score;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 1~5 척도 응답의 분산을 정규화해 팀 유사도 점수를 계산하는 공용 계산기다.
 * 완성 팀(3·4인) 점수 계산기와 미완성 팀(2·3인) 후보 순위 계산기가 동일한 공식을 공유하도록
 * 별도 컴포넌트로 분리했다.
 */
@Component
public class SimilarityScorer {

    private static final BigDecimal SCALE_MIN = BigDecimal.ONE;
    private static final BigDecimal SCALE_MAX = new BigDecimal("5");
    private static final BigDecimal MAX_VARIANCE = new BigDecimal("4");
    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    public BigDecimal score(List<BigDecimal> values, int maximumScore) {
        if (values == null || values.size() < 2) {
            throw new IllegalArgumentException("유사도 계산은 두 명 이상의 응답이 필요합니다.");
        }
        values.forEach(this::validateLikertScore);

        // 1~5 척도의 최대 분산 4를 기준으로 실제 분산을 정규화해, 응답이 가까울수록 높은 점수를 준다.
        BigDecimal mean = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), MATH_CONTEXT);
        BigDecimal variance = values.stream()
                .map(value -> value.subtract(mean, MATH_CONTEXT))
                .map(value -> value.multiply(value, MATH_CONTEXT))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), MATH_CONTEXT);
        BigDecimal normalized = variance.divide(MAX_VARIANCE, MATH_CONTEXT).min(BigDecimal.ONE);
        BigDecimal score = BigDecimal.ONE
                .subtract(normalized, MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(maximumScore), MATH_CONTEXT);
        return score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(maximumScore)).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateLikertScore(BigDecimal value) {
        if (value == null || value.compareTo(SCALE_MIN) < 0 || value.compareTo(SCALE_MAX) > 0) {
            throw new IllegalArgumentException("유사도 계산값은 1에서 5 사이여야 합니다.");
        }
    }
}
