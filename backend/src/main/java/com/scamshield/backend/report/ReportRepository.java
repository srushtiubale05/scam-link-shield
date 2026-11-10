package com.scamshield.backend.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByNormalizedUrl(String normalizedUrl);
    List<Report> findByStatus(ReportStatus status);
}