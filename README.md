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

项目默认使用内存 H2 数据库，并开放 `http://localhost:8080` 的基础 REST 接口。

#### 连接到 MySQL 等外部数据库

1. 创建或修改 `demo/src/main/resources/application-mysql.yml` 中的数据库连接信息：

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://数据库地址:3306/库名?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8
       username: 数据库账号
       password: 数据库密码
   ```

2. 在启动命令中指定使用 `mysql` 配置文件：

   ```bash
   cd demo
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

   或者在部署环境中设置环境变量：

   ```bash
   export SPRING_PROFILES_ACTIVE=mysql
   export SPRING_DATASOURCE_URL=jdbc:mysql://...
   export SPRING_DATASOURCE_USERNAME=...
   export SPRING_DATASOURCE_PASSWORD=...
   ```

   Spring Boot 会自动读取环境变量覆盖 `application-mysql.yml` 中的默认值，实现与实际数据库的连接。

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
