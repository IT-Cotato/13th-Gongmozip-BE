package org.cotato.gongmozip.domains.matching.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingResultResponse.TodayMatchingResultResponse;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingSuccessCode;
import org.cotato.gongmozip.domains.matching.service.MatchingResultQueryService;
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

@Tag(name = "Matching", description = "팀원 매칭 결과 조회 API")
@RestController
@RequestMapping("/api/matching/results")
@RequiredArgsConstructor
public class MatchingResultController {

    private final MatchingResultQueryService matchingResultQueryService;

    @Operation(
            summary = "오늘의 내 매칭 결과 조회",
            description =
                    """
                    오늘 신청한 매칭의 처리 상태를 조회합니다.
                    결과 계산이 끝났더라도 `publishedAt` 전에는 그룹, 팀원, 궁합 점수를 공개하지 않습니다.
                    공개 후 팀에 배정되면 3명 또는 4명이 신청에 사용한 프로필과 신청 당시 성향, 저장된 궁합 점수를 반환하고,
                    팀 크기 계획에서 미배정되면 `UNMATCHED`를 반환합니다.
                    이 API는 결과를 읽기만 하며 수락, 패스, 재배정 또는 실제 팀 생성 상태를 변경하지 않습니다.
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @GetMapping("/me/today")
    public ResponseEntity<BaseResponse<TodayMatchingResultResponse>> getTodayResult(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TodayMatchingResultResponse response = matchingResultQueryService.getTodayResult(userDetails.getMemberId());
        return BaseResponseFormatter.success(MatchingSuccessCode.MATCHING_RESULT_RETRIEVED, response);
    }
}
