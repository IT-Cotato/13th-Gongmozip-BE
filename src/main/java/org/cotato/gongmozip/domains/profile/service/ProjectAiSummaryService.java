package org.cotato.gongmozip.domains.profile.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.global.ai.AiClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAiSummaryService {

    private final AiClient aiClient;
    private final ProjectAiSummaryTxService projectAiSummaryTxService;

    @Async("aiSummaryExecutor")
    public void generateSummaryAsync(Long projectId, String projectName, String role, String description) {
        log.info("Starting async AI summary generation for projectId: {}", projectId);

        try {
            projectAiSummaryTxService.startProcessing(projectId);
        } catch (Exception e) {
            log.error("Failed to start AI summary processing for projectId: {}", projectId, e);
            return;
        }

        String summary = null;
        Exception apiException = null;
        try {
            summary = aiClient.generateSummary(projectName, role, description);
            log.info("Successfully received AI summary for projectId: {}", projectId);
        } catch (Exception e) {
            apiException = e;
            log.error("Failed to generate AI summary from API for projectId: {}. Error: ", projectId, e);
        }

        try {
            if (apiException == null && summary != null) {
                projectAiSummaryTxService.completeSummary(projectId, summary);
            } else {
                projectAiSummaryTxService.failSummary(projectId);
            }
        } catch (Exception e) {
            log.error("Failed to save AI summary state for projectId: {}", projectId, e);
        }
    }
}
