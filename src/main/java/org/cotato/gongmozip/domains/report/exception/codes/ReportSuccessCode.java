package org.cotato.gongmozip.domains.report.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportSuccessCode implements BaseSuccessCode {

    // 201
    REPORT_SUBMITTED(HttpStatus.CREATED, "REPORT_201_1", "신고 접수에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
