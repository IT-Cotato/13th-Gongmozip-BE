package org.cotato.gongmozip.domains.matching.algorithm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamSizePlannerTest {

    private final TeamSizePlanner planner = new TeamSizePlanner();

    @Test
    @DisplayName("배정 인원을 최대화하고 동률이면 4인 팀 수를 최대화한다")
    void plansConfirmedTeamSizes() {
        Map<Integer, List<Integer>> expected = Map.ofEntries(
                Map.entry(0, List.of()),
                Map.entry(1, List.of()),
                Map.entry(2, List.of()),
                Map.entry(3, List.of(3)),
                Map.entry(4, List.of(4)),
                Map.entry(5, List.of(4)),
                Map.entry(6, List.of(3, 3)),
                Map.entry(7, List.of(4, 3)),
                Map.entry(8, List.of(4, 4)),
                Map.entry(9, List.of(3, 3, 3)),
                Map.entry(10, List.of(4, 3, 3)),
                Map.entry(11, List.of(4, 4, 3)),
                Map.entry(12, List.of(4, 4, 4)),
                Map.entry(13, List.of(4, 3, 3, 3)),
                Map.entry(14, List.of(4, 4, 3, 3)),
                Map.entry(15, List.of(4, 4, 4, 3)));

        expected.forEach((poolSize, teamSizes) ->
                assertThat(planner.plan(poolSize)).as("poolSize=%d", poolSize).isEqualTo(teamSizes));
    }
}
