package org.cotato.gongmozip.domains.auth.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // 401
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "인증이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_3", "유효하지 않은 Refresh Token입니다."),
    KAKAO_ACCOUNT_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_401_4", "카카오 계정 정보가 없습니다."),
    KAKAO_EMAIL_NOT_PROVIDED(HttpStatus.UNAUTHORIZED, "AUTH_401_5", "카카오 이메일 정보가 없습니다."),
    GOOGLE_EMAIL_NOT_PROVIDED(HttpStatus.UNAUTHORIZED, "AUTH_401_6", "구글 이메일 정보가 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
