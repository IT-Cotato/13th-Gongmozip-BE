package org.cotato.gongmozip.domains.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.cotato.gongmozip.domains.matching.service.MatchingResponseDeadlineService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingResponseDeadlineJobs {

    private final MatchingResponseDeadlineService matchingResponseDeadlineService;

    // 마감 이후 남아 있는 PROPOSED 그룹을 5분마다 다시 조회해 일시 장애가 다음 날까지 이어지지 않게 한다.
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    // 정상 종료 시 즉시 해제되며, 서버가 작업 도중 중단돼도 최대 10분 뒤 다른 인스턴스가 재시도한다.
    @SchedulerLock(name = "matching-response-deadline", lockAtMostFor = "PT10M")
    public void processDeadlines() {
        // ShedLock은 여러 서버의 동시 실행을 막고, 아래 try-catch는 한 그룹의 도메인/DB 실패가
        // 다음 그룹 처리까지 중단시키는 것을 막는다. 실제 트랜잭션도 processGroup마다 분리돼 있다.
        var dueGroupIds = matchingResponseDeadlineService.findDueGroupIds();
        int failedCount = 0;
        for (Long groupId : dueGroupIds) {
            try {
                matchingResponseDeadlineService.processGroup(groupId);
            } catch (Exception exception) {
                failedCount++;
                log.error("매칭 응답 마감 처리 실패 - matchingGroupId: {}", groupId, exception);
            }
        }
        if (!dueGroupIds.isEmpty()) {
            log.info(
                    "매칭 응답 마감 처리 종료 - 대상: {}, 성공: {}, 실패: {}",
                    dueGroupIds.size(),
                    dueGroupIds.size() - failedCount,
                    failedCount);
        }
    }
}
