package org.cotato.gongmozip.domains.survey.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SurveySuccessCode implements BaseSuccessCode {
    QUESTIONS_RETRIEVED(HttpStatus.OK, "SURVEY_001", "질문 목록을 성공적으로 조회했습니다."),
    STATUS_RETRIEVED(HttpStatus.OK, "SURVEY_002", "설문 상태를 성공적으로 조회했습니다."),
    SURVEY_SUBMITTED(HttpStatus.OK, "SURVEY_003", "설문이 성공적으로 제출되었습니다."),
    RESULT_RETRIEVED(HttpStatus.OK, "SURVEY_004", "설문 결과를 성공적으로 조회했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
