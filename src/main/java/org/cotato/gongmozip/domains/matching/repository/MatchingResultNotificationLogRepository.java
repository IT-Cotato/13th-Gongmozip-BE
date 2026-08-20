package org.cotato.gongmozip.domains.matching.repository;

import java.time.LocalDate;
import org.cotato.gongmozip.domains.matching.entity.MatchingResultNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingResultNotificationLogRepository extends JpaRepository<MatchingResultNotificationLog, Long> {

    boolean existsByApplicationDate(LocalDate applicationDate);
}
