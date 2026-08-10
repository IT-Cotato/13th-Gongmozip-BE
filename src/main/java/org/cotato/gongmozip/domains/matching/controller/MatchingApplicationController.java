package org.cotato.gongmozip.domains.matching.controller;

import static org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.dto.request.MatchingApplicationRequest.ApplyRequest;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingSuccessCode;
import org.cotato.gongmozip.domains.matching.service.MatchingApplicationService;
import org.cotato.gongmozip.domains.matching.service.MatchingWithdrawalService;
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

@Tag(name = "Matching", description = "매칭 관련 API")
@RestController
@RequestMapping("/api/matching/applications")
@RequiredArgsConstructor
public class MatchingApplicationController {

    private final MatchingApplicationService matchingApplicationService;
    private final MatchingWithdrawalService matchingWithdrawalService;

    @Operation(
            summary = "현재 매칭 신청 인원 조회",
            description =
                    """
                    홈 화면의 "지금 000명이 함께할 팀을 찾고 있어요!"에 표시할 현재 매칭 신청 인원을 조회합니다.

                    대상 신청일 매칭풀에서 대기(`WAITING`) 또는 매칭 계산 중(`MATCHING`)인 신청 수를 반환합니다.
                    - 16시 결과 공개 전: 오늘 매칭풀의 신청 수
                    - 16시 결과 공개 이후: 다음 날 매칭풀에 미리 신청한 수

                    카운트다운 표시를 위한 기준 시각을 함께 반환합니다.
                    - `applicationDeadlineAt`: 대상 신청일의 신청 마감 시각(14시). 14~16시 매칭 진행 구간에는 이미 지난 시각이므로 카운트다운을 표시하지 않습니다.
                    - `resultPublishAt`: 대상 신청일의 매칭 결과 공개 시각(16시)
                    - `serverTime`: 서버 현재 시각. 클라이언트는 기기 시계 대신 `serverTime`과의 차이로 남은 시간을 계산해야 합니다.
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @GetMapping("/participant-count")
    public ResponseEntity<BaseResponse<ParticipantCountResponse>> getParticipantCount() {
        ParticipantCountResponse response = matchingApplicationService.getParticipantCount();
        return BaseResponseFormatter.success(MatchingSuccessCode.PARTICIPANT_COUNT_RETRIEVED, response);
    }

    @Operation(
            summary = "매칭 신청 자격 및 오늘 참여 현황 조회",
            description =
                    """
                    현재 회원이 오늘 매칭을 신청할 수 있는지 확인합니다. 이 API는 상태만 조회하며 신청을 생성하지 않습니다.

                    신청 가능 조건:
                    - 작성된 프로필이 한 개 이상 존재해야 합니다.
                    - 협업 유형 검사를 제출 완료해야 합니다.
                    - 한국 시간 기준 현재 시각이 당일 14시 전이어야 합니다.
                    - 취소·패스를 포함해 오늘 생성된 신청 이력이 없어야 합니다.
                    - 최근 협업거리 감소로 인한 매칭 제한 기간이 아니어야 합니다.

                    `eligible`은 위 조건을 모두 만족할 때만 `true`입니다. 만족하지 못한 조건은 `reasons`에 모두 반환합니다.
                    `participantCount`는 오늘 매칭풀에서 대기 또는 매칭 처리 중인 신청 수입니다.
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @GetMapping("/eligibility")
    public ResponseEntity<BaseResponse<EligibilityResponse>> getEligibility(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        EligibilityResponse response = matchingApplicationService.getEligibility(userDetails.getMemberId());
        return BaseResponseFormatter.success(MatchingSuccessCode.ELIGIBILITY_RETRIEVED, response);
    }

    @Operation(
            summary = "오늘의 내 매칭 신청 상태 조회",
            description =
                    """
                    오늘 생성된 내 매칭 신청과 현재 가능한 철회 방식을 조회합니다.

                    신청하지 않았다면 `appliedToday=false`, `status=NONE`으로 반환하고 나머지 신청 정보는 `null`입니다.
                    신청이 존재하면 신청 당시의 카테고리, 팀장 희망 여부, 역량 점수·그룹, 협업거리 스냅샷을 반환합니다.

                    프론트엔드는 직접 시각을 계산하지 않고 `withdrawal` 값을 사용해야 합니다.
                    - 14시 전: `FREE_CANCEL`, 예상 감점 0m
                    - 14시 이상 당일 자정 전: `PENALIZED_PASS`, 최근 7일 패스 횟수를 반영한 예상 감점 3~11m
                    - 자정 이후 또는 철회할 수 없는 상태: `withdrawable=false`
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @GetMapping("/me/today")
    public ResponseEntity<BaseResponse<TodayApplicationResponse>> getTodayApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TodayApplicationResponse response = matchingApplicationService.getTodayApplication(userDetails.getMemberId());
        return BaseResponseFormatter.success(MatchingSuccessCode.TODAY_APPLICATION_RETRIEVED, response);
    }

    @Operation(
            summary = "팀원 매칭 신청",
            description =
                    """
                    선택한 프로필, 공모전 카테고리, 팀장 희망 여부로 오늘의 매칭풀에 입장합니다.

                    한국 시간 기준 14시 전에만 신청할 수 있으며 회원당 하루 한 번만 가능합니다.
                    같은 날 신청을 무료 취소하거나 패스했더라도 다시 신청할 수 없습니다.

                    신청 시 다음 값을 `MatchingApplication`에 스냅샷으로 저장합니다.
                    - 선택한 프로필과 공모전 카테고리
                    - 팀장 희망 여부
                    - 현재 협업거리
                    - 가장 최근에 제출한 협업 유형 검사 점수와 캐릭터 결과
                    - 학점·프로젝트·수상·자격증·협업거리로 계산한 역량 점수와 그룹

                    스냅샷은 신청 후 프로필, 설문 결과 또는 회원 협업거리가 변경되어도 바뀌지 않습니다.
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @PostMapping
    public ResponseEntity<BaseResponse<ApplicationResponse>> apply(
            @RequestBody @Valid ApplyRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicationResponse response = matchingApplicationService.apply(userDetails.getMemberId(), request);
        return BaseResponseFormatter.success(MatchingSuccessCode.APPLICATION_CREATED, response);
    }

    @Operation(
            summary = "매칭 신청 통합 철회",
            description =
                    """
                    매칭 신청 취소와 패스(팀이 매칭됐는데 거절)를 구분하지 않고 호출하는 통합 철회 API
                    서버가 신청일과 한국 시간 기준 현재 시각을 비교해 처리 방식을 결정

                    - 신청일 14시 전: `FREE_CANCEL`로 처리하며 협업거리 감점 X
                    - 결과 생성 전 신청일 14시 이상 자정 전: `PENALIZED_PASS`로 처리하며 협업거리를 차감
                    - `PROPOSED` 결과: 공개 전후와 관계없이 다음 날 12시 전까지 `PENALIZED_PASS`로 처리합니다.

                    패스 감점은 최근 7일 패스 횟수에 따라 3m → 5m → 7m → 9m → 11m로 증가하며 최대 11m입니다.
                    취소 또는 패스가 완료되어도 해당 날짜의 신청 이력은 유지되므로 같은 날 재신청할 수 없습니다.
                    """)
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MatchingErrorCode.class)
    @PostMapping("/{applicationId}/withdraw")
    public ResponseEntity<BaseResponse<WithdrawalResponse>> withdraw(
            @Parameter(description = "철회할 본인 매칭 신청 ID", example = "100", required = true) @PathVariable("applicationId")
                    Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        WithdrawalResponse response = matchingWithdrawalService.withdraw(userDetails.getMemberId(), applicationId);
        return BaseResponseFormatter.success(MatchingSuccessCode.APPLICATION_WITHDRAWN, response);
    }
}
