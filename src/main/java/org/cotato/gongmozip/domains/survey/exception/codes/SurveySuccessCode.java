package org.cotato.gongmozip.domains.survey.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SurveySuccessCode implements BaseSuccessCode {

    // 200
    QUESTIONS_RETRIEVED(HttpStatus.OK, "SURVEY_200_1", "질문 목록을 성공적으로 조회했습니다."),
    STATUS_RETRIEVED(HttpStatus.OK, "SURVEY_200_2", "설문 상태를 성공적으로 조회했습니다."),
    SURVEY_SUBMITTED(HttpStatus.OK, "SURVEY_200_3", "설문이 성공적으로 제출되었습니다."),
    RESULT_RETRIEVED(HttpStatus.OK, "SURVEY_200_4", "설문 결과를 성공적으로 조회했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
