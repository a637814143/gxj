package com.gxj.cropyield.modules.report.service;

import com.gxj.cropyield.modules.report.dto.ReportDetailResponse;
import com.gxj.cropyield.modules.report.dto.ReportGenerationRequest;
import com.gxj.cropyield.modules.report.dto.ReportOverviewResponse;
import com.gxj.cropyield.modules.report.dto.ReportRequest;
import com.gxj.cropyield.modules.report.entity.Report;
/**
 * 报表分析模块的业务接口（接口），定义报表分析相关的核心业务操作。
 * <p>核心方法：getOverview、create、generate、getDetail。</p>
 */

public interface ReportService {

    ReportOverviewResponse getOverview();

    Report create(ReportRequest request);

    ReportDetailResponse generate(ReportGenerationRequest request);

    ReportDetailResponse getDetail(Long id);

    ReportExportResult export(Long id, String format);

    void deleteReport(Long id);
}
