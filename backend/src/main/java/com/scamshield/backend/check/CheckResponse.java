package com.scamshield.backend.check;

import com.scamshield.backend.report.ReportCategory;

public record CheckResponse(String verdict, String normalizedUrl,
                            ReportCategory category, Integer confirmations) {}