package com.scamshield.backend.report;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final AdminReportService service;
    private final ReportService reportService;

    public AdminReportController(AdminReportService service, ReportService reportService) {
        this.service = service;
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportResponse> list(@RequestParam(required = false) ReportStatus status) {
        return reportService.list(status);
    }

    @PostMapping("/{id}/approve")
    public ReportResponse approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    public ReportResponse reject(@PathVariable Long id) {
        return service.reject(id);
    }
}