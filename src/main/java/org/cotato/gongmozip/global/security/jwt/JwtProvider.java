package org.cotato.gongmozip.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // access 토큰 생성
    public String generateAccessToken(Long memberId, String email) {
        return buildToken(memberId, email, accessTokenExpiration);
    }

    // refresh 토큰 생성
    public String generateRefreshToken(Long memberId, String email) {
        return buildToken(memberId, email, refreshTokenExpiration);
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 편의 메서드: memberId 추출
    public Long getMemberId(String token) {
        return parseClaims(token).get("memberId", Long.class);
    }

    // 편의 메서드: email 추출
    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // Access Token 남은 만료 시간(ms) 반환
    public long getRemainingExpirationMillis(String token) {
        return parseClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }

    // 토큰 생성 메서드(email + memberId 조합)
    private String buildToken(Long memberId, String email, long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email) // 토큰 주인(이메일)
                .claim("memberId", memberId) // 커스텀 데이터 추가(memberId)
                .issuedAt(now) // 발급 시간
                .expiration(new Date(now.getTime() + expiration)) // 만료 시간
                .signWith(secretKey) // 서명 설정
                .compact(); // jwt 문자열로 직렬화
    }

    // 검증 메서드
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey) // 서명 검증 키 설정
                .build()
                .parseSignedClaims(token) // 파싱 + 서명 검증 수행
                .getPayload(); // Claims 객체 반환
    }
}
