package org.cotato.gongmozip.domains.matching.enums;

public enum MatchingGroupMemberStatus {
    PENDING,
    ACCEPTED,
    PASSED,
    EXPIRED,

    // 후속 응답 흐름 도입 전 저장된 행을 읽기 위한 레거시 호환값이다.
    REJECTED
}
