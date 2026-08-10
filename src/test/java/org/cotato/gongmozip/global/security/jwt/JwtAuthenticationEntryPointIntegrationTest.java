package org.cotato.gongmozip.global.security.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationEntryPointIntegrationTest {

    private static final String PROTECTED_ENDPOINT = "/api/mypage";

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String secret;

    @Test
    @DisplayName("만료된 액세스 토큰으로 보호된 엔드포인트 요청 시 401과 AUTH_401_8을 반환한다.")
    void expiredToken_returnsTokenExpiredCode() throws Exception {
        String expiredToken = buildToken(secret, -1000L);

        mockMvc.perform(get(PROTECTED_ENDPOINT).header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_8"));
    }

    @Test
    @DisplayName("토큰 없이 보호된 엔드포인트 요청 시 401과 AUTH_401_2를 반환한다.")
    void noToken_returnsUnauthorizedCode() throws Exception {
        mockMvc.perform(get(PROTECTED_ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_2"));
    }

    @Test
    @DisplayName("다른 키로 서명된 위조 토큰으로 요청 시 401과 AUTH_401_2를 반환한다.")
    void forgedToken_returnsUnauthorizedCode() throws Exception {
        String forgedToken = buildToken("forgedsecretkeyforgedsecretkeyforgedsecretkeyforgedsecretkey", 3600000L);

        mockMvc.perform(get(PROTECTED_ENDPOINT).header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_2"));
    }

    @Test
    @DisplayName("형식이 잘못된 무효 토큰으로 요청 시 401과 AUTH_401_2를 반환한다.")
    void malformedToken_returnsUnauthorizedCode() throws Exception {
        mockMvc.perform(get(PROTECTED_ENDPOINT).header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_2"));
    }

    private String buildToken(String signingSecret, long expirationMillis) {
        SecretKey key = Keys.hmacShaKeyFor(signingSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject("test@gongmozip.com")
                .claim("memberId", 1L)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }
}
