package com.scamshield.backend.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportRequest(
        @NotBlank String url,
        @NotNull ReportCategory category
) {}