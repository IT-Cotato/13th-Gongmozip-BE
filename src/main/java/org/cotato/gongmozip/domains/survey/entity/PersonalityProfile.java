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
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "personality_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PersonalityProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id", nullable = false, updatable = false)
    private Long profileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 매칭 신청 시 입력받는 값 (설문 제출 시점에는 null)
    @Enumerated(EnumType.STRING)
    @Column(name = "contest_category", length = 50)
    private InterestCategory contestCategory;

    // 매칭 신청 시 계산되는 역량 점수 (설문 제출 시점에는 null)
    @Column(name = "skill_score", precision = 5, scale = 2)
    private BigDecimal skillScore;

    // 매칭 신청 시 계산되는 역량 그룹 (설문 제출 시점에는 null)
    @Column(name = "skill_group")
    private Integer skillGroup;

    // 매칭 신청 시 입력받는 협업 거리 (설문 제출 시점에는 null)
    @Column(name = "collaboration_distance")
    private Integer collaborationDistance;

    // HEXACO 우호성 점수 (3문항 평균)
    @Column(name = "agreeableness_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal agreeablenessScore;

    // HEXACO 성실성 점수 (3문항 평균)
    @Column(name = "conscientiousness_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal conscientiousnessScore;

    // HEXACO 정직-겸손성 점수 (3문항 평균)
    @Column(name = "honesty_humility_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal honestyHumilityScore;

    // HEXACO 외향성 점수 (3문항 평균, 1.0~5.0)
    @Column(name = "extroversion_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal extroversionScore;

    // 팀 성향: 프로젝트 목표 선호 (1·3·5점)
    @Column(name = "goal_preference_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal goalPreferenceScore;

    // 팀 성향: 업무 방식 선호 (1·3·5점)
    @Column(name = "work_style_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal workStyleScore;

    // 팀 성향: 소통 방식 선호 (1·3·5점)
    @Column(name = "communication_style_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal communicationStyleScore;

    // 외향성 문항2 개별 점수 — 사회적 상황에서 먼저 다가가기 (정방향, 1~5)
    @Column(name = "extroversion_2_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal extroversion2Score;

    // 외향성 문항3 개별 점수 — 단체 회의 의견 표현 (역채점 반영, 1~5)
    @Column(name = "extroversion_3_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal extroversion3Score;

    // 외향성 유형 (I: <2.6 / A: 2.6~3.4 / E: >3.4)
    @Enumerated(EnumType.STRING)
    @Column(name = "extroversion_type", nullable = false, length = 1)
    private ExtroversionType extroversionType;

    // 캐릭터 유형 (X/Y 축 임계값 8점 기준)
    @Enumerated(EnumType.STRING)
    @Column(name = "character_type", nullable = false, length = 50)
    private CharacterType characterType;

    // X축: GOAL_PREFERENCE + WORK_STYLE + CONSCIENTIOUSNESS_1 (만점 15)
    @Column(name = "character_x_score", precision = 5, scale = 2)
    private BigDecimal characterXScore;

    // Y축: COMMUNICATION_STYLE + EXTROVERSION_2 + EXTROVERSION_3 (만점 15)
    @Column(name = "character_y_score", precision = 5, scale = 2)
    private BigDecimal characterYScore;

    public void updateSurveyResult(PersonalityProfile result) {
        this.agreeablenessScore = result.agreeablenessScore;
        this.conscientiousnessScore = result.conscientiousnessScore;
        this.honestyHumilityScore = result.honestyHumilityScore;
        this.extroversionScore = result.extroversionScore;
        this.goalPreferenceScore = result.goalPreferenceScore;
        this.workStyleScore = result.workStyleScore;
        this.communicationStyleScore = result.communicationStyleScore;
        this.extroversion2Score = result.extroversion2Score;
        this.extroversion3Score = result.extroversion3Score;
        this.extroversionType = result.extroversionType;
        this.characterType = result.characterType;
        this.characterXScore = result.characterXScore;
        this.characterYScore = result.characterYScore;
    }
}
