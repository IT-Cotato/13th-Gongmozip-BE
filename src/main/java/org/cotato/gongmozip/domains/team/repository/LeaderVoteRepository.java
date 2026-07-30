package org.cotato.gongmozip.domains.team.repository;

import java.util.List;
import org.cotato.gongmozip.domains.team.entity.LeaderVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaderVoteRepository extends JpaRepository<LeaderVote, Long> {

    List<LeaderVote> findByTeam_TeamIdAndRound(Long teamId, int round);

    boolean existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(Long teamId, Long voterTeamMemberId, int round);

    boolean existsByTeam_TeamId(Long teamId);

    @Query("SELECT MAX(lv.round) FROM LeaderVote lv WHERE lv.team.teamId = :teamId")
    Integer findMaxRoundByTeamId(@Param("teamId") Long teamId);
}
