package org.cotato.gongmozip.domains.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.domains.profile.enums.ProjectCategory;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "project_experiences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectExperience extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    @Builder.Default
    private ProjectCategory category = ProjectCategory.CONTEST;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "role", nullable = false, length = 200)
    private String role;

    @Convert(converter = StringListConverter.class)
    @Column(name = "tech_stacks", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private List<String> techStacks = new ArrayList<>();

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "ended_at")
    private LocalDate endedAt;

    @Column(name = "is_ongoing", nullable = false)
    private boolean isOngoing;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_summary_status", nullable = false)
    @Builder.Default
    private AiSummaryStatus aiSummaryStatus = AiSummaryStatus.NOT_CREATED;

    @Column(name = "ai_summary_generated_at")
    private LocalDateTime aiSummaryGeneratedAt;

    public void startAiSummaryProcessing() {
        this.aiSummaryStatus = AiSummaryStatus.PROCESSING;
    }

    public void completeAiSummary(String summary) {
        this.aiSummary = summary;
        this.aiSummaryStatus = AiSummaryStatus.COMPLETED;
        this.aiSummaryGeneratedAt = LocalDateTime.now();
    }

    public void failAiSummary() {
        this.aiSummaryStatus = AiSummaryStatus.FAILED;
    }

    public void outdateAiSummary() {
        this.aiSummaryStatus = AiSummaryStatus.OUTDATED;
    }

    public void pendingAiSummary() {
        this.aiSummaryStatus = AiSummaryStatus.PENDING;
    }

    public void updateProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateRole(String role) {
        this.role = role;
    }

    public void updateTechStacks(List<String> techStacks) {
        this.techStacks = techStacks;
    }

    public void updateStartedAt(LocalDate startedAt) {
        this.startedAt = startedAt;
    }

    public void updateEndedAt(LocalDate endedAt) {
        this.endedAt = endedAt;
    }

    public void updateIsOngoing(boolean isOngoing) {
        this.isOngoing = isOngoing;
    }

    public void updateAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public void updateCategory(ProjectCategory category) {
        this.category = category;
    }
}
