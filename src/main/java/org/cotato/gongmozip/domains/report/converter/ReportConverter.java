package org.cotato.gongmozip.domains.report.converter;

import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.report.dto.response.ReportResponse.ReportResultResponse;
import org.cotato.gongmozip.domains.report.entity.Report;
import org.cotato.gongmozip.domains.report.enums.ReportReason;
import org.cotato.gongmozip.domains.report.exception.ReportException;
import org.cotato.gongmozip.domains.report.exception.codes.ReportErrorCode;
import org.cotato.gongmozip.domains.team.entity.Team;

public final class ReportConverter {

    private ReportConverter() {}

    public static ReportReason toReportReason(String reasonCode) {
        try {
            return ReportReason.valueOf(reasonCode.trim());
        } catch (IllegalArgumentException e) {
            throw new ReportException(ReportErrorCode.INVALID_REPORT_REASON);
        }
    }

    public static Report toReport(
            Member reporter, Member reported, Team team, ReportReason reasonCode, String customReasonText) {
        return Report.builder()
                .reporterMember(reporter)
                .reportedMember(reported)
                .team(team)
                .reasonCode(reasonCode)
                .customReasonText(customReasonText)
                .build();
    }

    public static ReportResultResponse toReportResultResponse(Report report) {
        return new ReportResultResponse(
                report.getReportId(),
                report.getReportedMember().getMemberId(),
                report.getReasonCode().name(),
                report.getCreatedAt());
    }
}
