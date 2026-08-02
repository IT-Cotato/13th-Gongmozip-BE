package org.cotato.gongmozip.domains.matching.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingExplanation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingExplanationRepository extends JpaRepository<MatchingExplanation, Long> {
    Optional<MatchingExplanation> findFirstByOrderByCreatedAtDesc();
}
