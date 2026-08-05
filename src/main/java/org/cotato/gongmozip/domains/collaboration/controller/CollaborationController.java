package org.cotato.gongmozip.domains.collaboration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationDistanceResponse;
import org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationHistoryListResponse;
import org.cotato.gongmozip.domains.collaboration.exception.codes.CollaborationSuccessCode;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Collaboration Point", description = "협업거리 관련 API")
@RestController
@RequestMapping("/api/members/me/collaboration-distance")
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationPointService collaborationPointService;

    @Operation(summary = "내 협업거리 게이지 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @GetMapping
    public ResponseEntity<BaseResponse<CollaborationDistanceResponse>> getDistance(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CollaborationDistanceResponse response =
                collaborationPointService.getCollaborationDistance(userDetails.getMemberId());
        return BaseResponseFormatter.success(CollaborationSuccessCode.COLLABORATION_DISTANCE_RETRIEVED, response);
    }

    @Operation(summary = "내 협업거리 변경 내역 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @GetMapping("/histories")
    public ResponseEntity<BaseResponse<CollaborationHistoryListResponse>> getHistories(
            @org.springframework.data.web.PageableDefault(size = 10) org.springframework.data.domain.Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CollaborationHistoryListResponse response =
                collaborationPointService.getCollaborationHistories(userDetails.getMemberId(), pageable);
        return BaseResponseFormatter.success(CollaborationSuccessCode.COLLABORATION_HISTORY_RETRIEVED, response);
    }
}
