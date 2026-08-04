package org.cotato.gongmozip.domains.profile.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.profile.entity.ProjectEvaluation;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectEvaluationRepository extends JpaRepository<ProjectEvaluation, Long> {
    Optional<ProjectEvaluation> findByProjectExperience(ProjectExperience projectExperience);

    Optional<ProjectEvaluation> findByProjectExperience_ProjectId(Long projectId);

    // 여러 프로젝트의 저장 평가를 한 번에 가져와 매칭 역량 점수 계산 중 N+1 조회를 막는다.
    List<ProjectEvaluation> findAllByProjectExperienceIn(Collection<ProjectExperience> projects);
}
