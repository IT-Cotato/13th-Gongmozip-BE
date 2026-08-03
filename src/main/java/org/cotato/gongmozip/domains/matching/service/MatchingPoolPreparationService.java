package org.cotato.gongmozip.domains.matching.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.algorithm.MatchingPoolPartitioner;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolDefinition;
import org.cotato.gongmozip.domains.matching.config.MatchingAlgorithmProperties;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingBatchStatus;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingBatchRepository;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 당일 대기 신청을 카테고리·역량별 실행 풀로 확정하고 영속 배치와 연결하기 위해 만들었다.
 * 비관적 잠금과 별도 트랜잭션을 사용해 재실행되더라도 아직 준비되지 않은 신청만 한 번 분류한다.
 */
@Service
@RequiredArgsConstructor
public class MatchingPoolPreparationService {

    private final MatchingApplicationRepository matchingApplicationRepository;
    private final MatchingBatchRepository matchingBatchRepository;
    private final MatchingPoolPartitioner partitioner;
    private final MatchingAlgorithmProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void prepare(LocalDate applicationDate) {
        // matchingBatch가 없는 WAITING 신청만 잠가 다른 준비 작업이 같은 신청을 가져가지 못하게 한다.
        List<MatchingApplication> applications = matchingApplicationRepository.findUnpreparedWaitingWithLock(
                applicationDate, MatchingApplicationStatus.WAITING);
        if (applications.isEmpty()) {
            return;
        }

        // 카테고리가 다른 신청자는 같은 팀이 되지 않으므로 분할기 호출 전부터 입력을 격리한다.
        Map<InterestCategory, List<MatchingApplication>> byCategory = new EnumMap<>(InterestCategory.class);
        applications.forEach(application -> byCategory
                .computeIfAbsent(application.getContestCategory(), ignored -> new ArrayList<>())
                .add(application));

        for (Map.Entry<InterestCategory, List<MatchingApplication>> entry : byCategory.entrySet()) {
            Map<Long, MatchingApplication> byId = entry.getValue().stream()
                    .collect(Collectors.toMap(
                            MatchingApplication::getMatchingApplicationId, application -> application));
            List<MatchingPoolDefinition> definitions = partitioner.partition(
                    entry.getKey(),
                    entry.getValue().stream().map(MatchingCandidate::from).toList());
            for (MatchingPoolDefinition definition : definitions) {
                // 배치를 먼저 저장한 뒤 해당 풀의 모든 신청에 같은 배치와 유효 풀 번호를 연결한다.
                MatchingBatch batch = matchingBatchRepository.save(toBatch(applicationDate, definition));
                definition.candidates().forEach(candidate -> byId.get(candidate.applicationId())
                        .prepareForBatch(batch, definition.poolOrdinal()));
            }
        }
        matchingApplicationRepository.saveAll(applications);
    }

    private MatchingBatch toBatch(LocalDate applicationDate, MatchingPoolDefinition definition) {
        return MatchingBatch.builder()
                .applicationDate(applicationDate)
                .category(definition.category())
                .poolOrdinal(definition.poolOrdinal())
                .groupingMode(definition.groupingMode())
                .sourceQuartileFrom(definition.sourceQuartileFrom())
                .sourceQuartileTo(definition.sourceQuartileTo())
                .status(MatchingBatchStatus.PENDING)
                .randomSeed(stableSeed(applicationDate, definition))
                .publishedAt(LocalDateTime.of(applicationDate, properties.getResultPublishTime()))
                .build();
    }

    private long stableSeed(LocalDate applicationDate, MatchingPoolDefinition definition) {
        // 날짜와 풀 정의만으로 시드를 만들면 실패 후 재시도에서도 같은 Greedy 시작 순서를 재현할 수 있다.
        String value = applicationDate
                + ":"
                + definition.category().name()
                + ":"
                + definition.poolOrdinal()
                + ":"
                + definition.groupingMode().name()
                + ":"
                + definition.sourceQuartileFrom()
                + "-"
                + definition.sourceQuartileTo();
        long hash = 0xcbf29ce484222325L;
        // Java 런타임과 무관하게 동일한 값을 얻도록 FNV-1a 방식으로 직접 해시한다.
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
