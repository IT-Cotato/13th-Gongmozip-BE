package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchingTimePolicyTest {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate APPLICATION_DATE = LocalDate.of(2026, 7, 31);

    @DisplayName("13시 59분 59초에는 무료 취소로 판정한다.")
    @Test
    void 마감_직전에는_무료_취소다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T04:59:59Z");

        assertThat(policy.isApplicationOpen()).isTrue();
        assertThat(policy.resolveWithdrawalType(APPLICATION_DATE)).isEqualTo(WithdrawalType.FREE_CANCEL);
    }

    @DisplayName("14시부터는 패널티 패스로 판정한다.")
    @Test
    void 마감_시각부터는_패널티_패스다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T05:00:00Z");

        assertThat(policy.isApplicationOpen()).isFalse();
        assertThat(policy.resolveWithdrawalType(APPLICATION_DATE)).isEqualTo(WithdrawalType.PENALIZED_PASS);
    }

    @DisplayName("신청일 다음 날 자정부터는 철회할 수 없다.")
    @Test
    void 자정부터는_철회할_수_없다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T15:00:00Z");

        assertThatThrownBy(() -> policy.resolveWithdrawalType(APPLICATION_DATE))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.WITHDRAWAL_NOT_ALLOWED);
    }

    private MatchingTimePolicy policyAt(String instant) {
        return new MatchingTimePolicy(Clock.fixed(Instant.parse(instant), KOREA_ZONE));
    }
}
