-- 修复 crop_data 表的字段映射问题
-- 检查现有表并进行字段重命名以匹配实体类

-- 检查并重命名字段（如果字段名不匹配的话）
-- 根据 CropData.java 实体类，我们需要确保字段名正确

-- 如果表中的字段名和实体不一致，我们需要重命名
-- 先检查当前的表结构
DESCRIBE crop_data;

-- 重命名字段以匹配实体类（如果需要的话）
-- 注意：MySQL需要用 ALTER TABLE 来重命名字段

-- 如果 yield 字段名不匹配，重命名为 yield_amount（因为MySQL有yield是关键字）
ALTER TABLE crop_data CHANGE COLUMN yield yield_amount DECIMAL(15,2) COMMENT '产量（吨）';

-- 确保其他字段都存在
ALTER TABLE crop_data 
ADD COLUMN IF NOT EXISTS crop_name VARCHAR(100) COMMENT '作物名称' AFTER crop_type,
ADD COLUMN IF NOT EXISTS unit_yield DECIMAL(10,4) COMMENT '单产（吨/亩）' AFTER yield_amount,
ADD COLUMN IF NOT EXISTS season VARCHAR(20) COMMENT '季节' AFTER year,
ADD COLUMN IF NOT EXISTS weather_condition VARCHAR(100) COMMENT '天气条件' AFTER season,
ADD COLUMN IF NOT EXISTS pest_disease_level VARCHAR(50) COMMENT '病虫害程度' AFTER weather_condition,
ADD COLUMN IF NOT EXISTS fertilizer_usage DECIMAL(10,2) COMMENT '化肥使用量（吨）' AFTER pest_disease_level,
ADD COLUMN IF NOT EXISTS pesticide_usage DECIMAL(10,2) COMMENT '农药使用量（顿）' AFTER fertilizer_usage,
ADD COLUMN IF NOT EXISTS irrigation_area DECIMAL(15,2) COMMENT '灌溉面积（亩）' AFTER pesticide_usage,
ADD COLUMN IF NOT EXISTS data_source VARCHAR(200) COMMENT '数据来源' AFTER irrigation_area;

-- 更新现有数据，移除不必要的字段（如果存在）
-- 假设原始表可能有不同的字段名

-- 插入/更新测试数据，确保数据格式正确
INSERT INTO crop_data (
    region_code, region_name, crop_type, crop_name, planting_area, yield_amount, 
    unit_yield, price, total_value, year, season, weather_condition, 
    pest_disease_level, fertilizer_usage, pesticide_usage, irrigation_area, data_source
) VALUES 
('110000', '北京市', '粮食作物', '小麦', 1000.50, 500.25, 0.5000, 3000.00, 1500750.00, 2023, '秋', '晴朗', '轻微', 50.50, 10.20, 800.00, '统计局')
ON DUPLICATE KEY UPDATE
    region_name = VALUES(region_name),
    crop_name = VALUES(crop_name),
    planting_area = VALUES(planting_area),
    yield_amount = VALUES(yield_amount),
    unit_yield = VALUES(unit_yield),
    price = VALUES(price),
    total_value = VALUES(total_value),
    season = VALUES(season),
    weather_condition = VALUES(weather_condition),
    pest_disease_level = VALUES(pest_disease_level),
    fertilizer_usage = VALUES(fertilizer_usage),
    knowledge_usage = VALUES(pesticide_usage),
    irrigation_area = VALUES(irrigation_area),
    data_source = VALUES(data_source);

-- 批量插入更多测试数据
INSERT IGNORE INTO crop_data (
    region_code, region_name, crop_type, crop_name, planting_area, yield_amount, 
    unit_yield, price, total_value, year, season, weather_condition, 
    pest_disease_level, fertilizer_usage, pesticide_usage, irrigation_area, data_source
) VALUES 
('310000', '上海市', '蔬菜', '白菜', 500.00, 800.00, 1.6000, 2000.00, 1600000.00, 2023, '冬', '多云', '无', 30.00, 5.50, 450.00, '农业部'),
('440000', '广东省', '水果', '苹果', 2000.00, 3000.00, 1.5000, 5000.00, 15000000.00, 2023, '夏', '晴朗', '轻微', 100.00, 20.00, 1800.00, '林业局'),
('130000', '河北省', '粮食作物', '玉米', 2500.00, 1500.00, 0.6000, 2500.00, 3750000.00, 2023, '夏', '晴朗', '轻微', 75.00, 15.00, 2000.00, '统计局'),
('230000', '黑龙江省', '粮食作物', '水稻', 3000.00, 1800.00, 0.6000, 3200.00, 5760000.00, 2023, '秋', '多云', '轻微', 90.00, 18.00, 2800.00, '农业部'),
('110000', '北京市', '粮食作物', '小麦', 1100.60, 550.30, 0.4998, 3100.00, 1705930.00, 2022, '秋', '晴朗', '轻微', 55.03, 11.01, 880.48, '统计局'),
('310000', '上海市', '蔬菜', '白菜', 520.00, 832.00, 1.6000, 2050.00, 1705600.00, 2022, '冬', '多云', '无', 31.20, 5.72, 468.00, '农业部'),
('440000', '广东省', '水果', '苹果', 2100.00, 3150.00, 1.5000, 5100.00, 16065000.00, 2022, '夏', '晴朗', '轻微', 105.00, 21.00, 1890.00, '林业局'),
('130000', '河北省', '粮食作物', '玉米', 2600.00, 1560.00, 0.6000, 2550.00, 3978000.00, 2022, '夏', '晴朗', '轻微', 78.00, 15.60, 2080.00, '统计局'),
('230000', '黑龙江省', '粮食作物', '水稻', 3100.00, 1860.00, 0.6000, 3250.00, 6045000.00, 2022, '秋', '多云', '轻微', 93.00, 18.60, 2883.00, '农业部');

