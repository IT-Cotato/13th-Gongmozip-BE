package org.cotato.gongmozip.domains.review.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.profile.entity.StringListConverter;
import org.cotato.gongmozip.domains.review.enums.ReviewAgreementLevel;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_reviews_team_reviewer_reviewee",
                    columnNames = {"team_id", "reviewer_team_member_id", "reviewee_team_member_id"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id", nullable = false, updatable = false)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_team_member_id", nullable = false)
    private TeamMember reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_team_member_id", nullable = false)
    private TeamMember reviewee;

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_score", nullable = false, length = 20)
    private ReviewAgreementLevel communicationScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "participation_score", nullable = false, length = 20)
    private ReviewAgreementLevel participationScore;

    // ReviewKeyword.name() 값의 목록. 고정된 키워드 7개 중 중복 선택이므로 별도 테이블 없이
    // MatchingReason.commonPoints와 동일한 방식(StringListConverter)으로 저장한다.
    @Convert(converter = StringListConverter.class)
    @Column(name = "keywords", nullable = false, columnDefinition = "TEXT")
    private List<String> keywords;
}
