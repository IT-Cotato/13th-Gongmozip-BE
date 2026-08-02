package org.cotato.gongmozip.domains.team.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByStatusAndContestCandidateDeadlineAtLessThanEqual(TeamStatus status, LocalDateTime now);

    List<Team> findByStatusAndProgressCheckAtLessThanEqualAndProgressCheckNotifiedAtIsNull(
            TeamStatus status, LocalDateTime now);

    List<Team> findByStatusAndSubmissionCheckAtLessThanEqualAndSubmissionCheckNotifiedAtIsNull(
            TeamStatus status, LocalDateTime now);

    List<Team> findByStatusAndCreatedAtLessThanEqual(TeamStatus status, LocalDateTime cutoff);
}
