SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


CREATE TABLE IF NOT EXISTS base_region (
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

CREATE TABLE IF NOT EXISTS base_crop (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64),
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_base_crop_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '基础作物信息';

CREATE TABLE IF NOT EXISTS dataset_file (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    storage_path VARCHAR(256) NOT NULL,
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_dataset_file_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '原始数据文件登记';

CREATE TABLE IF NOT EXISTS data_import_job (
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
    started_at DATETIME,
    finished_at DATETIME,
    warnings_payload TEXT,
    preview_payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_import_task (task_id),
    KEY idx_import_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据导入任务记录';

CREATE TABLE IF NOT EXISTS data_import_job_error (
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

CREATE TABLE IF NOT EXISTS dataset_yield_record (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    dataset_file_id BIGINT UNSIGNED,
    crop_id BIGINT UNSIGNED NOT NULL,
    region_id BIGINT UNSIGNED NOT NULL,
    year INT NOT NULL,
    sown_area DOUBLE,
    production DOUBLE,
    yield_per_hectare DOUBLE,
    average_price DOUBLE,
    data_source VARCHAR(256),
    collected_at DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_yield_crop_region_year (crop_id, region_id, year),
    KEY idx_yield_dataset_file (dataset_file_id),
    KEY idx_yield_crop (crop_id),
    KEY idx_yield_region (region_id),
    CONSTRAINT fk_yield_dataset_file FOREIGN KEY (dataset_file_id) REFERENCES dataset_file (id) ON DELETE CASCADE,
    CONSTRAINT fk_yield_crop FOREIGN KEY (crop_id) REFERENCES base_crop (id) ON DELETE CASCADE,
    CONSTRAINT fk_yield_region FOREIGN KEY (region_id) REFERENCES base_region (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '作物年均单产记录';

CREATE TABLE IF NOT EXISTS dataset_price_record (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    dataset_file_id BIGINT UNSIGNED,
    crop_id BIGINT UNSIGNED NOT NULL,
    region_id BIGINT UNSIGNED NOT NULL,
    record_date DATE NOT NULL,
    price DOUBLE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_price_crop_region_date (crop_id, region_id, record_date),
    KEY idx_price_dataset_file (dataset_file_id),
    KEY idx_price_crop (crop_id),
    KEY idx_price_region (region_id),
    CONSTRAINT fk_price_dataset_file FOREIGN KEY (dataset_file_id) REFERENCES dataset_file (id) ON DELETE CASCADE,
    CONSTRAINT fk_price_crop FOREIGN KEY (crop_id) REFERENCES base_crop (id) ON DELETE CASCADE,
    CONSTRAINT fk_price_region FOREIGN KEY (region_id) REFERENCES base_region (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '作物价格时间序列';

CREATE TABLE IF NOT EXISTS forecast_model (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_forecast_model_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测模型定义';

CREATE TABLE IF NOT EXISTS forecast_task (
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

CREATE TABLE IF NOT EXISTS forecast_run (
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

CREATE TABLE IF NOT EXISTS forecast_run_series (
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

CREATE TABLE IF NOT EXISTS forecast_result (
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

CREATE TABLE IF NOT EXISTS report_summary (
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

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_permission_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统权限定义';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统角色定义';

CREATE TABLE IF NOT EXISTS sys_user (
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

CREATE TABLE IF NOT EXISTS sys_login_log (
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

CREATE TABLE IF NOT EXISTS sys_refresh_token (
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

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户与角色关联';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色与权限关联';

CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    action VARCHAR(128) NOT NULL,
    detail VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统操作日志';

SET FOREIGN_KEY_CHECKS = 1;
