DELETE FROM sys_role_permission;
DELETE FROM sys_user_role;
DELETE FROM sys_user;
DELETE FROM sys_role;
DELETE FROM sys_permission;

INSERT INTO sys_permission (code, name)
VALUES ('DATA_VIEW', '数据查看'),
       ('DATA_MANAGE', '数据管理'),
       ('USER_MANAGE', '用户管理');

INSERT INTO sys_role (code, name)
VALUES ('ADMIN', '系统管理员'),
       ('ANALYST', '数据分析员');

-- 为角色分配基础权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         JOIN sys_permission p ON
             (r.code = 'ADMIN' AND p.code IN ('DATA_VIEW', 'DATA_MANAGE', 'USER_MANAGE'))
             OR (r.code = 'ANALYST' AND p.code = 'DATA_VIEW');

INSERT INTO sys_user (username, password, full_name, email)
VALUES ('admin', '{noop}admin123', '管理员', 'admin@example.com');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
         JOIN sys_role r ON r.code = 'ADMIN'
WHERE u.username = 'admin';

INSERT INTO crops (name, category, description) VALUES
    ('水稻', '粮食作物', '云南主粮之一，集中分布在滇中和滇南稻区'),
    ('玉米', '粮食作物', '适宜山地丘陵地区，兼顾粮饲双用'),
    ('马铃薯', '薯类作物', '高海拔地区重要的口粮与商品作物'),
    ('油菜', '油料作物', '滇中、滇东高原为主要产区'),
    ('茶叶', '经济作物', '普洱、临沧、保山等地重要优势产业'),
    ('大豆', '豆类作物', '滇东北、滇西北地区特色粮油作物');

INSERT INTO regions (name, level, parent_name, description) VALUES
    ('云南省', 'PROVINCE', NULL, '省级统计总览'),
    ('昆明市', 'PREFECTURE', '云南省', '滇中城市群核心城市'),
    ('曲靖市', 'PREFECTURE', '云南省', '滇东北粮食主产区'),
    ('红河州', 'PREFECTURE', '云南省', '滇南山区粮经复合种植区'),
    ('大理州', 'PREFECTURE', '云南省', '滇西北高原生态农业区');

INSERT INTO yield_records (crop_id, region_id, harvest_year, sown_area, production, yield_per_hectare, average_price, data_source, collected_at) VALUES
    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '云南省'), 2020, 1280.5, 7354.2, 5.75, 2.55, '云南省统计局年鉴', DATE '2020-12-31'),
    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '云南省'), 2021, 1294.3, 7428.1, 5.74, 2.62, '云南省统计局年鉴', DATE '2021-12-31'),
    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '云南省'), 2022, 1302.7, 7486.9, 5.75, 2.68, '云南省统计局年鉴', DATE '2022-12-31'),
    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '曲靖市'), 2021, 412.5, 2326.4, 5.64, 2.15, '州市农业农村局统计', DATE '2021-11-30'),
    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '曲靖市'), 2022, 418.9, 2385.1, 5.69, 2.22, '州市农业农村局统计', DATE '2022-11-30'),
    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '曲靖市'), 2023, 425.4, 2441.6, 5.74, 2.35, '州市农业农村局统计', DATE '2023-11-30'),
    ((SELECT id FROM crops WHERE name = '马铃薯'), (SELECT id FROM regions WHERE name = '大理州'), 2021, 156.8, 306.5, 1.96, 3.25, '州市农业农村局统计', DATE '2021-10-31'),
    ((SELECT id FROM crops WHERE name = '马铃薯'), (SELECT id FROM regions WHERE name = '大理州'), 2022, 160.4, 314.2, 1.96, 3.38, '州市农业农村局统计', DATE '2022-10-31'),
    ((SELECT id FROM crops WHERE name = '油菜'), (SELECT id FROM regions WHERE name = '昆明市'), 2021, 98.6, 207.3, 2.10, 5.45, '州市农业农村局统计', DATE '2021-06-30'),
    ((SELECT id FROM crops WHERE name = '油菜'), (SELECT id FROM regions WHERE name = '昆明市'), 2022, 101.2, 213.9, 2.12, 5.62, '州市农业农村局统计', DATE '2022-06-30'),
    ((SELECT id FROM crops WHERE name = '茶叶'), (SELECT id FROM regions WHERE name = '红河州'), 2021, 312.5, 174.8, 0.56, 18.50, '云南省农业农村厅茶产业监测', DATE '2021-09-30'),
    ((SELECT id FROM crops WHERE name = '茶叶'), (SELECT id FROM regions WHERE name = '红河州'), 2022, 318.7, 179.6, 0.56, 19.20, '云南省农业农村厅茶产业监测', DATE '2022-09-30'),
    ((SELECT id FROM crops WHERE name = '大豆'), (SELECT id FROM regions WHERE name = '曲靖市'), 2022, 65.3, 120.8, 1.85, 4.45, '农业农村部大豆振兴计划', DATE '2022-10-31'),
    ((SELECT id FROM crops WHERE name = '大豆'), (SELECT id FROM regions WHERE name = '曲靖市'), 2023, 69.1, 129.4, 1.87, 4.58, '农业农村部大豆振兴计划', DATE '2023-10-31');
