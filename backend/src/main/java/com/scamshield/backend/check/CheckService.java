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

    public CheckService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public CheckResponse check(String url) {
        UrlNormalizer.Result norm;
        try {
            norm = UrlNormalizer.normalize(url);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        return reportRepository.findByNormalizedUrl(norm.normalizedUrl())
                .filter(r -> r.getStatus() == ReportStatus.VERIFIED)
                .map(r -> new CheckResponse("KNOWN_SCAM", r.getNormalizedUrl(),
                        r.getCategory(), r.getConfirmationCount()))
                .orElseGet(() -> new CheckResponse("UNKNOWN", norm.normalizedUrl(), null, null));
    }
}