package org.cotato.gongmozip.domains.matching.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "팀장 희망 여부: WANTS=팀장을 원함(유효 리더 1명), NEUTRAL=필요하면 가능(0.5명), DOES_NOT_WANT=원하지 않음(0명)")
@Getter
@RequiredArgsConstructor
public enum LeaderPreference {
    WANTS(2),
    NEUTRAL(1),
    DOES_NOT_WANT(0);

    // 0.5명을 정수 1단위로 표현해 향후 팀 궁합 계산에서 부동소수점 오차를 피한다.
    private final int effectiveLeaderUnits;
}
