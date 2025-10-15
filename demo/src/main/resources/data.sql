INSERT INTO categories (name, description) VALUES
    ('项目管理', '用于项目规划、任务跟踪和协同的工具'),
    ('数据分析', '提供数据洞察和预测分析能力的软件'),
    ('设计工具', '支持视觉设计与原型的工具'),
    ('开发运维', '支撑研发流程与自动化运维的软件'),
    ('办公效率', '提升团队日常办公效率的工具'),
    ('安全合规', '保障安全审计与合规管理的产品');

INSERT INTO software_assets (name, version, vendor, license_type, annual_cost, purchase_date, maintenance_expiry_date, seats, status, category_id) VALUES
    ('AgileFlow 项目套件', '4.5', 'HorizonSoft', '订阅', 12800.00, DATEADD('MONTH', -18, CURRENT_DATE), DATEADD('DAY', 75, CURRENT_DATE), 120, 'ACTIVE', (SELECT id FROM categories WHERE name='项目管理')),
    ('InsightPro 数据分析平台', '2.8', 'DataVista', '订阅', 17800.00, DATEADD('MONTH', -20, CURRENT_DATE), DATEADD('DAY', 20, CURRENT_DATE), 80, 'ACTIVE', (SELECT id FROM categories WHERE name='数据分析')),
    ('PixelCraft 设计云', '6.1', 'CreativeHub', '订阅', 9600.00, DATEADD('MONTH', -14, CURRENT_DATE), DATEADD('MONTH', 2, CURRENT_DATE), 45, 'MAINTENANCE', (SELECT id FROM categories WHERE name='设计工具')),
    ('DevTrack 研发平台', '5.3', 'BuildSphere', '永久授权', 21500.00, DATEADD('MONTH', -30, CURRENT_DATE), DATEADD('MONTH', 6, CURRENT_DATE), 150, 'ACTIVE', (SELECT id FROM categories WHERE name='开发运维')),
    ('OfficePilot 协同套件', '3.9', 'WorkLink', '订阅', 8400.00, DATEADD('MONTH', -10, CURRENT_DATE), DATEADD('DAY', 52, CURRENT_DATE), 200, 'ACTIVE', (SELECT id FROM categories WHERE name='办公效率')),
    ('SecureWatch 风险管理', '1.7', 'ShieldGuard', '订阅', 15200.00, DATEADD('MONTH', -8, CURRENT_DATE), DATEADD('MONTH', 5, CURRENT_DATE), 65, 'ACTIVE', (SELECT id FROM categories WHERE name='安全合规')),
    ('SprintNote 轻量项目', '1.3', 'LiteWorks', '订阅', 4800.00, DATEADD('MONTH', -6, CURRENT_DATE), DATEADD('DAY', 15, CURRENT_DATE), 35, 'MAINTENANCE', (SELECT id FROM categories WHERE name='项目管理')),
    ('FlowOps 自动化平台', '3.1', 'DevOps Lab', '订阅', 9800.00, DATEADD('MONTH', -16, CURRENT_DATE), DATEADD('DAY', 110, CURRENT_DATE), 60, 'ACTIVE', (SELECT id FROM categories WHERE name='开发运维')),
    ('VisionBoard 原型工具', '7.0', 'DesignForge', '订阅', 7200.00, DATEADD('MONTH', -12, CURRENT_DATE), DATEADD('MONTH', 1, CURRENT_DATE), 50, 'ACTIVE', (SELECT id FROM categories WHERE name='设计工具')),
    ('DataPilot 商业智能', '4.2', 'Insightive', '订阅', 16800.00, DATEADD('MONTH', -24, CURRENT_DATE), DATEADD('MONTH', 4, CURRENT_DATE), 95, 'ACTIVE', (SELECT id FROM categories WHERE name='数据分析'));

INSERT INTO usage_predictions (software_id, prediction_date, predicted_annual_cost, predicted_active_users, confidence, notes) VALUES
    ((SELECT id FROM software_assets WHERE name='AgileFlow 项目套件'), DATEADD('MONTH', -5, CURRENT_DATE), 13200.00, 118, 0.82, '季度新增 12 个项目团队'),
    ((SELECT id FROM software_assets WHERE name='AgileFlow 项目套件'), DATEADD('MONTH', -2, CURRENT_DATE), 13500.00, 126, 0.85, '规模化部署需求增长'),
    ((SELECT id FROM software_assets WHERE name='InsightPro 数据分析平台'), DATEADD('MONTH', -5, CURRENT_DATE), 18200.00, 74, 0.76, '新增 2 组分析师'),
    ((SELECT id FROM software_assets WHERE name='InsightPro 数据分析平台'), DATEADD('MONTH', -1, CURRENT_DATE), 18900.00, 81, 0.81, '扩展预测模型节点'),
    ((SELECT id FROM software_assets WHERE name='PixelCraft 设计云'), DATEADD('MONTH', -4, CURRENT_DATE), 9900.00, 42, 0.74, '品牌设计团队扩容'),
    ((SELECT id FROM software_assets WHERE name='PixelCraft 设计云'), DATEADD('MONTH', -1, CURRENT_DATE), 10050.00, 45, 0.78, '交付物版本需求增多'),
    ((SELECT id FROM software_assets WHERE name='DevTrack 研发平台'), DATEADD('MONTH', -6, CURRENT_DATE), 22000.00, 146, 0.79, '核心模块研发周期延长'),
    ((SELECT id FROM software_assets WHERE name='DevTrack 研发平台'), DATEADD('MONTH', -3, CURRENT_DATE), 22400.00, 153, 0.83, '新增持续交付流水线'),
    ((SELECT id FROM software_assets WHERE name='OfficePilot 协同套件'), DATEADD('MONTH', -5, CURRENT_DATE), 8700.00, 188, 0.72, '人力资源协作需求上涨'),
    ((SELECT id FROM software_assets WHERE name='OfficePilot 协同套件'), DATEADD('MONTH', -1, CURRENT_DATE), 9100.00, 205, 0.77, '会议协同模块升级'),
    ((SELECT id FROM software_assets WHERE name='SecureWatch 风险管理'), DATEADD('MONTH', -4, CURRENT_DATE), 15500.00, 61, 0.8, '审计频次提升'),
    ((SELECT id FROM software_assets WHERE name='SecureWatch 风险管理'), DATEADD('MONTH', -2, CURRENT_DATE), 15900.00, 64, 0.84, '纳入云安全模块'),
    ((SELECT id FROM software_assets WHERE name='FlowOps 自动化平台'), DATEADD('MONTH', -3, CURRENT_DATE), 10100.00, 59, 0.75, '上线新流水线 2 条'),
    ((SELECT id FROM software_assets WHERE name='VisionBoard 原型工具'), DATEADD('MONTH', -2, CURRENT_DATE), 7500.00, 48, 0.71, '跨事业部共用场景'),
    ((SELECT id FROM software_assets WHERE name='DataPilot 商业智能'), DATEADD('MONTH', -1, CURRENT_DATE), 17200.00, 99, 0.86, '增加实时数据源接入');
