package org.cotato.gongmozip.domains.matching.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
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

    // 레거시 신청 행은 NULL을 유지하고, 신규 API로 생성되는 행만 날짜 유일성 정책을 적용한다.
    @Column(name = "application_date")
    private LocalDate applicationDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MatchingApplicationStatus status = MatchingApplicationStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(name = "leader_preference", nullable = false, length = 30)
    private LeaderPreference leaderPreference;

    @Column(name = "first_matching", nullable = false)
    private boolean firstMatching;

    @Builder.Default
    @Column(name = "reassignment_priority", nullable = false)
    private boolean reassignmentPriority = false;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "contest_category", nullable = false, length = 50)
    private InterestCategory contestCategory;

    // 아래 구성요소 점수는 모두 가중치 적용 전 0~100 원점수다.
    @Column(name = "gpa_score", precision = 5, scale = 2)
    private BigDecimal gpaScore;

    @Column(name = "project_score", precision = 5, scale = 2)
    private BigDecimal projectScore;

    @Column(name = "award_score", precision = 5, scale = 2)
    private BigDecimal awardScore;

    @Column(name = "certification_score", precision = 5, scale = 2)
    private BigDecimal certificationScore;

    @Column(name = "collaboration_score", precision = 5, scale = 2)
    private BigDecimal collaborationScore;

    @Column(name = "skill_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal skillScore;

    // 신청 직후에는 null이며 14시 배치가 확정한 유효 풀 번호(1~4)를 저장한다.
    @Column(name = "skill_group")
    private Integer skillGroup;

    // 활동으로 변하는 회원 협업거리의 신청 시점 스냅샷이다.
    @Column(name = "collaboration_distance", nullable = false)
    private Integer collaborationDistance;

    // 매칭 신청 시점의 기존 협업 유형 검사 결과 스냅샷
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

    // 14시 전 철회 상태 전이 — 협업거리 감점 없음
    public void cancel(LocalDateTime canceledAt) {
        this.status = MatchingApplicationStatus.CANCELED;
        this.canceledAt = canceledAt;
    }

    // 14시 이후 철회 상태 전이 — 협업거리 감점은 CollaborationPointService에서 처리한다
    public void pass(LocalDateTime canceledAt) {
        this.status = MatchingApplicationStatus.PASSED;
        this.canceledAt = canceledAt;
    }
}
