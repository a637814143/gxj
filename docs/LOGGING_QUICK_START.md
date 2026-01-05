# 🚀 日志系统 - 快速启动指南

## ✅ 日志系统已完成

完整的审计日志系统已经实施完成，现在只需要**重启应用**即可生效！

## 📦 已实施的功能

### 1. 审计日志系统 ✅
- 自动记录所有重要操作
- 记录用户、IP、操作类型、执行时间
- 自动过滤敏感信息（密码、token等）
- 支持数据库查询和文件查看

### 2. 日志文件分离 ✅
- 应用日志：crop-yield.log
- 错误日志：crop-yield-error.log
- 审计日志：crop-yield-audit.log
- 自动滚动，按天分割

### 3. 用户管理日志 ✅
- 创建用户：完整日志 + 审计记录
- 更新用户：变更日志 + 审计记录
- 删除用户：删除日志 + 审计记录
- 重置密码：操作日志 + 审计记录（不记录密码）

## 🎯 启动应用

### 方法1: Maven启动（推荐）

```bash
cd demo
./mvnw spring-boot:run
```

### 方法2: IDE启动

在IDE中运行 `CropYieldApplication.java`

## 📊 验证审计日志

### 1. 检查数据库表

启动后，Flyway会自动创建审计日志表：

```sql
mysql -u root -p crop_yield_db

-- 查看审计日志表结构
SHOW CREATE TABLE sys_audit_log;

-- 查看索引
SHOW INDEX FROM sys_audit_log;
```

### 2. 测试审计日志记录

```bash
# 创建用户（会自动记录审计日志）
curl -X POST http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "username": "testuser",
    "password": "Test123456",
    "fullName": "测试用户",
    "email": "test@example.com",
    "roleIds": [1]
  }'
```

### 3. 查看审计日志

#### 方式1: 查看日志文件

```bash
# 查看审计日志文件
tail -f logs/crop-yield-audit.log

# 应该看到类似输出：
# [2026-01-05 22:50:00.123] 用户: admin, 操作: CREATE_USER, 模块: 用户管理, 描述: 创建用户, 结果: SUCCESS, 耗时: 45ms, IP: 192.168.1.100
```

#### 方式2: 查询数据库

```sql
-- 查看最近的审计日志
SELECT 
    username,
    operation,
    module,
    description,
    result,
    execution_time,
    ip_address,
    created_at
FROM sys_audit_log
ORDER BY created_at DESC
LIMIT 10;

-- 查看特定用户的操作
SELECT * FROM sys_audit_log 
WHERE username = 'admin' 
ORDER BY created_at DESC;

-- 查看失败的操作
SELECT * FROM sys_audit_log 
WHERE result = 'FAILURE' 
ORDER BY created_at DESC;

-- 统计用户操作次数
SELECT username, COUNT(*) as count 
FROM sys_audit_log 
GROUP BY username 
ORDER BY count DESC;
```

## 📁 日志文件说明

### 日志文件位置

所有日志文件位于 `logs/` 目录：

```
logs/
├── crop-yield.log              # 应用日志（当前）
├── crop-yield-2026-01-05.0.log # 应用日志（历史）
├── crop-yield-error.log        # 错误日志（当前）
├── crop-yield-error-2026-01-05.log # 错误日志（历史）
├── crop-yield-audit.log        # 审计日志（当前）
└── crop-yield-audit-2026-01-05.log # 审计日志（历史）
```

### 日志保留策略

| 日志类型 | 保留时间 | 单文件大小 | 总大小限制 |
|---------|---------|-----------|-----------|
| 应用日志 | 30天 | 100MB | 10GB |
| 错误日志 | 90天 | 无限制 | 5GB |
| 审计日志 | 365天 | 无限制 | 20GB |

### 查看日志命令

```bash
# 实时查看应用日志
tail -f logs/crop-yield.log

# 实时查看错误日志
tail -f logs/crop-yield-error.log

# 实时查看审计日志
tail -f logs/crop-yield-audit.log

# 搜索特定内容
grep "用户创建" logs/crop-yield.log
grep "ERROR" logs/crop-yield.log
grep "用户: admin" logs/crop-yield-audit.log

# 查看最近100行
tail -n 100 logs/crop-yield.log

# 查看特定时间段的日志
grep "2026-01-05 22:" logs/crop-yield.log
```

## 🎯 审计日志功能

### 自动记录的操作

当前已实现审计日志的操作：

| 操作 | 操作类型 | 模块 | 记录参数 |
|------|---------|------|---------|
| 创建用户 | CREATE_USER | 用户管理 | ✅ |
| 更新用户 | UPDATE_USER | 用户管理 | ✅ |
| 删除用户 | DELETE_USER | 用户管理 | ✅ |
| 重置密码 | RESET_PASSWORD | 用户管理 | ❌（敏感） |

### 审计日志内容

每条审计日志包含：

```json
{
  "username": "admin",           // 操作用户
  "operation": "CREATE_USER",    // 操作类型
  "module": "用户管理",           // 模块名称
  "description": "创建用户",      // 操作描述
  "ipAddress": "192.168.1.100",  // IP地址
  "userAgent": "Mozilla/5.0...", // 浏览器信息
  "requestUri": "/api/admin/users", // 请求URI
  "requestMethod": "POST",       // 请求方法
  "requestParams": "{...}",      // 请求参数（JSON）
  "result": "SUCCESS",           // 操作结果
  "executionTime": 45,           // 执行时间（毫秒）
  "createdAt": "2026-01-05 22:50:00"
}
```

### 敏感信息保护

审计日志会自动过滤以下敏感参数：
- password → ******
- pwd → ******
- secret → ******
- token → ******
- key → ******

## 🔍 日志查询示例

### 1. 查看用户的所有操作

```sql
SELECT 
    operation,
    module,
    description,
    result,
    created_at
FROM sys_audit_log
WHERE username = 'admin'
ORDER BY created_at DESC;
```

### 2. 查看失败的操作

```sql
SELECT 
    username,
    operation,
    module,
    error_message,
    created_at
FROM sys_audit_log
WHERE result = 'FAILURE'
ORDER BY created_at DESC;
```

### 3. 统计操作类型分布

```sql
SELECT 
    operation,
    COUNT(*) as count,
    AVG(execution_time) as avg_time
FROM sys_audit_log
GROUP BY operation
ORDER BY count DESC;
```

### 4. 查看慢操作

```sql
SELECT 
    username,
    operation,
    module,
    execution_time,
    created_at
FROM sys_audit_log
WHERE execution_time > 1000  -- 超过1秒
ORDER BY execution_time DESC;
```

### 5. 查看特定时间段的操作

```sql
SELECT * FROM sys_audit_log
WHERE created_at BETWEEN '2026-01-05 00:00:00' AND '2026-01-05 23:59:59'
ORDER BY created_at DESC;
```

## 📝 为新方法添加审计日志

### 步骤1: 添加@AuditLog注解

```java
@Service
public class YourService {
    
    private static final Logger log = LoggerFactory.getLogger(YourService.class);
    
    @AuditLog(
        operation = "YOUR_OPERATION",    // 操作类型（大写，下划线分隔）
        module = "模块名称",              // 模块名称（中文）
        description = "操作描述",         // 操作描述（中文）
        recordParams = true,             // 是否记录参数（默认true）
        recordResult = false             // 是否记录结果（默认false）
    )
    public Result yourMethod(Request request) {
        // 方法实现
    }
}
```

### 步骤2: 添加详细日志

```java
@AuditLog(operation = "CREATE_ENTITY", module = "实体管理", description = "创建实体")
public Entity createEntity(EntityRequest request) {
    log.info("开始创建实体 - 名称: {}", request.getName());
    
    try {
        // 业务逻辑
        Entity entity = save(request);
        
        log.info("实体创建成功 - ID: {}, 名称: {}", entity.getId(), entity.getName());
        return entity;
        
    } catch (Exception e) {
        log.error("实体创建失败 - 名称: {}, 错误: {}", request.getName(), e.getMessage(), e);
        throw e;
    }
}
```

## ⚠️ 注意事项

### 1. 敏感操作不记录参数

对于包含密码、token等敏感信息的操作，设置 `recordParams = false`：

```java
@AuditLog(
    operation = "RESET_PASSWORD",
    module = "用户管理",
    description = "重置密码",
    recordParams = false  // 不记录参数
)
public void resetPassword(Long userId, String newPassword) {
    // ...
}
```

### 2. 日志文件大小

- 日志文件会自动滚动，无需手动管理
- 超过大小限制会自动分割
- 超过保留时间会自动删除

### 3. 性能影响

- 审计日志采用异步保存，性能影响很小
- 建议生产环境关闭SQL DEBUG日志
- 可以通过配置调整日志级别

### 4. 日志备份

建议定期备份审计日志：

```bash
# 备份审计日志
tar -czf audit-logs-$(date +%Y%m%d).tar.gz logs/crop-yield-audit-*.log

# 清理旧的备份（保留90天）
find . -name "audit-logs-*.tar.gz" -mtime +90 -delete
```

## 🎉 完成！

日志系统已经完全配置好，重启应用即可使用！

**核心功能**：
- ✅ 完整的审计日志系统
- ✅ 自动记录所有重要操作
- ✅ 日志文件自动分离和滚动
- ✅ 敏感信息自动过滤
- ✅ 支持数据库查询和文件查看

**下一步**：
1. 重启应用
2. 测试用户操作
3. 查看审计日志
4. 根据需要为其他Service添加审计日志

---

**状态**: ✅ 就绪  
**下一步**: 重启应用  
**预期效果**: 完整的操作审计追踪
