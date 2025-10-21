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
INSERT INTO base_crop (id, code, name, category, description)
VALUES
    (1, 'WHEAT', '小麦', '粮食作物', '冬小麦是预测模型的主要示例作物。'),
    (2, 'CORN', '玉米', '粮食作物', '夏玉米用于演示多作物对比场景。')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
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
INSERT INTO sys_user (id, username, password, full_name, email)
VALUES (1, 'admin', 'Admin@123', '系统管理员', 'admin@example.com')
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

-- 报告中心示例数据：初始化数据集及历史记录
INSERT INTO dataset_file (id, name, type, storage_path, description)
VALUES
    (1, '云南小麦历年产量', 'YIELD', '/data/demo/yield-yunnan-wheat.csv', '报告中心示例数据：2019-2023 年云南小麦产量'),
    (2, '云南小麦月度价格', 'PRICE', '/data/demo/price-yunnan-wheat.csv', '报告中心示例数据：近两年市场价格')
ON DUPLICATE KEY UPDATE
    storage_path = VALUES(storage_path),
    description = VALUES(description);

INSERT INTO dataset_yield_record (id, crop_id, region_id, dataset_file_id, year, sown_area, production, yield_per_hectare, average_price, data_source, collected_at)
VALUES
    (1, 1, 2, 1, 2019, 118.2, 410.5, 3.47, 2280, '云南省统计局', '2020-01-15'),
    (2, 1, 2, 1, 2020, 119.6, 415.8, 3.48, 2310, '云南省统计局', '2021-01-16'),
    (3, 1, 2, 1, 2021, 120.1, 420.3, 3.50, 2355, '云南省统计局', '2022-01-18'),
    (4, 1, 2, 1, 2022, 121.0, 428.9, 3.55, 2390, '云南省统计局', '2023-01-20'),
    (5, 1, 2, 1, 2023, 122.4, 439.6, 3.59, 2425, '云南省统计局', '2024-01-19')
ON DUPLICATE KEY UPDATE
    sown_area = VALUES(sown_area),
    production = VALUES(production),
    yield_per_hectare = VALUES(yield_per_hectare),
    average_price = VALUES(average_price),
    data_source = VALUES(data_source),
    collected_at = VALUES(collected_at);

INSERT INTO dataset_price_record (id, crop_id, region_id, dataset_file_id, record_date, price)
VALUES
    (1, 1, 2, 2, '2023-01-01', 2360),
    (2, 1, 2, 2, '2023-04-01', 2395),
    (3, 1, 2, 2, '2023-07-01', 2420),
    (4, 1, 2, 2, '2023-10-01', 2455),
    (5, 1, 2, 2, '2024-01-01', 2480),
    (6, 1, 2, 2, '2024-04-01', 2525)
ON DUPLICATE KEY UPDATE
    price = VALUES(price);

