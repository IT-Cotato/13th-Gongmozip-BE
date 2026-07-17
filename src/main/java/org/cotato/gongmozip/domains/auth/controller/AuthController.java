package org.cotato.gongmozip.domains.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.LoginRequest;
import org.cotato.gongmozip.domains.auth.dto.response.AuthResponse.LoginResponse;
import org.cotato.gongmozip.domains.auth.dto.response.AuthResponse.LoginResult;
import org.cotato.gongmozip.domains.auth.exception.codes.AuthErrorCode;
import org.cotato.gongmozip.domains.auth.exception.codes.AuthSuccessCode;
import org.cotato.gongmozip.domains.auth.service.AuthService;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final AuthService authService;

    @Operation(summary = "이메일 로그인")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {AuthErrorCode.class, MemberErrorCode.class})
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request, HttpServletResponse response) {

        LoginResult result = authService.login(request);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, result.refreshToken())
                .httpOnly(true) // XSS 공격 방어
                .secure(false) // 운영 환경에서는 true로 변경 (HTTPS 필요)
                .sameSite("Strict")
                .maxAge(refreshTokenExpiration / 1000) // 쿠키 만료 시간(14일)
                .path("/api/auth") // 쿠키는 api/auth인 경우에만 전송
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return BaseResponseFormatter.success(AuthSuccessCode.LOGIN_SUCCESS, new LoginResponse(result.accessToken()));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails, // refresh 토큰 삭제용
            HttpServletRequest request, // access 토큰 블랙리스트 처리용
            HttpServletResponse response // 쿠키 제거용
            ) {

        String accessToken = extractToken(request);
        authService.logout(userDetails.getMemberId(), accessToken);

        ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false) // 운영 환경에서는 true로 변경 (HTTPS 필요)
                .sameSite("Strict")
                .maxAge(0) // 쿠키 즉시 만료
                .path("/api/auth")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return BaseResponseFormatter.success(AuthSuccessCode.LOGOUT_SUCCESS);
    }

    @Operation(summary = "토큰 재발급")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {AuthErrorCode.class})
    @PostMapping("/reissue")
    public ResponseEntity<BaseResponse<LoginResponse>> reissue(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {

        LoginResult result = authService.reissue(refreshToken);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .maxAge(refreshTokenExpiration / 1000)
                .path("/api/auth")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return BaseResponseFormatter.success(AuthSuccessCode.REISSUE_SUCCESS, new LoginResponse(result.accessToken()));
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
