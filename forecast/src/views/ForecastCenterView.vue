<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import BaseChart from '@/components/charts/BaseChart.vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

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

const historySeries = ref([])
const forecastSeries = ref([])
const metrics = ref(null)
const metadata = ref(null)
const loading = ref(false)
const errorMessage = ref('')

const disableSubmit = computed(() => !selectors.regionId || !selectors.cropId || !selectors.modelId)

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

  const historyData = xAxis.map(period => (historyMap[period] ?? null))
  const forecastData = xAxis.map(period => (forecastMap[period] ?? null))
  const lowerBand = xAxis.map(period => (lowerMap[period] ?? null))
  const upperBand = xAxis.map(period => (upperMap[period] ?? null))

  return {
    grid: { top: 48, left: 60, right: 28, bottom: 40 },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['历史产量', '预测产量', '预测区间'],
      top: 10
    },
    xAxis: {
      type: 'category',
      data: xAxis,
      axisLabel: { interval: 0 }
    },
    yAxis: {
      type: 'value',
      name: '吨 / 公顷',
      axisLabel: {
        formatter: value => (value == null ? '' : Number(value).toFixed(2))
      }
    },
    series: [
      {
        name: '历史产量',
        type: 'line',
        smooth: true,
        showSymbol: true,
        data: historyData,
        lineStyle: { color: '#409EFF' }
      },
      {
        name: '预测产量',
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

const resetResult = () => {
  historySeries.value = []
  forecastSeries.value = []
  metrics.value = null
  metadata.value = null
  errorMessage.value = ''
}

const fetchOptions = async (type, url) => {
  loadingOptions[type] = true
  try {
    const { data } = await axios.get(url)
    optionLists[type] = data?.data ?? []
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
    const { data } = await axios.post(`${API_BASE}/api/forecast/predict`, payload)
    const result = data?.data
    historySeries.value = result?.history ?? []
    forecastSeries.value = result?.forecast ?? []
    metrics.value = result?.metrics ?? null
    metadata.value = result?.metadata ?? null
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error.message || '预测服务调用失败'
    ElMessage.error(errorMessage.value)
    resetResult()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    fetchOptions('regions', `${API_BASE}/api/base/regions`),
    fetchOptions('crops', `${API_BASE}/api/base/crops`),
    fetchOptions('models', `${API_BASE}/api/forecast/models`)
  ])
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

const hasResult = computed(() => historySeries.value.length > 0 || forecastSeries.value.length > 0)
</script>

<template>
  <div class="forecast-page">
    <section class="panel">
      <header class="panel__header">
        <div>
          <h2>产量预测配置</h2>
          <p class="panel__subtitle">选择地区、作物和预测模型，配置时间窗口后发起预测</p>
        </div>
        <el-button type="primary" :disabled="disableSubmit" @click="runForecast">生成预测</el-button>
      </header>
      <el-form label-width="96px" label-position="left" class="panel__form">
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
              <el-input-number v-model="selectors.forecastPeriods" :min="1" :max="10" />
              <span class="form-hint">期</span>
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
        class="panel__alert"
        :title="errorMessage"
      />
    </section>

    <section class="panel">
      <header class="panel__header">
        <div>
          <h2>历史与预测走势</h2>
          <p class="panel__subtitle">
            历史产量与模型预测结果对比，包含预测区间与模型评估指标
          </p>
        </div>
        <el-tag v-if="metadata" size="large" type="info">生成时间：{{ generatedAtLabel }}</el-tag>
      </header>
      <div class="panel__body">
        <el-skeleton v-if="loading" animated :rows="6" />
        <template v-else>
          <BaseChart v-if="hasResult" :option="chartOption" :height="360" />
          <el-empty v-else description="请选择参数并点击生成预测以查看结果" />
        </template>
      </div>
    </section>

    <section class="panel" v-if="metadata || metrics">
      <header class="panel__header">
        <div>
          <h2>预测详情</h2>
          <p class="panel__subtitle">展示当前预测任务的上下文信息与模型指标</p>
        </div>
      </header>
      <div class="detail-grid">
        <div class="detail-card" v-if="metadata">
          <h3>运行参数</h3>
          <ul>
            <li><span>地区</span><strong>{{ metadata.regionName }}</strong></li>
            <li><span>作物</span><strong>{{ metadata.cropName }}</strong></li>
            <li><span>模型</span><strong>{{ metadata.modelName }} ({{ metadata.modelType }})</strong></li>
            <li><span>历史窗口</span><strong>{{ selectors.historyYears }} 年</strong></li>
            <li><span>预测步数</span><strong>{{ metadata.forecastPeriods }} 期</strong></li>
            <li><span>时间粒度</span><strong>{{ metadata.frequency === 'YEARLY' ? '年度' : '季度' }}</strong></li>
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
    </section>
  </div>
</template>

<style scoped>
.forecast-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 16px;
}

.panel {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.panel__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.panel__header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.panel__subtitle {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

.panel__form {
  margin-top: 12px;
}

.panel__alert {
  margin-top: 12px;
}

.panel__body {
  min-height: 320px;
}

.form-hint {
  margin-left: 8px;
  color: #909399;
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
  font-weight: 600;
  color: #303133;
}

@media (max-width: 768px) {
  .forecast-page {
    padding: 12px;
  }

  .panel {
    padding: 16px;
  }
}
</style>
