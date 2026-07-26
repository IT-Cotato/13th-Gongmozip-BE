package org.cotato.gongmozip.domains.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.PasswordResetCodeRequest;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.PasswordResetCodeVerifyRequest;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.PasswordResetRequest;
import org.cotato.gongmozip.domains.auth.dto.response.AuthResponse.PasswordResetVerifyResponse;
import org.cotato.gongmozip.domains.auth.service.AuthService;
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("비로그인 사용자도 비밀번호 재설정 인증코드를 요청할 수 있다.")
    void sendPasswordResetCode_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@gongmozip.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_4"));

        then(authService).should().sendPasswordResetCode(any(PasswordResetCodeRequest.class));
    }

    @Test
    @DisplayName("비로그인 사용자도 비밀번호 재설정 인증코드를 확인할 수 있다.")
    void verifyPasswordResetCode_unauthenticated() throws Exception {
        given(authService.verifyPasswordResetCode(any(PasswordResetCodeVerifyRequest.class)))
                .willReturn(new PasswordResetVerifyResponse("password-reset-token"));

        mockMvc.perform(post("/api/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@gongmozip.com\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_5"))
                .andExpect(jsonPath("$.data.passwordResetToken").value("password-reset-token"));

        then(authService).should().verifyPasswordResetCode(any(PasswordResetCodeVerifyRequest.class));
    }

    @Test
    @DisplayName("비로그인 사용자도 유효한 입력으로 비밀번호를 재설정할 수 있다.")
    void resetPassword_unauthenticated() throws Exception {
        mockMvc.perform(
                        patch("/api/auth/password-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "token": "reset-token",
                                  "newPassword": "newPassword123!",
                                  "newPasswordConfirm": "newPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_6"));

        then(authService).should().resetPassword(any(PasswordResetRequest.class));
    }

    @Test
    @DisplayName("비밀번호 정책에 맞지 않는 요청은 400을 반환한다.")
    void resetPassword_invalidPassword() throws Exception {
        mockMvc.perform(
                        patch("/api/auth/password-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "token": "reset-token",
                                  "newPassword": "passwordonly",
                                  "newPasswordConfirm": "passwordonly"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));
    }
}
