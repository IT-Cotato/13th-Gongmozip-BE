package org.cotato.gongmozip.domains.survey.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.survey.dto.request.SurveyRequest.AnswerRequest;
import org.cotato.gongmozip.domains.survey.dto.request.SurveyRequest.SubmitSurveyRequest;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.AxisResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.OptionResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.QuestionListResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.QuestionResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.SurveyResultResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.SurveyStatusResponse;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.survey.enums.QuestionType;
import org.cotato.gongmozip.domains.survey.exception.SurveyException;
import org.cotato.gongmozip.domains.survey.exception.codes.SurveyErrorCode;
import org.cotato.gongmozip.domains.survey.service.SurveyService;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SurveyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SurveyService surveyService;

    @MockitoBean
    private MemberRepository memberRepository;

    private final Member member =
            Member.builder().memberId(1L).email("member@example.com").build();
    private final CustomUserDetails userDetails = new CustomUserDetails(member);

    @DisplayName("인증된 사용자는 설문 질문 목록을 조회할 수 있다")
    @Test
    void getQuestions_returnsQuestionList() throws Exception {
        QuestionResponse question = new QuestionResponse(
                1L,
                "AGREEABLENESS_1",
                "질문",
                QuestionType.RATING,
                1,
                List.of(new OptionResponse(10L, "RATING_5", "매우 그렇다", "5", 5)));
        given(surveyService.getQuestions()).willReturn(new QuestionListResponse(List.of(question)));

        mockMvc.perform(get("/api/survey/questions").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SURVEY_001"))
                .andExpect(jsonPath("$.data.questions[0].questionId").value(1L))
                .andExpect(jsonPath("$.data.questions[0].options[0].optionId").value(10L));
    }

    @DisplayName("비인증 사용자는 설문 질문 목록을 조회할 수 없다")
    @Test
    void getQuestions_rejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/survey/questions")).andExpect(status().isUnauthorized());
    }

    @DisplayName("인증된 사용자는 자신의 설문 제출 상태를 조회할 수 있다")
    @Test
    void getStatus_returnsMemberSurveyStatus() throws Exception {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(surveyService.getSurveyStatus(member)).willReturn(new SurveyStatusResponse("SUBMITTED"));

        mockMvc.perform(get("/api/survey/status").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SURVEY_002"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @DisplayName("유효한 답변을 제출하면 분석 결과를 반환한다")
    @Test
    void submitSurvey_returnsAnalysisResult() throws Exception {
        SubmitSurveyRequest request = new SubmitSurveyRequest(List.of(new AnswerRequest(1L, 10L)));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(surveyService.submitSurvey(any(Member.class), any(SubmitSurveyRequest.class)))
                .willReturn(resultResponse());

        mockMvc.perform(post("/api/survey/submit")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SURVEY_003"))
                .andExpect(jsonPath("$.data.characterType").value("LEAD_RUNNER"))
                .andExpect(jsonPath("$.data.extroversionType").value("E"))
                .andExpect(jsonPath("$.data.axes.length()").value(3));
    }

    @DisplayName("답변 목록이 비어 있으면 400 응답을 반환한다")
    @Test
    void submitSurvey_rejectsEmptyAnswers() throws Exception {
        SubmitSurveyRequest request = new SubmitSurveyRequest(List.of());

        mockMvc.perform(post("/api/survey/submit")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));
    }

    @DisplayName("직전 제출 후 3개월 이내에 재응시하면 안내 메시지와 409 응답을 반환한다")
    @Test
    void submitSurvey_rejectsRetakeWithinThreeMonths() throws Exception {
        SubmitSurveyRequest request = new SubmitSurveyRequest(List.of(new AnswerRequest(1L, 10L)));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(surveyService.submitSurvey(any(Member.class), any(SubmitSurveyRequest.class)))
                .willThrow(new SurveyException(SurveyErrorCode.RETAKE_NOT_ALLOWED));

        mockMvc.perform(post("/api/survey/submit")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SURVEY_006"))
                .andExpect(jsonPath("$.message").value("협업 유형 검사는 3개월에 한 번만 재응시할 수 있습니다."));
    }

    @DisplayName("설문 결과가 없으면 404 응답을 반환한다")
    @Test
    void getResult_returnsNotFoundWhenSurveyWasNotSubmitted() throws Exception {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(surveyService.getResult(member)).willThrow(new SurveyException(SurveyErrorCode.SURVEY_NOT_SUBMITTED));

        mockMvc.perform(get("/api/survey/result").with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SURVEY_001"));
    }

    private SurveyResultResponse resultResponse() {
        BigDecimal score = new BigDecimal("5.00");
        return new SurveyResultResponse(
                CharacterType.LEAD_RUNNER,
                ExtroversionType.E,
                new BigDecimal("15.00"),
                new BigDecimal("15.00"),
                score,
                score,
                score,
                score,
                score,
                score,
                score,
                List.of(
                        new AxisResponse("즉흥형", "계획형", score),
                        new AxisResponse("독립형", "조율형", score),
                        new AxisResponse("신중형", "추진형", score)));
    }
}
