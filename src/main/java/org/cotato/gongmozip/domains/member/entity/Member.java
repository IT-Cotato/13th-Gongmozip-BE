package org.cotato.gongmozip.domains.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import org.cotato.gongmozip.domains.member.enums.Gender;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MemberStatus status;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private MemberRole role = MemberRole.USER;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    // 프로젝트 진행 중 이벤트로 적립/차감되는 현재 협업거리. 매칭 신청 시 이 값을
    // MatchingApplication.collaborationDistance에 스냅샷으로 저장한다.
    // docs/decisions/06-collaboration-point.md 참고.
    public static final int INITIAL_COLLABORATION_POINT = 100;
    public static final int MAX_COLLABORATION_POINT = 500;

    @Builder.Default
    @Column(name = "collaboration_point", nullable = false)
    private int collaborationPoint = INITIAL_COLLABORATION_POINT;

    @Column(name = "matching_blocked_until")
    private LocalDateTime matchingBlockedUntil;

    public void registerRequiredInfo(Gender gender, LocalDate birthDate) {
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void addCollaborationPoint(int delta) {
        this.collaborationPoint = Math.max(0, Math.min(MAX_COLLABORATION_POINT, this.collaborationPoint + delta));
    }

    public boolean isMatchingBlockedAt(LocalDateTime dateTime) {
        return matchingBlockedUntil != null && matchingBlockedUntil.isAfter(dateTime);
    }

    public void blockMatchingUntil(LocalDateTime blockedUntil) {
        if (this.matchingBlockedUntil == null || this.matchingBlockedUntil.isBefore(blockedUntil)) {
            this.matchingBlockedUntil = blockedUntil;
        }
    }
}
