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

    // 희망 공모전 분야 (IT_AI_TECH, MARKETING_AD_BRANDING 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "contest_category", nullable = false, length = 50)
    private InterestCategory contestCategory;

    // 역량 총점 (학점 20% + 프로젝트 40% + 수상경험 10% + 자격증 10% + 협업거리 20%)
    @Column(name = "skill_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal skillScore;

    // 역량 그룹 (1: 0~40점 / 2: 40~60점 / 3: 60~80점 / 4: 80~100점)
    @Column(name = "skill_group", nullable = false)
    private int skillGroup;

    // 협업 희망 거리 (단위: m, 최대 500m)
    @Builder.Default
    @Column(name = "collaboration_distance", nullable = false)
    private int collaborationDistance = 100;

    // HEXACO 우호성 점수 (문항 3개 평균)
    @Column(name = "agreeableness_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal agreeablenessScore;

    // HEXACO 성실성 점수 (문항 3개 평균)
    @Column(name = "conscientiousness_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal conscientiousnessScore;

    // HEXACO 정직-겸손성 점수 (문항 3개 평균)
    @Column(name = "honesty_humility_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal honestyHumilityScore;

    // HEXACO 외향성 점수 (문항 3개 평균, 1.0~5.0)
    @Column(name = "extroversion_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal extroversionScore;

    // 프로젝트 목표 선호 점수 (1~5점 척도)
    @Column(name = "goal_preference_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal goalPreferenceScore;

    // 업무 방식 선호 점수 (1~5점 척도)
    @Column(name = "work_style_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal workStyleScore;

    // 소통 방식 선호 점수 (1~5점 척도)
    @Column(name = "communication_style_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal communicationStyleScore;

    // 리더 희망 점수 (원합니다=1 / 상관없어요=0.5 / 원하지않아요=0)
    @Column(name = "leader_preference_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal leaderPreferenceScore;

    // 외향성 유형 (I: 1.0~2.6 / A: 2.6~3.4 / E: 3.4~5.0)
    @Enumerated(EnumType.STRING)
    @Column(name = "extroversion_type", nullable = false, length = 1)
    private ExtroversionType extroversionType;

    // 성향 캐릭터 유형
    @Column(name = "character_type", nullable = false, length = 50)
    private String characterType;
}
