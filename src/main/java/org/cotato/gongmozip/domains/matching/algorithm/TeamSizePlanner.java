package org.cotato.gongmozip.domains.matching.algorithm;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 매칭 알고리즘이 팀 구성과 팀 크기 선택을 동시에 탐색하지 않도록 3·4인 팀 개수를 미리 확정하기 위해 만들었다.
 * 배정 인원을 최대화하고, 동률이면 운영상 선호하는 4인 팀을 더 많이 만드는 계획을 선택한다.
 */
@Component
public class TeamSizePlanner {

    public List<Integer> plan(int poolSize) {
        return planSizes(poolSize);
    }

    public static List<Integer> planSizes(int poolSize) {
        if (poolSize < 0) {
            throw new IllegalArgumentException("매칭 풀 인원수는 음수일 수 없습니다.");
        }

        int bestAssigned = 0;
        int bestFourPersonTeamCount = 0;
        int bestThreePersonTeamCount = 0;
        // 가능한 3인·4인 팀 개수를 모두 비교한다. 풀 크기만 순회하므로 알고리즘 탐색보다 비용이 매우 작다.
        for (int fourPersonTeams = 0; fourPersonTeams <= poolSize / 4; fourPersonTeams++) {
            for (int threePersonTeams = 0; threePersonTeams <= poolSize / 3; threePersonTeams++) {
                int assigned = fourPersonTeams * 4 + threePersonTeams * 3;
                if (assigned > poolSize) {
                    continue;
                }
                if (assigned > bestAssigned
                        || (assigned == bestAssigned && fourPersonTeams > bestFourPersonTeamCount)) {
                    bestAssigned = assigned;
                    bestFourPersonTeamCount = fourPersonTeams;
                    bestThreePersonTeamCount = threePersonTeams;
                }
            }
        }

        List<Integer> teamSizes = new ArrayList<>(bestFourPersonTeamCount + bestThreePersonTeamCount);
        for (int index = 0; index < bestFourPersonTeamCount; index++) {
            teamSizes.add(4);
        }
        for (int index = 0; index < bestThreePersonTeamCount; index++) {
            teamSizes.add(3);
        }
        return List.copyOf(teamSizes);
    }
}
