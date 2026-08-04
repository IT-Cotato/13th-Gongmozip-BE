package org.cotato.gongmozip.domains.matching.algorithm;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolInput;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchedTeam;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.TeamCompatibilityScore;
import org.cotato.gongmozip.domains.matching.config.MatchingAlgorithmProperties;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingAlgorithmType;
import org.cotato.gongmozip.domains.matching.score.TeamCompatibilityCalculator;
import org.springframework.stereotype.Component;

/**
 * 완전탐색이 현실적인 시간 안에 끝나지 않는 큰 풀을 처리하기 위해 만든 휴리스틱 알고리즘이다.
 *
 * <p>재배정 우선순위를 유지한 여러 신청자 순서에서 Greedy 해를 만들고, 미배정자와 팀원의 교환으로 해를 개선한 뒤 가장 좋은
 * 계획을 선택한다. 고정 시드를 사용해 같은 입력의 결과를 재현할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class MultiStartGreedyMatchingAlgorithm implements TeamMatchingAlgorithm {

    private final TeamCompatibilityCalculator compatibilityCalculator;
    private final MatchingPlanComparator planComparator;
    private final MatchingAlgorithmProperties properties;
    private final Clock clock;

    @Override
    public MatchingPlan match(MatchingPoolInput input) {
        Instant startedAt = clock.instant();
        GreedyContext context = new GreedyContext();
        List<Start> starts = createStarts(input);
        MatchingPlan best = null;
        int executedStarts = 0;

        // 최초 한 번은 결과 생성을 보장하고, 이후 시작점부터는 공통 마감 시각을 지킨다.
        for (Start start : starts) {
            if (executedStarts > 0 && !clock.instant().isBefore(input.searchDeadline())) {
                break;
            }
            MatchingPlan plan = improve(runOnce(start, input, context), input, context);
            executedStarts++;
            if (best == null || planComparator.compare(plan, best) > 0) {
                best = plan;
            }
        }

        if (best == null) {
            best = MatchingPlan.create(
                    List.of(),
                    input.candidates(),
                    MatchingAlgorithmType.MULTI_START_GREEDY,
                    input.seed(),
                    Duration.ZERO,
                    0,
                    0,
                    0,
                    false);
        }
        return MatchingPlan.create(
                best.teams(),
                best.unassignedCandidates(),
                MatchingAlgorithmType.MULTI_START_GREEDY,
                input.seed(),
                Duration.between(startedAt, clock.instant()),
                context.evaluatedCombinationCount,
                context.exploredCandidateCount,
                executedStarts,
                false);
    }

    private MatchingPlan runOnce(Start start, MatchingPoolInput input, GreedyContext context) {
        List<MatchingCandidate> remaining = new ArrayList<>(start.candidateOrder());
        List<MatchedTeam> teams = new ArrayList<>();

        // 현재 순서의 첫 신청자를 기준점으로 삼아, 함께할 때 점수가 가장 높은 팀원을 탐욕적으로 고른다.
        for (int teamSize : start.teamSizeOrder()) {
            MatchingCandidate anchor = remaining.getFirst();
            TeamChoice bestChoice = findBestTeam(anchor, remaining, teamSize, context);
            if (bestChoice == null) {
                throw new IllegalStateException("탐욕 알고리즘이 확정된 팀 크기 계획을 생성하지 못했습니다.");
            }
            teams.add(bestChoice.team());
            remaining.removeAll(bestChoice.team().candidates());
        }
        return MatchingPlan.create(
                teams,
                remaining,
                MatchingAlgorithmType.MULTI_START_GREEDY,
                input.seed(),
                Duration.ZERO,
                0,
                0,
                1,
                false);
    }

    private TeamChoice findBestTeam(
            MatchingCandidate anchor, List<MatchingCandidate> remaining, int teamSize, GreedyContext context) {
        TeamChoice[] best = new TeamChoice[1];
        chooseTeammates(remaining, 1, teamSize - 1, new ArrayList<>(), indexes -> {
            List<MatchingCandidate> candidates = new ArrayList<>(teamSize);
            candidates.add(anchor);
            indexes.forEach(index -> candidates.add(remaining.get(index)));
            MatchedTeam team = new MatchedTeam(candidates, score(candidates, context));
            TeamChoice choice = new TeamChoice(team);
            context.exploredCandidateCount++;
            if (best[0] == null || compareTeam(choice.team(), best[0].team()) > 0) {
                best[0] = choice;
            }
        });
        return best[0];
    }

    private void chooseTeammates(
            List<MatchingCandidate> candidates,
            int fromIndex,
            int count,
            List<Integer> selected,
            Consumer<List<Integer>> consumer) {
        // 기준점과 결합할 팀원 후보를 순열 중복 없이 조합으로 생성한다.
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

    private MatchingPlan improve(MatchingPlan initial, MatchingPoolInput input, GreedyContext context) {
        MatchingPlan current = initial;
        // 미배정자를 기존 팀원과 한 명씩 바꿔 보고, 비교 우선순위가 실제로 좋아지는 동안만 반복한다.
        while (!current.unassignedCandidates().isEmpty()) {
            MatchingPlan bestImprovement = current;
            for (MatchingCandidate unassigned : current.unassignedCandidates()) {
                for (int teamIndex = 0; teamIndex < current.teams().size(); teamIndex++) {
                    MatchedTeam originalTeam = current.teams().get(teamIndex);
                    for (int memberIndex = 0;
                            memberIndex < originalTeam.candidates().size();
                            memberIndex++) {
                        List<MatchingCandidate> swappedCandidates = new ArrayList<>(originalTeam.candidates());
                        MatchingCandidate removed = swappedCandidates.set(memberIndex, unassigned);
                        MatchedTeam swappedTeam = new MatchedTeam(swappedCandidates, score(swappedCandidates, context));

                        List<MatchedTeam> swappedTeams = new ArrayList<>(current.teams());
                        swappedTeams.set(teamIndex, swappedTeam);
                        List<MatchingCandidate> swappedUnassigned = new ArrayList<>(current.unassignedCandidates());
                        swappedUnassigned.remove(unassigned);
                        swappedUnassigned.add(removed);
                        MatchingPlan candidate = MatchingPlan.create(
                                swappedTeams,
                                swappedUnassigned,
                                MatchingAlgorithmType.MULTI_START_GREEDY,
                                input.seed(),
                                Duration.ZERO,
                                0,
                                0,
                                1,
                                false);
                        context.exploredCandidateCount++;
                        if (planComparator.compare(candidate, bestImprovement) > 0) {
                            bestImprovement = candidate;
                        }
                    }
                }
            }
            if (bestImprovement == current) {
                return current;
            }
            current = bestImprovement;
        }
        return current;
    }

    private List<Start> createStarts(MatchingPoolInput input) {
        List<List<MatchingCandidate>> candidateOrders = createCandidateOrders(input);
        List<List<Integer>> teamSizeOrders = new ArrayList<>();
        teamSizeOrders.add(input.teamSizes());
        List<Integer> reversed = new ArrayList<>(input.teamSizes());
        Collections.reverse(reversed);
        if (!reversed.equals(input.teamSizes())) {
            teamSizeOrders.add(List.copyOf(reversed));
        }

        // 신청자 순서와 3·4인 팀 생성 순서의 조합을 모두 시작점으로 사용해 첫 선택 편향을 줄인다.
        List<Start> starts = new ArrayList<>();
        candidateOrders.forEach(order -> teamSizeOrders.forEach(sizes -> starts.add(new Start(order, sizes))));
        return List.copyOf(starts);
    }

    private List<List<MatchingCandidate>> createCandidateOrders(MatchingPoolInput input) {
        List<List<MatchingCandidate>> orders = new ArrayList<>();
        // 결정적인 기본 순서 세 개를 먼저 실행해 시드와 무관하게 최소한의 탐색 다양성을 확보한다.
        orders.add(orderByBuckets(
                input.candidates(),
                Comparator.comparing(MatchingCandidate::appliedAt).thenComparing(MatchingCandidate::applicationId)));
        orders.add(orderByBuckets(
                input.candidates(),
                Comparator.comparing(MatchingCandidate::skillScore)
                        .reversed()
                        .thenComparing(MatchingCandidate::applicationId)));
        orders.add(orderByBuckets(
                input.candidates(),
                Comparator.comparing(MatchingCandidate::skillScore).thenComparing(MatchingCandidate::applicationId)));

        for (int restart = 0; restart < properties.getGreedyRestartCount(); restart++) {
            // 풀 시드와 재시작 번호로 파생한 시드를 사용하므로 재실행해도 같은 셔플 순서를 만든다.
            long restartSeed = input.seed() ^ (0x9E3779B97F4A7C15L * (restart + 1L));
            orders.add(shuffledByBuckets(input.candidates(), restartSeed));
        }
        return orders;
    }

    private List<MatchingCandidate> orderByBuckets(
            List<MatchingCandidate> candidates, Comparator<MatchingCandidate> withinBucket) {
        List<MatchingCandidate> result = new ArrayList<>(candidates.size());
        forEachBucket(candidates, bucket -> {
            bucket.sort(withinBucket);
            result.addAll(bucket);
        });
        return List.copyOf(result);
    }

    private List<MatchingCandidate> shuffledByBuckets(List<MatchingCandidate> candidates, long seed) {
        List<MatchingCandidate> result = new ArrayList<>(candidates.size());
        Random random = new Random(seed);
        forEachBucket(candidates, bucket -> {
            bucket.sort(Comparator.comparing(MatchingCandidate::applicationId));
            Collections.shuffle(bucket, random);
            result.addAll(bucket);
        });
        return List.copyOf(result);
    }

    private void forEachBucket(List<MatchingCandidate> candidates, Consumer<List<MatchingCandidate>> consumer) {
        // 어떤 정렬·셔플에서도 재배정 대상과 팀장 희망자가 뒤로 밀리지 않도록 버킷 경계는 유지한다.
        consumer.accept(candidates.stream()
                .filter(MatchingCandidate::reassignmentPriority)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        consumer.accept(candidates.stream()
                .filter(candidate -> !candidate.reassignmentPriority())
                .filter(candidate -> candidate.leaderPreference() == LeaderPreference.WANTS)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        consumer.accept(candidates.stream()
                .filter(candidate -> !candidate.reassignmentPriority())
                .filter(candidate -> candidate.leaderPreference() != LeaderPreference.WANTS)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    }

    private TeamCompatibilityScore score(List<MatchingCandidate> team, GreedyContext context) {
        // 여러 시작점과 교환 단계에서 반복되는 동일 팀 점수 계산을 캐시한다.
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

    private int compareTeam(MatchedTeam left, MatchedTeam right) {
        int scoreCompared = left.score().totalScore().compareTo(right.score().totalScore());
        if (scoreCompared != 0) return scoreCompared;
        // 동점일 때 작은 신청 ID 조합을 택해 실행 순서와 무관한 결과를 만든다.
        List<Long> leftIds = left.canonicalApplicationIds();
        List<Long> rightIds = right.canonicalApplicationIds();
        for (int index = 0; index < Math.min(leftIds.size(), rightIds.size()); index++) {
            int compared = leftIds.get(index).compareTo(rightIds.get(index));
            if (compared != 0) return -compared;
        }
        return -Integer.compare(leftIds.size(), rightIds.size());
    }

    private record Start(List<MatchingCandidate> candidateOrder, List<Integer> teamSizeOrder) {}

    private record TeamChoice(MatchedTeam team) {}

    private static final class GreedyContext {
        // 모든 시작점이 점수 캐시와 탐색 지표를 공유해 중복 계산을 줄인다.
        private final Map<List<Long>, TeamCompatibilityScore> scoreCache = new HashMap<>();
        private long evaluatedCombinationCount;
        private long exploredCandidateCount;
    }
}
