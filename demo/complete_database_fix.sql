-- 完整的数据库修复SQL
-- 解决字段不匹配和表结构问题

-- 1. 首先创建或修复 regions 表
DROP TABLE IF EXISTS regions;
CREATE TABLE regions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '地区代码',
    name VARCHAR(100) NOT NULL COMMENT '地区名称',
    parent_code VARCHAR(20) COMMENT '父级地区代码',
    level INT DEFAULT 1 COMMENT '级别',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_code (parent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表';

-- 2. 重新创建 crop_data 表，确保字段名与实体类完全匹配
DROP TABLE IF EXISTS crop_data;
CREATE TABLE crop_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    region_code VARCHAR(20) NOT NULL COMMENT '地区代码',
    region_name VARCHAR(100) NOT NULL COMMENT '地区名称',
    crop_type VARCHAR(100) NOT NULL COMMENT '作物类型',
    crop_name VARCHAR(100) NOT NULL COMMENT '作物名称',
    planting_area DECIMAL(15,2) COMMENT '种植面积（亩）',
    yield_amount DECIMAL(15,2) COMMENT '产量（吨）',
    unit_yield DECIMAL(10,4) COMMENT '单产（吨/亩）',
    price DECIMAL(10,2) COMMENT '价格（元/吨）',
    total_value DECIMAL(15,2) COMMENT '总产值（元）',
    year INT NOT NULL COMMENT '年份',
    season VARCHAR(20) COMMENT '季节',
    weather_condition VARCHAR(100) COMMENT '天气条件',
    pest_disease_level VARCHAR(50) COMMENT '病虫害程度',
    fertilizer_usage DECIMAL(10,2) COMMENT '化肥使用量（吨）',
    pesticide_usage DECIMAL(10,2) COMMENT '农药使用量（吨）',
    irrigation_area DECIMAL(15,2) COMMENT '灌溉面积（亩）',
    data_source VARCHAR(200) COMMENT '数据来源',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_region_code_year (region_code, year),
    INDEX idx_crop_type_year (crop_type, year),
    INDEX idx_region_name_year (region_name, year),
    INDEX idx_year (year),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作物数据表';

-- 3. 创建其他必要的基础表
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    description TEXT COMMENT '角色描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '权限名称',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    description TEXT COMMENT '权限描述',
    resource VARCHAR(50) NOT NULL COMMENT '资源',
    action VARCHAR(50) NOT NULL COMMENT '操作',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE IF NOT EXISTS users (
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

CREATE TABLE prediction_models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模型名称',
    type VARCHAR(50) NOT NULL COMMENT '模型类型',
    description TEXT COMMENT '模型描述',
    parameters JSON COMMENT '模型参数',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预测模型表';

CREATE TABLE prediction_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT NOT NULL,
    crop_type VARCHAR(100) NOT NULL COMMENT '作物类型',
    region VARCHAR(100) NOT NULL COMMENT '地区',
    year INT NOT NULL COMMENT '预测年份',
    predicted_yield DECIMAL(15,2) NOT NULL COMMENT '预测产量',
    confidence_interval VARCHAR(50) COMMENT '置信区间',
    accuracy_score DECIMAL(5,4) COMMENT '准确度评分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (model_id) REFERENCES prediction_models(id) ON DELETE CASCADE,
    INDEX idx_crop_region_year (crop_type, region, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预测结果表';

-- 4. 插入基础数据
-- 插入地区数据
INSERT INTO regions (code, name, level) VALUES 
('110000', '北京市', 1),
('310000', '上海市', 1),
('440000', '广东省', 1),
('130000', '河北省', 1),
('230000', '黑龙江省', 1),
('510000', '四川省', 1),
('370000', '山东省', 1),
('430000', '湖南省', 1),
('320000', '江苏省', 1),
('330000', '浙江省', 1);

-- 插入角色数据
INSERT INTO roles (name, display_name, description) VALUES 
('ADMIN', '系统管理员', '系统管理员，拥有所有权限'),
('RESEARCHER', '研究员', '数据分析员，可以查看和操作数据'),
('FARMER', '农民', '普通用户，可以查看基础数据');

-- 插入权限数据
INSERT INTO permissions (name, display_name, description, resource, action) VALUES 
('user:read', '查看用户', '查看用户信息', 'user', 'read'),
('crop_data:read', '查看作物数据', '查看作物数据', 'crop_data', 'read'),
('crop_data:create', '创建作物数据', '创建新的作物数据记录', 'crop_data', 'create'),
('crop_data:update', '更新作物数据', '更新作物数据', 'crop_data', 'update'),
('crop_data:delete', '删除作物数据', '删除作物数据', 'crop_data', 'delete'),
('crop_data:import', '导入数据', '导入外部数据到系统', 'crop_data', 'import'),
('prediction:read', '查看预测', '查看预测结果', 'prediction', 'read'),
('prediction:create', '创建预测', '创建产量预测', 'prediction', 'create');

-- 为FARMER角色分配基础权限
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'FARMER' AND p.name IN ('crop_data:read', 'prediction:read');

-- 为RESEARCHER角色分配数据权限
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'RESEARCHER' AND p.name IN ('crop_data:read', 'crop_data:create', 'crop_data:update', 'prediction:read', 'prediction:create');

-- 为ADMIN角色分配所有权限
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'ADMIN';

-- 创建默认管理员
INSERT INTO users (username, password, email, real_name, status) VALUES 
('admin', 'admin123', 'admin@example.com', '系统管理员', 'ACTIVE');

-- 为管理员分配角色
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.username = 'admin' AND r.name = 'ADMIN';

-- 5. 插入作物测试数据
INSERT INTO crop_data (
    region_code, region_name, crop_type, crop_name, planting_area, yield_amount, 
    unit_yield, price, total_value, year, season, weather_condition, 
    pest_disease_level, fertilizer_usage, pesticide_usage, irrigation_area, data_source
) VALUES 
('110000', '北京市', '粮食作物', '小麦', 1000.50, 500.25, 0.5000, 3000.00, 1500750.00, 2023, '秋', '晴朗', '轻微', 50.50, 10.20, 800.00, '统计局'),
('310000', '上海市', '蔬菜', '白菜', 500.00, 800.00, 1.6000, 2000.00, 1600000.00, 2023, '冬', '多云', '无', 30.00, 5.50, 450.00, '农业部'),
('440000', '广东省', '水果', '苹果', 2000.00, 3000.00, 1.5000, 5000.00, 15000000.00, 2023, '夏', '晴朗', '轻微', 100.00, 20.00, 1800.00, '林业局'),
('130000', '河北省', '粮食作物', '玉米', 2500.00, 1500.00, 0.6000, 2500.00, 3750000.00, 2023, '夏', '晴朗', '轻微', 75.00, 15.00, 2000.00, '统计局'),
('230000', '黑龙江省', '粮食作物', '水稻', 3000.00, 1800.00, 0.6000, 3200.00, 5760000.00, 2023, '秋', '多云', '轻微', 90.00, 18.00, 2800.00, '农业部'),
('110000', '北京市', '粮食作物', '小麦', 1100.60, 550.30, 0.4998, 3100.00, 1705930.00, 2022, '秋', '晴朗', '轻微', 55.03, 11.01, 880.48, '统计局'),
('310000', '上海市', '蔬菜', '白菜', 520.00, 832.00, 1.6000, 2050.00, 1705600.00, 2022, '冬', '多云', '无', 31.20, 5.72, 468.00, '农业部'),
('440000', '广东省', '水果', '苹果', 2100.00, 3150.00, 1.5000, 5100.00, 16065000.00, 2022, '夏', '晴朗', '轻微', 105.00, 21.00, 1890.00, '林业局');

-- 6. 插入预测模型数据
INSERT INTO prediction_models (name, type, description, is_active) VALUES 
('线性回归模型', 'LINEAR_REGRESSION', '基于历史数据的线性回归预测模型', TRUE),
('支持向量机', 'SVM', '支持向量机回归预测模型', TRUE),
('随机森林', 'RANDOM_FOREST', '随机森林回归预测模型', TRUE);

-- 7. 插入测试预测结果
INSERT INTO prediction_results (
    model_id, crop_type, region, year, predicted_yield, confidence_interval, accuracy_score
) VALUES 
(1, '小麦', '北京市', 2024, 550.75, '±25.5', 0.85),
(1, '水稻', '上海市', 2024, 825.30, '±30.2', 0.78),
(2, '玉米', '河北省', 2024, 1650.20, '±50.8', 0.92);
