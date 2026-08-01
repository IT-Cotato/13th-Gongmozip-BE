package org.cotato.gongmozip.domains.profile.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectExperienceRepository extends JpaRepository<ProjectExperience, Long> {
    Page<ProjectExperience> findAllByProfile(Profile profile, Pageable pageable);

    List<ProjectExperience> findAllByProfile(Profile profile);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProjectExperience p where p.projectId = :projectId")
    Optional<ProjectExperience> findByIdWithLock(@Param("projectId") Long projectId);
}
