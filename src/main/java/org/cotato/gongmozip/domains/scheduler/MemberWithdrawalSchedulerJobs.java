package org.cotato.gongmozip.domains.scheduler;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.cotato.gongmozip.domains.member.service.MemberWithdrawService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberWithdrawalSchedulerJobs {

    private final MemberWithdrawService memberWithdrawService;
    private final Clock clock;

    // 재가입 제한 기간(14일)이 지난 탈퇴 회원의 개인정보를 매일 새벽 4시에 익명화한다.
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    // 정상 종료 시 즉시 해제되며, 서버 장애로 해제하지 못해도 최대 30분 후 만료되어 잠금이 고착되지 않는다.
    @SchedulerLock(name = "member-withdrawal-anonymize", lockAtMostFor = "PT30M")
    public void anonymizeExpiredWithdrawnMembers() {
        int anonymizedCount = memberWithdrawService.anonymizeExpiredWithdrawnMembers(LocalDateTime.now(clock));
        if (anonymizedCount > 0) {
            log.info("탈퇴 회원 개인정보 익명화 완료 - 대상: {}명", anonymizedCount);
        }
    }
}
