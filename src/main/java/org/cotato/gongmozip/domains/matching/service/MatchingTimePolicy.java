package org.cotato.gongmozip.domains.matching.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.config.MatchingAlgorithmProperties;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingTimePolicy {

    // 모든 시각은 TimeConfig에서 주입한 Asia/Seoul Clock을 기준으로 판정한다
    private static final LocalTime APPLICATION_DEADLINE = LocalTime.of(14, 0);
    private static final LocalTime RESPONSE_DEADLINE = LocalTime.of(12, 0);

    private final Clock clock;
    private final MatchingAlgorithmProperties properties;

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDateTime applicationDeadline(LocalDate applicationDate) {
        return applicationDate.atTime(APPLICATION_DEADLINE);
    }

    /** 가장 이른 재배정 가능일 이후에서 아직 14시 매칭이 시작되지 않은 가장 가까운 날짜를 반환한다. */
    public LocalDate nextAvailableMatchingDate(LocalDate earliestDate) {
        LocalDateTime currentTime = now();
        LocalDate currentDate = currentTime.toLocalDate();
        LocalDate nextScheduledDate =
                currentTime.isBefore(applicationDeadline(currentDate)) ? currentDate : currentDate.plusDays(1);
        return earliestDate.isAfter(nextScheduledDate) ? earliestDate : nextScheduledDate;
    }

    /** YML의 공개 시각 정책을 해당 신청일에 적용한다. 공개 시각은 배치에 중복 저장하지 않는다. */
    public LocalDateTime resultPublishAt(LocalDate applicationDate) {
        return applicationDate.atTime(properties.getResultPublishTime());
    }

    /** 공개 시각 정각을 포함해 {@code currentTime >= resultPublishAt}이면 공개된 것으로 본다. */
    public boolean isResultPublished(LocalDate applicationDate, LocalDateTime currentTime) {
        return !currentTime.isBefore(resultPublishAt(applicationDate));
    }

    public LocalDateTime responseDeadline(LocalDate applicationDate) {
        // 신청일 D의 결과는 D 16시에 공개되고, D+1 12시 정각부터 응답 마감으로 본다.
        return applicationDate.plusDays(1).atTime(RESPONSE_DEADLINE);
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
