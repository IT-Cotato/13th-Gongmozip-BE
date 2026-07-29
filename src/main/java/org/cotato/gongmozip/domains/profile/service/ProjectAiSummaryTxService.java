package org.cotato.gongmozip.domains.profile.service;

import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.cotato.gongmozip.domains.profile.repository.ProjectExperienceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectAiSummaryTxService {

    private final ProjectExperienceRepository projectExperienceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startProcessing(Long projectId) {
        ProjectExperience project = projectExperienceRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        project.startAiSummaryProcessing();
        projectExperienceRepository.saveAndFlush(project);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeSummary(Long projectId, String summary) {
        ProjectExperience project = projectExperienceRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        project.completeAiSummary(summary);
        projectExperienceRepository.saveAndFlush(project);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failSummary(Long projectId) {
        ProjectExperience project = projectExperienceRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        project.failAiSummary();
        projectExperienceRepository.saveAndFlush(project);
    }
}
