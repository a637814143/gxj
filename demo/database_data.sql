-- 初始化数据
-- 注意：请在创建表结构后执行此脚本

-- 1. 初始化角色数据
INSERT INTO roles (name, display_name, description) VALUES 
('ADMIN', '管理员', '系统管理员，拥有全部权限'),
('RESEARCHER', '研究员', '农业科研人员，可以进行数据分析和预测'),
('FARMER', '农户', '农户用户，可以查看预测结果和种植建议');

-- 2. 初始化权限数据
INSERT INTO permissions (name, display_name, description, resource, action) VALUES 
('user:create', '创建用户', '创建新用户', 'user', 'create'),
('user:read', '查看用户', '查看用户信息', 'user', 'read'),
('user:update', '更新用户', '更新用户信息', 'user', 'update'),
('user:delete', '删除用户', '删除用户', 'user', 'delete'),
('data:import', '导入数据', '导入作物数据', 'data', 'import'),
('data:export', '导出数据', '导出数据', 'data', 'export'),
('data:read', '查看数据', '查看作物数据', 'data', 'read'),
('prediction:run', '运行预测', '运行预测模型', 'prediction', 'run'),
('report:generate', '生成报告', '生成分析报告', 'report', 'generate'),
('system:config', '系统配置', '配置系统参数', 'system', 'config');

-- 3. 为管理员角色分配所有权限
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN';

-- 4. 为研究员角色分配相关权限
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'RESEARCHER' AND p.name IN ('data:import', 'data:export', 'data:read', 'prediction:run', 'report:generate');

-- 5. 为农户角色分配查看权限
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'FARMER' AND p.name = 'data:read';

-- 6. 创建默认管理员用户 (密码: admin123)
INSERT INTO users (username, password, email, real_name, organization, region_code, status) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'admin@example.com', '系统管理员', '农业部门', 'BJ', 'ACTIVE');

-- 7. 为管理员用户分配管理员角色
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ADMIN';

-- 8. 创建默认研究员用户 (密码: researcher123)
INSERT INTO users (username, password, email, real_name, organization, region_code, status) VALUES 
('researcher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'researcher@example.com', '研究员', '农业科研所', 'SH', 'ACTIVE');

-- 9. 为研究员用户分配研究员角色
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'researcher' AND r.name = 'RESEARCHER';

-- 10. 创建默认农户用户 (密码: farmer123)
INSERT INTO users (username, password, email, real_name, organization, region_code, status) VALUES 
('farmer', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'farmer@example.com', '农户', '农业合作社', 'GZ', 'ACTIVE');

-- 11. 为农户用户分配农户角色
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'farmer' AND r.name = 'FARMER';

-- 12. 初始化预测模型数据
INSERT INTO prediction_models (name, type, description, is_active) VALUES 
('ARIMA模型', 'ARIMA', '自回归积分滑动平均模型，适用于时间序列预测', TRUE),
('Prophet模型', 'PROPHET', 'Facebook开发的预测工具，适用于具有明显季节性特征的数据', TRUE),
('XGBoost模型', 'XGBOOST', '梯度提升决策树模型，适用于结构化数据预测', TRUE),
('LSTM模型', 'LSTM', '长短期记忆网络，适用于复杂时间序列预测', TRUE);

-- 13. 初始化地区数据
INSERT INTO regions (code, name, parent_code, level) VALUES 
('BJ', '北京市', NULL, 1),
('SH', '上海市', NULL, 1),
('GZ', '广州市', NULL, 1),
('SZ', '深圳市', NULL, 1),
('CD', '成都市', NULL, 1),
('WH', '武汉市', NULL, 1),
('NJ', '南京市', NULL, 1),
('XA', '西安市', NULL, 1);

-- 14. 初始化数据源
INSERT INTO data_sources (name, type, description, is_active) VALUES 
('国家统计局', 'API', '国家统计局农业数据接口', TRUE),
('农业部数据', 'FILE', '农业部提供的农业数据文件', TRUE),
('气象局数据', 'API', '气象局天气数据接口', TRUE),
('地方农业部门', 'DATABASE', '地方农业部门数据库', TRUE);


