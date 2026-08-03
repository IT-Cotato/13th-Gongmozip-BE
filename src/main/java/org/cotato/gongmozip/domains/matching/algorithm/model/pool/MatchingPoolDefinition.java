package org.cotato.gongmozip.domains.matching.algorithm.model.pool;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupingMode;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;

/**
 * 카테고리 신청자 분할 결과와 그 분할 근거를 배치 생성 계층에 전달하기 위해 만든 모델이다.
 * 유효 풀 번호와 원본 분위 범위를 함께 보존해 분위 병합이 일어나도 결과를 추적할 수 있게 한다.
 */
public record MatchingPoolDefinition(
        InterestCategory category,
        MatchingGroupingMode groupingMode,
        int poolOrdinal,
        int sourceQuartileFrom,
        int sourceQuartileTo,
        List<MatchingCandidate> candidates) {

    public MatchingPoolDefinition {
        Objects.requireNonNull(category, "카테고리는 null일 수 없습니다.");
        Objects.requireNonNull(groupingMode, "풀 분류 방식은 null일 수 없습니다.");
        Objects.requireNonNull(candidates, "후보 목록은 null일 수 없습니다.");
        if (poolOrdinal < 1 || poolOrdinal > 4) {
            throw new IllegalArgumentException("유효 풀 번호는 1에서 4 사이여야 합니다.");
        }
        if (sourceQuartileFrom < 1 || sourceQuartileTo > 4 || sourceQuartileFrom > sourceQuartileTo) {
            throw new IllegalArgumentException("원본 분위 범위는 1에서 4 사이여야 합니다.");
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("매칭 풀은 비어 있을 수 없습니다.");
        }
        candidates = candidates.stream()
                .sorted(Comparator.comparing(MatchingCandidate::skillScore)
                        .thenComparing(MatchingCandidate::appliedAt)
                        .thenComparing(MatchingCandidate::applicationId))
                .toList();
        if (candidates.stream().anyMatch(candidate -> candidate.category() != category)) {
            throw new IllegalArgumentException("모든 후보는 매칭 풀의 카테고리에 속해야 합니다.");
        }
    }

    /** 배치 조회와 로그에서 사용할 카테고리·유효 풀 번호 조합을 반환한다. */
    public MatchingPoolKey key() {
        return new MatchingPoolKey(category, poolOrdinal);
    }
}
