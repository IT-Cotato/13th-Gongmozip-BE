package org.cotato.gongmozip.domains.matching.algorithm.model.result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingAlgorithmType;

/**
 * 알고리즘의 팀 배치 결과와 품질·실행 지표를 하나의 원자적인 결과로 전달하기 위해 만든 모델이다.
 * 팀과 미배정자를 정규화하고 중복 배정을 차단해 비교기와 저장 계층이 항상 완전한 계획만 다루게 한다.
 */
public record MatchingPlan(
        List<MatchedTeam> teams,
        List<MatchingCandidate> unassignedCandidates,
        BigDecimal averageTeamScore,
        BigDecimal minimumTeamScore,
        BigDecimal teamScoreVariance,
        MatchingAlgorithmType selectedAlgorithm,
        long seed,
        Duration elapsedTime,
        long evaluatedCombinationCount,
        long exploredBranchCount,
        int greedyRestartCount,
        boolean fallbackOccurred) {

    private static final BigDecimal ZERO_SCORE = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal ZERO_VARIANCE = BigDecimal.ZERO.setScale(4);

    public MatchingPlan {
        Objects.requireNonNull(teams, "매칭 팀 목록은 null일 수 없습니다.");
        Objects.requireNonNull(unassignedCandidates, "미배정 후보 목록은 null일 수 없습니다.");
        Objects.requireNonNull(averageTeamScore, "팀 평균 점수는 null일 수 없습니다.");
        Objects.requireNonNull(minimumTeamScore, "팀 최저 점수는 null일 수 없습니다.");
        Objects.requireNonNull(teamScoreVariance, "팀 점수 분산은 null일 수 없습니다.");
        Objects.requireNonNull(selectedAlgorithm, "선택된 알고리즘은 null일 수 없습니다.");
        Objects.requireNonNull(elapsedTime, "실행시간은 null일 수 없습니다.");
        teams = canonicalTeams(teams);
        unassignedCandidates = unassignedCandidates.stream()
                .sorted(Comparator.comparing(MatchingCandidate::applicationId))
                .toList();
        validateDisjoint(teams, unassignedCandidates);
    }

    public static MatchingPlan create(
            List<MatchedTeam> teams,
            List<MatchingCandidate> unassigned,
            MatchingAlgorithmType algorithm,
            long seed,
            Duration elapsed,
            long evaluatedCombinations,
            long exploredBranches,
            int restartCount,
            boolean fallbackOccurred) {
        // 알고리즘 구현이 집계값을 따로 계산하지 않도록 팀 점수에서 계획 단위 통계를 일관되게 산출한다.
        List<MatchedTeam> canonicalTeams = canonicalTeams(teams);
        BigDecimal average = average(canonicalTeams);
        BigDecimal minimum = canonicalTeams.stream()
                .map(team -> team.score().totalScore())
                .min(BigDecimal::compareTo)
                .orElse(ZERO_SCORE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal variance = variance(canonicalTeams);
        return new MatchingPlan(
                canonicalTeams,
                List.copyOf(unassigned),
                average,
                minimum,
                variance,
                algorithm,
                seed,
                elapsed,
                evaluatedCombinations,
                exploredBranches,
                restartCount,
                fallbackOccurred);
    }

    public MatchingPlan withFallbackMetrics(
            Duration totalElapsed, long additionalCombinations, long additionalBranches) {
        // Greedy 결과에 앞서 수행된 완전탐색 지표를 합쳐 실제 전체 실행 비용을 보존한다.
        return new MatchingPlan(
                teams,
                unassignedCandidates,
                averageTeamScore,
                minimumTeamScore,
                teamScoreVariance,
                selectedAlgorithm,
                seed,
                totalElapsed,
                evaluatedCombinationCount + additionalCombinations,
                exploredBranchCount + additionalBranches,
                greedyRestartCount,
                true);
    }

    public int assignedCount() {
        return teams.stream().mapToInt(team -> team.candidates().size()).sum();
    }

    public long assignedReassignmentCount() {
        return teams.stream()
                .flatMap(team -> team.candidates().stream())
                .filter(MatchingCandidate::reassignmentPriority)
                .count();
    }

    public long assignedWantsCount() {
        return teams.stream()
                .flatMap(team -> team.candidates().stream())
                .filter(candidate -> candidate.leaderPreference() == LeaderPreference.WANTS)
                .count();
    }

    public List<Long> canonicalApplicationOrder() {
        // 팀 경계와 미배정 경계를 구분하는 표식까지 넣어 서로 다른 계획이 같은 ID 나열로 보이지 않게 한다.
        List<Long> ids = new ArrayList<>();
        for (MatchedTeam team : teams) {
            ids.addAll(team.canonicalApplicationIds());
            ids.add(-(long) team.candidates().size());
        }
        ids.add(Long.MAX_VALUE);
        ids.addAll(unassignedCandidates.stream()
                .map(MatchingCandidate::applicationId)
                .toList());
        return List.copyOf(ids);
    }

    private static List<MatchedTeam> canonicalTeams(List<MatchedTeam> teams) {
        // 팀 생성 순서가 아니라 팀원 ID 조합으로 정렬해 비교와 영속화 순서를 결정적으로 만든다.
        return teams.stream()
                .sorted((left, right) -> compareIds(left.canonicalApplicationIds(), right.canonicalApplicationIds()))
                .toList();
    }

    private static int compareIds(List<Long> left, List<Long> right) {
        for (int i = 0; i < Math.min(left.size(), right.size()); i++) {
            int compared = left.get(i).compareTo(right.get(i));
            if (compared != 0) {
                return compared;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private static BigDecimal average(List<MatchedTeam> teams) {
        if (teams.isEmpty()) {
            return ZERO_SCORE;
        }
        BigDecimal sum = teams.stream().map(team -> team.score().totalScore()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(teams.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal variance(List<MatchedTeam> teams) {
        if (teams.isEmpty()) {
            return ZERO_VARIANCE;
        }
        BigDecimal exactAverage = teams.stream()
                .map(team -> team.score().totalScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(teams.size()), 12, RoundingMode.HALF_UP);
        BigDecimal squaredDifferences = teams.stream()
                .map(team -> team.score().totalScore().subtract(exactAverage))
                .map(value -> value.multiply(value))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return squaredDifferences.divide(BigDecimal.valueOf(teams.size()), 4, RoundingMode.HALF_UP);
    }

    private static void validateDisjoint(List<MatchedTeam> teams, List<MatchingCandidate> unassigned) {
        // 배정 팀 내부뿐 아니라 미배정 목록까지 합쳐 신청 하나가 결과에 두 번 등장하는지 검사한다.
        List<Long> allIds = new ArrayList<>();
        teams.forEach(team -> team.candidates().forEach(candidate -> allIds.add(candidate.applicationId())));
        unassigned.forEach(candidate -> allIds.add(candidate.applicationId()));
        if (allIds.stream().distinct().count() != allIds.size()) {
            throw new IllegalArgumentException("하나의 신청을 결과에 두 번 이상 배정할 수 없습니다.");
        }
    }
}
