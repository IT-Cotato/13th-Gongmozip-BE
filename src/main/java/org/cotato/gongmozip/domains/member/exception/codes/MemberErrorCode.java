package org.cotato.gongmozip.domains.member.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    // 400
    INVALID_VERIFY_CODE(HttpStatus.BAD_REQUEST, "MEMBER_400_1", "인증코드가 올바르지 않습니다."),
    EXPIRED_VERIFY_CODE(HttpStatus.BAD_REQUEST, "MEMBER_400_2", "인증코드가 만료되었습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "MEMBER_400_3", "이메일 인증이 완료되지 않았습니다."),
    VERIFY_CODE_NOT_ISSUED(HttpStatus.BAD_REQUEST, "MEMBER_400_4", "인증코드가 발급된 적 없습니다. 먼저 이메일 인증을 요청해 주세요."),

    // 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER_409_1", "이미 가입된 이메일입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
