package org.cotato.gongmozip.domains.contest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.contest.dto.request.ContestRequest.CreateContestRequest;
import org.cotato.gongmozip.domains.contest.dto.request.ContestRequest.UpdateContestRequest;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.*;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestErrorCode;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestSuccessCode;
import org.cotato.gongmozip.domains.contest.service.ContestService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Contest", description = "공모전 및 스크랩 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;
    private final MemberRepository memberRepository;

    private Member getAuthenticatedMember(CustomUserDetails userDetails) {
        return memberRepository
                .findById(userDetails.getMemberId())
                .orElseThrow(() -> new org.cotato.gongmozip.domains.member.exception.MemberException(
                        MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Operation(summary = "관리자용 공모전 등록")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @PostMapping("/contests")
    public ResponseEntity<BaseResponse<ContestCreateResponse>> createContest(
            @RequestBody @Valid CreateContestRequest request) {
        ContestCreateResponse response = contestService.createContest(request);
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_CREATED, response);
    }

    @Operation(summary = "관리자용 공모전 수정")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @PatchMapping("/contests/{contestId}")
    public ResponseEntity<BaseResponse<ContestUpdateResponse>> updateContest(
            @PathVariable("contestId") Long contestId, @RequestBody @Valid UpdateContestRequest request) {
        ContestUpdateResponse response = contestService.updateContest(contestId, request);
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_UPDATED, response);
    }

    @Operation(summary = "관리자용 공모전 삭제")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @DeleteMapping("/contests/{contestId}")
    public ResponseEntity<BaseResponse<Void>> deleteContest(@PathVariable("contestId") Long contestId) {
        contestService.deleteContest(contestId);
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_DELETED);
    }

    @Operation(summary = "공모전 목록 조회 (검색 및 필터링)")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @GetMapping("/contests")
    public ResponseEntity<BaseResponse<ContestListResponse>> getContests(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", defaultValue = "deadlineAsc") String sort,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "20") Integer size) {
        ContestListResponse response = contestService.getContests(keyword, category, status, sort, page, size);
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_LIST_RETRIEVED, response);
    }

    @Operation(summary = "공모전 상세 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @GetMapping("/contests/{contestId}")
    public ResponseEntity<BaseResponse<ContestDetailResponse>> getContestDetail(
            @PathVariable("contestId") Long contestId) {
        ContestDetailResponse response = contestService.getContestDetail(contestId);
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_RETRIEVED, response);
    }

    @Operation(summary = "공모전 스크랩 등록")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @PostMapping("/contests/{contestId}/scraps")
    public ResponseEntity<BaseResponse<ScrapResponse>> scrapContest(
            @PathVariable("contestId") Long contestId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ScrapResponse response = contestService.scrapContest(contestId, member);
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_SCRAPPED, response);
    }

    @Operation(summary = "공모전 스크랩 취소")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @DeleteMapping("/contests/{contestId}/scraps")
    public ResponseEntity<BaseResponse<Void>> unscrapContest(
            @PathVariable("contestId") Long contestId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        contestService.unscrapContest(contestId, member);
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_UNSCRAPPED);
    }

    @Operation(summary = "공모전 스크랩 여부 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @GetMapping("/contests/{contestId}/scrap-status")
    public ResponseEntity<BaseResponse<ScrapStatusResponse>> getScrapStatus(
            @PathVariable("contestId") Long contestId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ScrapStatusResponse response = contestService.getScrapStatus(contestId, member);
        return BaseResponseFormatter.success(ContestSuccessCode.SCRAP_STATUS_RETRIEVED, response);
    }

    // 기능명세서 3.4.2 채팅방에 공유하기
    @Operation(summary = "공유용 공모전 정보 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ContestErrorCode.class)
    @GetMapping("/contests/{contestId}/share-preview")
    public ResponseEntity<BaseResponse<SharePreviewResponse>> getSharePreview(
            @PathVariable("contestId") Long contestId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 인증된 유저만 호출하도록 설정
        getAuthenticatedMember(userDetails);
        SharePreviewResponse response = contestService.getSharePreview(contestId);
        return BaseResponseFormatter.success(ContestSuccessCode.SHARE_PREVIEW_RETRIEVED, response);
    }
}
