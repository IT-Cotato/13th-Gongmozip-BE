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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.matching.converter.LeaderCandidateListConverter;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderCandidateResponse;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "leader_recommendations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LeaderRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leader_recommendation_id", nullable = false, updatable = false)
    private Long leaderRecommendationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, unique = true)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AiSummaryStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_member_id")
    private Member recommendedMember;

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;

    @Convert(converter = LeaderCandidateListConverter.class)
    @Column(name = "candidates", columnDefinition = "TEXT")
    private List<LeaderCandidateResponse> candidates;

    @Column(name = "team_summary", columnDefinition = "TEXT")
    private String teamSummary;

    @Column(name = "caution", columnDefinition = "TEXT")
    private String caution;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    public void startProcessing() {
        this.status = AiSummaryStatus.PROCESSING;
    }

    public void complete(
            Member recommendedMember,
            String recommendationReason,
            List<LeaderCandidateResponse> candidates,
            String teamSummary,
            String caution) {
        this.status = AiSummaryStatus.COMPLETED;
        this.recommendedMember = recommendedMember;
        this.recommendationReason = recommendationReason;
        this.candidates = candidates;
        this.teamSummary = teamSummary;
        this.caution = caution;
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
        this.recommendedMember = null;
        this.recommendationReason = null;
        this.candidates = null;
        this.teamSummary = null;
        this.caution = null;
        this.failureMessage = null;
        this.evaluatedAt = null;
    }
}
