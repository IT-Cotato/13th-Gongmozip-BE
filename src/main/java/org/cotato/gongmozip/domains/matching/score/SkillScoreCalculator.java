package org.cotato.gongmozip.domains.matching.score;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.cotato.gongmozip.domains.matching.vo.SkillScoreSnapshot;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.springframework.stereotype.Component;

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

    // 역량 점수 계산 — 원점수 변환 → 사용자 유형별 가중치 적용 → 4개 역량 그룹 판정
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
                resolveSkillGroup(total),
                firstMatching);
    }

    // 학점 원점수: 현재 학점 / 학점 만점 × 100
    private BigDecimal calculateGpaScore(Double gpa, Double gpaScale) {
        if (gpa == null || gpaScale == null || gpaScale <= 0 || gpa < 0 || gpa > gpaScale) {
            throw new IllegalArgumentException("Invalid GPA values");
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

    // 경계값 40·60·80점은 각각 상위 그룹에 포함한다
    private int resolveSkillGroup(BigDecimal total) {
        if (total.compareTo(new BigDecimal("40")) < 0) {
            return 1;
        }
        if (total.compareTo(new BigDecimal("60")) < 0) {
            return 2;
        }
        if (total.compareTo(new BigDecimal("80")) < 0) {
            return 3;
        }
        return 4;
    }
}
