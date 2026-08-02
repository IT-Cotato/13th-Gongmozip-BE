package org.cotato.gongmozip.domains.matching.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingReason;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingReasonRepository extends JpaRepository<MatchingReason, Long> {
    Optional<MatchingReason> findByMatchingGroup(MatchingGroup matchingGroup);

    Optional<MatchingReason> findByMatchingGroupMatchingGroupId(Long matchingGroupId);
}
