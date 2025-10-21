<template>
  <div class="forecast-page">
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
      <div class="hero-side">
        <div class="side-card">
          <div class="side-card-title">预测准备情况</div>
          <div class="side-items">
            <div v-for="item in quickOverview" :key="item.label" class="side-item">
              <div class="side-item-label">{{ item.label }}</div>
              <div class="side-item-value">{{ item.value }}</div>
              <div class="side-item-trend" :class="{ up: item.trend > 0, down: item.trend < 0 }">
                {{ formatTrend(item.trend) }}
              </div>
            </div>
          </div>
          <div class="side-divider" />
          <div class="side-footer">
            <div class="side-footer-title">执行建议</div>
            <ul>
              <li v-for="notice in reminders" :key="notice">{{ notice }}</li>
            </ul>
          </div>
        </div>
        <div class="hero-decor" />
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
import { ElMessage } from 'element-plus'
import BaseChart from '@/components/charts/BaseChart.vue'
import { executeForecast, fetchCrops, fetchForecastHistory, fetchModels, fetchRegions } from '@/services/forecast'

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
let historyRequestId = 0
const loading = ref(false)
const errorMessage = ref('')

const disableSubmit = computed(() => !selectors.regionId || !selectors.cropId || !selectors.modelId)

const hasResult = computed(() => historySeries.value.length > 0 || forecastSeries.value.length > 0)

const metricLabel = computed(() => metadata.value?.valueLabel || '产量')
const metricUnit = computed(() => metadata.value?.valueUnit || '吨 / 公顷')
const historyLegendLabel = computed(() => `历史${metricLabel.value}`)
const forecastLegendLabel = computed(() => `预测${metricLabel.value}`)

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
  const lowerMap = Object.fromEntries(forecastSeries.value.map(item => [item.period, item.lowerBound ?? null]))
  const upperMap = Object.fromEntries(forecastSeries.value.map(item => [item.period, item.upperBound ?? null]))

  const historyData = xAxis.map(period => historyMap[period] ?? null)
  const forecastData = xAxis.map(period => forecastMap[period] ?? null)
  const lowerBand = xAxis.map(period => lowerMap[period] ?? null)
  const upperBand = xAxis.map(period => upperMap[period] ?? null)

  return {
    grid: { top: 48, left: 60, right: 28, bottom: 40 },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: [historyLegendLabel.value, forecastLegendLabel.value, '预测区间'],
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
      },
      {
        name: '预测区间',
        type: 'line',
        data: upperBand,
        lineStyle: { opacity: 0 },
        stack: 'confidence-band',
        areaStyle: {
          color: 'rgba(103, 194, 58, 0.18)'
        },
        emphasis: { focus: 'series' }
      },
      {
        name: '预测区间',
        type: 'line',
        data: lowerBand,
        lineStyle: { opacity: 0 },
        stack: 'confidence-band',
        areaStyle: {
          color: 'rgba(103, 194, 58, 0.18)'
        },
        emphasis: { focus: 'series' }
      }
    ]
  }
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
  regions: fetchRegions,
  crops: fetchCrops,
  models: fetchModels,
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

const fetchOptions = async type => {
  loadingOptions[type] = true
  try {
    const fetcher = optionFetchers[type]
    if (!fetcher) return
    const list = await fetcher()
    optionLists[type] = Array.isArray(list) ? list : []
  } catch (error) {
    ElMessage.error(`加载${type === 'regions' ? '地区' : type === 'crops' ? '作物' : '模型'}列表失败`)
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
    fetchOptions('regions'),
    fetchOptions('crops'),
    fetchOptions('models')
  ])
  historyPagination.currentPage = 1
  await loadForecastHistory(1)
})

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
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 32px;
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
  width: 400px;
  height: 400px;
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

.hero-side {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 24px;
}

.side-card {
  border-radius: 20px;
  padding: 24px;
  background: linear-gradient(160deg, rgba(34, 98, 255, 0.92) 0%, rgba(100, 149, 255, 0.8) 65%, rgba(255, 255, 255, 0.95) 100%);
  color: #fff;
  box-shadow: 0 20px 45px rgba(32, 84, 204, 0.25);
  overflow: hidden;
  position: relative;
}

.side-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), transparent 65%);
  pointer-events: none;
}

.side-card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
}

.side-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(2px);
}

.side-item-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.side-item-value {
  font-size: 18px;
  font-weight: 700;
}

.side-item-trend {
  font-size: 12px;
  font-weight: 500;
}

.side-item-trend.up {
  color: #91ffba;
}

.side-item-trend.down {
  color: #ffd48a;
}

.side-divider {
  height: 1px;
  margin: 24px 0;
  background: rgba(255, 255, 255, 0.28);
}

.side-footer-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.side-footer ul {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.hero-decor {
  height: 140px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8), rgba(214, 228, 255, 0.6));
  box-shadow: inset 0 0 0 1px rgba(34, 98, 255, 0.1);
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
