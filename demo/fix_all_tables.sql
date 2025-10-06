-- 修复所有表结构以匹配实体类
-- 按正确顺序删除和重建表（先删除有外键约束的表）

-- 1. 删除有外键约束的表
DROP TABLE IF EXISTS model_parameters;
DROP TABLE IF EXISTS prediction_results;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS system_logs;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS role_permissions;

-- 2. 删除主表
DROP TABLE IF EXISTS prediction_models;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS crop_data;
DROP TABLE IF EXISTS regions;
DROP TABLE IF EXISTS data_sources;

-- 3. 重新创建主表
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    description TEXT COMMENT '角色描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '权限名称',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    description TEXT COMMENT '权限描述',
    resource VARCHAR(50) NOT NULL COMMENT '资源',
    action VARCHAR(50) NOT NULL COMMENT '操作',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    real_name VARCHAR(100) COMMENT '真实姓名',
    phone_number VARCHAR(20) COMMENT '手机号',
    organization VARCHAR(200) COMMENT '组织',
    region_code VARCHAR(20) COMMENT '地区代码',
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED') DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE crop_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    region_code VARCHAR(20) NOT NULL COMMENT '地区代码',
    region_name VARCHAR(100) NOT NULL COMMENT '地区名称',
    crop_type VARCHAR(100) NOT NULL COMMENT '作物类型',
    crop_name VARCHAR(100) NOT NULL COMMENT '作物名称',
    planting_area DECIMAL(15,2) COMMENT '种植面积（亩）',
    yield DECIMAL(15,2) COMMENT '产量（吨）',
    unit_yield DECIMAL(15,2) COMMENT '单产（吨/亩）',
    price DECIMAL(10,2) COMMENT '价格（元/吨）',
    total_value DECIMAL(15,2) COMMENT '总产值（元）',
    year INT NOT NULL COMMENT '年份',
    season VARCHAR(20) COMMENT '季节：春、夏、秋、冬',
    weather_condition VARCHAR(200) COMMENT '天气条件',
    pest_disease_level VARCHAR(50) COMMENT '病虫害程度',
    fertilizer_usage DECIMAL(10,2) COMMENT '化肥使用量（吨）',
    pesticide_usage DECIMAL(10,2) COMMENT '农药使用量（吨）',
    irrigation_area DECIMAL(15,2) COMMENT '灌溉面积（亩）',
    data_source VARCHAR(200) COMMENT '数据来源',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_region_year (region_code, year),
    INDEX idx_crop_year (crop_type, year),
    INDEX idx_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作物数据表';

CREATE TABLE regions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '地区代码',
    name VARCHAR(100) NOT NULL COMMENT '地区名称',
    parent_code VARCHAR(20) COMMENT '父级地区代码',
    level INT DEFAULT 1 COMMENT '级别',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_code (parent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表';

CREATE TABLE prediction_models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_type VARCHAR(50) NOT NULL COMMENT '模型类型：ARIMA, Prophet, XGBoost, LSTM',
    description TEXT COMMENT '模型描述',
    model_file_path VARCHAR(500) COMMENT '模型文件路径',
    parameters TEXT COMMENT '模型参数（JSON格式）',
    accuracy DOUBLE COMMENT '模型准确率',
    status ENUM('ACTIVE', 'INACTIVE', 'TRAINING', 'ERROR') DEFAULT 'ACTIVE' COMMENT '模型状态',
    created_by BIGINT COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_model_type (model_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预测模型表';

CREATE TABLE data_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    type VARCHAR(50) NOT NULL COMMENT '数据源类型',
    url VARCHAR(500) COMMENT '数据源URL',
    description TEXT COMMENT '描述',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源表';

-- 4. 创建有外键约束的表
CREATE TABLE role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE prediction_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT NOT NULL COMMENT '模型ID',
    region_code VARCHAR(20) NOT NULL COMMENT '地区代码',
    crop_type VARCHAR(100) NOT NULL COMMENT '作物类型',
    prediction_year INT NOT NULL COMMENT '预测年份',
    prediction_season VARCHAR(20) COMMENT '预测季节',
    predicted_yield DECIMAL(15,2) COMMENT '预测产量',
    predicted_area DECIMAL(15,2) COMMENT '预测种植面积',
    predicted_price DECIMAL(10,2) COMMENT '预测价格',
    confidence_interval_lower DECIMAL(15,2) COMMENT '置信区间下限',
    confidence_interval_upper DECIMAL(15,2) COMMENT '置信区间上限',
    accuracy_score DOUBLE COMMENT '预测准确度评分',
    scenario_name VARCHAR(200) COMMENT '情景名称',
    scenario_parameters TEXT COMMENT '情景参数（JSON格式）',
    created_by BIGINT COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (model_id) REFERENCES prediction_models(id) ON DELETE CASCADE,
    INDEX idx_model_region (model_id, region_code),
    INDEX idx_crop_year (crop_type, prediction_year),
    INDEX idx_region_year (region_code, prediction_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预测结果表';

CREATE TABLE model_parameters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT NOT NULL,
    parameter_name VARCHAR(100) NOT NULL COMMENT '参数名称',
    parameter_value TEXT NOT NULL COMMENT '参数值',
    parameter_type VARCHAR(50) DEFAULT 'STRING' COMMENT '参数类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (model_id) REFERENCES prediction_models(id) ON DELETE CASCADE,
    INDEX idx_model_parameter (model_id, parameter_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型参数表';

CREATE TABLE system_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '用户ID',
    action VARCHAR(100) NOT NULL COMMENT '操作',
    resource VARCHAR(100) COMMENT '资源',
    details TEXT COMMENT '详细信息',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_action (user_id, action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';

CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '报告名称',
    type VARCHAR(50) NOT NULL COMMENT '报告类型',
    format VARCHAR(20) NOT NULL COMMENT '报告格式',
    content LONGTEXT COMMENT '报告内容',
    file_path VARCHAR(500) COMMENT '文件路径',
    created_by BIGINT NOT NULL COMMENT '创建者',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_created_by (created_by),
    INDEX idx_type_format (type, format)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告表';


