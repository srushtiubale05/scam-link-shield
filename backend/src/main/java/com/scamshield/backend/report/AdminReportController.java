package com.scamshield.backend.report;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final AdminReportService service;

    public AdminReportController(AdminReportService service) {
        this.service = service;
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