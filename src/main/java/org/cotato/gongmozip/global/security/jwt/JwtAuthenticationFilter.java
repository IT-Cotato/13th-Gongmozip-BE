package org.cotato.gongmozip.global.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_EXPIRED_ATTRIBUTE = "tokenExpired";

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = AccessTokenExtractor.extract(request);

        if (StringUtils.hasText(token)) {
            try {
                String email = jwtProvider.getEmail(token);

                if (!isBlacklisted(token)) {
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(TOKEN_EXPIRED_ATTRIBUTE, true);
            } catch (JwtException | IllegalArgumentException e) {
                // 위조/무효 토큰은 인증 없이 통과시켜 EntryPoint에서 UNAUTHORIZED 처리
            } catch (MemberException e) {
                // 탈퇴했거나 존재하지 않는 회원의 토큰도 인증 없이 통과시켜 UNAUTHORIZED 처리
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlacklisted(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
            return redisUtil.exists(BLACKLIST_PREFIX + hash);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
}
