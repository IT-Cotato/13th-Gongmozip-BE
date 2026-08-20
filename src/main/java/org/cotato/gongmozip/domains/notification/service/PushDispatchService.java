package org.cotato.gongmozip.domains.notification.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.notification.entity.PushToken;
import org.cotato.gongmozip.domains.notification.repository.PushTokenRepository;
import org.cotato.gongmozip.global.push.FcmClient;
import org.cotato.gongmozip.global.push.PushPayload;
import org.cotato.gongmozip.global.push.PushSendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link PushNotificationService}의 afterCommit 콜백에서 호출되는 실제 발송 실행부다. {@code @Async}가
 * 같은 클래스 안에서의 self-invocation으로는 동작하지 않아(Spring AOP 프록시를 안 거침) 별도 빈으로
 * 분리했다 — 자세한 배경은 docs/decisions/13-fcm-push.md 참고.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushDispatchService {

    private final PushTokenRepository pushTokenRepository;
    private final FcmClient fcmClient;

    @Async("pushExecutor")
    public void dispatch(List<Long> memberIds, PushPayload payload) {
        List<PushToken> tokens = pushTokenRepository.findAllByMember_MemberIdIn(memberIds);
        for (PushToken pushToken : tokens) {
            PushSendResult result = fcmClient.send(pushToken.getToken(), payload);
            if (result == PushSendResult.INVALID_TOKEN) {
                removeInvalidToken(pushToken.getPushTokenId());
            } else if (result == PushSendResult.FAILURE) {
                log.warn("FCM 발송 실패 - pushTokenId: {}", pushToken.getPushTokenId());
            }
        }
    }

    // 무효 토큰(앱 삭제/알림 권한 철회 등)은 재시도 대상이 아니므로 바로 정리한다.
    @Transactional
    public void removeInvalidToken(Long pushTokenId) {
        pushTokenRepository.deleteById(pushTokenId);
    }
}
