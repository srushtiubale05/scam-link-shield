package com.scamshield.backend.indicator;

import com.scamshield.backend.report.Report;
import com.scamshield.backend.report.ReportRepository;
import com.scamshield.backend.report.ReportStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class IndicatorService {

    public record Result(String type, String value, String verdict,
                         long verifiedReports, List<String> categories) {}

    private final ReportRepository reports;

    public IndicatorService(ReportRepository reports) {
        this.reports = reports;
    }

    public Result check(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a UPI ID or phone number");
        }
        try {
            if (raw.contains("@")) {
                String upi = IndicatorNormalizer.upi(raw);
                return build("UPI", upi, reports.findByUpiId(upi));
            }
            String phone = IndicatorNormalizer.phone(raw);
            return build("PHONE", IndicatorNormalizer.maskPhone(phone), reports.findByPhone(phone));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // Only verified reports count. The verdict is REPORTED or NO_REPORTS, never "safe".
    private Result build(String type, String shown, List<Report> found) {
        List<Report> verified = found.stream().filter(r -> r.getStatus() == ReportStatus.VERIFIED).toList();
        List<String> cats = verified.stream().map(r -> r.getCategory().name()).distinct().sorted().toList();
        return new Result(type, shown, verified.isEmpty() ? "NO_REPORTS" : "REPORTED", verified.size(), cats);
    }
}