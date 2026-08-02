package org.cotato.gongmozip.domains.collaboration.service;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.collaboration.entity.CollaborationPointHistory;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.repository.CollaborationPointHistoryRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 협업거리 포인트 적립/차감을 기록하는 공용 서비스. 팀 나가기, 중간점검 응답, 프로젝트 완주,
 * 리뷰 작성 등 여러 트리거 지점에서 호출한다 (docs/decisions/06-collaboration-point.md).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollaborationPointService {

    private static final int LOSS_LOOKBACK_DAYS = 14;
    private static final int MATCHING_BLOCK_THRESHOLD = 50;
    private static final int MATCHING_BLOCK_DAYS = 7;

    private final CollaborationPointHistoryRepository collaborationPointHistoryRepository;
    private final Clock clock;

    // enum에 정의된 기본 변화량을 적용하는 일반 적립·차감 진입점
    @Transactional
    public void awardPoint(Member member, Team team, CollaborationPointReason reason) {
        changePoint(member, team, reason, reason.getDefaultDelta());
    }

    // 패스처럼 횟수에 따라 변화량이 달라지는 경우 직접 delta를 전달한다
    @Transactional
    public void changePoint(Member member, Team team, CollaborationPointReason reason, int delta) {
        int beforeChange = member.getCollaborationPoint();
        member.addCollaborationPoint(delta);
        // 0~500 클램핑 후 실제로 변한 값만 이력과 최근 감점 합계에 반영한다
        int actualDelta = member.getCollaborationPoint() - beforeChange;

        if (actualDelta < 0) {
            applyMatchingRestrictionIfNeeded(member, actualDelta);
        }

        collaborationPointHistoryRepository.save(CollaborationPointHistory.builder()
                .member(member)
                .team(team)
                .delta(actualDelta)
                .reasonCode(reason)
                .build());
    }

    // 최근 14일 실제 감점 합계가 이번 변경으로 50m 이상이면 그 시점부터 7일간 매칭을 제한한다
    private void applyMatchingRestrictionIfNeeded(Member member, int delta) {
        LocalDateTime now = LocalDateTime.now(clock);
        long recentLoss = collaborationPointHistoryRepository.sumLossSince(member, now.minusDays(LOSS_LOOKBACK_DAYS));
        long lossAfterChange = recentLoss + Math.abs((long) delta);
        if (lossAfterChange >= MATCHING_BLOCK_THRESHOLD && !member.isMatchingBlockedAt(now)) {
            member.blockMatchingUntil(now.plusDays(MATCHING_BLOCK_DAYS));
        }
    }
}
