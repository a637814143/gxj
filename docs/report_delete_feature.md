# 报表删除功能实现文档

## 功能概述

为报表中心添加删除功能，仅对**管理员（ADMIN）**和**技术人员（AGRICULTURE_DEPT）**开放，普通用户（FARMER）不显示删除按钮。

---

## 问题修复记录

### DELETE请求405错误 ✅ 已修复
**问题**: 删除报表时出现 "Request method 'DELETE' is not supported" 错误

**原因**: Spring Security配置中缺少DELETE方法的权限配置

**解决方案**: 在 `ApplicationSecurityConfig.java` 中添加DELETE权限：
```java
.requestMatchers(HttpMethod.DELETE, "/api/report/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT")
```

**修改文件**: `demo/src/main/java/com/gxj/cropyield/config/ApplicationSecurityConfig.java`

---

## 实现内容

### 1. 后端实现 ✅

#### 1.1 Controller层
**文件**: `demo/src/main/java/com/gxj/cropyield/modules/report/controller/ReportController.java`

**新增接口**:
```java
@DeleteMapping("/{id}")
public ApiResponse<Void> deleteReport(@PathVariable Long id) {
    reportService.deleteReport(id);
    return new ApiResponse<>(200, "报告删除成功", null);
}
```

**特点**:
- RESTful风格的DELETE请求
- 路径参数传递报告ID
- 返回统一的API响应格式

---

#### 1.2 Service层
**文件**: `demo/src/main/java/com/gxj/cropyield/modules/report/service/ReportService.java`

**新增接口方法**:
```java
void deleteReport(Long id);
```

**实现**: `demo/src/main/java/com/gxj/cropyield/modules/report/service/impl/ReportServiceImpl.java`

```java
@Override
@Transactional
public void deleteReport(Long id) {
    Report report = reportRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "报告不存在"));
    reportRepository.delete(report);
}
```

**特点**:
- 使用`@Transactional`确保事务一致性
- 先检查报告是否存在，不存在则抛出业务异常
- 使用JPA的`delete`方法删除报告

---

### 2. 前端实现 ✅

#### 2.1 API服务层
**文件**: `forecast/src/services/report.js`

**新增API调用**:
```javascript
export const deleteReport = reportId => apiClient.delete(`/api/report/${reportId}`)
```

---

#### 2.2 报表列表页面
**文件**: `forecast/src/views/ReportCenterView.vue`

**权限检查**:
```javascript
// 检查用户是否有删除权限（ADMIN 或 AGRICULTURE_DEPT）
const canDeleteReport = computed(() => {
  const roles = authStore.user?.roles
  if (!roles) return false
  if (Array.isArray(roles)) {
    return roles.includes('ADMIN') || roles.includes('AGRICULTURE_DEPT')
  }
  return roles === 'ADMIN' || roles === 'AGRICULTURE_DEPT'
})
```

**删除功能**:
```javascript
const handleDeleteReport = async (reportId) => {
  if (!reportId) return
  
  deletingReportId.value = reportId
  try {
    await deleteReport(reportId)
    ElMessage.success('报告删除成功')
    // 刷新报告列表
    await fetchReports()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '删除报告失败')
  } finally {
    deletingReportId.value = null
  }
}
```

**UI展示**:
```vue
<div class="report-actions">
  <el-link type="primary" @click="viewReport(report)">查看详情</el-link>
  <el-popconfirm
    v-if="canDeleteReport"
    title="确定要删除这份报告吗？"
    confirm-button-text="确定"
    cancel-button-text="取消"
    @confirm="handleDeleteReport(report.id)"
  >
    <template #reference>
      <el-link type="danger" :loading="deletingReportId === report.id">删除</el-link>
    </template>
  </el-popconfirm>
</div>
```

**特点**:
- 使用`v-if="canDeleteReport"`控制删除按钮显示
- 使用`el-popconfirm`二次确认，防止误删
- 显示加载状态，提升用户体验
- 删除成功后自动刷新列表

---

#### 2.3 报表详情抽屉
**文件**: `forecast/src/components/report/ReportDetailDrawer.vue`

**Props扩展**:
```javascript
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  reportId: { type: [Number, String], default: null },
  summary: { type: Object, default: null },
  canDelete: { type: Boolean, default: false }  // 新增
})

const emit = defineEmits(['update:modelValue', 'delete'])  // 新增delete事件
```

**删除功能**:
```javascript
const handleDelete = () => {
  if (!props.reportId) {
    ElMessage.warning('无法删除报告')
    return
  }
  emit('delete', props.reportId)
}
```

**UI展示**:
```vue
<el-popconfirm
  v-if="canDelete"
  title="确定要删除这份报告吗？"
  confirm-button-text="确定"
  cancel-button-text="取消"
  @confirm="handleDelete"
>
  <template #reference>
    <el-button type="danger" size="small">删除</el-button>
  </template>
</el-popconfirm>
```

**特点**:
- 通过Props传递权限控制
- 通过Emit事件通知父组件执行删除
- 删除后自动关闭抽屉

---

## 权限控制

### 角色权限矩阵

| 角色 | 角色代码 | 查看报表 | 导出报表 | 删除报表 |
|-----|---------|---------|---------|---------|
| 系统管理员 | ADMIN | ✅ | ✅ | ✅ |
| 技术人员 | AGRICULTURE_DEPT | ✅ | ✅ | ✅ |
| 普通用户 | FARMER | ✅ | ✅ | ❌ |

### 权限实现方式

**前端权限控制**:
- 使用`computed`属性检查用户角色
- 通过`v-if`指令控制删除按钮显示
- 不符合权限的用户看不到删除按钮

**后端权限控制**:
- ✅ Spring Security配置中添加DELETE权限
- ✅ 仅允许ADMIN和AGRICULTURE_DEPT角色删除
- 建议增强：添加方法级权限注解

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'AGRICULTURE_DEPT')")
public ApiResponse<Void> deleteReport(@PathVariable Long id) {
    reportService.deleteReport(id);
    return new ApiResponse<>(200, "报告删除成功", null);
}
```

**Spring Security配置**:
```java
// ApplicationSecurityConfig.java
.requestMatchers(HttpMethod.DELETE, "/api/report/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT")
```

---

## 用户体验优化

### 1. 二次确认 ✅
使用`el-popconfirm`组件，防止误删：
- 点击删除按钮后弹出确认框
- 需要用户再次确认才执行删除
- 可以取消操作

### 2. 加载状态 ✅
删除过程中显示加载状态：
- 按钮显示loading状态
- 防止重复点击
- 提升用户体验

### 3. 操作反馈 ✅
- 删除成功：显示成功提示
- 删除失败：显示错误信息
- 自动刷新列表

### 4. 视觉设计 ✅
- 删除按钮使用红色（danger类型）
- 与查看详情按钮并排显示
- 间距合理，易于点击

---

## 测试场景

### 1. 权限测试
- [ ] ADMIN用户可以看到删除按钮
- [ ] AGRICULTURE_DEPT用户可以看到删除按钮
- [ ] FARMER用户看不到删除按钮

### 2. 功能测试
- [ ] 点击删除按钮弹出确认框
- [ ] 点击确定成功删除报告
- [ ] 点击取消不删除报告
- [ ] 删除成功后列表自动刷新
- [ ] 删除成功后显示成功提示

### 3. 异常测试
- [ ] 删除不存在的报告显示错误提示
- [ ] 网络错误时显示错误提示
- [ ] 删除过程中按钮显示loading状态

### 4. 边界测试
- [ ] 删除最后一页的最后一条记录后，页码自动调整
- [ ] 删除当前查看的报告后，详情抽屉自动关闭

---

## API文档

### 删除报告

**接口地址**: `DELETE /api/report/{id}`

**请求参数**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|-------|------|------|------|------|
| id | Long | Path | 是 | 报告ID |

**请求示例**:
```http
DELETE /api/report/123 HTTP/1.1
Host: localhost:8080
Authorization: Bearer {token}
```

**响应示例**:

成功响应（200）:
```json
{
  "success": true,
  "message": "报告删除成功",
  "data": null,
  "timestamp": "2026-01-06T10:30:00"
}
```

失败响应（404）:
```json
{
  "success": false,
  "message": "报告不存在",
  "code": "NOT_FOUND",
  "timestamp": "2026-01-06T10:30:00"
}
```

---

## 数据库影响

### 级联删除
当删除报告时，相关的报告章节（ReportSection）会被级联删除：

```java
// Report实体中的配置
@OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ReportSection> sections = new ArrayList<>();
```

**说明**:
- `cascade = CascadeType.ALL`: 删除报告时自动删除所有章节
- `orphanRemoval = true`: 从集合中移除章节时自动删除

---

## 安全建议

### 1. 后端权限验证 ⚠️
**当前状态**: ✅ 已在Spring Security配置  
**建议**: 添加方法级权限注解增强安全性

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'AGRICULTURE_DEPT')")
public ApiResponse<Void> deleteReport(@PathVariable Long id) {
    reportService.deleteReport(id);
    return new ApiResponse<>(200, "报告删除成功", null);
}
```

### 2. 审计日志 💡
**建议**: 记录删除操作的审计日志

```java
@Override
@Transactional
@AuditLog(action = "DELETE_REPORT", description = "删除报告")
public void deleteReport(Long id) {
    Report report = reportRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "报告不存在"));
    reportRepository.delete(report);
}
```

### 3. 软删除 💡
**建议**: 使用软删除代替物理删除

```java
@Entity
public class Report {
    // ...
    private Boolean deleted = false;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
```

---

## 文件清单

### 后端文件
- ✅ `demo/src/main/java/com/gxj/cropyield/modules/report/controller/ReportController.java`
- ✅ `demo/src/main/java/com/gxj/cropyield/modules/report/service/ReportService.java`
- ✅ `demo/src/main/java/com/gxj/cropyield/modules/report/service/impl/ReportServiceImpl.java`
- ✅ `demo/src/main/java/com/gxj/cropyield/config/ApplicationSecurityConfig.java` (权限配置)

### 前端文件
- ✅ `forecast/src/services/report.js`
- ✅ `forecast/src/views/ReportCenterView.vue`
- ✅ `forecast/src/components/report/ReportDetailDrawer.vue`

### 文档文件
- ✅ `docs/report_delete_feature.md`

---

## 总结

### 实现的功能 ✅
1. ✅ 后端删除API接口
2. ✅ 前端删除按钮（带权限控制）
3. ✅ 二次确认对话框
4. ✅ 加载状态显示
5. ✅ 操作成功/失败提示
6. ✅ 自动刷新列表
7. ✅ 详情抽屉中的删除功能

### 权限控制 ✅
- ✅ ADMIN可以删除
- ✅ AGRICULTURE_DEPT可以删除
- ✅ FARMER不显示删除按钮

### 用户体验 ✅
- ✅ 二次确认防止误删
- ✅ 加载状态提示
- ✅ 操作反馈清晰
- ✅ 视觉设计合理

---

**实现时间**: 2026-01-06  
**实现人**: 恭浩杰  
**状态**: ✅ 完成
