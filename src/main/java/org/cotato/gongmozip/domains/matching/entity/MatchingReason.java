package org.cotato.gongmozip.domains.matching.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.matching.converter.TitleDescriptionListConverter;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.TitleDescriptionInfo;
import org.cotato.gongmozip.domains.profile.entity.StringListConverter;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "matching_reasons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchingReason extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_reason_id", nullable = false, updatable = false)
    private Long matchingReasonId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_group_id", nullable = false, unique = true)
    private MatchingGroup matchingGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AiSummaryStatus status;

    @Column(name = "headline")
    private String headline;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Convert(converter = TitleDescriptionListConverter.class)
    @Column(name = "strengths", columnDefinition = "TEXT")
    private List<TitleDescriptionInfo> strengths;

    @Convert(converter = StringListConverter.class)
    @Column(name = "common_points", columnDefinition = "TEXT")
    private List<String> commonPoints;

    @Convert(converter = TitleDescriptionListConverter.class)
    @Column(name = "complementary_points", columnDefinition = "TEXT")
    private List<TitleDescriptionInfo> complementaryPoints;

    @Convert(converter = StringListConverter.class)
    @Column(name = "cautions", columnDefinition = "TEXT")
    private List<String> cautions;

    @Builder.Default
    @Column(name = "total_compatibility_score", nullable = false)
    private Integer totalCompatibilityScore = 0;

    @Builder.Default
    @Column(name = "team_goal_score", nullable = false)
    private Integer teamGoalScore = 0;

    @Builder.Default
    @Column(name = "personality_score", nullable = false)
    private Integer personalityScore = 0;

    @Builder.Default
    @Column(name = "extraversion_complement_score", nullable = false)
    private Integer extraversionComplementScore = 0;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    public void startProcessing() {
        this.status = AiSummaryStatus.PROCESSING;
    }

    public void complete(
            String headline,
            String summary,
            List<TitleDescriptionInfo> strengths,
            List<String> commonPoints,
            List<TitleDescriptionInfo> complementaryPoints,
            List<String> cautions,
            Integer totalCompatibilityScore,
            Integer teamGoalScore,
            Integer personalityScore,
            Integer extraversionComplementScore) {
        this.status = AiSummaryStatus.COMPLETED;
        this.headline = headline;
        this.summary = summary;
        this.strengths = strengths;
        this.commonPoints = commonPoints;
        this.complementaryPoints = complementaryPoints;
        this.cautions = cautions;
        this.totalCompatibilityScore = totalCompatibilityScore;
        this.teamGoalScore = teamGoalScore;
        this.personalityScore = personalityScore;
        this.extraversionComplementScore = extraversionComplementScore;
        this.evaluatedAt = LocalDateTime.now();
        this.failureMessage = null;
    }

    public void fail(String failureMessage) {
        this.status = AiSummaryStatus.FAILED;
        this.failureMessage = failureMessage;
        this.evaluatedAt = LocalDateTime.now();
    }

    public void reset() {
        this.status = AiSummaryStatus.PENDING;
        this.failureMessage = null;
        this.evaluatedAt = null;
    }
}
