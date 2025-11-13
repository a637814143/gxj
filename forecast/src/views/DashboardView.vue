<template>
  <div :class="['dashboard-page', { 'user-friendly': isUserTheme }]">
    <section class="hero-card">
      <div class="hero-copy">
        <div class="hero-badge">云惠农作业智能分析系统</div>
        <h1 class="hero-title">数据管理中心驾驶舱</h1>
        <p class="hero-desc">
          聚焦重点区域与核心作物的生产动态，实时掌握数据导入、模型运行与风险预警情况，让管理决策更加高效可靠。
        </p>
        <div class="hero-stats">
          <div v-for="item in highlightStats" :key="item.label" class="hero-stat">
            <div class="hero-stat-label">{{ item.label }}</div>
            <div class="hero-stat-value">{{ item.value }}</div>
            <div class="hero-stat-sub">{{ item.sub }}</div>
          </div>
        </div>
      </div>
      <div class="hero-extra">
        <div class="extra-grid">
          <div
            v-for="item in quickOverview"
            :key="item.label"
            class="extra-card"
            :class="{ positive: item.trend > 0, negative: item.trend < 0 }"
          >
            <div class="extra-label">{{ item.label }}</div>
            <div class="extra-value">{{ item.value }}</div>
            <div class="extra-trend">{{ formatTrend(item.trend) }}</div>
          </div>
        </div>
        <div class="extra-card reminder-card">
          <div class="extra-title">重点提醒</div>
          <ul class="extra-list">
            <li v-for="notice in reminders" :key="notice">{{ notice }}</li>
          </ul>
        </div>
      </div>
    </section>

    <el-card class="filter-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">查询条件</div>
            <div class="card-subtitle">筛选关键维度，快速定位关注的数据批次</div>
          </div>
          <div class="card-actions">
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" :loading="searching" @click="handleSearch">查询</el-button>
          </div>
        </div>
      </template>
      <el-form :model="filterForm" label-position="top" class="filter-form">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="作物范围">
              <el-select v-model="filterForm.crop" placeholder="请选择">
                <el-option v-for="item in cropOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="行政区域">
              <el-select v-model="filterForm.region" placeholder="请选择">
                <el-option v-for="item in regionOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="年度批次">
              <el-select v-model="filterForm.year" placeholder="请选择">
            <el-option label="全部年份" :value="null" />
            <el-option v-for="item in yearOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="生产周期">
              <el-select v-model="filterForm.cycle" placeholder="请选择">
                <el-option v-for="item in cycleOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="scenario-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">多情景产量对比</div>
            <div class="card-subtitle">将全球变暖影响纳入预测，直观比较基准与暖化情景的产量差异</div>
          </div>
          <el-tag type="warning" effect="light">气候情景假设：基于长期升温敏感度推演</el-tag>
        </div>
      </template>
      <div class="scenario-content">
        <div class="scenario-chart">
          <BaseChart v-if="hasScenarioData" :option="scenarioChartOption" :height="340" />
          <el-empty v-else description="暂无预测结果可用于生成情景对比" />
        </div>
        <div v-if="scenarioInsightCards.length" class="scenario-insights">
          <div
            v-for="card in scenarioInsightCards"
            :key="card.key"
            class="scenario-insight-card"
            :style="{
              '--scenario-accent': card.color,
              '--scenario-accent-soft': `${card.color}1a`,
              '--scenario-accent-border': `${card.color}33`
            }"
          >
            <div class="scenario-insight-header">
              <span class="scenario-insight-dot" :style="{ background: card.color }"></span>
              <span class="scenario-insight-label">{{ card.label }}</span>
            </div>
            <div class="scenario-insight-temp">{{ card.temperature }}</div>
            <div class="scenario-insight-value">{{ formatNumber(card.value) }} 吨</div>
            <div class="scenario-insight-change" :class="{ negative: card.change < 0, positive: card.change > 0 }">
              {{ formatScenarioChange(card.change) }}
            </div>
            <div class="scenario-insight-desc">{{ card.description }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">历史预测数据</div>
            <div class="card-subtitle">结合最新导入的基础数据与模型输出，追踪各地区预测表现</div>
          </div>
          <div class="card-actions">
            <el-button
              :loading="exporting.excel"
              :disabled="!filteredTableData.length || exporting.pdf"
              @click="handleExportExcel"
            >
              导出 Excel
            </el-button>
            <el-button
              :loading="exporting.pdf"
              :disabled="!filteredTableData.length || exporting.excel"
              @click="handleExportPdf"
            >
              导出 PDF
            </el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="paginatedTableData"
        :border="false"
        :stripe="true"
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
        v-loading="summaryLoading"
        empty-text="暂无可用数据"
      >
        <el-table-column prop="year" label="年份" width="110" />
        <el-table-column prop="regionName" label="地区" min-width="180" />
        <el-table-column prop="cropName" label="作物" min-width="140" />
        <el-table-column label="播种面积 (公顷)" min-width="160">
          <template #default="{ row }">{{ formatNumber(row.sownArea) }}</template>
        </el-table-column>
        <el-table-column label="总产量 (吨)" min-width="150">
          <template #default="{ row }">{{ formatNumber(row.production) }}</template>
        </el-table-column>
        <el-table-column label="单产 (吨/公顷)" min-width="150">
          <template #default="{ row }">{{ formatNumber(row.yieldPerHectare) }}</template>
        </el-table-column>
        <el-table-column label="平均价格 (元/公斤)" min-width="180">
          <template #default="{ row }">{{ formatNumber(row.averagePrice) }}</template>
        </el-table-column>
        <el-table-column label="预计收益 (万元)" min-width="160">
          <template #default="{ row }">{{ formatNumber(row.estimatedRevenue) }}</template>
        </el-table-column>
        <el-table-column label="数据日期" width="160">
          <template #default="{ row }">{{ formatDate(row.collectedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              link
              size="small"
              :disabled="deletingRunId === row.runId"
              @click="handleDeleteRecord(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <div class="table-tip">
          <div class="pagination-info">
            <span class="pagination-badge">每页显示 5 条</span>
            <span>分页展示（5 条/页）</span>
            <span>共 {{ filteredTableData.length }} 条，当前第 {{ tablePagination.page }}/{{ totalTablePages }} 页</span>
          </div>
          <div class="tip-extra">可通过筛选条件查看特定地区或作物</div>
        </div>
        <el-pagination
          class="table-pagination"
          :current-page="tablePagination.page"
          :page-size="tablePagination.size"
          :total="filteredTableData.length"
          layout="prev, pager, next"
          background
          @current-change="handleTablePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import apiClient from '../services/http'
import { deleteForecastRun } from '@/services/forecast'
import { useAuthStore } from '@/stores/auth'
import BaseChart from '@/components/charts/BaseChart.vue'

const authStore = useAuthStore()
const isUserTheme = computed(() => {
  const roles = authStore.user?.roles
  if (!roles) return true
  if (Array.isArray(roles)) {
    return !roles.includes('ADMIN')
  }
  return roles !== 'ADMIN'
})

const summary = ref(null)
const summaryLoading = ref(false)
const deletingRunId = ref(null)
const exporting = reactive({ excel: false, pdf: false })

const TABLE_PAGE_SIZE = 5
const tablePagination = reactive({ page: 1, size: TABLE_PAGE_SIZE })

const currentYear = new Date().getFullYear()
const yearOptions = computed(() => Array.from({ length: 6 }, (_, index) => currentYear - index))

const cropOptions = ref([{ label: '全部作物', value: 'ALL' }])
const regionOptions = ref([{ label: '全部区域', value: 'ALL' }])
const cycleOptions = [
  { label: '全年', value: 'ALL' },
  { label: '春季', value: 'SPRING' },
  { label: '夏季', value: 'SUMMER' },
  { label: '秋季', value: 'AUTUMN' },
  { label: '冬季', value: 'WINTER' }
]

const DEFAULT_SCENARIO_CONFIGS = [
  {
    key: 'baseline',
    label: '基准',
    temperature: '当前气候',
    delta: 0,
    color: '#2563eb',
    description: '保持当前气候条件，沿用历史增长趋势的预测基线。',
    isBaseline: true
  },
  {
    key: 'warming15',
    label: '暖化+1.5℃',
    temperature: '+1.5℃ 情景',
    delta: -0.045,
    color: '#f97316',
    description: '较强暖化下预估轻微减产，需关注抗热品种与水肥调控。'
  },
  {
    key: 'warming20',
    label: '暖化+2℃',
    temperature: '+2℃ 情景',
    delta: -0.065,
    color: '#dc2626',
    description: '极端暖化风险显著增大，建议提前部署减灾与保障措施。'
  }
]

const SCENARIO_COLOR_PALETTE = ['#2563eb', '#f97316', '#dc2626', '#0f766e', '#9333ea', '#facc15']

const filterForm = reactive({
  crop: 'ALL',
  region: 'ALL',
  year: null,
  cycle: 'ALL'
})

const highlightStats = computed(() => {
  const data = summary.value
  if (!data) {
    return [
      { label: '总产量', value: '加载中…', sub: '正在请求数据库数据' },
      { label: '播种面积', value: '加载中…', sub: '请稍候' },
      { label: '平均单产', value: '加载中…', sub: '请稍候' },
      { label: '入库记录', value: '加载中…', sub: '等待导入数据' }
    ]
  }
  return [
    {
      label: '总产量',
      value: `${formatNumber(data.totalProduction)} 吨`,
      sub: `覆盖 ${formatInteger(data.recordCount)} 条记录`
    },
    {
      label: '播种面积',
      value: `${formatNumber(data.totalSownArea)} 公顷`,
      sub: '包含所有地区累计数据'
    },
    {
      label: '平均单产',
      value: `${formatNumber(data.averageYield)} 吨/公顷`,
      sub: '基于最新导入数据计算'
    },
    {
      label: '入库记录',
      value: `${formatInteger(data.recordCount)} 条`,
      sub: '导入任务实时汇总'
    }
  ]
})

const quickOverview = computed(() => {
  const data = summary.value
  const cropCount = data?.cropStructure?.length ?? 0
  const regionCount = data?.regionComparisons?.length ?? 0
  const forecastCount = data?.forecastOutlook?.length ?? 0
  return [
    { label: '监测作物', value: `${cropCount} 种`, trend: cropCount ? 1 : 0 },
    { label: '覆盖地区', value: `${regionCount} 个`, trend: regionCount ? 1 : 0 },
    { label: '预测展望', value: `${forecastCount} 期`, trend: forecastCount ? 1 : 0 }
  ]
})

const reminders = computed(() => {
  const data = summary.value
  if (!data) {
    return ['正在加载最新的产量与价格统计…', '请稍候']
  }
  const list = []
  const topCrop = data.cropStructure?.[0]
  if (topCrop) {
    list.push(`产量最高的作物：${topCrop.cropName}，占比 ${formatPercent(topCrop.share)}`)
  }
  const topRegion = data.regionComparisons?.[0]
  if (topRegion) {
    list.push(`${topRegion.regionName} ${formatNumber(topRegion.production)} 吨，平均单产 ${formatNumber(topRegion.yieldPerHectare)} 吨/公顷`)
  }
  const latestRecord = data.recentRecords?.[0]
  if (latestRecord) {
    list.push(`最新入库：${latestRecord.year} 年 ${latestRecord.regionName} ${latestRecord.cropName} 数据`)
  }
  if (!list.length) {
    list.push('暂无需要关注的提醒，欢迎导入最新的作物数据。')
  }
  return list
})

const tableData = computed(() => summary.value?.recentRecords ?? [])

const filteredTableData = computed(() => {
  let records = tableData.value
  if (filterForm.crop && filterForm.crop !== 'ALL') {
    records = records.filter(record => record.cropName === filterForm.crop)
  }
  if (filterForm.region && filterForm.region !== 'ALL') {
    records = records.filter(record => record.regionName === filterForm.region)
  }
  if (filterForm.year !== null) {
    records = records.filter(record => record.year === Number(filterForm.year))
  }
  return records
})

const exportFileNameBase = computed(() => {
  const parts = ['仪表盘预测数据']
  if (filterForm.region && filterForm.region !== 'ALL') {
    parts.push(filterForm.region)
  }
  if (filterForm.crop && filterForm.crop !== 'ALL') {
    parts.push(filterForm.crop)
  }
  if (filterForm.year !== null) {
    parts.push(`${filterForm.year}年`)
  }
  return parts.join('_')
})

const paginatedTableData = computed(() => {
  const size = tablePagination.size || TABLE_PAGE_SIZE
  const start = (tablePagination.page - 1) * size
  return filteredTableData.value.slice(start, start + size)
})

const totalTablePages = computed(() => {
  const size = tablePagination.size || TABLE_PAGE_SIZE
  const total = filteredTableData.value.length
  return Math.max(1, Math.ceil((total || 0) / size || 1))
})

const searching = computed(() => summaryLoading.value)

const applyScenarioDelta = (value, delta) => {
  if (value === null || value === undefined) {
    return null
  }
  const numeric = Number(value)
  if (Number.isNaN(numeric)) {
    return null
  }
  const scaled = numeric * (1 + (delta ?? 0))
  return Math.round(scaled * 100) / 100
}

const forecastPoints = computed(() => summary.value?.forecastOutlook ?? [])

const resolvedScenarioConfigs = computed(() => {
  const scenarios = Array.isArray(summary.value?.scenarioComparisons)
    ? summary.value.scenarioComparisons
    : []

  if (scenarios.length) {
    return scenarios.map((scenario, index) => {
      const color = scenario.color || SCENARIO_COLOR_PALETTE[index % SCENARIO_COLOR_PALETTE.length]
      const normalizeSeries = Array.isArray(scenario.series)
        ? scenario.series.map(point => ({
            label: point?.label ?? point?.period ?? point?.name ?? '',
            value: point?.value ?? null
          }))
        : []
      return {
        key: scenario.key ?? scenario.code ?? `scenario-${index}`,
        label: scenario.label ?? scenario.name ?? `情景${index + 1}`,
        temperature: scenario.temperature ?? scenario.temperatureLabel ?? '',
        color,
        description: scenario.description ?? '',
        isBaseline:
          typeof scenario.isBaseline === 'boolean'
            ? scenario.isBaseline
            : /基准|baseline/i.test(String(scenario.label ?? '')),
        delta: typeof scenario.delta === 'number' ? scenario.delta : null,
        series: normalizeSeries,
        change: typeof scenario.change === 'number' ? scenario.change : null
      }
    })
  }

  return DEFAULT_SCENARIO_CONFIGS.map((config, index) => ({
    ...config,
    color: config.color || SCENARIO_COLOR_PALETTE[index % SCENARIO_COLOR_PALETTE.length],
    series: [],
    change: null
  }))
})

const scenarioSeries = computed(() => {
  const points = forecastPoints.value
  return resolvedScenarioConfigs.value.map((config, index) => {
    let series = Array.isArray(config.series) ? config.series.filter(point => point && typeof point === 'object') : []
    if (!series.length && points.length) {
      if (config.delta !== null && config.delta !== undefined) {
        series = points.map(point => ({
          label: point?.label ?? '',
          value: applyScenarioDelta(point?.value, config.delta)
        }))
      } else if (config.isBaseline || index === 0) {
        series = points.map(point => ({
          label: point?.label ?? '',
          value: point?.value ?? null
        }))
      }
    }
    return {
      ...config,
      series
    }
  })
})

const scenarioConfigMap = computed(
  () => new Map(scenarioSeries.value.map(config => [config.label, config]))
)

const hasScenarioData = computed(() =>
  scenarioSeries.value.some(config => Array.isArray(config.series) && config.series.length)
)

const formatScenarioTooltip = params => {
  if (!Array.isArray(params) || !params.length) {
    return ''
  }
  const axisLabel = params[0]?.axisValueLabel ?? ''
  const lines = [`<div style="font-weight:600;margin-bottom:6px;">${axisLabel}</div>`]
  params.forEach(item => {
    const config = scenarioConfigMap.value.get(item.seriesName)
    const temperature = config?.temperature ? `（${config.temperature}）` : ''
    lines.push(
      `<div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">` +
        `<span>${item.marker}${item.seriesName}${temperature}</span>` +
        `<span style="font-weight:600;">${formatNumber(item.data)} 吨</span>` +
      `</div>`
    )
  })
  return lines.join('')
}

const scenarioChartOption = computed(() => {
  const scenarios = scenarioSeries.value.filter(config => config.series.length)
  if (!scenarios.length) {
    return {
      color: [],
      series: [],
      xAxis: { data: [] },
      yAxis: { type: 'value' }
    }
  }

  const baseline = scenarios.find(config => config.isBaseline) ?? scenarios[0]
  const categories = baseline.series.map(point => point?.label ?? '')

  const series = scenarios.map(config => {
    const valueMap = new Map(config.series.map(point => [point?.label ?? '', point?.value ?? null]))
    const data = categories.map(label => {
      const value = valueMap.get(label)
      if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return null
      }
      return Math.round(Number(value) * 100) / 100
    })

    return {
      name: config.label,
      type: 'line',
      smooth: true,
      showSymbol: true,
      symbolSize: 8,
      lineStyle: {
        width: 3
      },
      areaStyle: config.isBaseline ? { opacity: 0.1 } : undefined,
      emphasis: { focus: 'series' },
      data
    }
  })

  return {
    color: scenarios.map(config => config.color),
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderWidth: 0,
      padding: 12,
      textStyle: { color: '#f8fafc' },
      formatter: formatScenarioTooltip
    },
    legend: {
      data: scenarios.map(config => config.label),
      top: 18,
      textStyle: { color: '#475569', fontSize: 13 }
    },
    grid: { top: 78, left: 64, right: 28, bottom: 40 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: categories,
      axisTick: { alignWithLabel: true },
      axisLine: { lineStyle: { color: '#d0d7ff' } },
      axisLabel: { color: '#4b5563' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: {
        color: '#4b5563',
        formatter: value => formatNumber(value)
      },
      splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } }
    },
    series
  }
})

const scenarioInsightCards = computed(() => {
  const scenarios = scenarioSeries.value.filter(config => config.series.length)
  if (!scenarios.length) {
    return []
  }
  const baseline = scenarios.find(config => config.isBaseline) ?? scenarios[0]
  const baselineSeries = baseline.series
  const baselineLast = baselineSeries.length
    ? baselineSeries[baselineSeries.length - 1]?.value ?? null
    : null

  return scenarios
    .map(config => {
      const series = config.series
      const lastPoint = series.length ? series[series.length - 1] : null
      const lastValue = lastPoint?.value ?? null
      let change = 0
      if (config.change !== null && config.change !== undefined) {
        change = config.change
      } else if (
        !config.isBaseline &&
        baselineLast !== null &&
        baselineLast !== undefined &&
        baselineLast !== 0 &&
        lastValue !== null &&
        lastValue !== undefined
      ) {
        change = (Number(lastValue) - Number(baselineLast)) / Number(baselineLast)
      }
      const temperature = config.temperature || (config.isBaseline ? '当前气候' : '')

      return {
        key: config.key,
        label: config.label,
        temperature,
        value: lastValue,
        change,
        description: config.description,
        color: config.color
      }
    })
    .filter(card => card.value !== null && card.value !== undefined)
})

const updateFilterOptions = () => {
  const data = summary.value
  if (!data) {
    cropOptions.value = [{ label: '全部作物', value: 'ALL' }]
    regionOptions.value = [{ label: '全部区域', value: 'ALL' }]
    return
  }
  const crops = Array.from(new Set((data.cropStructure ?? []).map(item => item.cropName).filter(Boolean)))
  cropOptions.value = [{ label: '全部作物', value: 'ALL' }, ...crops.map(name => ({ label: name, value: name }))]
  if (!cropOptions.value.some(option => option.value === filterForm.crop)) {
    filterForm.crop = 'ALL'
  }

  const regions = Array.from(new Set((data.regionComparisons ?? []).map(item => item.regionName).filter(Boolean)))
  regionOptions.value = [{ label: '全部区域', value: 'ALL' }, ...regions.map(name => ({ label: name, value: name }))]
  if (!regionOptions.value.some(option => option.value === filterForm.region)) {
    filterForm.region = 'ALL'
  }

  if (filterForm.year !== null && !yearOptions.value.includes(filterForm.year)) {
    filterForm.year = null
  }
}

const fetchDashboardSummary = async () => {
  summaryLoading.value = true
  try {
    const { data } = await apiClient.get('/api/dashboard/summary')
    summary.value = data ?? null
    updateFilterOptions()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载仪表盘数据失败')
  } finally {
    summaryLoading.value = false
  }
}

const handleSearch = () => {
  tablePagination.page = 1
  fetchDashboardSummary()
}

const handleReset = () => {
  filterForm.crop = 'ALL'
  filterForm.region = 'ALL'
  filterForm.year = null
  filterForm.cycle = 'ALL'
  tablePagination.page = 1
  fetchDashboardSummary()
}

const handleTablePageChange = page => {
  tablePagination.page = page
}

const handleDeleteRecord = async record => {
  if (!record?.runId) {
    ElMessage.warning('缺少运行编号，无法删除该预测数据')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确定要删除这条预测数据吗？删除后将无法恢复。',
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      },
    )
  } catch (error) {
    return
  }
  deletingRunId.value = record.runId
  try {
    await deleteForecastRun(record.runId)
    ElMessage.success('预测数据已删除')
    const needPrevPage = paginatedTableData.value.length <= 1 && tablePagination.page > 1
    if (needPrevPage) {
      tablePagination.page -= 1
    }
    await fetchDashboardSummary()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '删除预测数据失败')
  } finally {
    if (deletingRunId.value === record.runId) {
      deletingRunId.value = null
    }
  }
}

const exportHeaders = [
  '年份',
  '地区',
  '作物',
  '播种面积 (公顷)',
  '总产量 (吨)',
  '单产 (吨/公顷)',
  '平均价格 (元/公斤)',
  '预计收益 (万元)',
  '数据日期'
]

const buildExcelRows = () =>
  filteredTableData.value.map(record => [
    record.year ?? '',
    record.regionName ?? '',
    record.cropName ?? '',
    record.sownArea ?? '',
    record.production ?? '',
    record.yieldPerHectare ?? '',
    record.averagePrice ?? '',
    record.estimatedRevenue ?? '',
    record.collectedAt ? formatDate(record.collectedAt) : ''
  ])

const buildPdfRows = () =>
  filteredTableData.value.map(record => [
    record.year ?? '-',
    record.regionName ?? '-',
    record.cropName ?? '-',
    formatNumber(record.sownArea),
    formatNumber(record.production),
    formatNumber(record.yieldPerHectare),
    formatNumber(record.averagePrice),
    formatNumber(record.estimatedRevenue),
    formatDate(record.collectedAt)
  ])

const ensureExportable = () => {
  if (!filteredTableData.value.length) {
    ElMessage.warning('暂无数据可导出')
    return false
  }
  return true
}

const getTimestamp = () => {
  const now = new Date()
  const yyyy = now.getFullYear()
  const mm = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  const hh = String(now.getHours()).padStart(2, '0')
  const mi = String(now.getMinutes()).padStart(2, '0')
  return `${yyyy}${mm}${dd}_${hh}${mi}`
}

const triggerDownload = (blob, fileName) => {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

const escapeExcelXml = value =>
  String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')

const buildExcelXml = () => {
  const headerRow = `<Row>${exportHeaders
    .map(title => `<Cell ss:StyleID="header"><Data ss:Type="String">${escapeExcelXml(title)}</Data></Cell>`)
    .join('')}</Row>`
  const dataRows = buildExcelRows()
    .map(row => {
      const cells = row
        .map(cell => {
          if (cell === null || cell === undefined || cell === '') {
            return '<Cell><Data ss:Type="String"></Data></Cell>'
          }
          const numeric = Number(cell)
          if (!Number.isNaN(numeric) && Number.isFinite(numeric)) {
            return `<Cell><Data ss:Type="Number">${escapeExcelXml(numeric)}</Data></Cell>`
          }
          return `<Cell><Data ss:Type="String">${escapeExcelXml(cell)}</Data></Cell>`
        })
        .join('')
      return `<Row>${cells}</Row>`
    })
    .join('')

  return `<?xml version="1.0"?><?mso-application progid="Excel.Sheet"?>` +
    '<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"' +
    ' xmlns:o="urn:schemas-microsoft-com:office:office"' +
    ' xmlns:x="urn:schemas-microsoft-com:office:excel"' +
    ' xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"' +
    ' xmlns:html="http://www.w3.org/TR/REC-html40">' +
    '<Styles>' +
    '<Style ss:ID="header">' +
    '<Font ss:Bold="1" />' +
    '<Interior ss:Color="#F0F7FF" ss:Pattern="Solid" />' +
    '</Style>' +
    '</Styles>' +
    `<Worksheet ss:Name="历史预测数据">` +
    '<Table>' +
    headerRow +
    dataRows +
    '</Table>' +
    '</Worksheet>' +
    '</Workbook>'
}

const handleExportExcel = () => {
  if (!ensureExportable() || exporting.pdf) {
    return
  }
  exporting.excel = true
  try {
    const workbookXml = buildExcelXml()
    const blob = new Blob([workbookXml], {
      type: 'application/vnd.ms-excel;charset=utf-8'
    })
    const fileName = `${exportFileNameBase.value}_${getTimestamp()}.xls`
    triggerDownload(blob, fileName)
    ElMessage.success('Excel 导出成功')
  } catch (error) {
    console.error('导出 Excel 失败', error)
    ElMessage.error('导出 Excel 失败，请稍后再试')
  } finally {
    exporting.excel = false
  }
}

const encodePdfText = text => {
  const value = String(text ?? '')
  const codePoints = Array.from(value)
  const bytes = [0xfe, 0xff]
  codePoints.forEach(char => {
    const code = char.codePointAt(0)
    if (code === undefined) return
    if (code > 0xffff) {
      const adjusted = code - 0x10000
      const high = 0xd800 + (adjusted >> 10)
      const low = 0xdc00 + (adjusted & 0x3ff)
      bytes.push((high >> 8) & 0xff, high & 0xff, (low >> 8) & 0xff, low & 0xff)
    } else {
      bytes.push((code >> 8) & 0xff, code & 0xff)
    }
  })
  return bytes.map(byte => byte.toString(16).padStart(2, '0')).join('').toUpperCase()
}

const wrapPdfLines = (text, maxChars = 80) => {
  const result = []
  let current = ''
  Array.from(text).forEach(char => {
    if (current.length >= maxChars) {
      result.push(current)
      current = ''
    }
    current += char
  })
  if (current) {
    result.push(current)
  }
  return result.length ? result : ['']
}

const buildPdfContentStream = lines => {
  const content = ['BT', '/F1 12 Tf', '1 0 0 1 50 800 Tm']
  const lineHeight = 18
  let isFirstLine = true
  lines.forEach(line => {
    const pieces = wrapPdfLines(line)
    pieces.forEach(part => {
      if (!isFirstLine) {
        content.push(`0 -${lineHeight} Td`)
      }
      content.push(`<${encodePdfText(part)}> Tj`)
      isFirstLine = false
    })
  })
  content.push('ET')
  return content.join('\n')
}

const buildSimplePdf = lines => {
  let pdf = '%PDF-1.7\n'
  const offsets = []
  const appendObject = (id, body) => {
    offsets[id] = pdf.length
    pdf += `${id} 0 obj\n${body}\nendobj\n`
  }
  const appendStreamObject = (id, body) => {
    offsets[id] = pdf.length
    pdf += `${id} 0 obj\n<< /Length ${body.length} >>\nstream\n${body}\nendstream\nendobj\n`
  }

  appendObject(1, '<< /Type /Catalog /Pages 2 0 R >>')
  appendObject(2, '<< /Type /Pages /Count 1 /Kids [3 0 R] >>')
  appendObject(
    3,
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>'
  )

  const contentStream = buildPdfContentStream(lines)
  appendStreamObject(4, contentStream)
  appendObject(
    5,
    '<< /Type /Font /Subtype /Type0 /BaseFont /STSong-Light /Encoding /UniGB-UCS2-H /DescendantFonts [6 0 R] >>'
  )
  appendObject(
    6,
    '<< /Type /Font /Subtype /CIDFontType0 /BaseFont /STSong-Light /CIDSystemInfo << /Registry (Adobe) /Ordering (GB1) /Supplement 2 >> /FontDescriptor 7 0 R /DW 600 >>'
  )
  appendObject(
    7,
    '<< /Type /FontDescriptor /FontName /STSong-Light /Flags 4 /FontBBox [-250 -250 1000 1000] /ItalicAngle 0 /Ascent 880 /Descent -120 /CapHeight 880 /StemV 80 >>'
  )

  const xrefStart = pdf.length
  pdf += 'xref\n'
  pdf += '0 8\n'
  pdf += '0000000000 65535 f \n'
  for (let i = 1; i <= 7; i += 1) {
    pdf += `${String(offsets[i]).padStart(10, '0')} 00000 n \n`
  }
  pdf += 'trailer\n'
  pdf += '<< /Size 8 /Root 1 0 R >>\n'
  pdf += 'startxref\n'
  pdf += `${xrefStart}\n`
  pdf += '%%EOF'

  return pdf
}

const handleExportPdf = () => {
  if (!ensureExportable() || exporting.excel) {
    return
  }
  exporting.pdf = true
  try {
    const lines = [
      exportHeaders.join(' | '),
      ...buildPdfRows().map(row => row.join(' | '))
    ]
    const pdfContent = buildSimplePdf(lines)
    const blob = new Blob([pdfContent], { type: 'application/pdf' })
    const fileName = `${exportFileNameBase.value}_${getTimestamp()}.pdf`
    triggerDownload(blob, fileName)
    ElMessage.success('PDF 导出成功')
  } catch (error) {
    console.error('导出 PDF 失败', error)
    ElMessage.error('导出 PDF 失败，请稍后再试')
  } finally {
    exporting.pdf = false
  }
}

onMounted(() => {
  fetchDashboardSummary()
})

watch(
  () => [filterForm.crop, filterForm.region, filterForm.year],
  () => {
    tablePagination.page = 1
  }
)

watch(
  filteredTableData,
  records => {
    const totalPages = Math.max(1, Math.ceil((records.length || 0) / (tablePagination.size || TABLE_PAGE_SIZE) || 1))
    if (tablePagination.page > totalPages) {
      tablePagination.page = totalPages
    }
    if (tablePagination.page < 1) {
      tablePagination.page = 1
    }
  },
  { immediate: true }
)

function formatNumber(value, fractionDigits = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: fractionDigits
  })
}

function formatInteger(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '0'
  }
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${(Number(value) * 100).toFixed(1)}%`
}

function formatScenarioChange(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value)) || Math.abs(Number(value)) < 0.0001) {
    return '较基准 持平'
  }
  const percent = (Number(value) * 100).toFixed(1)
  if (Number(value) > 0) {
    return `较基准 +${percent}%`
  }
  return `较基准 ${percent}%`
}

function formatTrend(value) {
  if (value > 0) return `较昨日 +${value}`
  if (value < 0) return `较昨日 ${value}`
  return '较昨日 持平'
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleDateString('zh-CN')
}

const tableHeaderStyle = () => ({
  background: '#f4f7fb',
  color: '#3c4b66',
  fontWeight: 600,
  fontSize: '13px'
})

const tableCellStyle = () => ({
  color: '#1f2d3d',
  fontSize: '13px'
})
</script>

<style scoped>

.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 32px;
}

.dashboard-page.user-friendly {
  position: relative;
  padding: 4px 0 40px;
  background: radial-gradient(circle at top left, rgba(129, 212, 250, 0.22), transparent 55%),
    radial-gradient(circle at 20% 80%, rgba(244, 143, 177, 0.2), transparent 60%),
    linear-gradient(160deg, #f4f9ff 0%, #fef6ff 52%, #ffffff 100%);
}

.dashboard-page.user-friendly::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.4), transparent 60%);
  pointer-events: none;
}

.hero-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 32px;
  border-radius: 24px;
  background: linear-gradient(120deg, #e8f1ff 0%, #f4f8ff 35%, #ffffff 100%);
  box-shadow: 0 28px 60px rgba(51, 112, 255, 0.15);
  overflow: hidden;
}

.dashboard-page.user-friendly .hero-card {
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: linear-gradient(120deg, rgba(231, 245, 255, 0.95) 0%, rgba(247, 233, 255, 0.9) 52%, rgba(255, 255, 255, 0.95) 100%);
  box-shadow: 0 26px 70px rgba(122, 123, 255, 0.18);
}

.hero-card::before {
  content: '';
  position: absolute;
  top: -160px;
  right: -160px;
  width: 360px;
  height: 360px;
  background: radial-gradient(circle at center, rgba(60, 132, 255, 0.35), transparent 65%);
  transform: rotate(12deg);
}

.dashboard-page.user-friendly .hero-card::before {
  background: radial-gradient(circle at center, rgba(129, 212, 250, 0.45), transparent 65%);
}

.hero-copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  font-size: 13px;
  border-radius: 999px;
  color: #2262ff;
  background: rgba(34, 98, 255, 0.12);
  font-weight: 600;
  letter-spacing: 1px;
}

.hero-title {
  margin: 0;
  font-size: 28px;
  color: #0d1b3d;
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #455471;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.hero-stat {
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: inset 0 0 0 1px rgba(34, 98, 255, 0.08);
  backdrop-filter: blur(4px);
}

.dashboard-page.user-friendly .hero-stat {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(232, 244, 255, 0.85));
  box-shadow: 0 16px 36px rgba(102, 126, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.hero-stat-label {
  font-size: 12px;
  color: #5c6d8d;
  margin-bottom: 8px;
}

.hero-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #163b8c;
  margin-bottom: 6px;
}

.hero-stat-sub {
  font-size: 12px;
  color: #6e7fa1;
}

.hero-extra {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 20px;
}

@media (max-width: 1200px) {
  .hero-extra {
    grid-template-columns: 1fr;
  }
}

.extra-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.extra-card {
  border-radius: 18px;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: inset 0 0 0 1px rgba(34, 98, 255, 0.08);
  backdrop-filter: blur(4px);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dashboard-page.user-friendly .extra-card {
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.92), rgba(231, 246, 255, 0.88));
  box-shadow: 0 18px 36px rgba(148, 163, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.extra-card.positive {
  border-left: 4px solid rgba(45, 212, 191, 0.9);
}

.extra-card.negative {
  border-left: 4px solid rgba(248, 113, 113, 0.85);
}

.extra-label {
  font-size: 13px;
  color: #52617f;
}

.extra-value {
  font-size: 22px;
  font-weight: 700;
  color: #1a2a4a;
}

.extra-trend {
  font-size: 12px;
  color: #64748b;
}

.extra-card.positive .extra-trend {
  color: #0f766e;
}

.extra-card.negative .extra-trend {
  color: #be123c;
}

.reminder-card {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(14, 165, 233, 0.12));
  border: 1px solid rgba(14, 165, 233, 0.15);
}

.dashboard-page.user-friendly .reminder-card {
  background: linear-gradient(135deg, rgba(165, 180, 252, 0.2), rgba(110, 231, 183, 0.18));
  border: 1px solid rgba(255, 255, 255, 0.55);
}

.extra-title {
  font-size: 15px;
  font-weight: 600;
  color: #163b8c;
}

.extra-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #42526b;
}

.dashboard-page.user-friendly .extra-title {
  color: #4338ca;
}

.dashboard-page.user-friendly .extra-list {
  color: #4c1d95;
}

.filter-card,
.scenario-card,
.table-card {
  border-radius: 20px;
  overflow: hidden;
}

.dashboard-page.user-friendly .filter-card,
.dashboard-page.user-friendly .scenario-card,
.dashboard-page.user-friendly .table-card {
  border: none;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.92) 0%, rgba(237, 248, 255, 0.9) 60%, rgba(255, 255, 255, 0.95) 100%);
  box-shadow: 0 28px 54px rgba(148, 163, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.dashboard-page.user-friendly :deep(.filter-card .el-card__header),
.dashboard-page.user-friendly :deep(.scenario-card .el-card__header),
.dashboard-page.user-friendly :deep(.table-card .el-card__header) {
  background: linear-gradient(120deg, rgba(230, 244, 255, 0.8), rgba(255, 255, 255, 0.85));
  border-bottom: 1px solid rgba(209, 213, 255, 0.35);
}

.dashboard-page.user-friendly :deep(.filter-card .el-card__body),
.dashboard-page.user-friendly :deep(.scenario-card .el-card__body),
.dashboard-page.user-friendly :deep(.table-card .el-card__body) {
  background: transparent;
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
  color: #1a2a4a;
}

.card-subtitle {
  font-size: 13px;
  color: #6c7d9c;
  margin-top: 4px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.scenario-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

@media (min-width: 992px) {
  .scenario-content {
    flex-direction: row;
    align-items: stretch;
  }
}

.scenario-chart {
  flex: 1.6;
  min-height: 320px;
}

.scenario-insights {
  flex: 1;
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.scenario-insight-card {
  position: relative;
  padding: 18px 20px;
  border-radius: 18px;
  background: linear-gradient(145deg, var(--scenario-accent-soft, rgba(37, 99, 235, 0.1)), rgba(255, 255, 255, 0.95));
  border: 1px solid var(--scenario-accent-border, rgba(37, 99, 235, 0.18));
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.scenario-insight-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
}

.scenario-insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1f2937;
}

.scenario-insight-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--scenario-accent, #2563eb);
  box-shadow: 0 0 0 4px var(--scenario-accent-soft, rgba(37, 99, 235, 0.16));
}

.scenario-insight-temp {
  font-size: 12px;
  color: #64748b;
  letter-spacing: 0.2px;
}

.scenario-insight-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.scenario-insight-change {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.scenario-insight-change.negative {
  color: #b91c1c;
}

.scenario-insight-change.positive {
  color: #047857;
}

.scenario-insight-desc {
  margin-top: auto;
  font-size: 12px;
  line-height: 1.5;
  color: #475569;
}

.filter-form {
  padding-top: 8px;
}

.table-footer {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: #5f6f8c;
  font-size: 12px;
}

.table-tip {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tip-extra {
  color: #7a8bad;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
}

.pagination-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #607d8b;
}

.pagination-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  background: #e3f2fd;
  color: #1565c0;
  font-weight: 600;
}

.dashboard-page.user-friendly .pagination-badge {
  background: rgba(129, 212, 250, 0.2);
  color: #0f766e;
}

@media (max-width: 992px) {
  .hero-card {
    padding: 24px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
  }
}

@media (max-width: 768px) {
  .hero-stats {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }

  .card-actions {
    justify-content: flex-start;
  }
}
</style>
