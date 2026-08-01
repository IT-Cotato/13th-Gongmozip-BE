package org.cotato.gongmozip.domains.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.cotato.gongmozip.domains.scheduler.service.TeamScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamSchedulerJobsTest {

    @Mock
    private TeamScheduleService teamScheduleService;

    @InjectMocks
    private TeamSchedulerJobs teamSchedulerJobs;

    @DisplayName("한 팀의 공모전 투표 마감 처리가 실패해도 나머지 팀은 계속 처리된다.")
    @Test
    void 한_팀의_공모전_투표_마감_처리가_실패해도_나머지_팀은_계속_처리된다() {
        // given
        given(teamScheduleService.findDueContestVotingDeadlineTeamIds()).willReturn(List.of(1L, 2L, 3L));
        willThrow(new RuntimeException("boom")).given(teamScheduleService).resolveContestVotingDeadlineForTeam(2L);

        // when
        teamSchedulerJobs.resolveContestVotingDeadlines();

        // then
        verify(teamScheduleService).resolveContestVotingDeadlineForTeam(1L);
        verify(teamScheduleService).resolveContestVotingDeadlineForTeam(2L);
        verify(teamScheduleService).resolveContestVotingDeadlineForTeam(3L);
    }

    @DisplayName("한 팀의 중간점검 알림 발행이 실패해도 나머지 팀은 계속 처리된다.")
    @Test
    void 한_팀의_중간점검_알림_발행이_실패해도_나머지_팀은_계속_처리된다() {
        // given
        given(teamScheduleService.findDueProgressCheckTeamIds()).willReturn(List.of(1L, 2L));
        willThrow(new RuntimeException("boom")).given(teamScheduleService).sendProgressCheckForTeam(1L);

        // when
        teamSchedulerJobs.sendProgressChecks();

        // then
        verify(teamScheduleService).sendProgressCheckForTeam(1L);
        verify(teamScheduleService).sendProgressCheckForTeam(2L);
    }

    @DisplayName("한 팀의 제출확인 알림 발행이 실패해도 나머지 팀은 계속 처리된다.")
    @Test
    void 한_팀의_제출확인_알림_발행이_실패해도_나머지_팀은_계속_처리된다() {
        // given
        given(teamScheduleService.findDueSubmissionCheckTeamIds()).willReturn(List.of(1L, 2L));
        willThrow(new RuntimeException("boom")).given(teamScheduleService).sendSubmissionCheckForTeam(1L);

        // when
        teamSchedulerJobs.sendSubmissionChecks();

        // then
        verify(teamScheduleService).sendSubmissionCheckForTeam(1L);
        verify(teamScheduleService).sendSubmissionCheckForTeam(2L);
    }
}
