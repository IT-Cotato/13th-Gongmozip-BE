package org.cotato.gongmozip.global.security.oauth2.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.cotato.gongmozip.global.security.jwt.JwtProvider;
import org.cotato.gongmozip.global.security.oauth2.dto.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    @Value("${oauth2.redirect-url}")
    private String redirectUrl;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final JwtProvider jwtProvider;
    private final RedisUtil redisUtil;

    // 소셜 로그인 성공 시 JWT 발급 후 쿠키에 담아 프론트로 리다이렉트
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Long memberId = oAuth2User.getMemberId();
        String email = oAuth2User.getEmail();

        // JWT 토큰 생성
        String accessToken = jwtProvider.generateAccessToken(memberId, email);
        String refreshToken = jwtProvider.generateRefreshToken(memberId, email);

        // Redis에 리프레시 토큰 저장
        redisUtil.set(REFRESH_TOKEN_PREFIX + memberId, refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);

        // Access Token: JS에서 읽어 Authorization 헤더로 사용하므로 non-HttpOnly
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .httpOnly(false)
                .secure(request.isSecure())
                .sameSite("Lax")
                .maxAge(accessTokenExpiration / 1000)
                .path("/")
                .build();

        // Refresh Token: 탈취 방지를 위해 HttpOnly, /api/auth 경로에서만 전송
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .maxAge(refreshTokenExpiration / 1000)
                .path("/api/auth")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 프론트엔드로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
