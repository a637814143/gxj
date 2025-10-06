-- 将所有用户密码更新为明文 "123456"
-- 这样所有用户都可以用密码 123456 登录

UPDATE users SET password = '123456' WHERE username = 'admin';
UPDATE users SET password = '123456' WHERE username = 'researcher';
UPDATE users SET password = '123456' WHERE username = 'farmer';

-- 验证更新结果
SELECT username, password FROM users ORDER BY username;

