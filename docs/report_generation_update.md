# 报告生成功能优化

## 修改概述

将报告生成功能从"报告中心"移至"预测中心"，使用户可以直接从预测记录生成报告，简化操作流程。

## 问题修复

### 问题 1：报告详情显示空白
**原因**：从预测中心跳转到报告中心时，报告列表还未刷新，导致 `activeReportSummary` 为空。

**解决方案**：在报告中心监听 `reportId` 查询参数时，先刷新报告列表，然后从列表中查找对应的报告摘要。

### 问题 2：部门用户菜单栏仍有"生成报告"按钮
**原因**：只删除了侧边栏快捷操作的按钮，但 UserLayout 中的快捷面板还保留着。

**解决方案**：从 UserLayout 的 `quickActionConfigs` 中移除"生成报告"配置项。

## 修改内容

### 1. 前端修改

#### 1.1 预测中心 (ForecastCenterView.vue)

**新增功能：**
- 在预测记录表格的操作列添加"生成报告"按钮
- 添加 `generatingReportRunId` 状态变量，用于显示加载状态
- 导入 `generateReport` API 和 `useRouter`

**新增函数：**
```javascript
const handleGenerateReport = async row => {
  // 验证必要字段
  if (!row?.forecastResultId) {
    ElMessage.warning('该预测记录缺少结果 ID，无法生成报告')
    return
  }
  
  if (!row?.cropId || !row?.regionId) {
    ElMessage.warning('该预测记录缺少作物或区域信息，无法生成报告')
    return
  }

  // 调用报告生成 API
  const payload = {
    cropId: row.cropId,
    regionId: row.regionId,
    startYear: nowYear - 4,
    endYear: nowYear,
    includeForecastComparison: true,
    forecastResultId: row.forecastResultId,
    title: `${row.regionName}${row.cropName}产量分析报告`,
    description: `基于预测结果 #${row.forecastResultId} 自动生成的专题分析`,
    author: null
  }

  // 生成成功后跳转到报告中心
  router.push({ 
    name: 'report',
    query: { reportId: detail?.summary?.id }
  })
}
```

#### 1.2 报告中心 (ReportCenterView.vue)

**移除功能：**
- 移除页面头部的"生成报告"按钮
- 移除 `ReportGenerateDialog` 组件导入
- 移除 `showGenerateDialog` 状态变量
- 移除 `canGenerateReport` 权限检查
- 移除 `createReport` 和 `handleGenerateSuccess` 函数
- 移除 `action=generate` 的路由监听

**新增功能：**
- 添加 `reportId` 查询参数监听，支持从预测中心跳转后自动打开报告详情
- **修复**：在打开报告详情前先刷新报告列表，确保新生成的报告在列表中

```javascript
watch(
  () => route.query.reportId,
  async reportId => {
    if (reportId) {
      const id = Number(reportId)
      if (!Number.isNaN(id)) {
        // 先刷新报告列表，确保新生成的报告在列表中
        await fetchReports()
        // 从列表中查找对应的报告摘要
        const report = reports.value.find(r => r.id === id)
        if (report) {
          activeReportSummary.value = report
        }
        activeReportId.value = id
        showDetailDrawer.value = true
      }
      router.replace({ query: {} }).catch(() => {})
    }
  },
  { immediate: true }
)
```

#### 1.3 侧边栏快捷操作 (SideQuickActions.vue)

**移除功能：**
- 移除"生成报告"快捷按钮
- 移除"报告列表"快捷按钮
- 简化 `visibleActions` 和 `handleAction` 逻辑

#### 1.4 用户布局 (UserLayout.vue)

**移除功能：**
- 从 `quickActionConfigs` 中移除"生成报告"配置项
- 移除 `navigateToReport` 函数
- 简化 `quickActions` 过滤逻辑
- 简化 `handleAction` 函数

**修改前：**
```javascript
const quickActionConfigs = computed(() => [
  // ... 其他配置
  { key: 'report-generate', label: '生成报告', icon: '📝', type: 'generate', accent: 'forest' },
  // ...
])

const quickActions = computed(() =>
  quickActionConfigs.value.filter(action => {
    if (action.type === 'route') {
      return canAccessRoute(action.name)
    }
    if (action.type === 'generate') {
      return canAccessRoute('report') && hasRole(['ADMIN', 'AGRICULTURE_DEPT'])
    }
    return true
  })
)
```

**修改后：**
```javascript
const quickActionConfigs = computed(() => [
  // ... 其他配置（移除了 report-generate）
])

const quickActions = computed(() =>
  quickActionConfigs.value.filter(action => {
    if (action.type === 'route') {
      return canAccessRoute(action.name)
    }
    return true
  })
)
```

### 2. 后端修改

#### 2.1 ForecastHistoryResponse.java

**新增字段：**
```java
public record ForecastHistoryResponse(
    Long runId,
    Long forecastResultId,
    String period,
    Integer year,
    Long regionId,        // 新增：区域 ID
    String regionName,
    Long cropId,          // 新增：作物 ID
    String cropName,
    String modelName,
    String modelType,
    String measurementLabel,
    String measurementUnit,
    Double measurementValue,
    Double predictedProduction,
    Double predictedYield,
    Double sownArea,
    Double averagePrice,      // 新增：平均价格（暂时为 null）
    Double estimatedRevenue,  // 新增：预计收益（暂时为 null）
    LocalDateTime generatedAt
) {}
```

**修改原因：**
- 前端生成报告需要 `cropId` 和 `regionId`
- 添加 `averagePrice` 和 `estimatedRevenue` 字段以支持未来的收益分析功能

#### 2.2 ForecastHistoryServiceImpl.java

**修改 mapSnapshot 方法：**
```java
private ForecastHistoryResponse mapSnapshot(ForecastSnapshot snapshot) {
    ForecastRun run = snapshot.getRun();
    LocalDateTime generatedAt = run.getUpdatedAt();
    Long forecastResultId = resolveForecastResultId(run, snapshot);
    return new ForecastHistoryResponse(
        run.getId(),
        forecastResultId,
        snapshot.getPeriod(),
        snapshot.getYear(),
        run.getRegion().getId(),      // 新增
        run.getRegion().getName(),
        run.getCrop().getId(),        // 新增
        run.getCrop().getName(),
        run.getModel().getName(),
        run.getModel().getType().name(),
        snapshot.getMeasurementLabel(),
        snapshot.getMeasurementUnit(),
        snapshot.getMeasurementValue(),
        snapshot.getPredictedProduction(),
        snapshot.getPredictedYield(),
        snapshot.getSownArea(),
        null,  // averagePrice - 可以后续从价格记录表查询
        null,  // estimatedRevenue - 可以后续计算
        generatedAt
    );
}
```

## 用户体验改进

### 修改前：
1. 用户在预测中心生成预测
2. 复制预测结果 ID
3. 导航到报告中心
4. 点击"生成报告"按钮
5. 在对话框中填写信息并粘贴预测结果 ID
6. 提交生成报告

### 修改后：
1. 用户在预测中心生成预测
2. 直接点击预测记录旁的"生成报告"按钮
3. 系统自动生成报告并跳转到报告详情（显示完整内容）

**操作步骤减少：从 6 步减少到 3 步**

## 技术要点

1. **数据完整性**：后端返回的预测历史记录必须包含 `cropId`、`regionId` 和 `forecastResultId`
2. **错误处理**：前端对缺失字段进行验证，给出明确的错误提示
3. **用户反馈**：生成报告时显示加载状态，成功后自动跳转
4. **报告详情加载**：跳转到报告中心时先刷新列表，确保新报告的摘要信息可用
5. **向后兼容**：保留报告中心的查看功能，只是移除了手动生成入口

## 测试建议

1. 测试预测记录中有完整数据的情况
2. 测试预测记录缺少 `forecastResultId` 的情况
3. 测试预测记录缺少 `cropId` 或 `regionId` 的情况
4. 测试生成报告后的跳转功能
5. 测试报告详情是否正确显示（不再是空白）
6. 测试报告中心的查看功能是否正常
7. 测试部门用户（AGRICULTURE_DEPT）菜单栏是否已移除"生成报告"按钮

## 后续优化建议

1. 在 `ForecastSnapshot` 实体中添加 `averagePrice` 和 `estimatedRevenue` 字段
2. 在预测执行时自动计算并保存这些字段
3. 支持批量生成报告功能
4. 添加报告生成进度提示
5. 优化报告详情的加载性能
