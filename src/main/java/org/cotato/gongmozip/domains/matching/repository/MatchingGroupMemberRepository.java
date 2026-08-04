package org.cotato.gongmozip.domains.matching.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchingGroupMemberRepository extends JpaRepository<MatchingGroupMember, Long> {
    List<MatchingGroupMember> findAllByMatchingGroup(MatchingGroup matchingGroup);

    Optional<MatchingGroupMember> findByMatchingGroup_MatchingGroupIdAndMember_MemberId(
            Long matchingGroupId, Long memberId);

    Optional<MatchingGroupMember> findByMatchingApplication_MatchingApplicationIdAndMember_MemberId(
            Long applicationId, Long memberId);

    @Query(
            """
            SELECT groupMember.matchingGroup.matchingGroupId
            FROM MatchingGroupMember groupMember
            WHERE groupMember.matchingApplication.matchingApplicationId = :applicationId
              AND groupMember.member.memberId = :memberId
            """)
    Optional<Long> findGroupIdByApplicationAndMember(
            @Param("applicationId") Long applicationId, @Param("memberId") Long memberId);

    boolean existsByMatchingGroup_MatchingGroupIdAndMember_MemberId(Long matchingGroupId, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT groupMember
            FROM MatchingGroupMember groupMember
            WHERE groupMember.matchingGroup = :matchingGroup
            ORDER BY groupMember.matchingGroupMemberId
            """)
    List<MatchingGroupMember> findAllByMatchingGroupWithLock(@Param("matchingGroup") MatchingGroup matchingGroup);

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

    @Query(
            """
            SELECT CASE WHEN COUNT(groupMember) > 0 THEN TRUE ELSE FALSE END
            FROM MatchingGroupMember groupMember
            JOIN groupMember.matchingGroup matchingGroup
            WHERE groupMember.member.memberId = :memberId
              AND matchingGroup.status = :groupStatus
              AND groupMember.responseStatus IN :responseStatuses
            """)
    boolean existsOpenResponseForMember(
            @Param("memberId") Long memberId,
            @Param("groupStatus") MatchingGroupStatus groupStatus,
            @Param("responseStatuses") List<MatchingGroupMemberStatus> responseStatuses);

    @Query(
            """
            SELECT application
            FROM MatchingGroupMember groupMember
            JOIN groupMember.matchingApplication application
            JOIN FETCH application.member
            JOIN FETCH application.profile
            LEFT JOIN FETCH application.matchingBatch
            JOIN groupMember.matchingGroup matchingGroup
            WHERE groupMember.member.memberId = :memberId
              AND matchingGroup.status = :groupStatus
            ORDER BY application.applicationDate DESC, application.matchingApplicationId DESC
            """)
    List<MatchingApplication> findOpenResultApplications(
            @Param("memberId") Long memberId, @Param("groupStatus") MatchingGroupStatus groupStatus, Pageable pageable);
}
