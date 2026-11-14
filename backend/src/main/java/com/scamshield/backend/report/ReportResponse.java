package com.scamshield.backend.report;

import java.time.Instant;

public record ReportResponse(Long id, String normalizedUrl, String domain,
                             ReportCategory category, ReportStatus status,
                             int confirmationCount, Instant createdAt, String source) {

    public static ReportResponse from(Report r) {
        String source = r.getSource() == null ? "COMMUNITY" : r.getSource();   // old rows have no source
        return new ReportResponse(r.getId(), r.getNormalizedUrl(), r.getDomain(),
                r.getCategory(), r.getStatus(), r.getConfirmationCount(), r.getCreatedAt(), source);
    }
}