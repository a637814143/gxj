<template>
  <div :class="['forecast-page', { 'user-friendly': isUserTheme }]">
    <section class="hero-card">
      <div class="hero-copy">
        <div class="hero-badge">云惠农作业智能分析系统</div>
        <h1 class="hero-title">智能预测运行中心</h1>
        <p class="hero-desc">
          灵活组合地区、作物与模型参数，实时生成历史与预测趋势对比，并输出关键评估指标，辅助业务决策与生产调度。
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
          <div class="extra-title">执行建议</div>
          <ul class="extra-list">
            <li v-for="notice in reminders" :key="notice">{{ notice }}</li>
          </ul>
        </div>
      </div>
    </section>

    <el-card class="panel-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">产量预测配置</div>
            <div class="card-subtitle">选择预测维度与时间窗口，准备好后一键生成预测结果</div>
          </div>
          <el-button type="primary" :disabled="disableSubmit" @click="runForecast">生成预测</el-button>
        </div>
      </template>
      <el-form label-width="96px" label-position="left" class="config-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="地区">
              <el-select
                v-model="selectors.regionId"
                filterable
                placeholder="请选择地区"
                :loading="loadingOptions.regions"
                clearable
              >
                <el-option
                  v-for="region in optionLists.regions"
                  :key="region.id"
                  :label="region.name"
                  :value="region.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="作物">
              <el-select
                v-model="selectors.cropId"
                filterable
                placeholder="请选择作物"
                :loading="loadingOptions.crops"
                :disabled="!selectors.regionId || loadingOptions.regions"
                clearable
              >
                <el-option
                  v-for="crop in optionLists.crops"
                  :key="crop.id"
                  :label="crop.name"
                  :value="crop.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="预测模型">
              <el-select
                v-model="selectors.modelId"
                placeholder="请选择模型"
                :loading="loadingOptions.models"
                clearable
              >
                <el-option
                  v-for="model in optionLists.models"
                  :key="model.id"
                  :label="`${model.name} (${model.type})`"
                  :value="model.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="历史窗口">
              <el-input-number v-model="selectors.historyYears" :min="3" :max="30" />
              <span class="form-hint">年</span>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="预测步数">
              <el-input-number v-model="selectors.forecastPeriods" :min="1" :max="3" :step="1" />
              <span class="form-hint">期 (最多 3 期)</span>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="时间粒度">
              <el-select v-model="selectors.frequency">
                <el-option label="年度" value="YEARLY" />
                <el-option label="季度" value="QUARTERLY" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-alert
        v-if="errorMessage"
        :closable="false"
        type="error"
        class="panel-alert"
        :title="errorMessage"
      />
    </el-card>

    <el-card class="panel-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">历史与预测走势</div>
            <div class="card-subtitle">包含预测区间、历史参考与最新生成时间，实时关注模型表现</div>
          </div>
          <el-tag v-if="metadata" size="large" type="info">生成时间：{{ generatedAtLabel }}</el-tag>
        </div>
      </template>
      <div class="chart-body">
        <el-skeleton v-if="loading" animated :rows="6" />
        <template v-else>
          <BaseChart v-if="hasResult" :option="chartOption" :height="360" />
          <el-empty v-else description="请选择参数并点击生成预测以查看结果" />
        </template>
      </div>
    </el-card>

    <el-card v-if="showScenarioPanel" class="panel-card scenario-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">多情景预测对比</div>
            <div class="card-subtitle">结合预测结果推演不同气候情景下的产量变化</div>
          </div>
          <el-tag type="warning" effect="light">气候范围：0 - 0.2℃（微幅增温情景）</el-tag>
        </div>
      </template>
      <div class="scenario-content">
        <div class="scenario-chart">
          <BaseChart v-if="hasScenarioData" :option="scenarioChartOption" :height="340" />
          <el-empty v-else description="暂无情景数据，请先生成预测" />
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

    <el-card v-if="metadata || metrics" class="panel-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">预测详情</div>
            <div class="card-subtitle">展示当前预测任务上下文信息与模型评估指标</div>
          </div>
        </div>
      </template>
      <div class="detail-grid">
        <div class="detail-card" v-if="metadata">
          <h3>运行参数</h3>
          <ul>
            <li v-if="runId"><span>运行编号</span><strong>#{{ runId }}</strong></li>
            <li v-if="forecastResultId">
              <span>结果 ID</span>
              <strong>
                #{{ forecastResultId }}
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="copyForecastResultId(forecastResultId)"
                >
                  复制
                </el-button>
              </strong>
            </li>
            <li><span>地区</span><strong>{{ metadata.regionName }}</strong></li>
            <li><span>作物</span><strong>{{ metadata.cropName }}</strong></li>
            <li><span>模型</span><strong>{{ metadata.modelName }} ({{ metadata.modelType }})</strong></li>
            <li><span>历史窗口</span><strong>{{ selectors.historyYears }} 年</strong></li>
            <li><span>预测步数</span><strong>{{ metadata.forecastPeriods }} 期</strong></li>
            <li><span>时间粒度</span><strong>{{ metadata.frequency === 'YEARLY' ? '年度' : '季度' }}</strong></li>
            <li><span>指标</span><strong>{{ metricLabel }}（{{ metricUnit }}）</strong></li>
          </ul>
        </div>
        <div class="detail-card" v-if="metrics">
          <h3>模型评估</h3>
          <ul>
            <li><span>MAE</span><strong>{{ formatMetric(metrics.mae) }}</strong></li>
            <li><span>RMSE</span><strong>{{ formatMetric(metrics.rmse) }}</strong></li>
            <li><span>MAPE</span><strong>{{ formatMetric(metrics.mape) }}%</strong></li>
            <li><span>R²</span><strong>{{ formatMetric(metrics.r2) }}</strong></li>
          </ul>
        </div>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">预测记录</div>
            <div class="card-subtitle">保存最近的预测结果，便于复盘与调用</div>
          </div>
          <el-tag v-if="hasHistoryRecords" size="large" type="success">
            共 {{ historyPagination.total }} 条
          </el-tag>
        </div>
      </template>
      <el-table
        :data="forecastHistory"
        :stripe="true"
        :border="false"
        v-loading="historyLoading"
        empty-text="暂无预测记录，请先生成预测"
      >
        <el-table-column prop="period" label="预测期" width="110" />
        <el-table-column label="结果ID" width="150">
          <template #default="{ row }">
            <div class="history-id">
              <span v-if="row.forecastResultId">#{{ row.forecastResultId }}</span>
              <span v-else>-</span>
              <el-button
                v-if="row.forecastResultId"
                type="primary"
                link
                size="small"
                @click="copyForecastResultId(row.forecastResultId)"
              >
                复制
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="regionName" label="地区" min-width="160" />
        <el-table-column prop="cropName" label="作物" min-width="140" />
        <el-table-column prop="modelName" label="模型" min-width="160">
          <template #default="{ row }">{{ row.modelName }} ({{ row.modelType }})</template>
        </el-table-column>
        <el-table-column label="指标预测值" min-width="160">
          <template #default="{ row }">
            {{ formatHistoryNumber(row.measurementValue) }}
            <span class="history-unit" v-if="row.measurementUnit">{{ row.measurementUnit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="推算总产量 (吨)" min-width="160">
          <template #default="{ row }">{{ formatHistoryNumber(row.predictedProduction) }}</template>
        </el-table-column>
        <el-table-column label="推算单产 (吨/公顷)" min-width="170">
          <template #default="{ row }">{{ formatHistoryNumber(row.predictedYield) }}</template>
        </el-table-column>
        <el-table-column label="参考播种面积 (公顷)" min-width="190">
          <template #default="{ row }">{{ formatHistoryNumber(row.sownArea) }}</template>
        </el-table-column>
        <el-table-column label="参考均价 (元/公斤)" min-width="190">
          <template #default="{ row }">{{ formatHistoryNumber(row.averagePrice) }}</template>
        </el-table-column>
        <el-table-column label="预计收益 (万元)" min-width="170">
          <template #default="{ row }">{{ formatHistoryNumber(row.estimatedRevenue) }}</template>
        </el-table-column>
        <el-table-column label="生成时间" min-width="200">
          <template #default="{ row }">{{ formatHistoryDateTime(row.generatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              link
              size="small"
              :disabled="deletingRunId === row.runId"
              @click="handleDeleteHistory(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div
        v-if="historyPagination.total > historyPagination.pageSize"
        class="table-pagination"
      >
        <el-pagination
          background
          layout="prev, pager, next"
          :total="historyPagination.total"
          :page-size="historyPagination.pageSize"
          :current-page="historyPagination.currentPage"
          @current-change="handleHistoryPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseChart from '@/components/charts/BaseChart.vue'
import {
  deleteForecastRun,
  executeForecast,
  fetchForecastHistory,
  fetchModels,
  fetchRegionCrops,
  fetchRegions,
} from '@/services/forecast'
import { useAuthStore } from '@/stores/auth'

const selectors = reactive({
  regionId: null,
  cropId: null,
  modelId: null,
  historyYears: 10,
  forecastPeriods: 3,
  frequency: 'YEARLY'
})

const optionLists = reactive({
  regions: [],
  crops: [],
  models: []
})

const cropRequestId = ref(0)

const authStore = useAuthStore()
const isUserTheme = computed(() => {
  const roles = authStore.user?.roles
  if (!roles) return true
  if (Array.isArray(roles)) {
    return !roles.includes('ADMIN')
  }
  return roles !== 'ADMIN'
})

const loadingOptions = reactive({
  regions: false,
  crops: false,
  models: false
})

const selectedRegion = computed(
  () => optionLists.regions.find(region => region.id === selectors.regionId) || null
)
const selectedCrop = computed(
  () => optionLists.crops.find(crop => crop.id === selectors.cropId) || null
)

const historySeries = ref([])
const forecastSeries = ref([])
const metrics = ref(null)
const metadata = ref(null)
const runId = ref(null)
const forecastResultId = ref(null)
const forecastHistory = ref([])
const historyPagination = reactive({
  currentPage: 1,
  pageSize: 5,
  total: 0,
})
const historyLoading = ref(false)
const deletingRunId = ref(null)
let historyRequestId = 0
const loading = ref(false)
const errorMessage = ref('')

const disableSubmit = computed(() => !selectors.regionId || !selectors.cropId || !selectors.modelId)

const hasResult = computed(() => historySeries.value.length > 0 || forecastSeries.value.length > 0)

const metricLabel = computed(() => metadata.value?.valueLabel || '产量')
const metricUnit = computed(() => metadata.value?.valueUnit || '吨 / 公顷')
const historyLegendLabel = computed(() => `历史${metricLabel.value}`)
const forecastLegendLabel = computed(() => `预测${metricLabel.value}`)

const DEFAULT_SCENARIO_CONFIGS = [
  {
    key: 'baseline',
    label: '基准',
    temperature: '当前气候（0℃ 基准）',
    delta: 0,
    color: '#2563eb',
    description: '保持当前气候条件，沿用历史增长趋势的预测基线。',
    isBaseline: true
  },
  {
    key: 'warming01',
    label: '微升+0.1℃',
    temperature: '+0.1℃ 轻微增温情景',
    delta: -0.015,
    color: '#f97316',
    description: '在 0.1℃ 的增温假设下，模型推测轻微减产，可关注灌溉与遮阴调控。'
  },
  {
    key: 'warming02',
    label: '微升+0.2℃',
    temperature: '+0.2℃ 微幅增温情景',
    delta: -0.028,
    color: '#dc2626',
    description: '当气温提升至 0.2℃ 时，需及早部署抗热品种与生产保障方案。'
  }
]

const SCENARIO_COLOR_PALETTE = ['#2563eb', '#f97316', '#dc2626', '#0f766e', '#9333ea', '#facc15']

function formatTrend(value) {
  if (value > 0) return `较昨日 +${value}`
  if (value < 0) return `较昨日 ${value}`
  return '较昨日 持平'
}

const highlightStats = computed(() => {
  const regionCount = optionLists.regions.length
  const cropCount = optionLists.crops.length
  const modelCount = optionLists.models.length
  return [
    {
      label: '可选地区',
      value: `${regionCount} 个`,
      sub: regionCount ? '覆盖重点产区与示范区' : '等待同步地区基础数据'
    },
    {
      label: '可选作物',
      value: `${cropCount} 种`,
      sub: cropCount ? '涵盖粮食、经济与特色作物' : '请同步作物基础库'
    },
    {
      label: '预测模型',
      value: `${modelCount} 个`,
      sub: modelCount ? '支持多模型对比与评估' : '等待注册预测模型'
    }
  ]
})

const quickOverview = computed(() => [
  {
    label: '历史窗口',
    value: `${selectors.historyYears} 年`,
    trend: selectors.historyYears > 10 ? 1 : selectors.historyYears < 10 ? -1 : 0
  },
  {
    label: '预测步数',
    value: `${selectors.forecastPeriods} 期`,
    trend: selectors.forecastPeriods > 3 ? 1 : selectors.forecastPeriods < 3 ? -1 : 0
  },
  {
    label: '最新结果',
    value: hasResult.value ? '已生成' : '待运行',
    trend: hasResult.value ? 1 : 0
  }
])

const reminders = [
  '建议在每次参数调整后重新生成预测结果',
  '如需对比不同模型，可复制当前配置并切换模型',
  '关注预测误差指标，及时优化数据质量和模型配置'
]

const hasHistoryRecords = computed(() => historyPagination.total > 0)

const combinedPeriods = computed(() => {
  const historyPeriods = historySeries.value.map(item => item.period)
  const forecastPeriods = forecastSeries.value.map(item => item.period)
  return Array.from(new Set([...historyPeriods, ...forecastPeriods]))
})

const chartOption = computed(() => {
  if (!historySeries.value.length && !forecastSeries.value.length) {
    return { tooltip: { trigger: 'axis' } }
  }

  const xAxis = combinedPeriods.value
  const historyMap = Object.fromEntries(historySeries.value.map(item => [item.period, item.value]))
  const forecastMap = Object.fromEntries(forecastSeries.value.map(item => [item.period, item.value]))
  const historyData = xAxis.map(period => historyMap[period] ?? null)
  const forecastData = xAxis.map(period => forecastMap[period] ?? null)

  return {
    grid: { top: 48, left: 60, right: 28, bottom: 40 },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: [historyLegendLabel.value, forecastLegendLabel.value],
      top: 10
    },
    xAxis: {
      type: 'category',
      data: xAxis,
      axisLabel: { interval: 0 }
    },
    yAxis: {
      type: 'value',
      name: metricUnit.value,
      axisLabel: {
        formatter: value => (value == null ? '' : Number(value).toFixed(2))
      }
    },
    series: [
      {
        name: historyLegendLabel.value,
        type: 'line',
        smooth: true,
        showSymbol: true,
        data: historyData,
        lineStyle: { color: '#409EFF' }
      },
      {
        name: forecastLegendLabel.value,
        type: 'line',
        smooth: true,
        showSymbol: true,
        data: forecastData,
        lineStyle: {
          type: 'dashed',
          color: '#67C23A'
        }
      }
    ]
  }
})

const parseNumeric = value => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : null
}

const forecastPoints = computed(() =>
  forecastSeries.value.map(point => ({
    label: point?.period ?? point?.label ?? '',
    value: parseNumeric(point?.value)
  }))
)

const resolvedScenarioConfigs = computed(() => {
  const scenarios = Array.isArray(metadata.value?.scenarioComparisons)
    ? metadata.value.scenarioComparisons
    : []

  if (scenarios.length) {
    return scenarios.map((scenario, index) => {
      const color = scenario.color || SCENARIO_COLOR_PALETTE[index % SCENARIO_COLOR_PALETTE.length]
      const normalizeSeries = Array.isArray(scenario.series)
        ? scenario.series.map(point => ({
            label: point?.label ?? point?.period ?? point?.name ?? '',
            value: parseNumeric(point?.value)
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
        change: typeof scenario.change === 'number' ? scenario.change : null,
        series: normalizeSeries,
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

const showScenarioPanel = computed(() => hasResult.value && hasScenarioData.value)

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
        `<span style='font-weight:600;'>${formatNumber(item.data)} 吨</span>` +
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
      tooltip: { trigger: 'axis' },
      legend: { data: [] },
      grid: { top: 48, left: 40, right: 24, bottom: 32 },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' }
    }
  }

  const baseline = scenarios.find(config => config.isBaseline) ?? scenarios[0]
  const categories = Array.from(
    new Set(
      baseline.series
        .map(point => point.label)
        .filter(label => label !== undefined && label !== null)
    )
  )

  const series = scenarios.map(config => {
    const dataMap = new Map(
      config.series
        .filter(point => point && point.label !== undefined && point.label !== null)
        .map(point => [point.label, point.value])
    )
    const data = categories.map(label => {
      const value = dataMap.get(label)
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

const generatedAtLabel = computed(() => {
  if (!metadata.value?.generatedAt) return ''
  const time = new Date(metadata.value.generatedAt)
  if (Number.isNaN(time.getTime())) {
    return metadata.value.generatedAt
  }
  return time.toLocaleString('zh-CN', { hour12: false })
})

const resetResult = (options = { keepError: false }) => {
  historySeries.value = []
  forecastSeries.value = []
  metrics.value = null
  metadata.value = null
  runId.value = null
  forecastResultId.value = null
  if (!options.keepError) {
    errorMessage.value = ''
  }
}

const optionFetchers = {
  models: fetchModels,
}

const loadCropOptions = async regionId => {
  cropRequestId.value += 1
  const currentRequest = cropRequestId.value

  if (!regionId) {
    optionLists.crops = []
    selectors.cropId = null
    return
  }

  loadingOptions.crops = true
  try {
    const list = await fetchRegionCrops(regionId)
    if (currentRequest !== cropRequestId.value) {
      return
    }
    optionLists.crops = Array.isArray(list) ? list : []

    const selectedExists = optionLists.crops.some(item => item && item.id === selectors.cropId)
    if (!selectedExists) {
      selectors.cropId = null
    }

    if (!optionLists.crops.length) {
      ElMessage.info('该地区暂无作物数据，请先导入对应作物记录')
    }
  } catch (error) {
    if (currentRequest === cropRequestId.value) {
      optionLists.crops = []
      selectors.cropId = null
      ElMessage.error(error?.response?.data?.message || '加载作物列表失败')
    }
  } finally {
    if (currentRequest === cropRequestId.value) {
      loadingOptions.crops = false
    }
  }
}

const loadRegionOptions = async () => {
  loadingOptions.regions = true
  try {
    const list = await fetchRegions()
    optionLists.regions = Array.isArray(list) ? list : []

    if (!optionLists.regions.length) {
      selectors.regionId = null
      optionLists.crops = []
      selectors.cropId = null
      ElMessage.info('暂无可用的天气数据地区')
      return
    }

    const currentRegionId = selectors.regionId
    const hasCurrent = currentRegionId
      && optionLists.regions.some(option => option && option.id === currentRegionId)

    if (hasCurrent) {
      await loadCropOptions(currentRegionId)
    } else {
      selectors.regionId = optionLists.regions[0]?.id ?? null
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载地区列表失败')
    optionLists.regions = []
    optionLists.crops = []
    selectors.regionId = null
    selectors.cropId = null
  } finally {
    loadingOptions.regions = false
  }
}

const loadForecastHistory = async (page = historyPagination.currentPage) => {
  const requestId = ++historyRequestId
  historyLoading.value = true
  try {
    const params = { page, size: historyPagination.pageSize }
    const response = await fetchForecastHistory(params)
    if (requestId !== historyRequestId) {
      return
    }
    forecastHistory.value = Array.isArray(response.items) ? response.items : []
    historyPagination.total = Number.isFinite(response.total) ? response.total : 0
    historyPagination.currentPage = Number.isFinite(response.page) ? response.page : page
    historyPagination.pageSize = Number.isFinite(response.size) ? response.size : historyPagination.pageSize
    if (runId.value && !forecastResultId.value) {
      const match = forecastHistory.value.find(item => item && item.runId === runId.value && item.forecastResultId)
      if (match) {
        forecastResultId.value = match.forecastResultId
      }
    }
  } catch (error) {
    if (requestId === historyRequestId) {
      ElMessage.error(error?.response?.data?.message || '加载预测记录失败')
    }
  } finally {
    if (requestId === historyRequestId) {
      historyLoading.value = false
    }
  }
}

const handleHistoryPageChange = page => {
  historyPagination.currentPage = page
  loadForecastHistory(page)
}

const handleDeleteHistory = async row => {
  if (!row?.runId) {
    ElMessage.warning('缺少运行编号，无法删除该记录')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确定要删除这条预测记录吗？删除后将无法恢复。',
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
  deletingRunId.value = row.runId
  try {
    await deleteForecastRun(row.runId)
    ElMessage.success('预测记录已删除')
    if (runId.value === row.runId) {
      resetResult()
    }
    const nextPage = forecastHistory.value.length <= 1 && historyPagination.currentPage > 1
      ? historyPagination.currentPage - 1
      : historyPagination.currentPage
    historyPagination.currentPage = nextPage
    await loadForecastHistory(nextPage)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '删除预测记录失败')
  } finally {
    if (deletingRunId.value === row.runId) {
      deletingRunId.value = null
    }
  }
}

const fetchOptions = async type => {
  loadingOptions[type] = true
  try {
    const fetcher = optionFetchers[type]
    if (!fetcher) return
    const list = await fetcher()
    optionLists[type] = Array.isArray(list) ? list : []
  } catch (error) {
    ElMessage.error('加载模型列表失败')
  } finally {
    loadingOptions[type] = false
  }
}

const runForecast = async () => {
  if (disableSubmit.value) {
    ElMessage.warning('请选择地区、作物和模型后再发起预测')
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const payload = {
      regionId: selectors.regionId,
      cropId: selectors.cropId,
      modelId: selectors.modelId,
      historyYears: selectors.historyYears,
      forecastPeriods: selectors.forecastPeriods,
      frequency: selectors.frequency
    }
    const result = await executeForecast(payload)
    historySeries.value = result.history
    forecastSeries.value = result.forecast
    metrics.value = result.metrics
    metadata.value = result.metadata
    runId.value = result.runId
    forecastResultId.value = result.forecastResultId ?? null
    if (!historySeries.value.length && !forecastSeries.value.length) {
      ElMessage.warning('预测服务返回了空结果，请检查历史数据是否充足')
    } else {
      ElMessage.success('预测生成成功')
    }
    historyPagination.currentPage = 1
    await loadForecastHistory(1)
  } catch (error) {
    resetResult({ keepError: true })
    errorMessage.value = error?.response?.data?.message || error.message || '预测服务调用失败'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    loadRegionOptions(),
    fetchOptions('models')
  ])
  historyPagination.currentPage = 1
  await loadForecastHistory(1)
})

watch(
  () => selectors.regionId,
  (newRegionId, oldRegionId) => {
    if (newRegionId === oldRegionId) {
      return
    }
    loadCropOptions(newRegionId)
  }
)

watch(
  () => [selectors.regionId, selectors.cropId, selectors.modelId],
  () => {
    resetResult()
  }
)

const formatMetric = value => {
  if (value === null || value === undefined) {
    return '--'
  }
  return Number(value).toFixed(2)
}

const formatNumber = (value, fractionDigits = 2) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--'
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: fractionDigits
  })
}

const copyForecastResultId = async id => {
  if (!id) {
    ElMessage.warning('暂无可复制的结果 ID')
    return
  }
  const text = String(id)
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      throw new Error('clipboard api unavailable')
    }
    ElMessage.success('结果 ID 已复制')
  } catch (error) {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.setAttribute('readonly', '')
    textarea.style.position = 'absolute'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)
    textarea.select()
    try {
      const succeeded = document.execCommand('copy')
      if (succeeded) {
        ElMessage.success('结果 ID 已复制')
      } else {
        throw new Error('execCommand failed')
      }
    } catch (fallbackError) {
      ElMessage.error('复制失败，请手动复制结果 ID')
      console.error('Failed to copy forecastResultId', fallbackError)
    } finally {
      document.body.removeChild(textarea)
    }
  }
}

const formatHistoryNumber = (value, fractionDigits = 2) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: fractionDigits,
  })
}

const formatHistoryDateTime = value => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '-'
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

function formatScenarioChange(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '变化数据缺失'
  }
  const percent = (Number(value) * 100).toFixed(1)
  if (Number(value) > 0) {
    return `预计增加 ${percent}%`
  }
  if (Number(value) < 0) {
    return `预计下降 ${Math.abs(percent)}%`
  }
  return '与基准持平'
}
</script>

<style scoped>
.forecast-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 32px;
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

.panel-card {
  border-radius: 20px;
  overflow: hidden;
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

.config-form {
  margin-top: 8px;
}

.form-hint {
  margin-left: 8px;
  color: #909399;
}

.panel-alert {
  margin-top: 16px;
}

.chart-body {
  min-height: 320px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-top: 8px;
}

.detail-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
}

.detail-card h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

.detail-card ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 8px;
}

.detail-card li {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
}

.detail-card strong {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  font-weight: 600;
  color: #303133;
}

.history-unit {
  margin-left: 4px;
  color: #909399;
  font-size: 12px;
}

.history-id {
  display: flex;
  align-items: center;
  gap: 6px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}

.scenario-card {
  border: 1px solid #e8ebf3;
}

.scenario-content {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);
  gap: 20px;
}

@media (max-width: 1200px) {
  .scenario-content {
    grid-template-columns: 1fr;
  }
}

.scenario-chart {
  min-height: 320px;
}

.scenario-insights {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.scenario-insight-card {
  border-radius: 16px;
  padding: 18px;
  background: linear-gradient(145deg, var(--scenario-accent-soft, rgba(37, 99, 235, 0.1)), rgba(255, 255, 255, 0.95));
  border: 1px solid var(--scenario-accent-border, rgba(37, 99, 235, 0.18));
  box-shadow: 0 18px 30px rgba(15, 23, 42, 0.08);
}

.scenario-insight-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 24px 36px rgba(15, 23, 42, 0.12);
}

.scenario-insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1f2937;
}

.scenario-insight-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--scenario-accent, #2563eb);
  box-shadow: 0 0 0 4px var(--scenario-accent-soft, rgba(37, 99, 235, 0.16));
}

.scenario-insight-temp {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.scenario-insight-value {
  margin-top: 12px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.scenario-insight-change {
  margin-top: 6px;
  font-size: 13px;
  color: #475569;
}

.scenario-insight-change.positive {
  color: #16a34a;
}

.scenario-insight-change.negative {
  color: #dc2626;
}

.scenario-insight-desc {
  margin-top: 10px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.6;
}

/* User theme enhancements */
.forecast-page.user-friendly {
  position: relative;
  padding: 4px 0 40px;
  background: radial-gradient(circle at 18% 22%, rgba(129, 212, 250, 0.22), transparent 55%),
    radial-gradient(circle at 82% 78%, rgba(244, 143, 177, 0.2), transparent 60%),
    linear-gradient(150deg, #f5fbff 0%, #fef7ff 52%, #ffffff 100%);
}

.forecast-page.user-friendly::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.45), transparent 65%);
  pointer-events: none;
}

.forecast-page.user-friendly .hero-card {
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: linear-gradient(115deg, rgba(229, 246, 255, 0.95) 0%, rgba(245, 232, 255, 0.9) 52%, rgba(255, 255, 255, 0.95) 100%);
  box-shadow: 0 28px 68px rgba(136, 132, 255, 0.18);
}

.forecast-page.user-friendly .hero-card::before {
  background: radial-gradient(circle at center, rgba(129, 212, 250, 0.45), transparent 65%);
}

.forecast-page.user-friendly .hero-stat {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(233, 246, 255, 0.85));
  box-shadow: 0 16px 36px rgba(102, 126, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.forecast-page.user-friendly .extra-card {
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.92), rgba(231, 246, 255, 0.88));
  box-shadow: 0 18px 36px rgba(148, 163, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.forecast-page.user-friendly .reminder-card {
  background: linear-gradient(135deg, rgba(165, 180, 252, 0.2), rgba(110, 231, 183, 0.18));
  border: 1px solid rgba(255, 255, 255, 0.55);
}

.forecast-page.user-friendly .extra-title {
  color: #4338ca;
}

.forecast-page.user-friendly .extra-list {
  color: #4c1d95;
}

.forecast-page.user-friendly .panel-card {
  border: none;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.92) 0%, rgba(237, 248, 255, 0.9) 60%, rgba(255, 255, 255, 0.95) 100%);
  box-shadow: 0 28px 54px rgba(148, 163, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.forecast-page.user-friendly :deep(.panel-card .el-card__header) {
  background: linear-gradient(120deg, rgba(230, 244, 255, 0.82), rgba(255, 255, 255, 0.88));
  border-bottom: 1px solid rgba(209, 213, 255, 0.3);
}

.forecast-page.user-friendly :deep(.panel-card .el-card__body) {
  background: transparent;
}

.forecast-page.user-friendly .detail-card {
  border: 1px solid rgba(209, 213, 255, 0.5);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(235, 245, 255, 0.88));
  box-shadow: 0 16px 34px rgba(136, 132, 255, 0.18);
}

.forecast-page.user-friendly .history-id span {
  color: #2563eb;
}

@media (max-width: 992px) {
  .hero-card {
    padding: 24px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-header > :last-child {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }
}

@media (max-width: 768px) {
  .hero-stats {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }

  .card-header > :last-child {
    justify-content: flex-start;
  }
}
</style>
