package org.cotato.gongmozip.domains.contest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.contest.dto.request.ContestRequest.AddContestCandidateRequest;
import org.cotato.gongmozip.domains.contest.dto.request.ContestRequest.SubmitContestVoteRequest;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestCandidateItemResponse;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestCandidateListResponse;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestVoteStatusResponse;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestErrorCode;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestSuccessCode;
import org.cotato.gongmozip.domains.contest.service.ContestVotingService;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ContestVoting", description = "팀 공모전 후보/투표 관련 API")
@RestController
@RequestMapping("/api/teams/{teamId}/contest-candidates")
@RequiredArgsConstructor
public class ContestVotingController {

    private final ContestVotingService contestVotingService;

    @Operation(summary = "후보 공모전 추가")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {ContestErrorCode.class, TeamErrorCode.class})
    @PostMapping
    public ResponseEntity<BaseResponse<ContestCandidateItemResponse>> addCandidate(
            @PathVariable("teamId") Long teamId,
            @RequestBody @Valid AddContestCandidateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ContestCandidateItemResponse response =
                contestVotingService.addCandidate(teamId, userDetails.getMemberId(), request.contestId());
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_CANDIDATE_ADDED, response);
    }

    @Operation(summary = "후보 공모전 리스트 조회")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {ContestErrorCode.class, TeamErrorCode.class})
    @GetMapping
    public ResponseEntity<BaseResponse<ContestCandidateListResponse>> getCandidates(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ContestCandidateListResponse response = contestVotingService.getCandidates(teamId, userDetails.getMemberId());
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_CANDIDATE_LIST_RETRIEVED, response);
    }

    @Operation(summary = "후보 공모전 삭제")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {ContestErrorCode.class, TeamErrorCode.class})
    @DeleteMapping("/{contestCandidateId}")
    public ResponseEntity<BaseResponse<Void>> removeCandidate(
            @PathVariable("teamId") Long teamId,
            @PathVariable("contestCandidateId") Long contestCandidateId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        contestVotingService.removeCandidate(teamId, userDetails.getMemberId(), contestCandidateId);
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_CANDIDATE_REMOVED);
    }

    @Operation(summary = "공모전 투표 (최대 2개 선택)")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {ContestErrorCode.class, TeamErrorCode.class})
    @PostMapping("/votes")
    public ResponseEntity<BaseResponse<Void>> submitVote(
            @PathVariable("teamId") Long teamId,
            @RequestBody @Valid SubmitContestVoteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        contestVotingService.submitVote(teamId, userDetails.getMemberId(), request.contestCandidateIds());
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_VOTE_SUBMITTED);
    }

    @Operation(
            summary = "공모전 투표 진행 상황 조회",
            description = "현재 라운드에 참여한 인원 수와 후보별 득표수를 조회합니다. 전원이 투표를 마치기 전에도 호출할 수 있습니다.")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {ContestErrorCode.class, TeamErrorCode.class})
    @GetMapping("/votes")
    public ResponseEntity<BaseResponse<ContestVoteStatusResponse>> getVoteStatus(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ContestVoteStatusResponse response = contestVotingService.getVoteStatus(teamId, userDetails.getMemberId());
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_VOTE_STATUS_RETRIEVED, response);
    }
}
