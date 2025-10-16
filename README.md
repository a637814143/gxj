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

默认的 Spring Boot 配置仍兼容内存 H2 数据库，若需要在 Navicat 中使用 MySQL，请先在本地创建数据库并执行 `docs/database-schema.sql` 中的建表脚本。脚本基于 InnoDB 引擎，已经为主外键、唯一约束和常用索引做好配置，可直接在 Navicat 的查询窗口运行。

示例连接配置：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/yunnan_agri?useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=none
```

执行完脚本后，即可在 Navicat 中浏览农作物、区域、产量、气象、预测等业务表，并将 Spring Boot 数据源指向该 MySQL 实例。

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
