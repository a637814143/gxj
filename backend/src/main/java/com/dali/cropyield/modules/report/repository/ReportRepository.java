package com.dali.cropyield.modules.report.repository;

import com.dali.cropyield.modules.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
