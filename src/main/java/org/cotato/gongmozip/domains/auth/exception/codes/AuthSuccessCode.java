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
    REISSUE_SUCCESS(HttpStatus.OK, "AUTH_200_3", "토큰 재발급에 성공하였습니다."),
    PASSWORD_RESET_CODE_SENT(HttpStatus.OK, "AUTH_200_4", "비밀번호 재설정 인증코드를 전송했습니다."),
    PASSWORD_RESET_CODE_VERIFIED(HttpStatus.OK, "AUTH_200_5", "비밀번호 재설정 인증코드가 확인되었습니다."),
    PASSWORD_RESET_SUCCESS(HttpStatus.OK, "AUTH_200_6", "비밀번호 재설정에 성공했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
