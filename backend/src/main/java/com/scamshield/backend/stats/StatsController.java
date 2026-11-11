package com.scamshield.backend.stats;

import com.scamshield.backend.report.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    public record Stats(long verified, long pending, long rejected,
                        Map<String, Long> byCategory, List<ReportResponse> recent) {}

    private final ReportRepository repo;

    public StatsController(ReportRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public Stats stats() {
        List<Report> verified = repo.findByStatus(ReportStatus.VERIFIED);
        Map<String, Long> byCategory = verified.stream()
                .collect(Collectors.groupingBy(r -> r.getCategory().name(), TreeMap::new, Collectors.counting()));
        List<ReportResponse> recent = repo.findTop10ByStatusOrderByVerifiedAtDesc(ReportStatus.VERIFIED)
                .stream().map(ReportResponse::from).toList();
        return new Stats(verified.size(),
                repo.findByStatus(ReportStatus.PENDING).size(),
                repo.findByStatus(ReportStatus.REJECTED).size(),
                byCategory, recent);
    }
}