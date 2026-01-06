<template>
  <div :class="['forecast-visualization-page', isUserTheme ? 'user-theme' : 'admin-theme']">
    <section class="hero-card">
      <div class="hero-text">
        <div class="hero-badge">数据预测洞察</div>
        <h1>预测结果可视化中心</h1>
        <p>
          将预测中心生成的历史运行记录快速转换为交互式图表，灵活切换图表类型、分组维度与地区/作物筛选，帮助不同角色更直观地理解模型输出。
        </p>
        <div class="hero-stats">
          <div v-for="item in highlightStats" :key="item.label" class="hero-stat">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-sub">{{ item.sub }}</div>
          </div>
        </div>
      </div>
      <div class="hero-illustration">
        <div class="chart-placeholder">📈</div>
        <div class="chart-caption">来自预测中心的最新记录将自动在此联动展示</div>
      </div>
    </section>

    <el-card class="control-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">可视化配置</div>
            <div class="card-subtitle">挑选需要分析的预测记录、图表类型与分组维度</div>
          </div>
          <el-button :loading="loading" type="primary" @click="loadVisualizationData">刷新预测记录</el-button>
        </div>
      </template>
      <div class="control-grid">
        <div class="control-field">
          <div class="control-label">地区</div>
          <el-select
            v-model="selectedRegions"
            multiple
            collapse-tags
            :max-collapse-tags="2"
            placeholder="选择地区"
            :disabled="!regionOptions.length"
          >
            <el-option v-for="option in regionOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </div>
        <div class="control-field">
          <div class="control-label">作物</div>
          <el-select
            v-model="selectedCrops"
            multiple
            collapse-tags
            :max-collapse-tags="2"
            placeholder="选择作物"
            :disabled="!cropOptions.length"
          >
            <el-option v-for="option in cropOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </div>
        <div class="control-field">
          <div class="control-label">模型</div>
          <el-select
            v-model="selectedModels"
            multiple
            collapse-tags
            :max-collapse-tags="2"
            placeholder="选择模型"
            :disabled="!modelOptions.length"
          >
            <el-option v-for="option in modelOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </div>
        <div class="control-field">
          <div class="control-label">指标</div>
          <el-select v-model="selectedMetric" placeholder="选择指标">
            <el-option v-for="metric in metricOptions" :key="metric.value" :label="metric.label" :value="metric.value" />
          </el-select>
        </div>
        <div class="control-field">
          <div class="control-label">图表类型</div>
          <el-select v-model="chartType" placeholder="选择图表类型">
            <el-option v-for="option in chartTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </div>
        <div class="control-field">
          <div class="control-label">分组维度</div>
          <el-radio-group v-model="grouping" class="grouping-selector" size="small">
            <el-radio-button v-for="option in groupingOptions" :key="option.value" :label="option.value">
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </el-card>

    <el-card class="chart-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">{{ metricLabel }}可视化</div>
            <div class="card-subtitle">{{ chartSubtitle }}</div>
          </div>
          <el-tag v-if="chartTypeLabel" size="large" type="info">{{ chartTypeLabel }}</el-tag>
        </div>
      </template>
      <div class="chart-body">
        <el-skeleton v-if="loading" animated :rows="8" />
        <div v-else-if="!records.length" class="empty-state">
          <div class="empty-icon">📊</div>
          <div class="empty-title">暂无预测记录</div>
          <div class="empty-desc">请先在预测中心生成预测数据，然后刷新此页面</div>
          <el-button type="primary" @click="loadVisualizationData" style="margin-top: 16px;">
            刷新数据
          </el-button>
        </div>
        <div v-else-if="!filteredRecords.length" class="empty-state">
          <div class="empty-icon">🔍</div>
          <div class="empty-title">没有符合条件的记录</div>
          <div class="empty-desc">请调整筛选条件（地区、作物、模型）后重试</div>
          <el-button @click="resetFilters" style="margin-top: 16px;">
            重置筛选条件
          </el-button>
        </div>
        <BaseChart v-else :option="chartOption" :height="360" />
      </div>
    </el-card>

    <el-row :gutter="16" class="insight-row">
      <el-col v-for="card in insightCards" :key="card.label" :xs="24" :sm="12" :md="6">
        <el-card class="insight-card" shadow="hover">
          <div class="insight-label">{{ card.label }}</div>
          <div class="insight-value">{{ card.value }}</div>
          <div class="insight-sub">{{ card.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">预测记录明细</div>
            <div class="card-subtitle">{{ filteredRecords.length ? `共 ${filteredRecords.length} 条` : '暂无符合条件的记录' }}</div>
          </div>
        </div>
      </template>
      <el-table :data="pagedRecords" border :header-cell-style="tableHeaderStyle" empty-text="暂无预测记录" stripe>
        <el-table-column prop="periodLabel" label="预测期" min-width="120" fixed />
        <el-table-column prop="regionName" label="地区" min-width="140" />
        <el-table-column prop="cropName" label="作物" min-width="140" />
        <el-table-column prop="modelName" label="模型" min-width="180">
          <template #default="{ row }">
            <div>{{ row.modelName }}</div>
            <el-tag v-if="row.modelType" size="small" type="info" style="margin-top: 4px;">
              {{ row.modelType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="指标值" min-width="150" align="right">
          <template #default="{ row }">
            <span style="font-weight: 600; color: #0f766e;">
              {{ formatNumber(metricAccessor(row)) }} {{ metricUnit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="generatedAt" label="生成时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.generatedAt) }}</template>
        </el-table-column>
      </el-table>
      <div v-if="filteredRecords.length > pageSize" class="table-pagination">
        <el-pagination
          v-model:current-page="tablePage"
          :page-size="pageSize"
          layout="prev, pager, next"
          :total="filteredRecords.length"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import BaseChart from '@/components/charts/BaseChart.vue'
import { fetchForecastHistory } from '@/services/forecast'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const records = ref([])
const selectedRegions = ref([])
const selectedCrops = ref([])
const selectedModels = ref([])
const selectedMetric = ref('predictedProduction')
const chartType = ref('line')
const grouping = ref('region')
const tablePage = ref(1)
const pageSize = 8

const metricOptions = [
  { value: 'predictedProduction', label: '推算总产量', unit: '吨' },
  { value: 'predictedYield', label: '推算单产', unit: '吨/公顷' },
]

const chartTypeOptions = [
  { value: 'line', label: '折线图' },
  { value: 'area', label: '面积图' },
  { value: 'bar', label: '柱状图' },
]

const groupingOptions = [
  { value: 'region', label: '按地区' },
  { value: 'crop', label: '按作物' },
  { value: 'model', label: '按模型' },
]

const metricAccessorMap = {
  predictedProduction: record => record.predictedProduction,
  predictedYield: record => record.predictedYield,
  measurementValue: record => record.measurementValue,
  estimatedRevenue: record => record.estimatedRevenue,
}

const isUserTheme = computed(() => {
  const roles = authStore.user?.roles
  if (!roles) return true
  if (Array.isArray(roles)) {
    return !roles.includes('ADMIN')
  }
  return roles !== 'ADMIN'
})

const regionOptions = computed(() => {
  const unique = new Map()
  records.value.forEach(record => {
    if (!record.regionName) return
    if (!unique.has(record.regionName)) {
      unique.set(record.regionName, { value: record.regionName, label: record.regionName })
    }
  })
  return Array.from(unique.values())
})

const cropOptions = computed(() => {
  const unique = new Map()
  records.value.forEach(record => {
    if (!record.cropName) return
    if (!unique.has(record.cropName)) {
      unique.set(record.cropName, { value: record.cropName, label: record.cropName })
    }
  })
  return Array.from(unique.values())
})

const modelOptions = computed(() => {
  const unique = new Map()
  records.value.forEach(record => {
    const key = record.modelName ? `${record.modelName} (${record.modelType || '未知'})` : null
    if (!key) return
    if (!unique.has(key)) {
      unique.set(key, { value: key, label: key })
    }
  })
  return Array.from(unique.values())
})

const metricConfig = computed(() => metricOptions.find(item => item.value === selectedMetric.value) || metricOptions[0])

const metricLabel = computed(() => metricConfig.value?.label || '指标')

const metricUnit = computed(() => {
  if (selectedMetric.value === 'measurementValue') {
    const recordWithUnit = filteredRecords.value.find(record => record.measurementUnit)
    return recordWithUnit?.measurementUnit || metricConfig.value?.unit || ''
  }
  return metricConfig.value?.unit || ''
})

const chartTypeLabel = computed(() => chartTypeOptions.find(option => option.value === chartType.value)?.label || '')

const metricAccessor = record => metricAccessorMap[selectedMetric.value]?.(record) ?? null

const filteredRecords = computed(() => {
  return records.value.filter(record => {
    const regionMatch = !selectedRegions.value.length || selectedRegions.value.includes(record.regionName)
    const cropMatch = !selectedCrops.value.length || selectedCrops.value.includes(record.cropName)
    const modelLabel = record.modelName ? `${record.modelName} (${record.modelType || '未知'})` : ''
    const modelMatch = !selectedModels.value.length || selectedModels.value.includes(modelLabel)
    return regionMatch && cropMatch && modelMatch
  })
})

const chartData = computed(() => {
  if (!filteredRecords.value.length) {
    return { categories: [], series: [] }
  }
  const categories = []
  const categorySet = new Set()
  
  const grouped = new Map()
  filteredRecords.value.forEach(record => {
    const category = record.periodLabel || formatDateTime(record.generatedAt)
    if (!categorySet.has(category)) {
      categorySet.add(category)
      categories.push(category)
    }
    
    // 智能构建图例标签：根据选择的数量决定显示哪些维度
    const hasMultipleRegions = selectedRegions.value.length > 1
    const hasMultipleCrops = selectedCrops.value.length > 1
    const hasMultipleModels = selectedModels.value.length > 1
    
    let groupLabel = ''
    
    if (grouping.value === 'region') {
      // 按地区分组
      groupLabel = record.regionName || '未知地区'
      if (hasMultipleCrops) {
        groupLabel += ` · ${record.cropName || '未知作物'}`
      }
      if (hasMultipleModels) {
        groupLabel += ` · ${record.modelName || '未知模型'}`
      }
    } else if (grouping.value === 'crop') {
      // 按作物分组
      groupLabel = record.cropName || '未知作物'
      if (hasMultipleRegions) {
        groupLabel += ` · ${record.regionName || '未知地区'}`
      }
      if (hasMultipleModels) {
        groupLabel += ` · ${record.modelName || '未知模型'}`
      }
    } else {
      // 按模型分组
      groupLabel = record.modelName || '未知模型'
      if (hasMultipleCrops) {
        groupLabel += ` · ${record.cropName || '未知作物'}`
      }
      if (hasMultipleRegions) {
        groupLabel += ` · ${record.regionName || '未知地区'}`
      }
    }
    
    const value = metricAccessor(record)
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
      return
    }
    if (!grouped.has(groupLabel)) {
      grouped.set(groupLabel, new Map())
    }
    const bucket = grouped.get(groupLabel)
    if (!bucket.has(category)) {
      bucket.set(category, { total: 0, count: 0 })
    }
    const cell = bucket.get(category)
    cell.total += Number(value)
    cell.count += 1
  })
  
  const series = Array.from(grouped.entries()).map(([name, bucket]) => {
    const data = categories.map(category => {
      const cell = bucket.get(category)
      if (!cell) return null
      return Number((cell.total / cell.count).toFixed(2))
    })
    return { name, data }
  })
  return { categories, series }
})

const hasChartData = computed(() => chartData.value.series.length > 0)

const chartOption = computed(() => {
  if (!hasChartData.value) {
    return { tooltip: { trigger: 'axis' } }
  }
  const seriesType = chartType.value === 'bar' ? 'bar' : 'line'
  const enableArea = chartType.value === 'area'
  
  // 计算数据范围以优化Y轴显示
  const allValues = chartData.value.series.flatMap(s => s.data.filter(v => v !== null))
  const minValue = allValues.length > 0 ? Math.min(...allValues) : 0
  const maxValue = allValues.length > 0 ? Math.max(...allValues) : 100
  const range = maxValue - minValue
  const yAxisMin = range > 0 ? Math.floor(minValue - range * 0.1) : 0
  const yAxisMax = range > 0 ? Math.ceil(maxValue + range * 0.1) : 100
  
  return {
    tooltip: {
      trigger: 'axis',
      formatter: params => {
        if (!params || !params.length) return ''
        let result = `<div style="font-weight: 600; margin-bottom: 4px;">${params[0].axisValue}</div>`
        params.forEach(param => {
          const value = param.value !== null && param.value !== undefined 
            ? formatNumber(param.value) + ' ' + metricUnit.value
            : '--'
          result += `<div style="display: flex; align-items: center; gap: 8px;">
            <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${param.color};"></span>
            <span>${param.seriesName}:</span>
            <span style="font-weight: 600;">${value}</span>
          </div>`
        })
        return result
      }
    },
    legend: {
      data: chartData.value.series.map(item => item.name),
      top: 0,
      left: 'center',
      type: chartData.value.series.length > 6 ? 'scroll' : 'plain',
      pageButtonPosition: 'end',
      textStyle: {
        fontSize: 12
      }
    },
    grid: { top: 48, left: 60, right: 24, bottom: 40 },
    xAxis: {
      type: 'category',
      boundaryGap: chartType.value === 'bar',
      data: chartData.value.categories,
      axisLabel: {
        rotate: chartData.value.categories.length > 10 ? 45 : 0,
        interval: chartData.value.categories.length > 20 ? 'auto' : 0
      }
    },
    yAxis: {
      type: 'value',
      name: metricUnit.value,
      min: yAxisMin,
      max: yAxisMax,
      axisLabel: {
        formatter: value => {
          if (value == null) return ''
          // 智能格式化：大数字用K/M表示
          if (Math.abs(value) >= 1000000) {
            return (value / 1000000).toFixed(1) + 'M'
          }
          if (Math.abs(value) >= 1000) {
            return (value / 1000).toFixed(1) + 'K'
          }
          return value.toFixed(0)
        },
      },
    },
    series: chartData.value.series.map(item => ({
      name: item.name,
      type: seriesType,
      smooth: seriesType === 'line',
      showSymbol: seriesType === 'line',
      symbolSize: 6,
      areaStyle: enableArea ? { opacity: 0.18 } : undefined,
      data: item.data,
      emphasis: {
        focus: 'series'
      }
    })),
  }
})

const highlightStats = computed(() => {
  const total = records.value.length
  const latest = records.value[0]?.generatedAt ? formatDateTime(records.value[0].generatedAt) : '暂无'
  return [
    { label: '累计记录', value: total ? `${total} 条` : '—', sub: '最近 80 次预测' },
    { label: '最新生成', value: latest, sub: '数据实时同步' },
    { label: '地区覆盖', value: `${regionOptions.value.length} 个`, sub: '可筛选地区' },
    { label: '作物覆盖', value: `${cropOptions.value.length} 种`, sub: '可筛选作物' },
  ]
})

const chartSubtitle = computed(() => {
  if (!filteredRecords.value.length) {
    return '请选择预测条件查看可视化结果'
  }
  const groupLabel = groupingOptions.find(item => item.value === grouping.value)?.label || '维度'
  let subtitle = `共 ${filteredRecords.value.length} 条记录，${groupLabel}统计`
  
  // 构建图例格式说明
  const hasMultipleRegions = selectedRegions.value.length > 1
  const hasMultipleCrops = selectedCrops.value.length > 1
  const hasMultipleModels = selectedModels.value.length > 1
  
  const parts = []
  if (grouping.value === 'region') {
    parts.push('地区')
    if (hasMultipleCrops) parts.push('作物')
    if (hasMultipleModels) parts.push('模型')
  } else if (grouping.value === 'crop') {
    parts.push('作物')
    if (hasMultipleRegions) parts.push('地区')
    if (hasMultipleModels) parts.push('模型')
  } else {
    parts.push('模型')
    if (hasMultipleCrops) parts.push('作物')
    if (hasMultipleRegions) parts.push('地区')
  }
  
  if (parts.length > 1) {
    subtitle += `（图例：${parts.join(' · ')}）`
  }
  
  return subtitle
})

const insightCards = computed(() => {
  if (!filteredRecords.value.length) {
    return []
  }
  const sorted = [...filteredRecords.value].sort((a, b) => Number(metricAccessor(b) || 0) - Number(metricAccessor(a) || 0))
  const best = sorted[0]
  const worst = sorted[sorted.length - 1]
  return [
    {
      label: '最高值',
      value: formatNumber(metricAccessor(best)),
      sub: `${best?.regionName || ''} · ${best?.cropName || ''}`,
    },
    {
      label: '最低值',
      value: formatNumber(metricAccessor(worst)),
      sub: `${worst?.regionName || ''} · ${worst?.cropName || ''}`,
    },
    {
      label: '平均值',
      value: formatNumber(
        filteredRecords.value.reduce((sum, record) => sum + (Number(metricAccessor(record)) || 0), 0) /
          filteredRecords.value.length,
      ),
      sub: metricLabel.value,
    },
    {
      label: '模型数量',
      value: `${modelOptions.value.length} 个`,
      sub: '可用于分组对比',
    },
  ]
})

const pagedRecords = computed(() => {
  const start = (tablePage.value - 1) * pageSize
  return filteredRecords.value.slice(start, start + pageSize)
})

const tableHeaderStyle = () => ({
  background: '#f4f7fb',
  color: '#3c4b66',
  fontWeight: 600,
  fontSize: '13px',
})

const formatNumber = (value, fractionDigits = 2) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--'
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: fractionDigits,
  })
}

const formatDateTime = value => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

const normalizeRecord = record => ({
  id: record.forecastResultId || record.runId || `${record.regionName || '未知'}-${record.cropName || '未知'}-${record.period || '未标注'}`,
  runId: record.runId ?? null,
  forecastResultId: record.forecastResultId ?? null,
  regionName: record.regionName || '未标注地区',
  cropName: record.cropName || '未标注作物',
  modelName: record.modelName || '未命名模型',
  modelType: record.modelType || '',
  periodLabel: record.period || record.periodLabel || record.periodName || formatDateTime(record.generatedAt),
  measurementValue: record.measurementValue ?? null,
  measurementUnit: record.measurementUnit || '',
  predictedProduction: record.predictedProduction ?? null,
  predictedYield: record.predictedYield ?? null,
  estimatedRevenue: record.estimatedRevenue ?? null,
  generatedAt: record.generatedAt || null,
})

const loadVisualizationData = async () => {
  loading.value = true
  try {
    const response = await fetchForecastHistory({ page: 1, size: 80 })
    const normalized = Array.isArray(response.items) ? response.items.map(normalizeRecord) : []
    const parseTime = value => {
      const date = value ? new Date(value) : null
      const time = date && !Number.isNaN(date.getTime()) ? date.getTime() : 0
      return time
    }
    records.value = normalized.sort((a, b) => parseTime(b.generatedAt) - parseTime(a.generatedAt))
    tablePage.value = 1
    if (records.value.length > 0) {
      ElMessage.success(`已加载 ${records.value.length} 条预测记录`)
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载预测记录失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  selectedRegions.value = regionOptions.value.map(o => o.value)
  selectedCrops.value = cropOptions.value.map(o => o.value)
  selectedModels.value = modelOptions.value.map(o => o.value)
  ElMessage.info('已重置筛选条件')
}

const syncSelection = (options, targetRef) => {
  if (!options.length) {
    targetRef.value = []
    return
  }
  const optionValues = options.map(option => option.value)
  if (!targetRef.value.length) {
    targetRef.value = optionValues
    return
  }
  targetRef.value = targetRef.value.filter(value => optionValues.includes(value))
  if (!targetRef.value.length) {
    targetRef.value = optionValues
  }
}

watch(regionOptions, options => syncSelection(options, selectedRegions), { immediate: true })
watch(cropOptions, options => syncSelection(options, selectedCrops), { immediate: true })
watch(modelOptions, options => syncSelection(options, selectedModels), { immediate: true })

watch([selectedRegions, selectedCrops, selectedModels, selectedMetric, grouping, chartType], () => {
  tablePage.value = 1
})

onMounted(() => {
  loadVisualizationData()
})
</script>

<style scoped>
.forecast-visualization-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 32px;
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);
  gap: 24px;
  padding: 32px;
  border-radius: 24px;
  background: linear-gradient(115deg, rgba(91, 143, 249, 0.12), rgba(255, 255, 255, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.hero-text h1 {
  margin: 8px 0;
  font-size: 28px;
  color: #0f172a;
}

.hero-text p {
  margin: 0 0 16px;
  color: #475569;
  line-height: 1.8;
}

.hero-badge {
  display: inline-flex;
  padding: 6px 16px;
  border-radius: 999px;
  background: rgba(14, 116, 144, 0.12);
  color: #0f766e;
  font-weight: 600;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.hero-stat {
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.3);
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.stat-sub {
  font-size: 12px;
  color: #94a3b8;
}

.hero-illustration {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(14, 116, 144, 0.08);
  border-radius: 20px;
  border: 1px dashed rgba(14, 116, 144, 0.3);
}

.chart-placeholder {
  font-size: 64px;
}

.control-card,
.chart-card,
.table-card {
  border-radius: 18px;
}

.control-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}

.control-label {
  margin-bottom: 6px;
  font-size: 13px;
  color: #4b5563;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.card-subtitle {
  font-size: 13px;
  color: #94a3b8;
}

.chart-body {
  min-height: 320px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  padding: 40px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #6b7280;
  text-align: center;
  max-width: 400px;
  line-height: 1.6;
}

.insight-row {
  margin-top: -8px;
}

.insight-card {
  border-radius: 16px;
  text-align: center;
}

.insight-label {
  font-size: 13px;
  color: #6b7280;
}

.insight-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.insight-sub {
  font-size: 12px;
  color: #94a3b8;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}

.user-theme .hero-card {
  background: radial-gradient(circle at 20% 20%, rgba(129, 212, 250, 0.35), transparent 60%),
    linear-gradient(140deg, rgba(255, 255, 255, 0.95), rgba(250, 245, 255, 0.92));
  box-shadow: 0 28px 60px rgba(99, 102, 241, 0.2);
}

.user-theme .insight-card {
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.95), rgba(219, 234, 254, 0.8));
}

.admin-theme .hero-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: none;
}

.admin-theme .hero-stat {
  background: #f8fafc;
}

.admin-theme .insight-card {
  background: #f8fafc;
}
</style>
