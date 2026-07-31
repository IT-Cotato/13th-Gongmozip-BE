package org.cotato.gongmozip.domains.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ReportRequest {

    private ReportRequest() {}

    public record SubmitReportRequest(
            @NotNull(message = "신고 대상 회원은 필수 입력 항목입니다.") Long reportedMemberId,
            Long teamId,
            @NotBlank(message = "신고 사유는 필수 입력 항목입니다.") String reasonCode,
            @Size(max = 500, message = "직접 입력 사유는 최대 500자까지 입력 가능합니다.") String customReasonText) {}
}
