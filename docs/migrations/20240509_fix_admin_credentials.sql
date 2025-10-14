-- 补丁脚本：重新为管理员账号加密密码并补齐角色绑定
-- 运行方式：在目标数据库上执行本脚本即可

-- 1. 确保角色存在
INSERT INTO sys_role (code, name, description)
VALUES
    ('ADMIN', '系统管理员', '拥有系统全部权限'),
    ('AGRICULTURE_DEPT', '农业部门用户', '可访问数据中心、高级预测与报告导出'),
    ('FARMER', '企业/农户用户', '可查看基础数据、基础预测与推荐方案')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- 2. 更新管理员密码为 BCrypt 密文（明文：Admin@123）
UPDATE sys_user
SET password = '$2b$12$85erHaIH0YDAkpgK0xcyEObkFZP9p0D1nhRgYxQSDIZW0Y8.DGMI2'
WHERE username = 'admin';

-- 3. 重新关联管理员角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'ADMIN'
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
