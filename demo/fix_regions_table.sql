-- 修复 regions 表结构
-- 检查并更新 regions 表

-- 先查看现有表结构，然后添加缺失字段
ALTER TABLE regions 
ADD COLUMN IF NOT EXISTS name VARCHAR(100) NOT NULL COMMENT '地区名称',
ADD COLUMN IF NOT EXISTS parent_code VARCHAR(20) COMMENT '父级地区代码',
ADD COLUMN IF NOT EXISTS level INT DEFAULT 1 COMMENT '级别',
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 如果表不存在，则创建整个表
CREATE TABLE IF NOT EXISTS regions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '地区代码',
    name VARCHAR(100) NOT NULL COMMENT '地区名称',
    parent_code VARCHAR(20) COMMENT 'pos父级地区代码',
    level INT DEFAULT 1 COMMENT '级别',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_code (parent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表';

-- 插入地区数据
INSERT IGNORE INTO regions (code, name, level) VALUES 
('110000', '北京市', 1),
('310000', '上海市', 1),
('440000', '广东省', 1),
('130000', '河北省', 1),
('230000', '黑龙江省', 1),
('510000', '四川省', 1),
('370000', '山东省', 1),
('430000', '湖南省', 1),
('320000', '江苏省', 1),
('330000', '浙江省', 1),
('410000', '河南省', 1),
('140000', '山西省', 1),
('210000', '辽宁省', 1),
('610000', '陕西省', 1),
('620000', '甘肃省', 1),
('630000', '青海省', 1),
('640000', '宁夏', 1),
('650000', '新疆', 1),
('540000', '西藏', 1),
('530000', '云南省', 1),
('520000', '贵州省', 1),
('460000', '海南省', 1),
('450000', '广西', 1),
('360000', '江西省', 1),
('350000', '福建省', 1),
('340000', '安徽省', 1),
('220000', '吉林省', 1),
('150000', '内蒙古', 1),
('120000', '天津市', 1),
('500000', '重庆市', 1);

