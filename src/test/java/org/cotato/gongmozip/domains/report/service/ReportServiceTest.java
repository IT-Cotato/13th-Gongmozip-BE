package org.cotato.gongmozip.domains.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.report.dto.request.ReportRequest.SubmitReportRequest;
import org.cotato.gongmozip.domains.report.dto.response.ReportResponse.ReportResultResponse;
import org.cotato.gongmozip.domains.report.entity.Report;
import org.cotato.gongmozip.domains.report.exception.ReportException;
import org.cotato.gongmozip.domains.report.exception.codes.ReportErrorCode;
import org.cotato.gongmozip.domains.report.repository.ReportRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private ReportService reportService;

    @DisplayName("정상적인 입력으로 신고하면 성공한다.")
    @Test
    void 정상적인_입력으로_신고하면_성공한다() {
        // given
        Member reporter = Member.builder().memberId(1L).email("a@gongmozip.com").build();
        Member reported = Member.builder().memberId(2L).email("b@gongmozip.com").build();
        SubmitReportRequest request = new SubmitReportRequest(2L, null, "FREE_RIDING", null);

        given(memberRepository.findById(1L)).willReturn(Optional.of(reporter));
        given(memberRepository.findById(2L)).willReturn(Optional.of(reported));
        given(reportRepository.save(any(Report.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        ReportResultResponse response = reportService.submitReport(1L, request);

        // then
        assertThat(response.reportedMemberId()).isEqualTo(2L);
        assertThat(response.reasonCode()).isEqualTo("FREE_RIDING");
    }

    @DisplayName("본인을 신고하면 예외가 발생한다.")
    @Test
    void 본인을_신고하면_예외가_발생한다() {
        // given
        SubmitReportRequest request = new SubmitReportRequest(1L, null, "FREE_RIDING", null);

        // when & then
        assertThatThrownBy(() -> reportService.submitReport(1L, request))
                .isInstanceOf(ReportException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReportErrorCode.CANNOT_REPORT_SELF);
    }

    @DisplayName("올바르지 않은 신고 사유면 예외가 발생한다.")
    @Test
    void 올바르지_않은_신고_사유면_예외가_발생한다() {
        // given
        SubmitReportRequest request = new SubmitReportRequest(2L, null, "INVALID_CODE", null);

        // when & then
        assertThatThrownBy(() -> reportService.submitReport(1L, request))
                .isInstanceOf(ReportException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReportErrorCode.INVALID_REPORT_REASON);
    }

    @DisplayName("기타 사유인데 직접 입력 텍스트가 없으면 예외가 발생한다.")
    @Test
    void 기타_사유인데_직접_입력_텍스트가_없으면_예외가_발생한다() {
        // given
        SubmitReportRequest request = new SubmitReportRequest(2L, null, "OTHER", null);

        // when & then
        assertThatThrownBy(() -> reportService.submitReport(1L, request))
                .isInstanceOf(ReportException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReportErrorCode.MISSING_CUSTOM_REASON);
    }
}
