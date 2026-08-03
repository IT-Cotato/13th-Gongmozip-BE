package org.cotato.gongmozip.domains.matching.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.algorithm.MatchingAlgorithmSelector;
import org.cotato.gongmozip.domains.matching.algorithm.TeamSizePlanner;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolKey;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingBatchStatus;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingBatchRepository;
import org.cotato.gongmozip.domains.matching.service.model.ClaimedMatchingPool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 여러 실행 주체가 같은 배치를 계산하지 않도록 DB 잠금 아래 배치와 신청자를 원자적으로 선점하기 위해 만들었다.
 * 짧은 새 트랜잭션에서 상태만 RUNNING으로 바꾸고 불변 입력을 반환해, 긴 알고리즘 계산은 트랜잭션 밖에서 수행되게 한다.
 */
@Service
@RequiredArgsConstructor
public class MatchingBatchClaimService {

    private final MatchingBatchRepository matchingBatchRepository;
    private final MatchingApplicationRepository matchingApplicationRepository;
    private final MatchingAlgorithmSelector algorithmSelector;
    private final TeamSizePlanner teamSizePlanner;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimedMatchingPool> claim(Long batchId) {
        // 배치 행을 먼저 잠가 다른 서버나 재시도 루프와의 중복 선점을 직렬화한다.
        MatchingBatch batch = matchingBatchRepository.findByIdWithLock(batchId).orElse(null);
        if (batch == null
                || batch.getStatus() == MatchingBatchStatus.SUCCEEDED
                || batch.getStatus() == MatchingBatchStatus.RUNNING) {
            return Optional.empty();
        }

        // 신청 행까지 잠근 상태에서 팀 크기와 최초 알고리즘을 확정하고 한 번에 MATCHING으로 전환한다.
        List<MatchingApplication> applications = matchingApplicationRepository.findAllByMatchingBatchAndStatusWithLock(
                batch, MatchingApplicationStatus.WAITING);
        List<Integer> teamSizes = teamSizePlanner.plan(applications.size());
        batch.start(algorithmSelector.initiallySelectedAlgorithm(applications.size()), LocalDateTime.now(clock));
        applications.forEach(application -> application.startMatching(batch));
        matchingApplicationRepository.saveAll(applications);
        matchingBatchRepository.saveAndFlush(batch);

        // 트랜잭션 종료 후에도 사용할 수 있도록 JPA 엔티티를 불변 후보 스냅샷으로 변환한다.
        List<MatchingCandidate> candidates =
                applications.stream().map(MatchingCandidate::from).toList();
        return Optional.of(new ClaimedMatchingPool(
                batch.getMatchingBatchId(),
                new MatchingPoolKey(batch.getCategory(), batch.getPoolOrdinal()),
                batch.getRandomSeed(),
                candidates,
                teamSizes));
    }
}
