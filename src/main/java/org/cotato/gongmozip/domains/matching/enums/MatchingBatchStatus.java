package org.cotato.gongmozip.domains.matching.enums;

/** 배치의 선점 가능 여부와 재시도 여부를 판단하기 위한 실행 생명주기다. */
public enum MatchingBatchStatus {
    // 풀 분류는 끝났지만 아직 선점되지 않은 상태다.
    PENDING,
    // 신청 선점이 끝나 알고리즘 계산 또는 결과 저장 중인 상태다.
    RUNNING,
    // 팀과 미배정 결과가 모두 저장된 종료 상태다.
    SUCCEEDED,
    // 신청은 WAITING으로 복구되어 같은 배치를 재시도할 수 있는 상태다.
    FAILED
}
