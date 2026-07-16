package org.cotato.gongmozip.domains.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "certifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Certification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certification_id", nullable = false, updatable = false)
    private Long certificationId;

    @Column(name = "certification_code", nullable = false, unique = true, length = 100)
    private String certificationCode;

    @Column(name = "certificate_name", nullable = false, length = 150)
    private String certificateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_code", nullable = false, length = 50)
    private CertificationCategory categoryCode;
}
