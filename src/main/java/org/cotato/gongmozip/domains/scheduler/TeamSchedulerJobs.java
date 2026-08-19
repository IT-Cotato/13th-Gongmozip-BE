package org.cotato.gongmozip.domains.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.cotato.gongmozip.domains.scheduler.service.TeamScheduleService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 실제 cron 트리거. 로직 자체는 {@link TeamScheduleService}에 있다. 대상 팀을 팀 단위 트랜잭션으로
 * 하나씩 처리한다 — 한 팀에서 예외가 나도 나머지 팀 처리는 계속되도록 팀마다 try-catch로 격리한다.
 *
 * <p>{@code @SchedulerLock}(ShedLock)으로 서버를 여러 대로 늘려도 같은 job이 인스턴스마다 중복
 * 실행되지 않게 한다 — 서버가 1대인 지금은 효과가 없지만, MatchingSchedulerJobs 등 다른
 * 스케줄러는 이미 붙어있던 걸 이 클래스만 빠뜨리고 있었다(스레드풀 점유 이슈 점검, 2026-08-15).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamSchedulerJobs {

    private final TeamScheduleService teamScheduleService;

    // 공모전 후보/투표 마감은 절대 시각(진입 시점 +24시간)이라 5분 간격으로 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "team-contest-voting-deadline", lockAtMostFor = "PT10M")
    public void resolveContestVotingDeadlines() {
        for (Long teamId : teamScheduleService.findDueContestVotingDeadlineTeamIds()) {
            try {
                teamScheduleService.resolveContestVotingDeadlineForTeam(teamId);
            } catch (Exception e) {
                log.error("공모전 투표 마감 처리 실패 - teamId: {}", teamId, e);
            }
        }
    }

    // 공모전 투표 마감 10분 전 리마인더도 같은 5분 간격으로 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "team-contest-vote-reminder", lockAtMostFor = "PT10M")
    public void sendContestVoteReminders() {
        for (Long teamId : teamScheduleService.findDueContestVoteReminderTeamIds()) {
            try {
                teamScheduleService.sendContestVoteReminderForTeam(teamId);
            } catch (Exception e) {
                log.error("공모전 투표 리마인더 발행 실패 - teamId: {}", teamId, e);
            }
        }
    }

    // 중간점검은 하루 1번(09시)만 확인하면 실제 절반 시점(progressCheckAt)이 지난 뒤 최대
    // 24시간 지연될 수 있어(2026-08-19), 09/14/19시 하루 3번으로 완화했다 — 지연 폭을
    // 절반 이하로 줄이면서도, 팀마다 다른 시각에 흩어져 알림이 가는 걸 막아 예측 가능한
    // 시간대(업무/저녁 시간)에만 발송되게 한다.
    @Scheduled(cron = "0 0 9,14,19 * * *")
    @SchedulerLock(name = "team-progress-check", lockAtMostFor = "PT30M")
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
    @SchedulerLock(name = "team-submission-check", lockAtMostFor = "PT30M")
    public void sendSubmissionChecks() {
        for (Long teamId : teamScheduleService.findDueSubmissionCheckTeamIds()) {
            try {
                teamScheduleService.sendSubmissionCheckForTeam(teamId);
            } catch (Exception e) {
                log.error("제출확인 알림 발행 실패 - teamId: {}", teamId, e);
            }
        }
    }

    // "진행 완료"로 응답하지 않은 팀에게 2시간 간격으로 재알림하므로 5분 간격으로 마감을 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "team-submission-check-reminder", lockAtMostFor = "PT10M")
    public void sendSubmissionCheckReminders() {
        for (Long teamId : teamScheduleService.findDueSubmissionCheckReminderTeamIds()) {
            try {
                teamScheduleService.sendSubmissionCheckReminderForTeam(teamId);
            } catch (Exception e) {
                log.error("제출확인 재알림 발행 실패 - teamId: {}", teamId, e);
            }
        }
    }

    // 인사 유도 타임아웃도 시각 단위(팀 생성 후 2시간)라 5분 간격으로 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "team-greeting-timeout", lockAtMostFor = "PT10M")
    public void forceAdvanceGreetings() {
        for (Long teamId : teamScheduleService.findDueGreetingTimeoutTeamIds()) {
            try {
                teamScheduleService.forceAdvanceGreetingForTeam(teamId);
            } catch (ObjectOptimisticLockingFailureException e) {
                // 팀원이 마지막 인사를 같은 시점에 보내 recordGreetingAndAdvance가 먼저 전이시킨
                // 정상적인 동시성 충돌(Team.version, 이슈 #62)이라 에러가 아니라 경고로만 남기고
                // 해당 팀은 건너뛴다 — 다음 주기에 재조회하면 이미 GREETING이 아니라 대상에서 빠진다.
                log.warn("인사 유도 강제 전이가 동시 처리로 건너뛰어짐 - teamId: {}", teamId);
            } catch (Exception e) {
                log.error("인사 유도 강제 전이 실패 - teamId: {}", teamId, e);
            }
        }
    }

    // 팀장 후보 등록(팀장 여부 투표) 마감도 시각 단위(LEADER_SELECTING 진입 후 3시간)라 5분
    // 간격으로 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "team-leader-candidacy-deadline", lockAtMostFor = "PT10M")
    public void resolveLeaderCandidacyDeadlines() {
        for (Long teamId : teamScheduleService.findDueLeaderCandidacyDeadlineTeamIds()) {
            try {
                teamScheduleService.resolveLeaderCandidacyDeadlineForTeam(teamId);
            } catch (Exception e) {
                log.error("팀장 후보 등록 마감 처리 실패 - teamId: {}", teamId, e);
            }
        }
    }

    // 팀장 투표 마감은 후보 등록이 끝날 때(또는 동률 재투표가 시작될 때)마다 새로 세팅되므로
    // (Team.leaderVoteDeadlineAt), 후보 등록 마감과 별도로 5분 간격으로 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "team-leader-vote-deadline", lockAtMostFor = "PT10M")
    public void resolveLeaderVoteDeadlines() {
        for (Long teamId : teamScheduleService.findDueLeaderVoteDeadlineTeamIds()) {
            try {
                teamScheduleService.resolveLeaderVoteDeadlineForTeam(teamId);
            } catch (Exception e) {
                log.error("팀장 투표 마감 처리 실패 - teamId: {}", teamId, e);
            }
        }
    }

    // 팀장 투표 마감 30분 전 리마인더도 같은 5분 간격으로 확인한다.
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "team-leader-vote-reminder", lockAtMostFor = "PT10M")
    public void sendLeaderVoteReminders() {
        for (Long teamId : teamScheduleService.findDueLeaderVoteReminderTeamIds()) {
            try {
                teamScheduleService.sendLeaderVoteReminderForTeam(teamId);
            } catch (Exception e) {
                log.error("팀장 투표 리마인더 발행 실패 - teamId: {}", teamId, e);
            }
        }
    }
}
