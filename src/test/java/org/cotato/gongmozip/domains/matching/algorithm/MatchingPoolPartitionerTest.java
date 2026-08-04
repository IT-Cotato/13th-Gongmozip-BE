package org.cotato.gongmozip.domains.matching.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cotato.gongmozip.domains.matching.support.MatchingCandidateFixture.candidate;

import java.util.List;
import java.util.stream.LongStream;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupingMode;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchingPoolPartitionerTest {

    private final MatchingPoolPartitioner partitioner = new MatchingPoolPartitioner();

    @Test
    @DisplayName("12명 미만은 역량 분위 없이 카테고리 단일 풀로 만든다")
    void createsCategoryOnlyPoolBelowTwelve() {
        var pools = partitioner.partition(InterestCategory.IT_AI_TECH, candidates(11));

        assertThat(pools).singleElement().satisfies(pool -> {
            assertThat(pool.groupingMode()).isEqualTo(MatchingGroupingMode.CATEGORY_ONLY);
            assertThat(pool.poolOrdinal()).isEqualTo(1);
            assertThat(pool.sourceQuartileFrom()).isEqualTo(1);
            assertThat(pool.sourceQuartileTo()).isEqualTo(4);
            assertThat(pool.candidates()).hasSize(11);
        });
    }

    @Test
    @DisplayName("24명은 6명씩 네 분위로 나누고 병합하지 않는다")
    void createsFourQuartilesAtTwentyFour() {
        var pools = partitioner.partition(InterestCategory.IT_AI_TECH, candidates(24));

        assertThat(pools).hasSize(4);
        assertThat(pools).extracting(pool -> pool.candidates().size()).containsExactly(6, 6, 6, 6);
        assertThat(pools).allMatch(pool -> pool.groupingMode() == MatchingGroupingMode.QUARTILE);
    }

    @Test
    @DisplayName("21명의 6·5·5·5 분위는 확정 규칙에 따라 11·10 풀로 병합한다")
    void mergesTwentyOneIntoElevenAndTen() {
        var pools = partitioner.partition(InterestCategory.IT_AI_TECH, candidates(21));

        assertThat(pools).extracting(pool -> pool.candidates().size()).containsExactly(11, 10);
        assertThat(pools)
                .extracting(pool -> pool.sourceQuartileFrom() + "-" + pool.sourceQuartileTo())
                .containsExactly("1-2", "3-4");
        assertThat(pools).allMatch(pool -> pool.groupingMode() == MatchingGroupingMode.MERGED_QUARTILE);
    }

    @Test
    @DisplayName("12·13·20·23·25명 경계에서도 같은 분할·병합 규칙을 적용한다")
    void handlesPartitionBoundaries() {
        assertThat(poolSizes(12)).containsExactly(6, 6);
        assertThat(poolSizes(13)).containsExactly(7, 6);
        assertThat(poolSizes(20)).containsExactly(10, 10);
        assertThat(poolSizes(23)).containsExactly(6, 6, 11);
        assertThat(poolSizes(25)).containsExactly(7, 6, 6, 6);
    }

    @Test
    @DisplayName("동일 역량 점수는 신청시각과 신청 ID 순서로 안정적으로 나눈다")
    void usesStableTieBreakOrder() {
        List<MatchingCandidate> tied = candidates(24).stream()
                .map(value -> new MatchingCandidate(
                        value.applicationId(),
                        value.memberId(),
                        value.profileId(),
                        value.appliedAt(),
                        value.category(),
                        java.math.BigDecimal.valueOf(50),
                        value.leaderPreference(),
                        value.reassignmentPriority(),
                        value.goalPreferenceScore(),
                        value.workStyleScore(),
                        value.communicationStyleScore(),
                        value.agreeablenessScore(),
                        value.conscientiousnessScore(),
                        value.honestyHumilityScore(),
                        value.extroversionType()))
                .toList();

        var pools = partitioner.partition(InterestCategory.IT_AI_TECH, tied);

        assertThat(pools.getFirst().candidates())
                .extracting(MatchingCandidate::applicationId)
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    private List<MatchingCandidate> candidates(int count) {
        return LongStream.rangeClosed(1, count).mapToObj(id -> candidate(id)).toList();
    }

    private List<Integer> poolSizes(int count) {
        return partitioner.partition(InterestCategory.IT_AI_TECH, candidates(count)).stream()
                .map(pool -> pool.candidates().size())
                .toList();
    }
}
