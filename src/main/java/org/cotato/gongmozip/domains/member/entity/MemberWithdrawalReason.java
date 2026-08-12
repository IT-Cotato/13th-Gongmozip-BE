package org.cotato.gongmozip.domains.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.member.enums.WithdrawalReasonType;
import org.cotato.gongmozip.global.entity.BaseEntity;

/**
 * 탈퇴 사유 통계용 엔티티. 회원 개인정보는 익명화 배치로 파기되지만 사유 통계는 영구 보존해야 하므로
 * 의도적으로 member FK를 두지 않는다.
 */
@Getter
@Entity
@Table(name = "member_withdrawal_reasons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberWithdrawalReason extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_reason_id", nullable = false, updatable = false)
    private Long withdrawalReasonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private WithdrawalReasonType reason;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;
}
