package org.cotato.gongmozip.domains.contest.repository;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.contest.entity.ContestCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestCandidateRepository extends JpaRepository<ContestCandidate, Long> {

    @Query(
            """
            SELECT cc FROM ContestCandidate cc
            JOIN FETCH cc.contest
            WHERE cc.team.teamId = :teamId
            """)
    List<ContestCandidate> findByTeamId(@Param("teamId") Long teamId);

    Optional<ContestCandidate> findByTeam_TeamIdAndContestCandidateId(Long teamId, Long contestCandidateId);

    boolean existsByTeam_TeamIdAndContest_ContestId(Long teamId, Long contestId);
}
