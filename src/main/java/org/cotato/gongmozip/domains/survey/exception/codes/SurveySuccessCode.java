package org.cotato.gongmozip.domains.survey.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SurveySuccessCode implements BaseSuccessCode {
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
