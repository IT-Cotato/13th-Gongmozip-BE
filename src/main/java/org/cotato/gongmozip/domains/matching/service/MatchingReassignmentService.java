package org.cotato.gongmozip.domains.matching.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.enums.MatchingReassignmentReason;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.springframework.stereotype.Service;

/**
 * 성사되지 못한 그룹의 피해자에게 다음 날짜 신청을 생성한다.
 *
 * <p>원본 신청을 새 배치에 다시 연결하지 않고 새 신청 행을 만든다. 그래야 원본 배치·그룹 이력을
 * 보존하면서 한 신청이 하나의 배치에만 속한다는 기존 모델을 지킬 수 있다.
 */
@Service
@RequiredArgsConstructor
public class MatchingReassignmentService {

    private final MatchingApplicationRepository matchingApplicationRepository;
    private final MatchingTimePolicy matchingTimePolicy;

    public MatchingApplication register(MatchingApplication sourceApplication, MatchingReassignmentReason reason) {
        LocalDate sourceDate = sourceApplication.getApplicationDate();
        if (sourceDate == null) {
            throw new MatchingException(MatchingErrorCode.MATCHING_REASSIGNMENT_CONFLICT);
        }
        // 원본 신청 다음 날부터 시작하되, 지연 처리로 이미 14시 매칭이 시작된 날짜는 건너뛴다.
        LocalDate targetDate = matchingTimePolicy.nextAvailableMatchingDate(sourceDate.plusDays(1));

        // (member_id, application_date) 유니크 정책을 애플리케이션 단계에서도 먼저 확인한다.
        // 같은 자동 재매칭을 재실행한 경우는 멱등하게 반환하고, 직접 신청과 충돌하면 전체 그룹 처리를 롤백한다.
        return matchingApplicationRepository
                .findByMemberAndApplicationDate(sourceApplication.getMember(), targetDate)
                .map(existing -> validateExisting(existing, sourceApplication))
                .orElseGet(() -> create(sourceApplication, targetDate, reason));
    }

    private MatchingApplication create(
            MatchingApplication sourceApplication, LocalDate targetDate, MatchingReassignmentReason reason) {
        // 두 상태 변경은 수동 패스/마감 처리의 바깥 트랜잭션 안에서 함께 커밋된다.
        // 따라서 새 신청 저장이 실패하면 원본도 REASSIGN_PENDING으로 홀로 남지 않는다.
        sourceApplication.waitForReassignment();
        return matchingApplicationRepository.save(sourceApplication.createReassignment(targetDate, reason));
    }

    private MatchingApplication validateExisting(MatchingApplication existing, MatchingApplication sourceApplication) {
        MatchingApplication existingSource = existing.getSourceApplication();
        // 동일 원본에서 이미 만든 행이면 스케줄러/요청 재실행으로 보고 기존 행을 재사용한다.
        if (existingSource != null
                && existingSource.getMatchingApplicationId().equals(sourceApplication.getMatchingApplicationId())) {
            return existing;
        }
        throw new MatchingException(MatchingErrorCode.MATCHING_REASSIGNMENT_CONFLICT);
    }
}
