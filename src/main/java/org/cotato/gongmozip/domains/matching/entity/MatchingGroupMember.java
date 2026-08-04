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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingResponseSource;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.global.entity.BaseEntity;

/**
 * 제안된 팀과 원본 매칭 신청을 일대일로 연결해 결과 구성원을 추적하기 위해 확장한 엔티티다.
 * 회원 참조도 함께 보존하되 저장 직전에 신청자와 회원이 같은지 검증해 잘못된 결과 연결을 차단한다.
 */
@Getter
@Entity
@Table(name = "matching_group_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchingGroupMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_group_member_id", nullable = false, updatable = false)
    private Long matchingGroupMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_group_id", nullable = false)
    private MatchingGroup matchingGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_application_id", unique = true)
    private MatchingApplication matchingApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", nullable = false, length = 30)
    private MatchingGroupMemberStatus responseStatus;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_source", length = 30)
    private MatchingResponseSource responseSource;

    /**
     * 이 그룹원 응답에 계산된 정책상 감점값(3/5/7/9/11)이다.
     *
     * <p>실제 협업거리 증감 원장은 {@code CollaborationPointHistory.delta}에 별도로 남는다. 이 값은
     * 동일한 패스 요청이 재시도됐을 때 최초 응답과 같은 감점값을 반환하면서 포인트를 다시 차감하지
     * 않기 위한 응답 단위 스냅샷이다.
     */
    @Column(name = "pass_penalty")
    private Integer passPenalty;

    public void accept(LocalDateTime respondedAt) {
        validatePending();
        this.responseStatus = MatchingGroupMemberStatus.ACCEPTED;
        this.respondedAt = respondedAt;
        this.responseSource = MatchingResponseSource.USER;
    }

    public void pass(LocalDateTime respondedAt, int passPenalty) {
        validatePending();
        if (passPenalty < 0) {
            throw new IllegalArgumentException("패스 감점은 양수로 기록해야 합니다.");
        }
        this.responseStatus = MatchingGroupMemberStatus.PASSED;
        this.respondedAt = respondedAt;
        this.responseSource = MatchingResponseSource.USER;
        this.passPenalty = passPenalty;
    }

    public void expire(LocalDateTime respondedAt, int passPenalty) {
        validatePending();
        if (passPenalty < 0) {
            throw new IllegalArgumentException("패스 감점은 양수로 기록해야 합니다.");
        }
        this.responseStatus = MatchingGroupMemberStatus.EXPIRED;
        this.respondedAt = respondedAt;
        this.responseSource = MatchingResponseSource.DEADLINE_JOB;
        this.passPenalty = passPenalty;
    }

    public boolean isActiveResponseTarget() {
        return responseStatus == MatchingGroupMemberStatus.PENDING
                || responseStatus == MatchingGroupMemberStatus.ACCEPTED;
    }

    private void validatePending() {
        if (responseStatus != MatchingGroupMemberStatus.PENDING) {
            throw new IllegalStateException("응답 대기 중인 그룹원만 응답 상태를 변경할 수 있습니다.");
        }
    }

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    private void validateApplicationMember() {
        // 알고리즘 결과 변환 과정에서 다른 회원의 신청이 연결되는 데이터 무결성 오류를 최종 방어한다.
        if (matchingApplication != null
                && member != null
                && !matchingApplication.getMember().getMemberId().equals(member.getMemberId())) {
            throw new IllegalStateException("매칭 신청자와 매칭 그룹 구성원은 같은 회원이어야 합니다.");
        }
    }
}
