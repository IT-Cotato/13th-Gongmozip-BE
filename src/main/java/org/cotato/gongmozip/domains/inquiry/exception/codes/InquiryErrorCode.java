package org.cotato.gongmozip.domains.inquiry.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InquiryErrorCode implements BaseErrorCode {

    // 400
    INQUIRY_INVALID_INPUT(HttpStatus.BAD_REQUEST, "INQUIRY_400_1", "유효하지 않은 요청 값입니다."),

    // 404
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY_404_1", "해당 정보로 접수된 문의가 없습니다. 메일 또는 비밀번호를 다시 확인해 주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
