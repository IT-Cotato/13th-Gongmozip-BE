package org.cotato.gongmozip.domains.profile.repository;

import java.util.List;
import org.cotato.gongmozip.domains.profile.entity.Certification;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.entity.ProfileCertification;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileCertificationRepository extends JpaRepository<ProfileCertification, Long> {

    @Query("SELECT pc FROM ProfileCertification pc WHERE pc.profile = :profile AND "
            + "(:categoryCode IS NULL OR pc.categoryCode = :categoryCode)")
    Page<ProfileCertification> findAllByProfileAndCategory(
            @Param("profile") Profile profile,
            @Param("categoryCode") CertificationCategory categoryCode,
            Pageable pageable);

    List<ProfileCertification> findAllByProfile(Profile profile);

    int countByProfile(Profile profile);

    boolean existsByProfileAndCertification(Profile profile, Certification certification);

    boolean existsByProfileAndIsCustomTrueAndCertificateNameIgnoreCase(Profile profile, String certificateName);
}
