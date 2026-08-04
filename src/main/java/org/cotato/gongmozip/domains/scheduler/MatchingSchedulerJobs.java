package org.cotato.gongmozip.domains.scheduler;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.cotato.gongmozip.domains.matching.service.MatchingBatchOrchestrator;
import org.cotato.gongmozip.domains.matching.service.MatchingTimePolicy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 일일 매칭 실행 시각과 분산 서버 중복 실행 방지 정책을 비즈니스 오케스트레이터에서 분리하기 위해 만들었다.
 * ShedLock을 사용해 여러 인스턴스가 떠 있어도 매일 14시 한 인스턴스만 배치를 시작한다.
 */
@Component
@RequiredArgsConstructor
public class MatchingSchedulerJobs {

    private final MatchingBatchOrchestrator matchingBatchOrchestrator;
    private final MatchingTimePolicy matchingTimePolicy;

    @Scheduled(cron = "0 0 14 * * *", zone = "Asia/Seoul")
    // 모든 서버 인스턴스가 같은 DB 잠금 이름을 사용해 한 서버만 일일 매칭을 실행하게 한다.
    // 정상 종료 시 즉시 해제되며, 서버 장애로 해제하지 못해도 최대 100분 후 만료되어 잠금이 고착되지 않는다.
    @SchedulerLock(name = "matching-daily-batch", lockAtMostFor = "PT100M")
    public void runDailyMatching() {
        // 테스트 가능한 시간 정책에서 오늘 날짜를 얻어 스케줄러의 시스템 시각 의존성을 격리한다.
        matchingBatchOrchestrator.runDaily(matchingTimePolicy.today());
    }
}
