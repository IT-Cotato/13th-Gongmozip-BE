package org.cotato.gongmozip.domains.contest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.cotato.gongmozip.domains.contest.enums.ContestStatus;
import org.cotato.gongmozip.domains.profile.entity.StringListConverter;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(
        name = "contest",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_contest_title_apply_end_at",
                    columnNames = {"title", "apply_end_at"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Contest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contest_id", nullable = false, updatable = false)
    private Long contestId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private InterestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ContestStatus status;

    @Column(name = "host_name", nullable = false, length = 150)
    private String hostName;

    @Column(name = "apply_start_at")
    private LocalDateTime applyStartAt;

    @Column(name = "apply_end_at", nullable = false)
    private LocalDateTime applyEndAt;

    @Column(name = "announcement_at")
    private LocalDateTime announcementAt;

    @Column(name = "eligibility_text", columnDefinition = "TEXT")
    private String eligibilityText;

    @Column(name = "prize_text", columnDefinition = "TEXT")
    private String prizeText;

    @Column(name = "location_text")
    private String locationText;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Convert(converter = StringListConverter.class)
    @Column(name = "detail_image_urls", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> detailImageUrls = new ArrayList<>();

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "is_team_participation", nullable = false)
    private boolean isTeamParticipation;

    @Column(name = "min_team_size")
    private Integer minTeamSize;

    @Column(name = "max_team_size")
    private Integer maxTeamSize;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    public void update(
            String title,
            String summary,
            String description,
            InterestCategory category,
            ContestStatus status,
            String hostName,
            LocalDateTime applyStartAt,
            LocalDateTime applyEndAt,
            String eligibilityText,
            String prizeText,
            String locationText,
            String thumbnailUrl,
            String sourceUrl,
            Boolean isTeamParticipation,
            Integer minTeamSize,
            Integer maxTeamSize) {
        if (title != null) this.title = title;
        if (summary != null) this.summary = summary;
        if (description != null) this.description = description;
        if (category != null) this.category = category;
        if (status != null) this.status = status;
        if (hostName != null) this.hostName = hostName;
        if (applyStartAt != null) this.applyStartAt = applyStartAt;
        if (applyEndAt != null) this.applyEndAt = applyEndAt;
        if (eligibilityText != null) this.eligibilityText = eligibilityText;
        if (prizeText != null) this.prizeText = prizeText;
        if (locationText != null) this.locationText = locationText;
        if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
        if (sourceUrl != null) this.sourceUrl = sourceUrl;
        if (isTeamParticipation != null) this.isTeamParticipation = isTeamParticipation;
        if (minTeamSize != null) this.minTeamSize = minTeamSize;
        if (maxTeamSize != null) this.maxTeamSize = maxTeamSize;
    }
}
