-- 插入基础角色数据
INSERT IGNORE INTO roles (name, display_name, description) VALUES 
('ADMIN', '系统管理员', '系统管理员，拥有所有权限'),
('RESEARCHER', '研究员', '数据分析员，可以查看和操作数据'),
('FARMER', '农民', '普通用户，可以查看基础数据');

-- 插入基础权限数据
INSERT IGNORE INTO permissions (name, display_name, description, resource, action) VALUES 
-- 用户管理权限
('user:create', '创建用户', '创建新用户', 'user', 'create'),
('user:read', '查看用户', '查看用户信息', 'user', 'read'),
('user:update', '更新用户', '更新用户信息', 'user', 'update'),
('user:delete', '删除用户', '删除用户', 'user', 'delete'),

-- 作物数据权限
('crop_data:create', '创建作物数据', '创建新的作物数据记录', 'crop_data', 'create'),
('crop_data:read', '查看作物数据', '查看作物数据', 'crop_data', 'read'),
('crop_data:update', '更新作物数据', '更新作物数据', 'crop_data', 'update'),
('crop_data:delete', '删除作物数据', '删除作物数据', 'crop_data', 'delete'),
('crop_data:import', '导入数据', '导入外部数据到系统', 'crop_data', 'import'),
('crop_data:export', '导出数据', '导出系统数据', 'crop_data', 'export'),

-- 预测模型权限
('prediction:create', '创建预测', '创建产量预测', 'prediction', 'create'),
('prediction:read', '查看预测', '查看预测结果', 'prediction', 'read'),
('prediction:update', '更新预测', '更新预测模型', 'prediction', 'update'),
('prediction:delete', '删除预测', '删除预测结果', 'prediction', 'delete'),

-- 报告权限
('report:create', '创建报告', '生成分析报告', 'report', 'create'),
('report:read', '查看报告', '查看报告内容', 'report', 'read'),
('report:export', '导出报告', '导出报告文件', 'report', 'export');

-- 为角色分配权限
-- 管理员拥有所有权限
INSERT IGNORE INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'ADMIN';

-- 研究员拥有数据相关权限
INSERT IGNORE INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'RESEARCHER' 
AND p.name IN (
    'crop_data:create', 'crop_data:read', 'crop_data:update', 'crop_data:import', 'crop_data:export',
    'prediction:create', 'prediction:read', 'prediction:update',
    'report:create', 'report:read', 'report:export',
    'user:read'
);

-- 农民只有查看权限
INSERT IGNORE INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'FARMER' 
AND p.name IN (
    'crop_data:read',
    'prediction:read',
    'report:read'
);

-- 使用明文密码，便于与取消加密后的登录逻辑保持一致
INSERT IGNORE INTO users (username, password, email, real_name, phone_number, organization, status) VALUES
('admin', 'admin123', 'admin@example.com', '系统管理员', '13800000000', '农业系统', 'ACTIVE');

-- 为管理员分配角色
INSERT IGNORE INTO user_roles (user_id, role_id) 
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.username = 'admin' AND r.name = 'ADMIN';

-- 插入一些预测模型数据
INSERT IGNORE INTO prediction_models (name, type, description, is_active) VALUES 
('线性回归模型', 'LINEAR_REGRESSION', '基于历史数据的线性回归预测模型', TRUE),
('支持向量机', 'SVM', '支持向量机回归预测模型', TRUE),
('随机森林', 'RANDOM_FOREST', '随机森林回归预测模型', TRUE),
('神经网络', 'NEURAL_NETWORK', '多层感知机神经网络模型', FALSE);

-- 插入一些地区数据
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
('330000', '浙江省', 1);

-- 插入一些测试预测结果
INSERT IGNORE INTO prediction_results (
    model_id, crop_type, region, year, predicted_yield, confidence_interval, accuracy_score
) VALUES 
(1, '小麦', '北京市', 2024, 550.75, '±25.5', 0.85),
(1, '水稻', '上海市', 2024, 825.30, '±30.2', 0.78),
(2, '玉米', '河北省', 2024, 1650.20, '±50.8', 0.92),
(3, '苹果', '广东省', 2024, 3250.00, '±120.5', 0.88);

-- 插入一些模型参数
INSERT IGNORE INTO model_parameters (model_id, parameter_name, parameter_value, parameter_type) VALUES 
(1, 'learning_rate', '0.01', 'DOUBLE'),
(1, 'max_iter', '1000', 'INTEGER'),
(1, 'regularization', '0.1', 'DOUBLE'),
(2, 'kernel', 'rbf', 'STRING'),
(2, 'gamma', 'auto', 'STRING'),
(2, 'C', '1.0', 'DOUBLE'),
(3, 'n_estimators', '100', 'INTEGER'),
(3, 'max_depth', '10', 'INTEGER'),
(3, 'random_state', '42', 'INTEGER');

