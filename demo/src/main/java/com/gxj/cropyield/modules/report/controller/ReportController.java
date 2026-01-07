package com.gxj.cropyield.modules.report.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.modules.report.dto.ReportDetailResponse;
import com.gxj.cropyield.modules.report.dto.ReportGenerationRequest;
import com.gxj.cropyield.modules.report.dto.ReportOverviewResponse;
import com.gxj.cropyield.modules.report.dto.ReportRequest;
import com.gxj.cropyield.modules.report.entity.Report;
import com.gxj.cropyield.modules.report.service.ReportExportResult;
import com.gxj.cropyield.modules.report.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * 报表分析模块的控制器，用于暴露报表分析相关的 REST 接口。
 * <p>核心方法：listReports、createReport、generate、detail。</p>
 */

/**
 * 报表管理控制器 - 生成报表、导出Excel、报表删除
 */
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

    @PostMapping("/generate")
    public ApiResponse<ReportDetailResponse> generate(@Valid @RequestBody ReportGenerationRequest request) {
        return ApiResponse.success(reportService.generate(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(reportService.getDetail(id));
    }

    @GetMapping("/export/{id}")
    public ResponseEntity<ByteArrayResource> export(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "pdf") String format) {
        ReportExportResult result = reportService.export(id, format);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(result.filename(), java.nio.charset.StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(result.mediaType())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(new ByteArrayResource(result.data()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return new ApiResponse<>(200, "报告删除成功", null);
    }
}
