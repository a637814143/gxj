<!--
  @component TrendChart
  @description 产量趋势分析图表组件 - 支持折线图、面积图、柱状图
  @props chartType - 图表类型 (line/area/bar)
  @props data - 图表数据
  @props loading - 加载状态
-->
<template>
  <div class="chart-container">
    <div ref="chartRef" class="chart" />
    <el-empty
      v-if="!loading && !hasData"
      description="暂无可视化数据"
      :image-size="80"
      class="chart-empty"
    />
    <div v-if="summaryText" class="chart-summary">{{ summaryText }}</div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  chartType: { type: String, default: 'line' },
  data: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  subtitle: { type: String, default: '' }
})

const chartRef = ref(null)
const chartInstance = ref(null)

const hasData = computed(() => {
  return props.data?.series?.some(s => s.data?.length > 0) || false
})

const summaryText = computed(() => {
  if (!hasData.value) return ''
  const { series } = props.data
  if (!series?.length) return ''
  
  const totalPoints = series.reduce((sum, s) => sum + (s.data?.length || 0), 0)
  return `共 ${series.length} 个系列，${totalPoints} 个数据点`
})

const initChart = () => {
  if (!chartRef.value) return
  
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
  
  chartInstance.value = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance.value || !hasData.value) return
  
  const { xAxis, series } = props.data
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: series.map(s => s.name),
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xAxis,
      boundaryGap: props.chartType === 'bar'
    },
    yAxis: {
      type: 'value',
      name: '产量(万吨)'
    },
    series: series.map(s => ({
      name: s.name,
      type: props.chartType === 'area' ? 'line' : props.chartType,
      data: s.data,
      smooth: props.chartType !== 'bar',
      areaStyle: props.chartType === 'area' ? {} : undefined
    }))
  }
  
  chartInstance.value.setOption(option, true)
}

const handleResize = () => {
  chartInstance.value?.resize()
}

watch(() => [props.chartType, props.data], () => {
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
.chart-container {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.chart {
  width: 100%;
  height: 400px;
}

.chart-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.chart-summary {
  margin-top: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  text-align: center;
}
</style>
