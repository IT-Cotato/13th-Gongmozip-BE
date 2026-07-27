package org.cotato.gongmozip.domains.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.LoginRequest;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.PasswordResetCodeRequest;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.PasswordResetCodeVerifyRequest;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.PasswordResetRequest;
import org.cotato.gongmozip.domains.auth.dto.response.AuthResponse.PasswordResetVerifyResponse;
import org.cotato.gongmozip.domains.auth.exception.AuthException;
import org.cotato.gongmozip.domains.auth.exception.codes.AuthErrorCode;
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
    @DisplayName("로그인 비밀번호를 5회 이상 틀리면 비밀번호 재설정 권장 응답을 반환한다.")
    void login_passwordResetRecommended() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new AuthException(AuthErrorCode.PASSWORD_RESET_RECOMMENDED));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@gongmozip.com\",\"password\":\"wrongPassword1!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_7"))
                .andExpect(jsonPath("$.message").value("비밀번호를 5회 이상 잘못 입력했습니다. 비밀번호 재설정을 권장합니다."));
    }

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
