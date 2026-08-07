package org.cotato.gongmozip.domains.profile.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.converter.ProjectEvaluationConverter;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.ProjectEvaluationResponse;
import org.cotato.gongmozip.domains.profile.entity.ProjectEvaluation;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.enums.ProjectCategory;
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
    private final ProjectAiSummaryTxService projectAiSummaryTxService;
    private final AiClient aiClient;

    private ProjectEvaluationService self;

    @Autowired
    public void setSelf(@Lazy ProjectEvaluationService self) {
        this.self = self;
    }

    @Transactional
    public Long evaluateProject(Long projectId, Member member) {
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
                .orElseGet(() -> ProjectEvaluationConverter.toProjectEvaluation(project));
        evaluation.pending();
        ProjectEvaluation saved = projectEvaluationRepository.save(evaluation);

        InterestCategory category = project.getProfile().getInterestCategories().isEmpty()
                ? InterestCategory.IT_AI_TECH
                : project.getProfile().getInterestCategories().get(0);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    self.evaluateProjectAsync(
                            projectId, project.getProjectName(), project.getRole(), project.getDescription(), category);
                } catch (TaskRejectedException e) {
                    log.error("AI project evaluation async trigger rejected due to thread pool saturation", e);
                    projectEvaluationTxService.fail(projectId, "Thread pool saturation: " + e.getMessage());
                }
            }
        });

        return saved.getProjectEvaluationId();
    }

    @Async("aiSummaryExecutor")
    public void evaluateProjectAsync(
            Long projectId, String projectName, String role, String description, InterestCategory category) {
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
            result = aiClient.evaluateProject(projectName, role, description, toKoreanCategory(category));
        } catch (Exception e) {
            apiException = e;
            log.error("AI API call failed for projectId: {}", projectId, e);
        }

        try {
            if (apiException == null && result != null) {
                // Calculate D and combined score
                ProjectExperience project = projectExperienceRepository
                        .findById(projectId)
                        .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

                long periodDays;
                if (project.isOngoing() || project.getEndedAt() == null) {
                    periodDays = java.time.temporal.ChronoUnit.DAYS.between(project.getStartedAt(), LocalDate.now());
                } else {
                    periodDays =
                            java.time.temporal.ChronoUnit.DAYS.between(project.getStartedAt(), project.getEndedAt());
                }
                if (periodDays < 0) {
                    periodDays = 0;
                }

                int dScore = lookupD(project.getCategory(), periodDays);
                int finalScore = Math.min(100, result.score() + dScore);

                projectEvaluationTxService.complete(
                        projectId,
                        finalScore,
                        result.rScore(),
                        result.oScore(),
                        result.fScore(),
                        result.injectionDetected(),
                        result.feedback());
            } else {
                projectEvaluationTxService.fail(
                        projectId, apiException != null ? apiException.getMessage() : "Empty result");
            }
        } catch (Exception e) {
            log.error("Failed to save final AI evaluation result for projectId: {}", projectId, e);
            try {
                projectEvaluationTxService.fail(projectId, "Failed to complete: " + e.getMessage());
            } catch (Exception failure) {
                log.error("Failed to persist failed state on complete error for projectId: {}", projectId, failure);
            }
        }
    }

    private String toKoreanCategory(InterestCategory category) {
        return switch (category) {
            case IT_AI_TECH -> "IT/AI/기술";
            case MARKETING_AD_BRANDING -> "마케팅/광고/브랜딩";
            case IDEA_PLANNING -> "아이디어/기획";
            case ART_DESIGN -> "미술/디자인";
            case PHOTO_VIDEO -> "사진/영상";
            case DATA_ANALYSIS -> "데이터 분석";
        };
    }

    private int lookupD(ProjectCategory category, long periodDays) {
        if (category == null) {
            category = ProjectCategory.CONTEST;
        }
        if (periodDays < 14) { // 2주 미만
            return switch (category) {
                case CONTEST -> 8;
                case EXTERNAL_ACTIVITY -> 6;
                case CAMPUS_PROJECT -> 3;
            };
        } else if (periodDays < 30) { // 2주 ~ 1개월
            return switch (category) {
                case CONTEST -> 12;
                case EXTERNAL_ACTIVITY -> 10;
                case CAMPUS_PROJECT -> 6;
            };
        } else if (periodDays < 60) { // 1개월 ~ 2개월
            return switch (category) {
                case CONTEST -> 16;
                case EXTERNAL_ACTIVITY -> 14;
                case CAMPUS_PROJECT -> 10;
            };
        } else if (periodDays < 90) { // 2개월 ~ 3개월
            return switch (category) {
                case CONTEST -> 20;
                case EXTERNAL_ACTIVITY -> 18;
                case CAMPUS_PROJECT -> 13;
            };
        } else { // 3개월 이상
            return switch (category) {
                case CONTEST -> 25;
                case EXTERNAL_ACTIVITY -> 22;
                case CAMPUS_PROJECT -> 17;
            };
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

        return ProjectEvaluationConverter.toProjectEvaluationResponse(evaluation);
    }
}
