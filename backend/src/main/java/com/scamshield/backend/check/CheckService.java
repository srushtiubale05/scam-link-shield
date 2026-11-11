package com.scamshield.backend.check;

import com.scamshield.backend.report.ReportRepository;
import com.scamshield.backend.report.ReportStatus;
import com.scamshield.backend.url.UrlNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckService {

    private final ReportRepository reportRepository;
    private final MlClient mlClient;

    public CheckService(ReportRepository reportRepository, MlClient mlClient) {
        this.reportRepository = reportRepository;
        this.mlClient = mlClient;
    }

    public CheckResponse check(String url) {
        UrlNormalizer.Result norm;
        try {
            norm = UrlNormalizer.normalize(url);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        // Layer 1: community-verified scam (source of truth)
        var known = reportRepository.findByNormalizedUrl(norm.normalizedUrl())
                .filter(r -> r.getStatus() == ReportStatus.VERIFIED);
        if (known.isPresent()) {
            var r = known.get();
            return new CheckResponse("KNOWN_SCAM", r.getNormalizedUrl(),
                    r.getCategory(), r.getConfirmationCount(), null);
        }

        // Layer 2: ML signal for unseen URLs (falls back to UNKNOWN if the service is down)
        var ml = mlClient.score(norm.normalizedUrl());
        if (ml.isPresent()) {
            String level = ml.get().level();
            String verdict = switch (level) {
                case "HIGH_RISK" -> "HIGH_RISK";
                case "SUSPICIOUS" -> "SUSPICIOUS";
                default -> "UNKNOWN";
            };
            return new CheckResponse(verdict, norm.normalizedUrl(), null, null, ml.get().score());
        }
        return new CheckResponse("UNKNOWN", norm.normalizedUrl(), null, null, null);
    }
}