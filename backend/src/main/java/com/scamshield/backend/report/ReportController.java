package com.scamshield.backend.report;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse submit(Authentication auth, @Valid @RequestBody ReportRequest req) {
        return reportService.submit(auth.getName(), req);
    }

    @PostMapping("/{id}/confirm")
    public ReportResponse confirm(Authentication auth, @PathVariable Long id) {
        return reportService.confirm(auth.getName(), id);
    }

    @GetMapping
    public List<ReportResponse> list(@RequestParam(required = false) ReportStatus status) {
        return reportService.list(status);
    }
}