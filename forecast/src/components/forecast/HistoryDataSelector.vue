<!--
  @component HistoryDataSelector
  @description 历史数据选择器 - 选择用于预测的历史数据
  @emits update:selection - 数据选择变更
-->
<template>
  <el-card class="history-data-selector" shadow="never">
    <template #header>
      <div class="selector-header">
        <div>
          <div class="selector-title">选择历史数据</div>
          <div class="selector-desc">选择作物和地区的历史产量数据用于预测</div>
        </div>
        <el-tag v-if="selectedCount > 0" type="success">
          已选择 {{ selectedCount }} 条数据
        </el-tag>
      </div>
    </template>
    
    <div class="selector-filters">
      <el-select
        v-model="filters.crop"
        placeholder="选择作物"
        clearable
        filterable
        @change="handleFilterChange"
      >
        <el-option
          v-for="crop in cropOptions"
          :key="crop"
          :label="crop"
          :value="crop"
        />
      </el-select>
      
      <el-select
        v-model="filters.region"
        placeholder="选择地区"
        clearable
        filterable
        @change="handleFilterChange"
      >
        <el-option
          v-for="region in regionOptions"
          :key="region"
          :label="region"
          :value="region"
        />
      </el-select>
      
      <el-select
        v-model="filters.yearRange"
        placeholder="选择年份范围"
        @change="handleFilterChange"
      >
        <el-option label="全部年份" value="all" />
        <el-option label="最近5年" value="5" />
        <el-option label="最近10年" value="10" />
        <el-option label="最近15年" value="15" />
      </el-select>
      
      <el-button type="primary" @click="handleSelectAll">全选</el-button>
      <el-button @click="handleClearSelection">清空</el-button>
    </div>
    
    <el-table
      ref="tableRef"
      :data="filteredData"
      @selection-change="handleSelectionChange"
      max-height="400"
      border
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="cropName" label="作物" width="120" />
      <el-table-column prop="regionName" label="地区" width="120" />
      <el-table-column prop="year" label="年份" width="100" />
      <el-table-column prop="production" label="产量(万吨)" width="120">
        <template #default="{ row }">
          {{ formatNumber(row.production) }}
        </template>
      </el-table-column>
      <el-table-column prop="sownArea" label="播种面积(千公顷)" width="150">
        <template #default="{ row }">
          {{ row.sownArea ? formatNumber(row.sownArea) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="数据质量" width="100">
        <template #default="{ row }">
          <el-tag :type="getQualityType(row)" size="small">
            {{ getQualityLabel(row) }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    
    <div v-if="selectedCount > 0" class="selection-summary">
      <el-alert type="info" :closable="false">
        <template #title>
          已选择 {{ selectedCount }} 条数据，覆盖 {{ yearSpan }} 年
        </template>
        <div class="summary-details">
          <span>作物: {{ uniqueCrops.join(', ') }}</span>
          <span>地区: {{ uniqueRegions.join(', ') }}</span>
        </div>
      </el-alert>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  selection: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:selection'])

const tableRef = ref(null)
const filters = ref({
  crop: '',
  region: '',
  yearRange: 'all'
})

const selectedData = ref([...props.selection])

const cropOptions = computed(() => {
  return [...new Set(props.data.map(d => d.cropName))].filter(Boolean).sort()
})

const regionOptions = computed(() => {
  return [...new Set(props.data.map(d => d.regionName))].filter(Boolean).sort()
})

const filteredData = computed(() => {
  let result = [...props.data]
  
  if (filters.value.crop) {
    result = result.filter(d => d.cropName === filters.value.crop)
  }
  
  if (filters.value.region) {
    result = result.filter(d => d.regionName === filters.value.region)
  }
  
  if (filters.value.yearRange !== 'all') {
    const years = parseInt(filters.value.yearRange)
    const currentYear = new Date().getFullYear()
    const minYear = currentYear - years
    result = result.filter(d => d.year >= minYear)
  }
  
  return result.sort((a, b) => b.year - a.year)
})

const selectedCount = computed(() => selectedData.value.length)

const yearSpan = computed(() => {
  if (selectedCount.value === 0) return 0
  const years = selectedData.value.map(d => d.year)
  return Math.max(...years) - Math.min(...years) + 1
})

const uniqueCrops = computed(() => {
  return [...new Set(selectedData.value.map(d => d.cropName))].filter(Boolean)
})

const uniqueRegions = computed(() => {
  return [...new Set(selectedData.value.map(d => d.regionName))].filter(Boolean)
})

const formatNumber = (value) => {
  return Number(value).toFixed(2)
}

const getQualityType = (row) => {
  if (!row.production || row.production <= 0) return 'danger'
  if (!row.sownArea || row.sownArea <= 0) return 'warning'
  return 'success'
}

const getQualityLabel = (row) => {
  if (!row.production || row.production <= 0) return '缺失'
  if (!row.sownArea || row.sownArea <= 0) return '部分'
  return '完整'
}

const handleFilterChange = () => {
  // 筛选变化时清空选择
  selectedData.value = []
  emit('update:selection', [])
}

const handleSelectionChange = (selection) => {
  selectedData.value = selection
  emit('update:selection', selection)
}

const handleSelectAll = () => {
  tableRef.value?.toggleAllSelection()
}

const handleClearSelection = () => {
  tableRef.value?.clearSelection()
}

watch(() => props.selection, (newVal) => {
  selectedData.value = [...newVal]
}, { deep: true })
</script>

<style scoped>
.history-data-selector {
  margin-bottom: 20px;
}

.selector-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.selector-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.selector-desc {
  font-size: 13px;
  color: #909399;
}

.selector-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.selector-filters .el-select {
  width: 180px;
}

.selection-summary {
  margin-top: 16px;
}

.summary-details {
  margin-top: 8px;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
