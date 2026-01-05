package com.gxj.cropyield.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志数据访问接口
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    
    /**
     * 按用户名查询审计日志
     */
    Page<AuditLogEntity> findByUsername(String username, Pageable pageable);
    
    /**
     * 按操作类型查询审计日志
     */
    Page<AuditLogEntity> findByOperation(String operation, Pageable pageable);
    
    /**
     * 按模块查询审计日志
     */
    Page<AuditLogEntity> findByModule(String module, Pageable pageable);
    
    /**
     * 按时间范围查询审计日志
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE a.createdAt BETWEEN :startTime AND :endTime ORDER BY a.createdAt DESC")
    Page<AuditLogEntity> findByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );
    
    /**
     * 查询失败的操作
     */
    Page<AuditLogEntity> findByResult(String result, Pageable pageable);
    
    /**
     * 统计用户操作次数
     */
    @Query("SELECT a.username, COUNT(a) FROM AuditLogEntity a GROUP BY a.username ORDER BY COUNT(a) DESC")
    List<Object[]> countByUsername();
    
    /**
     * 统计操作类型分布
     */
    @Query("SELECT a.operation, COUNT(a) FROM AuditLogEntity a GROUP BY a.operation ORDER BY COUNT(a) DESC")
    List<Object[]> countByOperation();
    
    /**
     * 删除指定时间之前的审计日志
     */
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
