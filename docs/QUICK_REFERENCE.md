# 组件快速参考指南

## 🚀 快速开始

### 导入组件
```javascript
// 可视化组件
import HeroStatsCard from '@/components/visualization/HeroStatsCard.vue'
import DataFilterPanel from '@/components/visualization/DataFilterPanel.vue'
import TrendChart from '@/components/visualization/TrendChart.vue'

// 预测组件
import ModelSelectionPanel from '@/components/forecast/ModelSelectionPanel.vue'
import ForecastTaskList from '@/components/forecast/ForecastTaskList.vue'

// 数据组件
import DataUploadPanel from '@/components/data/DataUploadPanel.vue'
import DataTableView from '@/components/data/DataTableView.vue'
```

---

## 📊 可视化组件

### HeroStatsCard - 统计卡片
```vue
<HeroStatsCard
  :stats="[
    { label: '总记录', value: '1000', sub: '条' },
    { label: '作物种类', value: '20', sub: '种' }
  ]"
  :snapshot="{ total: 1000, crops: 20, range: '2010-2023' }"
  :insights="['数据完整性良好']"
  selection-tag="全部数据"
/>
```

### DataFilterPanel - 筛选面板
```vue
<DataFilterPanel
  v-model:category="category"
  v-model:crop="crop"
  v-model:year="year"
  v-model:region="region"
  :category-options="categoryOpts"
  :crop-options="cropOpts"
  @refresh="loadData"
/>
```

### TrendChart - 趋势图
```vue
<TrendChart
  chart-type="line"
  :data="{
    xAxis: ['2020', '2021', '2022'],
    series: [{ name: '小麦', data: [100, 120, 150] }]
  }"
  :loading="false"
/>
```

### StructureChart - 结构图
```vue
<StructureChart
  chart-type="pie"
  :data="[
    { name: '小麦', value: 100 },
    { name: '玉米', value: 80 }
  ]"
/>
```

### MapChart - 地图
```vue
<MapChart
  chart-type="choropleth"
  :data="[
    { name: '昆明市', value: 100 },
    { name: '大理州', value: 80 }
  ]"
/>
```

---

## 🚀 预测组件

### ModelSelectionPanel - 模型选择
```vue
<ModelSelectionPanel
  v-model="selectedModel"
/>
<!-- selectedModel: 'ARIMA' | 'PROPHET' | 'LSTM' -->
```

### ParameterConfigForm - 参数配置
```vue
<ParameterConfigForm
  :model-type="selectedModel"
  :parameters="params"
  @update:parameters="params = $event"
/>
```

### HistoryDataSelector - 数据选择
```vue
<HistoryDataSelector
  :data="historyData"
  :selection="selectedData"
  @update:selection="selectedData = $event"
/>
```

### ForecastTaskList - 任务列表
```vue
<ForecastTaskList
  :tasks="tasks"
  :loading="loading"
  @view-result="handleViewResult"
  @delete-task="handleDelete"
/>
```

### ResultPreviewCard - 结果预览
```vue
<ResultPreviewCard
  :result="{
    cropName: '小麦',
    regionName: '昆明市',
    modelType: 'ARIMA',
    r2Score: 0.65,
    predictions: [...],
    history: [...]
  }"
/>
```

---

## 📁 数据组件

### DataUploadPanel - 数据上传
```vue
<DataUploadPanel
  @upload-success="handleUploadSuccess"
/>
```

### DataTableView - 数据表格
```vue
<DataTableView
  :data="records"
  :loading="loading"
  @edit="handleEdit"
  @delete="handleDelete"
  @export="handleExport"
/>
```

### DataEditDialog - 编辑对话框
```vue
<DataEditDialog
  :visible="dialogVisible"
  :data="editingData"
  :saving="saving"
  @save="handleSave"
  @close="dialogVisible = false"
/>
```

### DataStatisticsCard - 统计卡片
```vue
<DataStatisticsCard
  :data="records"
/>
```

---

## 🎨 常用模式

### v-model 双向绑定
```vue
<DataFilterPanel
  v-model:crop="selectedCrop"
  v-model:year="selectedYear"
/>
```

### 事件监听
```vue
<DataTableView
  @edit="row => editingData = row"
  @delete="row => deleteRecord(row.id)"
/>
```

### 条件渲染
```vue
<TrendChart
  v-if="chartMode === 'trend'"
  :data="trendData"
/>
```

### 加载状态
```vue
<DataTableView
  :loading="isLoading"
  :data="records"
/>
```

---

## 📝 类型定义

### YieldRecord
```typescript
interface YieldRecord {
  id?: number
  year: number
  cropName: string
  cropCategory: string
  regionName: string
  production: number
  sownArea?: number
  averagePrice?: number
  estimatedRevenue?: number
}
```

### ChartData
```typescript
interface TrendChartData {
  xAxis: string[]
  series: Array<{
    name: string
    data: number[]
  }>
}

interface StructureChartData {
  name: string
  value: number
}

interface MapChartData {
  name: string
  value: number
  coord?: [number, number]
}
```

### ForecastResult
```typescript
interface ForecastResult {
  cropName: string
  regionName: string
  modelType: string
  r2Score: number
  predictions: Array<{
    year: number
    value: number
    lowerBound: number
    upperBound: number
  }>
  history: Array<{
    year: number
    value: number
  }>
}
```

---

## 🔧 工具函数

### 数字格式化
```javascript
const formatNumber = (value, decimals = 2) => {
  return Number(value).toFixed(decimals)
}
```

### 日期格式化
```javascript
const formatDate = (date) => {
  return new Date(date).toLocaleDateString('zh-CN')
}
```

### 百分比格式化
```javascript
const formatPercentage = (value) => {
  return `${(value * 100).toFixed(1)}%`
}
```

---

## 🎯 最佳实践

### 1. 组件命名
```vue
<!-- ✅ 好的命名 -->
<DataFilterPanel />
<TrendChart />

<!-- ❌ 避免的命名 -->
<DFP />
<Chart1 />
```

### 2. Props 验证
```javascript
defineProps({
  data: {
    type: Array,
    required: true,
    validator: (value) => value.length > 0
  }
})
```

### 3. 事件命名
```javascript
// ✅ 使用动词
emit('update:modelValue')
emit('delete')
emit('submit')

// ❌ 避免名词
emit('data')
emit('value')
```

### 4. 响应式数据
```javascript
// ✅ 使用 ref/reactive
const data = ref([])
const form = reactive({})

// ❌ 避免直接赋值
let data = []
```

---

## 📚 相关文档

- [完整组件清单](./component_catalog.md)
- [重构总结](./frontend_refactoring_summary.md)
- [重构完成报告](./REFACTORING_COMPLETE.md)
- [代码组织分析](./frontend_code_organization_analysis.md)

---

**最后更新**: 2026-01-06  
**版本**: v1.0.0
