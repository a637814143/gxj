package com.dali.cropyield.modules.report.service;

import com.dali.cropyield.modules.report.dto.ReportRequest;
import com.dali.cropyield.modules.report.entity.Report;
import java.util.List;

public interface ReportService {

    List<Report> findAll();

    Report create(ReportRequest request);
}
