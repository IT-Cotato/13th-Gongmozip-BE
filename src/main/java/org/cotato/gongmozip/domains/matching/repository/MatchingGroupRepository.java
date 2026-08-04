package org.cotato.gongmozip.domains.matching.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchingGroupRepository extends JpaRepository<MatchingGroup, Long> {

    // 한 배치가 생성한 모든 팀을 조회해 배치 결과의 완전성을 확인한다.
    List<MatchingGroup> findAllByMatchingBatch(MatchingBatch matchingBatch);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT matchingGroup FROM MatchingGroup matchingGroup WHERE matchingGroup.matchingGroupId = :groupId")
    Optional<MatchingGroup> findByIdWithLock(@Param("groupId") Long groupId);

    @Query(
            """
            SELECT matchingGroup.matchingGroupId
            FROM MatchingGroup matchingGroup
            WHERE matchingGroup.status = :status
              AND matchingGroup.responseDeadlineAt <= :now
            ORDER BY matchingGroup.matchingGroupId
            """)
    List<Long> findDueGroupIds(@Param("status") MatchingGroupStatus status, @Param("now") LocalDateTime now);
}
