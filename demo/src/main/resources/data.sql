INSERT INTO base_crop (code, name, description, created_at, updated_at) VALUES
    ('RICE', '水稻', '云南主粮之一，集中分布在滇中和滇南稻区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MAIZE', '玉米', '适宜山地丘陵地区，兼顾粮饲双用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('POTATO', '马铃薯', '高海拔地区重要的口粮与商品作物', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('RAPESEED', '油菜', '滇中、滇东高原为主要产区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TEA', '茶叶', '普洱、临沧、保山等地重要优势产业', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SOYBEAN', '大豆', '滇东北、滇西北地区特色粮油作物', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO base_region (code, name, description, created_at, updated_at) VALUES
    ('YN_PROV', '云南省', '省级统计总览', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('KM_CITY', '昆明市', '滇中城市群核心城市', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('QJ_CITY', '曲靖市', '滇东北粮食主产区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('HH_PREF', '红河州', '滇南山区粮经复合种植区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DL_PREF', '大理州', '滇西北高原生态农业区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO dataset_yield_record (crop_id, region_id, record_year, yield_per_hectare, created_at, updated_at) VALUES
    ((SELECT id FROM base_crop WHERE code = 'RICE'), (SELECT id FROM base_region WHERE code = 'YN_PROV'), 2020, 5.75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'RICE'), (SELECT id FROM base_region WHERE code = 'YN_PROV'), 2021, 5.74, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'RICE'), (SELECT id FROM base_region WHERE code = 'YN_PROV'), 2022, 5.75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'MAIZE'), (SELECT id FROM base_region WHERE code = 'QJ_CITY'), 2021, 5.64, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'MAIZE'), (SELECT id FROM base_region WHERE code = 'QJ_CITY'), 2022, 5.69, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'MAIZE'), (SELECT id FROM base_region WHERE code = 'QJ_CITY'), 2023, 5.74, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'POTATO'), (SELECT id FROM base_region WHERE code = 'DL_PREF'), 2021, 1.96, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'POTATO'), (SELECT id FROM base_region WHERE code = 'DL_PREF'), 2022, 1.96, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'RAPESEED'), (SELECT id FROM base_region WHERE code = 'KM_CITY'), 2021, 2.10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'RAPESEED'), (SELECT id FROM base_region WHERE code = 'KM_CITY'), 2022, 2.12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'TEA'), (SELECT id FROM base_region WHERE code = 'HH_PREF'), 2021, 0.56, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'TEA'), (SELECT id FROM base_region WHERE code = 'HH_PREF'), 2022, 0.56, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'SOYBEAN'), (SELECT id FROM base_region WHERE code = 'QJ_CITY'), 2022, 1.85, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM base_crop WHERE code = 'SOYBEAN'), (SELECT id FROM base_region WHERE code = 'QJ_CITY'), 2023, 1.87, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
