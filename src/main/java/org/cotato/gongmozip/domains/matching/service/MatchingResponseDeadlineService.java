package org.cotato.gongmozip.domains.matching.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingReassignmentReason;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다음 날 12시가 지난 PROPOSED 그룹을 마감하는 서비스다.
 *
 * <p>대상 ID 조회와 그룹별 쓰기 처리를 분리한다. 긴 하나의 트랜잭션으로 모든 그룹을 묶지 않아 한
 * 그룹의 실패가 이미 처리한 다른 그룹까지 롤백시키지 않는다.
 */
@Service
@RequiredArgsConstructor
public class MatchingResponseDeadlineService {

    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingGroupMemberRepository matchingGroupMemberRepository;
    private final MemberRepository memberRepository;
    private final MatchingPassPenaltyService matchingPassPenaltyService;
    private final MatchingReassignmentService matchingReassignmentService;
    private final MatchingGroupCompletionService matchingGroupCompletionService;
    private final MatchingTimePolicy matchingTimePolicy;

    @Transactional(readOnly = true)
    public List<Long> findDueGroupIds() {
        // 이 단계에서는 락을 잡지 않고 처리 대상 ID만 짧게 읽는다. 실제 상태는 processGroup에서
        // 그룹 락을 얻은 뒤 다시 확인하므로 조회 직후 수락/패스가 들어와도 안전하다.
        return matchingGroupRepository.findDueGroupIds(MatchingGroupStatus.PROPOSED, matchingTimePolicy.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processGroup(Long groupId) {
        // 스케줄러가 이 메서드를 Spring 프록시를 통해 그룹마다 호출하므로 매 호출이 독립 트랜잭션이다.
        MatchingGroup group = matchingGroupRepository.findByIdWithLock(groupId).orElse(null);

        // 이미 다른 요청이 CONFIRMED/CANCELED/EXPIRED로 닫았거나 앞선 실행이 처리한 그룹이면
        // 아무것도 하지 않는다. 마감 작업 재실행의 첫 번째 멱등성 방어다.
        if (group == null || group.getStatus() != MatchingGroupStatus.PROPOSED) {
            return;
        }
        LocalDateTime now = matchingTimePolicy.now();

        // 대상 ID 조회와 락 획득 사이에 시간이 바뀌거나 잘못된 ID가 전달될 수 있어 락 안에서 마감
        // 시각을 다시 검사한다. 12시 정각은 처리 대상이다.
        if (group.getResponseDeadlineAt() == null || now.isBefore(group.getResponseDeadlineAt())) {
            return;
        }

        // 수동 응답과 같은 순서로 그룹원 전체를 잠근다: MatchingGroup → GroupMember ID 오름차순.
        List<MatchingGroupMember> groupMembers =
                matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group).stream()
                        .sorted(Comparator.comparing(MatchingGroupMember::getMatchingGroupMemberId))
                        .toList();
        List<MatchingGroupMember> pendingMembers = groupMembers.stream()
                .filter(member -> member.getResponseStatus() == MatchingGroupMemberStatus.PENDING)
                .toList();

        // 한 그룹에 미응답자가 여러 명이면 모든 Member를 ID 오름차순으로 먼저 잠근 뒤 포인트를
        // 변경한다. 여러 그룹/요청이 같은 회원 락을 잡을 때 순서가 달라 생기는 교착을 줄이기 위함이다.
        Map<Long, Member> lockedMembers = pendingMembers.stream()
                .map(member -> member.getMember().getMemberId())
                .distinct()
                .sorted()
                .map(memberId -> memberRepository
                        .findByIdWithLock(memberId)
                        .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND)))
                .collect(Collectors.toMap(Member::getMemberId, Function.identity()));

        for (MatchingGroupMember pendingMember : pendingMembers) {
            Member lockedMember = lockedMembers.get(pendingMember.getMember().getMemberId());

            // 패널티를 먼저 계산한 뒤 신청을 PASSED로 바꾼다. 그래야 이번 신청이 자신의 최근 패스
            // 횟수에 포함돼 단계가 하나 더 올라가는 오류를 피할 수 있다.
            int penalty = matchingPassPenaltyService.apply(lockedMember, now);
            pendingMember.expire(now, penalty);

            // 그룹원 응답은 자동 처리 원인을 보존하기 위해 EXPIRED로 두지만, 신청은 이후 누적 패스
            // 계산에 포함돼야 하므로 PASSED로 기록한다.
            pendingMember.getMatchingApplication().pass(now);
        }

        // 이제 PENDING은 모두 EXPIRED다. ACCEPTED가 3명 이상이면 이 호출이 3/4인 Team을 만들고
        // 그룹을 CONFIRMED로 바꾼다.
        if (matchingGroupCompletionService
                .completeIfReady(group, groupMembers, now)
                .isPresent()) {
            return;
        }

        // Team을 만들지 못했다면 그룹을 EXPIRED로 닫는다. 미응답 당사자는 제외하고, 이미 수락해
        // 피해를 본 사용자만 다음 날짜 자동 재매칭 대상으로 등록한다.
        group.expire(now);
        groupMembers.stream()
                .filter(member -> member.getResponseStatus() == MatchingGroupMemberStatus.ACCEPTED)
                .forEach(member -> matchingReassignmentService.register(
                        member.getMatchingApplication(), MatchingReassignmentReason.RESPONSE_DEADLINE_EXPIRED));
    }
}
