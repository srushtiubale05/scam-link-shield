package com.scamshield.backend.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportRequest(
        @NotBlank String url,
        @NotNull ReportCategory category,
        String upiId,
        String phone
) {
    /** Keeps older callers and tests that only pass a URL and a category working. */
    public ReportRequest(String url, ReportCategory category) {
        this(url, category, null, null);
    }
}