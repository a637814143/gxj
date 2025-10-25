<script setup>
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  option: {
    type: Object,
    default: () => ({})
  },
  height: {
    type: Number,
    default: 320
  }
})

const chartRef = ref(null)
let chartInstance

const renderChart = () => {
  if (!chartInstance) {
    return
  }
  chartInstance.setOption(props.option, true)
}

const resizeHandler = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  renderChart()
  window.addEventListener('resize', resizeHandler)
})

watch(
  () => props.option,
  () => {
    renderChart()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<template>
  <div ref="chartRef" class="chart" :style="{ height: `${props.height}px` }"></div>
</template>

<style scoped>
.chart {
  width: 100%;
}
</style>
