# 数据库设计说明

## 1. 概览
- **数据库名称**：`crop_yield`
- **使用场景**：支撑云南省农作物产量预测与可视化分析系统的数据管理、模型预测、报告存档等业务需求。
- **设计原则**：满足第三范式、可扩展性、历史版本管理需求，关键业务表预留冗余字段用于后续扩展。

## 2. 模块划分
1. **基础信息层**：作物、地区、指标、数据来源等基础字典表。
2. **数据资源层**：历史产量、价格、数据文件等业务数据表。
3. **预测分析层**：预测模型、预测任务、预测结果、评估指标。
4. **报告输出层**：报告档案、报告明细、模板配置。
5. **权限审计层**：用户、角色、权限、操作日志。

## 3. 核心表结构

### 3.1 基础信息表
| 表名 | 描述 | 关键字段 |
| --- | --- | --- |
| `ag_crop` | 作物字典 | `name`、`category`、`unit`、`description` |
| `ag_region` | 行政区划 | `code`(唯一编码)、`name`、`level`(省/州/县) 、`parent_id` |
| `ag_indicator` | 指标定义 | `indicator_code`、`name`、`unit`、`description` |
| `ag_datasource` | 数据来源 | `source_code`、`name`、`contact`、`remark` |

### 3.2 数据资源表
| 表名 | 描述 | 关键字段 |
| --- | --- | --- |
| `ag_yield_record` | 历史产量/面积记录 | `crop_id`、`region_id`、`year`、`sown_area`、`harvested_area`、`production`、`yield_per_mu` |
| `ag_price_record` | 市场价格记录 | `crop_id`、`year`、`average_price`、`unit`、`data_source` |
| `ag_dataset_file` | 数据文件档案 | `file_name`、`file_type`、`storage_path`、`description`、`imported_at` |
| `ag_quality_issue` | 数据质量问题 | `dataset_id`、`issue_type`、`description`、`status` |

### 3.3 预测分析表
| 表名 | 描述 | 关键字段 |
| --- | --- | --- |
| `fc_model` | 模型管理 | `name`、`algorithm`、`hyper_parameters`、`description` |
| `fc_task` | 预测任务 | `crop_id`、`region_id`、`model_id`、`start_year`、`end_year`、`horizon_years`、`status` |
| `fc_result` | 预测结果 | `task_id`、`target_year`、`predicted_production`、`predicted_yield_per_mu`、`confidence_lower/upper` |
| `fc_metric` | 模型评估指标 | `task_id`、`metric_name`、`metric_value` |
| `fc_scenario` | 情景模拟记录 | `task_id`、`scenario_type`、`assumption_json`、`recommendation` |

### 3.4 报告输出表
| 表名 | 描述 | 关键字段 |
| --- | --- | --- |
| `report_archive` | 报告档案 | `title`、`summary`、`file_url`、`generated_at`、`generated_by`、`status` |
| `report_section` | 报告章节 | `report_id`、`section_title`、`content`、`order_no` |

### 3.5 权限审计表
| 表名 | 描述 | 关键字段 |
| --- | --- | --- |
| `sys_user` | 用户 | `username`、`password`、`email`、`enabled`、`locked` |
| `sys_role` | 角色 | `code`、`name`、`description` |
| `sys_permission` | 权限 | `code`、`name`、`description` |
| `sys_user_role` | 用户角色关联 | `user_id`、`role_id` |
| `sys_role_permission` | 角色权限关联 | `role_id`、`permission_id` |
| `sys_operation_log` | 操作日志 | `user_id`、`action`、`module`、`ip`、`created_at` |

## 4. 表字段设计示例

### 4.1 `ag_yield_record`
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AI | 主键 |
| `crop_id` | BIGINT | FK `ag_crop.id` | 作物 |
| `region_id` | BIGINT | FK `ag_region.id` | 地区 |
| `year` | INT | NOT NULL | 年份 |
| `sown_area` | DECIMAL(16,4) | NULL | 播种面积(万亩) |
| `harvested_area` | DECIMAL(16,4) | NULL | 收获面积(万亩) |
| `production` | DECIMAL(16,4) | NULL | 总产量(万吨) |
| `yield_per_mu` | DECIMAL(16,4) | NULL | 单产(公斤/亩) |
| `data_source` | VARCHAR(64) | NULL | 数据来源标识 |
| `data_version` | VARCHAR(32) | NULL | 数据版本 |
| `created_at` | DATETIME | 默认当前 | 创建时间 |
| `updated_at` | DATETIME | 默认当前 | 更新时间 |

### 4.2 `fc_task`
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AI | 主键 |
| `crop_id` | BIGINT | FK `ag_crop.id` | 预测作物 |
| `region_id` | BIGINT | FK `ag_region.id` | 预测地区 |
| `model_id` | BIGINT | FK `fc_model.id` | 使用模型 |
| `start_year` | INT | NOT NULL | 训练起始年份 |
| `end_year` | INT | NOT NULL | 训练结束年份 |
| `horizon_years` | INT | NOT NULL | 预测期数 |
| `status` | VARCHAR(32) | NOT NULL | 任务状态（CREATED/RUNNING/COMPLETED/FAILED） |
| `started_at` | DATETIME | NULL | 启动时间 |
| `completed_at` | DATETIME | NULL | 完成时间 |
| `error_message` | VARCHAR(256) | NULL | 异常信息 |

## 5. 索引设计
- `ag_yield_record`: 复合索引 `(crop_id, region_id, year)`，用于趋势分析与预测训练的快速检索。
- `ag_price_record`: 索引 `(crop_id, year)`。
- `fc_task`: 索引 `(status)` 便于任务调度扫描；`(created_at)` 支持时间序列统计。
- `report_archive`: 索引 `(status, generated_at)` 加速报告检索。
- `sys_user`: 唯一索引 `username`；`sys_role` 唯一索引 `code`。

## 6. 数据一致性与审计
- 所有主数据表继承审计字段 `created_at`、`updated_at`，并使用触发器或应用层逻辑自动维护。
- 对关键业务操作（数据导入、模型训练、报告发布）写入 `sys_operation_log`，记录操作人、模块、耗时、结果。

## 7. 扩展规划
- **时序扩展**：如需更高效的时间序列分析，可引入 TimescaleDB 或 Doris，现阶段通过物化视图加速。
- **多源数据融合**：在 `ag_dataset_file` 中增加 `metadata_json` 字段存储额外信息，方便接入遥感、气象等数据。
- **数据湖对接**：预留与对象存储（OSS、MinIO）同步的机制，支持大文件存储与AI训练。
