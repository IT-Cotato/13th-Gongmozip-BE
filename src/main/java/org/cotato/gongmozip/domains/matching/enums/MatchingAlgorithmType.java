package org.cotato.gongmozip.domains.matching.enums;

/** 배치가 예상한 알고리즘과 실제 결과를 만든 알고리즘을 구분해 기록하기 위한 실행 방식이다. */
public enum MatchingAlgorithmType {
    // 팀을 만들 수 없는 0~2명 풀의 정상 처리 방식이다.
    NONE,
    // 작은 풀에서 모든 팀 조합을 탐색한다.
    BRUTE_FORCE,
    // 큰 풀 또는 완전탐색 시간 초과 시 여러 시작점의 근사해를 탐색한다.
    MULTI_START_GREEDY
}
