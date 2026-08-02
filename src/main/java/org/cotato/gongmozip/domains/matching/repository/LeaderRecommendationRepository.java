package org.cotato.gongmozip.domains.matching.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.LeaderRecommendation;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaderRecommendationRepository extends JpaRepository<LeaderRecommendation, Long> {
    Optional<LeaderRecommendation> findByTeam(Team team);

    Optional<LeaderRecommendation> findByTeamTeamId(Long teamId);
}
