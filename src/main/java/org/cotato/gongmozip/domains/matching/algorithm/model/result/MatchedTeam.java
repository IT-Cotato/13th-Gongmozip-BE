package org.cotato.gongmozip.domains.matching.algorithm.model.result;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;

/**
 * 알고리즘이 만든 한 팀과 그 팀의 세부 호환도 점수를 함께 전달하기 위한 값 객체다.
 * 팀원 순서를 신청 ID 기준으로 정규화해 탐색 순서가 달라도 같은 팀을 동일하게 비교하고 저장할 수 있게 한다.
 */
public record MatchedTeam(List<MatchingCandidate> candidates, TeamCompatibilityScore score) {

    public MatchedTeam {
        Objects.requireNonNull(candidates, "팀원 목록은 null일 수 없습니다.");
        Objects.requireNonNull(score, "팀 호환도 점수는 null일 수 없습니다.");
        if (candidates.size() < 3 || candidates.size() > 4) {
            throw new IllegalArgumentException("매칭 팀은 3명 또는 4명으로 구성해야 합니다.");
        }
        if (candidates.stream().map(MatchingCandidate::applicationId).distinct().count() != candidates.size()) {
            throw new IllegalArgumentException("한 매칭 팀에 동일한 신청을 중복으로 포함할 수 없습니다.");
        }
        candidates = candidates.stream()
                .sorted(Comparator.comparing(MatchingCandidate::applicationId))
                .toList();
    }

    /** 계획의 동점 해소와 캐시 키에 사용할 정규화된 신청 ID 목록을 반환한다. */
    public List<Long> canonicalApplicationIds() {
        return candidates.stream().map(MatchingCandidate::applicationId).toList();
    }
}
