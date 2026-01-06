# 前端组件清单

## 组件总览

本文档列出了所有拆分后的可复用组件，包括组件的功能、Props、Emits 和使用示例。

---

## 可视化组件 (visualization/)

### 1. HeroStatsCard.vue
**路径**: `forecast/src/components/visualization/HeroStatsCard.vue`  
**功能**: 顶部统计卡片，展示关键指标和智能洞察

**Props**:
```typescript
{
  stats: Array<{
    label: string
    value: string
    sub: string
  }>
  snapshot: {
    total: number
    crops: number
    range: string
  }
  insights: string[]
  selectionTag: string
}
```

**使用示例**:
```vue
<HeroStatsCard
  :stats="highlightStats"
  :snapshot="{ total: 1000, crops: 20, range: '2010-2023' }"
  :insights="['数据完整性良好', '建议关注小麦产量']"
  selection-tag="全部数据"
/>
```

---

### 2. DataFilterPanel.vue
**路径**: `forecast/src/components/visualization/DataFilterPanel.vue`  
**功能**: 数据筛选面板，提供多维度筛选功能

**Props**:
```typescript
{
  category: string
  crop: string
  year: string
  region: string
  chartMode: string
  categoryOptions: Array<{ value: string, display: string }>
  cropOptions: Array<{ value: string, display: string }>
  yearOptions: Array<{ value: string, display: string }>
  regionOptions: Array<{ value: string, display: string }>
  totalRecords: number
  loading: boolean
}
```

**Emits**:
- `update:category` - 类别变更
- `update:crop` - 作物变更
- `update:year` - 年份变更
- `update:region` - 地区变更
- `update:chartMode` - 图表模式变更
- `refresh` - 刷新数据

**使用示例**:
```vue
<DataFilterPanel
  v-model:category="selectedCategory"
  v-model:crop="selectedCrop"
  v-model:year="selectedYear"
  v-model:region="selectedRegion"
  v-model:chart-mode="selectedChartMode"
  :category-options="categoryOptions"
  :crop-options="cropOptions"
  :year-options="yearOptions"
  :region-options="regionOptions"
  :total-records="1000"
  :loading="isLoading"
  @refresh="loadData"
/>
```

---

### 3. SmartRecommendationPanel.vue
**路径**: `forecast/src/components/visualization/SmartRecommendationPanel.vue`  
**功能**: 智能图表推荐面板

**Props**:
```typescript
{
  smartEnabled: boolean
  trendType: string
  structureType: string
  mapType: string
  layoutTemplate: string
  recommendedTrendType: string
  recommendedStructureType: string
  recommendedMapType: string
  recommendationMessages: string[]
}
```

**Emits**:
- `update:smartEnabled` - 智能推荐开关
- `update:trendType` - 趋势图类型
- `update:structureType` - 结构图类型
- `update:mapType` - 地图类型
- `update:layoutTemplate` - 布局模板

---

### 4. TrendChart.vue
**路径**: `forecast/src/components/visualization/TrendChart.vue`  
**功能**: 产量趋势图表（折线图/面积图/柱状图）

**Props**:
```typescript
{
  chartType: 'line' | 'area' | 'bar'
  data: {
    xAxis: string[]
    series: Array<{
      name: string
      data: number[]
    }>
  }
  loading: boolean
  subtitle: string
}
```

---

### 5. StructureChart.vue
**路径**: `forecast/src/components/visualization/StructureChart.vue`  
**功能**: 结构占比图表（饼图/旭日图/雷达图）

**Props**:
```typescript
{
  chartType: 'pie' | 'sunburst' | 'radar'
  data: Array<{
    name: string
    value: number
  }>
  loading: boolean
}
```

---

### 6. MapChart.vue
**路径**: `forecast/src/components/visualization/MapChart.vue`  
**功能**: 地理分布图表（分级着色地图/热力图）

**Props**:
```typescript
{
  chartType: 'choropleth' | 'heatmap'
  data: Array<{
    name: string
    value: number
    coord?: [number, number]
  }>
  loading: boolean
}
```

---

## 预测组件 (forecast/)

### 1. ModelSelectionPanel.vue
**路径**: `forecast/src/components/forecast/ModelSelectionPanel.vue`  
**功能**: 模型选择面板

**Props**:
```typescript
{
  modelValue: string  // 'ARIMA' | 'PROPHET' | 'LSTM' | 'ENSEMBLE'
}
```

**Emits**:
- `update:modelValue` - 模型选择变更

**使用示例**:
```vue
<ModelSelectionPanel
  v-model="selectedModel"
/>
```

---

### 2. ParameterConfigForm.vue
**路径**: `forecast/src/components/forecast/ParameterConfigForm.vue`  
**功能**: 参数配置表单

**Props**:
```typescript
{
  modelType: string
  parameters: {
    forecastSteps?: number
    confidenceLevel?: number
    arimaP?: number
    arimaD?: number
    arimaQ?: number
    lstmEpochs?: number
    lstmHiddenSize?: number
    lstmLearningRate?: number
    lstmDropout?: number
    prophetSeasonality?: string
    prophetYearlySeason?: boolean
    prophetWeeklySeason?: boolean
  }
}
```

**Emits**:
- `update:parameters` - 参数变更

---

### 3. HistoryDataSelector.vue
**路径**: `forecast/src/components/forecast/HistoryDataSelector.vue`  
**功能**: 历史数据选择器

**Props**:
```typescript
{
  data: Array<YieldRecord>
  selection: Array<YieldRecord>
}
```

**Emits**:
- `update:selection` - 数据选择变更

---

### 4. ForecastTaskList.vue
**路径**: `forecast/src/components/forecast/ForecastTaskList.vue`  
**功能**: 预测任务列表

**Props**:
```typescript
{
  tasks: Array<{
    id: number
    modelType: string
    cropName: string
    regionName: string
    status: string
    progress: number
    r2Score: number
    createdAt: string
  }>
  loading: boolean
}
```

**Emits**:
- `view-result` - 查看结果
- `delete-task` - 删除任务
- `cancel-task` - 取消任务
- `retry-task` - 重试任务
- `refresh` - 刷新列表

---

### 5. ResultPreviewCard.vue
**路径**: `forecast/src/components/forecast/ResultPreviewCard.vue`  
**功能**: 预测结果预览卡片

**Props**:
```typescript
{
  result: {
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
}
```

---

## 数据中心组件 (data/)

### 1. DataUploadPanel.vue
**路径**: `forecast/src/components/data/DataUploadPanel.vue`  
**功能**: 数据上传面板

**Emits**:
- `upload-success` - 上传成功

**使用示例**:
```vue
<DataUploadPanel
  @upload-success="handleUploadSuccess"
/>
```

---

### 2. DataTableView.vue
**路径**: `forecast/src/components/data/DataTableView.vue`  
**功能**: 数据表格视图

**Props**:
```typescript
{
  data: Array<YieldRecord>
  loading: boolean
}
```

**Emits**:
- `edit` - 编辑数据
- `delete` - 删除数据
- `batch-delete` - 批量删除
- `export` - 导出数据
- `selection-change` - 选择变更

---

### 3. DataEditDialog.vue
**路径**: `forecast/src/components/data/DataEditDialog.vue`  
**功能**: 数据编辑对话框

**Props**:
```typescript
{
  visible: boolean
  data: YieldRecord | null
  saving: boolean
}
```

**Emits**:
- `save` - 保存数据
- `close` - 关闭对话框

**使用示例**:
```vue
<DataEditDialog
  :visible="dialogVisible"
  :data="editingData"
  :saving="isSaving"
  @save="handleSave"
  @close="dialogVisible = false"
/>
```

---

### 4. DataStatisticsCard.vue
**路径**: `forecast/src/components/data/DataStatisticsCard.vue`  
**功能**: 数据统计卡片

**Props**:
```typescript
{
  data: Array<YieldRecord>
}
```

**使用示例**:
```vue
<DataStatisticsCard
  :data="yieldRecords"
/>
```

---

## 组件使用指南

### 导入组件

```javascript
// 可视化组件
import HeroStatsCard from '@/components/visualization/HeroStatsCard.vue'
import DataFilterPanel from '@/components/visualization/DataFilterPanel.vue'
import SmartRecommendationPanel from '@/components/visualization/SmartRecommendationPanel.vue'
import TrendChart from '@/components/visualization/TrendChart.vue'
import StructureChart from '@/components/visualization/StructureChart.vue'
import MapChart from '@/components/visualization/MapChart.vue'

// 预测组件
import ModelSelectionPanel from '@/components/forecast/ModelSelectionPanel.vue'
import ParameterConfigForm from '@/components/forecast/ParameterConfigForm.vue'
import HistoryDataSelector from '@/components/forecast/HistoryDataSelector.vue'
import ForecastTaskList from '@/components/forecast/ForecastTaskList.vue'
import ResultPreviewCard from '@/components/forecast/ResultPreviewCard.vue'

// 数据中心组件
import DataUploadPanel from '@/components/data/DataUploadPanel.vue'
import DataTableView from '@/components/data/DataTableView.vue'
import DataEditDialog from '@/components/data/DataEditDialog.vue'
import DataStatisticsCard from '@/components/data/DataStatisticsCard.vue'
```

### 组件命名规范

所有组件遵循以下命名规范：
- 使用 PascalCase 命名
- 文件名与组件名一致
- 组件名要描述性强，避免缩写

### 组件文档注释

每个组件都包含详细的文档注释：
```vue
<!--
  @component ComponentName
  @description 组件描述
  @props
    - propName: 属性描述
  @emits
    - eventName: 事件描述
  @example
    <ComponentName :prop="value" @event="handler" />
-->
```

---

## 组件依赖关系

### 可视化组件依赖
- ECharts (图表渲染)
- Element Plus (UI组件)
- Vuedraggable (拖拽排序)

### 预测组件依赖
- ECharts (结果图表)
- Element Plus (UI组件)

### 数据中心组件依赖
- Element Plus (UI组件)
- Axios (文件上传)

---

## 组件测试

### 单元测试示例

```javascript
import { mount } from '@vue/test-utils'
import TrendChart from '@/components/visualization/TrendChart.vue'

describe('TrendChart', () => {
  it('renders correctly', () => {
    const wrapper = mount(TrendChart, {
      props: {
        chartType: 'line',
        data: {
          xAxis: ['2020', '2021', '2022'],
          series: [{
            name: '小麦',
            data: [100, 120, 150]
          }]
        }
      }
    })
    expect(wrapper.find('.chart').exists()).toBe(true)
  })
})
```

---

## 组件性能优化

### 1. 懒加载
```javascript
const TrendChart = defineAsyncComponent(() =>
  import('@/components/visualization/TrendChart.vue')
)
```

### 2. 防抖/节流
```javascript
import { debounce } from 'lodash-es'

const handleResize = debounce(() => {
  chartInstance.value?.resize()
}, 200)
```

### 3. 虚拟滚动
对于大数据量表格，使用虚拟滚动优化性能。

---

## 组件维护指南

### 添加新组件
1. 在对应目录创建组件文件
2. 添加文档注释
3. 编写单元测试
4. 更新本文档

### 修改现有组件
1. 确保向后兼容
2. 更新文档注释
3. 更新单元测试
4. 更新本文档

### 删除组件
1. 检查依赖关系
2. 通知使用方
3. 提供迁移方案
4. 更新本文档

---

## 总结

### 组件统计
- **可视化组件**: 6个
- **预测组件**: 5个
- **数据中心组件**: 4个
- **总计**: 15个可复用组件

### 代码质量
- ✅ 所有组件都有文档注释
- ✅ 遵循统一的命名规范
- ✅ Props 和 Emits 定义清晰
- ✅ 支持 TypeScript 类型推导

### 复用性
- ✅ 单一职责原则
- ✅ 高内聚低耦合
- ✅ 可在多个视图中复用
- ✅ 易于测试和维护

---

**文档创建时间**: 2026-01-06  
**组件总数**: 15个  
**覆盖视图**: 3个（VisualizationCenterView, ForecastCenterView, DataCenterView）  
**维护人**: 恭浩杰
