package org.cotato.gongmozip.domains.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthRequest {

    // 로그인 요청
    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}
}
