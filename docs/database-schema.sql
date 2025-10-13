-- 数据库初始化脚本（MySQL 8.x）

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 权限相关表
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_permission (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    code         VARCHAR(64)  NOT NULL UNIQUE,
    name         VARCHAR(128) NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE sys_role (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    code         VARCHAR(64)  NOT NULL UNIQUE,
    name         VARCHAR(128) NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE sys_user (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(64)  NOT NULL UNIQUE,
    password     VARCHAR(128) NOT NULL,
    full_name    VARCHAR(128),
    email        VARCHAR(128),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE sys_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_role_perm_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 2. 基础信息
DROP TABLE IF EXISTS base_crop;
DROP TABLE IF EXISTS base_region;

CREATE TABLE base_crop (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    code         VARCHAR(64)  NOT NULL UNIQUE,
    name         VARCHAR(128) NOT NULL,
    description  VARCHAR(256),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE base_region (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    code         VARCHAR(64)  NOT NULL UNIQUE,
    name         VARCHAR(128) NOT NULL,
    description  VARCHAR(256),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 3. 数据资源
DROP TABLE IF EXISTS dataset_price_record;
DROP TABLE IF EXISTS dataset_yield_record;
DROP TABLE IF EXISTS dataset_file;

CREATE TABLE dataset_file (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(128) NOT NULL,
    type         VARCHAR(32)  NOT NULL,
    storage_path VARCHAR(256) NOT NULL,
    description  VARCHAR(256),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE dataset_yield_record (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    crop_id           BIGINT       NOT NULL,
    region_id         BIGINT       NOT NULL,
    year              INT          NOT NULL,
    yield_per_hectare DECIMAL(10,2) NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_yield_crop FOREIGN KEY (crop_id) REFERENCES base_crop (id),
    CONSTRAINT fk_yield_region FOREIGN KEY (region_id) REFERENCES base_region (id),
    INDEX idx_yield_year (year)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE dataset_price_record (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    crop_id     BIGINT       NOT NULL,
    region_id   BIGINT       NOT NULL,
    record_date DATE         NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_price_crop FOREIGN KEY (crop_id) REFERENCES base_crop (id),
    CONSTRAINT fk_price_region FOREIGN KEY (region_id) REFERENCES base_region (id),
    INDEX idx_price_record_date (record_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 4. 预测分析
DROP TABLE IF EXISTS forecast_result;
DROP TABLE IF EXISTS forecast_task;
DROP TABLE IF EXISTS forecast_model;

CREATE TABLE forecast_model (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(128) NOT NULL,
    type         VARCHAR(32)  NOT NULL,
    description  VARCHAR(512),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE forecast_task (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_id    BIGINT       NOT NULL,
    crop_id     BIGINT       NOT NULL,
    region_id   BIGINT       NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    parameters  VARCHAR(512),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_model FOREIGN KEY (model_id) REFERENCES forecast_model (id),
    CONSTRAINT fk_task_crop FOREIGN KEY (crop_id) REFERENCES base_crop (id),
    CONSTRAINT fk_task_region FOREIGN KEY (region_id) REFERENCES base_region (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE forecast_result (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL,
    target_year     INT          NOT NULL,
    predicted_yield DECIMAL(10,2) NOT NULL,
    evaluation      VARCHAR(1024),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_result_task FOREIGN KEY (task_id) REFERENCES forecast_task (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 5. 报告输出
DROP TABLE IF EXISTS report_summary;

CREATE TABLE report_summary (
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    title              VARCHAR(128) NOT NULL,
    description        VARCHAR(512),
    forecast_result_id BIGINT,
    insights           VARCHAR(2048),
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_result FOREIGN KEY (forecast_result_id) REFERENCES forecast_result (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- 6. 初始化示例数据
INSERT INTO sys_permission (code, name) VALUES
 ('DASHBOARD_VIEW', '查看仪表盘'),
 ('DATA_MANAGE', '数据管理'),
 ('FORECAST_MANAGE', '预测任务管理');

INSERT INTO sys_role (code, name) VALUES
 ('ADMIN', '系统管理员'),
 ('ANALYST', '业务分析员'),
 ('VIEWER', '决策查看者');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE code = 'ADMIN'), id FROM sys_permission;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE code = 'ANALYST'), id FROM sys_permission WHERE code IN ('DASHBOARD_VIEW', 'DATA_MANAGE');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE code = 'VIEWER'), id FROM sys_permission WHERE code = 'DASHBOARD_VIEW';

INSERT INTO sys_user (username, password, full_name, email) VALUES
 ('admin', '$2a$10$wF2oTObgGJE0E7E5Wdl66uYcmGeXgnz9K/Y/xFdVtOfvtTDHkJ/xS', '平台管理员', 'admin@example.com');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u CROSS JOIN sys_role r WHERE u.username = 'admin' AND r.code = 'ADMIN';

INSERT INTO base_crop (code, name, description) VALUES
 ('RICE', '水稻', '云南主粮之一，集中分布在滇中和滇南稻区'),
 ('CORN', '玉米', '适宜山地丘陵地区，兼顾粮饲双用');

INSERT INTO base_region (code, name, description) VALUES
 ('YN', '云南省', '省级统计总览'),
 ('YN-KM', '昆明市', '滇中城市群核心城市');
