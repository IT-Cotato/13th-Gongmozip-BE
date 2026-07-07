package org.cotato.gongmozip.domains.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.LoginRequest;
import org.cotato.gongmozip.domains.auth.exception.AuthException;
import org.cotato.gongmozip.domains.auth.exception.codes.AuthErrorCode;
import org.cotato.gongmozip.domains.auth.dto.response.AuthResponse.LoginResult;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.cotato.gongmozip.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private AuthService authService;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_EMAIL = "test@gongmozip.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final long REFRESH_TOKEN_EXPIRATION = 1209600000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
    }

    private Member createMember() {
        return Member.builder()
                .memberId(TEST_MEMBER_ID)
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    // ========== 로그인 메서드 테스트 ==========

    @DisplayName("존재하지 않는 이메일로 로그인하면 회원 없음 예외가 발생한다.")
    @Test
    void 존재하지_않는_이메일로_로그인하면_회원_없음_예외가_발생한다() {
        // given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(MemberException.class)
                .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @DisplayName("비밀번호가 틀리면 비밀번호 불일치 예외가 발생한다.")
    @Test
    void 비밀번호가_틀리면_비밀번호_불일치_예외가_발생한다() {
        // given
        LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongPassword");
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.INVALID_PASSWORD.getMessage());
    }

    @DisplayName("로그인 성공 시 Access Token과 Refresh Token이 생성되어 반환된다.")
    @Test
    void 로그인_성공_시_AccessToken과_RefreshToken이_생성되어_반환된다() {
        // given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(jwtProvider.generateAccessToken(TEST_MEMBER_ID, TEST_EMAIL)).willReturn(ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(TEST_MEMBER_ID, TEST_EMAIL)).willReturn(REFRESH_TOKEN);

        // when
        LoginResult result = authService.login(request);

        // then
        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    @DisplayName("로그인 성공 시 Refresh Token이 Redis에 저장된다.")
    @Test
    void 로그인_성공_시_RefreshToken이_Redis에_저장된다() {
        // given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(jwtProvider.generateAccessToken(TEST_MEMBER_ID, TEST_EMAIL)).willReturn(ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(TEST_MEMBER_ID, TEST_EMAIL)).willReturn(REFRESH_TOKEN);

        // when
        authService.login(request);

        // then
        then(redisUtil).should().set(
                eq("refresh:" + TEST_MEMBER_ID),
                eq(REFRESH_TOKEN),
                eq(REFRESH_TOKEN_EXPIRATION),
                eq(TimeUnit.MILLISECONDS)
        );
    }
}
