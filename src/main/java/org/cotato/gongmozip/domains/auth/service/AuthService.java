package org.cotato.gongmozip.domains.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
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
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.cotato.gongmozip.global.security.jwt.JwtProvider;
import org.cotato.gongmozip.global.verification.EmailVerificationResult;
import org.cotato.gongmozip.global.verification.EmailVerificationService;
import org.cotato.gongmozip.global.verification.EmailVerificationService.Purpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String PASSWORD_RESET_TOKEN_PREFIX = "password-reset:token:";
    private static final String PASSWORD_RESET_MEMBER_PREFIX = "password-reset:member:";
    private static final int PASSWORD_RESET_TOKEN_BYTES = 32;
    private static final long PASSWORD_RESET_TOKEN_TTL_MINUTES = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final MemberRepository memberRepository;
    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisUtil redisUtil;
    private final EmailVerificationService emailVerificationService;

    // 로그인 메서드
    public LoginResult login(LoginRequest request) {
        // 이메일이 존재하지 않는 경우
        Member member = memberRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 비밀번호 틀린 경우
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

        // 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(member.getMemberId(), member.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(member.getMemberId(), member.getEmail());

        // refresh 토큰만 redis에 저장
        redisUtil.set(
                REFRESH_TOKEN_PREFIX + member.getMemberId(),
                refreshToken,
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS);

        return new LoginResult(accessToken, refreshToken);
    }

    // 토큰 재발급 메서드
    public LoginResult reissue(String refreshToken) {
        // refresh 토큰이 유효하지 않은 경우
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // refresh 토큰에서 회원 정보 추출
        Long memberId = jwtProvider.getMemberId(refreshToken);
        String email = jwtProvider.getEmail(refreshToken);

        // redis에 저장된 refresh 토큰과 일치하지 않는 경우
        String storedToken = redisUtil.get(REFRESH_TOKEN_PREFIX + memberId);
        if (!refreshToken.equals(storedToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // access 토큰과 refresh 토큰 새로 발급
        String newAccessToken = jwtProvider.generateAccessToken(memberId, email);
        String newRefreshToken = jwtProvider.generateRefreshToken(memberId, email);
        // 새 refresh 토큰으로 redis 값 갱신
        redisUtil.set(REFRESH_TOKEN_PREFIX + memberId, newRefreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);

        return new LoginResult(newAccessToken, newRefreshToken);
    }

    // 로그아웃 메서드
    public void logout(Long memberId, String accessToken) {
        // access 토큰의 남은 유효 시간 동안 블랙리스트에 저장
        long remainingMillis = jwtProvider.getRemainingExpirationMillis(accessToken);
        if (remainingMillis > 0) {
            redisUtil.set(BLACKLIST_PREFIX + sha256(accessToken), "logout", remainingMillis, TimeUnit.MILLISECONDS);
        }
        // redis에 저장된 refresh 토큰 삭제
        redisUtil.delete(REFRESH_TOKEN_PREFIX + memberId);
    }

    // 비밀번호 재설정 인증 코드 전송 메서드
    public void sendPasswordResetCode(PasswordResetCodeRequest request) {
        // 이메일이 존재하지 않는 경우
        Member member = memberRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 이메일 로그인 계정이 아닌 경우
        if (!authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL)) {
            throw new AuthException(AuthErrorCode.PASSWORD_RESET_UNAVAILABLE);
        }

        // 이전에 발급된 비밀번호 재설정 토큰 무효화
        invalidatePasswordResetToken(member.getMemberId());

        // 공통 이메일 인증 서비스를 통해 인증 코드 전송
        try {
            emailVerificationService.sendCode(Purpose.PASSWORD_RESET, member.getEmail(), "[공모집] 비밀번호 재설정 인증코드");
        } catch (MailException e) {
            throw new AuthException(AuthErrorCode.PASSWORD_RESET_EMAIL_SEND_FAILED);
        }
    }

    // 비밀번호 재설정 인증 코드 확인 메서드
    public PasswordResetVerifyResponse verifyPasswordResetCode(PasswordResetCodeVerifyRequest request) {
        // 이메일이 존재하지 않는 경우
        Member member = memberRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 이메일 로그인 계정이 아닌 경우
        if (!authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL)) {
            throw new AuthException(AuthErrorCode.PASSWORD_RESET_UNAVAILABLE);
        }

        // 공통 이메일 인증 결과를 auth 도메인 예외로 변환
        EmailVerificationResult result =
                emailVerificationService.verifyCode(Purpose.PASSWORD_RESET, request.email(), request.code());
        switch (result) {
            case INVALID_CODE -> throw new AuthException(AuthErrorCode.INVALID_PASSWORD_RESET_CODE);
            case EXPIRED_CODE -> throw new AuthException(AuthErrorCode.EXPIRED_PASSWORD_RESET_CODE);
            case CODE_NOT_ISSUED -> throw new AuthException(AuthErrorCode.PASSWORD_RESET_CODE_NOT_ISSUED);
            case TOO_MANY_ATTEMPTS -> throw new AuthException(AuthErrorCode.TOO_MANY_PASSWORD_RESET_ATTEMPTS);
            case VERIFIED -> {}
        }

        // 기존 토큰을 무효화하고 새로운 일회용 토큰 생성
        invalidatePasswordResetToken(member.getMemberId());
        String token = generatePasswordResetToken();
        String tokenHash = sha256(token);
        // 원본 토큰 대신 해시와 회원 id를 redis에 저장
        redisUtil.set(
                PASSWORD_RESET_TOKEN_PREFIX + tokenHash,
                member.getMemberId().toString(),
                PASSWORD_RESET_TOKEN_TTL_MINUTES,
                TimeUnit.MINUTES);
        // 회원별 최신 토큰 해시를 저장해 이전 토큰 사용 방지
        redisUtil.set(
                PASSWORD_RESET_MEMBER_PREFIX + member.getMemberId(),
                tokenHash,
                PASSWORD_RESET_TOKEN_TTL_MINUTES,
                TimeUnit.MINUTES);

        return new PasswordResetVerifyResponse(token);
    }

    @Transactional
    // 비밀번호 재설정 메서드
    public void resetPassword(PasswordResetRequest request) {
        // 새 비밀번호와 비밀번호 확인이 일치하지 않는 경우
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new AuthException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        // 토큰을 해시한 후 redis에서 원자적으로 조회 및 삭제
        String tokenHash = sha256(request.token());
        String memberIdValue = redisUtil.getAndDelete(PASSWORD_RESET_TOKEN_PREFIX + tokenHash);
        if (memberIdValue == null) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        // redis에 저장된 회원 id 형식이 올바르지 않은 경우
        Long memberId;
        try {
            memberId = Long.valueOf(memberIdValue);
        } catch (NumberFormatException e) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        // 회원에게 가장 최근에 발급된 토큰인지 확인
        String memberKey = PASSWORD_RESET_MEMBER_PREFIX + memberId;
        if (!tokenHash.equals(redisUtil.get(memberKey))) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        // 비밀번호를 암호화하여 변경
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.changePassword(passwordEncoder.encode(request.newPassword()));

        // 비밀번호 재설정 정보와 기존 refresh 토큰 삭제
        redisUtil.delete(memberKey);
        redisUtil.delete(REFRESH_TOKEN_PREFIX + memberId);
    }

    // 비밀번호 재설정용 랜덤 토큰 생성 메서드
    private String generatePasswordResetToken() {
        byte[] bytes = new byte[PASSWORD_RESET_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // 기존 비밀번호 재설정 토큰 무효화 메서드
    private void invalidatePasswordResetToken(Long memberId) {
        String memberKey = PASSWORD_RESET_MEMBER_PREFIX + memberId;
        String previousTokenHash = redisUtil.get(memberKey);
        if (previousTokenHash != null) {
            redisUtil.delete(PASSWORD_RESET_TOKEN_PREFIX + previousTokenHash);
            redisUtil.delete(memberKey);
        }
    }

    // 토큰 해시 메서드
    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
