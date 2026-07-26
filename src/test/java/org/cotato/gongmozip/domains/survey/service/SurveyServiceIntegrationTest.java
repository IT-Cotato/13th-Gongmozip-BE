package org.cotato.gongmozip.domains.survey.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.survey.dto.request.SurveyRequest.AnswerRequest;
import org.cotato.gongmozip.domains.survey.dto.request.SurveyRequest.SubmitSurveyRequest;
import org.cotato.gongmozip.domains.survey.entity.PersonalityProfile;
import org.cotato.gongmozip.domains.survey.entity.SurveyOption;
import org.cotato.gongmozip.domains.survey.entity.SurveyQuestion;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;
import org.cotato.gongmozip.domains.survey.enums.QuestionType;
import org.cotato.gongmozip.domains.survey.repository.PersonalityProfileRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveyAnswerRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveyOptionRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveyQuestionRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveySubmissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SurveyServiceIntegrationTest {

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

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SurveyQuestionRepository surveyQuestionRepository;

    @Autowired
    private SurveyOptionRepository surveyOptionRepository;

    @Autowired
    private SurveySubmissionRepository surveySubmissionRepository;

    @Autowired
    private SurveyAnswerRepository surveyAnswerRepository;

    @Autowired
    private PersonalityProfileRepository personalityProfileRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("재설문해도 회원별 제출과 성향 결과는 하나만 유지되고 답변은 교체된다")
    @Test
    void resubmit_replacesAnswersAndUpdatesExistingRows() {
        surveyOptionRepository.deleteAllInBatch();
        surveyQuestionRepository.deleteAllInBatch();

        Member member = memberRepository.save(Member.builder()
                .email("survey-integration@example.com")
                .status(MemberStatus.ACTIVE)
                .build());
        SurveyRequests requests = saveSurveyQuestionsAndOptions();

        surveyService.submitSurvey(member, requests.lowScoreRequest());
        entityManager.flush();

        SurveySubmission firstSubmission =
                surveySubmissionRepository.findByMember(member).orElseThrow();
        PersonalityProfile firstProfile = personalityProfileRepository
                .findTopByMemberOrderByProfileIdDesc(member)
                .orElseThrow();
        Long submissionId = firstSubmission.getSurveySubmissionId();
        Long profileId = firstProfile.getProfileId();
        assertThat(surveyAnswerRepository.findBySubmission(firstSubmission)).hasSize(15);
        assertThat(firstProfile.getAgreeablenessScore()).isEqualByComparingTo("1.00");

        surveyService.submitSurvey(member, requests.highScoreRequest());
        entityManager.flush();
        entityManager.clear();

        Member persistedMember = memberRepository.findById(member.getMemberId()).orElseThrow();
        SurveySubmission updatedSubmission =
                surveySubmissionRepository.findByMember(persistedMember).orElseThrow();
        PersonalityProfile updatedProfile = personalityProfileRepository
                .findTopByMemberOrderByProfileIdDesc(persistedMember)
                .orElseThrow();

        assertThat(updatedSubmission.getSurveySubmissionId()).isEqualTo(submissionId);
        assertThat(updatedProfile.getProfileId()).isEqualTo(profileId);
        assertThat(surveyAnswerRepository.findBySubmission(updatedSubmission))
                .hasSize(15)
                .allSatisfy(answer ->
                        assertThat(answer.getSelectedOption().getScoreWeight()).isEqualByComparingTo("5"));
        assertThat(updatedProfile.getAgreeablenessScore()).isEqualByComparingTo("5.00");
        assertThat(updatedProfile.getCharacterXScore()).isEqualByComparingTo("15");
        assertThat(updatedProfile.getCharacterYScore()).isEqualByComparingTo("15");
    }

    private SurveyRequests saveSurveyQuestionsAndOptions() {
        List<AnswerRequest> lowScoreAnswers = new ArrayList<>();
        List<AnswerRequest> highScoreAnswers = new ArrayList<>();

        for (int index = 0; index < QUESTION_KEYS.size(); index++) {
            SurveyQuestion question = surveyQuestionRepository.save(SurveyQuestion.builder()
                    .questionKey(QUESTION_KEYS.get(index))
                    .questionText(QUESTION_KEYS.get(index))
                    .questionType(QuestionType.RATING)
                    .isRequired(true)
                    .displayOrder(index + 1)
                    .build());
            SurveyOption lowScoreOption = surveyOptionRepository.save(SurveyOption.builder()
                    .question(question)
                    .optionKey("RATING_1")
                    .optionLabel("1")
                    .optionValue("1")
                    .displayOrder(1)
                    .scoreWeight(BigDecimal.ONE)
                    .build());
            SurveyOption highScoreOption = surveyOptionRepository.save(SurveyOption.builder()
                    .question(question)
                    .optionKey("RATING_5")
                    .optionLabel("5")
                    .optionValue("5")
                    .displayOrder(5)
                    .scoreWeight(new BigDecimal("5"))
                    .build());
            lowScoreAnswers.add(new AnswerRequest(question.getQuestionId(), lowScoreOption.getOptionId()));
            highScoreAnswers.add(new AnswerRequest(question.getQuestionId(), highScoreOption.getOptionId()));
        }

        return new SurveyRequests(new SubmitSurveyRequest(lowScoreAnswers), new SubmitSurveyRequest(highScoreAnswers));
    }

    private record SurveyRequests(SubmitSurveyRequest lowScoreRequest, SubmitSurveyRequest highScoreRequest) {}
}
