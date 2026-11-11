package com.scamshield.backend.report;

import com.scamshield.backend.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AdminReportService {

    private final ReportRepository reportRepository;

    public AdminReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional
    public ReportResponse approve(Long id) {
        Report r = find(id);
        r.setStatus(ReportStatus.VERIFIED);
        r.setVerifiedAt(Instant.now());
        adjustReputation(r.getReporter(), +5);
        return ReportResponse.from(reportRepository.save(r));
    }

    @Transactional
    public ReportResponse reject(Long id) {
        Report r = find(id);
        r.setStatus(ReportStatus.REJECTED);
        r.setVerifiedAt(null);
        adjustReputation(r.getReporter(), -10);
        return ReportResponse.from(reportRepository.save(r));
    }

    private Report find(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    private void adjustReputation(User u, int delta) {
        u.setReputationScore(Math.max(0, Math.min(100, u.getReputationScore() + delta)));
    }
}