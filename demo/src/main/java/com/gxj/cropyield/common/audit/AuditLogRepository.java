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
    

    Page<AuditLogEntity> findByUsername(String username, Pageable pageable);
    

    Page<AuditLogEntity> findByOperation(String operation, Pageable pageable);
    

    Page<AuditLogEntity> findByModule(String module, Pageable pageable);
    

    @Query("SELECT a FROM AuditLogEntity a WHERE a.createdAt BETWEEN :startTime AND :endTime ORDER BY a.createdAt DESC")
    Page<AuditLogEntity> findByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );
    

    Page<AuditLogEntity> findByResult(String result, Pageable pageable);
    

    @Query("SELECT a.username, COUNT(a) FROM AuditLogEntity a GROUP BY a.username ORDER BY COUNT(a) DESC")
    List<Object[]> countByUsername();
    

    @Query("SELECT a.operation, COUNT(a) FROM AuditLogEntity a GROUP BY a.operation ORDER BY COUNT(a) DESC")
    List<Object[]> countByOperation();
    

    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
