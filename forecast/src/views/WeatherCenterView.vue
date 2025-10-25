<template>
  <div class="weather-page">
    <section class="weather-toolbar">
      <div class="toolbar-main">
        <h1 class="toolbar-title">实时天气监测</h1>
        <p class="toolbar-subtitle">
          查看农服重点区域的实时气象数据，辅助安排生产计划和防灾减灾。
        </p>
      </div>
      <div class="toolbar-actions">
        <el-select
          v-model="selectedLocationId"
          placeholder="选择监测地点"
          class="location-select"
          size="large"
          :teleported="false"
        >
          <el-option
            v-for="option in locationOptions"
            :key="option.id"
            :label="option.label"
            :value="option.id"
          />
        </el-select>
        <el-button type="primary" size="large" :loading="isRefreshing" @click="refreshWeather">
          手动刷新
        </el-button>
      </div>
    </section>

    <el-alert
      v-if="errorMessage"
      class="weather-alert"
      type="warning"
      :title="errorMessage"
      :closable="false"
      show-icon
    />

    <div v-if="showSkeleton" class="weather-skeleton">
      <el-skeleton animated :rows="6" />
    </div>

    <div v-else-if="currentWeather" class="weather-content">
      <section class="weather-summary">
        <el-card class="summary-card">
          <div class="summary-main">
            <div class="summary-temp">
              <span class="value">{{ formatNumber(currentWeather.temperature) }}</span>
              <span class="unit">℃</span>
            </div>
            <div class="summary-status">{{ skyLabel }}</div>
            <div class="summary-feel">体感温度 {{ formatNumber(currentWeather.apparentTemperature) }}℃</div>
          </div>
          <div class="summary-meta">
            <span>更新于 {{ updatedLabel }}</span>
            <span v-if="observationLabel">观测时间 {{ observationLabel }}</span>
          </div>
        </el-card>
        <el-card class="summary-card">
          <div class="summary-grid">
            <div class="grid-item">
              <div class="grid-label">空气湿度</div>
              <div class="grid-value">{{ formatPercent(currentWeather.humidity) }}</div>
            </div>
            <div class="grid-item">
              <div class="grid-label">气压</div>
              <div class="grid-value">{{ formatNumber(currentWeather.pressure) }} hPa</div>
            </div>
            <div class="grid-item">
              <div class="grid-label">能见度</div>
              <div class="grid-value">{{ formatNumber(currentWeather.visibility) }} km</div>
            </div>
            <div class="grid-item">
              <div class="grid-label">降水</div>
              <div class="grid-value">{{ precipitationLabel }}</div>
            </div>
          </div>
        </el-card>
        <el-card class="summary-card">
          <div class="summary-grid">
            <div class="grid-item">
              <div class="grid-label">风速</div>
              <div class="grid-value">{{ formatNumber(currentWeather.wind?.speed) }} m/s</div>
            </div>
            <div class="grid-item">
              <div class="grid-label">风向</div>
              <div class="grid-value">{{ formatDirection(currentWeather.wind?.direction) }}</div>
            </div>
            <div class="grid-item">
              <div class="grid-label">空气质量指数</div>
              <div class="grid-value">{{ formatNumber(currentWeather.airQuality?.aqi) }}</div>
            </div>
            <div class="grid-item">
              <div class="grid-label">空气质量描述</div>
              <div class="grid-value">{{ currentWeather.airQuality?.description || '—' }}</div>
            </div>
          </div>
        </el-card>
      </section>

      <section class="weather-insights">
        <el-card class="insight-card">
          <template #header>
            <div class="card-header">
              <span>重点提示</span>
            </div>
          </template>
          <div class="insight-body">
            <p v-if="currentWeather.forecastKeypoint" class="insight-text">
              {{ currentWeather.forecastKeypoint }}
            </p>
            <p v-else-if="currentWeather.precipitationDescription" class="insight-text">
              {{ currentWeather.precipitationDescription }}
            </p>
            <p v-else class="insight-text">当前无特别天气提示，关注实时数据即可。</p>
          </div>
        </el-card>
        <el-card class="insight-card">
          <template #header>
            <div class="card-header">
              <span>空气质量</span>
            </div>
          </template>
          <div class="insight-body air-quality">
            <div class="aq-item">
              <span class="aq-label">PM2.5</span>
              <span class="aq-value">{{ formatNumber(currentWeather.airQuality?.pm25) }} μg/m³</span>
            </div>
            <div class="aq-item">
              <span class="aq-label">PM10</span>
              <span class="aq-value">{{ formatNumber(currentWeather.airQuality?.pm10) }} μg/m³</span>
            </div>
            <div class="aq-item">
              <span class="aq-label">O₃</span>
              <span class="aq-value">{{ formatNumber(currentWeather.airQuality?.o3) }} μg/m³</span>
            </div>
            <div class="aq-item">
              <span class="aq-label">SO₂</span>
              <span class="aq-value">{{ formatNumber(currentWeather.airQuality?.so2) }} μg/m³</span>
            </div>
            <div class="aq-item">
              <span class="aq-label">NO₂</span>
              <span class="aq-value">{{ formatNumber(currentWeather.airQuality?.no2) }} μg/m³</span>
            </div>
            <div class="aq-item">
              <span class="aq-label">CO</span>
              <span class="aq-value">{{ formatNumber(currentWeather.airQuality?.co) }} mg/m³</span>
            </div>
          </div>
        </el-card>
      </section>
    </div>

    <el-empty v-else description="暂无天气数据" />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useWeatherStore, LOCATION_PRESETS } from '../stores/weather'

const weatherStore = useWeatherStore()

const locationOptions = LOCATION_PRESETS

const selectedLocationId = computed({
  get: () => weatherStore.selectedLocationId,
  set: value => weatherStore.setLocation(value)
})

const currentWeather = computed(() => weatherStore.current)
const isLoading = computed(() => weatherStore.isLoading)
const errorMessage = computed(() => weatherStore.error)

const showSkeleton = computed(() => isLoading.value && !currentWeather.value)
const isRefreshing = computed(() => isLoading.value)

const updatedLabel = computed(() => {
  if (!weatherStore.lastUpdated) {
    return '—'
  }
  const date = new Date(weatherStore.lastUpdated)
  return date.toLocaleString()
})

const observationLabel = computed(() => {
  const time = currentWeather.value?.observationTime
  if (!time) {
    return ''
  }
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleString()
})

const precipitationLabel = computed(() => {
  const precipitation = currentWeather.value?.precipitation
  if (!precipitation) {
    return '—'
  }
  const local = toFiniteNumber(precipitation.localIntensity)
  const nearestIntensity = toFiniteNumber(precipitation.nearestIntensity)
  const nearestDistance = toFiniteNumber(precipitation.nearestDistance)
  const localLabel = local !== null ? `${formatNumber(local)} mm/h` : '—'
  if (nearestIntensity !== null && nearestDistance !== null) {
    return `${localLabel}，${formatNumber(nearestDistance)} km 附近降水 ${formatNumber(nearestIntensity)} mm/h`
  }
  return localLabel
})

const skyLabel = computed(() => {
  const sky = currentWeather.value?.skyCondition
  if (!sky) return '—'
  return SKYCON_MAP[sky] || sky
})

const refreshWeather = () => {
  weatherStore.fetchWeather().catch(() => {})
}

const formatNumber = value => {
  if (value === null || value === undefined) {
    return '—'
  }
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '—'
  }
  return Math.round(numeric * 10) / 10
}

const formatPercent = value => {
  if (value === null || value === undefined) {
    return '—'
  }
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '—'
  }
  return `${Math.round(numeric)}%`
}

const formatDirection = value => {
  if (value === null || value === undefined) {
    return '—'
  }
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '—'
  }
  const normalized = ((numeric % 360) + 360) % 360
  const index = Math.round(normalized / 45)
  return WIND_DIRECTIONS[index % WIND_DIRECTIONS.length]
}

const toFiniteNumber = value => {
  if (value === null || value === undefined) {
    return null
  }
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : null
}

onMounted(() => {
  weatherStore.initialize()
})

const SKYCON_MAP = {
  CLEAR_DAY: '晴天',
  CLEAR_NIGHT: '晴夜',
  PARTLY_CLOUDY_DAY: '多云',
  PARTLY_CLOUDY_NIGHT: '多云',
  CLOUDY: '阴',
  LIGHT_HAZE: '轻雾霾',
  MODERATE_HAZE: '中度雾霾',
  HEAVY_HAZE: '重度雾霾',
  LIGHT_RAIN: '小雨',
  MODERATE_RAIN: '中雨',
  HEAVY_RAIN: '大雨',
  STORM_RAIN: '暴雨',
  FOG: '雾',
  LIGHT_SNOW: '小雪',
  MODERATE_SNOW: '中雪',
  HEAVY_SNOW: '大雪',
  STORM_SNOW: '暴雪',
  DUST: '浮尘',
  SAND: '沙尘',
  WIND: '大风'
}

const WIND_DIRECTIONS = ['北', '东北', '东', '东南', '南', '西南', '西', '西北', '北']
</script>

<style scoped>
.weather-page {
  padding: 32px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.weather-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.toolbar-main {
  flex: 1 1 320px;
}

.toolbar-title {
  font-size: 28px;
  font-weight: 600;
  color: #0b3d2e;
  margin-bottom: 8px;
}

.toolbar-subtitle {
  font-size: 14px;
  color: #546e7a;
  max-width: 520px;
  line-height: 1.6;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.location-select {
  min-width: 220px;
}

.weather-alert {
  max-width: 680px;
}

.weather-skeleton {
  background: rgba(255, 255, 255, 0.85);
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 16px 32px rgba(11, 61, 46, 0.08);
}

.weather-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.weather-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.summary-card {
  border-radius: 16px;
  box-shadow: 0 12px 28px rgba(11, 61, 46, 0.12);
  overflow: hidden;
}

.summary-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-temp {
  font-size: 48px;
  font-weight: 600;
  color: #0b3d2e;
  line-height: 1;
}

.summary-temp .unit {
  font-size: 20px;
  margin-left: 4px;
  color: #2e7d32;
}

.summary-status {
  font-size: 18px;
  color: #2e7d32;
}

.summary-feel {
  font-size: 14px;
  color: #607d8b;
}

.summary-meta {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #78909c;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.grid-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.grid-label {
  font-size: 13px;
  color: #78909c;
}

.grid-value {
  font-size: 18px;
  font-weight: 500;
  color: #0b3d2e;
}

.weather-insights {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}

.insight-card {
  border-radius: 16px;
  box-shadow: 0 12px 28px rgba(11, 61, 46, 0.12);
  overflow: hidden;
}

.card-header {
  font-weight: 600;
  color: #0b3d2e;
}

.insight-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.insight-text {
  font-size: 15px;
  color: #455a64;
  line-height: 1.7;
}

.air-quality {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px 16px;
}

.aq-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.aq-label {
  font-size: 13px;
  color: #78909c;
}

.aq-value {
  font-size: 16px;
  color: #0b3d2e;
  font-weight: 500;
}

@media (max-width: 768px) {
  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .location-select {
    flex: 1 1 auto;
  }
}
</style>
