# 农作物产量预测与可视化分析系统

## 项目简介

基于Spring Boot + Vue3的前后端分离农作物产量预测与可视化分析系统，支持多种预测模型和数据分析功能。

## 技术栈

### 后端
- **框架**: Spring Boot 3.5.6
- **数据库**: MySQL 8.0
- **安全**: Spring Security + JWT
- **ORM**: Spring Data JPA
- **文档处理**: Apache POI
- **构建工具**: Maven

### 前端
- **框架**: Vue 3.2.13
- **UI组件**: Element Plus
- **状态管理**: Vuex 4
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **图表库**: ECharts + Vue-ECharts
- **构建工具**: Vue CLI

## 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Vue3 前端     │    │  Spring Boot    │    │   MySQL 数据库  │
│                 │    │     后端        │    │                 │
│ - 用户界面      │◄──►│ - RESTful API   │◄──►│ - 用户数据      │
│ - 数据可视化    │    │ - 业务逻辑      │    │ - 作物数据      │
│ - 预测分析      │    │ - 预测模型      │    │ - 预测结果      │
│ - 报告生成      │    │ - 权限控制      │    │ - 系统日志      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 功能模块

### 1. 用户管理
- **管理员**: 用户管理、系统配置、权限控制
- **科研人员**: 数据分析、预测建模、报告生成
- **农户**: 查看预测结果、种植建议

### 2. 数据管理
- 数据导入（Excel、CSV）
- 数据清洗和验证
- 数据预览和编辑
- 数据备份和恢复

### 3. 预测分析
- **ARIMA模型**: 时间序列分析
- **Prophet模型**: Facebook预测工具
- **XGBoost模型**: 梯度提升算法
- **LSTM模型**: 长短期记忆网络
- 情景模拟和参数调整

### 4. 数据可视化
- 趋势分析图表
- 地区对比图表
- 结构占比图表
- 地理分布地图
- 交互式图表操作

### 5. 报告管理
- 自动生成分析报告
- 多格式导出（PDF、Word、Excel）
- 报告模板管理
- 报告分享和打印

## 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- MySQL 8.0+
- Maven 3.6+

### 后端启动

1. 克隆项目
```bash
git clone <repository-url>
cd bishe/demo
```

2. 配置数据库
```sql
CREATE DATABASE crop_forecast CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 修改配置文件
```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/crop_forecast
spring.datasource.username=your_username
spring.datasource.password=your_password
```

4. 启动后端服务
```bash
./mvnw.cmd spring-boot:run
```

### 前端启动

1. 进入前端目录
```bash
cd ../forecast
```

2. 安装依赖
```bash
npm install
```

3. 启动开发服务器
```bash
npm run serve
```

4. 访问系统
- 前端地址: http://localhost:8081
- 后端API: http://localhost:8080

## 默认账户

| 角色 | 用户名 | 密码 | 权限 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 全部权限 |
| 研究员 | researcher | researcher123 | 数据分析、预测 |
| 农户 | farmer | farmer123 | 查看预测结果 |

## 项目结构

```
bishe/
├── demo/                    # Spring Boot 后端
│   ├── src/main/java/
│   │   └── nongye/example/demo/
│   │       ├── config/      # 配置类
│   │       ├── controller/  # 控制器
│   │       ├── entity/      # 实体类
│   │       ├── repository/  # 数据访问层
│   │       ├── service/     # 业务逻辑层
│   │       ├── security/    # 安全配置
│   │       └── util/        # 工具类
│   └── src/main/resources/
│       └── application.properties
└── forecast/                # Vue3 前端
    ├── src/
    │   ├── api/            # API接口
    │   ├── assets/         # 静态资源
    │   ├── components/     # 组件
    │   ├── layout/         # 布局组件
    │   ├── router/         # 路由配置
    │   ├── store/          # 状态管理
    │   ├── utils/          # 工具函数
    │   └── views/          # 页面组件
    └── package.json
```

## 开发计划

### 已完成功能
- [x] 基础框架搭建
- [x] 用户认证和权限管理
- [x] 数据管理界面
- [x] 预测分析界面
- [x] 数据可视化界面
- [x] 报告管理界面
- [x] 系统设置界面

### 待开发功能
- [ ] 预测模型集成
- [ ] 数据导入功能
- [ ] 图表渲染
- [ ] 报告生成
- [ ] 文件上传下载
- [ ] 系统监控
- [ ] 数据备份

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

- 项目作者: [您的姓名]
- 邮箱: [您的邮箱]
- 项目链接: [项目地址]

---

**注意**: 这是一个毕业设计项目，目前处于开发阶段，部分功能仍在完善中。
