package org.cotato.gongmozip.global.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.global.ai.dto.LeaderCandidateSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 팀장 추천 규칙기반 알고리즘 검증 (docs/decisions/02-leader-election.md
 * "팀장 추천 및 추천 이유" 표 기준).
 */
class MockAiClientTest {

    private final MockAiClient aiClient = new MockAiClient(new GeminiClient("", "gemini-2.5-flash-lite"));

    @DisplayName("적합도 점수 표: 후보 유형 × 잔여 다수 유형 조합을 그대로 반영한다.")
    @Test
    void 적합도_점수_표를_그대로_반영한다() {
        // E 후보
        assertThat(aiClient.compatibilityScore(
                        ExtroversionType.E, List.of(ExtroversionType.I, ExtroversionType.I, ExtroversionType.A)))
                .isEqualTo(10);
        assertThat(aiClient.compatibilityScore(
                        ExtroversionType.E, List.of(ExtroversionType.A, ExtroversionType.A, ExtroversionType.I)))
                .isEqualTo(7);
        assertThat(aiClient.compatibilityScore(
                        ExtroversionType.E, List.of(ExtroversionType.E, ExtroversionType.E, ExtroversionType.I)))
                .isEqualTo(3);
        // A 후보: 잔여 구성과 무관하게 항상 6
        assertThat(aiClient.compatibilityScore(
                        ExtroversionType.A, List.of(ExtroversionType.I, ExtroversionType.I, ExtroversionType.E)))
                .isEqualTo(6);
        assertThat(aiClient.compatibilityScore(ExtroversionType.A, List.of(ExtroversionType.E, ExtroversionType.E)))
                .isEqualTo(6);
        // I 후보
        assertThat(aiClient.compatibilityScore(
                        ExtroversionType.I, List.of(ExtroversionType.E, ExtroversionType.E, ExtroversionType.A)))
                .isEqualTo(5);
        assertThat(aiClient.compatibilityScore(
                        ExtroversionType.I, List.of(ExtroversionType.A, ExtroversionType.A, ExtroversionType.E)))
                .isEqualTo(2);
        assertThat(aiClient.compatibilityScore(
                        ExtroversionType.I, List.of(ExtroversionType.I, ExtroversionType.I, ExtroversionType.E)))
                .isEqualTo(2);
    }

    @DisplayName("잔여 팀원 다수 유형이 갈리면(동률) 후보 유형과 무관하게 7점을 준다.")
    @Test
    void 잔여_다수_유형이_동률이면_후보_유형과_무관하게_7점이다() {
        List<ExtroversionType> tiedRemaining = List.of(ExtroversionType.E, ExtroversionType.I, ExtroversionType.A);

        assertThat(aiClient.compatibilityScore(ExtroversionType.E, tiedRemaining))
                .isEqualTo(7);
        assertThat(aiClient.compatibilityScore(ExtroversionType.I, tiedRemaining))
                .isEqualTo(7);
        // A 후보는 원래 "무관"이라 6점이 될 것 같지만, 다수결 미적용 특례가 우선한다 (7점).
        assertThat(aiClient.compatibilityScore(ExtroversionType.A, tiedRemaining))
                .isEqualTo(7);
    }

    @DisplayName("최종점수 = 적합도 점수 + (필요하면 응답자만 가점 2점).")
    @Test
    void 필요하면_응답자만_가점_2점을_받는다() {
        LeaderCandidateSnapshot neutralCandidate = snapshot(1L, LeaderPreference.NEUTRAL, ExtroversionType.E, "4.0");
        LeaderCandidateSnapshot doesNotWantCandidate =
                snapshot(2L, LeaderPreference.DOES_NOT_WANT, ExtroversionType.E, "4.0");
        List<LeaderCandidateSnapshot> team = List.of(
                neutralCandidate,
                doesNotWantCandidate,
                snapshot(3L, LeaderPreference.DOES_NOT_WANT, ExtroversionType.I, "3.0"),
                snapshot(4L, LeaderPreference.DOES_NOT_WANT, ExtroversionType.I, "2.5"));

        assertThat(aiClient.finalScore(neutralCandidate, team)).isEqualTo(12); // 적합도 10 + 가점 2
        assertThat(aiClient.finalScore(doesNotWantCandidate, team)).isEqualTo(10); // 적합도 10 + 0
    }

    @DisplayName("추천 2인은 최종점수 상위 2명이다.")
    @Test
    void 추천_2인은_최종점수_상위_2명이다() {
        List<LeaderCandidateSnapshot> members = List.of(
                snapshot(1L, LeaderPreference.NEUTRAL, ExtroversionType.E, "4.0"), // 잔여 I다수 -> 10 + 2 = 12
                snapshot(2L, LeaderPreference.DOES_NOT_WANT, ExtroversionType.I, "2.0"), // 잔여 E/I/A 동률 -> 7 + 0 = 7
                snapshot(3L, LeaderPreference.DOES_NOT_WANT, ExtroversionType.I, "2.5"), // 잔여 E/I/A 동률 -> 7 + 0 = 7
                snapshot(4L, LeaderPreference.NEUTRAL, ExtroversionType.A, "3.0")); // 잔여 I다수, A후보는 무관 -> 6 + 2 = 8

        List<Long> recommended = aiClient.recommendLeaderCandidates(100L, members);

        assertThat(recommended).containsExactly(1L, 4L);
    }

    @DisplayName("최종점수가 같으면 '필요하면' 응답자가 '아니요' 응답자보다 우선한다.")
    @Test
    void 동률_시_필요하면_응답자가_우선한다() {
        // c1(E,NEUTRAL): 잔여(I,E,E) 다수=E -> 적합도 3 + 가점 2 = 5
        // c2(I,DOES_NOT_WANT): 잔여(E,E,E) 다수=E -> 적합도 5 + 가점 0 = 5 (최종점수 동률)
        LeaderCandidateSnapshot c1 = snapshot(1L, LeaderPreference.NEUTRAL, ExtroversionType.E, "3.0");
        LeaderCandidateSnapshot c2 = snapshot(2L, LeaderPreference.DOES_NOT_WANT, ExtroversionType.I, "3.0");
        LeaderCandidateSnapshot m3 = snapshot(3L, LeaderPreference.DOES_NOT_WANT, ExtroversionType.E, "3.0");
        LeaderCandidateSnapshot m4 = snapshot(4L, LeaderPreference.DOES_NOT_WANT, ExtroversionType.E, "3.0");
        List<LeaderCandidateSnapshot> team = List.of(c1, c2, m3, m4);
        assertThat(aiClient.finalScore(c1, team)).isEqualTo(aiClient.finalScore(c2, team));

        Long winner = aiClient.recommendTiebreakLeader(200L, team, List.of(1L, 2L));

        assertThat(winner).isEqualTo(1L);
    }

    @DisplayName("동률 후보 중 팀장을 고를 때는 동률이 아니었던 후보를 무시하고 동률 후보만 대상으로 한다.")
    @Test
    void 동률_후보_중에서만_추천한다() {
        List<LeaderCandidateSnapshot> team = List.of(
                snapshot(1L, LeaderPreference.NEUTRAL, ExtroversionType.E, "4.0"),
                snapshot(2L, LeaderPreference.NEUTRAL, ExtroversionType.I, "3.0"),
                snapshot(3L, LeaderPreference.DOES_NOT_WANT, ExtroversionType.I, "2.0"));

        Long winner = aiClient.recommendTiebreakLeader(300L, team, List.of(2L, 3L));

        assertThat(winner).isIn(2L, 3L);
    }

    @DisplayName("같은 팀·같은 후보 조합이면 팀ID 시드 고정 랜덤이 항상 같은 결과를 낸다.")
    @Test
    void 팀ID_시드_고정_랜덤은_재현_가능하다() {
        // 적합도/희망여부/외향성 점수가 완전히 동일해 마지막 랜덤 tiebreak까지 가야 하는 상황
        List<LeaderCandidateSnapshot> team = List.of(
                snapshot(1L, LeaderPreference.NEUTRAL, ExtroversionType.I, "3.0"),
                snapshot(2L, LeaderPreference.NEUTRAL, ExtroversionType.I, "3.0"),
                snapshot(3L, LeaderPreference.NEUTRAL, ExtroversionType.E, "3.0"));

        Long first = aiClient.recommendTiebreakLeader(999L, team, List.of(1L, 2L));
        Long second = aiClient.recommendTiebreakLeader(999L, team, List.of(1L, 2L));

        assertThat(first).isEqualTo(second);
    }

    @DisplayName("공모전 추천은 입력 순서(마감 내림차순)를 그대로 유지한 채 최대 3개만 자른다.")
    @Test
    void 공모전_추천은_입력_순서를_유지한_채_최대_3개로_제한한다() {
        List<Long> orderedByDeadlineDesc = List.of(10L, 20L, 30L, 40L);

        List<Long> recommended = aiClient.recommendContests(InterestCategory.IT_AI_TECH, orderedByDeadlineDesc);

        assertThat(recommended).containsExactly(10L, 20L, 30L);
    }

    private LeaderCandidateSnapshot snapshot(
            Long teamMemberId,
            LeaderPreference leaderPreference,
            ExtroversionType extroversionType,
            String extroversionScore) {
        return new LeaderCandidateSnapshot(
                teamMemberId, leaderPreference, extroversionType, new BigDecimal(extroversionScore));
    }
}
