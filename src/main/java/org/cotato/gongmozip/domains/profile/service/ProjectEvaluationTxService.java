package org.cotato.gongmozip.domains.profile.service;

import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.profile.converter.ProjectEvaluationConverter;
import org.cotato.gongmozip.domains.profile.entity.ProjectEvaluation;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.cotato.gongmozip.domains.profile.repository.ProjectEvaluationRepository;
import org.cotato.gongmozip.domains.profile.repository.ProjectExperienceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectEvaluationTxService {

    private final ProjectEvaluationRepository projectEvaluationRepository;
    private final ProjectExperienceRepository projectExperienceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long startProcessing(Long projectId) {
        ProjectExperience project = projectExperienceRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        ProjectEvaluation evaluation = projectEvaluationRepository
                .findByProjectExperience(project)
                .orElseGet(() -> ProjectEvaluationConverter.toProjectEvaluation(project));

        evaluation.startProcessing();
        ProjectEvaluation saved = projectEvaluationRepository.saveAndFlush(evaluation);
        return saved.getProjectEvaluationId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long projectId, int score, String feedback) {
        ProjectExperience project = projectExperienceRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        ProjectEvaluation evaluation = projectEvaluationRepository
                .findByProjectExperience(project)
                .orElseThrow(() -> new IllegalStateException("Evaluation not found for project: " + projectId));

        evaluation.complete(score, feedback);
        projectEvaluationRepository.saveAndFlush(evaluation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long projectId, String errorMessage) {
        ProjectExperience project = projectExperienceRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        ProjectEvaluation evaluation = projectEvaluationRepository
                .findByProjectExperience(project)
                .orElseGet(() -> ProjectEvaluationConverter.toProjectEvaluation(project));

        evaluation.fail(errorMessage);
        projectEvaluationRepository.saveAndFlush(evaluation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pending(Long projectId) {
        ProjectExperience project = projectExperienceRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        ProjectEvaluation evaluation = projectEvaluationRepository
                .findByProjectExperience(project)
                .orElseGet(() -> ProjectEvaluationConverter.toProjectEvaluation(project));

        evaluation.pending();
        projectEvaluationRepository.saveAndFlush(evaluation);
    }
}
