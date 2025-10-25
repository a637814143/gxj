<script setup>
import { computed } from 'vue'

const props = defineProps({
  points: {
    type: Array,
    default: () => []
  }
})

const polylinePoints = computed(() => {
  if (!props.points.length) {
    return ''
  }
  const maxValue = Math.max(...props.points.map(point => point.count || 0), 1)
  const stepX = props.points.length === 1 ? 100 : 100 / (props.points.length - 1)

  return props.points
    .map((point, index) => {
      const value = point.count || 0
      const x = index * stepX
      const y = 100 - (value / maxValue) * 90 - 5
      return `${x.toFixed(2)},${y.toFixed(2)}`
    })
    .join(' ')
})

const gradientId = `trend-gradient-${Math.random().toString(36).substring(2, 8)}`
</script>

<template>
  <div class="trend-chart">
    <svg viewBox="0 0 100 100" preserveAspectRatio="none" class="trend-chart__svg">
      <defs>
        <linearGradient :id="gradientId" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stop-color="rgba(51, 147, 255, 0.35)" />
          <stop offset="100%" stop-color="rgba(51, 147, 255, 0)" />
        </linearGradient>
      </defs>
      <polygon
        v-if="polylinePoints"
        :points="`${polylinePoints} 100,100 0,100`"
        :fill="`url(#${gradientId})`"
        fill-opacity="0.6"
      />
      <polyline
        v-if="polylinePoints"
        :points="polylinePoints"
        fill="none"
        stroke="var(--accent-color)"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <g v-for="(point, index) in points" :key="point.label">
        <circle
          v-if="polylinePoints"
          :cx="polylinePoints.split(' ')[index]?.split(',')[0]"
          :cy="polylinePoints.split(' ')[index]?.split(',')[1]"
          r="2.2"
          fill="var(--accent-color)"
        />
      </g>
    </svg>
    <ul class="trend-chart__labels">
      <li v-for="point in points" :key="point.label">
        <span class="label">{{ point.label }}</span>
        <span class="value">{{ point.count }}</span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.trend-chart {
  display: grid;
  gap: 16px;
}

.trend-chart__svg {
  width: 100%;
  height: 220px;
}

.trend-chart__labels {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 8px 12px;
  padding: 0;
  margin: 0;
  font-size: 13px;
  color: var(--muted-color);
}

.trend-chart__labels .value {
  font-weight: 600;
  color: var(--text-primary);
  margin-left: 8px;
}
</style>
