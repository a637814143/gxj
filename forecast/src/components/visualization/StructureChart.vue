<!--
  @component StructureChart
  @description 种植结构占比图表组件 - 支持饼图、旭日图、雷达图
  @props chartType - 图表类型 (pie/sunburst/radar)
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
  chartType: { type: String, default: 'pie' },
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const chartRef = ref(null)
const chartInstance = ref(null)

const hasData = computed(() => props.data?.length > 0)

const summaryText = computed(() => {
  if (!hasData.value) return ''
  const total = props.data.reduce((sum, item) => sum + (item.value || 0), 0)
  return `共 ${props.data.length} 个类别，总计 ${total.toFixed(2)} 万吨`
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
  
  let option = {}
  
  if (props.chartType === 'pie') {
    option = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} 万吨 ({d}%)'
      },
      legend: {
        orient: 'vertical',
        right: 10,
        top: 'center'
      },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%'
        },
        data: props.data
      }]
    }
  } else if (props.chartType === 'sunburst') {
    option = {
      tooltip: {
        trigger: 'item'
      },
      series: [{
        type: 'sunburst',
        data: props.data,
        radius: [0, '90%'],
        label: {
          rotate: 'radial'
        }
      }]
    }
  } else if (props.chartType === 'radar') {
    const indicator = props.data.map(item => ({
      name: item.name,
      max: Math.max(...props.data.map(d => d.value)) * 1.2
    }))
    
    option = {
      tooltip: {},
      radar: {
        indicator
      },
      series: [{
        type: 'radar',
        data: [{
          value: props.data.map(item => item.value),
          name: '产量分布'
        }]
      }]
    }
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
