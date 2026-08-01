package org.cotato.gongmozip.domains.matching.score;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.cotato.gongmozip.domains.matching.vo.SkillScoreSnapshot;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SkillScoreCalculatorTest {

    private final SkillScoreCalculator calculator = new SkillScoreCalculator();

    @DisplayName("첫 매칭 회원은 프로젝트 50%, 협업거리 10% 가중치로 계산한다.")
    @Test
    void 첫_매칭_가중치를_적용한다() {
        Profile profile = Profile.builder().gpa(4.0).gpaScale(4.0).build();

        SkillScoreSnapshot result = calculator.calculate(profile, new BigDecimal("50"), 1, 0, 100, true);

        assertThat(result.gpaScore()).isEqualByComparingTo("100.00");
        assertThat(result.awardScore()).isEqualByComparingTo("50.00");
        assertThat(result.collaborationScore()).isEqualByComparingTo("20.00");
        assertThat(result.totalScore()).isEqualByComparingTo("52.00");
        assertThat(result.skillGroup()).isEqualTo(2);
    }

    @DisplayName("기존 회원은 프로젝트 40%, 협업거리 20% 가중치로 계산한다.")
    @Test
    void 기존_회원_가중치를_적용한다() {
        Profile profile = Profile.builder().gpa(4.0).gpaScale(4.0).build();

        SkillScoreSnapshot result = calculator.calculate(profile, new BigDecimal("50"), 1, 0, 100, false);

        assertThat(result.totalScore()).isEqualByComparingTo("49.00");
        assertThat(result.skillGroup()).isEqualTo(2);
    }

    @DisplayName("40, 60, 80점은 각각 상위 그룹의 시작점이다.")
    @Test
    void 그룹_경계값은_상위_그룹에_포함된다() {
        Profile zeroGpa = Profile.builder().gpa(0.0).gpaScale(4.5).build();
        Profile fullGpa = Profile.builder().gpa(4.5).gpaScale(4.5).build();

        assertThat(calculator
                        .calculate(zeroGpa, new BigDecimal("100"), 0, 0, 0, false)
                        .skillGroup())
                .isEqualTo(2); // 프로젝트 40점
        assertThat(calculator
                        .calculate(fullGpa, new BigDecimal("100"), 0, 0, 0, false)
                        .skillGroup())
                .isEqualTo(3); // 학점 20 + 프로젝트 40 = 60점
        assertThat(calculator
                        .calculate(fullGpa, new BigDecimal("100"), 6, 0, 500, false)
                        .skillGroup())
                .isEqualTo(4); // 90점
    }

    @DisplayName("수상과 자격증은 0개면 0점, 6개 이상이면 원점수 100점으로 제한한다.")
    @Test
    void 개수_점수는_0에서_100_사이로_제한된다() {
        Profile profile = Profile.builder().gpa(0.0).gpaScale(4.5).build();

        SkillScoreSnapshot result = calculator.calculate(profile, BigDecimal.ZERO, 6, 10, 0, false);

        assertThat(result.awardScore()).isEqualByComparingTo("100.00");
        assertThat(result.certificationScore()).isEqualByComparingTo("100.00");
        assertThat(result.totalScore()).isEqualByComparingTo("20.00");
    }
}
