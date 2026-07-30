package org.cotato.gongmozip.domains.report.repository;

import org.cotato.gongmozip.domains.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {}
