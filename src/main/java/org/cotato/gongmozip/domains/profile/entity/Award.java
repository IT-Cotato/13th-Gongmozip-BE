package org.cotato.gongmozip.domains.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "awards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Award extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "award_id", nullable = false, updatable = false)
    private Long awardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "award_name", nullable = false, length = 200)
    private String awardName;

    @Column(name = "organization_name", length = 200)
    private String organizationName;

    @Column(name = "award_rank", length = 100)
    private String awardRank;

    @Column(name = "awarded_at")
    private LocalDate awardedAt;

    public void updateAwardName(String awardName) {
        this.awardName = awardName;
    }

    public void updateOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public void updateAwardRank(String awardRank) {
        this.awardRank = awardRank;
    }

    public void updateAwardedAt(LocalDate awardedAt) {
        this.awardedAt = awardedAt;
    }
}
