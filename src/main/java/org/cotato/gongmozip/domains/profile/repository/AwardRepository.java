package org.cotato.gongmozip.domains.profile.repository;

import java.util.List;
import org.cotato.gongmozip.domains.profile.entity.Award;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardRepository extends JpaRepository<Award, Long> {
    Page<Award> findAllByProfile(Profile profile, Pageable pageable);

    List<Award> findAllByProfile(Profile profile);

    int countByProfile(Profile profile);
}
