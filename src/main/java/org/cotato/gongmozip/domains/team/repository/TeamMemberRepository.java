package org.cotato.gongmozip.domains.team.repository;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    // profile뿐 아니라 member도 함께 fetch join한다 — 협업거리 포인트 지급 등에서
    // teamMember.getMember()를 호출하는 곳이 많아, 안 그러면 팀원 수만큼 추가 SELECT가 나간다.
    @Query(
            """
            SELECT tm FROM TeamMember tm
            JOIN FETCH tm.profile
            JOIN FETCH tm.member
            WHERE tm.team.teamId = :teamId AND tm.status = :status
            """)
    List<TeamMember> findByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") TeamMemberStatus status);

    @Query(
            """
            SELECT tm FROM TeamMember tm
            JOIN FETCH tm.profile
            JOIN FETCH tm.member
            WHERE tm.team.teamId IN :teamIds AND tm.status = :status
            """)
    List<TeamMember> findByTeamIdInAndStatus(
            @Param("teamIds") List<Long> teamIds, @Param("status") TeamMemberStatus status);

    @Query(
            """
            SELECT tm FROM TeamMember tm
            JOIN FETCH tm.team
            WHERE tm.member.memberId = :memberId AND tm.status = :status
            """)
    List<TeamMember> findByMemberIdAndStatus(
            @Param("memberId") Long memberId, @Param("status") TeamMemberStatus status);

    Optional<TeamMember> findByTeam_TeamIdAndMember_MemberId(Long teamId, Long memberId);

    @Query(
            value =
                    """
            SELECT tm FROM TeamMember tm
            JOIN FETCH tm.team t
            LEFT JOIN FETCH t.contest
            WHERE tm.member.memberId = :memberId
              AND tm.status = :status
              AND t.status NOT IN :completedStatuses
            ORDER BY t.createdAt DESC, t.teamId DESC
            """,
            countQuery =
                    """
            SELECT COUNT(tm) FROM TeamMember tm
            JOIN tm.team t
            WHERE tm.member.memberId = :memberId
              AND tm.status = :status
              AND t.status NOT IN :completedStatuses
            """)
    Page<TeamMember> findOngoingProjects(
            @Param("memberId") Long memberId,
            @Param("status") TeamMemberStatus status,
            @Param("completedStatuses") List<TeamStatus> completedStatuses,
            Pageable pageable);

    @Query(
            """
            SELECT COUNT(tm) FROM TeamMember tm
            JOIN tm.team t
            WHERE tm.member.memberId = :memberId
              AND tm.status = :status
              AND t.status NOT IN :completedStatuses
            """)
    int countOngoingProjects(
            @Param("memberId") Long memberId,
            @Param("status") TeamMemberStatus status,
            @Param("completedStatuses") List<TeamStatus> completedStatuses);

    @Query(
            value =
                    """
            SELECT tm FROM TeamMember tm
            JOIN FETCH tm.team t
            LEFT JOIN FETCH t.contest
            WHERE tm.member.memberId = :memberId
              AND tm.status = :status
              AND t.status IN :teamStatuses
              AND tm.isCompletedProjectDeleted = false
            ORDER BY t.completedAt DESC, t.updatedAt DESC, t.teamId DESC
            """,
            countQuery =
                    """
            SELECT COUNT(tm) FROM TeamMember tm
            JOIN tm.team t
            WHERE tm.member.memberId = :memberId
              AND tm.status = :status
              AND t.status IN :teamStatuses
              AND tm.isCompletedProjectDeleted = false
            """)
    Page<TeamMember> findCompletedProjects(
            @Param("memberId") Long memberId,
            @Param("status") TeamMemberStatus status,
            @Param("teamStatuses") List<TeamStatus> teamStatuses,
            Pageable pageable);

    @Query(
            """
            SELECT tm FROM TeamMember tm
            JOIN FETCH tm.team t
            LEFT JOIN FETCH t.contest
            WHERE tm.member.memberId = :memberId
              AND tm.status = :status
              AND t.status IN :completedStatuses
              AND tm.isCompletedProjectDeleted = false
            """)
    List<TeamMember> findCompletedProjectsAll(
            @Param("memberId") Long memberId,
            @Param("status") TeamMemberStatus status,
            @Param("completedStatuses") List<TeamStatus> completedStatuses);
}
