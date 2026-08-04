package org.cotato.gongmozip.domains.matching.algorithm;

import java.util.Comparator;
import java.util.List;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;
import org.springframework.stereotype.Component;

/**
 * Brute Force와 Greedy가 같은 기준으로 결과를 선택하도록 계획 우선순위를 한곳에 모으기 위해 만들었다.
 * 반환값이 양수이면 왼쪽 계획이 더 좋으며, 품질 지표가 모두 같을 때는 신청 ID로 결정적인 승자를 만든다.
 */
@Component
public class MatchingPlanComparator implements Comparator<MatchingPlan> {

    @Override
    public int compare(MatchingPlan left, MatchingPlan right) {
        // 배정 인원과 재배정 우선 대상의 배정을 팀 점수보다 먼저 보장한다.
        int compared = Integer.compare(left.assignedCount(), right.assignedCount());
        if (compared != 0) return compared;

        compared = Long.compare(left.assignedReassignmentCount(), right.assignedReassignmentCount());
        if (compared != 0) return compared;

        compared = left.averageTeamScore().compareTo(right.averageTeamScore());
        if (compared != 0) return compared;

        compared = Long.compare(left.assignedWantsCount(), right.assignedWantsCount());
        if (compared != 0) return compared;

        compared = left.minimumTeamScore().compareTo(right.minimumTeamScore());
        if (compared != 0) return compared;

        compared = right.teamScoreVariance().compareTo(left.teamScoreVariance());
        if (compared != 0) return compared;

        // ID가 더 작은 정규형을 결정적 승자로 간주해 동일 입력의 결과가 항상 같게 한다.
        return -compareIds(left.canonicalApplicationOrder(), right.canonicalApplicationOrder());
    }

    private int compareIds(List<Long> left, List<Long> right) {
        for (int i = 0; i < Math.min(left.size(), right.size()); i++) {
            int compared = left.get(i).compareTo(right.get(i));
            if (compared != 0) return compared;
        }
        return Integer.compare(left.size(), right.size());
    }
}
