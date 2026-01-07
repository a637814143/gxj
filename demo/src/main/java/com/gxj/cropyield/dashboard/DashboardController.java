package com.gxj.cropyield.dashboard;

import com.gxj.cropyield.dashboard.dto.DashboardSummaryResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
/**
 * 驾驶舱统计模块的控制器，用于暴露驾驶舱统计相关的 REST 接口。
 * <p>核心方法：getSummary、exportForecastDataAsPdf。</p>
 */

/**
 * 仪表盘控制器 - 数据统计、概览信息
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportForecastDataAsPdf(
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer year
    ) {
        byte[] pdfData = dashboardService.exportForecastDataAsPdf(crop, region, year);
        String filename = "仪表盘预测数据_" + LocalDate.now() + ".pdf";
        
        try {
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename*=UTF-8''" + encodedFilename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfData);
        } catch (UnsupportedEncodingException e) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dashboard_data.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfData);
        }
    }
}

