package org.cotato.gongmozip.domains.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.report.dto.request.ReportRequest.SubmitReportRequest;
import org.cotato.gongmozip.domains.report.dto.response.ReportResponse.ReportResultResponse;
import org.cotato.gongmozip.domains.report.exception.codes.ReportErrorCode;
import org.cotato.gongmozip.domains.report.exception.codes.ReportSuccessCode;
import org.cotato.gongmozip.domains.report.service.ReportService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report", description = "사용자 신고 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "사용자 신고")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ReportErrorCode.class)
    @PostMapping("/reports")
    public ResponseEntity<BaseResponse<ReportResultResponse>> submitReport(
            @RequestBody @Valid SubmitReportRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ReportResultResponse response = reportService.submitReport(userDetails.getMemberId(), request);
        return BaseResponseFormatter.success(ReportSuccessCode.REPORT_SUBMITTED, response);
    }
}
