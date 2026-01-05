-- ============================================
-- 异步预测任务表创建脚本
-- 版本: V4
-- 创建日期: 2026-01-05
-- 说明: 创建异步预测任务表，用于跟踪长时间运行的预测任务
-- ============================================

CREATE TABLE IF NOT EXISTS async_forecast_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id VARCHAR(36) UNIQUE NOT NULL COMMENT '任务ID(UUID)',
    status VARCHAR(20) NOT NULL COMMENT '任务状态',
    task_type VARCHAR(20) NOT NULL COMMENT '任务类型',
    model_id BIGINT COMMENT '模型ID',
    crop_id BIGINT COMMENT '作物ID',
    region_id BIGINT COMMENT '区域ID',
    target_year INT COMMENT '目标年份',
    progress INT DEFAULT 0 COMMENT '进度百分比',
    current_step VARCHAR(200) COMMENT '当前步骤',
    result_id BIGINT COMMENT '结果ID',
    error_message TEXT COMMENT '错误信息',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '完成时间',
    execution_time BIGINT COMMENT '执行时长(毫秒)',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    INDEX idx_async_task_task_id (task_id),
    INDEX idx_async_task_status (status),
    INDEX idx_async_task_type (task_type),
    INDEX idx_async_task_created_at (created_at DESC),
    INDEX idx_async_task_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步预测任务表';

-- 验证表创建
SELECT 
    TABLE_NAME,
    TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'async_forecast_task';
