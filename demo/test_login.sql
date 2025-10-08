-- 测试登录问题的SQL脚本
-- 1. 检查用户是否存在
SELECT id, username, email, status FROM users WHERE username = 'admin';

-- 2. 检查用户角色分配
SELECT u.username, r.name as role_name 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
JOIN roles r ON ur.role_id = r.id 
WHERE u.username = 'admin';

-- 3. 检查角色权限
SELECT r.name as role_name, p.name as permission_name
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE r.name = 'ADMIN';

-- 4. 如果用户不存在，重新创建
INSERT IGNORE INTO users (username, password, email, real_name, organization, region_code, status) VALUES
('admin', '123456', 'admin@example.com', '系统管理员', '农业部门', 'BJ', 'ACTIVE');

-- 5. 重新分配角色
INSERT IGNORE INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ADMIN';

