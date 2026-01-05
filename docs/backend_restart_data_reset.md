# 后端重启时数据重置分析

## 概述
本文档说明后端重启时哪些数据会被重置/更新，哪些数据会保留。

---

## 配置说明

### Spring Boot SQL 初始化配置
```yaml
spring:
  sql:
    init:
      mode: always              # 每次启动都执行 SQL 脚本
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
  jpa:
    hibernate:
      ddl-auto: none           # 不自动生成表结构
```

**关键点：** `mode: always` 意味着每次后端启动时都会执行 `schema.sql` 和 `data.sql`

---

## 一、每次重启都会重置/更新的数据

### 1. 系统权限 (sys_permission)
**表：** `sys_permission`  
**重置数据：**
- DASHBOARD_VIEW - 仪表盘查看
- DATA_MANAGE - 数据管理
- FORECAST_MANAGE - 预测任务管理

**行为：** `ON DUPLICATE KEY UPDATE` - 如果存在则更新名称和描述

---

### 2. 系统角色 (sys_role)
**表：** `sys_role`  
**重置数据：**
- ADMIN - 系统管理员
- AGRICULTURE_DEPT - 农业部门用户
- FARMER - 种植户用户

**行为：** `ON DUPLICATE KEY UPDATE` - 如果存在则更新名称和描述

---

### 3. 角色权限绑定 (sys_role_permission)
**表：** `sys_role_permission`  
**重置数据：**
- 管理员角色绑定所有权限
- 农业部门角色绑定：DASHBOARD_VIEW, DATA_MANAGE, FORECAST_MANAGE
- 农户角色绑定：DASHBOARD_VIEW

**行为：** `ON DUPLICATE KEY UPDATE` - 每次重启都会重新绑定

---

### 4. 管理员账号 (sys_user)
**表：** `sys_user`  
**重置数据：**
- admin (id=1) - 系统管理员

**行为：** 
- `ON DUPLICATE KEY UPDATE` - 更新全名和邮箱
- **密码不会被 data.sql 重置**，但会被 `AuthDataInitializer` 处理（见下文）

---

### 5. 管理员角色绑定 (sys_user_role)
**表：** `sys_user_role`  
**重置数据：**
- admin 绑定 ADMIN 角色

**行为：** `ON DUPLICATE KEY UPDATE` - 每次重启都会重新绑定

---

### 6. 预测模型定义 (forecast_model)
**表：** `forecast_model`  
**重置数据（通过 Java 代码）：**
- DeepLearning4j LSTM 产量模型
- 天气因子多元回归模型
- ARIMA 季节性时序模型
- Prophet 事件影响模型

**行为：** `ForecastModelDataInitializer` 类在启动时执行
- 如果模型不存在则插入
- 如果模型存在则更新名称、类型和描述

---

### 7. 管理员账号密码重置
**表：** `sys_user`  
**特殊行为（通过 Java 代码）：**

`AuthDataInitializer` 类在启动时执行：
1. **密码加密：** 检查所有用户的密码，如果未加密则自动加密
2. **管理员密码重置：** 如果 admin 账号的密码不是 `Admin@123`，则自动重置为默认密码
3. **管理员角色修复：** 如果 admin 账号缺少 ADMIN 角色，则自动添加

**⚠️ 重要：** 如果你修改了 admin 账号的密码，每次重启后都会被重置为 `Admin@123`

---

## 二、不会被重置的数据（用户创建的数据）

以下数据在重启后会保留：

### 1. 基础数据
- 用户添加的行政区域 (base_region)
- 用户添加的农作物信息 (base_crop)

### 2. 用户创建的数据
### 2. 用户账号数据
- 新增的用户账号（除了 admin 管理员账号）
- 用户修改的个人信息（除了 admin 的全名和邮箱）
- 用户的登录日志 (sys_login_log)
- 用户的刷新令牌 (sys_refresh_token)
- 邮箱验证码记录 (auth_email_verification_code)

### 3. 业务数据
- 用户上传的所有数据集文件 (dataset_file)
- 用户导入的所有产量记录 (dataset_yield_record)
- 用户导入的所有气象记录 (dataset_weather_record)
- 数据导入任务记录 (data_import_job)
- 数据导入错误记录 (data_import_job_error)

### 4. 预测相关数据
- 预测任务配置 (forecast_task)
- 预测执行记录 (forecast_run)
- 预测结果时间序列 (forecast_run_series)
- 预测结果快照 (forecast_snapshot)
- 预测结果摘要 (forecast_result)
- 模型注册信息 (model_registry)

### 5. 报告数据
- 预测报告摘要 (report_summary)
- 预测报告章节 (report_section)

### 6. 咨询数据
- 用户创建的所有咨询会话 (consultation)
- 用户发送的所有咨询消息 (consultation_message)
- 咨询参与者记录 (consultation_participant)

### 7. 系统配置
- 系统设置 (system_setting)
- 系统操作日志 (sys_log)

---

## 三、如何防止数据重置

### 方案1：修改 SQL 初始化模式（推荐）
修改 `application.yml`：
```yaml
spring:
  sql:
    init:
      mode: never  # 改为 never，不再执行 SQL 脚本
```

**优点：** 完全停止数据重置  
**缺点：** 新部署时需要手动初始化数据库

---

### 方案2：使用条件初始化
修改 `application.yml`：
```yaml
spring:
  sql:
    init:
      mode: ${SQL_INIT_MODE:never}  # 通过环境变量控制
```

启动时设置环境变量：
```bash
# 首次部署时
SQL_INIT_MODE=always java -jar app.jar

# 后续启动时
java -jar app.jar  # 默认为 never
```

---

### 方案3：修改 data.sql 使用 INSERT IGNORE
将 `data.sql` 中的 `ON DUPLICATE KEY UPDATE` 改为 `INSERT IGNORE`：
```sql
-- 修改前
INSERT INTO base_region (id, code, name, level, parent_code, parent_name, description)
VALUES (1, 'YUNNAN', '云南省', 'PROVINCE', NULL, NULL, '云南省省级行政区。')
ON DUPLICATE KEY UPDATE name = VALUES(name), ...;

-- 修改后
INSERT IGNORE INTO base_region (id, code, name, level, parent_code, parent_name, description)
VALUES (1, 'YUNNAN', '云南省', 'PROVINCE', NULL, NULL, '云南省省级行政区。');
```

**优点：** 只在数据不存在时插入，不会更新已有数据  
**缺点：** 需要修改所有 INSERT 语句

---

### 方案4：禁用管理员密码重置
修改 `AuthDataInitializer.java` 的 `refreshAdminAccount` 方法：
```java
private User refreshAdminAccount(User admin, Role adminRole) {
    boolean requiresSave = false;

    // 注释掉密码重置逻辑
    /*
    if (!passwordEncoder.matches(DEFAULT_ADMIN_PASSWORD, admin.getPassword())) {
        admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
        logger.warn("检测到管理员密码不是默认口令，已自动重置为默认密码以便登录。请尽快修改密码。");
        requiresSave = true;
    }
    */

    if (!admin.getRoles().contains(adminRole)) {
        var updatedRoles = new java.util.HashSet<>(admin.getRoles());
        updatedRoles.add(adminRole);
        admin.setRoles(updatedRoles);
        logger.warn("检测到管理员角色缺失，已重新绑定 ADMIN 角色。");
        requiresSave = true;
    }

    return requiresSave ? userRepository.save(admin) : admin;
}
```

---

## 四、总结

### 会被重置的核心数据
1. ✅ 系统权限和角色定义
2. ✅ 角色权限绑定关系
3. ✅ 管理员账号（admin）的全名和邮箱
4. ✅ 管理员密码（会被重置为 Admin@123）
5. ✅ 管理员角色绑定
6. ✅ 预测模型定义（4个模型）

### 不会被重置的核心数据
1. ❌ 基础数据（行政区域、农作物）- 由用户自行添加
2. ❌ 用户创建的业务数据（预测任务、报告、咨询等）
3. ❌ 用户上传的数据集和记录
4. ❌ 新增的用户账号
5. ❌ 系统配置和日志

### 建议
- **生产环境：** 使用方案1或方案2，将 `spring.sql.init.mode` 设置为 `never`
- **开发环境：** 保持 `always` 以便快速重置测试数据
- **管理员密码：** 如果需要修改密码，建议同时修改 `AuthDataInitializer` 中的密码重置逻辑
