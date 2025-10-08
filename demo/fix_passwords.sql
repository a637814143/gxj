-- 修复用户密码，将其统一为明文 "123456"

-- 更新admin用户密码
UPDATE users SET password = '123456' WHERE username = 'admin';

-- 更新researcher用户密码
UPDATE users SET password = '123456' WHERE username = 'researcher';

-- 更新farmer用户密码
UPDATE users SET password = '123456' WHERE username = 'farmer';

-- 验证更新结果
SELECT username, password, email, status FROM users;

