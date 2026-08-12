package org.cotato.gongmozip.domains.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.LoginRequest;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.PasswordResetCodeRequest;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.PasswordResetCodeVerifyRequest;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.PasswordResetRequest;
import org.cotato.gongmozip.domains.auth.dto.response.AuthResponse.LoginResult;
import org.cotato.gongmozip.domains.auth.dto.response.AuthResponse.PasswordResetVerifyResponse;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;
import org.cotato.gongmozip.domains.auth.exception.AuthException;
import org.cotato.gongmozip.domains.auth.exception.codes.AuthErrorCode;
import org.cotato.gongmozip.domains.auth.repository.AuthAccountRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.cotato.gongmozip.global.security.jwt.JwtProvider;
import org.cotato.gongmozip.global.verification.EmailVerificationResult;
import org.cotato.gongmozip.global.verification.EmailVerificationService;
import org.cotato.gongmozip.global.verification.EmailVerificationService.Purpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AuthAccountRepository authAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_EMAIL = "test@gongmozip.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String NEW_ACCESS_TOKEN = "newAccessToken";
    private static final String NEW_REFRESH_TOKEN = "newRefreshToken";
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

    @DisplayName("비밀번호 실패가 5회 미만이면 비밀번호 불일치 예외가 발생한다.")
    @Test
    void 비밀번호_실패가_5회_미만이면_비밀번호_불일치_예외가_발생한다() {
        // given
        LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongPassword");
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).willReturn(false);
        given(redisUtil.increment("login:failure:" + TEST_MEMBER_ID)).willReturn(4L);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.INVALID_PASSWORD.getMessage());
        then(redisUtil).should().increment("login:failure:" + TEST_MEMBER_ID);
    }

    @DisplayName("비밀번호를 5회 이상 틀리면 로그인 제한 예외가 발생한다.")
    @Test
    void 비밀번호를_5회_이상_틀리면_로그인_제한_예외가_발생한다() {
        // given
        LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongPassword");
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).willReturn(false);
        given(redisUtil.increment("login:failure:" + TEST_MEMBER_ID)).willReturn(5L);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.LOGIN_LOCKED.getMessage());
    }

    @DisplayName("로그인이 제한된 계정은 올바른 비밀번호를 입력해도 로그인할 수 없다.")
    @Test
    void 로그인이_제한된_계정은_올바른_비밀번호를_입력해도_로그인할_수_없다() {
        // given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(redisUtil.get("login:failure:" + TEST_MEMBER_ID)).willReturn("5");

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.LOGIN_LOCKED.getMessage());
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
        then(redisUtil).should(never()).delete("login:failure:" + TEST_MEMBER_ID);
        then(jwtProvider).should(never()).generateAccessToken(anyLong(), anyString());
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
        then(redisUtil).should().delete("login:failure:" + TEST_MEMBER_ID);
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
        then(redisUtil)
                .should()
                .set(
                        eq("refresh:" + TEST_MEMBER_ID),
                        eq(REFRESH_TOKEN),
                        eq(REFRESH_TOKEN_EXPIRATION),
                        eq(TimeUnit.MILLISECONDS));
    }

    // ========== 로그아웃 메서드 테스트 ==========

    @DisplayName("로그아웃 시 유효한 Access Token은 블랙리스트에 등록된다.")
    @Test
    void 로그아웃_시_유효한_AccessToken은_블랙리스트에_등록된다() {
        // given
        long remainingMillis = 1800000L;
        given(jwtProvider.getRemainingExpirationMillis(ACCESS_TOKEN)).willReturn(remainingMillis);

        // when
        authService.logout(TEST_MEMBER_ID, ACCESS_TOKEN);

        // then
        then(redisUtil)
                .should()
                .set(
                        argThat(key -> key.startsWith("blacklist:")),
                        eq("logout"),
                        eq(remainingMillis),
                        eq(TimeUnit.MILLISECONDS));
    }

    @DisplayName("로그아웃 시 만료된 Access Token은 블랙리스트에 등록되지 않는다.")
    @Test
    void 로그아웃_시_만료된_AccessToken은_블랙리스트에_등록되지_않는다() {
        // given
        given(jwtProvider.getRemainingExpirationMillis(ACCESS_TOKEN)).willReturn(0L);

        // when
        authService.logout(TEST_MEMBER_ID, ACCESS_TOKEN);

        // then
        then(redisUtil)
                .should(never())
                .set(argThat(key -> key.startsWith("blacklist:")), eq("logout"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @DisplayName("로그아웃 시 Refresh Token이 Redis에서 삭제된다.")
    @Test
    void 로그아웃_시_RefreshToken이_Redis에서_삭제된다() {
        // given
        given(jwtProvider.getRemainingExpirationMillis(ACCESS_TOKEN)).willReturn(1800000L);

        // when
        authService.logout(TEST_MEMBER_ID, ACCESS_TOKEN);

        // then
        then(redisUtil).should().delete("refresh:" + TEST_MEMBER_ID);
    }

    // ========== 토큰 재발급 메서드 테스트 ==========

    @DisplayName("유효하지 않은 Refresh Token으로 재발급하면 예외가 발생한다.")
    @Test
    void 유효하지_않은_RefreshToken으로_재발급하면_예외가_발생한다() {
        // given
        given(jwtProvider.validateToken(REFRESH_TOKEN)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @DisplayName("Redis에 저장된 토큰과 불일치하면 예외가 발생한다.")
    @Test
    void Redis에_저장된_토큰과_불일치하면_예외가_발생한다() {
        // given
        given(jwtProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtProvider.getMemberId(REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
        given(redisUtil.get("refresh:" + TEST_MEMBER_ID)).willReturn("differentToken");

        // when & then
        assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @DisplayName("탈퇴한 회원이 재발급하면 탈퇴 회원 예외가 발생하고 Refresh Token이 삭제된다.")
    @Test
    void 탈퇴한_회원이_재발급하면_탈퇴_회원_예외가_발생하고_RefreshToken이_삭제된다() {
        // given
        Member member = createMember();
        member.withdraw(java.time.LocalDateTime.now());
        given(jwtProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtProvider.getMemberId(REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
        given(redisUtil.get("refresh:" + TEST_MEMBER_ID)).willReturn(REFRESH_TOKEN);
        given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                .isInstanceOf(MemberException.class)
                .hasMessage(MemberErrorCode.WITHDRAWN_MEMBER.getMessage());
        then(redisUtil).should().delete("refresh:" + TEST_MEMBER_ID);
        then(jwtProvider).should(never()).generateAccessToken(anyLong(), anyString());
    }

    @DisplayName("재발급 성공 시 새 Access Token과 Refresh Token이 반환된다.")
    @Test
    void 재발급_성공_시_새_AccessToken과_RefreshToken이_반환된다() {
        // given
        given(jwtProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtProvider.getMemberId(REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
        given(jwtProvider.getEmail(REFRESH_TOKEN)).willReturn(TEST_EMAIL);
        given(redisUtil.get("refresh:" + TEST_MEMBER_ID)).willReturn(REFRESH_TOKEN);
        given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(createMember()));
        given(jwtProvider.generateAccessToken(TEST_MEMBER_ID, TEST_EMAIL)).willReturn(NEW_ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(TEST_MEMBER_ID, TEST_EMAIL)).willReturn(NEW_REFRESH_TOKEN);

        // when
        LoginResult result = authService.reissue(REFRESH_TOKEN);

        // then
        assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
    }

    @DisplayName("재발급 성공 시 새 Refresh Token이 Redis에 저장된다.")
    @Test
    void 재발급_성공_시_새_RefreshToken이_Redis에_저장된다() {
        // given
        given(jwtProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtProvider.getMemberId(REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
        given(jwtProvider.getEmail(REFRESH_TOKEN)).willReturn(TEST_EMAIL);
        given(redisUtil.get("refresh:" + TEST_MEMBER_ID)).willReturn(REFRESH_TOKEN);
        given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(createMember()));
        given(jwtProvider.generateAccessToken(TEST_MEMBER_ID, TEST_EMAIL)).willReturn(NEW_ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(TEST_MEMBER_ID, TEST_EMAIL)).willReturn(NEW_REFRESH_TOKEN);

        // when
        authService.reissue(REFRESH_TOKEN);

        // then
        then(redisUtil)
                .should()
                .set(
                        eq("refresh:" + TEST_MEMBER_ID),
                        eq(NEW_REFRESH_TOKEN),
                        eq(REFRESH_TOKEN_EXPIRATION),
                        eq(TimeUnit.MILLISECONDS));
    }

    // ========== 비밀번호 재설정 인증코드 발송 테스트 ==========

    @DisplayName("가입되지 않은 이메일이면 계정 존재 여부를 노출하지 않는다.")
    @Test
    void 가입되지_않은_이메일이면_계정_존재_여부를_노출하지_않는다() {
        PasswordResetCodeRequest request = new PasswordResetCodeRequest(TEST_EMAIL);
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

        authService.sendPasswordResetCode(request);

        then(authAccountRepository).should(never()).existsByMemberAndProvider(any(), eq(AuthProvider.EMAIL));
        then(emailVerificationService).should(never()).sendCode(eq(Purpose.PASSWORD_RESET), anyString(), anyString());
    }

    @DisplayName("소셜 로그인 전용 계정이면 계정 유형을 노출하지 않는다.")
    @Test
    void 소셜_로그인_전용_계정이면_계정_유형을_노출하지_않는다() {
        PasswordResetCodeRequest request = new PasswordResetCodeRequest(TEST_EMAIL);
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL))
                .willReturn(false);

        authService.sendPasswordResetCode(request);

        then(emailVerificationService).should(never()).sendCode(eq(Purpose.PASSWORD_RESET), anyString(), anyString());
    }

    @DisplayName("이메일 로그인 계정이면 비밀번호 재설정 인증코드 전송을 요청한다.")
    @Test
    void 이메일_로그인_계정이면_비밀번호_재설정_인증코드_전송을_요청한다() {
        PasswordResetCodeRequest request = new PasswordResetCodeRequest(TEST_EMAIL);
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL))
                .willReturn(true);

        authService.sendPasswordResetCode(request);

        then(emailVerificationService).should().sendCode(Purpose.PASSWORD_RESET, TEST_EMAIL, "[공모집] 비밀번호 재설정 인증코드");
    }

    @DisplayName("인증코드 메일 전송에 실패하면 발급 내역을 삭제한다.")
    @Test
    void 인증코드_메일_전송에_실패하면_발급_내역을_삭제한다() {
        PasswordResetCodeRequest request = new PasswordResetCodeRequest(TEST_EMAIL);
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL))
                .willReturn(true);
        willThrow(new MailSendException("SMTP timeout"))
                .given(emailVerificationService)
                .sendCode(Purpose.PASSWORD_RESET, TEST_EMAIL, "[공모집] 비밀번호 재설정 인증코드");

        assertThatThrownBy(() -> authService.sendPasswordResetCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.PASSWORD_RESET_EMAIL_SEND_FAILED.getMessage());
    }

    // ========== 비밀번호 재설정 인증코드 확인 테스트 ==========

    @DisplayName("미가입 이메일의 인증코드를 확인하면 일반적인 코드 불일치 예외가 발생한다.")
    @Test
    void 미가입_이메일의_인증코드를_확인하면_코드_불일치_예외가_발생한다() {
        PasswordResetCodeVerifyRequest request = new PasswordResetCodeVerifyRequest(TEST_EMAIL, "123456");
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyPasswordResetCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.INVALID_PASSWORD_RESET_CODE.getMessage());
        then(emailVerificationService).should(never()).verifyCode(eq(Purpose.PASSWORD_RESET), anyString(), anyString());
    }

    @DisplayName("소셜 로그인 전용 계정의 인증코드를 확인하면 일반적인 코드 불일치 예외가 발생한다.")
    @Test
    void 소셜_로그인_전용_계정의_인증코드를_확인하면_코드_불일치_예외가_발생한다() {
        PasswordResetCodeVerifyRequest request = new PasswordResetCodeVerifyRequest(TEST_EMAIL, "123456");
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL))
                .willReturn(false);

        assertThatThrownBy(() -> authService.verifyPasswordResetCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.INVALID_PASSWORD_RESET_CODE.getMessage());
        then(emailVerificationService).should(never()).verifyCode(eq(Purpose.PASSWORD_RESET), anyString(), anyString());
    }

    @DisplayName("발급된 인증코드가 없으면 일반적인 코드 불일치 예외가 발생한다.")
    @Test
    void 발급된_인증코드가_없으면_코드_불일치_예외가_발생한다() {
        PasswordResetCodeVerifyRequest request = new PasswordResetCodeVerifyRequest(TEST_EMAIL, "123456");
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL))
                .willReturn(true);
        given(emailVerificationService.verifyCode(Purpose.PASSWORD_RESET, TEST_EMAIL, "123456"))
                .willReturn(EmailVerificationResult.CODE_NOT_ISSUED);

        assertThatThrownBy(() -> authService.verifyPasswordResetCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.INVALID_PASSWORD_RESET_CODE.getMessage());
    }

    @DisplayName("발급된 인증코드가 만료되면 코드 만료 예외가 발생한다.")
    @Test
    void 발급된_인증코드가_만료되면_코드_만료_예외가_발생한다() {
        PasswordResetCodeVerifyRequest request = new PasswordResetCodeVerifyRequest(TEST_EMAIL, "123456");
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL))
                .willReturn(true);
        given(emailVerificationService.verifyCode(Purpose.PASSWORD_RESET, TEST_EMAIL, "123456"))
                .willReturn(EmailVerificationResult.EXPIRED_CODE);

        assertThatThrownBy(() -> authService.verifyPasswordResetCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.EXPIRED_PASSWORD_RESET_CODE.getMessage());
    }

    @DisplayName("인증코드가 일치하지 않으면 실패 횟수를 증가시킨다.")
    @Test
    void 인증코드가_일치하지_않으면_실패_횟수를_증가시킨다() {
        PasswordResetCodeVerifyRequest request = new PasswordResetCodeVerifyRequest(TEST_EMAIL, "000000");
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL))
                .willReturn(true);
        given(emailVerificationService.verifyCode(Purpose.PASSWORD_RESET, TEST_EMAIL, "000000"))
                .willReturn(EmailVerificationResult.INVALID_CODE);

        assertThatThrownBy(() -> authService.verifyPasswordResetCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.INVALID_PASSWORD_RESET_CODE.getMessage());
    }

    @DisplayName("인증코드 확인 실패가 5회이면 시도 횟수 초과 예외가 발생한다.")
    @Test
    void 인증코드_확인_실패가_5회이면_시도_횟수_초과_예외가_발생한다() {
        PasswordResetCodeVerifyRequest request = new PasswordResetCodeVerifyRequest(TEST_EMAIL, "123456");
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL))
                .willReturn(true);
        given(emailVerificationService.verifyCode(Purpose.PASSWORD_RESET, TEST_EMAIL, "123456"))
                .willReturn(EmailVerificationResult.TOO_MANY_ATTEMPTS);

        assertThatThrownBy(() -> authService.verifyPasswordResetCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.TOO_MANY_PASSWORD_RESET_ATTEMPTS.getMessage());
    }

    @DisplayName("인증코드가 일치하면 비밀번호 변경용 일회용 토큰을 발급한다.")
    @Test
    void 인증코드가_일치하면_비밀번호_변경용_일회용_토큰을_발급한다() throws Exception {
        PasswordResetCodeVerifyRequest request = new PasswordResetCodeVerifyRequest(TEST_EMAIL, "123456");
        Member member = createMember();
        given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(member));
        given(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL))
                .willReturn(true);
        given(emailVerificationService.verifyCode(Purpose.PASSWORD_RESET, TEST_EMAIL, "123456"))
                .willReturn(EmailVerificationResult.VERIFIED);

        PasswordResetVerifyResponse response = authService.verifyPasswordResetCode(request);
        String tokenHash = sha256(response.passwordResetToken());

        assertThat(response.passwordResetToken()).isNotBlank();
        then(redisUtil)
                .should()
                .set("password-reset:token:" + tokenHash, TEST_MEMBER_ID.toString(), 10L, TimeUnit.MINUTES);
        then(redisUtil).should().set("password-reset:member:" + TEST_MEMBER_ID, tokenHash, 10L, TimeUnit.MINUTES);
    }

    // ========== 비밀번호 재설정 테스트 ==========

    @DisplayName("비밀번호 확인이 일치하지 않으면 토큰을 소비하지 않는다.")
    @Test
    void 비밀번호_확인이_일치하지_않으면_토큰을_소비하지_않는다() {
        PasswordResetRequest request = new PasswordResetRequest("resetToken", "newPassword123!", "different123!");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.PASSWORD_MISMATCH.getMessage());
        then(redisUtil).should(never()).getAndDelete(anyString());
    }

    @DisplayName("만료되었거나 이미 사용한 토큰이면 비밀번호를 재설정할 수 없다.")
    @Test
    void 만료되었거나_이미_사용한_토큰이면_비밀번호를_재설정할_수_없다() throws Exception {
        String token = "expiredToken";
        PasswordResetRequest request = new PasswordResetRequest(token, "newPassword123!", "newPassword123!");
        given(redisUtil.getAndDelete("password-reset:token:" + sha256(token))).willReturn(null);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.INVALID_PASSWORD_RESET_TOKEN.getMessage());
    }

    @DisplayName("유효한 일회용 토큰이면 비밀번호를 변경하고 Refresh Token을 폐기한다.")
    @Test
    void 유효한_일회용_토큰이면_비밀번호를_변경하고_RefreshToken을_폐기한다() throws Exception {
        String token = "validResetToken";
        String tokenHash = sha256(token);
        String newPassword = "newPassword123!";
        String newEncodedPassword = "newEncodedPassword";
        PasswordResetRequest request = new PasswordResetRequest(token, newPassword, newPassword);
        Member member = createMember();

        given(redisUtil.getAndDelete("password-reset:token:" + tokenHash)).willReturn(TEST_MEMBER_ID.toString());
        given(redisUtil.get("password-reset:member:" + TEST_MEMBER_ID)).willReturn(tokenHash);
        given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
        given(passwordEncoder.encode(newPassword)).willReturn(newEncodedPassword);

        TransactionSynchronizationManager.initSynchronization();
        try {
            authService.resetPassword(request);

            assertThat(member.getPassword()).isEqualTo(newEncodedPassword);
            then(redisUtil).should(never()).delete("password-reset:member:" + TEST_MEMBER_ID);
            then(redisUtil).should(never()).delete("refresh:" + TEST_MEMBER_ID);
            then(redisUtil).should(never()).delete("login:failure:" + TEST_MEMBER_ID);

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            then(redisUtil).should().delete("password-reset:member:" + TEST_MEMBER_ID);
            then(redisUtil).should().delete("refresh:" + TEST_MEMBER_ID);
            then(redisUtil).should().delete("login:failure:" + TEST_MEMBER_ID);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private String sha256(String value) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
