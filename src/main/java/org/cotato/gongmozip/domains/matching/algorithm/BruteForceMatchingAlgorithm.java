package org.cotato.gongmozip.domains.matching.algorithm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.algorithm.exception.MatchingSearchTimeoutException;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolInput;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchedTeam;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.TeamCompatibilityScore;
import org.cotato.gongmozip.domains.matching.enums.MatchingAlgorithmType;
import org.cotato.gongmozip.domains.matching.score.TeamCompatibilityCalculator;
import org.springframework.stereotype.Component;

/**
 * 작은 매칭 풀에서 가능한 팀 구성을 모두 탐색해 비교 기준상 최선의 결과를 찾기 위해 만든 알고리즘이다.
 *
 * <p>풀 크기가 작을 때는 휴리스틱보다 정확한 결과를 얻을 수 있지만 탐색량이 급격히 증가하므로, 지정된 마감 시각을 넘기면
 * {@link MatchingSearchTimeoutException}을 발생시켜 Greedy 알고리즘으로 전환할 수 있게 한다.
 */
@Component
@RequiredArgsConstructor
public class BruteForceMatchingAlgorithm implements TeamMatchingAlgorithm {

    private final TeamCompatibilityCalculator compatibilityCalculator;
    private final MatchingPlanComparator planComparator;
    private final Clock clock;

    @Override
    public MatchingPlan match(MatchingPoolInput input) {
        Instant startedAt = clock.instant();
        SearchContext context = new SearchContext(input);

        // 남은 신청자, 남은 팀 크기, 현재까지 만든 팀을 재귀 상태로 넘기며 완전한 배치 계획을 탐색한다.
        search(
                new ArrayList<>(input.candidates()),
                new ArrayList<>(input.teamSizes()),
                new ArrayList<>(),
                new ArrayList<>(),
                context);

        if (context.bestPlan == null) {
            throw new IllegalStateException("완전탐색이 완전한 매칭 계획을 생성하지 못했습니다.");
        }
        return MatchingPlan.create(
                context.bestPlan.teams(),
                context.bestPlan.unassignedCandidates(),
                MatchingAlgorithmType.BRUTE_FORCE,
                input.seed(),
                Duration.between(startedAt, clock.instant()),
                context.evaluatedCombinationCount,
                context.exploredBranchCount,
                0,
                false);
    }

    private void search(
            List<MatchingCandidate> remaining,
            List<Integer> remainingTeamSizes,
            List<MatchedTeam> teams,
            List<MatchingCandidate> unassigned,
            SearchContext context) {
        context.exploredBranchCount++;
        checkDeadline(context);

        // 남은 인원이 고정된 팀 크기 계획과 미배정 목표 인원을 정확히 채울 수 없는 가지는 즉시 버린다.
        int unassignedNeeded = context.unassignedTarget - unassigned.size();
        int requiredTeamMembers =
                remainingTeamSizes.stream().mapToInt(Integer::intValue).sum();
        if (unassignedNeeded < 0 || remaining.size() != requiredTeamMembers + unassignedNeeded) {
            return;
        }
        if (remaining.isEmpty()) {
            if (remainingTeamSizes.isEmpty() && unassignedNeeded == 0) {
                consider(teams, unassigned, context);
            }
            return;
        }
        if (cannotBeatCurrentBest(remaining, teams, context, remainingTeamSizes.size())) {
            return;
        }

        // 첫 신청자를 기준점으로 고정하면 같은 팀 조합을 순서만 바꿔 다시 탐색하는 중복을 줄일 수 있다.
        MatchingCandidate anchor = remaining.getFirst();
        if (unassignedNeeded > 0) {
            List<MatchingCandidate> nextRemaining = new ArrayList<>(remaining);
            nextRemaining.removeFirst();
            unassigned.add(anchor);
            search(nextRemaining, remainingTeamSizes, teams, unassigned, context);
            unassigned.removeLast();
        }

        Set<Integer> attemptedSizes = new HashSet<>();
        for (int sizeIndex = 0; sizeIndex < remainingTeamSizes.size(); sizeIndex++) {
            int teamSize = remainingTeamSizes.get(sizeIndex);
            // 같은 크기의 팀 슬롯은 서로 구분되지 않으므로 현재 단계에서는 한 번만 시도한다.
            if (!attemptedSizes.add(teamSize) || remaining.size() < teamSize) {
                continue;
            }
            int selectedSizeIndex = sizeIndex;
            List<Integer> selectedIndexes = new ArrayList<>();
            chooseTeammates(remaining, 1, teamSize - 1, selectedIndexes, indexes -> {
                List<MatchingCandidate> teamCandidates = new ArrayList<>(teamSize);
                teamCandidates.add(anchor);
                indexes.forEach(index -> teamCandidates.add(remaining.get(index)));
                MatchedTeam team = new MatchedTeam(teamCandidates, score(teamCandidates, context));

                Set<Long> teamIds = teamCandidates.stream()
                        .map(MatchingCandidate::applicationId)
                        .collect(java.util.stream.Collectors.toSet());
                List<MatchingCandidate> nextRemaining = remaining.stream()
                        .filter(candidate -> !teamIds.contains(candidate.applicationId()))
                        .toList();
                List<Integer> nextTeamSizes = new ArrayList<>(remainingTeamSizes);
                nextTeamSizes.remove(selectedSizeIndex);
                teams.add(team);
                search(nextRemaining, nextTeamSizes, teams, unassigned, context);
                teams.removeLast();
            });
        }
    }

    private void chooseTeammates(
            List<MatchingCandidate> candidates,
            int fromIndex,
            int count,
            List<Integer> selected,
            java.util.function.Consumer<List<Integer>> consumer) {
        // 기준점을 제외한 팀원 인덱스 조합을 생성한다. 순열이 아닌 조합만 만들어 중복 점수를 계산하지 않는다.
        if (count == 0) {
            consumer.accept(List.copyOf(selected));
            return;
        }
        for (int index = fromIndex; index <= candidates.size() - count; index++) {
            selected.add(index);
            chooseTeammates(candidates, index + 1, count - 1, selected, consumer);
            selected.removeLast();
        }
    }

    private TeamCompatibilityScore score(List<MatchingCandidate> team, SearchContext context) {
        // 팀원 ID 정렬값을 캐시 키로 사용해 서로 다른 탐색 가지에서 만난 동일 팀의 점수를 재사용한다.
        List<Long> key =
                team.stream().map(MatchingCandidate::applicationId).sorted().toList();
        TeamCompatibilityScore cached = context.scoreCache.get(key);
        if (cached != null) {
            return cached;
        }
        TeamCompatibilityScore calculated = compatibilityCalculator.calculate(team);
        context.scoreCache.put(key, calculated);
        context.evaluatedCombinationCount++;
        return calculated;
    }

    private void consider(List<MatchedTeam> teams, List<MatchingCandidate> unassigned, SearchContext context) {
        // 완성된 계획은 모든 알고리즘이 공유하는 우선순위 비교기로 평가한다.
        MatchingPlan candidate = MatchingPlan.create(
                teams,
                unassigned,
                MatchingAlgorithmType.BRUTE_FORCE,
                context.input.seed(),
                Duration.ZERO,
                0,
                0,
                0,
                false);
        if (context.bestPlan == null || planComparator.compare(candidate, context.bestPlan) > 0) {
            context.bestPlan = candidate;
        }
    }

    private void checkDeadline(SearchContext context) {
        // 부분 결과를 반환하지 않고 예외로 종료해야 선택기가 전체 입력을 Greedy로 다시 계산할 수 있다.
        if (!clock.instant().isBefore(context.input.searchDeadline())) {
            throw new MatchingSearchTimeoutException(context.evaluatedCombinationCount, context.exploredBranchCount);
        }
    }

    private boolean cannotBeatCurrentBest(
            List<MatchingCandidate> remaining, List<MatchedTeam> teams, SearchContext context, int remainingTeamCount) {
        if (context.bestPlan == null) {
            return false;
        }
        long assignedReassignments = teams.stream()
                .flatMap(team -> team.candidates().stream())
                .filter(MatchingCandidate::reassignmentPriority)
                .count();
        long maximumReassignments = assignedReassignments
                + remaining.stream()
                        .filter(MatchingCandidate::reassignmentPriority)
                        .count();
        if (maximumReassignments < context.bestPlan.assignedReassignmentCount()) {
            return true;
        }
        if (maximumReassignments > context.bestPlan.assignedReassignmentCount()) {
            return false;
        }

        // 남은 모든 팀이 만점이라고 가정한 낙관적 상한도 현재 최선보다 낮으면 더 탐색할 필요가 없다.
        int totalTeamCount = teams.size() + remainingTeamCount;
        if (totalTeamCount == 0) {
            return false;
        }
        BigDecimal currentScore =
                teams.stream().map(team -> team.score().totalScore()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maximumAverage = currentScore
                .add(BigDecimal.valueOf(100L * remainingTeamCount))
                .divide(BigDecimal.valueOf(totalTeamCount), 2, RoundingMode.HALF_UP);
        return maximumAverage.compareTo(context.bestPlan.averageTeamScore()) < 0;
    }

    private static final class SearchContext {
        // 한 번의 탐색에서 최선 계획, 팀 점수 캐시, 운영 지표를 함께 보관한다.
        private final MatchingPoolInput input;
        private final int unassignedTarget;
        private final Map<List<Long>, TeamCompatibilityScore> scoreCache = new HashMap<>();
        private MatchingPlan bestPlan;
        private long evaluatedCombinationCount;
        private long exploredBranchCount;

        private SearchContext(MatchingPoolInput input) {
            this.input = input;
            this.unassignedTarget = input.candidates().size()
                    - input.teamSizes().stream().mapToInt(Integer::intValue).sum();
        }
    }
}
