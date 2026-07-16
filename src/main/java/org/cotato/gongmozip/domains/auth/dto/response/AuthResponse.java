package org.cotato.gongmozip.domains.auth.dto.response;

public class AuthResponse {

    // 로그인 응답 (클라이언트 반환용)
    public record LoginResponse(String accessToken) {}

    // 로그인 내부 전달용 (서비스 -> 컨트롤러)
    public record LoginResult(String accessToken, String refreshToken) {}
}
