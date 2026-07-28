package org.cotato.gongmozip.domains.survey.converter;

import java.math.BigDecimal;
import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.AxisResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.OptionResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.QuestionResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.SurveyResultResponse;
import org.cotato.gongmozip.domains.survey.entity.SurveyAnswer;
import org.cotato.gongmozip.domains.survey.entity.SurveyOption;
import org.cotato.gongmozip.domains.survey.entity.SurveyQuestion;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;

public class SurveyConverter {

    private SurveyConverter() {}

    // 질문 엔티티와 선택지 목록을 질문 -> DTO
    public static QuestionResponse toQuestionResponse(SurveyQuestion question, List<SurveyOption> options) {
        List<OptionResponse> optionResponses =
                options.stream().map(SurveyConverter::toOptionResponse).toList();
        return new QuestionResponse(
                question.getQuestionId(),
                question.getQuestionKey(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getDisplayOrder(),
                optionResponses);
    }

    // 선택지 엔티티 -> 선택지 응답 DTO
    private static OptionResponse toOptionResponse(SurveyOption option) {
        return new OptionResponse(
                option.getOptionId(),
                option.getOptionKey(),
                option.getOptionLabel(),
                option.getOptionValue(),
                option.getDisplayOrder());
    }

    // 제출·질문·선택지를 조합 -> 답변 엔티티를 생성
    public static SurveyAnswer toSurveyAnswer(
            SurveySubmission submission, SurveyQuestion question, SurveyOption selectedOption) {
        return SurveyAnswer.builder()
                .submission(submission)
                .question(question)
                .selectedOption(selectedOption)
                .build();
    }

    // 제출 엔티티와 캐릭터 정보를 설문 결과 응답 DTO로 변환
    public static SurveyResultResponse toResultResponse(
            SurveySubmission submission, CurrentCharacterResponse character) {
        // CONSCIENTIOUSNESS_1 단독 점수는 X축 합산에서 역산 (characterXScore = GOAL + WORK + CONSC_1)
        BigDecimal conscientiousness1Score = submission
                .getCharacterXScore()
                .subtract(submission.getGoalPreferenceScore())
                .subtract(submission.getWorkStyleScore());

        return new SurveyResultResponse(
                submission.getCharacterType(),
                submission.getExtroversionType(),
                submission.getCharacterXScore(),
                submission.getCharacterYScore(),
                submission.getAgreeablenessScore(),
                submission.getConscientiousnessScore(),
                submission.getHonestyHumilityScore(),
                submission.getExtroversionScore(),
                submission.getGoalPreferenceScore(),
                submission.getWorkStyleScore(),
                submission.getCommunicationStyleScore(),
                buildAxes(submission, conscientiousness1Score),
                character);
    }

    // 캐릭터 유형별로 표시할 성향 축 3개를 조립한다
    private static List<AxisResponse> buildAxes(SurveySubmission submission, BigDecimal conscientiousness1Score) {
        BigDecimal workStyle = submission.getWorkStyleScore();
        BigDecimal communication = submission.getCommunicationStyleScore();
        BigDecimal goal = submission.getGoalPreferenceScore();
        BigDecimal ext2 = submission.getExtroversion2Score();
        BigDecimal ext3 = submission.getExtroversion3Score();

        return switch (submission.getCharacterType()) {
            case LEAD_RUNNER -> List.of(
                    new AxisResponse("즉흥형", "계획형", workStyle),
                    new AxisResponse("독립형", "조율형", communication),
                    new AxisResponse("신중형", "추진형", conscientiousness1Score));
            case TRACK_RUNNER -> List.of(
                    new AxisResponse("배려형", "주장형", ext3),
                    new AxisResponse("속도형", "꼼꼼형", workStyle),
                    new AxisResponse("신중형", "추진형", conscientiousness1Score));
            case BOOST_RUNNER -> List.of(
                    new AxisResponse("신중형", "추진형", conscientiousness1Score),
                    new AxisResponse("관찰형", "교류형", ext2),
                    new AxisResponse("속도형", "꼼꼼형", workStyle));
            case FREE_RUNNER -> List.of(
                    new AxisResponse("유연형", "계획형", workStyle),
                    new AxisResponse("탐색형", "안정형", goal),
                    new AxisResponse("독립형", "조율형", communication));
        };
    }
}
