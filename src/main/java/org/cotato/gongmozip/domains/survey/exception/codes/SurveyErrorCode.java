package org.cotato.gongmozip.domains.survey.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SurveyErrorCode implements BaseErrorCode {
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
