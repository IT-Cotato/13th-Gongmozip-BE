package org.cotato.gongmozip.domains.matching.algorithm.model.pool;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.cotato.gongmozip.domains.matching.algorithm.TeamSizePlanner;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;

/**
 * 하나의 알고리즘 실행에 필요한 후보, 고정 팀 크기, 재현 시드와 마감시간을 묶기 위해 만든 입력 모델이다.
 * 생성 시 풀의 불변조건을 검증해 Brute Force와 Greedy가 입력 방어 로직 없이 탐색 자체에 집중하게 한다.
 */
public record MatchingPoolInput(
        LocalDate applicationDate,
        InterestCategory category,
        int poolOrdinal,
        List<MatchingCandidate> candidates,
        List<Integer> teamSizes,
        long seed,
        Instant searchDeadline) {

    public MatchingPoolInput {
        Objects.requireNonNull(applicationDate, "신청일은 null일 수 없습니다.");
        Objects.requireNonNull(category, "카테고리는 null일 수 없습니다.");
        Objects.requireNonNull(candidates, "후보 목록은 null일 수 없습니다.");
        Objects.requireNonNull(teamSizes, "팀 크기 계획은 null일 수 없습니다.");
        Objects.requireNonNull(searchDeadline, "탐색 마감시각은 null일 수 없습니다.");
        if (poolOrdinal < 1 || poolOrdinal > 4) {
            throw new IllegalArgumentException("유효 풀 번호는 1에서 4 사이여야 합니다.");
        }
        // 어떤 조회 순서로 전달돼도 알고리즘 시작 순서가 같도록 신청 ID 기준으로 정규화한다.
        candidates = candidates.stream()
                .sorted(Comparator.comparing(MatchingCandidate::applicationId))
                .toList();
        if (candidates.stream().map(MatchingCandidate::applicationId).distinct().count() != candidates.size()) {
            throw new IllegalArgumentException("매칭 풀에 동일한 신청을 중복으로 포함할 수 없습니다.");
        }
        for (MatchingCandidate candidate : candidates) {
            if (candidate.category() != category) {
                throw new IllegalArgumentException("모든 후보는 매칭 풀의 카테고리에 속해야 합니다.");
            }
        }
        teamSizes = List.copyOf(teamSizes);
        if (!teamSizes.equals(TeamSizePlanner.planSizes(candidates.size()))) {
            throw new IllegalArgumentException("팀 크기는 확정된 3인·4인 팀 계획과 일치해야 합니다.");
        }
    }

    public MatchingPoolInput(
            LocalDate applicationDate,
            InterestCategory category,
            int poolOrdinal,
            List<MatchingCandidate> candidates,
            long seed,
            Instant searchDeadline) {
        // 팀 크기를 별도로 지정하지 않는 호출자는 공통 계획기의 최적 3·4인 조합을 사용한다.
        this(
                applicationDate,
                category,
                poolOrdinal,
                candidates,
                TeamSizePlanner.planSizes(candidates.size()),
                seed,
                searchDeadline);
    }
}
