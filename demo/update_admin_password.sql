-- 取消密码加密后，直接将密码更新为明文 "123456"

UPDATE users SET password = '123456' WHERE username = 'admin';

-- 验证更新结果
SELECT username, password FROM users WHERE username = 'admin';

