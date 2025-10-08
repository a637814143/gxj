package com.dali.cropyield.modules.report.service.impl;

import com.dali.cropyield.modules.report.dto.ReportRequest;
import com.dali.cropyield.modules.report.entity.Report;
import com.dali.cropyield.modules.report.repository.ReportRepository;
import com.dali.cropyield.modules.report.service.ReportService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

    @Override
    public List<Report> findAll() {
        return reportRepository.findAll();
    }

    @Override
    @Transactional
    public Report create(ReportRequest request) {
        Report report = new Report();
        report.setTitle(request.title());
        report.setSummary(request.summary());
        report.setFileUrl(request.fileUrl());
        report.setGeneratedBy(request.generatedBy());
        report.setGeneratedAt(LocalDateTime.now());
        report.setStatus("GENERATED");
        return reportRepository.save(report);
    }
}
