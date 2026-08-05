package org.cotato.gongmozip.domains.contest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestSummaryResponse;
import org.cotato.gongmozip.domains.contest.dto.response.RecommendationResponse.RecommendationReasonResponse;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestErrorCode;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestSuccessCode;
import org.cotato.gongmozip.domains.contest.service.ContestRecommendationService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Contest Recommendation", description = "공모전 AI 추천 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContestRecommendationController {

    private final ContestRecommendationService recommendationService;

    @Operation(summary = "홈 추천 공모전 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @GetMapping("/recommendations/contests")
    public ResponseEntity<BaseResponse<List<ContestSummaryResponse>>> getHomeRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ContestSummaryResponse> response = recommendationService.getHomeRecommendations(userDetails.getMemberId());
        return BaseResponseFormatter.success(ContestSuccessCode.RECOMMENDED_CONTESTS_RETRIEVED, response);
    }

    @Operation(summary = "프로필별 추천 공모전 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @GetMapping("/profiles/{profileId}/contest-recommendations")
    public ResponseEntity<BaseResponse<List<ContestSummaryResponse>>> getProfileRecommendations(
            @PathVariable("profileId") Long profileId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ContestSummaryResponse> response =
                recommendationService.getProfileRecommendations(profileId, userDetails.getMemberId());
        return BaseResponseFormatter.success(ContestSuccessCode.RECOMMENDED_CONTESTS_RETRIEVED, response);
    }

    @Operation(summary = "추천 공모전 사유 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @GetMapping("/recommendations/contests/{contestId}/reason")
    public ResponseEntity<BaseResponse<RecommendationReasonResponse>> getRecommendationReason(
            @PathVariable("contestId") Long contestId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecommendationReasonResponse response =
                recommendationService.getRecommendationReason(contestId, userDetails.getMemberId());
        return BaseResponseFormatter.success(ContestSuccessCode.RECOMMENDATION_REASON_RETRIEVED, response);
    }
}
