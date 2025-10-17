# 云南主要农作物产量分析与预测系统设计

## 1. 总体架构

```mermaid
graph TD
    subgraph Client[前端应用（forecast/Vue3）]
        UI[数据管理界面]
        Chart[ECharts 可视化]
        ForecastView[预测与决策视图]
    end

    subgraph Gateway[Nginx 反向代理/静态资源服务]
        SSR[静态资源托管]
        APIProxy[API 代理]
    end

    subgraph Backend[后端服务（demo/Spring Boot）]
        Controller[REST Controller]
        Service[业务服务层]
        DataPipeline[数据导入与清洗模块]
        Analytics[可视化数据接口]
        Forecast[预测服务编排]
        Recommendation[收益与推荐模块]
    end

    subgraph Persistence[数据与模型存储]
        MySQL[(MySQL 数据库)]
        ObjectStore[(文件存储/MinIO)]
        ModelRegistry[模型仓库]
    end

    subgraph ML[机器学习执行环境]
        PyService[Python 预测服务（ARIMA/Prophet/LSTM）]
        Scheduler[任务调度器 (Quartz/Spring Scheduling)]
    end

    subgraph Infra[基础设施]
        Auth[统一认证/Token 管理]
        Monitor[日志&监控 (ELK/Prometheus)]
    end

    Client -->|HTTPS| Gateway
    Gateway -->|REST API| Backend
    Backend -->|JPA/MyBatis| MySQL
    Backend -->|文件/模型| ObjectStore
    Backend -->|HTTP/gRPC| PyService
    Backend --> Scheduler
    PyService --> ModelRegistry
    Backend --> Monitor
    Gateway --> Auth
    Client --> Auth
```

> 说明：系统采用前后端分离架构。Spring Boot 后端提供统一 RESTful API，负责数据管理、预测编排与决策支持；Vue3 前端消费 API，展示可视化图表与预测结果。ARIMA/Prophet/LSTM 模型在独立的 Python 服务中训练与推理，Spring Boot 通过异步任务调度与其通信。

## 2. 前后端分离技术方案

### 前端（forecast 包）
- **框架**：Vue 3 + Vite，采用组合式 API。
- **状态管理**：Pinia 管理全局状态（用户、当前筛选条件、图表配置等）。
- **路由**：Vue Router 分离数据管理、可视化、预测分析、决策支持等页面。
- **UI 组件**：Element Plus/Ant Design Vue 负责表格、表单、对话框等组件。
- **可视化**：ECharts 封装为自定义组件，支持多维度折线、柱状、热力、地图等图表。
- **数据交互**：Axios + 拦截器统一处理鉴权 Token、错误提示与加载状态。
- **国际化**：vue-i18n 预留多语言扩展；默认中文。
- **构建部署**：Vite 打包，静态资源由 Nginx 托管或由 Spring Boot 静态目录代理；使用 `.env` 区分开发/生产 API 基础路径。

### 后端（demo 包）
- **框架**：Spring Boot 3.x，分层结构 Controller -> Service -> Repository。
- **数据访问**：Spring Data JPA 或 MyBatis-Plus；结合 Specification/QueryWrapper 支持动态筛选。
- **数据导入与清洗**：利用 Apache Commons CSV/POI 解析 Excel/CSV，集成 MapStruct 做 DTO-Entity 转换，Bean Validation 校验。
- **预测任务编排**：
  - Spring Scheduler/Quartz 定时触发训练、回测。
  - 使用 Spring Cloud OpenFeign/RestTemplate 调用 Python 预测服务。
  - 采用消息队列（如 RabbitMQ）可选提升异步能力。
- **安全**：Spring Security + JWT，RBAC 控制数据导入/预测操作权限。
- **文档**：Springdoc OpenAPI 生成 Swagger 接口文档。
- **监控**：Spring Boot Actuator + Micrometer（Prometheus）。

### 数据导入示例

仓库提供了一个可直接导入的示例文件 [`docs/import-sample.csv`](./import-sample.csv)。

- 覆盖系统当前内置的“云南省”“昆明市”“小麦”“玉米”等基础数据，也包含一个新的“楚雄州”和“马铃薯”记录，便于验证自动建档逻辑。
- 表头已经按照 `DataImportService` 的同义词映射准备，包含作物、地区、年份、播种面积、产量、单产、平均价格、数据来源、采集日期等字段，且单位设置可触发自动换算（如“千公顷”“万吨”“元/吨”）。
- 导入后应能看到任务进度正常推进，并在进度详情页预览到 4 条数据，其中新增作物和地区会在基础库中自动创建。

### Python 预测服务
- 运行于独立容器或虚拟环境，提供 REST/gRPC 接口。
- 使用 `pmdarima`、`prophet`、`torch`/`tensorflow` 实现 ARIMA、Prophet、LSTM 模型。
- 使用 MLflow/自定义模型仓库存储模型版本及评估指标。
- 返回预测值、置信区间、模型元数据，供后端入库展示。

## 3. 数据库表结构设计

### 3.1 基础维表

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `base_region` | 行政区域基础信息 | `id`(PK)、`code`、`name`、`level`、`parent_code`、`description` |
| `base_crop` | 农作物基础信息 | `id`(PK)、`code`、`name`、`category`、`description` |
| `dataset_file` | 数据文件登记 | `id`(PK)、`name`、`type`、`storage_path`、`description` |

### 3.2 产量与气象数据

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `dataset_yield_record` | 作物年均单产 | `id`(PK)、`crop_id`(FK)、`region_id`(FK)、`year`、`sown_area`、`production`、`yield_per_hectare`、`average_price`、`data_source`、`collected_at` |
| `dataset_price_record` | 作物价格序列 | `id`(PK)、`crop_id`(FK)、`region_id`(FK)、`record_date`、`price` |
| `data_import_job` | 数据导入任务队列 | `id`(PK)、`task_id`、`dataset_name`、`dataset_type`、`total_rows`、`processed_rows`、`inserted_rows`、`updated_rows`、`skipped_rows`、`failed_rows`、`status`、`message`、`created_at`、`updated_at` |
| `data_import_job_error` | 导入错误摘要 | `id`(PK)、`job_id`(FK)、`line_number`、`error_code`、`message`、`raw_value`、`created_at`、`updated_at` |

### 3.3 预测与评估

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `forecast_model` | 预测模型配置 | `id`(PK)、`name`、`type`、`description` |
| `forecast_task` | 预测任务 | `id`(PK)、`model_id`(FK)、`crop_id`(FK)、`region_id`(FK)、`status`、`parameters`、`created_at`、`updated_at` |
| `forecast_run` | 预测执行记录 | `id`(PK)、`model_id`(FK)、`crop_id`(FK)、`region_id`(FK)、`status`、`forecast_periods`、`history_years`、`frequency`、`mae`、`rmse`、`mape`、`r2` |
| `forecast_run_series` | 预测序列明细 | `id`(PK)、`run_id`(FK)、`period`、`value`、`lower_bound`、`upper_bound`、`historical` |
| `forecast_result` | 预测结果摘要 | `id`(PK)、`task_id`(FK)、`target_year`、`predicted_yield`、`evaluation` |
| `report_summary` | 预测报告 | `id`(PK)、`title`、`forecast_result_id`(FK)、`insights` |

### 3.4 用户与审计

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `sys_user` | 系统用户账户 | `id`(PK)、`username`、`password`、`full_name`、`email`、`created_at`、`updated_at` |
| `sys_role` | 角色定义 | `id`(PK)、`code`、`name`、`description`、`created_at`、`updated_at` |
| `sys_permission` | 权限点定义 | `id`(PK)、`code`、`name`、`description`、`created_at`、`updated_at` |
| `sys_user_role` | 用户角色关联 | `user_id`(FK)、`role_id`(FK)、`created_at` |
| `sys_role_permission` | 角色权限关联 | `role_id`(FK)、`permission_id`(FK)、`created_at` |
| `sys_refresh_token` | 刷新令牌 | `id`(PK)、`token`、`user_id`(FK)、`expires_at`、`created_at`、`updated_at` |
| `sys_login_log` | 登录日志 | `id`(PK)、`username`、`success`、`ip_address`、`user_agent`、`message`、`created_at`、`updated_at` |
| `sys_log` | 系统操作日志 | `id`(PK)、`username`、`action`、`detail`、`created_at`、`updated_at` |

**索引与约束建议**
- `dataset_yield_record` 建立 `(crop_id, region_id, year)` 唯一索引，保证年度单产唯一。
- `dataset_price_record` 建立 `(crop_id, region_id, record_date)` 唯一索引，快速定位价格时间点。
- `forecast_result` 建立 `(task_id, target_year)` 唯一索引，便于查询各任务年度预测。
- 所有外键字段加索引提升联表效率。

## 4. 项目初始化配置与依赖

### 4.1 后端（demo）

**Maven 依赖**
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-csv</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

**关键配置**（`application.yml`）
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/yunnan_agri?useSSL=false&serverTimezone=Asia/Shanghai
    username: agri_user
    password: strong_password
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
    show-sql: true
  jackson:
    serialization:
      write-dates-as-timestamps: false

app:
  security:
    jwt-secret: ${JWT_SECRET}
    token-validity: 86400
  ml-service:
    base-url: http://ml-service:8000
    timeout: 30s

logging:
  level:
    root: INFO
    com.gxj.cropyield: DEBUG
```

**初始化脚本**
- 使用 Flyway/Liquibase 维护数据库结构，初始迁移创建上述表。
- 在 `data.sql` 预置常用区域、作物数据。
- 配置 Docker Compose：包含 `springboot-app`、`mysql`、`ml-service`、`nginx`、`minio` 服务。

### 4.2 前端（forecast）

**依赖**
```bash
npm install vue@3 vite pinia vue-router axios echarts element-plus @vueuse/core --save
npm install @types/echarts sass --save-dev
```

**项目结构建议**
```
src/
  api/           # axios 实例与接口封装
  stores/        # Pinia store
  views/         # 页面视图（DataManagement.vue / Visualization.vue / Forecast.vue / Decision.vue）
  components/    # 图表组件、表单组件
  router/        # 路由配置
  assets/        # 全局样式、主题
  utils/         # 工具方法（格式化、权限指令）
```

**环境变量示例**
```
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api

# .env.production
VITE_API_BASE_URL=https://api.example.com/api
```

**全局样式与布局**
- 使用 CSS 变量统一颜色、字体。
- 自定义主题适配云南农业色彩（绿色、金色）。
- 支持暗黑模式（ECharts、Element Plus 主题联动）。

### 4.3 Python 预测服务

**环境依赖（requirements.txt）**
```
fastapi
uvicorn
pydantic
pandas
numpy
scikit-learn
pmdarima
prophet
torch
mlflow
joblib
redis
loguru
```

**基础结构**
```
app/
  main.py         # FastAPI 入口
  routers/
    forecast.py   # 预测接口
  services/
    arima_service.py
    prophet_service.py
    lstm_service.py
  models/
    schemas.py    # 请求/响应模型
  repository/
    model_registry.py
```

## 5. 开发流程建议
1. 定义统一 API 契约（OpenAPI），前后端依据接口并行开发。
2. 通过 Docker Compose 启动依赖服务，`npm run dev` 与 `mvn spring-boot:run` 联调。
3. 引入 CI/CD（GitHub Actions）：自动构建、运行单元测试、前端 Lint、后端测试。
4. 使用 SonarQube/Checkstyle + ESLint/Prettier 保证代码质量。
5. 部署时后端、Python 服务容器部署于同一网络；前端静态资源部署 Nginx/CDN。
