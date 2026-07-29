package org.cotato.gongmozip.domains.survey.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.survey.dto.request.SurveyRequest.SubmitSurveyRequest;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.QuestionListResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.SurveyResultResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.SurveyStatusResponse;
import org.cotato.gongmozip.domains.survey.exception.codes.SurveyErrorCode;
import org.cotato.gongmozip.domains.survey.exception.codes.SurveySuccessCode;
import org.cotato.gongmozip.domains.survey.service.SurveyService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Survey", description = "협업 유형 검사 API")
@RestController
@RequestMapping("/api/survey")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;
    private final MemberRepository memberRepository;

    private Member getAuthenticatedMember(CustomUserDetails userDetails) {
        return memberRepository
                .findById(userDetails.getMemberId())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Operation(
            summary = "협업 유형 검사 질문 목록 조회",
            description =
                    """
                    설문 질문 15개를 랜덤 순서로 반환합니다.

                    **테스트 순서: ① 현재 API를 먼저 호출해 questionId와 optionId를 확인하세요.**

                    응답의 `questionId`와 선택지의 `optionId`는 `/submit` 호출 시 사용합니다.
                    질문 순서는 매 요청마다 셔플되므로 고정된 순서에 의존하지 마세요.
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = SurveyErrorCode.class)
    @GetMapping("/questions")
    public ResponseEntity<BaseResponse<QuestionListResponse>> getQuestions() {
        QuestionListResponse response = surveyService.getQuestions();
        return BaseResponseFormatter.success(SurveySuccessCode.QUESTIONS_RETRIEVED, response);
    }

    @Operation(
            summary = "협업 유형 검사 제출 상태 조회",
            description =
                    """
                    현재 회원의 설문 제출 상태를 반환합니다.

                    | status | 의미 |
                    |--------|------|
                    | NONE | 한 번도 제출하지 않음 → 설문 화면으로 이동 |
                    | SUBMITTED | 제출 완료 → 결과 화면으로 이동 |

                    **테스트 순서: ② 현재 상태를 확인합니다.**
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = SurveyErrorCode.class)
    @GetMapping("/status")
    public ResponseEntity<BaseResponse<SurveyStatusResponse>> getSurveyStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        SurveyStatusResponse response = surveyService.getSurveyStatus(member);
        return BaseResponseFormatter.success(SurveySuccessCode.STATUS_RETRIEVED, response);
    }

    @Operation(
            summary = "협업 유형 검사 답변 제출",
            description =
                    """
                    15개 필수 문항에 대한 답변을 제출하고, 캐릭터 유형 분석 결과를 즉시 반환합니다.

                    **요청 형식**
                    ```json
                    {
                      "answers": [
                        { "questionId": 1, "selectedOptionId": 3 },
                        { "questionId": 2, "selectedOptionId": 7 }
                      ]
                    }
                    ```
                    - `questionId`, `selectedOptionId`는 `/questions` 응답에서 확인합니다.
                    - 15개 필수 문항을 모두 포함해야 합니다. 누락 시 `SURVEY_400_2` 에러.
                    - 선택지가 해당 질문에 속하지 않으면 `SURVEY_400_1` 에러.

                    **재제출은 직전 제출 후 3개월이 지난 시점부터 가능**합니다.
                    3개월 이내에 다시 제출하면 `SURVEY_409_1` 에러를 반환하며, 기존 답변과 결과는 유지됩니다.

                    **응답의 `axes`** — 캐릭터 유형별 3가지 성향 축 점수 (leftLabel ↔ rightLabel, score 1~5).
                    score가 5에 가까울수록 rightLabel 성향, 1에 가까울수록 leftLabel 성향입니다.

                    **테스트 순서: ③ `/questions`에서 얻은 ID로 답변을 구성해 제출합니다.**
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = SurveyErrorCode.class)
    @PostMapping("/submit")
    public ResponseEntity<BaseResponse<SurveyResultResponse>> submitSurvey(
            @RequestBody @Valid SubmitSurveyRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        SurveyResultResponse response = surveyService.submitSurvey(member, request);
        return BaseResponseFormatter.success(SurveySuccessCode.SURVEY_SUBMITTED, response);
    }

    @Operation(
            summary = "협업 유형 검사 결과 조회",
            description =
                    """
                    가장 최근에 제출한 설문 결과를 반환합니다.

                    - 설문을 제출한 적 없으면 `SURVEY_404_1` 에러를 반환합니다.
                    - 재제출한 경우 가장 최근 제출 결과를 반환합니다.

                    **테스트 순서: ④ `/submit` 후 결과가 저장됐는지 여기서 확인합니다.**
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = SurveyErrorCode.class)
    @GetMapping("/result")
    public ResponseEntity<BaseResponse<SurveyResultResponse>> getResult(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        SurveyResultResponse response = surveyService.getResult(member);
        return BaseResponseFormatter.success(SurveySuccessCode.RESULT_RETRIEVED, response);
    }
}
