<!--
  @component DataFilterPanel
  @description 数据筛选面板组件 - 提供作物类别、作物、年份、地区的筛选功能
  @emits update:category - 类别变更
  @emits update:crop - 作物变更
  @emits update:year - 年份变更
  @emits update:region - 地区变更
  @emits update:chartMode - 图表模式变更
  @emits refresh - 刷新数据
-->
<template>
  <div class="filter-bar">
    <div class="filter-summary">
      <el-tag v-if="hasAnySelection" class="filter-tag" type="success" effect="light">
        {{ selectionTagText }}
      </el-tag>
      <span>{{ datasetSummaryText }}</span>
    </div>
    <div class="filter-controls">
      <el-select
        :model-value="chartMode"
        class="chart-mode-select"
        placeholder="选择图表视图"
        @update:model-value="$emit('update:chartMode', $event)"
      >
        <el-option
          v-for="option in chartModeOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-select
        :model-value="category"
        class="category-select"
        :disabled="!categoryOptions.length"
        placeholder="选择作物类别"
        @update:model-value="$emit('update:category', $event)"
      >
        <el-option
          v-for="option in categoryOptions"
          :key="option.value"
          :label="option.display"
          :value="option.value"
        />
      </el-select>
      <el-select
        :model-value="crop"
        class="crop-select"
        :disabled="!cropOptions.length"
        placeholder="筛选作物"
        @update:model-value="$emit('update:crop', $event)"
      >
        <el-option
          v-for="option in cropOptions"
          :key="option.value"
          :label="option.display"
          :value="option.value"
        />
      </el-select>
      <el-select
        :model-value="year"
        class="year-select"
        :disabled="!yearOptions.length"
        placeholder="筛选年份"
        @update:model-value="$emit('update:year', $event)"
      >
        <el-option
          v-for="option in yearOptions"
          :key="option.value"
          :label="option.display"
          :value="option.value"
        />
      </el-select>
      <el-select
        :model-value="region"
        class="region-select"
        :disabled="!regionOptions.length"
        placeholder="筛选地区"
        @update:model-value="$emit('update:region', $event)"
      >
        <el-option
          v-for="option in regionOptions"
          :key="option.value"
          :label="option.display"
          :value="option.value"
        />
      </el-select>
      <el-button type="primary" :loading="loading" @click="$emit('refresh')">
        刷新数据
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  category: { type: String, required: true },
  crop: { type: String, required: true },
  year: { type: String, required: true },
  region: { type: String, required: true },
  chartMode: { type: String, required: true },
  categoryOptions: { type: Array, default: () => [] },
  cropOptions: { type: Array, default: () => [] },
  yearOptions: { type: Array, default: () => [] },
  regionOptions: { type: Array, default: () => [] },
  totalRecords: { type: Number, default: 0 },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:category', 'update:crop', 'update:year', 'update:region', 'update:chartMode', 'refresh'])

const chartModeOptions = [
  { label: '展示全部图表', value: 'all' },
  { label: '仅产量趋势', value: 'trend' },
  { label: '仅结构占比', value: 'structure' },
  { label: '仅地理热力', value: 'map' }
]

const hasAnySelection = computed(() => {
  return props.category !== '__ALL__' || props.crop !== '__ALL_CROPS__' ||
    props.year !== '__ALL_YEARS__' || props.region !== '__ALL_REGIONS__'
})

const selectionTagText = computed(() => {
  const parts = []
  if (props.category !== '__ALL__') {
    const option = props.categoryOptions.find(o => o.value === props.category)
    if (option) parts.push(option.display)
  }
  if (props.crop !== '__ALL_CROPS__') {
    const option = props.cropOptions.find(o => o.value === props.crop)
    if (option) parts.push(option.display)
  }
  if (props.year !== '__ALL_YEARS__') {
    const option = props.yearOptions.find(o => o.value === props.year)
    if (option) parts.push(option.display)
  }
  if (props.region !== '__ALL_REGIONS__') {
    const option = props.regionOptions.find(o => o.value === props.region)
    if (option) parts.push(option.display)
  }
  return parts.join(' · ') || '全部数据'
})

const datasetSummaryText = computed(() => {
  return `共 ${props.totalRecords} 条记录`
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.filter-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #606266;
}

.filter-tag {
  font-weight: 500;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chart-mode-select,
.category-select,
.crop-select,
.year-select,
.region-select {
  width: 160px;
}

@media (max-width: 1400px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-summary {
    justify-content: center;
  }

  .filter-controls {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
