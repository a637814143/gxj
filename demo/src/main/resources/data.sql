-- 初始化基础区域信息
INSERT INTO region (id, name, parent_id, level, geo_code, latitude, longitude)
VALUES
    (1, '全国', NULL, 'COUNTRY', '000000', NULL, NULL),
    (2, '默认省份', 1, 'PROVINCE', '100000', 34.343000, 108.939000),
    (3, '示例市', 2, 'CITY', '100100', 34.267000, 108.939000)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    parent_id = VALUES(parent_id),
    level = VALUES(level),
    geo_code = VALUES(geo_code),
    latitude = VALUES(latitude),
    longitude = VALUES(longitude);

-- 初始化基础农作物信息
INSERT INTO crop (id, name, category, variety, unit, description)
VALUES
    (1, '小麦', '粮食作物', '冬小麦', '吨', '冬小麦是当前预测模型的主要示例作物。'),
    (2, '玉米', '粮食作物', '夏玉米', '吨', '夏玉米用于演示多地区预测场景。')
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    variety = VALUES(variety),
    unit = VALUES(unit),
    description = VALUES(description);

-- 初始化示例数据来源
INSERT INTO data_source (id, name, type, description, imported_by, import_time)
VALUES
    (1, '农业农村部统计年报', 'GOVERNMENT', '来自农业农村部的官方产量统计年报数据。', 'system', CURRENT_TIMESTAMP),
    (2, '国家气象中心', 'METEOROLOGICAL', '用于预测模型的关键气象指标数据。', 'system', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    description = VALUES(description),
    imported_by = VALUES(imported_by),
    import_time = VALUES(import_time);

-- 初始化系统权限
INSERT INTO sys_permission (code, name, description)
VALUES
    ('DASHBOARD_VIEW', '仪表盘查看', '允许访问平台仪表盘和统计总览。'),
    ('DATA_MANAGE', '数据管理', '允许管理基础数据和数据导入任务。'),
    ('FORECAST_MANAGE', '预测任务管理', '允许创建及管理预测任务。')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description);

-- 初始化系统角色
INSERT INTO sys_role (code, name, description)
VALUES
    ('ADMIN', '系统管理员', '拥有平台的全部功能权限。'),
    ('AGRICULTURE_DEPT', '农业部门用户', '负责维护农业基础数据与预测任务。'),
    ('FARMER', '种植户用户', '可以查看与使用预测结果。')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description);

-- 绑定管理员角色全部权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.code = 'ADMIN'
ON DUPLICATE KEY UPDATE
    created_at = VALUES(created_at);

-- 绑定农业部门角色权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.code IN ('DASHBOARD_VIEW', 'DATA_MANAGE', 'FORECAST_MANAGE')
WHERE r.code = 'AGRICULTURE_DEPT'
ON DUPLICATE KEY UPDATE
    created_at = VALUES(created_at);

-- 绑定农户角色权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.code = 'DASHBOARD_VIEW'
WHERE r.code = 'FARMER'
ON DUPLICATE KEY UPDATE
    created_at = VALUES(created_at);

-- 初始化管理员账户（密码在应用启动时将自动加密）
INSERT INTO sys_user (username, password, full_name, email, status)
VALUES ('admin', 'Admin@123', '系统管理员', 'admin@example.com', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    email = VALUES(email),
    status = VALUES(status);

-- 绑定管理员角色
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.code = 'ADMIN'
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
    created_at = VALUES(created_at);
