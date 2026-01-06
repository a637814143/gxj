-- 添加审计日志表
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100),
    operation VARCHAR(50) NOT NULL,
    module VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    description VARCHAR(500),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    request_uri VARCHAR(200),
    request_method VARCHAR(10),
    request_params TEXT,
    result VARCHAR(20),
    error_message TEXT,
    execution_time BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_username (username),
    KEY idx_audit_operation (operation),
    KEY idx_audit_module (module),
    KEY idx_audit_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统审计日志';
