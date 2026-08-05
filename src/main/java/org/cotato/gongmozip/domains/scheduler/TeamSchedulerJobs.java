package org.cotato.gongmozip.domains.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.scheduler.service.TeamScheduleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 실제 cron 트리거. 로직 자체는 {@link TeamScheduleService}에 있다. 대상 팀을 팀 단위 트랜잭션으로
 * 하나씩 처리한다 — 한 팀에서 예외가 나도 나머지 팀 처리는 계속되도록 팀마다 try-catch로 격리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamSchedulerJobs {

    private final TeamScheduleService teamScheduleService;

    // 공모전 후보/투표 마감은 시각 단위(오늘 23시)라 5분 간격으로 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    public void resolveContestVotingDeadlines() {
        for (Long teamId : teamScheduleService.findDueContestVotingDeadlineTeamIds()) {
            try {
                teamScheduleService.resolveContestVotingDeadlineForTeam(teamId);
            } catch (Exception e) {
                log.error("공모전 투표 마감 처리 실패 - teamId: {}", teamId, e);
            }
        }
    }

    // 중간점검/제출확인은 날짜 단위 비교라 하루 한 번이면 충분하다.
    @Scheduled(cron = "0 0 9 * * *")
    public void sendProgressChecks() {
        for (Long teamId : teamScheduleService.findDueProgressCheckTeamIds()) {
            try {
                teamScheduleService.sendProgressCheckForTeam(teamId);
            } catch (Exception e) {
                log.error("중간점검 알림 발행 실패 - teamId: {}", teamId, e);
            }
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendSubmissionChecks() {
        for (Long teamId : teamScheduleService.findDueSubmissionCheckTeamIds()) {
            try {
                teamScheduleService.sendSubmissionCheckForTeam(teamId);
            } catch (Exception e) {
                log.error("제출확인 알림 발행 실패 - teamId: {}", teamId, e);
            }
        }
    }

    // 인사 유도 타임아웃도 시각 단위(팀 생성 후 2시간)라 5분 간격으로 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    public void forceAdvanceGreetings() {
        for (Long teamId : teamScheduleService.findDueGreetingTimeoutTeamIds()) {
            try {
                teamScheduleService.forceAdvanceGreetingForTeam(teamId);
            } catch (Exception e) {
                log.error("인사 유도 강제 전이 실패 - teamId: {}", teamId, e);
            }
        }
    }

    // 팀장 여부 투표/팀장 투표 마감도 시각 단위(LEADER_SELECTING 진입 후 2시간)라 5분 간격으로
    // 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    public void resolveLeaderSelectionDeadlines() {
        for (Long teamId : teamScheduleService.findDueLeaderSelectionDeadlineTeamIds()) {
            try {
                teamScheduleService.resolveLeaderSelectionDeadlineForTeam(teamId);
            } catch (Exception e) {
                log.error("팀장 선출 마감 처리 실패 - teamId: {}", teamId, e);
            }
        }
    }
}
