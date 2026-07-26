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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.survey.enums.SubmissionStatus;
import org.cotato.gongmozip.domains.survey.vo.SurveyScoreSnapshot;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "survey_submissions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SurveySubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_submission_id", nullable = false, updatable = false)
    private Long surveySubmissionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    // 회원별 제출은 하나만 유지하며, 재검사 시 답변과 제출 시각을 갱신한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubmissionStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // HEXACO 우호성 점수 (3문항 평균)
    @Column(name = "agreeableness_score", precision = 5, scale = 2)
    private BigDecimal agreeablenessScore;

    // HEXACO 성실성 점수 (3문항 평균)
    @Column(name = "conscientiousness_score", precision = 5, scale = 2)
    private BigDecimal conscientiousnessScore;

    // HEXACO 정직-겸손성 점수 (3문항 평균)
    @Column(name = "honesty_humility_score", precision = 5, scale = 2)
    private BigDecimal honestyHumilityScore;

    // HEXACO 외향성 점수 (3문항 평균, 1.0~5.0)
    @Column(name = "extroversion_score", precision = 5, scale = 2)
    private BigDecimal extroversionScore;

    // 팀 성향: 프로젝트 목표 선호 (1·3·5점)
    @Column(name = "goal_preference_score", precision = 5, scale = 2)
    private BigDecimal goalPreferenceScore;

    // 팀 성향: 업무 방식 선호 (1·3·5점)
    @Column(name = "work_style_score", precision = 5, scale = 2)
    private BigDecimal workStyleScore;

    // 팀 성향: 소통 방식 선호 (1·3·5점)
    @Column(name = "communication_style_score", precision = 5, scale = 2)
    private BigDecimal communicationStyleScore;

    // 외향성 문항2 개별 점수 — 사회적 상황에서 먼저 다가가기 (정방향, 1~5)
    @Column(name = "extroversion_2_score", precision = 5, scale = 2)
    private BigDecimal extroversion2Score;

    // 외향성 문항3 개별 점수 — 단체 회의 의견 표현 (역채점 반영, 1~5)
    @Column(name = "extroversion_3_score", precision = 5, scale = 2)
    private BigDecimal extroversion3Score;

    // 외향성 유형 (I: <2.6 / A: 2.6~3.4 / E: >3.4)
    @Enumerated(EnumType.STRING)
    @Column(name = "extroversion_type", length = 1)
    private ExtroversionType extroversionType;

    // 캐릭터 유형 (X/Y 축 임계값 8점 기준)
    @Enumerated(EnumType.STRING)
    @Column(name = "character_type", length = 50)
    private CharacterType characterType;

    // X축: GOAL_PREFERENCE + WORK_STYLE + CONSCIENTIOUSNESS_1 (만점 15)
    @Column(name = "character_x_score", precision = 5, scale = 2)
    private BigDecimal characterXScore;

    // Y축: COMMUNICATION_STYLE + EXTROVERSION_2 + EXTROVERSION_3 (만점 15)
    @Column(name = "character_y_score", precision = 5, scale = 2)
    private BigDecimal characterYScore;

    public void submit() {
        this.status = SubmissionStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    public void recordScores(SurveyScoreSnapshot snapshot) {
        this.agreeablenessScore = snapshot.agreeablenessScore();
        this.conscientiousnessScore = snapshot.conscientiousnessScore();
        this.honestyHumilityScore = snapshot.honestyHumilityScore();
        this.extroversionScore = snapshot.extroversionScore();
        this.goalPreferenceScore = snapshot.goalPreferenceScore();
        this.workStyleScore = snapshot.workStyleScore();
        this.communicationStyleScore = snapshot.communicationStyleScore();
        this.extroversion2Score = snapshot.extroversion2Score();
        this.extroversion3Score = snapshot.extroversion3Score();
        this.extroversionType = snapshot.extroversionType();
        this.characterType = snapshot.characterType();
        this.characterXScore = snapshot.characterXScore();
        this.characterYScore = snapshot.characterYScore();
    }
}
