-- 基础行政区域和农作物信息由用户自行添加

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

-- 初始化管理员账户（密码在应用启动时自动加密）
INSERT INTO sys_user (id, username, password, full_name, email, department_code)
VALUES (1, 'admin', 'Admin@123', '系统管理员', 'admin@example.com', NULL)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    email = VALUES(email);

-- 绑定管理员角色
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.code = 'ADMIN'
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
    created_at = VALUES(created_at);