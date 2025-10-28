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
        <el-card class="summary-card summary-card--hero">
          <div class="hero-card">
            <div class="hero-top">
              <div class="hero-location">
                <span class="hero-location-name">{{ selectedLocationLabel }}</span>
                <span class="hero-updated">更新于 {{ updatedLabel }}</span>
              </div>
              <div class="hero-emoji">{{ skyEmoji }}</div>
            </div>
            <div class="hero-temperature">
              <span class="hero-value">{{ formatNumber(currentWeather.temperature) }}</span>
              <span class="hero-unit">℃</span>
            </div>
            <div class="hero-status-row">
              <span class="hero-status">{{ skyLabel }}</span>
              <span class="hero-feel">体感 {{ formatNumber(currentWeather.apparentTemperature) }}℃</span>
            </div>
            <div class="hero-meta-row">
              <span v-if="observationLabel">观测 {{ observationLabel }}</span>
              <span>湿度 {{ formatPercent(currentWeather.humidity) }}</span>
              <span>风向 {{ formatDirection(currentWeather.wind?.direction) }}</span>
              <span v-if="precipitationLabel !== '—'">降水 {{ precipitationLabel }}</span>
            </div>
            <div v-if="heroHighlights.length" class="hero-highlights">
              <div v-for="chip in heroHighlights" :key="chip.label" class="highlight-chip">
                <span class="chip-icon">{{ chip.icon }}</span>
                <div class="chip-body">
                  <span class="chip-label">{{ chip.label }}</span>
                  <span class="chip-value">{{ chip.value }}</span>
                </div>
              </div>
            </div>
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
        <el-card class="insight-card insight-card--suggestion">
          <template #header>
            <div class="card-header">
              <span>农事建议</span>
            </div>
          </template>
          <ul class="suggestion-list">
            <li v-for="item in farmingRecommendations" :key="item.title" class="suggestion-item">
              <span class="suggestion-icon">{{ item.icon }}</span>
              <div class="suggestion-body">
                <p class="suggestion-title">{{ item.title }}</p>
                <p class="suggestion-text">{{ item.text }}</p>
              </div>
            </li>
          </ul>
        </el-card>
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

const selectedLocation = computed(() =>
  locationOptions.find(option => option.id === selectedLocationId.value) || null
)

const selectedLocationLabel = computed(() => selectedLocation.value?.label || '重点监测站点')

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

const skyEmoji = computed(() => {
  const code = currentWeather.value?.skyCondition
  return SKY_EMOJI_MAP[code] || '🌦️'
})

const heroHighlights = computed(() => {
  const highlights = []
  const humidityValue = toFiniteNumber(currentWeather.value?.humidity)
  if (humidityValue !== null) {
    highlights.push({ icon: '💧', label: '相对湿度', value: formatPercent(humidityValue) })
  }
  const windSpeedValue = toFiniteNumber(currentWeather.value?.wind?.speed)
  if (windSpeedValue !== null) {
    highlights.push({ icon: '🍃', label: '风速', value: `${formatNumber(windSpeedValue)} m/s` })
  }
  const pressureValue = toFiniteNumber(currentWeather.value?.pressure)
  if (pressureValue !== null) {
    highlights.push({ icon: '🎯', label: '气压', value: `${formatNumber(pressureValue)} hPa` })
  }
  const aqiValue = toFiniteNumber(currentWeather.value?.airQuality?.aqi)
  if (aqiValue !== null) {
    highlights.push({ icon: '🌫️', label: '空气质量指数', value: formatNumber(aqiValue) })
  }
  return highlights.slice(0, 3)
})

const comfortLevel = computed(() => {
  const apparent = toFiniteNumber(
    currentWeather.value?.apparentTemperature ?? currentWeather.value?.temperature
  )
  if (apparent === null) {
    return '暂无体感温度数据，建议结合现场感受安排作业。'
  }
  if (apparent < 5) {
    return '体感寒冷，需加强棚室保温并做好防寒措施。'
  }
  if (apparent < 15) {
    return '气温偏凉，可适当通风换气并注意劳作保暖。'
  }
  if (apparent <= 28) {
    return '气温舒适，适宜开展巡田、施肥等田间作业。'
  }
  if (apparent <= 34) {
    return '气温略高，户外作业请避开午后时段并补水防暑。'
  }
  return '闷热天气，减少高强度作业并安排降温和灌溉。'
})

const operationWindow = computed(() => {
  const precipitation = toFiniteNumber(currentWeather.value?.precipitation?.localIntensity)
  if (precipitation !== null && precipitation > 0.3) {
    return '当前有降水过程，暂停喷药、采收等露天作业以免受损。'
  }
  const windSpeedValue = toFiniteNumber(currentWeather.value?.wind?.speed)
  if (windSpeedValue !== null && windSpeedValue > 8) {
    return '风力较大，注意温室覆膜与秧苗固定，谨慎使用无人机作业。'
  }
  return '气象条件平稳，可按计划安排施肥、病虫监测等田间工作。'
})

const fieldReminder = computed(() => {
  const humidityValue = toFiniteNumber(currentWeather.value?.humidity)
  if (humidityValue !== null && humidityValue > 85) {
    return '空气湿度偏高，需加强通风并关注病害滋生风险。'
  }
  const aqiValue = toFiniteNumber(currentWeather.value?.airQuality?.aqi)
  if (aqiValue !== null && aqiValue > 100) {
    return '空气质量略差，缩短户外作业时长并佩戴防护用品。'
  }
  const temperatureValue = toFiniteNumber(currentWeather.value?.temperature)
  if (temperatureValue !== null && temperatureValue < 5) {
    return '气温较低，做好秧苗防寒与设施保温工作。'
  }
  return '暂无明显气象风险，保持常规巡田与田间管理即可。'
})

const farmingRecommendations = computed(() => [
  { icon: '🧑‍🌾', title: '作业舒适度', text: comfortLevel.value },
  { icon: '⏱️', title: '作业窗口', text: operationWindow.value },
  { icon: '🌱', title: '田间提醒', text: fieldReminder.value }
])

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

const SKY_EMOJI_MAP = {
  CLEAR_DAY: '☀️',
  CLEAR_NIGHT: '🌙',
  PARTLY_CLOUDY_DAY: '⛅',
  PARTLY_CLOUDY_NIGHT: '☁️',
  CLOUDY: '☁️',
  LIGHT_HAZE: '🌤️',
  MODERATE_HAZE: '🌥️',
  HEAVY_HAZE: '🌫️',
  LIGHT_RAIN: '🌦️',
  MODERATE_RAIN: '🌧️',
  HEAVY_RAIN: '🌧️',
  STORM_RAIN: '⛈️',
  FOG: '🌫️',
  LIGHT_SNOW: '🌨️',
  MODERATE_SNOW: '❄️',
  HEAVY_SNOW: '❄️',
  STORM_SNOW: '🌨️',
  DUST: '🌪️',
  SAND: '🌪️',
  WIND: '💨'
}
</script>

<style scoped>
.weather-page {
  position: relative;
  padding: 32px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: calc(100vh - 80px);
  background: linear-gradient(135deg, #e7f8f1 0%, #f0f4ff 55%, #ffffff 100%);
  overflow: hidden;
}

.weather-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 15% 20%, rgba(46, 125, 50, 0.18), transparent 55%),
    radial-gradient(circle at 85% 15%, rgba(25, 118, 210, 0.18), transparent 60%),
    radial-gradient(circle at 80% 75%, rgba(255, 202, 40, 0.16), transparent 60%);
  z-index: 0;
  pointer-events: none;
}

.weather-page > * {
  position: relative;
  z-index: 1;
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
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}

.summary-card {
  border-radius: 18px;
  box-shadow: 0 18px 40px rgba(11, 61, 46, 0.12);
  overflow: hidden;
  border: none;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
}

.summary-card :deep(.el-card__body) {
  padding: 24px;
}

.summary-card--hero {
  position: relative;
  color: #f6fffb;
  background: linear-gradient(135deg, #0f4c3a 0%, #18796b 55%, #45b7af 100%);
  box-shadow: 0 24px 60px rgba(15, 76, 58, 0.35);
  grid-column: span 2;
}

.summary-card--hero :deep(.el-card__body) {
  padding: 28px;
  background: transparent;
}

.hero-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.hero-location {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-location-name {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.hero-updated {
  font-size: 12px;
  opacity: 0.8;
}

.hero-emoji {
  font-size: 48px;
  filter: drop-shadow(0 12px 18px rgba(0, 0, 0, 0.25));
}

.hero-temperature {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.hero-value {
  font-size: 60px;
  font-weight: 700;
  line-height: 1;
}

.hero-unit {
  font-size: 20px;
  opacity: 0.85;
}

.hero-status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 16px;
  font-weight: 500;
}

.hero-feel {
  font-size: 14px;
  font-weight: 400;
  opacity: 0.85;
}

.hero-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  font-size: 13px;
  opacity: 0.9;
}

.hero-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.highlight-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(8px);
}

.chip-icon {
  font-size: 20px;
}

.chip-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chip-label {
  font-size: 12px;
  opacity: 0.85;
}

.chip-value {
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 980px) {
  .summary-card--hero {
    grid-column: span 1;
  }
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

.insight-card--suggestion {
  background: linear-gradient(135deg, rgba(15, 76, 58, 0.1), rgba(33, 150, 83, 0.08));
}

.insight-card--suggestion :deep(.el-card__body) {
  padding: 20px 22px;
}

.suggestion-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.suggestion-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.suggestion-icon {
  font-size: 22px;
  line-height: 1;
}

.suggestion-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.suggestion-title {
  font-size: 14px;
  font-weight: 600;
  color: #0b3d2e;
}

.suggestion-text {
  font-size: 13px;
  color: #455a64;
  line-height: 1.6;
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
