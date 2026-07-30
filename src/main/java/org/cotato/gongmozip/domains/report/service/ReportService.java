package org.cotato.gongmozip.domains.report.service;

import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.report.converter.ReportConverter;
import org.cotato.gongmozip.domains.report.dto.request.ReportRequest.SubmitReportRequest;
import org.cotato.gongmozip.domains.report.dto.response.ReportResponse.ReportResultResponse;
import org.cotato.gongmozip.domains.report.entity.Report;
import org.cotato.gongmozip.domains.report.enums.ReportReason;
import org.cotato.gongmozip.domains.report.exception.ReportException;
import org.cotato.gongmozip.domains.report.exception.codes.ReportErrorCode;
import org.cotato.gongmozip.domains.report.repository.ReportRepository;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public ReportResultResponse submitReport(Long reporterMemberId, SubmitReportRequest request) {
        if (reporterMemberId.equals(request.reportedMemberId())) {
            throw new ReportException(ReportErrorCode.CANNOT_REPORT_SELF);
        }

        ReportReason reasonCode = ReportConverter.toReportReason(request.reasonCode());
        if (reasonCode == ReportReason.OTHER
                && (request.customReasonText() == null
                        || request.customReasonText().isBlank())) {
            throw new ReportException(ReportErrorCode.MISSING_CUSTOM_REASON);
        }

        Member reporter = memberRepository
                .findById(reporterMemberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        Member reported = memberRepository
                .findById(request.reportedMemberId())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Team team = null;
        if (request.teamId() != null) {
            team = teamRepository
                    .findById(request.teamId())
                    .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        }

        Report saved = reportRepository.save(
                ReportConverter.toReport(reporter, reported, team, reasonCode, request.customReasonText()));
        return ReportConverter.toReportResultResponse(saved);
    }
}
