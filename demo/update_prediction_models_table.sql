-- 更新 prediction_models 表结构以匹配实体类
-- 删除旧表并重新创建

DROP TABLE IF EXISTS prediction_models;

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


