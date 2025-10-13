-- Reset existing data to avoid duplication when the application restarts
DELETE FROM sys_role_permission;
DELETE FROM sys_user_role;
DELETE FROM sys_permission;
DELETE FROM sys_role;
DELETE FROM sys_user;
DELETE FROM dataset_yield_record;
DELETE FROM dataset_price_record;
DELETE FROM dataset_file;
DELETE FROM base_crop;
DELETE FROM base_region;

-- --- 权限与角色初始化 ---
INSERT INTO sys_permission (code, name, created_at, updated_at) VALUES
    ('DASHBOARD_VIEW', '查看仪表盘', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DATA_MANAGE', '数据管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FORECAST_MANAGE', '预测任务管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_role (code, name, created_at, updated_at) VALUES
    ('ADMIN', '系统管理员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ANALYST', '业务分析员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('VIEWER', '决策查看者', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 角色权限关联
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE code = 'ADMIN'), id FROM sys_permission;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE code = 'ANALYST'), id FROM sys_permission WHERE code IN ('DASHBOARD_VIEW', 'DATA_MANAGE');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE code = 'VIEWER'), id FROM sys_permission WHERE code = 'DASHBOARD_VIEW';

-- 系统用户
INSERT INTO sys_user (username, password, full_name, email, created_at, updated_at) VALUES
    ('admin', '$2a$10$wF2oTObgGJE0E7E5Wdl66uYcmGeXgnz9K/Y/xFdVtOfvtTDHkJ/xS', '平台管理员', 'admin@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('analyst', '$2a$10$wF2oTObgGJE0E7E5Wdl66uYcmGeXgnz9K/Y/xFdVtOfvtTDHkJ/xS', '数据分析员', 'analyst@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u CROSS JOIN sys_role r WHERE u.username = 'admin' AND r.code = 'ADMIN';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u CROSS JOIN sys_role r WHERE u.username = 'analyst' AND r.code = 'ANALYST';

-- --- 基础信息 ---
INSERT INTO base_crop (code, name, description, created_at, updated_at) VALUES
    ('RICE', '水稻', '云南主粮之一，集中分布在滇中和滇南稻区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CORN', '玉米', '适宜山地丘陵地区，兼顾粮饲双用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('POTATO', '马铃薯', '高海拔地区重要的口粮与商品作物', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('RAPESEED', '油菜', '滇中、滇东高原为主要产区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TEA', '茶叶', '普洱、临沧、保山等地重要优势产业', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SOYBEAN', '大豆', '滇东北、滇西北地区特色粮油作物', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO base_region (code, name, description, created_at, updated_at) VALUES
    ('YN', '云南省', '省级统计总览', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('YN-KM', '昆明市', '滇中城市群核心城市', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('YN-QJ', '曲靖市', '滇东北粮食主产区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('YN-HH', '红河州', '滇南山区粮经复合种植区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('YN-DL', '大理州', '滇西北高原生态农业区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --- 产量与价格示例数据 ---
INSERT INTO dataset_yield_record (crop_id, region_id, year, yield_per_hectare, created_at, updated_at) VALUES
    ((SELECT id FROM base_crop WHERE code = 'RICE'), (SELECT id FROM base_region WHERE code = 'YN'), 2020, 5.75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'RICE'), (SELECT id FROM base_region WHERE code = 'YN'), 2021, 5.74, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'RICE'), (SELECT id FROM base_region WHERE code = 'YN'), 2022, 5.75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'CORN'), (SELECT id FROM base_region WHERE code = 'YN-QJ'), 2021, 5.64, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'CORN'), (SELECT id FROM base_region WHERE code = 'YN-QJ'), 2022, 5.69, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'CORN'), (SELECT id FROM base_region WHERE code = 'YN-QJ'), 2023, 5.74, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'POTATO'), (SELECT id FROM base_region WHERE code = 'YN-DL'), 2021, 1.96, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'POTATO'), (SELECT id FROM base_region WHERE code = 'YN-DL'), 2022, 1.96, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'RAPESEED'), (SELECT id FROM base_region WHERE code = 'YN-KM'), 2021, 2.10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'RAPESEED'), (SELECT id FROM base_region WHERE code = 'YN-KM'), 2022, 2.12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'TEA'), (SELECT id FROM base_region WHERE code = 'YN-HH'), 2021, 0.56, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'TEA'), (SELECT id FROM base_region WHERE code = 'YN-HH'), 2022, 0.56, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'SOYBEAN'), (SELECT id FROM base_region WHERE code = 'YN-QJ'), 2022, 1.85, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'SOYBEAN'), (SELECT id FROM base_region WHERE code = 'YN-QJ'), 2023, 1.87, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO dataset_price_record (crop_id, region_id, record_date, price, created_at, updated_at) VALUES
    ((SELECT id FROM base_crop WHERE code = 'RAPESEED'), (SELECT id FROM base_region WHERE code = 'YN-KM'), DATE '2022-06-30', 5.62, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'TEA'), (SELECT id FROM base_region WHERE code = 'YN-HH'), DATE '2022-09-30', 19.20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'SOYBEAN'), (SELECT id FROM base_region WHERE code = 'YN-QJ'), DATE '2023-10-31', 4.58, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO dataset_file (name, type, storage_path, description, created_at, updated_at) VALUES
    ('云南省主要农作物产量年鉴', 'YIELD', '/data/yield/yn-annual.csv', '来自云南省统计局的年度产量汇总', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('重点作物市场价格监测', 'PRICE', '/data/price/key-crops.xlsx', '州市农业农村局市场价格监测数据', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
