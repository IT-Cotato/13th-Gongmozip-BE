package org.cotato.gongmozip.domains.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "matching_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchingApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_application_id", nullable = false, updatable = false)
    private Long matchingApplicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    // 매칭 신청 시 입력받는 공모전 관심 분야
    @Enumerated(EnumType.STRING)
    @Column(name = "contest_category", nullable = false, length = 50)
    private InterestCategory contestCategory;

    // 매칭 신청 시 계산되는 역량 점수
    @Column(name = "skill_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal skillScore;

    // 매칭 신청 시 계산되는 역량 그룹
    @Column(name = "skill_group", nullable = false)
    private Integer skillGroup;

    // 매칭 신청 시 입력받는 협업 거리
    @Column(name = "collaboration_distance", nullable = false)
    private Integer collaborationDistance;

    // 매칭 신청 시점의 설문 결과 스냅샷
    @Column(name = "agreeableness_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal agreeablenessScore;

    @Column(name = "conscientiousness_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal conscientiousnessScore;

    @Column(name = "honesty_humility_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal honestyHumilityScore;

    @Column(name = "extroversion_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal extroversionScore;

    @Column(name = "goal_preference_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal goalPreferenceScore;

    @Column(name = "work_style_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal workStyleScore;

    @Column(name = "communication_style_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal communicationStyleScore;

    @Column(name = "extroversion_2_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal extroversion2Score;

    @Column(name = "extroversion_3_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal extroversion3Score;

    @Enumerated(EnumType.STRING)
    @Column(name = "extroversion_type", nullable = false, length = 1)
    private ExtroversionType extroversionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "character_type", nullable = false, length = 50)
    private CharacterType characterType;

    @Column(name = "character_x_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal characterXScore;

    @Column(name = "character_y_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal characterYScore;

    public static MatchingApplication snapshotOf(
            SurveySubmission submission,
            Profile profile,
            InterestCategory contestCategory,
            BigDecimal skillScore,
            Integer skillGroup,
            Integer collaborationDistance) {
        return MatchingApplication.builder()
                .member(submission.getMember())
                .profile(profile)
                .contestCategory(contestCategory)
                .skillScore(skillScore)
                .skillGroup(skillGroup)
                .collaborationDistance(collaborationDistance)
                .agreeablenessScore(submission.getAgreeablenessScore())
                .conscientiousnessScore(submission.getConscientiousnessScore())
                .honestyHumilityScore(submission.getHonestyHumilityScore())
                .extroversionScore(submission.getExtroversionScore())
                .goalPreferenceScore(submission.getGoalPreferenceScore())
                .workStyleScore(submission.getWorkStyleScore())
                .communicationStyleScore(submission.getCommunicationStyleScore())
                .extroversion2Score(submission.getExtroversion2Score())
                .extroversion3Score(submission.getExtroversion3Score())
                .extroversionType(submission.getExtroversionType())
                .characterType(submission.getCharacterType())
                .characterXScore(submission.getCharacterXScore())
                .characterYScore(submission.getCharacterYScore())
                .build();
    }
}
