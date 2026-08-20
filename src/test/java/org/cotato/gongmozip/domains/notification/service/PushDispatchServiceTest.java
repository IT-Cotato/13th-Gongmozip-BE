package org.cotato.gongmozip.domains.notification.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.notification.entity.PushToken;
import org.cotato.gongmozip.domains.notification.repository.PushTokenRepository;
import org.cotato.gongmozip.global.push.FcmClient;
import org.cotato.gongmozip.global.push.PushPayload;
import org.cotato.gongmozip.global.push.PushSendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushDispatchServiceTest {

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private FcmClient fcmClient;

    @InjectMocks
    private PushDispatchService pushDispatchService;

    @DisplayName("회원 목록에 등록된 모든 토큰으로 발송한다.")
    @Test
    void dispatchSendsToEveryTokenOfGivenMembers() {
        Member member = Member.builder().memberId(1L).build();
        PushToken tokenA = PushToken.builder().member(member).token("token-a").build();
        PushToken tokenB = PushToken.builder().member(member).token("token-b").build();
        PushPayload payload = new PushPayload("제목", "본문", Map.of());
        given(pushTokenRepository.findAllByMember_MemberIdIn(List.of(1L))).willReturn(List.of(tokenA, tokenB));
        given(fcmClient.send("token-a", payload)).willReturn(PushSendResult.SUCCESS);
        given(fcmClient.send("token-b", payload)).willReturn(PushSendResult.SUCCESS);

        pushDispatchService.dispatch(List.of(1L), payload);

        verify(fcmClient).send("token-a", payload);
        verify(fcmClient).send("token-b", payload);
        verify(pushTokenRepository, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }

    @DisplayName("발송 결과가 INVALID_TOKEN이면 해당 토큰을 삭제한다.")
    @Test
    void dispatchRemovesInvalidToken() {
        Member member = Member.builder().memberId(1L).build();
        PushToken token = PushToken.builder().member(member).token("dead-token").build();
        org.springframework.test.util.ReflectionTestUtils.setField(token, "pushTokenId", 100L);
        PushPayload payload = new PushPayload("제목", "본문", Map.of());
        given(pushTokenRepository.findAllByMember_MemberIdIn(List.of(1L))).willReturn(List.of(token));
        given(fcmClient.send("dead-token", payload)).willReturn(PushSendResult.INVALID_TOKEN);

        pushDispatchService.dispatch(List.of(1L), payload);

        verify(pushTokenRepository).deleteById(100L);
    }
}
