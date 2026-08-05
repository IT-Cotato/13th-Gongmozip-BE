package org.cotato.gongmozip.domains.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.cotato.gongmozip.domains.matching.service.MatchingResponseDeadlineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class MatchingResponseDeadlineJobsTest {

    @Mock
    private MatchingResponseDeadlineService deadlineService;

    @Test
    void 한_그룹의_실패가_다음_그룹_처리를_막지_않는다() {
        given(deadlineService.findDueGroupIds()).willReturn(List.of(1L, 2L, 3L));
        org.mockito.Mockito.doThrow(new IllegalStateException("failure"))
                .when(deadlineService)
                .processGroup(2L);
        MatchingResponseDeadlineJobs jobs = new MatchingResponseDeadlineJobs(deadlineService);

        jobs.processDeadlines();

        verify(deadlineService).processGroup(1L);
        verify(deadlineService).processGroup(2L);
        verify(deadlineService).processGroup(3L);
    }

    @Test
    void 실패한_그룹은_다음_실행에서_다시_처리한다() {
        given(deadlineService.findDueGroupIds()).willReturn(List.of(1L, 2L), List.of(2L));
        org.mockito.Mockito.doThrow(new IllegalStateException("temporary failure"))
                .doNothing()
                .when(deadlineService)
                .processGroup(2L);
        MatchingResponseDeadlineJobs jobs = new MatchingResponseDeadlineJobs(deadlineService);

        jobs.processDeadlines();
        jobs.processDeadlines();

        verify(deadlineService).processGroup(1L);
        verify(deadlineService, times(2)).processGroup(2L);
    }

    @Test
    void 마감_작업은_5분마다_실행하고_비정상_잠금은_10분까지만_유지한다() throws Exception {
        Method method = MatchingResponseDeadlineJobs.class.getDeclaredMethod("processDeadlines");

        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        SchedulerLock schedulerLock = method.getAnnotation(SchedulerLock.class);

        assertThat(scheduled.cron()).isEqualTo("0 */5 * * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
        assertThat(schedulerLock.name()).isEqualTo("matching-response-deadline");
        assertThat(schedulerLock.lockAtMostFor()).isEqualTo("PT10M");
    }
}
