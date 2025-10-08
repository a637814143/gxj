-- Schema blueprint for crop_yield database

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    email VARCHAR(128),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    locked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(128),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(256),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
);

CREATE TABLE IF NOT EXISTS ag_crop (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE,
    category VARCHAR(64),
    unit VARCHAR(64),
    description VARCHAR(256),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ag_region (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    level VARCHAR(32),
    parent_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_region_parent FOREIGN KEY (parent_id) REFERENCES ag_region(id)
);

CREATE TABLE IF NOT EXISTS ag_yield_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    crop_id BIGINT NOT NULL,
    region_id BIGINT NOT NULL,
    year INT NOT NULL,
    sown_area DECIMAL(16,4),
    harvested_area DECIMAL(16,4),
    production DECIMAL(16,4),
    yield_per_mu DECIMAL(16,4),
    data_source VARCHAR(64),
    data_version VARCHAR(32),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_yield_crop FOREIGN KEY (crop_id) REFERENCES ag_crop(id),
    CONSTRAINT fk_yield_region FOREIGN KEY (region_id) REFERENCES ag_region(id)
);

CREATE INDEX IF NOT EXISTS idx_yield_crop_region_year ON ag_yield_record (crop_id, region_id, year);

CREATE TABLE IF NOT EXISTS ag_price_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    crop_id BIGINT NOT NULL,
    year INT NOT NULL,
    average_price DECIMAL(16,4),
    unit VARCHAR(32),
    data_source VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_price_crop FOREIGN KEY (crop_id) REFERENCES ag_crop(id)
);

CREATE TABLE IF NOT EXISTS ag_dataset_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(128) NOT NULL,
    file_type VARCHAR(64) NOT NULL,
    storage_path VARCHAR(256),
    description VARCHAR(256),
    imported_at DATE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fc_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE,
    algorithm VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    hyper_parameters TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fc_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    crop_id BIGINT NOT NULL,
    region_id BIGINT NOT NULL,
    model_id BIGINT,
    start_year INT NOT NULL,
    end_year INT NOT NULL,
    horizon_years INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at DATETIME,
    completed_at DATETIME,
    error_message VARCHAR(256),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_crop FOREIGN KEY (crop_id) REFERENCES ag_crop(id),
    CONSTRAINT fk_task_region FOREIGN KEY (region_id) REFERENCES ag_region(id),
    CONSTRAINT fk_task_model FOREIGN KEY (model_id) REFERENCES fc_model(id)
);

CREATE INDEX IF NOT EXISTS idx_task_status ON fc_task (status);
CREATE INDEX IF NOT EXISTS idx_task_created_at ON fc_task (created_at);

CREATE TABLE IF NOT EXISTS fc_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    target_year INT NOT NULL,
    predicted_production DECIMAL(16,4),
    predicted_yield_per_mu DECIMAL(16,4),
    predicted_revenue DECIMAL(16,4),
    confidence_lower DECIMAL(16,4),
    confidence_upper DECIMAL(16,4),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_result_task FOREIGN KEY (task_id) REFERENCES fc_task(id)
);

CREATE TABLE IF NOT EXISTS report_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(128) NOT NULL,
    summary VARCHAR(512),
    file_url VARCHAR(256),
    generated_at DATETIME,
    generated_by VARCHAR(64),
    status VARCHAR(32),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_report_status_generated_at ON report_archive (status, generated_at);
