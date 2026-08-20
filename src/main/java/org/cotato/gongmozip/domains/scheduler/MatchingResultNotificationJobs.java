package org.cotato.gongmozip.domains.scheduler;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.cotato.gongmozip.domains.matching.service.MatchingApplicationService;
import org.cotato.gongmozip.domains.matching.service.MatchingTimePolicy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매칭 결과 공개 시각에 "매칭 결과가 공개되었어요" 알림함 항목을 남긴다(docs/decisions/11-notification.md).
 *
 * <p>cron은 {@code matching.result-publish-time}(기본 16:00, application.yml)과 같은 값을 손으로
 * 맞춰둔 것이다 — {@link MatchingSchedulerJobs}가 14시 매칭 배치를 하드코딩 cron으로 트리거하는 것과
 * 같은 방식(스프링 표현식 cron에는 그 프로퍼티를 직접 참조할 수 없음). 운영 중 result-publish-time을
 * 바꾸면 이 cron도 함께 바꿔야 한다.
 */
@Component
@RequiredArgsConstructor
public class MatchingResultNotificationJobs {

    private final MatchingApplicationService matchingApplicationService;
    private final MatchingTimePolicy matchingTimePolicy;

    @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "matching-result-published-notification", lockAtMostFor = "PT10M")
    public void notifyResultPublished() {
        matchingApplicationService.notifyTodayResultPublished(matchingTimePolicy.today());
    }
}
