package org.cotato.gongmozip.domains.profile.repository;

import java.util.List;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectExperienceRepository extends JpaRepository<ProjectExperience, Long> {
    Page<ProjectExperience> findAllByProfile(Profile profile, Pageable pageable);

    List<ProjectExperience> findAllByProfile(Profile profile);
}
