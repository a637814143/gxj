package com.gxj.cropyield.modules.report.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.forecast.entity.ForecastResult;
import com.gxj.cropyield.modules.forecast.repository.ForecastResultRepository;
import com.gxj.cropyield.modules.report.dto.ReportRequest;
import com.gxj.cropyield.modules.report.entity.Report;
import com.gxj.cropyield.modules.report.repository.ReportRepository;
import com.gxj.cropyield.modules.report.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ForecastResultRepository forecastResultRepository;

    public ReportServiceImpl(ReportRepository reportRepository,
                             ForecastResultRepository forecastResultRepository) {
        this.reportRepository = reportRepository;
        this.forecastResultRepository = forecastResultRepository;
    }

    @Override
    public List<Report> listAll() {
        return reportRepository.findAll();
    }

    @Override
    @Transactional
    public Report create(ReportRequest request) {
        ForecastResult result = forecastResultRepository.findById(request.forecastResultId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "预测结果不存在"));

        Report report = new Report();
        report.setTitle(request.title());
        report.setDescription(request.description());
        report.setForecastResult(result);
        report.setInsights(request.insights());
        return reportRepository.save(report);
    }
}
