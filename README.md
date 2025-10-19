# 农作物产量预测与可视化分析平台

本项目根据开题报告要求，采用 Spring Boot + Vue3 前后端分离架构，实现农作物产量预测与可视化分析平台的基础骨架，包含需求分析、数据库设计以及前后端初始代码。

## 目录结构

- `docs/requirements.md`：项目需求分析文档。
- `docs/database-design.md`：数据库设计说明。
- `docs/database-schema.sql`：可直接执行的数据库初始化 SQL。
- `demo/`：Spring Boot 后端工程，提供基础模块与 REST API 框架。
- `forecast/`：Vue3 前端工程，包含页面路由与布局雏形。
- `machine/`：基于 Flask 的机器学习服务，提供农作物产量预测接口。

## 快速开始

### 后端

```bash
cd demo
mvn spring-boot:run
```

项目默认使用内存 H2 数据库，并开放 `http://localhost:8080` 的基础 REST 接口。

### 机器学习服务

```bash
cd machine
python -m venv .venv
source .venv/bin/activate  # Windows 使用 .venv\\Scripts\\activate
pip install -r requirements.txt
python app.py
```

启动后默认监听 `http://localhost:5001`，提供 `/predict` 等接口供前端调用，可通过环境变量 `CROP_DATASET_PATH` 指定自定义数据集。

> ℹ️ Windows `cmd.exe` 中运行示例 `curl` 时请写成单行并用双引号包裹 JSON 负载，详细示例见 [`machine/README.md`](machine/README.md)。

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
