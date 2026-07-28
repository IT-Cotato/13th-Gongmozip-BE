package org.cotato.gongmozip.domains.profile.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.cotato.gongmozip.domains.profile.repository.ProjectExperienceRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAiSummaryService {

    private final ProjectExperienceRepository projectExperienceRepository;
    private final AiClient aiClient;

    @Async("aiSummaryExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateSummaryAsync(Long projectId, String projectName, String role, String description) {
        log.info("Starting async AI summary generation for projectId: {}", projectId);
        ProjectExperience project = null;
        try {
            project = projectExperienceRepository
                    .findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

            project.startAiSummaryProcessing();
            projectExperienceRepository.saveAndFlush(project);

            String summary = aiClient.generateSummary(projectName, role, description);
            project.completeAiSummary(summary);
            log.info("Successfully completed AI summary for projectId: {}", projectId);
        } catch (Exception e) {
            log.error("Failed to generate AI summary for projectId: {}. Error: ", projectId, e);
            if (project != null) {
                project.failAiSummary();
            }
        } finally {
            if (project != null) {
                projectExperienceRepository.saveAndFlush(project);
            }
        }
    }
}
