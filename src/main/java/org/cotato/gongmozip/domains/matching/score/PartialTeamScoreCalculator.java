package org.cotato.gongmozip.domains.matching.score;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.springframework.stereotype.Component;

/**
 * 팀 자리를 한 명씩 채우는 순차 선택 알고리즘이 다음 팀원 후보의 순위를 정할 때 사용하는 계산기다.
 *
 * <p>완성 팀(3·4인)의 저장·응답용 최종 점수는 {@link TeamCompatibilityCalculator}가 계속 담당한다.
 * 이 계산기의 결과는 후보 비교에만 사용하며 어떤 응답이나 스키마에도 저장하지 않는다. 미완성 팀
 * 상태에서는 리더 조화도·외향성 정책표를 확정할 수 없으므로 유사도 6개 항목만 합산하고, 팀장 몰림
 * 억제와 재배정 대상자 우선 흡수는 별도 축으로 유지한다.
 */
@Component
@RequiredArgsConstructor
public class PartialTeamScoreCalculator {

    private static final int WANTS_PILE_UP_PENALTY = 1;
    private static final int REASSIGNMENT_PICK_BONUS = 1;

    private final SimilarityScorer similarityScorer;

    public PartialScore evaluateWithCandidate(List<MatchingCandidate> current, MatchingCandidate candidate) {
        validate(current, candidate);
        List<MatchingCandidate> combined = new ArrayList<>(current.size() + 1);
        combined.addAll(current);
        combined.add(candidate);

        BigDecimal similaritySum = similarity(combined, MatchingCandidate::goalPreferenceScore, 10)
                .add(similarity(combined, MatchingCandidate::workStyleScore, 10))
                .add(similarity(combined, MatchingCandidate::communicationStyleScore, 10))
                .add(similarity(combined, MatchingCandidate::agreeablenessScore, 20))
                .add(similarity(combined, MatchingCandidate::conscientiousnessScore, 10))
                .add(similarity(combined, MatchingCandidate::honestyHumilityScore, 10));

        long existingWants = current.stream()
                .filter(member -> member.leaderPreference() == LeaderPreference.WANTS)
                .count();
        int wantsPenalty = existingWants >= 1 && candidate.leaderPreference() == LeaderPreference.WANTS
                ? WANTS_PILE_UP_PENALTY
                : 0;
        int reassignmentBonus = candidate.reassignmentPriority() ? REASSIGNMENT_PICK_BONUS : 0;

        return new PartialScore(
                similaritySum, wantsPenalty, reassignmentBonus, candidate.appliedAt(), candidate.applicationId());
    }

    private BigDecimal similarity(
            List<MatchingCandidate> combined, Function<MatchingCandidate, BigDecimal> extractor, int maximumScore) {
        return similarityScorer.score(combined.stream().map(extractor).toList(), maximumScore);
    }

    private void validate(List<MatchingCandidate> current, MatchingCandidate candidate) {
        if (current == null || current.isEmpty()) {
            throw new IllegalArgumentException("현재 팀은 한 명 이상이어야 합니다.");
        }
        if (current.size() > 3) {
            throw new IllegalArgumentException("미완성 팀은 세 명 이하여야 합니다.");
        }
        if (candidate == null) {
            throw new IllegalArgumentException("후보는 null일 수 없습니다.");
        }
        Set<Long> ids = new HashSet<>();
        for (MatchingCandidate member : current) {
            if (member == null || !ids.add(member.applicationId())) {
                throw new IllegalArgumentException("현재 팀에 null 또는 중복 후보를 포함할 수 없습니다.");
            }
        }
        if (!ids.add(candidate.applicationId())) {
            throw new IllegalArgumentException("후보가 이미 현재 팀에 포함돼 있습니다.");
        }
    }

    /**
     * 후보 순위 비교에만 사용하는 부분 팀 점수. {@link #compareTo}가 클수록 더 좋은 후보다.
     */
    public record PartialScore(
            BigDecimal similaritySum,
            int wantsPenalty,
            int reassignmentBonus,
            LocalDateTime candidateAppliedAt,
            long candidateApplicationId)
            implements Comparable<PartialScore> {

        @Override
        public int compareTo(PartialScore other) {
            int scoreCompared = similaritySum.compareTo(other.similaritySum);
            if (scoreCompared != 0) return scoreCompared;
            int selfAdjustment = reassignmentBonus - wantsPenalty;
            int otherAdjustment = other.reassignmentBonus - other.wantsPenalty;
            int adjustmentCompared = Integer.compare(selfAdjustment, otherAdjustment);
            if (adjustmentCompared != 0) return adjustmentCompared;
            // 이른 신청 시각이 더 큰 값(우선)이 되도록 반대 방향으로 비교한다.
            int appliedCompared = other.candidateAppliedAt.compareTo(candidateAppliedAt);
            if (appliedCompared != 0) return appliedCompared;
            // 작은 신청 ID가 더 큰 값(우선)이 되도록 반대 방향으로 비교한다.
            return Long.compare(other.candidateApplicationId, candidateApplicationId);
        }
    }
}
