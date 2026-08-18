package org.cotato.gongmozip.domains.team.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.SubmitCompletionRequest;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.exception.codes.TeamSuccessCode;
import org.cotato.gongmozip.domains.team.service.TeamProgressService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "TeamProgress", description = "제출확인 관련 API (팀장 전용)")
@RestController
@RequestMapping("/api/teams/{teamId}")
@RequiredArgsConstructor
public class TeamProgressController {

    private final TeamProgressService teamProgressService;

    @Operation(summary = "공모전 제출 여부 확인 (팀장 전용)")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @PatchMapping("/submission")
    public ResponseEntity<BaseResponse<Void>> submitCompletion(
            @PathVariable("teamId") Long teamId,
            @RequestBody @Valid SubmitCompletionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamProgressService.submitCompletion(teamId, userDetails.getMemberId(), request.completed());
        return BaseResponseFormatter.success(TeamSuccessCode.SUBMISSION_RECORDED);
    }
}
