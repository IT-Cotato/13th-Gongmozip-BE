package org.cotato.gongmozip.domains.matching.service.model;

import java.util.List;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolKey;

/**
 * DB 선점 트랜잭션이 끝난 뒤 알고리즘 계산에 필요한 값만 안전하게 전달하기 위해 만든 불변 스냅샷이다.
 * JPA 엔티티 대신 후보와 팀 크기 계획을 담아 긴 알고리즘 실행이 영속성 컨텍스트에 의존하지 않게 한다.
 */
public record ClaimedMatchingPool(
        Long batchId, MatchingPoolKey poolKey, long seed, List<MatchingCandidate> candidates, List<Integer> teamSizes) {
    public ClaimedMatchingPool {
        // 호출자가 원본 목록을 바꿔 선점된 입력이 달라지는 일을 막는다.
        candidates = List.copyOf(candidates);
        teamSizes = List.copyOf(teamSizes);
    }
}
