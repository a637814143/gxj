-- H2 权限与默认用户初始化脚本
-- 用于开发环境手动初始化，避免在应用启动时自动执行。

DELETE FROM sys_role_permission;
DELETE FROM sys_user_role;
DELETE FROM sys_user;
DELETE FROM sys_role;
DELETE FROM sys_permission;

INSERT INTO sys_permission (code, name)
VALUES ('DATA_VIEW', '数据查看'),
       ('DATA_MANAGE', '数据管理'),
       ('USER_MANAGE', '用户管理');

INSERT INTO sys_role (code, name)
VALUES ('ADMIN', '系统管理员'),
       ('ANALYST', '数据分析员');

-- 为角色分配基础权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         JOIN sys_permission p ON
             (r.code = 'ADMIN' AND p.code IN ('DATA_VIEW', 'DATA_MANAGE', 'USER_MANAGE'))
             OR (r.code = 'ANALYST' AND p.code = 'DATA_VIEW');

INSERT INTO sys_user (username, password, full_name, email)
VALUES ('admin', '{noop}admin123', '管理员', 'admin@example.com');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
         JOIN sys_role r ON r.code = 'ADMIN'
WHERE u.username = 'admin';
