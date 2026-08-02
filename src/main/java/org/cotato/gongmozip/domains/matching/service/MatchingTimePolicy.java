package org.cotato.gongmozip.domains.matching.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingTimePolicy {

    // 모든 시각은 TimeConfig에서 주입한 Asia/Seoul Clock을 기준으로 판정한다
    private static final LocalTime APPLICATION_DEADLINE = LocalTime.of(14, 0);

    private final Clock clock;

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDateTime applicationDeadline(LocalDate applicationDate) {
        return applicationDate.atTime(APPLICATION_DEADLINE);
    }

    // 신청일 기준 패스 가능 종료 시각 — 현재 정책은 다음 날 00:00
    public LocalDateTime withdrawalDeadline(LocalDate applicationDate) {
        // TODO: 임시 매칭 수락/패스 정책 구현 시 '당일 자정' 제한시간을 정책 설정값으로 분리한다.
        return applicationDate.plusDays(1).atStartOfDay();
    }

    // 14:00 정각은 마감 이후이므로 신청 불가다
    public boolean isApplicationOpen() {
        return now().isBefore(applicationDeadline(today()));
    }

    // 하나의 철회 요청을 14시 전 FREE_CANCEL, 14시 이후 PENALIZED_PASS로 구분한다
    public WithdrawalType resolveWithdrawalType(LocalDate applicationDate) {
        if (applicationDate == null) {
            throw new MatchingException(MatchingErrorCode.WITHDRAWAL_NOT_ALLOWED);
        }
        LocalDateTime now = now();
        LocalDateTime applicationStart = applicationDate.atStartOfDay();
        // 신청일 이전이거나 다음 날 자정에 도달했다면 어떤 방식으로도 철회할 수 없다
        if (now.isBefore(applicationStart) || !now.isBefore(withdrawalDeadline(applicationDate))) {
            throw new MatchingException(MatchingErrorCode.WITHDRAWAL_NOT_ALLOWED);
        }
        if (now.isBefore(applicationDeadline(applicationDate))) {
            return WithdrawalType.FREE_CANCEL;
        }
        return WithdrawalType.PENALIZED_PASS;
    }
}
