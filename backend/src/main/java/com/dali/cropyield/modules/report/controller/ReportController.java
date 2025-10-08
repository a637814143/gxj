package com.dali.cropyield.modules.report.controller;

import com.dali.cropyield.common.response.ApiResponse;
import com.dali.cropyield.modules.report.dto.ReportRequest;
import com.dali.cropyield.modules.report.entity.Report;
import com.dali.cropyield.modules.report.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ApiResponse<List<Report>> list() {
        return ApiResponse.success(reportService.findAll());
    }

    @PostMapping
    public ApiResponse<Report> create(@Valid @RequestBody ReportRequest request) {
        return ApiResponse.success(reportService.create(request));
    }
}
