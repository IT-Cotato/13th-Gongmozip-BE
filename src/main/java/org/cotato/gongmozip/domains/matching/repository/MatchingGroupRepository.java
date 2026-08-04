package org.cotato.gongmozip.domains.matching.repository;

import java.util.List;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingGroupRepository extends JpaRepository<MatchingGroup, Long> {

    // 한 배치가 생성한 모든 팀을 조회해 배치 결과의 완전성을 확인한다.
    List<MatchingGroup> findAllByMatchingBatch(MatchingBatch matchingBatch);
}
