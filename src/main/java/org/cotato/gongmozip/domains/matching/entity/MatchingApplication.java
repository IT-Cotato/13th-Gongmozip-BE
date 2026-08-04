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
import org.cotato.gongmozip.domains.matching.enums.MatchingReassignmentReason;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.global.entity.BaseEntity;

/**
 * 사용자의 매칭 신청과 신청 시점의 프로필·성향·역량 스냅샷을 보존하는 엔티티다.
 * 이번 배치 구현에서는 소속 배치와 유효 풀을 함께 기록하고, 준비부터 배정 결과까지의 상태 전이를 엔티티가 직접 보호하도록
 * 확장했다.
 */
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_batch_id")
    private MatchingBatch matchingBatch;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_application_id")
    private MatchingApplication sourceApplication;

    @Builder.Default
    @Column(name = "reassignment_count", nullable = false)
    private int reassignmentCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "reassignment_reason", length = 50)
    private MatchingReassignmentReason reassignmentReason;

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
        if (status == MatchingApplicationStatus.WAITING) {
            this.matchingBatch = null;
        }
        this.status = MatchingApplicationStatus.CANCELED;
        this.canceledAt = canceledAt;
    }

    // 14시 이후 철회 상태 전이 — 협업거리 감점은 CollaborationPointService에서 처리한다
    public void pass(LocalDateTime canceledAt) {
        if (status == MatchingApplicationStatus.WAITING || status == MatchingApplicationStatus.MATCHING) {
            // 계산 중 철회된 신청은 현재 배치 결과가 될 수 없으므로 재시도 입력에서 제외한다.
            this.matchingBatch = null;
        }
        this.status = MatchingApplicationStatus.PASSED;
        this.canceledAt = canceledAt;
    }

    // 분할이 끝난 신청을 하나의 배치와 유효 풀에 연결한다. 이 단계까지 상태는 WAITING을 유지한다.
    public void prepareForBatch(MatchingBatch batch, int poolOrdinal) {
        if (status != MatchingApplicationStatus.WAITING || matchingBatch != null) {
            throw new IllegalStateException("아직 배치가 준비되지 않은 대기 신청만 배치에 포함할 수 있습니다.");
        }
        if (poolOrdinal < 1 || poolOrdinal > 4) {
            throw new IllegalArgumentException("유효 풀 번호는 1에서 4 사이여야 합니다.");
        }
        this.matchingBatch = batch;
        this.skillGroup = poolOrdinal;
    }

    // 선점된 배치가 자신의 배치인지 확인한 뒤 알고리즘 계산 대상 상태로 전환한다.
    public void startMatching(MatchingBatch batch) {
        if (status != MatchingApplicationStatus.WAITING) {
            throw new IllegalStateException("대기 중인 신청만 매칭을 시작할 수 있습니다.");
        }
        if (matchingBatch == null || !matchingBatch.equals(batch)) {
            throw new IllegalStateException("신청이 해당 매칭 배치에 포함될 준비가 되지 않았습니다.");
        }
        this.status = MatchingApplicationStatus.MATCHING;
    }

    // 계산 결과 팀이 배정된 신청을 제안 대기 상태로 전환한다.
    public void propose(MatchingBatch batch) {
        validateProcessedBatch(batch);
        validateMatchingStatus();
        this.status = MatchingApplicationStatus.PROPOSED;
    }

    // 고정된 팀 크기 계획에서 배정되지 못한 신청을 최종 미매칭 상태로 전환한다.
    public void failToMatch(MatchingBatch batch) {
        validateProcessedBatch(batch);
        validateMatchingStatus();
        this.status = MatchingApplicationStatus.FAILED;
    }

    public void match() {
        if (status != MatchingApplicationStatus.PROPOSED) {
            throw new IllegalStateException("제안 상태의 신청만 최종 매칭할 수 있습니다.");
        }
        this.status = MatchingApplicationStatus.MATCHED;
    }

    public void waitForReassignment() {
        if (status != MatchingApplicationStatus.PROPOSED) {
            throw new IllegalStateException("제안 상태의 피해 신청만 재배정 대기로 전환할 수 있습니다.");
        }
        this.status = MatchingApplicationStatus.REASSIGN_PENDING;
    }

    public MatchingApplication createReassignment(
            LocalDate applicationDate, MatchingReassignmentReason reassignmentReason) {
        if (applicationDate == null || reassignmentReason == null) {
            throw new IllegalArgumentException("재배정 날짜와 사유는 필수입니다.");
        }
        return MatchingApplication.builder()
                .member(member)
                .profile(profile)
                .applicationDate(applicationDate)
                .status(MatchingApplicationStatus.WAITING)
                .leaderPreference(leaderPreference)
                .firstMatching(firstMatching)
                .reassignmentPriority(true)
                .sourceApplication(this)
                .reassignmentCount(reassignmentCount + 1)
                .reassignmentReason(reassignmentReason)
                .contestCategory(contestCategory)
                .gpaScore(gpaScore)
                .projectScore(projectScore)
                .awardScore(awardScore)
                .certificationScore(certificationScore)
                .collaborationScore(collaborationScore)
                .skillScore(skillScore)
                .collaborationDistance(collaborationDistance)
                .agreeablenessScore(agreeablenessScore)
                .conscientiousnessScore(conscientiousnessScore)
                .honestyHumilityScore(honestyHumilityScore)
                .extroversionScore(extroversionScore)
                .goalPreferenceScore(goalPreferenceScore)
                .workStyleScore(workStyleScore)
                .communicationStyleScore(communicationStyleScore)
                .extroversion2Score(extroversion2Score)
                .extroversion3Score(extroversion3Score)
                .extroversionType(extroversionType)
                .characterType(characterType)
                .characterXScore(characterXScore)
                .characterYScore(characterYScore)
                .build();
    }

    // 배치 계산·저장 실패 시 처리 중이던 신청만 WAITING으로 되돌려 같은 배치에서 재시도할 수 있게 한다.
    public void restoreWaitingAfterBatchFailure(MatchingBatch batch) {
        validateProcessedBatch(batch);
        if (status == MatchingApplicationStatus.MATCHING) {
            this.status = MatchingApplicationStatus.WAITING;
        }
    }

    private void validateProcessedBatch(MatchingBatch batch) {
        // 다른 배치의 늦은 결과가 현재 신청 상태를 덮어쓰는 것을 막는다.
        if (matchingBatch == null || batch == null || !matchingBatch.equals(batch)) {
            throw new IllegalStateException("신청이 해당 매칭 배치에 속하지 않습니다.");
        }
    }

    private void validateMatchingStatus() {
        // 철회 등으로 MATCHING을 벗어난 신청에는 계산 결과를 반영하지 않는다.
        if (status != MatchingApplicationStatus.MATCHING) {
            throw new IllegalStateException("매칭 계산 중인 신청에만 배치 결과를 반영할 수 있습니다.");
        }
    }
}
