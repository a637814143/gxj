-- 更新 prediction_results 表结构以匹配实体类
-- 删除旧表并重新创建

DROP TABLE IF EXISTS prediction_results;

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


