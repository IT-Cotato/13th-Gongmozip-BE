package org.cotato.gongmozip.domains.member.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements BaseSuccessCode {

    // 200
    EMAIL_VERIFY_CODE_SENT(HttpStatus.OK, "MEMBER_200_1", "인증코드가 발송되었습니다."),
    EMAIL_VERIFY_SUCCESS(HttpStatus.OK, "MEMBER_200_2", "이메일 인증에 성공하였습니다."),

    // 201
    SIGN_UP_SUCCESS(HttpStatus.CREATED, "MEMBER_201_1", "회원가입에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
