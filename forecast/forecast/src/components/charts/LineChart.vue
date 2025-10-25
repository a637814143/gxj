<script setup>
import * as echarts from 'echarts'
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps({
  points: {
    type: Array,
    default: () => []
  },
  height: {
    type: Number,
    default: 320
  }
})

const option = computed(() => ({
  grid: { left: '8%', right: '4%', top: '10%', bottom: '12%' },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: props.points.map(point => point.label),
    boundaryGap: false,
    axisLine: { lineStyle: { color: '#8aa0b3' } }
  },
  yAxis: {
    type: 'value',
    axisLine: { lineStyle: { color: '#8aa0b3' } },
    splitLine: { lineStyle: { color: '#e4ecf4' } }
  },
  series: [
    {
      type: 'line',
      data: props.points.map(point => point.value),
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0, 122, 255, 0.35)' },
          { offset: 1, color: 'rgba(0, 122, 255, 0.05)' }
        ])
      },
      smooth: true,
      showSymbol: true,
      symbolSize: 8,
      itemStyle: { color: '#1d8efa' },
      lineStyle: { width: 3 }
    }
  ]
}))
</script>

<template>
  <BaseChart :option="option" :height="height" />
</template>
