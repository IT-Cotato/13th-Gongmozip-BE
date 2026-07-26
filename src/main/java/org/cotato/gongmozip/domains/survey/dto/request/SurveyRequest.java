package org.cotato.gongmozip.domains.survey.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class SurveyRequest {

    // 설문 제출 요청
    public record SubmitSurveyRequest(@NotEmpty @Valid List<AnswerRequest> answers) {}

    // 개별 답변 (질문 ID + 선택한 선택지 ID)
    public record AnswerRequest(@NotNull Long questionId, @NotNull Long selectedOptionId) {}
}
