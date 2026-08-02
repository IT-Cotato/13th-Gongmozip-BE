package org.cotato.gongmozip.domains.profile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.ProjectEvaluationRequest;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.ProjectEvaluationCreateResponse;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.ProjectEvaluationResponse;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileErrorCode;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileSuccessCode;
import org.cotato.gongmozip.domains.profile.service.ProjectEvaluationService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project Evaluation", description = "프로젝트 경험 AI 평가 API")
@RestController
@RequestMapping("/api/ai/project-evaluations")
@RequiredArgsConstructor
public class ProjectEvaluationController {

    private final ProjectEvaluationService projectEvaluationService;
    private final MemberRepository memberRepository;

    private Member getAuthenticatedMember(CustomUserDetails userDetails) {
        return memberRepository
                .findById(userDetails.getMemberId())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Operation(
            summary = "프로젝트 경험 AI 평가 생성 및 재생성",
            description =
                    """
                    프로젝트 경험 내용을 AI로 분석하여 역량 평가 점수 생성을 비동기로 요청합니다.

                    기존 평가가 있는 상태에서 재요청 시, 기존 결과를 지우고 새로 평가하여 덮어씁니다.
                    분석 진행 중(PENDING, PROCESSING)에 중복 요청 시 409 Conflict 예외가 발생합니다.
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PostMapping
    public ResponseEntity<BaseResponse<ProjectEvaluationCreateResponse>> evaluateProject(
            @RequestBody @Valid ProjectEvaluationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        Long evaluationId = projectEvaluationService.evaluateProject(request.projectExperienceId(), member);
        return BaseResponseFormatter.success(
                ProfileSuccessCode.PROJECT_EVALUATION_REQUESTED, new ProjectEvaluationCreateResponse(evaluationId));
    }

    @Operation(
            summary = "프로젝트 경험 AI 평가 결과 및 상태 조회",
            description =
                    """
                    프로젝트 경험 AI 평가의 진행 상태(status: PENDING, PROCESSING, COMPLETED, FAILED) 및
                    완료된 경우 평가 점수(score: 100점 만점)와 피드백 내용을 조회합니다.
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/{evaluationId}")
    public ResponseEntity<BaseResponse<ProjectEvaluationResponse>> getEvaluation(
            @PathVariable("evaluationId") Long evaluationId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProjectEvaluationResponse response = projectEvaluationService.getEvaluation(evaluationId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROJECT_EVALUATION_RETRIEVED, response);
    }
}
