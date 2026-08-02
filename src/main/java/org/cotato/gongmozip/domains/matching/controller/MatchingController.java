package org.cotato.gongmozip.domains.matching.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderRecommendationCreateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderRecommendationDetailResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingExplanationResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingReasonCreateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingReasonDetailResponse;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingSuccessCode;
import org.cotato.gongmozip.domains.matching.service.MatchingService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Matching", description = "팀원 매칭 결과 AI 분석 및 팀장 추천 API")
@RestController
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;
    private final MemberRepository memberRepository;

    private Member getAuthenticatedMember(CustomUserDetails userDetails) {
        return memberRepository
                .findById(userDetails.getMemberId())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Operation(
            summary = "AI 분석 매칭 설명 조회",
            description = "AI 분석 매칭(HEXACO 성향 분석, 팀 시너지, 외향성 상보성 등)에 대한 정적 소개 정보를 조회합니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @GetMapping("/api/matching-explanations")
    public ResponseEntity<BaseResponse<MatchingExplanationResponse>> getExplanation() {
        MatchingExplanationResponse response = matchingService.getMatchingExplanation();
        return BaseResponseFormatter.success(MatchingSuccessCode.MATCHING_EXPLANATION_RETRIEVED, response);
    }

    @Operation(
            summary = "매칭 추천 사유 생성 및 재생성",
            description =
                    """
                    지정된 매칭 결과를 기반으로 팀의 강점, 공통점 및 성향 조화를 설명하는 AI 매칭 사유 생성을 비동기로 요청합니다.
                    기존 결과가 있을 시 재생성하여 덮어씁니다. 진행 중일 시 409 Conflict 예외가 발생합니다.
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @PostMapping("/api/ai/matching-results/{matchingResultId}/reason")
    public ResponseEntity<BaseResponse<MatchingReasonCreateResponse>> createReason(
            @PathVariable("matchingResultId") Long matchingResultId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        MatchingReasonCreateResponse response = matchingService.createMatchingReason(matchingResultId, member);
        return BaseResponseFormatter.success(MatchingSuccessCode.MATCHING_REASON_REQUESTED, response);
    }

    @Operation(summary = "매칭 추천 사유 조회", description = "지정된 매칭 결과에 생성된 AI 추천 사유 상세 내용 및 진행 상태를 조회합니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @GetMapping("/api/ai/matching-results/{matchingResultId}/reason")
    public ResponseEntity<BaseResponse<MatchingReasonDetailResponse>> getReason(
            @PathVariable("matchingResultId") Long matchingResultId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        MatchingReasonDetailResponse response = matchingService.getMatchingReason(matchingResultId, member);
        return BaseResponseFormatter.success(MatchingSuccessCode.MATCHING_REASON_RETRIEVED, response);
    }

    @Operation(
            summary = "AI 팀장 추천 생성 및 재생성",
            description =
                    """
                    팀원들의 성향 분석 분포를 기준으로 팀장 추천 후보 및 추천 사유를 비동기로 생성합니다.
                    기존 결과가 있을 시 재생성하여 덮어씁니다. 진행 중일 시 409 Conflict 예외가 발생합니다.
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @PostMapping("/api/ai/teams/{teamId}/leader-recommendation")
    public ResponseEntity<BaseResponse<LeaderRecommendationCreateResponse>> createLeaderRecommendation(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        LeaderRecommendationCreateResponse response = matchingService.createLeaderRecommendation(teamId, member);
        return BaseResponseFormatter.success(MatchingSuccessCode.LEADER_RECOMMENDATION_REQUESTED, response);
    }

    @Operation(summary = "AI 팀장 추천 결과 조회", description = "팀원 성향 분포 및 최우선 추천 팀장, 추천 사유, 순위별 후보군 목록을 조회합니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @GetMapping("/api/ai/teams/{teamId}/leader-recommendation")
    public ResponseEntity<BaseResponse<LeaderRecommendationDetailResponse>> getLeaderRecommendation(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        LeaderRecommendationDetailResponse response = matchingService.getLeaderRecommendation(teamId, member);
        return BaseResponseFormatter.success(MatchingSuccessCode.LEADER_RECOMMENDATION_RETRIEVED, response);
    }
}
