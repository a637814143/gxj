# 农作物产量预测与可视化分析系统

基于 Spring Boot + Vue3 的前后端分离项目，用于云南省主要农作物的历史数据管理、趋势分析、产量预测与报告输出。

## 仓库结构

```
backend/   # Spring Boot 后端服务
frontend/  # Vue3 + Element Plus 前端单页应用
forecast/  # 预留的模型研究资料
```

## 快速开始

### 后端
```bash
cd backend
./mvnw spring-boot:run
```

### 前端
```bash
cd frontend
npm install
npm run dev
```

## 文档
- `docs/requirements.md`：需求分析
- `docs/database-design.md`：数据库设计说明
- `docs/database-schema.sql`：数据库初始化 SQL
