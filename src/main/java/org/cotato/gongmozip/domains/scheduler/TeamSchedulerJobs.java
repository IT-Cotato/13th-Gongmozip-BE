package org.cotato.gongmozip.domains.scheduler;

import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.scheduler.service.TeamScheduleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 실제 cron 트리거. 로직 자체는 {@link TeamScheduleService}에 있다. */
@Component
@RequiredArgsConstructor
public class TeamSchedulerJobs {

    private final TeamScheduleService teamScheduleService;

    // 공모전 후보/투표 마감은 시각 단위(오늘 23시)라 5분 간격으로 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    public void resolveContestVotingDeadlines() {
        teamScheduleService.resolveDueContestVotingDeadlines();
    }

    // 중간점검/제출확인은 날짜 단위 비교라 하루 한 번이면 충분하다.
    @Scheduled(cron = "0 0 9 * * *")
    public void sendProgressChecks() {
        teamScheduleService.sendDueProgressChecks();
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendSubmissionChecks() {
        teamScheduleService.sendDueSubmissionChecks();
    }
}
