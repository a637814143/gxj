SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS recommendation_record;
DROP TABLE IF EXISTS model_metric;
DROP TABLE IF EXISTS forecast_result;
DROP TABLE IF EXISTS forecast_task;
DROP TABLE IF EXISTS data_cleaning_log;
DROP TABLE IF EXISTS climate_indicator;
DROP TABLE IF EXISTS production_record;
DROP TABLE IF EXISTS data_source;
DROP TABLE IF EXISTS crop;
DROP TABLE IF EXISTS region;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_refresh_token;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_permission;

CREATE TABLE region (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(128)  NOT NULL,
    parent_id     BIGINT UNSIGNED,
    level         VARCHAR(32)   NOT NULL,
    geo_code      VARCHAR(32),
    latitude      DECIMAL(9, 6),
    longitude     DECIMAL(9, 6),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_region_name_parent (name, parent_id),
    KEY idx_region_parent (parent_id),
    CONSTRAINT fk_region_parent FOREIGN KEY (parent_id) REFERENCES region (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '行政区域信息表';

CREATE TABLE crop (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(128)  NOT NULL,
    category       VARCHAR(64)   NOT NULL,
    variety        VARCHAR(128),
    growth_cycle   INT,
    unit           VARCHAR(16)   NOT NULL DEFAULT '吨',
    description    VARCHAR(512),
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_crop_name_variety (name, variety)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '农作物基础信息表';

CREATE TABLE data_source (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(128)  NOT NULL,
    type          VARCHAR(64)   NOT NULL,
    description   VARCHAR(512),
    imported_by   VARCHAR(64),
    import_time   DATETIME,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据来源记录表';

CREATE TABLE production_record (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    region_id        BIGINT UNSIGNED NOT NULL,
    crop_id          BIGINT UNSIGNED NOT NULL,
    data_source_id   BIGINT UNSIGNED,
    stat_year        INT            NOT NULL,
    stat_month       TINYINT        NOT NULL,
    area_planted     DECIMAL(14, 2),
    yield_amount     DECIMAL(14, 2) NOT NULL,
    yield_per_unit   DECIMAL(14, 4),
    unit_price       DECIMAL(10, 2),
    is_estimated     BOOLEAN        NOT NULL DEFAULT FALSE,
    quality_flag     VARCHAR(32),
    collected_at     DATE,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_production_period (region_id, crop_id, stat_year, stat_month),
    KEY idx_production_source (data_source_id),
    CONSTRAINT fk_production_region FOREIGN KEY (region_id) REFERENCES region (id) ON DELETE CASCADE,
    CONSTRAINT fk_production_crop FOREIGN KEY (crop_id) REFERENCES crop (id) ON DELETE CASCADE,
    CONSTRAINT fk_production_source FOREIGN KEY (data_source_id) REFERENCES data_source (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '农作物产量时序数据表';

CREATE TABLE climate_indicator (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    region_id        BIGINT UNSIGNED NOT NULL,
    data_source_id   BIGINT UNSIGNED,
    stat_date        DATE           NOT NULL,
    avg_temp         DECIMAL(5, 2),
    rainfall         DECIMAL(10, 2),
    sunshine_hours   DECIMAL(8, 2),
    soil_moisture    DECIMAL(8, 2),
    extreme_event    VARCHAR(128),
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_climate_period (region_id, stat_date),
    KEY idx_climate_source (data_source_id),
    CONSTRAINT fk_climate_region FOREIGN KEY (region_id) REFERENCES region (id) ON DELETE CASCADE,
    CONSTRAINT fk_climate_source FOREIGN KEY (data_source_id) REFERENCES data_source (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '气象指标记录表';

CREATE TABLE data_cleaning_log (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    record_type    VARCHAR(32)   NOT NULL,
    record_id      BIGINT UNSIGNED NOT NULL,
    issue_type     VARCHAR(64)   NOT NULL,
    action_taken   VARCHAR(512),
    processed_by   VARCHAR(64),
    processed_at   DATETIME,
    status         VARCHAR(32)   NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_cleaning_record (record_type, record_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据清洗与质量追踪日志';

CREATE TABLE forecast_task (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    task_code       VARCHAR(64)   NOT NULL,
    crop_id         BIGINT UNSIGNED NOT NULL,
    region_id       BIGINT UNSIGNED NOT NULL,
    model_type      VARCHAR(32)   NOT NULL,
    horizon         INT           NOT NULL,
    status          VARCHAR(32)   NOT NULL,
    trigger_type    VARCHAR(32)   NOT NULL,
    scheduled_time  DATETIME,
    created_by      VARCHAR(64),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_task_code (task_code),
    KEY idx_task_region_crop (region_id, crop_id),
    CONSTRAINT fk_task_crop FOREIGN KEY (crop_id) REFERENCES crop (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_region FOREIGN KEY (region_id) REFERENCES region (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测任务配置表';

CREATE TABLE forecast_result (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    task_id            BIGINT UNSIGNED NOT NULL,
    forecast_date      DATE           NOT NULL,
    prediction_start   DATE,
    prediction_end     DATE,
    predicted_value    DECIMAL(14, 2) NOT NULL,
    lower_bound        DECIMAL(14, 2),
    upper_bound        DECIMAL(14, 2),
    confidence         DECIMAL(5, 2),
    model_version      VARCHAR(64),
    generated_at       DATETIME       NOT NULL,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_forecast_period (task_id, forecast_date),
    CONSTRAINT fk_result_task FOREIGN KEY (task_id) REFERENCES forecast_task (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预测结果明细表';

CREATE TABLE model_metric (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    task_id        BIGINT UNSIGNED NOT NULL,
    metric_name    VARCHAR(64)   NOT NULL,
    metric_value   DECIMAL(16, 6) NOT NULL,
    dataset_type   VARCHAR(32)   NOT NULL,
    calculated_at  DATETIME      NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_metric_task FOREIGN KEY (task_id) REFERENCES forecast_task (id) ON DELETE CASCADE,
    KEY idx_metric_task (task_id),
    KEY idx_metric_name (metric_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '模型评估指标表';

CREATE TABLE recommendation_record (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    task_id           BIGINT UNSIGNED NOT NULL,
    scenario_name     VARCHAR(128)  NOT NULL,
    expected_revenue  DECIMAL(14, 2),
    cost              DECIMAL(14, 2),
    profit            DECIMAL(14, 2),
    recommendation    VARCHAR(512),
    assumptions       VARCHAR(512),
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_recommendation_task FOREIGN KEY (task_id) REFERENCES forecast_task (id) ON DELETE CASCADE,
    KEY idx_recommendation_task (task_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收益与决策建议表';

CREATE TABLE sys_permission (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(64)   NOT NULL,
    name        VARCHAR(128)  NOT NULL,
    description VARCHAR(256),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_permission_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统权限定义表';

CREATE TABLE sys_role (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(64)   NOT NULL,
    name        VARCHAR(128)  NOT NULL,
    description VARCHAR(256),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统角色定义表';

CREATE TABLE sys_user (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(64)   NOT NULL,
    password      VARCHAR(128)  NOT NULL,
    full_name     VARCHAR(128),
    email         VARCHAR(128),
    phone         VARCHAR(32),
    status        VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_username (username),
    UNIQUE KEY uq_user_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

CREATE TABLE sys_login_log (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(64)   NOT NULL,
    success      BOOLEAN       NOT NULL,
    ip_address   VARCHAR(64),
    user_agent   VARCHAR(256),
    message      VARCHAR(256),
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_login_username (username),
    KEY idx_login_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户登录日志';

CREATE TABLE sys_refresh_token (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    token        VARCHAR(128)  NOT NULL,
    expires_at   DATETIME      NOT NULL,
    user_id      BIGINT UNSIGNED NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_refresh_token (token),
    KEY idx_refresh_user (user_id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '刷新令牌表';

CREATE TABLE sys_user_role (
    user_id    BIGINT UNSIGNED NOT NULL,
    role_id    BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户与角色关联表';

CREATE TABLE sys_role_permission (
    role_id       BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色与权限关联表';

SET FOREIGN_KEY_CHECKS = 1;
