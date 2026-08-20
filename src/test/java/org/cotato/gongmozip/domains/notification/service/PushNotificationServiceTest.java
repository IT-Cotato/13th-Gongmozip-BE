package org.cotato.gongmozip.domains.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.global.push.FcmClient;
import org.cotato.gongmozip.global.push.PushPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * sendToMembers는 TransactionSynchronizationManager.registerSynchronization을 쓰기 때문에, 실제
 * 트랜잭션 없이 테스트하려면 동기화를 직접 초기화하고 afterCommit을 수동으로 트리거해야 한다
 * (ChatbotOrchestrationServiceTest와 동일한 패턴).
 */
@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private PushDispatchService pushDispatchService;

    @Mock
    private FcmClient fcmClient;

    @InjectMocks
    private PushNotificationService pushNotificationService;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @DisplayName("커밋 이후에만 실제 발송이 예약되고, 커밋 전에는 발송되지 않는다.")
    @Test
    void sendToMembersDispatchesOnlyAfterCommit() {
        given(fcmClient.isEnabled()).willReturn(true);
        Member member = Member.builder().memberId(1L).build();
        PushPayload payload = new PushPayload("제목", "본문", Map.of());

        pushNotificationService.sendToMembers(List.of(member), payload);

        verify(pushDispatchService, never()).dispatch(List.of(1L), payload);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(pushDispatchService).dispatch(List.of(1L), payload);
    }

    @DisplayName("FCM이 비활성화 상태면 발송을 예약하지 않는다.")
    @Test
    void sendToMembersSkipsWhenFcmDisabled() {
        given(fcmClient.isEnabled()).willReturn(false);
        Member member = Member.builder().memberId(1L).build();

        pushNotificationService.sendToMembers(List.of(member), new PushPayload("제목", "본문", Map.of()));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(pushDispatchService, never()).dispatch(any(), any());
    }

    @DisplayName("수신자가 없으면 발송을 예약하지 않는다.")
    @Test
    void sendToMembersSkipsWhenNoReceivers() {
        pushNotificationService.sendToMembers(List.of(), new PushPayload("제목", "본문", Map.of()));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(pushDispatchService, never()).dispatch(any(), any());
    }
}
