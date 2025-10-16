-- 初始化基础区域信息
INSERT INTO base_region (id, code, name, description)
VALUES
    (1, 'CN-NATION', '全国', '国家级行政区，用于聚合全国范围的数据。'),
    (2, 'CN-YN', '云南省', '系统默认示例省份，演示区域维度。'),
    (3, 'CN-YN-KM', '昆明市', '示例地市，演示城市级别的数据。')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description);

-- 初始化基础农作物信息
INSERT INTO base_crop (id, code, name, description)
VALUES
    (1, 'WHEAT', '小麦', '冬小麦是预测模型的主要示例作物。'),
    (2, 'CORN', '玉米', '夏玉米用于演示多作物对比场景。')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description);

-- 初始化示例数据文件登记
INSERT INTO dataset_file (id, name, type, storage_path, description)
VALUES
    (1, '2023年云南小麦产量.xlsx', 'YIELD', '/data/seed/yield-2023.xlsx', '历史产量明细，来源于农业农村厅年报。'),
    (2, '2023年云南玉米价格.csv', 'PRICE', '/data/seed/price-2023.csv', '批发市场均价数据，来源于省发改委。'),
    (3, '2023年昆明气象指标.xlsx', 'WEATHER', '/data/seed/weather-2023.xlsx', '昆明地区气象监测数据。')
ON DUPLICATE KEY UPDATE
    type = VALUES(type),
    storage_path = VALUES(storage_path),
    description = VALUES(description);

-- 初始化作物年均单产示例数据
INSERT INTO dataset_yield_record (id, crop_id, region_id, year, yield_per_hectare)
VALUES
    (1, 1, 2, 2021, 5.6),
    (2, 1, 2, 2022, 5.8),
    (3, 1, 2, 2023, 6.0),
    (4, 2, 2, 2023, 8.4),
    (5, 1, 3, 2023, 6.3)
ON DUPLICATE KEY UPDATE
    yield_per_hectare = VALUES(yield_per_hectare);

-- 初始化作物价格示例数据
INSERT INTO dataset_price_record (id, crop_id, region_id, record_date, price)
VALUES
    (1, 1, 2, '2023-05-01', 2360.00),
    (2, 1, 2, '2023-06-01', 2385.00),
    (3, 1, 2, '2023-07-01', 2402.00),
    (4, 2, 2, '2023-07-01', 2150.00),
    (5, 1, 3, '2023-07-01', 2450.00)
ON DUPLICATE KEY UPDATE
    price = VALUES(price);

-- 初始化预测模型配置
INSERT INTO forecast_model (id, name, type, description)
VALUES
    (1, 'LSTM作物产量模型', 'LSTM', '基于 10 年历史数据训练的 LSTM 年度产量预测模型。'),
    (2, 'Prophet季节性模型', 'PROPHET', '捕捉季节波动的 Prophet 模型，适用于价格预测。')
ON DUPLICATE KEY UPDATE
    type = VALUES(type),
    description = VALUES(description);

-- 初始化预测任务
INSERT INTO forecast_task (id, model_id, crop_id, region_id, status, parameters)
VALUES
    (1, 1, 1, 2, 'SUCCESS', '{"historyYears":5,"forecastPeriods":3}'),
    (2, 2, 1, 3, 'PENDING', '{"historyYears":3,"forecastPeriods":4}')
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    parameters = VALUES(parameters);

-- 初始化预测执行记录
INSERT INTO forecast_run (id, model_id, crop_id, region_id, status, forecast_periods, history_years, frequency, external_request_id, error_message, mae, rmse, mape, r2)
VALUES
    (1, 1, 1, 2, 'SUCCESS', 3, 5, 'YEAR', 'RUN-20231001-0001', NULL, 0.42, 0.68, 5.30, 0.91)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    forecast_periods = VALUES(forecast_periods),
    history_years = VALUES(history_years),
    frequency = VALUES(frequency),
    error_message = VALUES(error_message),
    mae = VALUES(mae),
    rmse = VALUES(rmse),
    mape = VALUES(mape),
    r2 = VALUES(r2);

-- 初始化预测执行序列数据
INSERT INTO forecast_run_series (id, run_id, period, value, lower_bound, upper_bound, historical)
VALUES
    (1, 1, '2021', 5.6, NULL, NULL, TRUE),
    (2, 1, '2022', 5.8, NULL, NULL, TRUE),
    (3, 1, '2023', 6.0, NULL, NULL, TRUE),
    (4, 1, '2024', 6.2, 5.8, 6.6, FALSE),
    (5, 1, '2025', 6.4, 5.9, 6.9, FALSE)
ON DUPLICATE KEY UPDATE
    value = VALUES(value),
    lower_bound = VALUES(lower_bound),
    upper_bound = VALUES(upper_bound),
    historical = VALUES(historical);

-- 初始化预测结果摘要
INSERT INTO forecast_result (id, task_id, target_year, predicted_yield, evaluation)
VALUES
    (1, 1, 2024, 6.2, '模型表现稳定，预测误差控制在 10% 以内。'),
    (2, 1, 2025, 6.4, '逐年增产趋势明显，需关注极端天气。')
ON DUPLICATE KEY UPDATE
    predicted_yield = VALUES(predicted_yield),
    evaluation = VALUES(evaluation);

-- 初始化预测报告摘要
INSERT INTO report_summary (id, title, description, forecast_result_id, insights)
VALUES
    (1, '2024 年小麦产量预测报告', '基于 LSTM 模型的年度预测结果概览。', 1, '建议提前储备灌溉水源，并关注 6-7 月的降雨波动。')
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    forecast_result_id = VALUES(forecast_result_id),
    insights = VALUES(insights);

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

-- 初始化系统操作日志示例
INSERT INTO sys_log (id, username, action, detail)
VALUES
    (1, 'admin', 'LOGIN', '管理员登录系统。'),
    (2, 'admin', 'VIEW_DATA_CENTER', '查看数据中心概览页面。')
ON DUPLICATE KEY UPDATE
    action = VALUES(action),
    detail = VALUES(detail);
