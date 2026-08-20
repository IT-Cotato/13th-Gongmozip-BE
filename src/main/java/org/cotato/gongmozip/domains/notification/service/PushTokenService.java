package org.cotato.gongmozip.domains.notification.service;

import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.notification.entity.PushToken;
import org.cotato.gongmozip.domains.notification.repository.PushTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FCM 등록 토큰의 등록/해제만 담당한다. 실제 발송은 {@link PushNotificationService}가 담당한다
 * (docs/decisions/13-fcm-push.md).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;
    private final MemberRepository memberRepository;

    /**
     * 토큰을 upsert한다. 같은 물리 기기에서 로그아웃 후 다른 계정으로 로그인하면 FCM이 같은 토큰을
     * 재사용하므로, 이미 존재하는 토큰이면 소유자만 갈아끼우고 새로 만들지 않는다.
     */
    @Transactional
    public void register(Long memberId, String token) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        pushTokenRepository
                .findByToken(token)
                .ifPresentOrElse(
                        existing -> existing.reassignTo(member),
                        () -> pushTokenRepository.save(
                                PushToken.builder().member(member).token(token).build()));
    }

    /** 로그아웃 시 호출 — 본인 소유 토큰만 해제한다. 이미 없는 토큰이어도(0건 삭제) 에러 없이 끝난다. */
    @Transactional
    public void unregister(Long memberId, String token) {
        pushTokenRepository.deleteByMember_MemberIdAndToken(memberId, token);
    }
}
