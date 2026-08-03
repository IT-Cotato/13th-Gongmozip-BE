package org.cotato.gongmozip.domains.matching.score;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.vo.SkillScoreSnapshot;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.springframework.stereotype.Component;

/**
 * 프로필의 서로 다른 역량 지표를 공통 0~100 척도로 바꾸고 신청자 유형에 맞는 가중 총점을 계산하기 위해 만들었다.
 * 역량 기반 풀 분류는 14시 배치가 전체 신청자 분포를 보고 수행하므로, 이 계산기는 신청 시점의 개인 점수만 확정한다.
 */
@Component
public class SkillScoreCalculator {

    // 각 역량 항목은 먼저 0~100 원점수로 통일한 뒤 아래 가중치를 곱한다
    private static final int SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal GPA_WEIGHT = new BigDecimal("0.20");
    private static final BigDecimal PROJECT_WEIGHT_EXISTING = new BigDecimal("0.40");
    private static final BigDecimal PROJECT_WEIGHT_FIRST = new BigDecimal("0.50");
    private static final BigDecimal AWARD_WEIGHT = new BigDecimal("0.10");
    private static final BigDecimal CERTIFICATION_WEIGHT = new BigDecimal("0.10");
    private static final BigDecimal COLLABORATION_WEIGHT_EXISTING = new BigDecimal("0.20");
    private static final BigDecimal COLLABORATION_WEIGHT_FIRST = new BigDecimal("0.10");

    // 원점수 변환과 사용자 유형별 가중치 적용 결과를 하나의 신청 시점 스냅샷으로 반환한다.
    public SkillScoreSnapshot calculate(
            Profile profile,
            BigDecimal projectScore,
            int awardCount,
            int certificationCount,
            int collaborationDistance,
            boolean firstMatching) {
        // 학점·프로젝트·수상·자격증·협업거리를 모두 0~100 범위로 변환한다
        BigDecimal gpaScore = calculateGpaScore(profile.getGpa(), profile.getGpaScale());
        BigDecimal normalizedProjectScore = normalizeScore(projectScore);
        BigDecimal awardScore = calculateCountScore(awardCount);
        BigDecimal certificationScore = calculateCountScore(certificationCount);
        BigDecimal collaborationScore = BigDecimal.valueOf(
                        Math.min(Math.max(collaborationDistance, 0), Member.MAX_COLLABORATION_POINT))
                .divide(BigDecimal.valueOf(Member.MAX_COLLABORATION_POINT), 6, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // 첫 매칭은 협업 이력이 없으므로 프로젝트 비중을 10% 높이고 협업거리 비중을 낮춘다
        BigDecimal projectWeight = firstMatching ? PROJECT_WEIGHT_FIRST : PROJECT_WEIGHT_EXISTING;
        BigDecimal collaborationWeight = firstMatching ? COLLABORATION_WEIGHT_FIRST : COLLABORATION_WEIGHT_EXISTING;

        BigDecimal total = gpaScore.multiply(GPA_WEIGHT)
                .add(normalizedProjectScore.multiply(projectWeight))
                .add(awardScore.multiply(AWARD_WEIGHT))
                .add(certificationScore.multiply(CERTIFICATION_WEIGHT))
                .add(collaborationScore.multiply(collaborationWeight))
                .setScale(SCALE, RoundingMode.HALF_UP);

        return new SkillScoreSnapshot(
                gpaScore,
                normalizedProjectScore,
                awardScore,
                certificationScore,
                collaborationScore,
                total,
                firstMatching);
    }

    // 학점 원점수: 현재 학점 / 학점 만점 × 100
    private BigDecimal calculateGpaScore(Double gpa, Double gpaScale) {
        if (gpa == null
                || gpaScale == null
                || !Double.isFinite(gpa)
                || !Double.isFinite(gpaScale)
                || gpaScale <= 0
                || gpa < 0
                || gpa > gpaScale) {
            throw new MatchingException(MatchingErrorCode.INVALID_PROFILE_GPA);
        }
        return BigDecimal.valueOf(gpa)
                .divide(BigDecimal.valueOf(gpaScale), 6, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    // 수상·자격증 개수 점수: 0개=0, 1개=50, 이후 10점씩 증가, 6개 이상=100
    private BigDecimal calculateCountScore(int count) {
        if (count <= 0) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        if (count >= 6) {
            return ONE_HUNDRED.setScale(SCALE);
        }
        return BigDecimal.valueOf(40L + count * 10L).setScale(SCALE);
    }

    // 외부 프로젝트 평가값이 범위를 벗어나도 최종 계산에는 0~100만 사용한다
    private BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        return score.max(BigDecimal.ZERO).min(ONE_HUNDRED).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
