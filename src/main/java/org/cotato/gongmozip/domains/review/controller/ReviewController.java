package org.cotato.gongmozip.domains.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.review.dto.request.ReviewRequest.WriteReviewRequest;
import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewResultResponse;
import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewTargetListResponse;
import org.cotato.gongmozip.domains.review.exception.codes.ReviewErrorCode;
import org.cotato.gongmozip.domains.review.exception.codes.ReviewSuccessCode;
import org.cotato.gongmozip.domains.review.service.ReviewService;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
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

@Tag(name = "Review", description = "팀원 리뷰 관련 API")
@RestController
@RequestMapping("/api/teams/{teamId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "팀원 리뷰 작성 (팀 SUBMITTED 상태에서만 가능)")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {ReviewErrorCode.class, TeamErrorCode.class})
    @PostMapping
    public ResponseEntity<BaseResponse<ReviewResultResponse>> writeReview(
            @PathVariable("teamId") Long teamId,
            @RequestBody @Valid WriteReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ReviewResultResponse response = reviewService.writeReview(teamId, userDetails.getMemberId(), request);
        return BaseResponseFormatter.success(ReviewSuccessCode.REVIEW_SUBMITTED, response);
    }

    @Operation(summary = "리뷰 대상 팀원 목록 조회 (이미 작성한 팀원은 alreadyReviewed=true로 표시)")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {TeamErrorCode.class})
    @GetMapping("/targets")
    public ResponseEntity<BaseResponse<ReviewTargetListResponse>> getReviewTargets(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ReviewTargetListResponse response = reviewService.getReviewTargets(teamId, userDetails.getMemberId());
        return BaseResponseFormatter.success(ReviewSuccessCode.REVIEW_TARGETS_FETCHED, response);
    }
}
