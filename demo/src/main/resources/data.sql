INSERT INTO crops (name, category, description) VALUES
    ('水稻', '粮食作物', '云南主粮之一，集中分布在滇中和滇南稻区'),
    ('玉米', '粮食作物', '适宜山地丘陵地区，兼顾粮饲双用'),
    ('马铃薯', '薯类作物', '高海拔地区重要的口粮与商品作物'),
    ('油菜', '油料作物', '滇中、滇东高原为主要产区'),
    ('茶叶', '经济作物', '普洱、临沧、保山等地重要优势产业'),
    ('大豆', '豆类作物', '滇东北、滇西北地区特色粮油作物'),
    ('小麦', '粮食作物', '滇北冷凉地区重要粮食补充'),
    ('烤烟', '经济作物', '曲靖、昭通等地优势产业');

INSERT INTO regions (name, level, parent_name, description) VALUES
    ('云南省', 'PROVINCE', NULL, '省级统计总览'),
    ('昆明市', 'PREFECTURE', '云南省', '滇中城市群核心城市'),
    ('曲靖市', 'PREFECTURE', '云南省', '滇东北粮食主产区'),
    ('红河州', 'PREFECTURE', '云南省', '滇南山区粮经复合种植区'),
    ('大理州', 'PREFECTURE', '云南省', '滇西北高原生态农业区'),
    ('玉溪市', 'PREFECTURE', '云南省', '滇中优质粮油和烤烟基地'),
    ('保山市', 'PREFECTURE', '云南省', '滇西咖啡、茶叶及特色农业走廊'),
    ('昭通市', 'PREFECTURE', '云南省', '滇东北高原粮经兼顾产区');

INSERT INTO yield_records (crop_id, region_id, harvest_year, sown_area, production, yield_per_hectare, average_price, data_source, collected_at) VALUES
    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '云南省'), 2019, 1265.4, 7280.1, 5.75, 2.48, '云南省统计局年鉴', DATE '2019-12-31'),
    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '云南省'), 2020, 1280.5, 7354.2, 5.75, 2.55, '云南省统计局年鉴', DATE '2020-12-31'),
    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '云南省'), 2021, 1294.3, 7428.1, 5.74, 2.62, '云南省统计局年鉴', DATE '2021-12-31'),
    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '云南省'), 2022, 1302.7, 7486.9, 5.75, 2.68, '云南省统计局年鉴', DATE '2022-12-31'),
    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '云南省'), 2023, 1310.8, 7558.6, 5.76, 2.72, '云南省统计局年鉴', DATE '2023-12-31'),

    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '曲靖市'), 2019, 405.2, 2295.3, 5.66, 2.05, '州市农业农村局统计', DATE '2019-11-30'),
    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '曲靖市'), 2020, 408.7, 2318.8, 5.68, 2.10, '州市农业农村局统计', DATE '2020-11-30'),
    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '曲靖市'), 2021, 412.5, 2326.4, 5.64, 2.15, '州市农业农村局统计', DATE '2021-11-30'),
    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '曲靖市'), 2022, 418.9, 2385.1, 5.69, 2.22, '州市农业农村局统计', DATE '2022-11-30'),
    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '曲靖市'), 2023, 425.4, 2441.6, 5.74, 2.35, '州市农业农村局统计', DATE '2023-11-30'),

    ((SELECT id FROM crops WHERE name = '马铃薯'), (SELECT id FROM regions WHERE name = '大理州'), 2019, 152.4, 298.6, 1.96, 3.10, '州市农业农村局统计', DATE '2019-10-31'),
    ((SELECT id FROM crops WHERE name = '马铃薯'), (SELECT id FROM regions WHERE name = '大理州'), 2020, 154.9, 301.2, 1.94, 3.18, '州市农业农村局统计', DATE '2020-10-31'),
    ((SELECT id FROM crops WHERE name = '马铃薯'), (SELECT id FROM regions WHERE name = '大理州'), 2021, 156.8, 306.5, 1.96, 3.25, '州市农业农村局统计', DATE '2021-10-31'),
    ((SELECT id FROM crops WHERE name = '马铃薯'), (SELECT id FROM regions WHERE name = '大理州'), 2022, 160.4, 314.2, 1.96, 3.38, '州市农业农村局统计', DATE '2022-10-31'),
    ((SELECT id FROM crops WHERE name = '马铃薯'), (SELECT id FROM regions WHERE name = '大理州'), 2023, 164.2, 321.5, 1.96, 3.46, '州市农业农村局统计', DATE '2023-10-31'),

    ((SELECT id FROM crops WHERE name = '油菜'), (SELECT id FROM regions WHERE name = '昆明市'), 2019, 95.1, 199.4, 2.10, 5.20, '州市农业农村局统计', DATE '2019-06-30'),
    ((SELECT id FROM crops WHERE name = '油菜'), (SELECT id FROM regions WHERE name = '昆明市'), 2020, 96.8, 203.8, 2.11, 5.32, '州市农业农村局统计', DATE '2020-06-30'),
    ((SELECT id FROM crops WHERE name = '油菜'), (SELECT id FROM regions WHERE name = '昆明市'), 2021, 98.6, 207.3, 2.10, 5.45, '州市农业农村局统计', DATE '2021-06-30'),
    ((SELECT id FROM crops WHERE name = '油菜'), (SELECT id FROM regions WHERE name = '昆明市'), 2022, 101.2, 213.9, 2.12, 5.62, '州市农业农村局统计', DATE '2022-06-30'),
    ((SELECT id FROM crops WHERE name = '油菜'), (SELECT id FROM regions WHERE name = '昆明市'), 2023, 103.7, 219.6, 2.12, 5.74, '州市农业农村局统计', DATE '2023-06-30'),

    ((SELECT id FROM crops WHERE name = '茶叶'), (SELECT id FROM regions WHERE name = '红河州'), 2019, 305.1, 170.2, 0.56, 18.10, '云南省农业农村厅茶产业监测', DATE '2019-09-30'),
    ((SELECT id FROM crops WHERE name = '茶叶'), (SELECT id FROM regions WHERE name = '红河州'), 2020, 309.8, 172.4, 0.56, 18.35, '云南省农业农村厅茶产业监测', DATE '2020-09-30'),
    ((SELECT id FROM crops WHERE name = '茶叶'), (SELECT id FROM regions WHERE name = '红河州'), 2021, 312.5, 174.8, 0.56, 18.50, '云南省农业农村厅茶产业监测', DATE '2021-09-30'),
    ((SELECT id FROM crops WHERE name = '茶叶'), (SELECT id FROM regions WHERE name = '红河州'), 2022, 318.7, 179.6, 0.56, 19.20, '云南省农业农村厅茶产业监测', DATE '2022-09-30'),
    ((SELECT id FROM crops WHERE name = '茶叶'), (SELECT id FROM regions WHERE name = '红河州'), 2023, 324.1, 184.3, 0.57, 19.85, '云南省农业农村厅茶产业监测', DATE '2023-09-30'),

    ((SELECT id FROM crops WHERE name = '大豆'), (SELECT id FROM regions WHERE name = '曲靖市'), 2019, 58.2, 108.6, 1.87, 4.05, '农业农村部大豆振兴计划', DATE '2019-10-31'),
    ((SELECT id FROM crops WHERE name = '大豆'), (SELECT id FROM regions WHERE name = '曲靖市'), 2020, 60.7, 112.4, 1.85, 4.18, '农业农村部大豆振兴计划', DATE '2020-10-31'),
    ((SELECT id FROM crops WHERE name = '大豆'), (SELECT id FROM regions WHERE name = '曲靖市'), 2021, 62.4, 116.9, 1.87, 4.33, '农业农村部大豆振兴计划', DATE '2021-10-31'),
    ((SELECT id FROM crops WHERE name = '大豆'), (SELECT id FROM regions WHERE name = '曲靖市'), 2022, 65.3, 120.8, 1.85, 4.45, '农业农村部大豆振兴计划', DATE '2022-10-31'),
    ((SELECT id FROM crops WHERE name = '大豆'), (SELECT id FROM regions WHERE name = '曲靖市'), 2023, 69.1, 129.4, 1.87, 4.58, '农业农村部大豆振兴计划', DATE '2023-10-31'),

    ((SELECT id FROM crops WHERE name = '小麦'), (SELECT id FROM regions WHERE name = '昭通市'), 2021, 82.4, 274.1, 3.33, 2.85, '云南省粮食统计简报', DATE '2021-07-31'),
    ((SELECT id FROM crops WHERE name = '小麦'), (SELECT id FROM regions WHERE name = '昭通市'), 2022, 83.9, 279.6, 3.33, 2.92, '云南省粮食统计简报', DATE '2022-07-31'),
    ((SELECT id FROM crops WHERE name = '小麦'), (SELECT id FROM regions WHERE name = '昭通市'), 2023, 85.1, 283.4, 3.33, 3.02, '云南省粮食统计简报', DATE '2023-07-31'),

    ((SELECT id FROM crops WHERE name = '烤烟'), (SELECT id FROM regions WHERE name = '玉溪市'), 2021, 96.2, 158.5, 1.65, 32.50, '云南省烟草专卖局统计', DATE '2021-08-31'),
    ((SELECT id FROM crops WHERE name = '烤烟'), (SELECT id FROM regions WHERE name = '玉溪市'), 2022, 97.5, 160.8, 1.65, 33.10, '云南省烟草专卖局统计', DATE '2022-08-31'),
    ((SELECT id FROM crops WHERE name = '烤烟'), (SELECT id FROM regions WHERE name = '玉溪市'), 2023, 99.1, 164.3, 1.66, 33.80, '云南省烟草专卖局统计', DATE '2023-08-31'),

    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '红河州'), 2021, 186.4, 1085.1, 5.82, 2.43, '云南省统计局年鉴', DATE '2021-12-31'),
    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '红河州'), 2022, 188.7, 1098.5, 5.82, 2.51, '云南省统计局年鉴', DATE '2022-12-31'),
    ((SELECT id FROM crops WHERE name = '水稻'), (SELECT id FROM regions WHERE name = '红河州'), 2023, 191.2, 1114.6, 5.83, 2.56, '云南省统计局年鉴', DATE '2023-12-31'),

    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '保山市'), 2021, 138.4, 758.2, 5.48, 2.26, '州市农业农村局统计', DATE '2021-11-30'),
    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '保山市'), 2022, 140.6, 772.4, 5.49, 2.31, '州市农业农村局统计', DATE '2022-11-30'),
    ((SELECT id FROM crops WHERE name = '玉米'), (SELECT id FROM regions WHERE name = '保山市'), 2023, 142.9, 784.1, 5.49, 2.39, '州市农业农村局统计', DATE '2023-11-30');

INSERT INTO user_accounts (username, password, display_name, email, phone, organization, active, last_login_at) VALUES
    ('admin', '$2b$10$UIK.o5XlSbcZonHxSAd6q.607GNxXel70qH7vl2Jr2cBDC38DZ6p2', '系统管理员', 'admin@yagri.gov.cn', '0871-88888801', '云南省农业农村厅信息中心', TRUE, NULL),
    ('manager', '$2b$10$wn7gRf3OXSzE8.Pz6m/No.BDuUVk7Hxdw6K9bZA4g.w3RhNz2QuI.', '州级农业主管', 'manager@ynagri.gov.cn', '0871-88888802', '红河州农业农村局', TRUE, NULL),
    ('analyst', '$2b$10$.IdzH1/NTPVVJvw0STVC1uzIjeGybOH9FqrnO7kAeBjGxrrh6/ENK', '农业数据分析师', 'analyst@ynagridata.cn', '0871-88888803', '云南农科大数据中心', TRUE, NULL),
    ('farmer', '$2b$10$t0KtEVW7cK81BlfEceuXxuSv/NTh7W3hZyZDq168zHSaQ5tIRygfO', '示范家庭农场', 'farmer@familyfarm.cn', '13800001111', '保山示范农场', TRUE, NULL);

INSERT INTO user_account_roles (user_id, role) VALUES
    ((SELECT id FROM user_accounts WHERE username = 'admin'), 'ADMIN'),
    ((SELECT id FROM user_accounts WHERE username = 'admin'), 'AGRICULTURE_DEPARTMENT'),
    ((SELECT id FROM user_accounts WHERE username = 'manager'), 'AGRICULTURE_DEPARTMENT'),
    ((SELECT id FROM user_accounts WHERE username = 'manager'), 'ENTERPRISE'),
    ((SELECT id FROM user_accounts WHERE username = 'analyst'), 'ENTERPRISE'),
    ((SELECT id FROM user_accounts WHERE username = 'analyst'), 'AGRICULTURE_DEPARTMENT'),
    ((SELECT id FROM user_accounts WHERE username = 'farmer'), 'FARMER');
