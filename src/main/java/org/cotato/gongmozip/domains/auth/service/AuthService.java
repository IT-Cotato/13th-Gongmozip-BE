package org.cotato.gongmozip.domains.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.LoginRequest;
import org.cotato.gongmozip.domains.auth.dto.response.AuthResponse.LoginResult;
import org.cotato.gongmozip.domains.auth.exception.AuthException;
import org.cotato.gongmozip.domains.auth.exception.codes.AuthErrorCode;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.cotato.gongmozip.global.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisUtil redisUtil;

    // 로그인 메서드
    public LoginResult login(LoginRequest request) {
        // 이메일이 존재하지 않는 경우
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 비밀번호 틀린 경우
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

        // 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(member.getMemberId(), member.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(member.getMemberId(), member.getEmail());

        // refresh 토큰만 redis에 저장
        redisUtil.set(REFRESH_TOKEN_PREFIX + member.getMemberId(), refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);

        return new LoginResult(accessToken, refreshToken);
    }

    // 토큰 재발급 메서드
    public LoginResult reissue(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = jwtProvider.getMemberId(refreshToken);
        String email = jwtProvider.getEmail(refreshToken);

        String storedToken = redisUtil.get(REFRESH_TOKEN_PREFIX + memberId);
        if (!refreshToken.equals(storedToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccessToken = jwtProvider.generateAccessToken(memberId, email);
        String newRefreshToken = jwtProvider.generateRefreshToken(memberId, email);
        redisUtil.set(REFRESH_TOKEN_PREFIX + memberId, newRefreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);

        return new LoginResult(newAccessToken, newRefreshToken);
    }

    // 로그아웃 메서드
    public void logout(Long memberId, String accessToken) {
        long remainingMillis = jwtProvider.getRemainingExpirationMillis(accessToken);
        if (remainingMillis > 0) {
            redisUtil.set(BLACKLIST_PREFIX + sha256(accessToken), "logout", remainingMillis, TimeUnit.MILLISECONDS);
        }
        redisUtil.delete(REFRESH_TOKEN_PREFIX + memberId);
    }

    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 사용 불가", e);
        }
    }
}
