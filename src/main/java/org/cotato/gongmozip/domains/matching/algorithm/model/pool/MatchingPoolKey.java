package org.cotato.gongmozip.domains.matching.algorithm.model.pool;

import org.cotato.gongmozip.domains.profile.enums.InterestCategory;

/** 카테고리마다 다시 시작하는 유효 풀 번호를 로그와 계층 간 전달에서 혼동 없이 식별하기 위한 값 객체다. */
public record MatchingPoolKey(InterestCategory category, int poolOrdinal) {
    public MatchingPoolKey {
        if (category == null) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
        if (poolOrdinal < 1 || poolOrdinal > 4) {
            throw new IllegalArgumentException("유효 풀 번호는 1에서 4 사이여야 합니다.");
        }
    }
}
