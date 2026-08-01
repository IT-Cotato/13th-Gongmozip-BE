package org.cotato.gongmozip.domains.profile.converter;

import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.ProjectEvaluationCreateResponse;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.ProjectEvaluationResponse;
import org.cotato.gongmozip.domains.profile.entity.ProjectEvaluation;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;

public class ProjectEvaluationConverter {

    public static ProjectEvaluation toProjectEvaluation(ProjectExperience project) {
        return ProjectEvaluation.builder()
                .projectExperience(project)
                .status(AiSummaryStatus.NOT_CREATED)
                .build();
    }

    public static ProjectEvaluationResponse toProjectEvaluationResponse(ProjectEvaluation evaluation) {
        return new ProjectEvaluationResponse(
                evaluation.getProjectEvaluationId(),
                evaluation.getStatus().name(),
                evaluation.getScore(),
                evaluation.getFeedback());
    }

    public static ProjectEvaluationCreateResponse toProjectEvaluationCreateResponse(ProjectEvaluation evaluation) {
        return new ProjectEvaluationCreateResponse(evaluation.getProjectEvaluationId());
    }
}
