package org.cotato.gongmozip.domains.matching.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchedTeam;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.TeamCompatibilityScore;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingBatchStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingBatchRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 트랜잭션 밖에서 계산된 계획을 검증한 뒤 팀·팀원·신청·배치 상태를 한 번에 저장하기 위해 만들었다.
 * 성공과 실패를 각각 독립 트랜잭션으로 처리해 결과의 일부 저장을 막고, 실패 시 신청을 재시도 가능한 상태로 복구한다.
 */
@Service
@RequiredArgsConstructor
public class MatchingResultPersistenceService {

    private final MatchingBatchRepository matchingBatchRepository;
    private final MatchingApplicationRepository matchingApplicationRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingGroupMemberRepository matchingGroupMemberRepository;
    private final MatchingTimePolicy matchingTimePolicy;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistSuccess(Long batchId, MatchingPlan plan) {
        // 계산 중 철회나 중복 저장을 감지할 수 있도록 배치와 소속 신청을 쓰기 잠금으로 다시 확인한다.
        MatchingBatch batch = getLockedBatch(batchId);
        if (batch.getStatus() != MatchingBatchStatus.RUNNING) {
            throw new IllegalStateException("실행 중인 배치에만 매칭 결과를 저장할 수 있습니다.");
        }
        List<MatchingApplication> applications = matchingApplicationRepository.findAllByMatchingBatchWithLock(batch);
        validatePlanCoversInput(applications, plan);
        Map<Long, MatchingApplication> byId = applications.stream()
                .collect(Collectors.toMap(MatchingApplication::getMatchingApplicationId, application -> application));

        // 팀과 구성원을 저장한 뒤 같은 트랜잭션 안에서 해당 신청을 PROPOSED로 전환한다.
        for (MatchedTeam team : plan.teams()) {
            MatchingGroup group = matchingGroupRepository.save(toGroup(batch, team));
            List<MatchingGroupMember> members = team.candidates().stream()
                    .map(candidate -> toGroupMember(group, byId.get(candidate.applicationId())))
                    .toList();
            matchingGroupMemberRepository.saveAll(members);
            team.candidates()
                    .forEach(candidate -> byId.get(candidate.applicationId()).propose(batch));
        }
        plan.unassignedCandidates()
                .forEach(candidate -> byId.get(candidate.applicationId()).failToMatch(batch));

        // 모든 하위 결과가 준비된 뒤 배치를 성공 처리해 SUCCEEDED 상태가 불완전한 결과를 가리키지 않게 한다.
        matchingApplicationRepository.saveAll(applications);
        batch.succeed(plan, LocalDateTime.now(clock));
        matchingBatchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistFailure(Long batchId, Throwable failure) {
        MatchingBatch batch = getLockedBatch(batchId);
        if (batch.getStatus() != MatchingBatchStatus.RUNNING) {
            return;
        }
        // 이번 배치가 소유한 MATCHING 신청만 WAITING으로 되돌려 다음 실행에서 동일 풀을 재시도할 수 있게 한다.
        List<MatchingApplication> applications = matchingApplicationRepository.findAllByMatchingBatchWithLock(batch);
        applications.forEach(application -> application.restoreWaitingAfterBatchFailure(batch));
        matchingApplicationRepository.saveAll(applications);
        batch.fail(
                "오류 유형=" + failure.getClass().getSimpleName() + ", 메시지=" + failure.getMessage(),
                LocalDateTime.now(clock));
        matchingBatchRepository.saveAndFlush(batch);
    }

    private MatchingGroup toGroup(MatchingBatch batch, MatchedTeam team) {
        // 계산 근거를 다시 산출하지 않고 알고리즘이 반환한 세부 점수를 결과 행에 그대로 고정한다.
        TeamCompatibilityScore score = team.score();
        return MatchingGroup.builder()
                .matchingBatch(batch)
                .category(batch.getCategory())
                .skillGroup(batch.getPoolOrdinal())
                .teamSize(team.candidates().size())
                .matchingScore(score.totalScore())
                .leaderHarmonyScore(score.leaderHarmonyScore())
                .goalSimilarityScore(score.goalSimilarityScore())
                .workStyleSimilarityScore(score.workStyleSimilarityScore())
                .communicationSimilarityScore(score.communicationSimilarityScore())
                .agreeablenessSimilarityScore(score.agreeablenessSimilarityScore())
                .conscientiousnessSimilarityScore(score.conscientiousnessSimilarityScore())
                .honestyHumilitySimilarityScore(score.honestyHumilitySimilarityScore())
                .extroversionComplementScore(score.extroversionComplementScore())
                // 알고리즘이 팀 조합을 만들었다고 즉시 실제 Team이 되는 것은 아니다.
                // 여기서는 사용자의 수락/패스를 기다리는 제안 결과와 다음 날 12시 마감만 저장한다.
                .status(MatchingGroupStatus.PROPOSED)
                .responseDeadlineAt(matchingTimePolicy.responseDeadline(batch.getApplicationDate()))
                .build();
    }

    private MatchingGroupMember toGroupMember(MatchingGroup group, MatchingApplication application) {
        if (application == null) {
            throw new IllegalStateException("매칭 계획에 알 수 없는 신청이 포함되어 있습니다.");
        }
        return MatchingGroupMember.builder()
                .matchingGroup(group)
                .matchingApplication(application)
                .member(application.getMember())
                // 실제 응답은 16시 공개 후 받으므로 결과 저장 시 모든 그룹원을 PENDING으로 시작한다.
                .responseStatus(MatchingGroupMemberStatus.PENDING)
                .build();
    }

    private void validatePlanCoversInput(List<MatchingApplication> applications, MatchingPlan plan) {
        // 선점 후 상태가 바뀐 신청이 있으면 오래된 계산 결과를 저장하지 않는다.
        if (applications.stream()
                .anyMatch(application -> application.getStatus() != MatchingApplicationStatus.MATCHING)) {
            throw new IllegalStateException("매칭 실행 중 선점한 신청의 상태가 변경되었습니다.");
        }
        Set<Long> inputIds = applications.stream()
                .map(MatchingApplication::getMatchingApplicationId)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> resultIds = new HashSet<>();
        plan.teams().stream()
                .flatMap(team -> team.candidates().stream())
                .map(MatchingCandidate::applicationId)
                .forEach(resultIds::add);
        plan.unassignedCandidates().stream()
                .map(MatchingCandidate::applicationId)
                .forEach(resultIds::add);
        // 팀 배정자와 미배정자를 합친 ID 집합이 선점 입력과 정확히 같아야 결과 유실·외부 ID 유입이 없다.
        if (!inputIds.equals(resultIds)) {
            throw new IllegalStateException("매칭 결과는 선점한 풀의 모든 신청을 정확히 한 번씩 포함해야 합니다.");
        }
    }

    private MatchingBatch getLockedBatch(Long batchId) {
        return matchingBatchRepository
                .findByIdWithLock(batchId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 배치를 찾을 수 없습니다. 배치 ID=" + batchId));
    }
}
