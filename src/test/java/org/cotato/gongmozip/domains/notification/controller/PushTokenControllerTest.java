package org.cotato.gongmozip.domains.notification.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.notification.service.PushTokenService;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PushTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PushTokenService pushTokenService;

    private final Member member =
            Member.builder().memberId(1L).email("member@example.com").build();
    private final CustomUserDetails userDetails = new CustomUserDetails(member);

    @DisplayName("로그인한 회원은 FCM 토큰을 등록할 수 있다.")
    @Test
    void registerPushToken() throws Exception {
        mockMvc.perform(post("/api/notifications/push-tokens")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenBody("fcm-token-a"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_201_1"));
    }

    @DisplayName("빈 토큰으로 등록하면 400을 반환한다.")
    @Test
    void registerPushTokenRejectsBlankToken() throws Exception {
        mockMvc.perform(post("/api/notifications/push-tokens")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenBody(""))))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("로그인한 회원은 FCM 토큰을 해제할 수 있다.")
    @Test
    void unregisterPushToken() throws Exception {
        mockMvc.perform(delete("/api/notifications/push-tokens")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenBody("fcm-token-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_200_4"));
    }

    @DisplayName("비인증 사용자는 토큰을 등록할 수 없다.")
    @Test
    void registerPushTokenRejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/notifications/push-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenBody("fcm-token-a"))))
                .andExpect(status().isUnauthorized());
    }

    private record TokenBody(String token) {}
}
