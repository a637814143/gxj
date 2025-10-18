package com.gxj.cropyield.modules.report.service;

import com.gxj.cropyield.modules.report.dto.ReportOverviewResponse;
import com.gxj.cropyield.modules.report.dto.ReportRequest;
import com.gxj.cropyield.modules.report.entity.Report;

public interface ReportService {

    ReportOverviewResponse getOverview();

    Report create(ReportRequest request);
}
