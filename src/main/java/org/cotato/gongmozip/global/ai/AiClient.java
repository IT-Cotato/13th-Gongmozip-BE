package org.cotato.gongmozip.global.ai;

import org.cotato.gongmozip.global.ai.dto.ProjectEvaluationResult;

public interface AiClient {
    String generateSummary(String projectName, String role, String description);

    ProjectEvaluationResult evaluateProject(String projectName, String role, String description);
}
