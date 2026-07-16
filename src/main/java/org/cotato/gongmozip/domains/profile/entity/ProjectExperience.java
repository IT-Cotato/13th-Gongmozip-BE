package org.cotato.gongmozip.domains.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
}
