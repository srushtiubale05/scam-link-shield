package com.scamshield.backend.report;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmationRepository extends JpaRepository<Confirmation, Long> {
    boolean existsByReportIdAndUserId(Long reportId, Long userId);
}