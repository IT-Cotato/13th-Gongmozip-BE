package org.cotato.gongmozip.domains.auth.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements BaseSuccessCode {

    // 200
    LOGIN_SUCCESS(HttpStatus.OK, "AUTH_200_1", "로그인에 성공하였습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "AUTH_200_2", "로그아웃에 성공하였습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
