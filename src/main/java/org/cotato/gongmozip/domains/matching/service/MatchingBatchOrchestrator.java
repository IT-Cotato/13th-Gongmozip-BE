package org.cotato.gongmozip.domains.matching.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.matching.algorithm.MatchingAlgorithmSelector;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolInput;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;
import org.cotato.gongmozip.domains.matching.config.MatchingAlgorithmProperties;
import org.cotato.gongmozip.domains.matching.enums.MatchingBatchStatus;
import org.cotato.gongmozip.domains.matching.repository.MatchingBatchRepository;
import org.cotato.gongmozip.domains.matching.service.model.ClaimedMatchingPool;
import org.cotato.gongmozip.global.config.TimeConfig;
import org.springframework.stereotype.Service;

/**
 * 일일 매칭의 풀 준비, 선점, 알고리즘 계산, 결과 저장을 트랜잭션 경계에 맞춰 순서대로 조율하기 위해 만들었다.
 * 풀별 실패를 격리해 다음 풀을 계속 처리하고, 한 번 즉시 재시도하되 전체 운영 마감시각은 넘기지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingBatchOrchestrator {

    private final MatchingPoolPreparationService preparationService;
    private final MatchingBatchRepository matchingBatchRepository;
    private final MatchingBatchClaimService claimService;
    private final MatchingAlgorithmSelector algorithmSelector;
    private final MatchingResultPersistenceService persistenceService;
    private final MatchingAlgorithmProperties properties;
    private final Clock clock;

    public void runDaily(LocalDate applicationDate) {
        Instant finalDeadline = applicationDate
                .atTime(properties.getBatchDeadline())
                .atZone(TimeConfig.KOREA_ZONE_ID)
                .toInstant();
        Instant bruteForceDeadline = finalDeadline.minus(properties.getFallbackReserve());

        // 아직 배치에 속하지 않은 신청을 먼저 분류한 뒤, 준비 또는 실패 상태의 배치만 처리한다.
        preparationService.prepare(applicationDate);
        List<Long> batchIds = matchingBatchRepository.findProcessableIds(
                applicationDate, List.of(MatchingBatchStatus.PENDING, MatchingBatchStatus.FAILED));
        for (Long batchId : batchIds) {
            // 남은 풀은 FAILED/PENDING 상태로 두어 다음 스케줄 실행에서 안전하게 재시도한다.
            if (!clock.instant().isBefore(finalDeadline)) {
                log.error("매칭 배치 마감시각에 도달했습니다. 남은 풀은 다음 실행에서 재시도합니다. 신청일={}, 배치 ID={}", applicationDate, batchId);
                break;
            }
            // 일시적인 계산·저장 실패는 같은 실행에서 한 번 더 선점해 복구를 시도한다.
            for (int attempt = 0; attempt < 2; attempt++) {
                OptionalResult result = claimService
                        .claim(batchId)
                        .map(claimed -> processClaimedPool(applicationDate, claimed, bruteForceDeadline))
                        .orElse(OptionalResult.NOT_CLAIMED);
                if (result != OptionalResult.FAILED) {
                    break;
                }
            }
        }
    }

    private OptionalResult processClaimedPool(
            LocalDate applicationDate, ClaimedMatchingPool claimed, Instant bruteForceDeadline) {
        try {
            // 이 구간에는 DB 트랜잭션이 없어 탐색이 길어져도 커넥션과 행 잠금을 점유하지 않는다.
            MatchingPoolInput input = new MatchingPoolInput(
                    applicationDate,
                    claimed.poolKey().category(),
                    claimed.poolKey().poolOrdinal(),
                    claimed.candidates(),
                    claimed.teamSizes(),
                    claimed.seed(),
                    bruteForceDeadline);
            MatchingPlan plan = algorithmSelector.match(input);
            persistenceService.persistSuccess(claimed.batchId(), plan);
            log.info(
                    "매칭 풀 처리 완료 - 배치 ID={}, 풀={}, 알고리즘={}, 배정 인원={}, 미배정 인원={}, 대체 알고리즘 사용={}",
                    claimed.batchId(),
                    claimed.poolKey(),
                    plan.selectedAlgorithm(),
                    plan.assignedCount(),
                    plan.unassignedCandidates().size(),
                    plan.fallbackOccurred());
            return OptionalResult.SUCCEEDED;
        } catch (Exception failure) {
            log.error("매칭 풀 처리 실패 - 배치 ID={}, 풀={}", claimed.batchId(), claimed.poolKey(), failure);
            try {
                // 실패 저장도 별도 트랜잭션이므로 신청을 WAITING으로 복구한 뒤 같은 배치를 다시 선점할 수 있다.
                persistenceService.persistFailure(claimed.batchId(), failure);
            } catch (Exception persistenceFailure) {
                log.error("매칭 배치 실패 상태 저장 실패 - 배치 ID={}", claimed.batchId(), persistenceFailure);
            }
            return OptionalResult.FAILED;
        }
    }

    private enum OptionalResult {
        SUCCEEDED,
        FAILED,
        NOT_CLAIMED
    }
}
