package org.cotato.gongmozip.global.websocket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetailsService;
import org.cotato.gongmozip.global.security.jwt.JwtProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT 프레임의 Authorization 헤더로 JWT를 검증한다. HTTP 핸드셰이크 단계가 아니라
 * 프레임 레벨에서 인증하므로 SecurityConfig에서 "/ws/**"는 permitAll로 열어둔다.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisUtil redisUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token == null || !jwtProvider.validateToken(token) || isBlacklisted(token)) {
                throw new MessagingException("유효하지 않은 인증 정보입니다.");
            }

            UserDetails userDetails = customUserDetailsService.loadUserByUsername(jwtProvider.getEmail(token));
            accessor.setUser(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
            // wrap()/getAccessor()로 얻은 accessor의 변경이 실제 전달되는 Message에 반영되도록
            // 명시적으로 재구성한다 (in-place mutation에만 의존하지 않음).
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
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
