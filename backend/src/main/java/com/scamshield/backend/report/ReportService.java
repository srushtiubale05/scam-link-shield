package com.scamshield.backend.report;

import com.scamshield.backend.indicator.IndicatorNormalizer;
import com.scamshield.backend.url.UrlNormalizer;
import com.scamshield.backend.user.User;
import com.scamshield.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ConfirmationRepository confirmationRepository;
    private final UserRepository userRepository;
    private final int verifyThreshold;

    public ReportService(ReportRepository reportRepository,
                         ConfirmationRepository confirmationRepository,
                         UserRepository userRepository,
                         @Value("${app.report.verify-threshold:3}") int verifyThreshold) {
        this.reportRepository = reportRepository;
        this.confirmationRepository = confirmationRepository;
        this.userRepository = userRepository;
        this.verifyThreshold = verifyThreshold;
    }

    /** New URL creates a report. An already-reported URL counts as a confirmation. */
    @Transactional
    public ReportResponse submit(String email, ReportRequest req) {
        User user = currentUser(email);

        UrlNormalizer.Result norm;
        String upi;
        String phone;
        try {
            norm = UrlNormalizer.normalize(req.url());
            upi = IndicatorNormalizer.upi(req.upiId());
            phone = IndicatorNormalizer.phone(req.phone());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        var existing = reportRepository.findByNormalizedUrl(norm.normalizedUrl());
        if (existing.isPresent()) {
            Report r = existing.get();
            if (r.getUpiId() == null && upi != null) r.setUpiId(upi);          // fill in missing details
            if (r.getPhone() == null && phone != null) r.setPhone(phone);
            return confirmInternal(r, user);
        }

        Report report = Report.builder()
                .originalUrl(req.url().trim())
                .normalizedUrl(norm.normalizedUrl())
                .domain(norm.domain())
                .category(req.category())
                .reporter(user)
                .upiId(upi)
                .phone(phone)
                .build();
        report = reportRepository.save(report);
        confirmationRepository.save(Confirmation.builder().report(report).user(user).build());
        return ReportResponse.from(report);
    }

    @Transactional
    public ReportResponse confirm(String email, Long reportId) {
        User user = currentUser(email);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        return confirmInternal(report, user);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> list(ReportStatus status) {
        List<Report> reports = status == null ? reportRepository.findAll() : reportRepository.findByStatus(status);
        return reports.stream().map(ReportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> mine(String email) {
        User u = currentUser(email);
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(u.getId())
                .stream().map(ReportResponse::from).toList();
    }

    private ReportResponse confirmInternal(Report report, User user) {
        if (confirmationRepository.existsByReportIdAndUserId(report.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already confirmed this report");
        }
        confirmationRepository.save(Confirmation.builder().report(report).user(user).build());
        report.setConfirmationCount(report.getConfirmationCount() + 1);

        if (report.getStatus() == ReportStatus.PENDING
                && report.getConfirmationCount() >= verifyThreshold) {
            report.setStatus(ReportStatus.VERIFIED);
            report.setVerifiedAt(Instant.now());
        }
        return ReportResponse.from(reportRepository.save(report));
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}