-- ============================================
-- 审计日志表创建脚本
-- 版本: V3
-- 创建日期: 2026-01-05
-- 说明: 创建审计日志表，用于记录系统操作审计信息
-- ============================================

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(100) COMMENT '操作用户名',
    operation VARCHAR(50) NOT NULL COMMENT '操作类型',
    module VARCHAR(50) NOT NULL COMMENT '模块名称',
    entity_type VARCHAR(100) COMMENT '实体类型',
    entity_id BIGINT COMMENT '实体ID',
    description VARCHAR(500) COMMENT '操作描述',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    request_uri VARCHAR(200) COMMENT '请求URI',
    request_method VARCHAR(10) COMMENT '请求方法',
    request_params TEXT COMMENT '请求参数',
    result VARCHAR(20) COMMENT '操作结果',
    error_message TEXT COMMENT '错误信息',
    execution_time BIGINT COMMENT '执行时间(毫秒)',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_audit_log_username (username),
    INDEX idx_audit_log_operation (operation),
    INDEX idx_audit_log_module (module),
    INDEX idx_audit_log_created_at (created_at DESC),
    INDEX idx_audit_log_result (result),
    INDEX idx_audit_log_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 验证表创建
SELECT 
    TABLE_NAME,
    TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'sys_audit_log';
