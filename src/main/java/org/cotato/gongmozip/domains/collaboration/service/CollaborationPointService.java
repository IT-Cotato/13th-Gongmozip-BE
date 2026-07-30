package org.cotato.gongmozip.domains.collaboration.service;

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

    private final CollaborationPointHistoryRepository collaborationPointHistoryRepository;

    @Transactional
    public void awardPoint(Member member, Team team, CollaborationPointReason reason) {
        int delta = reason.getDefaultDelta();
        member.addCollaborationPoint(delta);

        collaborationPointHistoryRepository.save(CollaborationPointHistory.builder()
                .member(member)
                .team(team)
                .delta(delta)
                .reasonCode(reason)
                .build());
    }
}
