package com.scamshield.backend.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByNormalizedUrl(String normalizedUrl);

    List<Report> findByStatus(ReportStatus status);

    List<Report> findTop10ByStatusOrderByVerifiedAtDesc(ReportStatus status);

    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<Report> findByNormalizedUrlIn(Collection<String> urls);

    List<Report> findByUpiId(String upiId);

    List<Report> findByPhone(String phone);

    /** Removes feed entries that no feed has listed since the cutoff. Community reports are never deleted. */
    @Transactional
    @Modifying
    @Query("delete from Report r where r.source <> 'COMMUNITY' and r.lastSeen < :cutoff")
    int deleteExpiredFeedReports(@Param("cutoff") Instant cutoff);
}