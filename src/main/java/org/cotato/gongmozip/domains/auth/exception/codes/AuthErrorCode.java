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
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "인증이 필요합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
