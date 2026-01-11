SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET @current_schema := DATABASE();


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

CREATE TABLE IF NOT EXISTS system_setting (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    default_region_id BIGINT UNSIGNED,
    notify_email VARCHAR(128),
    cluster_enabled TINYINT(1) NOT NULL DEFAULT 1,
    pending_change_count INT NOT NULL DEFAULT 0,
    security_strategy VARCHAR(64),
    announcement_title VARCHAR(128),
    announcement_message VARCHAR(512),
    announcement_status VARCHAR(32) NOT NULL DEFAULT 'INACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_system_setting_region FOREIGN KEY (default_region_id) REFERENCES base_region (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统配置项';

CREATE TABLE IF NOT EXISTS base_crop (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64),
    harvest_season VARCHAR(32) NOT NULL DEFAULT 'ANNUAL',
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_base_crop_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '基础作物信息';

-- 如果历史实例中 base_crop 表缺少 harvest_season 列，则启动时补齐，避免 Hibernate 校验失败
SET @ddl := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @current_schema
              AND table_name = 'base_crop'
              AND column_name = 'harvest_season'
        ),
        'SELECT 1',
        "ALTER TABLE base_crop ADD COLUMN harvest_season VARCHAR(32) NOT NULL DEFAULT 'ANNUAL' AFTER category"
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
    dataset_file_id BIGINT UNSIGNED,
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
    warnings_payload TINYTEXT,
    preview_payload TINYTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_import_task (task_id),
    KEY idx_import_status (status),
    KEY idx_import_dataset_file (dataset_file_id),
    CONSTRAINT fk_import_dataset_file FOREIGN KEY (dataset_file_id) REFERENCES dataset_file (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据导入任务记录';

-- 如果历史实例中 data_import_job 的 LOB 列类型被创建为 TEXT，则在启动时调整为 TINYTEXT 以匹配实体定义
SET @ddl := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @current_schema
              AND table_name = 'data_import_job'
              AND column_name = 'warnings_payload'
              AND data_type <> 'tinytext'
        ),
        'ALTER TABLE data_import_job MODIFY warnings_payload TINYTEXT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @current_schema
              AND table_name = 'data_import_job'
              AND column_name = 'preview_payload'
              AND data_type <> 'tinytext'
        ),
        'ALTER TABLE data_import_job MODIFY preview_payload TINYTEXT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

CREATE TABLE IF NOT EXISTS dataset_weather_record (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    dataset_file_id BIGINT UNSIGNED,
    region_id BIGINT UNSIGNED NOT NULL,
    record_date DATE NOT NULL,
    max_temperature DOUBLE,
    min_temperature DOUBLE,
    weather_text VARCHAR(128),
    wind VARCHAR(128),
    sunshine_hours DOUBLE,
    data_source VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_weather_region_date (region_id, record_date),
    KEY idx_weather_dataset_file (dataset_file_id),
    KEY idx_weather_region (region_id),
    CONSTRAINT fk_weather_dataset_file FOREIGN KEY (dataset_file_id) REFERENCES dataset_file (id) ON DELETE CASCADE,
    CONSTRAINT fk_weather_region FOREIGN KEY (region_id) REFERENCES base_region (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '地区逐日气象记录';

CREATE TABLE IF NOT EXISTS forecast_model (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    description VARCHAR(512),
    parameters TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_forecast_model_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测模型定义';

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_model ADD COLUMN parameters TEXT NULL AFTER description',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'forecast_model'
      AND column_name = 'parameters'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS model_registry (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    model_name VARCHAR(128) NOT NULL,
    model_type VARCHAR(32) NOT NULL,
    storage_uri VARCHAR(255) NOT NULL,
    metrics_json VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_model_registry_type (model_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测模型产出与元数据登记';

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
    measurement_label VARCHAR(64),
    measurement_unit VARCHAR(32),
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

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_run ADD COLUMN measurement_label VARCHAR(64) NULL AFTER r2',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'forecast_run'
      AND column_name = 'measurement_label'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_run ADD COLUMN measurement_unit VARCHAR(32) NULL AFTER measurement_label',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'forecast_run'
      AND column_name = 'measurement_unit'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

CREATE TABLE IF NOT EXISTS forecast_snapshot (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT UNSIGNED NOT NULL,
    period VARCHAR(32) NOT NULL,
    year INT,
    measurement_value DOUBLE,
    measurement_label VARCHAR(64),
    measurement_unit VARCHAR(32),
    predicted_production DOUBLE,
    predicted_yield DOUBLE,
    sown_area DOUBLE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_snapshot_run (run_id),
    CONSTRAINT fk_snapshot_run FOREIGN KEY (run_id) REFERENCES forecast_run (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测结果快照';

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_snapshot ADD COLUMN measurement_value DOUBLE NULL AFTER year',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'forecast_snapshot'
      AND column_name = 'measurement_value'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_snapshot ADD COLUMN measurement_label VARCHAR(64) NULL AFTER measurement_value',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'forecast_snapshot'
      AND column_name = 'measurement_label'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_snapshot ADD COLUMN measurement_unit VARCHAR(32) NULL AFTER measurement_label',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'forecast_snapshot'
      AND column_name = 'measurement_unit'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_snapshot ADD COLUMN predicted_production DOUBLE NULL AFTER measurement_unit',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'forecast_snapshot'
      AND column_name = 'predicted_production'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_snapshot ADD COLUMN predicted_yield DOUBLE NULL AFTER predicted_production',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'forecast_snapshot'
      AND column_name = 'predicted_yield'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_snapshot ADD COLUMN sown_area DOUBLE NULL AFTER predicted_yield',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'forecast_snapshot'
      AND column_name = 'sown_area'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS forecast_result (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT UNSIGNED NOT NULL,
    target_year INT NOT NULL,
    predicted_yield DOUBLE,
    measurement_value DOUBLE,
    measurement_label VARCHAR(64),
    measurement_unit VARCHAR(32),
    predicted_production DOUBLE,
    evaluation VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_result_task_year (task_id, target_year),
    KEY idx_result_task (task_id),
    CONSTRAINT fk_result_task FOREIGN KEY (task_id) REFERENCES forecast_task (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测结果摘要';

SET @ddl := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'forecast_result'
              AND column_name = 'predicted_yield'
              AND is_nullable = 'NO'
        ),
        'ALTER TABLE forecast_result MODIFY COLUMN predicted_yield DOUBLE NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_result ADD COLUMN measurement_value DOUBLE NULL AFTER predicted_yield',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'forecast_result'
      AND column_name = 'measurement_value'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_result ADD COLUMN measurement_label VARCHAR(64) NULL AFTER measurement_value',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'forecast_result'
      AND column_name = 'measurement_label'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_result ADD COLUMN measurement_unit VARCHAR(32) NULL AFTER measurement_label',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'forecast_result'
      AND column_name = 'measurement_unit'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE forecast_result ADD COLUMN predicted_production DOUBLE NULL AFTER measurement_unit',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'forecast_result'
      AND column_name = 'predicted_production'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS report_summary (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    author VARCHAR(128),
    coverage_period VARCHAR(128),
    forecast_result_id BIGINT UNSIGNED,
    insights VARCHAR(2048),
    status VARCHAR(32),
    published_at DATETIME,
    auto_generated TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_report_result (forecast_result_id),
    CONSTRAINT fk_report_result FOREIGN KEY (forecast_result_id) REFERENCES forecast_result (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测报告摘要';

CREATE TABLE IF NOT EXISTS report_section (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT UNSIGNED NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    data TEXT,
    sort_order INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_section_report (report_id),
    CONSTRAINT fk_section_report FOREIGN KEY (report_id) REFERENCES report_summary (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测报告章节';

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE report_summary ADD COLUMN author VARCHAR(128) NULL AFTER description',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'report_summary'
      AND column_name = 'author'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE report_summary ADD COLUMN coverage_period VARCHAR(128) NULL AFTER author',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'report_summary'
      AND column_name = 'coverage_period'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE report_summary ADD COLUMN status VARCHAR(32) NULL AFTER insights',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'report_summary'
      AND column_name = 'status'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE report_summary ADD COLUMN published_at DATETIME NULL AFTER status',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'report_summary'
      AND column_name = 'published_at'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE report_summary ADD COLUMN auto_generated TINYINT(1) NOT NULL DEFAULT 0 AFTER published_at',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'report_summary'
      AND column_name = 'auto_generated'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
    department_code VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_username (username),
    UNIQUE KEY uq_user_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户';

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS department_code VARCHAR(64)
        AFTER email;

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

CREATE TABLE IF NOT EXISTS auth_email_verification_code (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(128) NOT NULL,
    code VARCHAR(16) NOT NULL,
    expires_at DATETIME NOT NULL,
    used TINYINT(1) NOT NULL DEFAULT 0,
    requested_ip VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_auth_email (email),
    KEY idx_auth_ip_created (requested_ip, created_at),
    KEY idx_auth_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '邮箱验证码记录';

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

CREATE TABLE IF NOT EXISTS consultation (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(128) NOT NULL,
    crop_type VARCHAR(64) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    priority VARCHAR(32) NOT NULL DEFAULT 'normal',
    department_code VARCHAR(64),
    created_by BIGINT UNSIGNED NOT NULL,
    assigned_to BIGINT UNSIGNED,
    last_message_at DATETIME,
    closed_at DATETIME,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_consultation_status (status),
    KEY idx_consultation_creator (created_by),
    KEY idx_consultation_assignee (assigned_to),
    CONSTRAINT fk_consultation_creator FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_consultation_assignee FOREIGN KEY (assigned_to) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '在线咨询会话';

CREATE TABLE IF NOT EXISTS consultation_participant (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    consultation_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    is_owner TINYINT(1) NOT NULL DEFAULT 0,
    last_read_at DATETIME,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_consultation_participant (consultation_id, user_id),
    KEY idx_participant_user (user_id),
    CONSTRAINT fk_participant_consultation FOREIGN KEY (consultation_id) REFERENCES consultation (id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '在线咨询参与人';

CREATE TABLE IF NOT EXISTS consultation_message (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    consultation_id BIGINT UNSIGNED NOT NULL,
    sender_id BIGINT UNSIGNED NOT NULL,
    sender_role VARCHAR(64),
    content TEXT,
    recalled TINYINT(1) NOT NULL DEFAULT 0,
    recalled_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_message_consultation (consultation_id),
    KEY idx_message_sender (sender_id),
    CONSTRAINT fk_message_consultation FOREIGN KEY (consultation_id) REFERENCES consultation (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '在线咨询消息';

CREATE TABLE IF NOT EXISTS consultation_attachment (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT UNSIGNED NOT NULL,
    file_name VARCHAR(256) NOT NULL,
    original_name VARCHAR(256),
    content_type VARCHAR(128),
    file_size BIGINT,
    storage_path VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_attachment_message (message_id),
    CONSTRAINT fk_attachment_message FOREIGN KEY (message_id) REFERENCES consultation_message (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '在线咨询消息附件';

-- Backfill recall columns for existing installations without relying on
-- MySQL 8 "IF NOT EXISTS" support so MySQL 5.7 deployments succeed.
SET @missing_recalled := (
    SELECT COUNT(*) = 0
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'consultation_message'
      AND COLUMN_NAME = 'recalled'
);
SET @ddl_recalled := IF(
    @missing_recalled,
    'ALTER TABLE consultation_message ADD COLUMN recalled TINYINT(1) NOT NULL DEFAULT 0;',
    'DO 0'
);
PREPARE stmt FROM @ddl_recalled;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @missing_recalled_at := (
    SELECT COUNT(*) = 0
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'consultation_message'
      AND COLUMN_NAME = 'recalled_at'
);
SET @ddl_recalled_at := IF(
    @missing_recalled_at,
    'ALTER TABLE consultation_message ADD COLUMN recalled_at TIMESTAMP NULL DEFAULT NULL;',
    'DO 0'
);
PREPARE stmt2 FROM @ddl_recalled_at;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET FOREIGN_KEY_CHECKS = 1;
