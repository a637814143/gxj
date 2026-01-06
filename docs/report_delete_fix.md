# 报表删除功能405错误修复

## 问题描述
用户在删除报表时遇到错误：
```
Request method 'DELETE' is not supported
```

## 问题分析

### 1. 检查项
- ✅ CORS配置：WebConfig.java中已允许DELETE方法
- ✅ Controller映射：ReportController.java中已有@DeleteMapping
- ✅ 前端请求：report.js中正确使用axios.delete
- ❌ **Security配置：缺少DELETE权限配置**

### 2. 根本原因
Spring Security的`ApplicationSecurityConfig.java`中只配置了GET和POST权限，没有配置DELETE权限：

```java
// 原配置 - 缺少DELETE
.requestMatchers(HttpMethod.GET, "/api/report/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT", "FARMER")
.requestMatchers(HttpMethod.POST, "/api/report/**", "/api/report/export/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT")
// 缺少DELETE配置！
```

## 解决方案

### 修改文件
`demo/src/main/java/com/gxj/cropyield/config/ApplicationSecurityConfig.java`

### 添加配置
在Security配置中添加DELETE权限：

```java
.requestMatchers(HttpMethod.GET, "/api/report/export/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT")
.requestMatchers(HttpMethod.POST, "/api/report/**", "/api/report/export/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT")
.requestMatchers(HttpMethod.DELETE, "/api/report/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT")  // 新增
.requestMatchers(HttpMethod.POST, "/api/datasets/**", "/api/base/**").hasRole("ADMIN")
```

### 权限说明
- **ADMIN**（系统管理员）：可以删除报表
- **AGRICULTURE_DEPT**（技术人员）：可以删除报表
- **FARMER**（普通用户）：无删除权限（前端不显示删除按钮）

## 验证步骤

### 1. 重启后端服务
```bash
cd demo
mvn spring-boot:run
```

### 2. 测试删除功能
1. 使用ADMIN或AGRICULTURE_DEPT账号登录
2. 进入报表中心
3. 点击报表的"删除"按钮
4. 确认删除操作
5. 验证报表被成功删除

### 3. 测试权限控制
1. 使用FARMER账号登录
2. 进入报表中心
3. 验证看不到"删除"按钮

## 技术要点

### Spring Security HTTP方法权限
Spring Security需要为每个HTTP方法单独配置权限：
- `HttpMethod.GET` - 查询操作
- `HttpMethod.POST` - 创建操作
- `HttpMethod.PUT` - 更新操作
- `HttpMethod.DELETE` - 删除操作
- `HttpMethod.PATCH` - 部分更新操作

### 权限配置顺序
Spring Security按照配置顺序匹配规则，更具体的规则应该放在前面：
```java
// 1. 先配置具体路径
.requestMatchers(HttpMethod.GET, "/api/report/export/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT")

// 2. 再配置通用路径
.requestMatchers(HttpMethod.GET, "/api/report/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT", "FARMER")
```

### CORS vs Security
- **CORS配置**：控制跨域请求，在WebConfig中配置
- **Security配置**：控制权限验证，在ApplicationSecurityConfig中配置
- 两者都需要正确配置，缺一不可

## 相关文档
- [报表删除功能实现文档](./report_delete_feature.md)
- [Spring Security配置](../demo/src/main/java/com/gxj/cropyield/config/ApplicationSecurityConfig.java)

---

**修复时间**: 2026-01-06  
**状态**: ✅ 已修复
