package org.cotato.gongmozip.domains.collaboration.repository;

import org.cotato.gongmozip.domains.collaboration.entity.CollaborationPointHistory;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollaborationPointHistoryRepository extends JpaRepository<CollaborationPointHistory, Long> {

    boolean existsByMember_MemberIdAndTeam_TeamIdAndReasonCode(
            Long memberId, Long teamId, CollaborationPointReason reasonCode);
}
