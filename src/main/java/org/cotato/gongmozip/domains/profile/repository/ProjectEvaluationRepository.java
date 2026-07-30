package org.cotato.gongmozip.domains.profile.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.profile.entity.ProjectEvaluation;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectEvaluationRepository extends JpaRepository<ProjectEvaluation, Long> {
    Optional<ProjectEvaluation> findByProjectExperience(ProjectExperience projectExperience);

    Optional<ProjectEvaluation> findByProjectExperience_ProjectId(Long projectId);
}
