package org.cotato.gongmozip.domains.matching.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.vo.SkillScoreSnapshot;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SkillScoreCalculatorTest {

    private final SkillScoreCalculator calculator = new SkillScoreCalculator();

    @Test
    @DisplayName("첫 매칭 사용자는 프로젝트 50%, 협업거리 10% 가중치로 계산한다")
    void calculateFirstMatchingScore() {
        Profile profile = Profile.builder().gpa(4.0).gpaScale(4.0).build();
        SkillScoreSnapshot result = calculator.calculate(profile, new BigDecimal("50"), 1, 0, 100, true);

        assertThat(result.gpaScore()).isEqualByComparingTo("100.00");
        assertThat(result.awardScore()).isEqualByComparingTo("50.00");
        assertThat(result.collaborationScore()).isEqualByComparingTo("20.00");
        assertThat(result.totalScore()).isEqualByComparingTo("52.00");
        assertThat(result.firstMatching()).isTrue();
    }

    @Test
    @DisplayName("기존 사용자는 프로젝트 40%, 협업거리 20% 가중치로 계산한다")
    void calculateExistingMemberScore() {
        Profile profile = Profile.builder().gpa(4.0).gpaScale(4.0).build();
        SkillScoreSnapshot result = calculator.calculate(profile, new BigDecimal("50"), 1, 0, 100, false);

        assertThat(result.totalScore()).isEqualByComparingTo("49.00");
        assertThat(result.firstMatching()).isFalse();
    }

    @Test
    @DisplayName("역량 계산 결과에는 점수만 담고 그룹은 14시 배치에서 결정한다")
    void scoreSnapshotDoesNotDecideGroup() {
        Profile profile = Profile.builder().gpa(4.5).gpaScale(4.5).build();
        SkillScoreSnapshot result = calculator.calculate(profile, new BigDecimal("100"), 6, 6, 500, false);

        assertThat(result.totalScore()).isEqualByComparingTo("100.00");
        assertThat(SkillScoreSnapshot.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("skillGroup");
    }

    @Test
    @DisplayName("수상과 자격증 개수 점수는 0점에서 100점 사이로 제한한다")
    void clampCountScores() {
        Profile profile = Profile.builder().gpa(0.0).gpaScale(4.5).build();
        SkillScoreSnapshot result = calculator.calculate(profile, BigDecimal.ZERO, 6, 10, 0, false);

        assertThat(result.awardScore()).isEqualByComparingTo("100.00");
        assertThat(result.certificationScore()).isEqualByComparingTo("100.00");
        assertThat(result.totalScore()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("잘못된 학점 정보는 매칭 도메인 예외로 변환한다")
    void rejectInvalidGpa() {
        List<Profile> invalidProfiles = List.of(
                Profile.builder().gpa(null).gpaScale(4.5).build(),
                Profile.builder().gpa(4.0).gpaScale(0.0).build(),
                Profile.builder().gpa(4.6).gpaScale(4.5).build(),
                Profile.builder().gpa(Double.NaN).gpaScale(4.5).build());

        for (Profile profile : invalidProfiles) {
            assertThatThrownBy(() -> calculator.calculate(profile, BigDecimal.ZERO, 0, 0, 0, false))
                    .isInstanceOf(MatchingException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.INVALID_PROFILE_GPA);
        }
    }
}
