-- 更新 crop_data 表结构以匹配代码实体
-- 首先删除原有表（如果数据不重要）
DROP TABLE IF EXISTS crop_data;

-- 重新创建 crop_data 表，匹配 CropData 实体
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

-- 插入测试数据
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
('440000', '广东省', '水果', '苹果', 2100.00, 3150.00, 1.5000, 5100.00, 16065000.00, 2022, '夏', '晴朗', '轻微', 105.00, 21.00, 1890.00, '林业局'),
('130000', '河北省', '粮食作物', '玉米', 2600.00, 1560.00, 0.6000, 2550.00, 3978000.00, 2022, '夏', '晴朗', '轻微', 78.00, 15.60, 2080.00, '统计局'),
('230000', '黑龙江省', '粮食作物', '水稻', 3100.00, 1860.00, 0.6000, 3250.00, 6045000.00, 2022, '秋', '多云', '轻微', 93.00, 18.60, 2883.00, '农业部'),
('110000', '北京市', '粮食作物', '小麦', 1050.45, 525.23, 0.5003, 2950.00, 1549435.50, 2021, '秋', '晴朗', '轻微', 52.52, 10.50, 840.36, '统计局'),
('310000', '上海市', '蔬菜', '白菜', 490.00, 784.00, 1.6000, 1950.00, 1528800.00, 2021, '冬', '多云', '轻微', 29.40, 5.39, 441.00, '农业部'),
('440000', '广东省', '水果', '苹果', 1900.00, 2850.00, 1.5000, 4900.00, 13965000.00, 2021, '夏', '晴朗', '无', 95.00, 19.00, 1710.00, '林业局'),
('130000', '河北省', '粮食作物', '玉米', 2400.00, 1440.00, 0.6000, 2450.00, 3528000.00, 2021, '夏', '晴朗', '轻微', 72.00, 14.40, 1920.00, '统计局'),
('230000', '黑龙江省', '粮食作物', '水稻', 2900.00, 1740.00, 0.6000, 3150.00, 5481000.00, 2021, '秋', '多云', '轻微', 87.00, 17.40, 2685.00, '农业部');

-- 创建其他可能缺失的表结构，如果不存在的话

-- 确保用户角色表存在
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 确保角色权限表存在
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 确保数据源表存在（用于记录数据导入来源）
CREATE TABLE IF NOT EXISTS data_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    type VARCHAR(50) NOT NULL COMMENT '数据源类型',
    url VARCHAR(500) COMMENT '数据源URL',
    description TEXT COMMENT '描述',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源表';

-- 插入基础数据源
INSERT IGNORE INTO data_sources (name, type, description) VALUES 
('统计局', 'GOVERNMENT', '国家统计局官方数据'),
('农业部', 'GOVERNMENT', '农业部统计数据'),
('林业局', 'GOVERNMENT', '林业局统计数据'),
('文件导入', 'FILE_IMPORT', '用户上传的文件数据'),
('手工录入', 'MANUAL', '手工录入的数据');

-- 确保报告表存在（用于存储生成的报告）
CREATE TABLE IF NOT EXISTS reports (
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

