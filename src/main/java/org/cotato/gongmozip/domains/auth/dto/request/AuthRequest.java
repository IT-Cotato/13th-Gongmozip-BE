package org.cotato.gongmozip.domains.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.cotato.gongmozip.global.validation.PasswordPolicy;

public class AuthRequest {

    // 로그인 요청
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    // 비밀번호 재설정 인증코드 발송 요청
    public record PasswordResetCodeRequest(@Email @NotBlank String email) {}

    // 비밀번호 재설정 인증코드 확인 요청
    public record PasswordResetCodeVerifyRequest(
            @Email @NotBlank String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "인증코드는 6자리 숫자여야 합니다.") String code) {}

    // 비밀번호 재설정 요청
    public record PasswordResetRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String newPassword,
            @NotBlank String newPasswordConfirm) {}
}
