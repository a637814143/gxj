package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.AsyncForecastTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 异步预测任务Repository
 */
@Repository
public interface AsyncForecastTaskRepository extends JpaRepository<AsyncForecastTask, Long> {
    
    /**
     * 根据任务ID查询
     */
    Optional<AsyncForecastTask> findByTaskId(String taskId);
    
    /**
     * 查询指定状态的任务
     */
    List<AsyncForecastTask> findByStatus(String status);
    
    /**
     * 查询运行中的任务
     */
    List<AsyncForecastTask> findByStatusIn(List<String> statuses);
    
    /**
     * 查询超时的任务
     */
    List<AsyncForecastTask> findByStatusAndStartTimeBefore(String status, LocalDateTime time);
    
    /**
     * 删除旧任务
     */
    void deleteByCreatedAtBefore(LocalDateTime time);
}
