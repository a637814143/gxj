# 农作物产量预测与可视化分析平台

本项目根据开题报告要求，采用 Spring Boot + Vue3 前后端分离架构，实现农作物产量预测与可视化分析平台的基础骨架，包含需求分析、数据库设计以及前后端初始代码。

## 目录结构

- `docs/requirements.md`：项目需求分析文档。
- `docs/database-design.md`：数据库设计说明。
- `docs/database-schema.sql`：可直接执行的数据库初始化 SQL。
- `demo/`：Spring Boot 后端工程，提供基础模块与 REST API 框架。
- `forecast/`：Vue3 前端工程，包含页面路由与布局雏形。

## 快速开始

### 后端

```bash
cd demo
mvn spring-boot:run
```

后端默认使用 MySQL 数据库。请先在本地创建与 `application.yml` 中一致的连接信息，并执行 `docs/database-schema.sql` 里的建表脚本完成初始化。脚本基于 InnoDB 引擎，已经为主外键、唯一约束和常用索引做好配置，可直接在 Navicat 的查询窗口运行。

若需调整数据库连接信息，只需修改 `demo/src/main/resources/application.yml` 中的数据源配置字段（`url`、`username`、`password` 等）。

执行完脚本后，即可在 Navicat 中浏览农作物、区域、产量、气象、预测等业务表，并让 Spring Boot 与该 MySQL 实例联动。

### 前端

```bash
cd forecast
npm install
npm run dev
```

Vite 开发服务器默认运行在 `http://localhost:5173`。

## 后续工作指引

- 根据业务需求补充具体的领域逻辑、数据校验与权限控制。
- 将前端页面与后端接口对接，并完善数据可视化与交互体验。
- 完善自动化测试、部署脚本及监控告警等非功能需求。
