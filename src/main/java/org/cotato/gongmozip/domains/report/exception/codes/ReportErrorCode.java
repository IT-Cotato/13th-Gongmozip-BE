package org.cotato.gongmozip.domains.report.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {

    // 400
    INVALID_REPORT_REASON(HttpStatus.BAD_REQUEST, "REPORT_400_1", "올바르지 않은 신고 사유입니다."),
    MISSING_CUSTOM_REASON(HttpStatus.BAD_REQUEST, "REPORT_400_2", "기타 사유 선택 시 직접 입력한 사유가 필요합니다."),
    CANNOT_REPORT_SELF(HttpStatus.BAD_REQUEST, "REPORT_400_3", "본인은 신고할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
