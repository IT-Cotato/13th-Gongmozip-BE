package org.cotato.gongmozip.domains.survey.repository;

import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.survey.entity.MatchingApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingApplicationRepository extends JpaRepository<MatchingApplication, Long> {
    boolean existsByProfile(Profile profile);
}
