package org.cotato.gongmozip.domains.scheduler;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.cotato.gongmozip.domains.matching.service.MatchingApplicationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매칭 결과 공개 시각에 "매칭 결과가 공개되었어요" 알림함 항목을 남긴다(docs/decisions/11-notification.md).
 *
 * <p>{@link MatchingResponseDeadlineJobs}와 동일하게 5분마다 폴링하며, 실제 공개 여부 판단은
 * {@code MatchingApplicationService.notifyTodayResultPublishedIfDue}가 그때그때
 * {@code matching.algorithm.result-publish-time}(application.yml) 설정값으로 직접 확인한다 — 고정
 * cron으로 시각을 따로 하드코딩하지 않아, 그 설정값을 바꾸면 다음 폴링부터 곧바로 반영된다(최초 구현은
 * "0 0 16 * * *" 고정 cron이었는데, 설정값만 바꾸면 알림 시점이 조용히 어긋나는 문제가 있었다). 하루에
 * 한 번만 보내는 멱등성은 {@code matching_result_notification_logs}가 보장한다.
 */
@Component
@RequiredArgsConstructor
public class MatchingResultNotificationJobs {

    private final MatchingApplicationService matchingApplicationService;

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "matching-result-published-notification", lockAtMostFor = "PT10M")
    public void notifyResultPublished() {
        matchingApplicationService.notifyTodayResultPublishedIfDue();
    }
}
