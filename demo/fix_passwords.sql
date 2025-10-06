-- 修复用户密码，将明文密码更新为BCrypt加密格式
-- 密码 "123456" 的BCrypt加密结果

-- 更新admin用户密码
UPDATE users SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi' WHERE username = 'admin';

-- 更新researcher用户密码  
UPDATE users SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi' WHERE username = 'researcher';

-- 更新farmer用户密码
UPDATE users SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi' WHERE username = 'farmer';

-- 验证更新结果
SELECT username, password, email, status FROM users;

