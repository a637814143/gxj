-- 更新 crop_data 表结构以匹配实体类
-- 删除旧表并重新创建

DROP TABLE IF EXISTS crop_data;

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


