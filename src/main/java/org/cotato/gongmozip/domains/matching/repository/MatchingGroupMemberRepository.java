package org.cotato.gongmozip.domains.matching.repository;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchingGroupMemberRepository extends JpaRepository<MatchingGroupMember, Long> {
    List<MatchingGroupMember> findAllByMatchingGroup(MatchingGroup matchingGroup);

    // 본인의 원본 신청으로 제안 그룹을 찾고 결과 점수와 배치 공개 시각을 한 번에 로딩한다.
    @Query(
            """
            SELECT groupMember
            FROM MatchingGroupMember groupMember
            JOIN FETCH groupMember.matchingGroup matchingGroup
            JOIN FETCH matchingGroup.matchingBatch
            WHERE groupMember.matchingApplication = :application
            """)
    Optional<MatchingGroupMember> findResultMembership(@Param("application") MatchingApplication application);

    // 결과 화면에 필요한 3명 또는 4명의 원본 신청·회원·선택 프로필을 한 번에 조회한다.
    @Query(
            """
            SELECT groupMember
            FROM MatchingGroupMember groupMember
            JOIN FETCH groupMember.member
            JOIN FETCH groupMember.matchingApplication application
            JOIN FETCH application.profile
            WHERE groupMember.matchingGroup = :matchingGroup
            ORDER BY application.matchingApplicationId
            """)
    List<MatchingGroupMember> findResultMembers(@Param("matchingGroup") MatchingGroup matchingGroup);

    boolean existsByMatchingGroupAndMember(MatchingGroup matchingGroup, Member member);
}
