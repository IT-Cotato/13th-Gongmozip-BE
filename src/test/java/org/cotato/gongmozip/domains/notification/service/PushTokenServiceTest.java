package org.cotato.gongmozip.domains.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.notification.entity.PushToken;
import org.cotato.gongmozip.domains.notification.repository.PushTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PushTokenService pushTokenService;

    @DisplayName("처음 등록하는 토큰이면 새 PushToken 행을 저장한다.")
    @Test
    void registerSavesNewTokenWhenNotExists() {
        Member member = Member.builder().memberId(1L).build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(pushTokenRepository.findByToken("token-a")).willReturn(Optional.empty());

        pushTokenService.register(1L, "token-a");

        verify(pushTokenRepository).save(any(PushToken.class));
    }

    @DisplayName("이미 존재하는 토큰이면 새로 만들지 않고 소유자만 갈아끼운다.")
    @Test
    void registerReassignsExistingToken() {
        Member oldOwner = Member.builder().memberId(1L).build();
        Member newOwner = Member.builder().memberId(2L).build();
        PushToken existing =
                PushToken.builder().member(oldOwner).token("token-a").build();
        given(memberRepository.findById(2L)).willReturn(Optional.of(newOwner));
        given(pushTokenRepository.findByToken("token-a")).willReturn(Optional.of(existing));

        pushTokenService.register(2L, "token-a");

        assertThat(existing.getMember()).isEqualTo(newOwner);
        verify(pushTokenRepository, never()).save(any(PushToken.class));
    }

    @DisplayName("존재하지 않는 회원이면 예외를 던진다.")
    @Test
    void registerRejectsUnknownMember() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pushTokenService.register(1L, "token-a"))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.MEMBER_NOT_FOUND);
    }

    @DisplayName("해제는 본인 소유 토큰만 삭제하도록 리포지토리에 위임한다.")
    @Test
    void unregisterDelegatesToRepository() {
        pushTokenService.unregister(1L, "token-a");

        verify(pushTokenRepository).deleteByMember_MemberIdAndToken(1L, "token-a");
    }
}
