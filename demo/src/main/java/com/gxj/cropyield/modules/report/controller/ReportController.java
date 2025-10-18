package com.gxj.cropyield.modules.report.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.report.dto.ReportOverviewResponse;
import com.gxj.cropyield.modules.report.dto.ReportRequest;
import com.gxj.cropyield.modules.report.entity.Report;
import com.gxj.cropyield.modules.report.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ApiResponse<ReportOverviewResponse> listReports() {
        return ApiResponse.success(reportService.getOverview());
    }

    @PostMapping
    public ApiResponse<Report> createReport(@Valid @RequestBody ReportRequest request) {
        return ApiResponse.success(reportService.create(request));
    }
}
