SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS report_summary;
DROP TABLE IF EXISTS data_import_job_error;
DROP TABLE IF EXISTS data_import_job;
DROP TABLE IF EXISTS forecast_run_series;
DROP TABLE IF EXISTS forecast_result;
DROP TABLE IF EXISTS forecast_run;
DROP TABLE IF EXISTS forecast_task;
DROP TABLE IF EXISTS forecast_model;
DROP TABLE IF EXISTS dataset_yield_record;
DROP TABLE IF EXISTS dataset_file;
DROP TABLE IF EXISTS base_crop;
DROP TABLE IF EXISTS base_region;
DROP TABLE IF EXISTS sys_log;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_refresh_token;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_permission;

CREATE TABLE base_region (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    level VARCHAR(32) NOT NULL,
    parent_code VARCHAR(64),
    parent_name VARCHAR(128),
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_base_region_code (code),
    KEY idx_base_region_level (level)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '基础行政区域信息';

CREATE TABLE base_crop (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64),
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_base_crop_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '基础作物信息';

CREATE TABLE dataset_file (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    storage_path VARCHAR(256) NOT NULL,
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_dataset_file_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '原始数据文件登记';

CREATE TABLE data_import_job (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    dataset_name VARCHAR(128),
    dataset_description VARCHAR(256),
    dataset_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    original_filename VARCHAR(256),
    storage_path VARCHAR(512),
    total_rows INT,
    processed_rows INT,
    inserted_rows INT,
    updated_rows INT,
    skipped_rows INT,
    failed_rows INT,
    warning_count INT,
    message VARCHAR(512),
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    warnings_payload TINYTEXT,
    preview_payload TINYTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_import_task (task_id),
    KEY idx_import_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据导入任务记录';

CREATE TABLE data_import_job_error (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT UNSIGNED NOT NULL,
    line_number INT,
    error_code VARCHAR(64),
    message VARCHAR(512),
    raw_value VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_error_job (job_id),
    CONSTRAINT fk_error_job FOREIGN KEY (job_id) REFERENCES data_import_job (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据导入错误摘要';

CREATE TABLE dataset_yield_record (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crop_id BIGINT UNSIGNED NOT NULL,
    region_id BIGINT UNSIGNED NOT NULL,
    year INT NOT NULL,
    sown_area DOUBLE,
    production DOUBLE,
    yield_per_hectare DOUBLE,
    data_source VARCHAR(256),
    collected_at DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_yield_crop_region_year (crop_id, region_id, year),
    KEY idx_yield_crop (crop_id),
    KEY idx_yield_region (region_id),
    CONSTRAINT fk_yield_crop FOREIGN KEY (crop_id) REFERENCES base_crop (id) ON DELETE CASCADE,
    CONSTRAINT fk_yield_region FOREIGN KEY (region_id) REFERENCES base_region (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '作物年均单产记录';

CREATE TABLE forecast_model (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_forecast_model_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测模型定义';

CREATE TABLE forecast_task (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT UNSIGNED NOT NULL,
    crop_id BIGINT UNSIGNED NOT NULL,
    region_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(32) NOT NULL,
    parameters VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_task_model_crop_region (model_id, crop_id, region_id),
    KEY idx_task_model (model_id),
    KEY idx_task_crop (crop_id),
    KEY idx_task_region (region_id),
    CONSTRAINT fk_task_model FOREIGN KEY (model_id) REFERENCES forecast_model (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_crop FOREIGN KEY (crop_id) REFERENCES base_crop (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_region FOREIGN KEY (region_id) REFERENCES base_region (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测任务配置';

CREATE TABLE forecast_run (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT UNSIGNED NOT NULL,
    crop_id BIGINT UNSIGNED NOT NULL,
    region_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(32) NOT NULL,
    forecast_periods INT NOT NULL,
    history_years INT,
    frequency VARCHAR(32),
    external_request_id VARCHAR(64),
    error_message VARCHAR(512),
    mae DOUBLE,
    rmse DOUBLE,
    mape DOUBLE,
    r2 DOUBLE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_run_external_request (external_request_id),
    KEY idx_run_model (model_id),
    KEY idx_run_crop (crop_id),
    KEY idx_run_region (region_id),
    CONSTRAINT fk_run_model FOREIGN KEY (model_id) REFERENCES forecast_model (id) ON DELETE CASCADE,
    CONSTRAINT fk_run_crop FOREIGN KEY (crop_id) REFERENCES base_crop (id) ON DELETE CASCADE,
    CONSTRAINT fk_run_region FOREIGN KEY (region_id) REFERENCES base_region (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测执行记录';

CREATE TABLE forecast_run_series (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT UNSIGNED NOT NULL,
    period VARCHAR(32) NOT NULL,
    value DOUBLE NOT NULL,
    lower_bound DOUBLE,
    upper_bound DOUBLE,
    historical TINYINT(1) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_run_series_run (run_id),
    CONSTRAINT fk_run_series_run FOREIGN KEY (run_id) REFERENCES forecast_run (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测结果时间序列';

CREATE TABLE forecast_result (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT UNSIGNED NOT NULL,
    target_year INT NOT NULL,
    predicted_yield DOUBLE NOT NULL,
    evaluation VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_result_task_year (task_id, target_year),
    KEY idx_result_task (task_id),
    CONSTRAINT fk_result_task FOREIGN KEY (task_id) REFERENCES forecast_task (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测结果摘要';

CREATE TABLE report_summary (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    forecast_result_id BIGINT UNSIGNED,
    insights VARCHAR(2048),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_report_result (forecast_result_id),
    CONSTRAINT fk_report_result FOREIGN KEY (forecast_result_id) REFERENCES forecast_result (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测报告摘要';

CREATE TABLE sys_permission (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_permission_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统权限定义';

CREATE TABLE sys_role (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统角色定义';

CREATE TABLE sys_user (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    full_name VARCHAR(128),
    email VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_username (username),
    UNIQUE KEY uq_user_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户';

CREATE TABLE sys_login_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    success TINYINT(1) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(256),
    message VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_login_username (username),
    KEY idx_login_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户登录日志';

CREATE TABLE sys_refresh_token (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_refresh_token (token),
    KEY idx_refresh_user (user_id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '刷新令牌';

CREATE TABLE sys_user_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户与角色关联';

CREATE TABLE sys_role_permission (
    role_id BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色与权限关联';

CREATE TABLE sys_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    action VARCHAR(128) NOT NULL,
    detail VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统操作日志';

-- 初始化默认角色与管理员账户，支持重复执行
INSERT INTO sys_role (code, name, description) VALUES
    ('ADMIN', '系统管理员', '默认管理员角色'),
    ('AGRICULTURE_DEPT', '农业部门用户', '农业主管部门默认角色'),
    ('FARMER', '企业/农户用户', '企业/农户默认角色')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO sys_user (username, password, full_name, email) VALUES
    ('admin', '$2b$10$8MWkbhb1FAusZAaOMcBj1u1sxMlX5bdDLhmw98ouzYRR33givkqYa', '系统管理员', NULL)
ON DUPLICATE KEY UPDATE password = VALUES(password), full_name = VALUES(full_name), email = VALUES(email);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

SET FOREIGN_KEY_CHECKS = 1;
