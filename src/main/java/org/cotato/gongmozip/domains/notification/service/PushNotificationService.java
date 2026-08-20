package org.cotato.gongmozip.domains.notification.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.global.push.FcmClient;
import org.cotato.gongmozip.global.push.PushPayload;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 알림함 저장(NotificationService)과는 별개로, "누구에게 무엇을 푸시로 보낼지"만 책임진다
 * (docs/decisions/13-fcm-push.md). 알림함에 안 쌓이는 일반 채팅 메시지도 이 서비스를 통해 푸시는 나간다.
 *
 * <p>호출 시점의 트랜잭션이 커밋된 뒤에만 실제 발송을 예약한다 — 커밋 전에 보내면, 이후 같은
 * 트랜잭션에서 실패해 롤백되더라도 이미 사용자 폰에 뜬 알림은 되돌릴 수 없다(VARCHAR(500) 트랜잭션
 * 롤백 버그와 같은 종류의 문제). 반드시 {@code @Transactional} 메서드 안에서 호출해야 한다 — 활성
 * 트랜잭션이 없으면 {@link TransactionSynchronizationManager#registerSynchronization}이 예외를 던진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final PushDispatchService pushDispatchService;
    private final FcmClient fcmClient;

    public void sendToMembers(List<Member> receivers, PushPayload payload) {
        if (receivers.isEmpty() || !fcmClient.isEnabled()) {
            return;
        }
        List<Long> memberIds = receivers.stream().map(Member::getMemberId).toList();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    pushDispatchService.dispatch(memberIds, payload);
                } catch (TaskRejectedException e) {
                    log.error("푸시 발송 비동기 작업 제출 실패 - memberIds: {}", memberIds, e);
                }
            }
        });
    }
}
