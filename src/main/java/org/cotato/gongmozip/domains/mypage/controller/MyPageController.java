package org.cotato.gongmozip.domains.mypage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.*;
import org.cotato.gongmozip.domains.mypage.exception.codes.MyPageErrorCode;
import org.cotato.gongmozip.domains.mypage.exception.codes.MyPageSuccessCode;
import org.cotato.gongmozip.domains.mypage.service.MyPageService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyPage", description = "마이페이지 관련 API")
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @Operation(summary = "마이페이지 메인 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MyPageErrorCode.class)
    @GetMapping
    public ResponseEntity<BaseResponse<MyPageMainResponse>> getMyPageMain(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MyPageMainResponse response = myPageService.getMyPageMain(userDetails.getMemberId());
        return BaseResponseFormatter.success(MyPageSuccessCode.MYPAGE_MAIN_RETRIEVED, response);
    }

    @Operation(summary = "진행 중 프로젝트 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MyPageErrorCode.class)
    @GetMapping("/projects/ongoing")
    public ResponseEntity<BaseResponse<OngoingProjectsResponse>> getOngoingProjects(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OngoingProjectsResponse response = myPageService.getOngoingProjects(userDetails.getMemberId(), page, size);
        return BaseResponseFormatter.success(MyPageSuccessCode.ONGOING_PROJECTS_RETRIEVED, response);
    }

    @Operation(summary = "완료 프로젝트 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MyPageErrorCode.class)
    @GetMapping("/projects/completed")
    public ResponseEntity<BaseResponse<CompletedProjectsResponse>> getCompletedProjects(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CompletedProjectsResponse response = myPageService.getCompletedProjects(userDetails.getMemberId(), page, size);
        return BaseResponseFormatter.success(MyPageSuccessCode.COMPLETED_PROJECTS_RETRIEVED, response);
    }

    @Operation(summary = "받은 팀원 후기 키워드 통계 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MyPageErrorCode.class)
    @GetMapping("/reviews")
    public ResponseEntity<BaseResponse<ReviewStatisticsResponse>> getReviewStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ReviewStatisticsResponse response = myPageService.getReviewStatistics(userDetails.getMemberId());
        return BaseResponseFormatter.success(MyPageSuccessCode.REVIEWS_RETRIEVED, response);
    }

    @Operation(summary = "내 스크랩 공모전 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MyPageErrorCode.class)
    @GetMapping("/scrapped-contests")
    public ResponseEntity<BaseResponse<ScrappedContestsResponse>> getScrappedContests(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ScrappedContestsResponse response = myPageService.getScrappedContests(userDetails.getMemberId(), page, size);
        return BaseResponseFormatter.success(MyPageSuccessCode.SCRAPPED_CONTESTS_RETRIEVED, response);
    }
}
