package org.cotato.gongmozip.domains.survey.dto.response;

import java.math.BigDecimal;
import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.survey.enums.QuestionType;

public class SurveyResponse {

    // 질문 목록 조회 응답
    public record QuestionListResponse(List<QuestionResponse> questions) {}

    // 개별 질문 (선택지 목록 포함)
    public record QuestionResponse(
            Long questionId,
            String questionKey,
            String questionText,
            QuestionType questionType,
            int displayOrder,
            List<OptionResponse> options) {}

    // 개별 선택지
    public record OptionResponse(
            Long optionId, String optionKey, String optionLabel, String optionValue, int displayOrder) {}

    // 설문 제출 상태 조회 응답 (NONE: 미제출 / SUBMITTED: 제출 완료)
    public record SurveyStatusResponse(String status) {}

    // 캐릭터 유형별 성향 축 (leftLabel ↔ rightLabel, score 1~5)
    public record AxisResponse(String leftLabel, String rightLabel, BigDecimal score) {}

    // 설문 분석 결과 응답
    public record SurveyResultResponse(
            CharacterType characterType,
            ExtroversionType extroversionType,
            BigDecimal characterXScore,
            BigDecimal characterYScore,
            BigDecimal agreeablenessScore,
            BigDecimal conscientiousnessScore,
            BigDecimal honestyHumilityScore,
            BigDecimal extroversionScore,
            BigDecimal goalPreferenceScore,
            BigDecimal workStyleScore,
            BigDecimal communicationStyleScore,
            List<AxisResponse> axes,
            CurrentCharacterResponse character) {}
}
