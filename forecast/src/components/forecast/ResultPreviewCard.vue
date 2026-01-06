<!--
  @component ResultPreviewCard
  @description 预测结果预览卡片 - 展示预测结果的图表和数据
  @props result - 预测结果数据
-->
<template>
  <el-card class="result-preview-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <div>
          <div class="card-title">{{ result.cropName }} - {{ result.regionName }}</div>
          <div class="card-subtitle">
            {{ result.modelType }} 模型 · R² = {{ formatR2(result.r2Score) }}
          </div>
        </div>
        <el-tag :type="getR2TagType(result.r2Score)">
          {{ getR2Label(result.r2Score) }}
        </el-tag>
      </div>
    </template>
    
    <div class="result-chart">
      <div ref="chartRef" class="chart" />
    </div>
    
    <el-divider />
    
    <div class="result-data">
      <div class="data-title">预测数据</div>
      <el-table :data="forecastData" size="small" border>
        <el-table-column prop="year" label="年份" width="100" />
        <el-table-column label="预测产量(万吨)" width="140">
          <template #default="{ row }">
            {{ formatNumber(row.value) }}
          </template>
        </el-table-column>
        <el-table-column label="置信区间" min-width="180">
          <template #default="{ row }">
            {{ formatNumber(row.lower) }} ~ {{ formatNumber(row.upper) }}
          </template>
        </el-table-column>
        <el-table-column label="增长率" width="100">
          <template #default="{ row }">
            <span :class="getGrowthClass(row.growth)">
              {{ formatGrowth(row.growth) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <div class="result-summary">
      <el-alert type="info" :closable="false">
        <template #title>预测摘要</template>
        <div class="summary-content">
          <div>平均预测产量: <strong>{{ avgForecast }}</strong> 万吨</div>
          <div>预测趋势: <strong>{{ trendLabel }}</strong></div>
          <div>模型精度: <strong>{{ accuracyLabel }}</strong></div>
        </div>
      </el-alert>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  result: { type: Object, required: true }
})

const chartRef = ref(null)
const chartInstance = ref(null)

const forecastData = computed(() => {
  if (!props.result.predictions) return []
  return props.result.predictions.map((pred, index) => ({
    year: pred.year,
    value: pred.value,
    lower: pred.lowerBound,
    upper: pred.upperBound,
    growth: index > 0 ? ((pred.value - props.result.predictions[index - 1].value) / props.result.predictions[index - 1].value * 100) : 0
  }))
})

const avgForecast = computed(() => {
  if (!forecastData.value.length) return '0.00'
  const avg = forecastData.value.reduce((sum, d) => sum + d.value, 0) / forecastData.value.length
  return avg.toFixed(2)
})

const trendLabel = computed(() => {
  if (forecastData.value.length < 2) return '数据不足'
  const firstValue = forecastData.value[0].value
  const lastValue = forecastData.value[forecastData.value.length - 1].value
  const change = ((lastValue - firstValue) / firstValue * 100)
  if (change > 5) return '上升趋势'
  if (change < -5) return '下降趋势'
  return '平稳趋势'
})

const accuracyLabel = computed(() => {
  const r2 = props.result.r2Score
  if (r2 >= 0.6) return '优秀'
  if (r2 >= 0.4) return '良好'
  if (r2 >= 0.2) return '一般'
  return '较差'
})

const formatNumber = (value) => {
  return Number(value).toFixed(2)
}

const formatR2 = (value) => {
  return value != null ? value.toFixed(3) : '-'
}

const formatGrowth = (value) => {
  const sign = value >= 0 ? '+' : ''
  return `${sign}${value.toFixed(2)}%`
}

const getR2TagType = (r2) => {
  if (r2 >= 0.6) return 'success'
  if (r2 >= 0.4) return 'primary'
  if (r2 >= 0.2) return 'warning'
  return 'danger'
}

const getR2Label = (r2) => {
  if (r2 >= 0.6) return '优秀'
  if (r2 >= 0.4) return '良好'
  if (r2 >= 0.2) return '一般'
  return '较差'
}

const getGrowthClass = (growth) => {
  if (growth > 0) return 'growth-positive'
  if (growth < 0) return 'growth-negative'
  return 'growth-neutral'
}

const initChart = () => {
  if (!chartRef.value) return
  
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
  
  chartInstance.value = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance.value) return
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['历史数据', '预测值', '置信区间']
    },
    xAxis: {
      type: 'category',
      data: [...(props.result.history?.map(h => h.year) || []), ...forecastData.value.map(f => f.year)]
    },
    yAxis: {
      type: 'value',
      name: '产量(万吨)'
    },
    series: [
      {
        name: '历史数据',
        type: 'line',
        data: props.result.history?.map(h => h.value) || [],
        itemStyle: { color: '#409eff' }
      },
      {
        name: '预测值',
        type: 'line',
        data: [...Array(props.result.history?.length || 0).fill(null), ...forecastData.value.map(f => f.value)],
        itemStyle: { color: '#67c23a' },
        lineStyle: { type: 'dashed' }
      },
      {
        name: '置信区间',
        type: 'line',
        data: [...Array(props.result.history?.length || 0).fill(null), ...forecastData.value.map(f => [f.lower, f.upper])],
        areaStyle: { opacity: 0.2 },
        itemStyle: { color: '#e6a23c' },
        lineStyle: { opacity: 0 }
      }
    ]
  }
  
  chartInstance.value.setOption(option)
}

const handleResize = () => {
  chartInstance.value?.resize()
}

watch(() => props.result, () => {
  updateChart()
}, { deep: true })

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance.value?.dispose()
})
</script>

<style scoped>
.result-preview-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.card-subtitle {
  font-size: 13px;
  color: #909399;
}

.result-chart {
  margin-bottom: 20px;
}

.chart {
  width: 100%;
  height: 300px;
}

.result-data {
  margin-bottom: 20px;
}

.data-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
}

.growth-positive {
  color: #67c23a;
  font-weight: 600;
}

.growth-negative {
  color: #f56c6c;
  font-weight: 600;
}

.growth-neutral {
  color: #909399;
}

.result-summary {
  margin-top: 16px;
}

.summary-content {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.8;
}

.summary-content div {
  margin-bottom: 4px;
}
</style>
