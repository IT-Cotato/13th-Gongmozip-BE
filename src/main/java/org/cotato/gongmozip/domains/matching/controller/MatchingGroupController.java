package org.cotato.gongmozip.domains.matching.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingGroupResponse.AcceptResponse;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingGroupResponse.GroupResponsesResponse;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingSuccessCode;
import org.cotato.gongmozip.domains.matching.service.MatchingGroupQueryService;
import org.cotato.gongmozip.domains.matching.service.MatchingResponseService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Matching", description = "매칭 그룹 응답 API")
@RestController
@RequestMapping("/api/matching/groups")
@RequiredArgsConstructor
public class MatchingGroupController {

    private final MatchingGroupQueryService matchingGroupQueryService;
    private final MatchingResponseService matchingResponseService;

    @Operation(summary = "매칭 그룹 응답 현황 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @GetMapping("/{matchingGroupId}/responses")
    public ResponseEntity<BaseResponse<GroupResponsesResponse>> getResponses(
            @PathVariable Long matchingGroupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponsesResponse response =
                matchingGroupQueryService.getResponses(userDetails.getMemberId(), matchingGroupId);
        return BaseResponseFormatter.success(MatchingSuccessCode.MATCHING_GROUP_RESPONSES_RETRIEVED, response);
    }

    @Operation(summary = "매칭 결과 수락")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @PostMapping("/{matchingGroupId}/accept")
    public ResponseEntity<BaseResponse<AcceptResponse>> accept(
            @PathVariable Long matchingGroupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        AcceptResponse response = matchingResponseService.accept(userDetails.getMemberId(), matchingGroupId);
        return BaseResponseFormatter.success(MatchingSuccessCode.MATCHING_RESPONSE_ACCEPTED, response);
    }
}
