package org.cotato.gongmozip.domains.auth.dto.response;

public class AuthResponse {

    // 로그인 응답
    public record LoginResponse(
            String accessToken
    ) {}
}
