package org.cotato.gongmozip.domains.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "project_evaluations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectEvaluation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_evaluation_id", nullable = false, updatable = false)
    private Long projectEvaluationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private ProjectExperience projectExperience;

    @Column(name = "score")
    private Integer score;

    @Column(name = "r_score")
    private Integer rScore;

    @Column(name = "o_score")
    private Integer oScore;

    @Column(name = "f_score")
    private Integer fScore;

    @Column(name = "injection_detected", nullable = false)
    @Builder.Default
    private boolean injectionDetected = false;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AiSummaryStatus status = AiSummaryStatus.NOT_CREATED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    public void startProcessing() {
        this.status = AiSummaryStatus.PROCESSING;
    }

    public void complete(
            Integer score, Integer rScore, Integer oScore, Integer fScore, boolean injectionDetected, String feedback) {
        this.score = score;
        this.rScore = rScore;
        this.oScore = oScore;
        this.fScore = fScore;
        this.injectionDetected = injectionDetected;
        this.feedback = feedback;
        this.status = AiSummaryStatus.COMPLETED;
        this.evaluatedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void fail(String errorMessage) {
        this.status = AiSummaryStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void pending() {
        this.score = null;
        this.rScore = null;
        this.oScore = null;
        this.fScore = null;
        this.injectionDetected = false;
        this.feedback = null;
        this.errorMessage = null;
        this.evaluatedAt = null;
        this.status = AiSummaryStatus.PENDING;
    }
}
