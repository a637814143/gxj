# 项目巡检记录

## 1. 仓库概览
- 后端位于 `demo/`，基于 Spring Boot 3.5.6，整合 Web、JPA、Validation、Security 等 Starter，并引入 JWT、Apache Commons CSV、Apache POI 以及 OpenPDF 等依赖。
- 前端位于 `forecast/`，使用 Vue 3 + Vite，包含 ECharts 作为主要可视化库。
- `docs/` 提供需求分析、数据库设计、初始化 SQL 以及迁移脚本。

## 2. 检查过程
- 阅读 README 了解前后端启动方式与目录结构。
- 审查后端 `pom.xml` 了解依赖配置，确认 Java 版本为 21。
- 尝试执行 `./mvnw test` 验证后端构建情况。

## 3. 发现的问题
- Maven 编译失败，`JwtTokenProvider` 中 `Jwts.parserBuilder()` 方法不可用，需核对 jjwt 版本 API。
- `ResultCode` 缺少 `UNAUTHORIZED` 与 `FORBIDDEN` 常量声明，导致安全配置及认证服务编译失败。
- `RoleRepository` 未实现 `findByCode` 方法或方法命名与接口不一致，影响初始化逻辑。

## 4. 建议修复方向
1. 根据 jjwt 0.12.6 的 API 调整 JWT 解析逻辑（例如使用 `Jwts.parser().verifyWith(...)` 相关新接口）。
2. 在 `ResultCode` 中补充鉴权相关的状态码定义，或调整引用处使用的枚举/常量。
3. 为 `RoleRepository` 添加 `findByCode` 查询方法（可使用 `Optional<Role> findByCode(String code);`）。
4. 修复后再次运行 `./mvnw test` 确认构建通过，再安排前端依赖安装与构建验证。

## 5. 后续工作建议
- 结合 `docs/requirements.md` 的功能规划，梳理后端模块的实际实现进度。
- 为核心模块补充单元测试，并配置 CI 以自动执行构建。
- 在前端项目中补充基础路由与组件示例，便于后续联调。
