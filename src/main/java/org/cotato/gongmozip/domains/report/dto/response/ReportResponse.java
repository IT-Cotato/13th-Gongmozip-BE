package org.cotato.gongmozip.domains.report.dto.response;

import java.time.LocalDateTime;

public final class ReportResponse {

    private ReportResponse() {}

    public record ReportResultResponse(
            Long reportId, Long reportedMemberId, String reasonCode, LocalDateTime createdAt) {}
}
