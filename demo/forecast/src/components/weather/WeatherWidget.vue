<template>
  <div class="weather-widget">
    <div class="weather-top">
      <el-select
        v-model="selectedLocationId"
        size="small"
        class="weather-select"
        placeholder="选择位置"
        :teleported="false"
      >
        <el-option
          v-for="option in locationOptions"
          :key="option.id"
          :label="option.label"
          :value="option.id"
        />
      </el-select>
      <span v-if="updatedLabel" class="weather-updated">{{ updatedLabel }}</span>
    </div>

    <div v-if="isLoading" class="weather-loading">
      <el-skeleton animated :rows="2" />
    </div>

    <el-alert
      v-else-if="errorMessage"
      type="warning"
      :title="errorMessage"
      :closable="false"
      show-icon
      class="weather-alert"
    />

    <div v-else-if="currentWeather" class="weather-body">
      <div class="weather-primary">
        <div class="weather-temp">
          <span class="value">{{ formatNumber(currentWeather.temperature) }}</span>
          <span class="unit">℃</span>
        </div>
        <div class="weather-sky">{{ skyLabel }}</div>
      </div>
      <div class="weather-details">
        <span>体感 {{ formatNumber(currentWeather.apparentTemperature) }}℃</span>
        <span>湿度 {{ formatPercent(currentWeather.humidity) }}</span>
        <span>风速 {{ formatNumber(currentWeather.wind?.speed) }} m/s</span>
        <span v-if="isValidNumber(currentWeather.wind?.direction)">
          风向 {{ formatDirection(currentWeather.wind.direction) }}
        </span>
      </div>
      <div class="weather-secondary">
        <span v-if="isValidNumber(currentWeather.airQuality?.aqi)">
          空气质量 AQI {{ formatNumber(currentWeather.airQuality.aqi) }}
        </span>
        <span v-if="currentWeather.airQuality?.description">
          {{ currentWeather.airQuality.description }}
        </span>
      </div>
      <p v-if="currentWeather.forecastKeypoint" class="weather-tip">
        {{ currentWeather.forecastKeypoint }}
      </p>
      <p v-else-if="currentWeather.precipitationDescription" class="weather-tip">
        {{ currentWeather.precipitationDescription }}
      </p>
    </div>

    <div v-else class="weather-empty">暂无天气数据</div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useWeatherStore, LOCATION_PRESETS } from '../../stores/weather'

const weatherStore = useWeatherStore()

const locationOptions = LOCATION_PRESETS

const selectedLocationId = computed({
  get: () => weatherStore.selectedLocationId,
  set: value => weatherStore.setLocation(value)
})

const currentWeather = computed(() => weatherStore.current)
const isLoading = computed(() => weatherStore.isLoading)
const errorMessage = computed(() => weatherStore.error)

const updatedLabel = computed(() => {
  if (!weatherStore.lastUpdated) {
    return ''
  }
  const date = new Date(weatherStore.lastUpdated)
  return `更新于 ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
})

const skyLabel = computed(() => {
  const sky = currentWeather.value?.skyCondition
  if (!sky) return '—'
  return SKYCON_MAP[sky] || sky
})

const formatNumber = value => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '—'
  }
  return Math.round(numeric * 10) / 10
}

const formatPercent = value => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '—'
  }
  return `${Math.round(numeric)}%`
}

const formatDirection = value => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '—'
  }
  const normalized = ((numeric % 360) + 360) % 360
  const index = Math.round(normalized / 45)
  return WIND_DIRECTIONS[index % WIND_DIRECTIONS.length]
}

const isValidNumber = value => Number.isFinite(Number(value))

onMounted(() => {
  weatherStore.initialize()
  weatherStore.startAutoRefresh()
})

onUnmounted(() => {
  weatherStore.stopAutoRefresh()
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
  WIND: '大风',
  晴: '晴',
  少云: '少云',
  多云: '多云',
  阴: '阴',
  晴间多云: '晴间多云',
  阵雨: '阵雨',
  雷阵雨: '雷阵雨',
  小雨: '小雨',
  中雨: '中雨',
  大雨: '大雨',
  暴雨: '暴雨',
  小雪: '小雪',
  中雪: '中雪',
  大雪: '大雪',
  暴雪: '暴雪',
  雨夹雪: '雨夹雪',
  雾: '雾',
  霾: '雾霾',
  沙尘暴: '沙尘暴'
}

const WIND_DIRECTIONS = ['北', '东北', '东', '东南', '南', '西南', '西', '西北', '北']
</script>

<style scoped>
.weather-widget {
  min-width: 240px;
  max-width: 320px;
  padding: 12px;
  border-radius: 12px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.7));
  box-shadow: 0 8px 20px rgba(15, 81, 45, 0.12);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.weather-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.weather-select {
  flex: 1;
}

.weather-updated {
  font-size: 12px;
  color: #546e7a;
}

.weather-loading,
.weather-empty {
  font-size: 13px;
  color: #607d8b;
  min-height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.weather-alert {
  width: 100%;
}

.weather-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #1a3c34;
}

.weather-primary {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.weather-temp {
  font-size: 28px;
  font-weight: 600;
  color: #0b3d2e;
}

.weather-temp .value {
  line-height: 1;
}

.weather-temp .unit {
  font-size: 14px;
  margin-left: 4px;
  color: #2e7d32;
}

.weather-sky {
  font-size: 14px;
  color: #2e7d32;
  font-weight: 500;
}

.weather-details,
.weather-secondary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  font-size: 12px;
  color: #546e7a;
}

.weather-tip {
  font-size: 12px;
  color: #1b5e20;
  background: rgba(76, 175, 80, 0.1);
  border-radius: 8px;
  padding: 6px 8px;
  line-height: 1.4;
}

@media (max-width: 1200px) {
  .weather-widget {
    min-width: 200px;
  }
}
</style>
