package com.gxj.cropyield.modules.report.service;

import com.gxj.cropyield.modules.report.dto.ReportRequest;
import com.gxj.cropyield.modules.report.entity.Report;

import java.util.List;

public interface ReportService {

    List<Report> listAll();

    Report create(ReportRequest request);
}
