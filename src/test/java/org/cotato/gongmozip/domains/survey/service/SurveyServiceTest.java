package org.cotato.gongmozip.domains.survey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.character.enums.CharacterPalette;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.survey.dto.request.SurveyRequest.AnswerRequest;
import org.cotato.gongmozip.domains.survey.dto.request.SurveyRequest.SubmitSurveyRequest;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.QuestionListResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.SurveyResultResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.SurveyStatusResponse;
import org.cotato.gongmozip.domains.survey.entity.SurveyOption;
import org.cotato.gongmozip.domains.survey.entity.SurveyQuestion;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.survey.enums.QuestionType;
import org.cotato.gongmozip.domains.survey.enums.SubmissionStatus;
import org.cotato.gongmozip.domains.survey.exception.SurveyException;
import org.cotato.gongmozip.domains.survey.exception.codes.SurveyErrorCode;
import org.cotato.gongmozip.domains.survey.repository.SurveyAnswerRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveyOptionRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveyQuestionRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveySubmissionRepository;
import org.cotato.gongmozip.domains.survey.vo.SurveyScoreSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private CharacterService characterService;

    private static final List<String> QUESTION_KEYS = List.of(
            "AGREEABLENESS_1",
            "AGREEABLENESS_2",
            "AGREEABLENESS_3",
            "CONSCIENTIOUSNESS_1",
            "CONSCIENTIOUSNESS_2",
            "CONSCIENTIOUSNESS_3",
            "HONESTY_HUMILITY_1",
            "HONESTY_HUMILITY_2",
            "HONESTY_HUMILITY_3",
            "EXTROVERSION_1",
            "EXTROVERSION_2",
            "EXTROVERSION_3",
            "GOAL_PREFERENCE",
            "WORK_STYLE",
            "COMMUNICATION_STYLE");

    private final Member member =
            Member.builder().memberId(1L).email("member@example.com").build();

    @Mock
    private SurveyQuestionRepository surveyQuestionRepository;

    @Mock
    private SurveyOptionRepository surveyOptionRepository;

    @Mock
    private SurveySubmissionRepository surveySubmissionRepository;

    @Mock
    private SurveyAnswerRepository surveyAnswerRepository;

    @InjectMocks
    private SurveyService surveyService;

    @DisplayName("질문 목록을 선택지와 함께 반환한다")
    @Test
    void getQuestions_returnsQuestionsWithOptions() {
        SurveyFixture fixture = surveyFixture("3");
        stubQuestions(fixture);

        QuestionListResponse response = surveyService.getQuestions();

        assertThat(response.questions()).hasSize(QUESTION_KEYS.size());
        assertThat(response.questions())
                .extracting(question -> question.questionKey())
                .containsExactlyInAnyOrderElementsOf(QUESTION_KEYS);
        assertThat(response.questions()).allSatisfy(question -> {
            assertThat(question.options()).hasSize(1);
            assertThat(question.options().getFirst().optionKey()).isEqualTo("RATING_3");
        });
        then(surveyOptionRepository).should().findAllByQuestions(fixture.questions());
    }

    @DisplayName("제출 이력이 없으면 설문 상태는 NONE이며 재응시가 가능하다")
    @Test
    void getSurveyStatus_returnsNone() {
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.empty());

        SurveyStatusResponse response = surveyService.getSurveyStatus(member);

        assertThat(response.status()).isEqualTo("NONE");
        assertThat(response.canRetest()).isTrue();
        assertThat(response.nextRetakeAt()).isNull();
    }

    @DisplayName("제출이 완료되었으나 3개월이 경과하지 않았으면 canRetest는 false이다")
    @Test
    void getSurveyStatus_whenSubmittedUnder3Months_returnsCanRetestFalse() {
        LocalDateTime submittedAt = LocalDateTime.now().minusMonths(1);
        SurveySubmission submission = submission(submittedAt);
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.of(submission));

        SurveyStatusResponse response = surveyService.getSurveyStatus(member);

        assertThat(response.status()).isEqualTo("SUBMITTED");
        assertThat(response.canRetest()).isFalse();
        assertThat(response.nextRetakeAt()).isEqualTo(submittedAt.plusMonths(3));
    }

    @DisplayName("제출이 완료되고 3개월이 경과했으면 canRetest는 true이다")
    @Test
    void getSurveyStatus_whenSubmittedOver3Months_returnsCanRetestTrue() {
        LocalDateTime submittedAt = LocalDateTime.now().minusMonths(4);
        SurveySubmission submission = submission(submittedAt);
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.of(submission));

        SurveyStatusResponse response = surveyService.getSurveyStatus(member);

        assertThat(response.status()).isEqualTo("SUBMITTED");
        assertThat(response.canRetest()).isTrue();
        assertThat(response.nextRetakeAt()).isEqualTo(submittedAt.plusMonths(3));
    }

    @DisplayName("최초 설문 제출 시 제출과 답변을 생성하고 점수를 submission에 기록한다")
    @Test
    void submitSurvey_createsSurveyData() {
        SurveyFixture fixture = surveyFixture("5");
        stubQuestions(fixture);
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.empty());
        given(surveySubmissionRepository.save(any(SurveySubmission.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        SurveyResultResponse response = surveyService.submitSurvey(member, fixture.request());

        ArgumentCaptor<SurveySubmission> submissionCaptor = ArgumentCaptor.forClass(SurveySubmission.class);
        then(surveySubmissionRepository).should().save(submissionCaptor.capture());
        then(surveyAnswerRepository).should().saveAll(any());
        assertThat(submissionCaptor.getValue().getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(submissionCaptor.getValue().getSubmittedAt()).isNotNull();
        assertThat(submissionCaptor.getValue().getAgreeablenessScore()).isEqualByComparingTo("5.00");
        assertThat(response.characterType()).isEqualTo(CharacterType.LEAD_RUNNER);
        assertThat(response.extroversionType()).isEqualTo(ExtroversionType.E);
        assertThat(response.agreeablenessScore()).isEqualByComparingTo("5.00");
        assertThat(response.characterXScore()).isEqualByComparingTo("15");
        assertThat(response.characterYScore()).isEqualByComparingTo("15");
        assertThat(response.axes()).hasSize(3);
        then(surveyOptionRepository).should().findAllByQuestions(fixture.questions());
    }

    @DisplayName("직전 제출 후 3개월이 지나면 기존 제출을 재사용하고 답변과 점수만 교체한다")
    @Test
    void resubmit_reusesSubmissionAndUpdatesScores() {
        SurveyFixture fixture = surveyFixture("5");
        stubQuestions(fixture);
        SurveySubmission submission = submission(LocalDateTime.now().minusMonths(3));
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.of(submission));

        surveyService.submitSurvey(member, fixture.request());

        then(surveySubmissionRepository).should(never()).save(any(SurveySubmission.class));
        then(surveyAnswerRepository).should().deleteAllBySubmission(submission);
        then(surveyAnswerRepository).should().flush();
        then(surveyAnswerRepository).should().saveAll(any());
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(submission.getSubmittedAt()).isNotNull();
        assertThat(submission.getAgreeablenessScore()).isEqualByComparingTo("5.00");
    }

    @DisplayName("직전 제출 후 3개월이 지나지 않으면 재응시할 수 없다")
    @Test
    void resubmit_rejectsRetakeWithinThreeMonths() {
        SurveyFixture fixture = surveyFixture("5");
        stubQuestions(fixture);
        SurveySubmission submission =
                submission(LocalDateTime.now().minusMonths(3).plusDays(1));
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.of(submission));

        assertSurveyError(fixture.request(), SurveyErrorCode.RETAKE_NOT_ALLOWED);

        then(surveyAnswerRepository).shouldHaveNoInteractions();
        then(surveySubmissionRepository).should(never()).save(any(SurveySubmission.class));
    }

    @DisplayName("필수 질문의 답변이 누락되면 제출할 수 없다")
    @Test
    void submitSurvey_rejectsMissingRequiredAnswer() {
        SurveyFixture fixture = surveyFixture("3");
        stubQuestions(fixture);
        SubmitSurveyRequest request =
                new SubmitSurveyRequest(fixture.request().answers().subList(0, QUESTION_KEYS.size() - 1));

        assertSurveyError(request, SurveyErrorCode.MISSING_REQUIRED_ANSWER);
        then(surveySubmissionRepository).shouldHaveNoInteractions();
    }

    @DisplayName("점수 계산에 필요한 질문 키가 없으면 제출할 수 없다")
    @Test
    void submitSurvey_rejectsMissingScoreQuestionKey() {
        SurveyFixture fixture = surveyFixture("3");
        fixture.questions().removeLast();
        fixture.options().removeLast();
        SubmitSurveyRequest request =
                new SubmitSurveyRequest(fixture.request().answers().subList(0, QUESTION_KEYS.size() - 1));
        stubQuestions(fixture);

        assertSurveyError(request, SurveyErrorCode.QUESTION_NOT_FOUND);
        then(surveySubmissionRepository).should(never()).save(any(SurveySubmission.class));
    }

    @DisplayName("존재하지 않는 질문 ID가 포함되면 제출할 수 없다")
    @Test
    void submitSurvey_rejectsUnknownQuestion() {
        SurveyFixture fixture = surveyFixture("3");
        stubQuestions(fixture);
        List<AnswerRequest> answers = new ArrayList<>(fixture.request().answers());
        answers.add(new AnswerRequest(999L, fixture.options().getFirst().getOptionId()));

        assertSurveyError(new SubmitSurveyRequest(answers), SurveyErrorCode.QUESTION_NOT_FOUND);
    }

    @DisplayName("존재하지 않는 선택지 ID가 포함되면 제출할 수 없다")
    @Test
    void submitSurvey_rejectsUnknownOption() {
        SurveyFixture fixture = surveyFixture("3");
        stubQuestions(fixture);
        List<AnswerRequest> answers = new ArrayList<>(fixture.request().answers());
        answers.set(0, new AnswerRequest(fixture.questions().getFirst().getQuestionId(), 999L));

        assertSurveyError(new SubmitSurveyRequest(answers), SurveyErrorCode.OPTION_NOT_FOUND);
    }

    @DisplayName("다른 질문에 속한 선택지를 선택하면 제출할 수 없다")
    @Test
    void submitSurvey_rejectsOptionFromAnotherQuestion() {
        SurveyFixture fixture = surveyFixture("3");
        stubQuestions(fixture);
        List<AnswerRequest> answers = new ArrayList<>(fixture.request().answers());
        answers.set(
                0,
                new AnswerRequest(
                        fixture.questions().getFirst().getQuestionId(),
                        fixture.options().get(1).getOptionId()));

        assertSurveyError(new SubmitSurveyRequest(answers), SurveyErrorCode.OPTION_NOT_BELONG_TO_QUESTION);
    }

    @DisplayName("저장된 성향 결과를 조회한다")
    @Test
    void getResult_returnsStoredProfile() {
        SurveySubmission submission = submittedSubmissionWithScores();
        CurrentCharacterResponse character = new CurrentCharacterResponse(
                CharacterType.FREE_RUNNER,
                "프리러너",
                CharacterPalette.DEFAULT,
                "정해진 길보다 나만의 방식으로 답을 찾아요!",
                List.of("유연함"),
                List.of("상황에 맞게 방향을 바꾸며 답을 찾아가는 러너"),
                submission.getSubmittedAt(),
                submission.getUpdatedAt());
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.of(submission));
        given(characterService.getCurrentCharacter(member)).willReturn(character);

        SurveyResultResponse response = surveyService.getResult(member);

        assertThat(response.characterType()).isEqualTo(CharacterType.FREE_RUNNER);
        assertThat(response.extroversionType()).isEqualTo(ExtroversionType.I);
        assertThat(response.characterXScore()).isEqualByComparingTo("5.3");
        assertThat(response.characterYScore()).isEqualByComparingTo("7.2");
        assertThat(response.agreeablenessScore()).isEqualByComparingTo("1.1");
        assertThat(response.conscientiousnessScore()).isEqualByComparingTo("1.2");
        assertThat(response.honestyHumilityScore()).isEqualByComparingTo("1.3");
        assertThat(response.extroversionScore()).isEqualByComparingTo("1.4");
        assertThat(response.goalPreferenceScore()).isEqualByComparingTo("2.1");
        assertThat(response.workStyleScore()).isEqualByComparingTo("2.2");
        assertThat(response.communicationStyleScore()).isEqualByComparingTo("2.3");
        assertThat(submission.getExtroversion2Score()).isEqualByComparingTo("2.4");
        assertThat(submission.getExtroversion3Score()).isEqualByComparingTo("2.5");
        assertThat(response.axes()).hasSize(3);
        assertThat(response.character()).isSameAs(character);
    }

    @DisplayName("저장된 성향 결과가 없으면 예외가 발생한다")
    @Test
    void getResult_throwsWhenProfileDoesNotExist() {
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.empty());

        assertThatThrownBy(() -> surveyService.getResult(member))
                .isInstanceOf(SurveyException.class)
                .extracting(exception -> ((SurveyException) exception).getErrorCode())
                .isEqualTo(SurveyErrorCode.SURVEY_NOT_SUBMITTED);
    }

    private void assertSurveyError(SubmitSurveyRequest request, SurveyErrorCode expectedErrorCode) {
        assertThatThrownBy(() -> surveyService.submitSurvey(member, request))
                .isInstanceOf(SurveyException.class)
                .extracting(exception -> ((SurveyException) exception).getErrorCode())
                .isEqualTo(expectedErrorCode);
    }

    private void stubQuestions(SurveyFixture fixture) {
        given(surveyQuestionRepository.findAllByOrderByDisplayOrderAsc()).willReturn(fixture.questions());
        given(surveyOptionRepository.findAllByQuestions(fixture.questions())).willReturn(fixture.options());
    }

    private SurveyFixture surveyFixture(String score) {
        List<SurveyQuestion> questions = new ArrayList<>();
        List<SurveyOption> options = new ArrayList<>();
        List<AnswerRequest> answers = new ArrayList<>();
        for (int index = 0; index < QUESTION_KEYS.size(); index++) {
            long questionId = index + 1L;
            SurveyQuestion question = SurveyQuestion.builder()
                    .questionId(questionId)
                    .questionKey(QUESTION_KEYS.get(index))
                    .questionText(QUESTION_KEYS.get(index))
                    .questionType(QuestionType.RATING)
                    .isRequired(true)
                    .displayOrder(index + 1)
                    .build();
            SurveyOption option = SurveyOption.builder()
                    .optionId(100L + questionId)
                    .question(question)
                    .optionKey("RATING_" + score)
                    .optionLabel(score)
                    .optionValue(score)
                    .displayOrder(Integer.parseInt(score))
                    .scoreWeight(new BigDecimal(score))
                    .build();
            questions.add(question);
            options.add(option);
            answers.add(new AnswerRequest(questionId, option.getOptionId()));
        }
        return new SurveyFixture(questions, options, new SubmitSurveyRequest(answers));
    }

    private SurveySubmission submission() {
        return submission(null);
    }

    private SurveySubmission submission(LocalDateTime submittedAt) {
        return SurveySubmission.builder()
                .surveySubmissionId(10L)
                .member(member)
                .status(SubmissionStatus.SUBMITTED)
                .submittedAt(submittedAt)
                .build();
    }

    private SurveySubmission submittedSubmissionWithScores() {
        SurveySubmission submission = SurveySubmission.builder()
                .surveySubmissionId(10L)
                .member(member)
                .status(SubmissionStatus.SUBMITTED)
                .build();
        SurveyScoreSnapshot snapshot = SurveyScoreSnapshot.builder()
                .agreeablenessScore(new BigDecimal("1.1"))
                .conscientiousnessScore(new BigDecimal("1.2"))
                .honestyHumilityScore(new BigDecimal("1.3"))
                .extroversionScore(new BigDecimal("1.4"))
                .goalPreferenceScore(new BigDecimal("2.1"))
                .workStyleScore(new BigDecimal("2.2"))
                .communicationStyleScore(new BigDecimal("2.3"))
                .extroversion2Score(new BigDecimal("2.4"))
                .extroversion3Score(new BigDecimal("2.5"))
                .extroversionType(ExtroversionType.I)
                .characterType(CharacterType.FREE_RUNNER)
                .characterXScore(new BigDecimal("5.3"))
                .characterYScore(new BigDecimal("7.2"))
                .build();
        submission.recordScores(snapshot);
        return submission;
    }

    private record SurveyFixture(
            List<SurveyQuestion> questions, List<SurveyOption> options, SubmitSurveyRequest request) {}
}
