package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.cotato.gongmozip.domains.matching.config.MatchingAlgorithmProperties;
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

    @DisplayName("15시 59분 59초까지는 매칭 진행 중이라 신청할 수 없다.")
    @Test
    void 결과_공개_직전까지는_신청할_수_없다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T06:59:59Z");

        assertThat(policy.isApplicationOpen()).isFalse();
    }

    @DisplayName("16시 결과 공개 정각부터 다시 신청할 수 있고 대상일은 다음 날이다.")
    @Test
    void 결과_공개_시각부터는_다음_날_신청으로_열린다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T07:00:00Z");

        assertThat(policy.isApplicationOpen()).isTrue();
        assertThat(policy.currentApplicationDate()).isEqualTo(APPLICATION_DATE.plusDays(1));
    }

    @DisplayName("14시 전에는 오늘이 신청 대상일이다.")
    @Test
    void 마감_전에는_오늘이_신청_대상일이다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T04:59:59Z");

        assertThat(policy.currentApplicationDate()).isEqualTo(APPLICATION_DATE);
    }

    @DisplayName("단일 시각 판정은 14시 전이면 오늘을 신청 대상일로 반환한다.")
    @Test
    void 단일_시각_판정은_마감_전이면_오늘을_반환한다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T04:59:59Z");

        assertThat(policy.resolveApplicationDate()).isEqualTo(APPLICATION_DATE);
    }

    @DisplayName("단일 시각 판정은 매칭 진행 구간(14~16시)이면 마감 예외를 던진다.")
    @Test
    void 단일_시각_판정은_매칭_진행_구간이면_마감_예외다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T05:00:00Z");

        assertThatThrownBy(policy::resolveApplicationDate)
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.APPLICATION_DEADLINE_PASSED);
    }

    @DisplayName("단일 시각 판정은 16시 결과 공개부터 다음 날을 신청 대상일로 반환한다.")
    @Test
    void 단일_시각_판정은_결과_공개_후_다음_날을_반환한다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T07:00:00Z");

        assertThat(policy.resolveApplicationDate()).isEqualTo(APPLICATION_DATE.plusDays(1));
    }

    @DisplayName("16시 이후 접수된 익일 신청은 신청일 전날에도 무료 취소로 판정한다.")
    @Test
    void 익일_신청은_전날에도_무료_취소다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T08:00:00Z");

        assertThat(policy.resolveWithdrawalType(APPLICATION_DATE.plusDays(1))).isEqualTo(WithdrawalType.FREE_CANCEL);
    }

    @DisplayName("신청일 다음 날 자정부터는 철회할 수 없다.")
    @Test
    void 자정부터는_철회할_수_없다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T15:00:00Z");

        assertThatThrownBy(() -> policy.resolveWithdrawalType(APPLICATION_DATE))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.WITHDRAWAL_NOT_ALLOWED);
    }

    @DisplayName("YML 공개 시각 정책을 신청일에 적용하고 정각부터 공개한다.")
    @Test
    void 공개_시각을_신청일에_적용한다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T07:00:00Z");
        LocalDateTime publishedAt = APPLICATION_DATE.atTime(16, 0);

        assertThat(policy.resultPublishAt(APPLICATION_DATE)).isEqualTo(publishedAt);
        assertThat(policy.isResultPublished(APPLICATION_DATE, publishedAt.minusNanos(1)))
                .isFalse();
        assertThat(policy.isResultPublished(APPLICATION_DATE, publishedAt)).isTrue();
    }

    @DisplayName("14시 전에는 오늘을 가장 가까운 매칭 날짜로 반환한다.")
    @Test
    void 매칭_시작_전에는_오늘을_반환한다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T04:59:59Z");

        assertThat(policy.nextAvailableMatchingDate(APPLICATION_DATE)).isEqualTo(APPLICATION_DATE);
    }

    @DisplayName("14시 정각부터는 오늘 매칭이 시작됐으므로 다음 날을 반환한다.")
    @Test
    void 매칭_시작_시각부터는_다음_날을_반환한다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T05:00:00Z");

        assertThat(policy.nextAvailableMatchingDate(APPLICATION_DATE)).isEqualTo(APPLICATION_DATE.plusDays(1));
    }

    @DisplayName("원본 신청의 다음 날이 아직 미래라면 그 날짜보다 앞당기지 않는다.")
    @Test
    void 가장_이른_재배정일을_보존한다() {
        MatchingTimePolicy policy = policyAt("2026-07-31T03:00:00Z");
        LocalDate earliestDate = APPLICATION_DATE.plusDays(2);

        assertThat(policy.nextAvailableMatchingDate(earliestDate)).isEqualTo(earliestDate);
    }

    private MatchingTimePolicy policyAt(String instant) {
        MatchingAlgorithmProperties properties = new MatchingAlgorithmProperties();
        properties.setResultPublishTime(LocalTime.of(16, 0));
        return new MatchingTimePolicy(Clock.fixed(Instant.parse(instant), KOREA_ZONE), properties);
    }
}
