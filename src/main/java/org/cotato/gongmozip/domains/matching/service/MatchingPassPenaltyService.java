package org.cotato.gongmozip.domains.matching.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.stereotype.Service;

/** 수동 패스와 12시 자동 패스가 같은 누적 감점 정책을 사용하도록 계산과 포인트 반영을 모은 서비스다. */
@Service
@RequiredArgsConstructor
public class MatchingPassPenaltyService {

    private static final int FIRST_PASS_PENALTY = 3;
    private static final int PASS_PENALTY_STEP = 2;
    private static final int MAX_PASS_PENALTY = 11;
    private static final int PASS_REPEAT_WINDOW_DAYS = 7;

    private final MatchingApplicationRepository matchingApplicationRepository;
    private final CollaborationPointService collaborationPointService;

    public int calculateNextPenalty(Member member, LocalDateTime now) {
        // 호출 시점의 신청은 아직 PROPOSED이므로 아래 개수에는 과거 패스만 포함된다.
        // 이번 패스를 상태 변경한 뒤 계산하면 자기 자신까지 세어 첫 패스가 5점이 되는 오류가 생긴다.
        long recentPassCount = matchingApplicationRepository.countByMemberAndStatusAndCanceledAtGreaterThanEqual(
                member, MatchingApplicationStatus.PASSED, now.minusDays(PASS_REPEAT_WINDOW_DAYS));
        long calculatedPenalty = FIRST_PASS_PENALTY + recentPassCount * PASS_PENALTY_STEP;
        return (int) Math.min(calculatedPenalty, MAX_PASS_PENALTY);
    }

    public int apply(Member member, LocalDateTime now) {
        int penalty = calculateNextPenalty(member, now);
        // 실제 포인트 원장에는 음수 delta를 기록한다. 반환한 양수 penalty는 호출부가
        // MatchingGroupMember에 저장해 해당 패스 응답의 정책값과 멱등 응답을 보존한다.
        collaborationPointService.changePoint(member, null, CollaborationPointReason.MATCHING_PASS_PENALTY, -penalty);
        return penalty;
    }
}
