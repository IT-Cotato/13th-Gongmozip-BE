package org.cotato.gongmozip.domains.matching.repository;

import java.util.List;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingGroupMemberRepository extends JpaRepository<MatchingGroupMember, Long> {
    List<MatchingGroupMember> findAllByMatchingGroup(MatchingGroup matchingGroup);

    boolean existsByMatchingGroupAndMember(MatchingGroup matchingGroup, Member member);
}
