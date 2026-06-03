package com.metertracking.controller;

import com.metertracking.dto.ReportDTO;
import com.metertracking.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Главный дашборд — все данные одним запросом.
     * GET /api/admin/reports/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ReportDTO.DashboardResponse> getDashboard() {
        return ResponseEntity.ok(reportService.getDashboard());
    }
}
