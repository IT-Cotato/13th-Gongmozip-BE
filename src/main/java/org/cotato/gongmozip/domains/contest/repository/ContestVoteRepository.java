package org.cotato.gongmozip.domains.contest.repository;

import java.util.List;
import org.cotato.gongmozip.domains.contest.entity.ContestVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestVoteRepository extends JpaRepository<ContestVote, Long> {

    List<ContestVote> findByTeam_TeamIdAndRound(Long teamId, int round);

    boolean existsByContestCandidate_ContestCandidateIdAndVoterTeamMember_TeamMemberIdAndRound(
            Long contestCandidateId, Long voterTeamMemberId, int round);

    boolean existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(Long teamId, Long voterTeamMemberId, int round);

    @Query("SELECT MAX(cv.round) FROM ContestVote cv WHERE cv.team.teamId = :teamId")
    Integer findMaxRoundByTeamId(@Param("teamId") Long teamId);

    @Query(
            """
            SELECT COUNT(DISTINCT cv.voterTeamMember.teamMemberId) FROM ContestVote cv
            WHERE cv.team.teamId = :teamId AND cv.round = :round
            """)
    long countDistinctVotersByTeamIdAndRound(@Param("teamId") Long teamId, @Param("round") int round);
}
