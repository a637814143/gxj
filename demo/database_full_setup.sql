-- 完整数据库初始化脚本
-- 包含表结构和基础数据，适用于 MySQL 8+

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================
-- 清理旧表
-- =====================
DROP TABLE IF EXISTS system_logs;
DROP TABLE IF EXISTS prediction_results;
DROP TABLE IF EXISTS crop_data;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS prediction_models;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS regions;

-- =====================
-- 表结构定义
-- =====================
CREATE TABLE regions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(20) NOT NULL UNIQUE COMMENT '地区代码',
    name         VARCHAR(100) NOT NULL COMMENT '地区名称',
    parent_code  VARCHAR(20) COMMENT '父级地区代码',
    level        INT DEFAULT 1 COMMENT '级别',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_code (parent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表';

CREATE TABLE roles (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    description  TEXT COMMENT '角色描述',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE permissions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE COMMENT '权限名称',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    description  TEXT COMMENT '权限描述',
    resource     VARCHAR(50) NOT NULL COMMENT '资源',
    action       VARCHAR(50) NOT NULL COMMENT '操作',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password      VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    email         VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    real_name     VARCHAR(100) COMMENT '真实姓名',
    phone_number  VARCHAR(20) COMMENT '手机号',
    organization  VARCHAR(200) COMMENT '组织',
    region_code   VARCHAR(20) COMMENT '所属地区代码',
    status        ENUM('ACTIVE', 'INACTIVE', 'LOCKED') DEFAULT 'ACTIVE' COMMENT '状态',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_region_code FOREIGN KEY (region_code) REFERENCES regions(code) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE prediction_models (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name       VARCHAR(150) NOT NULL COMMENT '模型名称',
    model_type       VARCHAR(50)  NOT NULL COMMENT '模型类型',
    description      TEXT COMMENT '模型描述',
    model_file_path  VARCHAR(255) COMMENT '模型文件路径',
    parameters       JSON COMMENT '模型参数（JSON）',
    accuracy         DECIMAL(6,4) COMMENT '模型准确率',
    status           ENUM('ACTIVE', 'INACTIVE', 'TRAINING', 'ERROR') DEFAULT 'ACTIVE' COMMENT '状态',
    created_by       BIGINT COMMENT '创建者',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_prediction_models_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预测模型表';

CREATE TABLE role_permissions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id        BIGINT NOT NULL,
    permission_id  BIGINT NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

CREATE TABLE user_roles (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    role_id    BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE crop_data (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    region_code           VARCHAR(20)  NOT NULL COMMENT '地区代码',
    region_name           VARCHAR(100) NOT NULL COMMENT '地区名称',
    crop_type             VARCHAR(100) NOT NULL COMMENT '作物类型',
    crop_name             VARCHAR(100) NOT NULL COMMENT '作物名称',
    planting_area         DECIMAL(15,2) COMMENT '种植面积（亩）',
    `yield`               DECIMAL(15,2) COMMENT '产量（吨）',
    unit_yield            DECIMAL(10,4) COMMENT '单产（吨/亩）',
    price                 DECIMAL(10,2) COMMENT '价格（元/吨）',
    total_value           DECIMAL(15,2) COMMENT '总产值（元）',
    year                  INT NOT NULL COMMENT '年份',
    season                VARCHAR(20) COMMENT '季节',
    weather_condition     VARCHAR(100) COMMENT '天气条件',
    pest_disease_level    VARCHAR(50) COMMENT '病虫害程度',
    fertilizer_usage      DECIMAL(10,2) COMMENT '化肥使用量（吨）',
    pesticide_usage       DECIMAL(10,2) COMMENT '农药使用量（吨）',
    irrigation_area       DECIMAL(15,2) COMMENT '灌溉面积（亩）',
    data_source           VARCHAR(200) COMMENT '数据来源',
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_crop_data_region FOREIGN KEY (region_code) REFERENCES regions(code) ON UPDATE CASCADE ON DELETE CASCADE,
    INDEX idx_region_code_year (region_code, year),
    INDEX idx_crop_type_year (crop_type, year),
    INDEX idx_region_name_year (region_name, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作物数据表';

CREATE TABLE prediction_results (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id                    BIGINT NOT NULL,
    region_code                 VARCHAR(20) NOT NULL COMMENT '地区代码',
    crop_type                   VARCHAR(100) NOT NULL COMMENT '作物类型',
    prediction_year             INT NOT NULL COMMENT '预测年份',
    prediction_season           VARCHAR(20) COMMENT '预测季节',
    predicted_yield             DECIMAL(15,2) COMMENT '预测产量（吨）',
    predicted_area              DECIMAL(15,2) COMMENT '预测种植面积（亩）',
    predicted_price             DECIMAL(10,2) COMMENT '预测价格（元/吨）',
    confidence_interval_lower   DECIMAL(15,2) COMMENT '置信区间下限',
    confidence_interval_upper   DECIMAL(15,2) COMMENT '置信区间上限',
    accuracy_score              DECIMAL(6,4) COMMENT '预测准确度评分',
    scenario_name               VARCHAR(100) COMMENT '情景名称',
    scenario_parameters         JSON COMMENT '情景参数（JSON）',
    created_by                  BIGINT COMMENT '创建者',
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_prediction_results_model FOREIGN KEY (model_id) REFERENCES prediction_models(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_prediction_results_region FOREIGN KEY (region_code) REFERENCES regions(code) ON UPDATE CASCADE,
    CONSTRAINT fk_prediction_results_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_prediction_lookup (region_code, crop_type, prediction_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预测结果表';

CREATE TABLE system_logs (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT COMMENT '用户ID',
    username         VARCHAR(50) COMMENT '用户名',
    operation        VARCHAR(100) NOT NULL COMMENT '操作类型',
    module           VARCHAR(100) NOT NULL COMMENT '模块名称',
    description      TEXT COMMENT '操作描述',
    ip_address       VARCHAR(45) COMMENT 'IP地址',
    user_agent       TEXT COMMENT '用户代理',
    request_url      VARCHAR(255) COMMENT '请求URL',
    request_method   VARCHAR(20) COMMENT '请求方法',
    request_params   TEXT COMMENT '请求参数',
    response_status  INT COMMENT '响应状态码',
    execution_time   BIGINT COMMENT '执行时间（毫秒）',
    log_level        ENUM('DEBUG', 'INFO', 'WARN', 'ERROR') DEFAULT 'INFO' COMMENT '日志级别',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_system_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_system_logs_user (user_id, created_at),
    INDEX idx_system_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================
-- 基础数据初始化
-- =====================
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO regions (code, name, parent_code, level) VALUES
('110000', '北京市', NULL, 1),
('120000', '天津市', NULL, 1),
('310000', '上海市', NULL, 1),
('320100', '南京市', '320000', 2),
('330100', '杭州市', '330000', 2),
('340100', '合肥市', '340000', 2),
('350100', '福州市', '350000', 2),
('360100', '南昌市', '360000', 2),
('440100', '广州市', '440000', 2),
('510100', '成都市', '510000', 2);

INSERT INTO roles (name, display_name, description) VALUES
('ADMIN', '系统管理员', '系统管理员，拥有全部权限'),
('RESEARCHER', '科研人员', '负责数据分析与模型训练'),
('FARMER', '农户', '查看预测结果与种植建议');

INSERT INTO permissions (name, display_name, description, resource, action) VALUES
('user:create', '创建用户', '创建系统用户', 'user', 'create'),
('user:read', '查看用户', '查看用户信息', 'user', 'read'),
('user:update', '更新用户', '更新用户信息', 'user', 'update'),
('user:delete', '删除用户', '删除系统用户', 'user', 'delete'),
('role:manage', '角色管理', '管理角色与权限', 'role', 'manage'),
('data:import', '导入数据', '导入作物数据', 'data', 'import'),
('data:export', '导出数据', '导出系统数据', 'data', 'export'),
('data:read', '查看数据', '查看作物数据', 'data', 'read'),
('prediction:run', '执行预测', '运行预测模型', 'prediction', 'run'),
('prediction:read', '查看预测', '查看预测结果', 'prediction', 'read'),
('report:generate', '生成报告', '生成分析报告', 'report', 'generate'),
('system:log', '查看日志', '查看系统日志', 'system', 'log');

-- 角色权限映射
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON 1=1
WHERE r.name = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('data:read', 'prediction:read', 'report:generate', 'prediction:run', 'data:import', 'data:export')
WHERE r.name = 'RESEARCHER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('data:read', 'prediction:read')
WHERE r.name = 'FARMER';

-- 默认用户，密码均为 BCrypt("admin123")
INSERT INTO users (username, password, email, real_name, phone_number, organization, region_code, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'admin@example.com', '系统管理员', '13800000000', '农业农村部', '110000', 'ACTIVE'),
('researcher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'researcher@example.com', '科研人员', '13900000000', '国家农业信息中心', '440100', 'ACTIVE'),
('farmer', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'farmer@example.com', '示范农户', '13700000000', '广州市示范合作社', '440100', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ADMIN' WHERE u.username = 'admin';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'RESEARCHER' WHERE u.username = 'researcher';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'FARMER' WHERE u.username = 'farmer';

INSERT INTO crop_data (
    region_code, region_name, crop_type, crop_name, planting_area, `yield`, unit_yield,
    price, total_value, year, season, weather_condition, pest_disease_level,
    fertilizer_usage, pesticide_usage, irrigation_area, data_source
) VALUES
('110000', '北京市', '粮食作物', '小麦', 1250.50, 620.30, 0.4962, 3200.00, 1984960.00, 2022, '秋', '晴朗', '轻微', 58.30, 12.40, 900.00, '国家统计局'),
('440100', '广州市', '经济作物', '甘蔗', 980.00, 812.40, 0.8290, 2600.00, 2112240.00, 2023, '冬', '多云', '无', 46.80, 8.60, 720.00, '地方农业部门'),
('510100', '成都市', '粮食作物', '水稻', 1500.00, 975.60, 0.6504, 2850.00, 2780460.00, 2023, '夏', '小雨', '中度', 70.50, 15.30, 1100.00, '农业气象监测网');

INSERT INTO prediction_models (
    model_name, model_type, description, model_file_path, parameters, accuracy, status, created_by
) VALUES
('ARIMA 粮食预测模型', 'ARIMA', '适用于粮食作物时间序列预测的ARIMA模型', '/models/arima_wheat_2023.pkl', JSON_OBJECT('p', 2, 'd', 1, 'q', 2), 0.8940, 'ACTIVE', (SELECT id FROM users WHERE username = 'researcher')),
('XGBoost 综合模型', 'XGBOOST', '融合多维度特征的XGBoost预测模型', '/models/xgboost_multi_2023.model', JSON_OBJECT('max_depth', 6, 'learning_rate', 0.1, 'n_estimators', 200), 0.9215, 'ACTIVE', (SELECT id FROM users WHERE username = 'researcher')),
('LSTM 气候敏感模型', 'LSTM', '基于气象序列的LSTM产量预测模型', '/models/lstm_climate_2023.h5', JSON_OBJECT('timesteps', 12, 'hidden_units', 128), 0.8765, 'INACTIVE', (SELECT id FROM users WHERE username = 'researcher'));

INSERT INTO prediction_results (
    model_id, region_code, crop_type, prediction_year, prediction_season,
    predicted_yield, predicted_area, predicted_price,
    confidence_interval_lower, confidence_interval_upper,
    accuracy_score, scenario_name, scenario_parameters, created_by
) VALUES
((SELECT id FROM prediction_models WHERE model_type = 'ARIMA'), '110000', '粮食作物', 2024, '秋', 640.50, 1280.00, 3300.00, 610.00, 670.00, 0.9030, '常规情景', JSON_OBJECT('rainfall', '正常', 'temperature', '正常'), (SELECT id FROM users WHERE username = 'researcher')),
((SELECT id FROM prediction_models WHERE model_type = 'XGBOOST'), '440100', '经济作物', 2024, '冬', 845.10, 990.00, 2750.00, 800.00, 890.00, 0.9270, '增产情景', JSON_OBJECT('fertilizer', '优化', 'policy', '补贴'), (SELECT id FROM users WHERE username = 'researcher')),
((SELECT id FROM prediction_models WHERE model_type = 'LSTM'), '510100', '粮食作物', 2024, '夏', 995.80, 1520.00, 2980.00, 940.00, 1050.00, 0.8825, '气候改善', JSON_OBJECT('rainfall', '偏多', 'temperature', '适宜'), (SELECT id FROM users WHERE username = 'researcher'));

INSERT INTO system_logs (
    user_id, username, operation, module, description, ip_address,
    user_agent, request_url, request_method, request_params,
    response_status, execution_time, log_level
) VALUES
((SELECT id FROM users WHERE username = 'admin'), 'admin', 'LOGIN', '认证模块', '管理员登录系统', '192.168.1.10', 'Mozilla/5.0', '/api/auth/login', 'POST', '{"username":"admin"}', 200, 185, 'INFO'),
((SELECT id FROM users WHERE username = 'researcher'), 'researcher', 'IMPORT_DATA', '数据管理', '导入2023年甘蔗数据', '192.168.1.20', 'Mozilla/5.0', '/api/data/import', 'POST', '{"file":"sugarcane_2023.xlsx"}', 200, 642, 'INFO'),
((SELECT id FROM users WHERE username = 'researcher'), 'researcher', 'RUN_MODEL', '预测分析', '运行XGBoost预测模型', '192.168.1.20', 'Mozilla/5.0', '/api/prediction/run', 'POST', '{"modelId":2}', 200, 1280, 'INFO'),
((SELECT id FROM users WHERE username = 'farmer'), 'farmer', 'VIEW_RESULT', '预测分析', '查看2024年产量预测', '192.168.1.30', 'Mozilla/5.0', '/api/prediction/results', 'GET', '{"year":2024}', 200, 95, 'INFO');

SET FOREIGN_KEY_CHECKS = 1;
