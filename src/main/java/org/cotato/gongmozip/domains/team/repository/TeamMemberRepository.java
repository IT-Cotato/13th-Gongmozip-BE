package org.cotato.gongmozip.domains.team.repository;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @Query(
            """
            SELECT tm FROM TeamMember tm
            JOIN FETCH tm.profile
            WHERE tm.team.teamId = :teamId AND tm.status = :status
            """)
    List<TeamMember> findByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") TeamMemberStatus status);

    @Query(
            """
            SELECT tm FROM TeamMember tm
            JOIN FETCH tm.team
            WHERE tm.member.memberId = :memberId AND tm.status = :status
            """)
    List<TeamMember> findByMemberIdAndStatus(
            @Param("memberId") Long memberId, @Param("status") TeamMemberStatus status);

    Optional<TeamMember> findByTeam_TeamIdAndMember_MemberId(Long teamId, Long memberId);
}
