package org.cotato.gongmozip.domains.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class MemberAuthRequest {

    // 이메일 인증코드 발송 요청
    public record EmailVerifyRequest(@Email @NotBlank String email) {}

    // 이메일 인증코드 확인 요청
    public record EmailVerifyConfirmRequest(@Email @NotBlank String email, @NotBlank String code) {}
}
