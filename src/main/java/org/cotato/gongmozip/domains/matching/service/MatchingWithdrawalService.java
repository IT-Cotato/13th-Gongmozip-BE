package org.cotato.gongmozip.domains.matching.service;

import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.WithdrawalResponse;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.springframework.stereotype.Service;

/**
 * 기존 신청 철회 API 하나로 결과 생성 전 철회와 결과 생성 후 패스를 모두 처리하는 라우터다.
 *
 * <p>이 클래스에는 쓰기 트랜잭션을 두지 않는다. 분기 후 호출되는 {@link MatchingApplicationService}와
 * {@link MatchingResponseService}가 각자의 상태 변경 범위에 맞는 트랜잭션과 락을 시작한다.
 */
@Service
@RequiredArgsConstructor
public class MatchingWithdrawalService {

    private final MatchingGroupMemberRepository matchingGroupMemberRepository;
    private final MatchingApplicationService matchingApplicationService;
    private final MatchingResponseService matchingResponseService;

    public WithdrawalResponse withdraw(Long memberId, Long applicationId) {
        // 결과 그룹원 행이 있다는 것은 해당 신청이 이미 PROPOSED 결과에 포함됐다는 뜻이다.
        // 컨트롤러가 신청 상태를 보고 분기하지 않게 해, 철회 정책의 진입점을 이곳 하나로 유지한다.
        boolean proposedResultExists = matchingGroupMemberRepository
                .findByMatchingApplication_MatchingApplicationIdAndMember_MemberId(applicationId, memberId)
                .isPresent();
        if (proposedResultExists) {
            // 결과 생성 후 철회는 공개 전후와 관계없이 그룹 응답(PASSED)으로 처리해야 한다.
            return matchingResponseService.pass(memberId, applicationId);
        }
        // 아직 결과 그룹이 없다면 14시 전 무료 취소/14시 이후 패스인 기존 신청 철회 정책을 사용한다.
        return matchingApplicationService.withdraw(memberId, applicationId);
    }
}
