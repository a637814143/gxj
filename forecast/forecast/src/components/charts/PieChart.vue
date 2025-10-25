<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps({
  slices: {
    type: Array,
    default: () => []
  },
  height: {
    type: Number,
    default: 320
  }
})

const option = computed(() => {
  const data = props.slices.map(slice => ({
    name: slice.cropName,
    value: slice.production,
    area: slice.sownArea,
    share: slice.share
  }))
  return {
    tooltip: {
      trigger: 'item',
      formatter: params => {
        const area = params.data?.area ?? 0
        const share = params.data?.share ?? 0
        const production = typeof params.value === 'number' ? params.value.toFixed(2) : '-'
        return [
          `${params.name}`,
          `产量：${production} 万吨`,
          `播种面积：${area.toFixed(2)} 千公顷`,
          `占比：${(share * 100).toFixed(1)}%`
        ].join('\n')
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle'
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['60%', '50%'],
        label: {
          formatter: params => `${params.name}\n${(params.percent || 0).toFixed(1)}%`
        },
        data
      }
    ]
  }
})
</script>

<template>
  <BaseChart :option="option" :height="height" />
</template>
