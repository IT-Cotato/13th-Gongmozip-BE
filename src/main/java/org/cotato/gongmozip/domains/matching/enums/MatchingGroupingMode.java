package org.cotato.gongmozip.domains.matching.enums;

/** 신청자 규모에 따라 카테고리 내부 풀을 어떤 기준으로 나눴는지 추적하기 위한 분류 방식이다. */
public enum MatchingGroupingMode {
    // 신청자가 적어 역량 분할 없이 카테고리 전체를 하나의 풀로 사용한다.
    CATEGORY_ONLY,
    // 네 분위가 각각 충분한 인원을 가져 분위별 풀을 유지한다.
    QUARTILE,
    // 작은 분위가 생겨 인접 분위와 합친 유효 풀을 사용한다.
    MERGED_QUARTILE
}
