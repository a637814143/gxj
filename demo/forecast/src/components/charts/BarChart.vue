<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  height: {
    type: Number,
    default: 320
  }
})

const option = computed(() => {
  const categories = props.items.map(item => item.regionName)
  const production = props.items.map(item => item.production ?? 0)
  const area = props.items.map(item => item.sownArea ?? 0)
  const yields = props.items.map(item => item.yieldPerHectare ?? 0)

  const format = value => (typeof value === 'number' ? value.toFixed(2) : '-')

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const index = params[0]?.dataIndex ?? 0
        const region = categories[index] ?? ''
        const productionValue = production[index] ?? 0
        const areaValue = area[index] ?? 0
        const yieldValue = yields[index] ?? 0
        return [
          region,
          `产量：${format(productionValue)} 万吨`,
          `播种面积：${format(areaValue)} 千公顷`,
          `平均单产：${format(yieldValue)} 吨/公顷`
        ].join('\n')
      }
    },
    legend: {
      data: ['产量(万吨)', '播种面积(千公顷)']
    },
    grid: { left: '5%', right: '4%', top: '12%', bottom: '6%', containLabel: true },
    xAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#8aa0b3' } },
      splitLine: { lineStyle: { color: '#e4ecf4' } }
    },
    yAxis: {
      type: 'category',
      data: categories,
      axisLine: { lineStyle: { color: '#8aa0b3' } }
    },
    series: [
      {
        name: '产量(万吨)',
        type: 'bar',
        data: production,
        itemStyle: { color: '#1d8efa' },
        barWidth: 16
      },
      {
        name: '播种面积(千公顷)',
        type: 'bar',
        data: area,
        itemStyle: { color: '#7bd88f' },
        barWidth: 16
      }
    ]
  }
})
</script>

<template>
  <BaseChart :option="option" :height="height" />
</template>
