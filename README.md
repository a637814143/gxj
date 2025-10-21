# 农作物产量预测与可视化分析平台

本仓库实现了一个前后端分离的农作物产量预测与可视化分析系统，后端基于 Spring Boot 构建统一的数据与模型服务，前端使用 Vue 3 + Element Plus 提供数据导入、预测配置、仪表盘展示等多种业务界面。

## 架构与目录

- `demo/`：Spring Boot 3.5 服务，整合了鉴权、数据导入、预测运行、报表与系统配置等 REST API，并通过 MySQL 持久化业务数据。
- `forecast/`：Vite 驱动的 Vue 3 前端，内置 Pinia 状态、Vue Router 路由守卫与 axios 拦截器，对接后端 API 并提供角色控制的业务界面。
- `docs/`：系统设计说明、数据库建模脚本及导入示例数据，支撑业务分析与快速演示。

```
├── docs                    # 需求与数据库设计文档
├── demo                    # Spring Boot 后端工程
│   ├── src/main/java       # 控制器、服务、实体与安全配置
│   ├── src/main/resources  # application.yml、schema.sql、data.sql
│   └── src/test/java       # 集成测试（鉴权流程）
└── forecast                # Vue 3 前端工程
    ├── src/components      # 图表与通用组件
    ├── src/layouts         # 布局与导航
    ├── src/views           # 仪表盘、数据中心、预测等业务页面
    └── src/stores          # Pinia 状态管理（含鉴权逻辑）
```

## 后端服务（`demo/`）

### 技术与运行前提

- JDK 17（`pom.xml` 中通过 `<java.version>` 固定）。
- MySQL 8：`application.yml` 默认连接到 `jdbc:mysql://localhost:3306/database-schema`，会自动执行 `schema.sql` 与 `data.sql` 初始化结构与基础数据。
- Maven Wrapper：仓库已包含 `mvnw`，无需额外安装 Maven。

启动步骤：

```bash
cd demo
./mvnw spring-boot:run
```

首次启动会创建默认管理员账号 `admin/Admin@123` 并确保系统角色完备，可用于登录并配置其他用户。

### 功能模块总览

| 模块 | 关键 API | 功能摘要 |
| --- | --- | --- |
| 鉴权与账号 | `/api/auth/captcha`、`/api/auth/register`、`/api/auth/login`、`/api/auth/refresh`、`/api/auth/me` | 图形验证码、注册登录、刷新令牌与当前用户查询，并记录登录日志。 |
| 基础资料 | `/api/base/crops`、`/api/base/regions` | 维护作物与行政区域信息（增删改查）。 |
| 数据集管理 | `/api/datasets/files`、`/api/datasets/yield-records`、`/api/datasets/price-records` | 登记原始数据文件，录入产量与价格记录，为仪表盘与预测提供数据基础。 |
| 数据导入 | `/api/data-import/upload`、`/api/data-import/tasks` | 上传 CSV/Excel、查询导入任务状态、查看明细并批量清理任务记录。 |
| 预测管理 | `/api/forecast/models`、`/api/forecast/tasks`、`/api/forecast/predict`、`/api/forecast/history` | 定义预测模型与任务，调用预测执行接口并查询近期预测历史，用于驱动前端可视化。 |
| 报告中心 | `/api/report` | 汇总预测结果并生成报告实体，供前端展示与导出扩展。 |
| 仪表盘 | `/api/dashboard/summary` | 聚合历史产量、区域对比与预测展望，驱动前端驾驶舱图表。 |
| 系统管理 | `/api/system/settings`、`/api/system/logs`、`/api/auth/users/**` | 维护系统参数、分页查看操作日志以及管理员用户管理，接口权限由 Spring Security 约束。 |

Spring Security 采用 JWT 认证，拦截链在 `ApplicationSecurityConfig` 中配置，对不同角色授予精细化访问控制，并提供统一的 401/403 JSON 响应格式。

### 数据库结构

`schema.sql` 定义了基础作物、区域、数据集、预测运行、报表以及权限体系等 20 余张核心业务表；`data.sql` 预置了作物样例、权限角色与管理员账号，方便开箱即用。

### 测试与调试

- 自动化：运行 `./mvnw test` 会执行 `AuthControllerIntegrationTest`，验证注册、登录、刷新令牌以及携带令牌访问受保护资源的完整链路。
- 手工：启动后端后，可使用 `curl` 访问 `/api/auth/login` 获取令牌，再带上 `Authorization: Bearer <token>` 调试其他模块接口。

## 前端应用（`forecast/`）

### 技术栈与目录要点

- Vue 3 + Vite 7，配合 Element Plus、ECharts、Pinia 与 Vue Router 构建业务页面与可视化效果。
- `src/router/index.js` 定义了登录、注册以及仪表盘、数据中心、预测中心、报表、用户与系统设置等路由，并基于用户角色进行守卫跳转。
- `src/stores/auth.js` 封装鉴权状态、令牌刷新、自动登录等逻辑，与 axios 拦截器协作处理 401 续期与重定向。
- `src/views/DashboardView.vue` 等页面利用 Element Plus 组件与自定义图表展示导入数据、预测结果与业务提醒，是展示层的核心示例。

### 本地运行

```bash
cd forecast
npm install
npm run dev
```

默认开发服务器监听 `http://localhost:5173`。如需连接远程后端，可在根目录创建 `.env.local` 并设置 `VITE_API_BASE_URL`，axios 客户端会优先使用该地址；否则会根据浏览器地址回落到 `http://localhost:8080`。

构建生产资源：

```bash
npm run build
npm run preview  # 可选：本地预览构建结果
```

## 典型使用流程

1. **管理员初始化**：使用默认账号登录，补充系统设置、维护作物与区域基础数据，并按需创建业务用户与角色。
2. **数据导入**：在数据中心页面上传 CSV/Excel，后端会生成导入任务并写入 `dataset_*` 与 `forecast_*` 相关表，为仪表盘与模型提供数据来源。
3. **模型配置与运行**：维护预测模型与任务后，通过 `/api/forecast/predict` 触发预测，生成的 `forecast_run`、`forecast_snapshot` 等数据可在前端可视化组件中查看。
4. **业务呈现**：仪表盘与报表页面整合产量趋势、区域对比、预测展望以及导入明细，辅助农业管理与决策。

## 相关文档

- 系统设计说明：`docs/system_design.md`
- SQL 初始化脚本：`docs/database-schema.sql`
- 数据导入示例：`docs/import-sample.csv`

如需扩展功能，可在现有模块基础上补充领域逻辑、完善自动化测试，并根据部署环境调整安全与配置策略。
