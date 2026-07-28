package org.cotato.gongmozip.domains.survey.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SurveyErrorCode implements BaseErrorCode {

    // 400
    OPTION_NOT_BELONG_TO_QUESTION(HttpStatus.BAD_REQUEST, "SURVEY_400_1", "선택지가 해당 질문에 속하지 않습니다."),
    MISSING_REQUIRED_ANSWER(HttpStatus.BAD_REQUEST, "SURVEY_400_2", "모든 필수 질문에 답변해야 합니다."),

    // 404
    SURVEY_NOT_SUBMITTED(HttpStatus.NOT_FOUND, "SURVEY_404_1", "제출된 설문 결과가 없습니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SURVEY_404_2", "존재하지 않는 질문입니다."),
    OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SURVEY_404_3", "존재하지 않는 선택지입니다."),

    // 409
    RETAKE_NOT_ALLOWED(HttpStatus.CONFLICT, "SURVEY_409_1", "협업 유형 검사는 3개월에 한 번만 재응시할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
