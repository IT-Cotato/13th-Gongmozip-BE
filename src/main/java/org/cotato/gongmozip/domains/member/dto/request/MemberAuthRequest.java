package org.cotato.gongmozip.domains.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import org.cotato.gongmozip.domains.member.enums.Gender;
import org.cotato.gongmozip.global.validation.PasswordPolicy;

public class MemberAuthRequest {

    // 이메일 인증코드 발송 요청
    public record EmailVerifyRequest(@Email @NotBlank String email) {}

    // 이메일 인증코드 확인 요청
    public record EmailVerifyConfirmRequest(@Email @NotBlank String email, @NotBlank String code) {}

    // 회원가입 요청
    public record SignUpRequest(
            @Email @NotBlank String email,
            @NotBlank @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String password,
            @NotNull Gender gender,
            @NotNull LocalDate birthDate) {}

    // 소셜 회원 필수 정보(성별/생년월일) 등록 요청
    public record RegisterRequiredInfoRequest(@NotNull Gender gender, @NotNull LocalDate birthDate) {}
}
