<!--
  @component MapChart
  @description 地理分布热力图表组件 - 支持分级着色地图和热力图
  @props chartType - 图表类型 (choropleth/heatmap)
  @props data - 图表数据
  @props loading - 加载状态
-->
<template>
  <div class="chart-container">
    <div ref="chartRef" class="chart map-chart" />
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
import yunnanGeoJson from '../../assets/maps/yunnan.json'

const props = defineProps({
  chartType: { type: String, default: 'choropleth' },
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const chartRef = ref(null)
const chartInstance = ref(null)

const hasData = computed(() => props.data?.length > 0)

const summaryText = computed(() => {
  if (!hasData.value) return ''
  const total = props.data.reduce((sum, item) => sum + (item.value || 0), 0)
  return `覆盖 ${props.data.length} 个地区，总计 ${total.toFixed(2)} 万吨`
})

const initChart = () => {
  if (!chartRef.value) return
  
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
  
  chartInstance.value = echarts.init(chartRef.value)
  
  // 注册云南地图
  echarts.registerMap('yunnan', yunnanGeoJson)
  
  updateChart()
}

const updateChart = () => {
  if (!chartInstance.value || !hasData.value) return
  
  let option = {}
  
  if (props.chartType === 'choropleth') {
    option = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}<br/>产量: {c} 万吨'
      },
      visualMap: {
        min: 0,
        max: Math.max(...props.data.map(d => d.value)),
        text: ['高', '低'],
        realtime: false,
        calculable: true,
        inRange: {
          color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695']
        }
      },
      series: [{
        type: 'map',
        map: 'yunnan',
        roam: true,
        label: {
          show: true,
          fontSize: 10
        },
        emphasis: {
          label: {
            show: true
          },
          itemStyle: {
            areaColor: '#ffd700'
          }
        },
        data: props.data
      }]
    }
  } else if (props.chartType === 'heatmap') {
    option = {
      tooltip: {
        position: 'top'
      },
      visualMap: {
        min: 0,
        max: Math.max(...props.data.map(d => d.value)),
        calculable: true,
        orient: 'horizontal',
        left: 'center',
        bottom: '15%'
      },
      geo: {
        map: 'yunnan',
        roam: true,
        label: {
          show: false
        },
        itemStyle: {
          areaColor: '#f3f3f3',
          borderColor: '#999'
        }
      },
      series: [{
        type: 'scatter',
        coordinateSystem: 'geo',
        data: props.data.map(item => ({
          name: item.name,
          value: item.coord ? [...item.coord, item.value] : [0, 0, item.value]
        })),
        symbolSize: val => Math.sqrt(val[2]) * 3,
        label: {
          formatter: '{b}',
          position: 'right',
          show: false
        },
        emphasis: {
          label: {
            show: true
          }
        }
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

.map-chart {
  height: 500px;
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
