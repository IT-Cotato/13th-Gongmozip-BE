package org.cotato.gongmozip.domains.team.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.LeaderCandidacyRequest;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.LeaderVoteRequest;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.exception.codes.TeamSuccessCode;
import org.cotato.gongmozip.domains.team.service.LeaderElectionService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Leader", description = "팀장 선출 관련 API")
@RestController
@RequestMapping("/api/teams/{teamId}")
@RequiredArgsConstructor
public class LeaderController {

    private final LeaderElectionService leaderElectionService;

    @Operation(summary = "팀장 여부 투표")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @PatchMapping("/leader-candidacy")
    public ResponseEntity<BaseResponse<Void>> submitCandidacy(
            @PathVariable("teamId") Long teamId,
            @RequestBody LeaderCandidacyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        leaderElectionService.submitCandidacy(teamId, userDetails.getMemberId(), request.wants());
        return BaseResponseFormatter.success(TeamSuccessCode.LEADER_CANDIDACY_SUBMITTED);
    }

    @Operation(summary = "팀장 투표")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @PostMapping("/leader-votes")
    public ResponseEntity<BaseResponse<Void>> castVote(
            @PathVariable("teamId") Long teamId,
            @RequestBody LeaderVoteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        leaderElectionService.castVote(teamId, userDetails.getMemberId(), request.candidateTeamMemberId());
        return BaseResponseFormatter.success(TeamSuccessCode.LEADER_VOTE_SUBMITTED);
    }

    @Operation(summary = "팀장 투표 동률 시 AI 추천 수락 (선착순 확정)")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @PostMapping("/leader-votes/ai-recommendation/accept")
    public ResponseEntity<BaseResponse<Void>> acceptAiRecommendation(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        leaderElectionService.acceptAiRecommendation(teamId, userDetails.getMemberId());
        return BaseResponseFormatter.success(TeamSuccessCode.AI_RECOMMENDATION_ACCEPTED);
    }

    @Operation(
            summary = "팀장 투표 동률 시 재투표 요청",
            description =
                    "동률이었던 후보들을 대상으로 재투표가 시작됐음을 팀 채팅방에 안내 카드로 알립니다. " + "실제 투표는 이 API가 아니라 팀장 투표 API를 다시 호출해 진행합니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @PostMapping("/leader-votes/revote")
    public ResponseEntity<BaseResponse<Void>> requestRevote(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        leaderElectionService.requestRevote(teamId, userDetails.getMemberId());
        return BaseResponseFormatter.success(TeamSuccessCode.LEADER_REVOTE_REQUESTED);
    }
}
