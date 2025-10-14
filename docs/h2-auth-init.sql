-- H2 数据库环境下的系统权限初始化脚本
DELETE FROM sys_role_permission;
DELETE FROM sys_user_role;
DELETE FROM sys_user;
DELETE FROM sys_role;
DELETE FROM sys_permission;

INSERT INTO sys_permission (code, name) VALUES
    ('DASHBOARD_VIEW', '仪表盘查看'),
    ('DATA_MANAGE', '数据资源管理'),
    ('FORECAST_RUN', '预测任务执行');

INSERT INTO sys_role (code, name) VALUES
    ('ADMIN', '系统管理员'),
    ('ANALYST', '农业分析员');

INSERT INTO sys_user (username, password, full_name, email) VALUES
    ('admin', '{noop}admin123', '系统管理员', 'admin@example.com'),
    ('analyst', '{noop}analyst123', '农业分析员', 'analyst@example.com');

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
    ((SELECT id FROM sys_role WHERE code = 'ADMIN'), (SELECT id FROM sys_permission WHERE code = 'DASHBOARD_VIEW')),
    ((SELECT id FROM sys_role WHERE code = 'ADMIN'), (SELECT id FROM sys_permission WHERE code = 'DATA_MANAGE')),
    ((SELECT id FROM sys_role WHERE code = 'ADMIN'), (SELECT id FROM sys_permission WHERE code = 'FORECAST_RUN')),
    ((SELECT id FROM sys_role WHERE code = 'ANALYST'), (SELECT id FROM sys_permission WHERE code = 'DASHBOARD_VIEW')),
    ((SELECT id FROM sys_role WHERE code = 'ANALYST'), (SELECT id FROM sys_permission WHERE code = 'FORECAST_RUN'));

INSERT INTO sys_user_role (user_id, role_id) VALUES
    ((SELECT id FROM sys_user WHERE username = 'admin'), (SELECT id FROM sys_role WHERE code = 'ADMIN')),
    ((SELECT id FROM sys_user WHERE username = 'analyst'), (SELECT id FROM sys_role WHERE code = 'ANALYST'));
