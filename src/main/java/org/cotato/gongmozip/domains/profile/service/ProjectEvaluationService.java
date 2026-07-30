package org.cotato.gongmozip.domains.profile.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.ProjectEvaluationResponse;
import org.cotato.gongmozip.domains.profile.entity.ProjectEvaluation;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.domains.profile.exception.ProfileException;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileErrorCode;
import org.cotato.gongmozip.domains.profile.repository.ProjectEvaluationRepository;
import org.cotato.gongmozip.domains.profile.repository.ProjectExperienceRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.cotato.gongmozip.global.ai.dto.ProjectEvaluationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectEvaluationService {

    private final ProjectEvaluationRepository projectEvaluationRepository;
    private final ProjectExperienceRepository projectExperienceRepository;
    private final ProjectEvaluationTxService projectEvaluationTxService;
    private final AiClient aiClient;

    private ProjectEvaluationService self;

    @Autowired
    public void setSelf(@Lazy ProjectEvaluationService self) {
        this.self = self;
    }

    @Transactional
    public void evaluateProject(Long projectId, Member member) {
        ProjectExperience project = projectExperienceRepository
                .findByIdWithLock(projectId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROJECT_NOT_FOUND));

        if (!project.getProfile().getMember().getMemberId().equals(member.getMemberId())) {
            throw new ProfileException(ProfileErrorCode.PROFILE_ACCESS_DENIED);
        }

        projectEvaluationRepository.findByProjectExperience(project).ifPresent(evaluation -> {
            if (evaluation.getStatus() == AiSummaryStatus.PENDING
                    || evaluation.getStatus() == AiSummaryStatus.PROCESSING) {
                throw new ProfileException(ProfileErrorCode.PROJECT_EVALUATION_GENERATION_IN_PROGRESS);
            }
        });

        ProjectEvaluation evaluation = projectEvaluationRepository
                .findByProjectExperience(project)
                .orElseGet(() -> ProjectEvaluation.builder()
                        .projectExperience(project)
                        .status(AiSummaryStatus.NOT_CREATED)
                        .build());
        evaluation.pending();
        projectEvaluationRepository.save(evaluation);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    self.evaluateProjectAsync(
                            projectId, project.getProjectName(), project.getRole(), project.getDescription());
                } catch (TaskRejectedException e) {
                    log.error("AI project evaluation async trigger rejected due to thread pool saturation", e);
                    projectEvaluationTxService.fail(projectId, "Thread pool saturation: " + e.getMessage());
                }
            }
        });
    }

    @Async("aiSummaryExecutor")
    public void evaluateProjectAsync(Long projectId, String projectName, String role, String description) {
        log.info("Starting async AI evaluation for projectId: {}", projectId);
        try {
            projectEvaluationTxService.startProcessing(projectId);
        } catch (Exception e) {
            log.error("Failed to start processing for projectId: {}", projectId, e);
            try {
                projectEvaluationTxService.fail(projectId, "Failed to start processing: " + e.getMessage());
            } catch (Exception failure) {
                log.error("Failed to persist failed state for projectId: {}", projectId, failure);
            }
            return;
        }

        ProjectEvaluationResult result = null;
        Exception apiException = null;
        try {
            result = aiClient.evaluateProject(projectName, role, description);
        } catch (Exception e) {
            apiException = e;
            log.error("AI API call failed for projectId: {}", projectId, e);
        }

        try {
            if (apiException == null && result != null) {
                projectEvaluationTxService.complete(projectId, result.score(), result.feedback());
            } else {
                projectEvaluationTxService.fail(
                        projectId, apiException != null ? apiException.getMessage() : "Empty result");
            }
        } catch (Exception e) {
            log.error("Failed to save final AI evaluation result for projectId: {}", projectId, e);
        }
    }

    public ProjectEvaluationResponse getEvaluation(Long evaluationId, Member member) {
        ProjectEvaluation evaluation = projectEvaluationRepository
                .findById(evaluationId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROJECT_EVALUATION_NOT_FOUND));

        ProjectExperience project = evaluation.getProjectExperience();
        if (!project.getProfile().getMember().getMemberId().equals(member.getMemberId())) {
            throw new ProfileException(ProfileErrorCode.PROFILE_ACCESS_DENIED);
        }

        Integer score = evaluation.getScore();
        String feedback = evaluation.getFeedback();

        return new ProjectEvaluationResponse(
                evaluation.getProjectEvaluationId(), evaluation.getStatus().name(), score, feedback);
    }
}
