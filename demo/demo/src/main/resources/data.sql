-- 初始化基础行政区域信息
INSERT INTO base_region (id, code, name, level, parent_code, parent_name, description)
VALUES
    (1, 'YUNNAN', '云南省', 'PROVINCE', NULL, NULL, '云南省省级行政区。'),
    (2, 'KUNMING', '昆明市', 'PREFECTURE', 'YUNNAN', '云南省', '云南省省会城市。')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    level = VALUES(level),
    parent_code = VALUES(parent_code),
    parent_name = VALUES(parent_name),
    description = VALUES(description);

-- 初始化基础农作物信息
INSERT INTO base_crop (id, code, name, category, harvest_season, description)
VALUES
    (1, 'WHEAT', '小麦', '粮食作物', 'SUMMER_GRAIN', '冬小麦是预测模型的主要示例作物。'),
    (2, 'CORN', '玉米', '粮食作物', 'AUTUMN_GRAIN', '夏玉米用于演示多作物对比场景。')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    harvest_season = VALUES(harvest_season),
    description = VALUES(description);

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

-- 初始化示例农业部门与农户账号
INSERT INTO sys_user (id, username, password, full_name, email, department_code)
VALUES
    (2, 'agri', 'Agri@123', '农业技术员', 'agri@example.com', 'PLANT_PROTECTION'),
    (3, 'farmer', 'Farmer@123', '示例农户', 'farmer@example.com', NULL)
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

-- 绑定农业部门与农户角色
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.code = 'AGRICULTURE_DEPT'
WHERE u.username = 'agri'
ON DUPLICATE KEY UPDATE
    created_at = VALUES(created_at);

INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.code = 'FARMER'
WHERE u.username = 'farmer'
ON DUPLICATE KEY UPDATE
    created_at = VALUES(created_at);

-- 报告中心示例数据：初始化数据集及历史记录
INSERT INTO dataset_file (id, name, type, storage_path, description)
VALUES
    (1, '云南小麦历年产量', 'YIELD', '/data/demo/yield-yunnan-wheat.csv', '报告中心示例数据：2019-2023 年云南小麦产量'),
    (2, '昆明逐日气象监测', 'WEATHER', '/data/demo/weather-kunming.csv', '示例气象数据：昆明地区逐日天气统计')
ON DUPLICATE KEY UPDATE
    storage_path = VALUES(storage_path),
    description = VALUES(description);

INSERT INTO dataset_yield_record (id, crop_id, region_id, dataset_file_id, year, sown_area, production, yield_per_hectare, data_source, collected_at)
VALUES
    (1, 1, 2, 1, 2019, 118.2, 410.5, 3.47, '云南省统计局', '2020-01-15'),
    (2, 1, 2, 1, 2020, 119.6, 415.8, 3.48, '云南省统计局', '2021-01-16'),
    (3, 1, 2, 1, 2021, 120.1, 420.3, 3.50, '云南省统计局', '2022-01-18'),
    (4, 1, 2, 1, 2022, 121.0, 428.9, 3.55, '云南省统计局', '2023-01-20'),
    (5, 1, 2, 1, 2023, 122.4, 439.6, 3.59, '云南省统计局', '2024-01-19')
ON DUPLICATE KEY UPDATE
    sown_area = VALUES(sown_area),
    production = VALUES(production),
    yield_per_hectare = VALUES(yield_per_hectare),
    data_source = VALUES(data_source),
    collected_at = VALUES(collected_at);

INSERT INTO dataset_weather_record (id, region_id, dataset_file_id, record_date, max_temperature, min_temperature, weather_text, wind, sunshine_hours, data_source)
VALUES
    (1, 2, 2, '2009-01-01', 10.4, 5.6, '阴', '南风3级', 0.1, '示例数据'),
    (2, 2, 2, '2009-01-02', 12.1, 6.0, '多云', '北风2级', 0.0, '示例数据'),
    (3, 2, 2, '2009-01-03', 11.5, 4.8, '小雨', '东北风3级', 0.5, '示例数据'),
    (4, 2, 2, '2009-01-04', 9.6, 3.9, '阴', '东风2级', 0.2, '示例数据'),
    (5, 2, 2, '2009-01-05', 8.8, 2.5, '多云', '西南风2级', 1.4, '示例数据')
ON DUPLICATE KEY UPDATE
    max_temperature = VALUES(max_temperature),
    min_temperature = VALUES(min_temperature),
    weather_text = VALUES(weather_text),
    wind = VALUES(wind),
    sunshine_hours = VALUES(sunshine_hours),
    data_source = VALUES(data_source);

-- 初始化示例在线咨询数据
INSERT INTO consultation (id, subject, crop_type, description, status, priority, department_code, created_by, assigned_to, last_message_at, created_at, updated_at)
VALUES
    (1, '水稻病害咨询', '水稻', '稻叶出现褐色斑点，请协助诊断。', 'processing', 'high', 'PLANT_PROTECTION', 3, 2, DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 10 MINUTE))
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    priority = VALUES(priority),
    assigned_to = VALUES(assigned_to),
    last_message_at = VALUES(last_message_at);

INSERT INTO consultation_participant (id, consultation_id, user_id, role_code, is_owner, last_read_at, created_at, updated_at)
VALUES
    (1, 1, 3, 'FARMER', 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2, 1, 2, 'AGRICULTURE_DEPT', 0, DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 5 MINUTE))
ON DUPLICATE KEY UPDATE
    last_read_at = VALUES(last_read_at);

INSERT INTO consultation_message (id, consultation_id, sender_id, sender_role, content, created_at, updated_at)
VALUES
    (1, 1, 3, 'FARMER', '专家好，我的水稻叶片上出现褐色梭形斑点，并逐渐扩散。', DATE_SUB(NOW(), INTERVAL 23 MINUTE), DATE_SUB(NOW(), INTERVAL 23 MINUTE)),
    (2, 1, 2, 'AGRICULTURE_DEPT', '初步判断为稻瘟病，请在傍晚前喷施三环唑，并注意田间通风。', DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE))
ON DUPLICATE KEY UPDATE
    content = VALUES(content);

