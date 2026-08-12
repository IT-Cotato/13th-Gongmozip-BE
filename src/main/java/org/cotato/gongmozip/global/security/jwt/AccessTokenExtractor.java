package org.cotato.gongmozip.global.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Authorization 헤더(Bearer)를 우선 확인하고, 없으면 accessToken 쿠키에서 토큰을 추출한다.
 * 소셜 로그인은 HttpOnly 쿠키로 토큰을 전달하므로(JS 접근 차단) 쿠키 폴백이 필요하다.
 */
public final class AccessTokenExtractor {

    public static final String ACCESS_TOKEN_COOKIE = "accessToken";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private AccessTokenExtractor() {}

    public static String extract(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            // 빈 Bearer 헤더("Bearer ")는 무시하고 쿠키 폴백으로 넘어간다
            if (StringUtils.hasText(token)) {
                return token;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
