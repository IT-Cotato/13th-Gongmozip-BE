package org.cotato.gongmozip.domains.contest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.contest.dto.request.ContestRequest.ShareContestRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 기능명세서 3.4.2/5.1.4 공모전을 채팅방에 공유하기. 공유는 후보 등록과 별개 액션이다 —
// 후보로 넣으려면 공유된 카드의 "+" 버튼에서 기존 POST .../contest-candidates를 호출한다.
@Tag(name = "ContestShare", description = "공모전 채팅방 공유 API")
@RestController
@RequestMapping("/api/teams/{teamId}/contest-shares")
@RequiredArgsConstructor
public class ContestShareController {

    private final ContestVotingService contestVotingService;

    @Operation(summary = "공모전 채팅방 공유")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {ContestErrorCode.class, TeamErrorCode.class})
    @PostMapping
    public ResponseEntity<BaseResponse<Void>> shareContest(
            @PathVariable("teamId") Long teamId,
            @RequestBody @Valid ShareContestRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        contestVotingService.shareContest(teamId, userDetails.getMemberId(), request.contestId());
        return BaseResponseFormatter.success(ContestSuccessCode.CONTEST_SHARED);
    }
}
