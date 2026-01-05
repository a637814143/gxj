package com.gxj.cropyield.modules.report.repository;

import com.gxj.cropyield.modules.report.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * 报表分析模块的数据访问接口（接口），封装了对报表分析相关数据表的持久化操作。
 */

public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {"sections"})
    Optional<Report> findWithSectionsById(Long id);
    
    // 预加载关联实体的分页查询
    @EntityGraph(attributePaths = {"forecastResult", "forecastResult.task", 
                                   "forecastResult.task.crop", "forecastResult.task.region"})
    Page<Report> findAll(Pageable pageable);
}
