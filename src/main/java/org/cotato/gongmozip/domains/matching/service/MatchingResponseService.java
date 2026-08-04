package org.cotato.gongmozip.domains.matching.service;

import static org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.WithdrawalResponse;
import static org.cotato.gongmozip.domains.matching.dto.response.MatchingGroupResponse.AcceptResponse;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.converter.MatchingApplicationConverter;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingReassignmentReason;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공개된 매칭 결과의 수락과 패스를 처리하는 쓰기 서비스다.
 *
 * <p>같은 그룹의 수락·패스·자동 마감은 항상 그룹 락에서 직렬화한다. 상태를 검사한 뒤 락을 잡는
 * 방식은 두 요청이 모두 오래된 PENDING을 볼 수 있으므로, 실제 엔티티 상태 판정은 락 획득 뒤에만
 * 수행한다.
 */
@Service
@RequiredArgsConstructor
public class MatchingResponseService {

    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingGroupMemberRepository matchingGroupMemberRepository;
    private final MemberRepository memberRepository;
    private final MatchingPassPenaltyService matchingPassPenaltyService;
    private final MatchingReassignmentService matchingReassignmentService;
    private final MatchingGroupCompletionService matchingGroupCompletionService;
    private final MatchingTimePolicy matchingTimePolicy;

    @Transactional
    public AcceptResponse accept(Long memberId, Long groupId) {
        // 락 전에 그룹/그룹원 엔티티를 읽지 않는다. 영속성 컨텍스트에 오래된 응답 상태가 남는 것을
        // 피하고, 아래 쓰기 락 조회가 다른 트랜잭션의 최신 커밋 상태를 읽게 하기 위해서다.
        MatchingGroup group = getLockedGroup(groupId);
        List<MatchingGroupMember> groupMembers = getLockedGroupMembers(group);
        MatchingGroupMember requester = findRequester(groupMembers, memberId);
        LocalDateTime now = matchingTimePolicy.now();
        validatePublished(group, now);

        // 네트워크 재시도는 성공으로 응답한다. 마지막 수락으로 그룹이 이미 CONFIRMED됐더라도
        // 이 검사를 그룹 상태 검사보다 먼저 해야 기존 teamId를 멱등하게 돌려줄 수 있다.
        if (requester.getResponseStatus() == MatchingGroupMemberStatus.ACCEPTED) {
            return toAcceptResponse(group, requester);
        }
        // 수락은 번복할 수 없으므로 PASSED/EXPIRED 상태에서는 다시 응답하지 못한다.
        if (requester.getResponseStatus() != MatchingGroupMemberStatus.PENDING) {
            throw new MatchingException(MatchingErrorCode.MATCHING_RESPONSE_ALREADY_SUBMITTED);
        }
        validateOpenGroup(group);
        validateBeforeDeadline(group, now);

        requester.accept(now);
        // 아직 다른 PENDING 사용자가 있으면 Optional.empty()로 끝나고 이번 사용자의 수락만 커밋된다.
        // 마지막 필요한 응답이라면 같은 트랜잭션에서 실제 Team 생성까지 진행한다.
        matchingGroupCompletionService.completeIfReady(group, groupMembers, now);
        return toAcceptResponse(group, requester);
    }

    @Transactional
    public WithdrawalResponse pass(Long memberId, Long applicationId) {
        // 여기서는 엔티티 대신 groupId 스칼라만 먼저 얻는다. 락 전에 MatchingGroupMember를 영속성
        // 컨텍스트에 올리면 동시 수락이 커밋한 최신 상태 대신 오래된 PENDING을 사용할 수 있다.
        Long groupId = matchingGroupMemberRepository
                .findGroupIdByApplicationAndMember(applicationId, memberId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.APPLICATION_NOT_FOUND));
        MatchingGroup group = getLockedGroup(groupId);
        List<MatchingGroupMember> groupMembers = getLockedGroupMembers(group);

        // URL의 신청 ID와 인증 회원 ID를 함께 비교해 다른 그룹원의 신청을 대신 패스하지 못하게 한다.
        MatchingGroupMember requester = groupMembers.stream()
                .filter(member ->
                        applicationId.equals(member.getMatchingApplication().getMatchingApplicationId()))
                .filter(member -> memberId.equals(member.getMember().getMemberId()))
                .findFirst()
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.APPLICATION_NOT_FOUND));
        LocalDateTime now = matchingTimePolicy.now();

        // 통합 철회는 결과 공개 여부와 관계없이 14시 이후 패널티 패스로 허용한다.
        // 수락은 위 accept()에서 공개 여부를 계속 검증하므로 공개 전 결과를 확정할 수는 없다.

        // 이미 성공한 패스 요청의 재시도는 저장해 둔 penalty를 그대로 반환한다.
        // Member 락과 포인트 서비스를 다시 거치지 않으므로 이중 감점이 발생하지 않는다.
        if (requester.getResponseStatus() == MatchingGroupMemberStatus.PASSED) {
            return toPassResponse(requester, requester.getPassPenalty() == null ? 0 : requester.getPassPenalty());
        }
        // ACCEPTED는 최종 의사결정이며 패스로 번복할 수 없다. EXPIRED도 수동 응답 대상이 아니다.
        if (requester.getResponseStatus() != MatchingGroupMemberStatus.PENDING) {
            throw new MatchingException(MatchingErrorCode.MATCHING_RESPONSE_ALREADY_SUBMITTED);
        }
        validateOpenGroup(group);
        validateBeforeDeadline(group, now);

        // 공통 잠금 순서의 마지막 단계다: MatchingGroup → GroupMember ID 순 → Member.
        // 포인트 변경까지 Member 락 안에서 수행해 같은 회원의 다른 감점 요청과 충돌하지 않게 한다.
        Member lockedMember = memberRepository
                .findByIdWithLock(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 현재 신청을 PASSED로 바꾸기 전에 계산해야 최근 패스 횟수에 자기 자신이 포함되지 않는다.
        int penalty = matchingPassPenaltyService.apply(lockedMember, now);
        requester.pass(now, penalty);
        requester.getMatchingApplication().pass(now);

        // active는 PENDING 또는 ACCEPTED다. 방금 PASSED로 바뀐 요청자는 여기서 자연스럽게 제외된다.
        long activeMemberCount = groupMembers.stream()
                .filter(MatchingGroupMember::isActiveResponseTarget)
                .count();
        if (activeMemberCount >= 3) {
            // 4인 제안에서 한 명이 패스해도 남은 세 명을 해체하지 않는다.
            // 세 명 모두 ACCEPTED면 즉시 3인 Team을 만들고, PENDING이 있으면 마감까지 계속 기다린다.
            matchingGroupCompletionService.completeIfReady(group, groupMembers, now);
        } else {
            // 3명 미만으로는 Team을 만들 수 없다. 패스 당사자를 제외한 active 피해자만 다음 날짜
            // 재매칭 대상으로 등록하고, 원본 그룹은 이력 보존을 위해 CANCELED로 닫는다.
            group.cancel(now);
            groupMembers.stream()
                    .filter(MatchingGroupMember::isActiveResponseTarget)
                    .forEach(member -> matchingReassignmentService.register(
                            member.getMatchingApplication(), MatchingReassignmentReason.GROUP_MEMBER_PASSED));
        }
        return MatchingApplicationConverter.toWithdrawalResponse(
                requester.getMatchingApplication(),
                WithdrawalType.PENALIZED_PASS,
                penalty,
                lockedMember.getCollaborationPoint());
    }

    private MatchingGroup getLockedGroup(Long groupId) {
        return matchingGroupRepository
                .findByIdWithLock(groupId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_GROUP_NOT_FOUND));
    }

    private List<MatchingGroupMember> getLockedGroupMembers(MatchingGroup group) {
        // Repository 쿼리도 ORDER BY를 사용하지만 서비스에서도 다시 정렬해, 이후 로직이 항상 같은
        // 순서를 사용한다는 계약을 명시적으로 유지한다. 고정 순서는 그룹 간 교착 가능성을 줄인다.
        return matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group).stream()
                .sorted(Comparator.comparing(MatchingGroupMember::getMatchingGroupMemberId))
                .toList();
    }

    private MatchingGroupMember findRequester(List<MatchingGroupMember> groupMembers, Long memberId) {
        return groupMembers.stream()
                .filter(member -> memberId.equals(member.getMember().getMemberId()))
                .findFirst()
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_GROUP_ACCESS_DENIED));
    }

    private void validatePublished(MatchingGroup group, LocalDateTime now) {
        if (group.getMatchingBatch() == null
                || group.getMatchingBatch().getApplicationDate() == null
                || !matchingTimePolicy.isResultPublished(
                        group.getMatchingBatch().getApplicationDate(), now)) {
            throw new MatchingException(MatchingErrorCode.MATCHING_RESULT_NOT_PUBLISHED);
        }
    }

    private void validateBeforeDeadline(MatchingGroup group, LocalDateTime now) {
        // now < deadline인 경우만 허용하므로 다음 날 12:00:00 정각부터는 마감 이후다.
        if (group.getResponseDeadlineAt() == null || !now.isBefore(group.getResponseDeadlineAt())) {
            throw new MatchingException(MatchingErrorCode.MATCHING_RESPONSE_DEADLINE_PASSED);
        }
    }

    private void validateOpenGroup(MatchingGroup group) {
        if (group.getStatus() != MatchingGroupStatus.PROPOSED) {
            throw new MatchingException(MatchingErrorCode.MATCHING_GROUP_ALREADY_CLOSED);
        }
    }

    private AcceptResponse toAcceptResponse(MatchingGroup group, MatchingGroupMember requester) {
        Long teamId = group.getTeam() != null && requester.getResponseStatus() == MatchingGroupMemberStatus.ACCEPTED
                ? group.getTeam().getTeamId()
                : null;
        return new AcceptResponse(
                group.getMatchingGroupId(),
                group.getStatus(),
                requester.getResponseStatus(),
                group.getConfirmedTeamSize(),
                teamId);
    }

    private WithdrawalResponse toPassResponse(MatchingGroupMember requester, int penalty) {
        return MatchingApplicationConverter.toWithdrawalResponse(
                requester.getMatchingApplication(),
                WithdrawalType.PENALIZED_PASS,
                penalty,
                requester.getMember().getCollaborationPoint());
    }
}
