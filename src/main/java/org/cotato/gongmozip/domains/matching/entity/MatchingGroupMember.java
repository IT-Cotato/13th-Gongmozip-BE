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
