package org.cotato.gongmozip.domains.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.global.entity.BaseEntity;

/**
 * 회원의 FCM 등록 토큰 한 건(docs/decisions/13-fcm-push.md). 회원 1명이 여러 기기(폰/데스크톱 브라우저 등)를
 * 쓰면 토큰도 여러 개일 수 있어 회원 기준 1:N이다. {@code token} 자체는 유니크하다 — 같은 물리 기기에서
 * 로그아웃 후 다른 계정으로 로그인하면 토큰이 재사용되므로, 등록은 항상 upsert(이미 있는 토큰이면
 * {@link #reassignTo}로 소유자만 갈아끼움)로 처리한다.
 */
@Getter
@Entity
@Table(name = "push_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PushToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_token_id", nullable = false, updatable = false)
    private Long pushTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    public void reassignTo(Member member) {
        this.member = member;
    }
}
