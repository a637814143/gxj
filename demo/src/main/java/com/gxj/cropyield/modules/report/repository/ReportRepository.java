package com.gxj.cropyield.modules.report.repository;

import com.gxj.cropyield.modules.report.entity.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {"sections"})
    Optional<Report> findWithSectionsById(Long id);
}
