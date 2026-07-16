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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "profile_certifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProfileCertification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_certification_id", nullable = false, updatable = false)
    private Long profileCertificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_id")
    private Certification certification;

    @Column(name = "certificate_name", nullable = false, length = 150)
    private String certificateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_code", nullable = false, length = 50)
    private CertificationCategory categoryCode;

    @Column(name = "issuer", length = 100)
    private String issuer;

    @Column(name = "acquired_at")
    private LocalDate acquiredAt;

    @Column(name = "is_custom", nullable = false)
    private boolean isCustom;

    public void updateCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }

    public void updateCategoryCode(CertificationCategory categoryCode) {
        this.categoryCode = categoryCode;
    }

    public void updateIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void updateAcquiredAt(LocalDate acquiredAt) {
        this.acquiredAt = acquiredAt;
    }
}
