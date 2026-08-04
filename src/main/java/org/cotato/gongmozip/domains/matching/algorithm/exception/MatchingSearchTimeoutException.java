package org.cotato.gongmozip.domains.matching.algorithm.exception;

import lombok.Getter;

/**
 * 완전탐색이 운영 마감시간을 침범하기 전에 중단하고 Greedy fallback으로 제어를 넘기기 위한 예외다.
 * 중단 시점의 탐색량을 함께 전달해 최종 배치 실행 지표가 유실되지 않게 한다.
 */
@Getter
public class MatchingSearchTimeoutException extends RuntimeException {

    private final long evaluatedCombinationCount;
    private final long exploredBranchCount;

    public MatchingSearchTimeoutException(long evaluatedCombinationCount, long exploredBranchCount) {
        super("완전탐색 매칭의 탐색 제한시간을 초과했습니다.");
        this.evaluatedCombinationCount = evaluatedCombinationCount;
        this.exploredBranchCount = exploredBranchCount;
    }
}
