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

上述命令会启动平台后端入口 `CropYieldApplication`，默认监听 `http://localhost:8080`。

应用会读取 `demo/src/main/resources/application.yml` 中的 MySQL 数据源配置，请确保数据库已准备就绪。

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
