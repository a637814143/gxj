# 数据库设计说明

## 1. 概述

数据库采用 MySQL 兼容设计，覆盖基础信息、数据资源、预测任务、报告输出与权限控制等核心模块。整体遵循以下原则：

- **模块化**：不同业务域分库表（前缀区分 base/dataset/forecast/report/sys）。
- **可扩展**：字段预留描述、参数、扩展 JSON 等，便于后续功能拓展。
- **审计性**：所有表继承公共字段 `id`、`created_at`、`updated_at`，支撑数据追踪。

## 2. 表结构概览

| 模块 | 表名 | 说明 |
| ---- | ---- | ---- |
| 基础信息 | `base_crop` | 作物基础信息（编码、名称、描述等） |
|  | `base_region` | 区域基础信息（编码、名称、气候描述） |
| 数据资源 | `dataset_file` | 数据文件元数据（类型、路径、描述） |
|  | `dataset_yield_record` | 历史产量记录（作物、区域、年份、产量） |
|  | `dataset_price_record` | 市场价格记录（作物、区域、日期、价格） |
| 预测分析 | `forecast_model` | 预测模型定义（名称、类型、描述） |
|  | `forecast_task` | 预测任务配置（模型、作物、区域、参数、状态） |
|  | `forecast_result` | 预测输出结果（目标年份、预测值、评价） |
| 报告输出 | `report_summary` | 分析报告（标题、摘要、关联预测结果） |
| 权限审计 | `sys_user` | 系统用户（账号、姓名、邮箱） |
|  | `sys_role` | 角色定义 |
|  | `sys_permission` | 权限点 |
|  | `sys_user_role` | 用户-角色关联 |
|  | `sys_role_permission` | 角色-权限关联 |

## 3. 关键表字段说明

### 3.1 `dataset_yield_record`
- `crop_id` / `region_id`：外键关联作物与区域，支持多区域多作物组合。
- `year`：年份，整型便于范围查询。
- `yield_per_hectare`：单位面积产量（吨/公顷），支持浮点。

### 3.2 `forecast_task`
- `model_id`、`crop_id`、`region_id`：关联模型与数据域。
- `status`：任务状态（PENDING、RUNNING、SUCCESS、FAILED）。
- `parameters`：JSON 字符串，记录特征字段、时间范围等动态参数。

### 3.3 `forecast_result`
- `task_id`：关联来源任务。
- `target_year`：预测目标年份。
- `predicted_yield`：预测产量数值。
- `evaluation`：评价指标或说明。

### 3.4 权限表
- `sys_user` 与 `sys_role`、`sys_permission` 采用多对多关系，满足多角色、多权限管理需求。

## 4. 约束与索引

- 所有基础信息表在 `code` 字段上设置唯一索引，确保编码唯一性。
- 关联表建立外键约束，确保数据一致性，可根据实际部署在高并发场景下调整为逻辑约束。
- 常用查询字段（如 `dataset_yield_record.year`、`dataset_price_record.record_date`）建立普通索引以提升性能。

## 5. 初始化数据建议

- `sys_role` 预置「管理员」「分析员」「查看者」三个角色。
- `sys_permission` 预置菜单、接口权限分类，初期可与角色一一绑定。
- `base_crop`、`base_region` 可导入典型作物与重点区域基础数据，为预测模型准备输入。

详细建表 SQL 见 `docs/database-schema.sql`，H2 环境初始化示例可参考 `docs/h2-auth-init.sql`。
