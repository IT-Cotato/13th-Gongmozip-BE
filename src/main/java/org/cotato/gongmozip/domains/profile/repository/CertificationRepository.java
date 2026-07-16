package org.cotato.gongmozip.domains.profile.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.profile.entity.Certification;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    Optional<Certification> findByCertificationCode(String certificationCode);

    @Query("SELECT c FROM Certification c WHERE "
            + "(:keyword IS NULL OR LOWER(c.certificateName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
            + "(:categoryCode IS NULL OR c.categoryCode = :categoryCode)")
    Page<Certification> searchCertifications(
            @Param("keyword") String keyword,
            @Param("categoryCode") CertificationCategory categoryCode,
            Pageable pageable);
}
