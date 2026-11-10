package com.scamshield.backend.report;

import java.time.Instant;

public record ReportResponse(Long id, String normalizedUrl, String domain,
                             ReportCategory category, ReportStatus status,
                             int confirmationCount, Instant createdAt) {

    public static ReportResponse from(Report r) {
        return new ReportResponse(r.getId(), r.getNormalizedUrl(), r.getDomain(),
                r.getCategory(), r.getStatus(), r.getConfirmationCount(), r.getCreatedAt());
    }
}