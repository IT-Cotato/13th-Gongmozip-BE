package org.cotato.gongmozip.domains.matching.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolDefinition;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupingMode;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.springframework.stereotype.Component;

/**
 * 같은 카테고리의 신청자를 역량이 비슷한 실행 단위로 나누기 위해 만든 분할 클래스
 * 신청자가 적으면 카테고리 전체를 사용하고, 충분하면 4분위(스킬그룹)로 나누되 작은 분위는 인접 분위와 합쳐 팀을 만들 수 있는 크기를
 * 확보한다.
 */
@Component
public class MatchingPoolPartitioner {

    private static final int QUARTILE_COUNT = 4;
    private static final int CATEGORY_ONLY_THRESHOLD = 12;
    private static final int MERGE_THRESHOLD = 24;
    private static final int MINIMUM_MERGED_POOL_SIZE = 6;

    // 역량 점수가 낮은 신청자부터 정렬하고, 동점이면 신청 시각과 신청 ID 오름차순으로 순서를 확정한다.
    // 같은 입력의 분할 결과가 실행마다 달라지지 않도록 모든 동점 조건에 결정적인 정렬 기준을 둔다.
    private static final Comparator<MatchingCandidate> CANDIDATE_ORDER = Comparator.comparing(
                    MatchingCandidate::skillScore)
            .thenComparing(MatchingCandidate::appliedAt)
            .thenComparing(MatchingCandidate::applicationId);

    public List<MatchingPoolDefinition> partition(
            InterestCategory category, List<MatchingCandidate> categoryCandidates) {
        Objects.requireNonNull(category, "카테고리는 null일 수 없습니다.");
        Objects.requireNonNull(categoryCandidates, "카테고리별 후보 목록은 null일 수 없습니다.");
        if (categoryCandidates.isEmpty()) {
            return List.of();
        }

        List<MatchingCandidate> sorted =
                categoryCandidates.stream().sorted(CANDIDATE_ORDER).toList();
        if (sorted.stream().anyMatch(candidate -> candidate.category() != category)) {
            throw new IllegalArgumentException("모든 후보는 요청한 카테고리에 속해야 합니다.");
        }
        if (sorted.size() < CATEGORY_ONLY_THRESHOLD) {
            return List.of(toDefinition(category, MatchingGroupingMode.CATEGORY_ONLY, 1, new Segment(1, 4, sorted)));
        }

        // 12명 이상은 먼저 역량 점수순으로 4분할한다.
        // 24명 이상이면 각 분위가 최소 6명이므로 병합 없이 네 풀을 유지할 수 있다.
        List<Segment> quartiles = createQuartiles(sorted);
        if (sorted.size() >= MERGE_THRESHOLD) {
            return toDefinitions(category, MatchingGroupingMode.QUARTILE, quartiles);
        }
        // 12~23명은 4분할하면 6명 미만인 분위가 생기므로 인접 분위와 합쳐 실행 가능한 풀로 만든다.
        return toDefinitions(category, MatchingGroupingMode.MERGED_QUARTILE, mergeUndersized(quartiles));
    }

    /**
     * 역량 점수순으로 정렬된 후보를 인원 차이가 최대 1명인 네 개의 연속 구간으로 나눈다.
     * 전체 인원을 4로 나눈 몫을 기본 인원으로 사용하고, 나머지는 낮은 분위부터 한 명씩 추가한다.
     * 예를 들어 13명은 4·3·3·3명, 21명은 6·5·5·5명으로 나뉜다.
     */
    private List<Segment> createQuartiles(List<MatchingCandidate> sorted) {
        int base = sorted.size() / QUARTILE_COUNT;
        int remainder = sorted.size() % QUARTILE_COUNT;
        List<Segment> result = new ArrayList<>(QUARTILE_COUNT);
        int fromIndex = 0;
        // 나머지는 낮은 분위부터 한 명씩 배분해 분위별 인원 차이가 최대 1명이 되게 한다.
        for (int quartile = 1; quartile <= QUARTILE_COUNT; quartile++) {
            int size = base + (quartile <= remainder ? 1 : 0);
            int toIndex = fromIndex + size;
            result.add(new Segment(quartile, quartile, sorted.subList(fromIndex, toIndex)));
            fromIndex = toIndex;
        }
        return result;
    }

    /**
     * 6명 미만인 분위가 없어질 때까지 역량상 인접한 분위끼리 병합한다.
     * 가장 작은 미달 풀부터 처리하고, 크기가 같으면 높은 분위의 풀을 먼저 처리해 결과를 결정적으로 유지한다.
     * 예를 들어 13명의 4·3·3·3 분위는 최종적으로 7·6명인 두 개의 유효 풀로 합쳐진다.
     */
    private List<Segment> mergeUndersized(List<Segment> quartiles) {
        List<Segment> segments = new ArrayList<>(quartiles);
        // 팀 편성이 어려운 가장 작은 구간부터 인접 구간에 합치며 모든 유효 풀을 6명 이상으로 만든다.
        while (segments.stream().anyMatch(segment -> segment.size() < MINIMUM_MERGED_POOL_SIZE)) {
            // 미달 풀 중 인원이 가장 적은 풀을 고른다. 동률이면 toQuartile이 큰 높은 분위가 먼저 선택된다.
            Segment target = segments.stream()
                    .filter(segment -> segment.size() < MINIMUM_MERGED_POOL_SIZE)
                    .min(Comparator.comparingInt(Segment::size)
                            .thenComparing(
                                    Comparator.comparingInt(Segment::toQuartile).reversed()))
                    .orElseThrow(() -> new IllegalStateException("병합할 최소 크기 미만의 매칭 풀을 찾을 수 없습니다."));
            int targetIndex = segments.indexOf(target);
            int neighborIndex = chooseNeighborIndex(segments, targetIndex);
            int lowerIndex = Math.min(targetIndex, neighborIndex);
            int upperIndex = Math.max(targetIndex, neighborIndex);
            Segment merged = segments.get(lowerIndex).merge(segments.get(upperIndex));
            segments.remove(upperIndex);
            segments.set(lowerIndex, merged);
        }
        return List.copyOf(segments);
    }

    private int chooseNeighborIndex(List<Segment> segments, int targetIndex) {
        // 양 끝 분위는 인접한 풀이 하나뿐이므로 안쪽 풀과 합친다.
        if (targetIndex == 0) {
            return 1;
        }
        if (targetIndex == segments.size() - 1) {
            return targetIndex - 1;
        }
        // 가운데 분위는 합친 뒤 한쪽 풀만 지나치게 커지지 않도록 더 작은 이웃을 선택한다.
        Segment lower = segments.get(targetIndex - 1);
        Segment higher = segments.get(targetIndex + 1);
        if (lower.size() < higher.size()) {
            return targetIndex - 1;
        }
        // 양쪽 크기가 같을 때는 높은 분위와 합쳐 결과를 결정적으로 유지한다.
        return targetIndex + 1;
    }

    private List<MatchingPoolDefinition> toDefinitions(
            InterestCategory category, MatchingGroupingMode mode, List<Segment> segments) {
        List<MatchingPoolDefinition> definitions = new ArrayList<>(segments.size());
        // 병합 후 남은 최종 풀의 낮은 역량 구간부터 1, 2, 3, 4 순서로 유효 풀 번호를 다시 붙인다.
        for (int index = 0; index < segments.size(); index++) {
            definitions.add(toDefinition(category, mode, index + 1, segments.get(index)));
        }
        return List.copyOf(definitions);
    }

    private MatchingPoolDefinition toDefinition(
            InterestCategory category, MatchingGroupingMode mode, int poolOrdinal, Segment segment) {
        return new MatchingPoolDefinition(
                category, mode, poolOrdinal, segment.fromQuartile(), segment.toQuartile(), segment.candidates());
    }

    private record Segment(int fromQuartile, int toQuartile, List<MatchingCandidate> candidates) {
        private Segment {
            candidates = List.copyOf(candidates);
        }

        private int size() {
            return candidates.size();
        }

        private Segment merge(Segment other) {
            // 병합 후에도 원래 역량 정렬을 복구해 다음 분할 결과와 저장 순서를 안정화한다.
            List<MatchingCandidate> merged = new ArrayList<>(candidates.size() + other.candidates.size());
            merged.addAll(candidates);
            merged.addAll(other.candidates);
            merged.sort(CANDIDATE_ORDER);
            return new Segment(
                    Math.min(fromQuartile, other.fromQuartile), Math.max(toQuartile, other.toQuartile), merged);
        }
    }
}
