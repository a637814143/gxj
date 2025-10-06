-- 更新admin用户密码为正确的BCrypt哈希
-- 这个哈希对应密码 "123456"

UPDATE users SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi' WHERE username = 'admin';

-- 验证更新结果
SELECT username, password FROM users WHERE username = 'admin';

