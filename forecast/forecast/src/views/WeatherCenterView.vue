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
          :teleported="true"
          popper-class="weather-location-dropdown"
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
        <div class="summary-metrics">
          <el-card
            v-for="metric in metricCards"
            :key="metric.label"
            class="summary-card metric-card"
            :class="[`metric-card--${metric.intent}`]"
          >
            <div class="metric-header">
              <span class="metric-icon">{{ metric.icon }}</span>
              <div class="metric-titles">
                <span class="metric-label">{{ metric.label }}</span>
                <span v-if="metric.subLabel" class="metric-sub-label">{{ metric.subLabel }}</span>
              </div>
              <span v-if="metric.badge" class="metric-badge">{{ metric.badge }}</span>
            </div>
            <div class="metric-value">{{ metric.value }}</div>
            <div v-if="metric.detail" class="metric-detail">{{ metric.detail }}</div>
            <div v-if="metric.footer" class="metric-footer">{{ metric.footer }}</div>
          </el-card>
        </div>
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
        <el-card
          :class="['insight-card', 'insight-card--callout', keypointInfo.accentClass]"
        >
          <template #header>
            <div class="card-header card-header--callout">
              <div class="card-icon">{{ keypointInfo.icon }}</div>
              <div class="card-heading">
                <span class="card-title">重点提示</span>
                <span class="card-subtitle">{{ keypointInfo.subtitle }}</span>
              </div>
              <span v-if="keypointInfo.badge" class="card-badge">{{ keypointInfo.badge }}</span>
            </div>
          </template>
          <div class="insight-body callout-body">
            <p class="insight-text">{{ keypointInfo.message }}</p>
            <ul v-if="keypointInfo.actions.length" class="callout-actions">
              <li v-for="action in keypointInfo.actions" :key="action">{{ action }}</li>
            </ul>
          </div>
        </el-card>
        <el-card
          :class="['insight-card', 'insight-card--air', airQualitySummary.accentClass]"
        >
          <template #header>
            <div class="card-header card-header--air">
              <div class="card-icon">🌫️</div>
              <div class="card-heading">
                <span class="card-title">空气质量</span>
                <span class="card-subtitle">{{ airQualitySummary.subtitle }}</span>
              </div>
              <span v-if="airQualitySummary.badge" class="card-badge">{{ airQualitySummary.badge }}</span>
            </div>
          </template>
          <div class="insight-body air-quality">
            <div class="aq-overview">
              <span class="aq-overview-value">{{ airQualitySummary.aqi }}</span>
              <span class="aq-overview-label">AQI</span>
            </div>
            <div class="aq-metrics">
              <div
                v-for="metric in airQualitySummary.metrics"
                :key="metric.label"
                class="aq-pill"
                :class="`aq-pill--${metric.severity}`"
              >
                <span class="aq-pill-label">{{ metric.label }}</span>
                <span class="aq-pill-value">{{ metric.value }}</span>
              </div>
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

const metricCards = computed(() => {
  const humidityValue = toFiniteNumber(currentWeather.value?.humidity)
  const pressureValue = toFiniteNumber(currentWeather.value?.pressure)
  const visibilityValue = toFiniteNumber(currentWeather.value?.visibility)
  const precipitation = precipitationLabel.value
  const windSpeedValue = toFiniteNumber(currentWeather.value?.wind?.speed)
  const windDirection = formatDirection(currentWeather.value?.wind?.direction)
  const aqiValue = toFiniteNumber(currentWeather.value?.airQuality?.aqi)
  const airDescription = currentWeather.value?.airQuality?.description || '—'

  return [
    {
      intent: 'moisture',
      icon: '💧',
      label: '空气湿度',
      subLabel: describeHumidityLevel(humidityValue),
      value: formatPercent(currentWeather.value?.humidity),
      detail: humidityValue === null ? '' : `露点 ${formatNumber(currentWeather.value?.dewPoint)}℃`
    },
    {
      intent: 'pressure',
      icon: '🎯',
      label: '大气压力',
      subLabel: pressureValue === null ? '' : '海平面气压',
      value: pressureValue === null ? '—' : `${formatNumber(pressureValue)} hPa`,
      detail: visibilityValue === null ? '' : `能见度 ${formatNumber(visibilityValue)} km`
    },
    {
      intent: 'wind',
      icon: '🍃',
      label: '风场状况',
      subLabel: describeWindLevel(windSpeedValue),
      value: windSpeedValue === null ? '—' : `${formatNumber(windSpeedValue)} m/s`,
      detail: `风向 ${windDirection}`,
      footer: precipitation && precipitation !== '—' ? `降水 ${precipitation}` : ''
    },
    {
      intent: 'air',
      icon: '🌫️',
      label: '空气质量',
      subLabel: airDescription !== '—' ? airDescription : describeAqiLevel(aqiValue)?.label,
      value: aqiValue === null ? '—' : formatNumber(aqiValue),
      badge: describeAqiLevel(aqiValue)?.badge || '',
      detail: comfortLevel.value
    }
  ]
})

const farmingRecommendations = computed(() => [
  { icon: '🧑‍🌾', title: '作业舒适度', text: comfortLevel.value },
  { icon: '⏱️', title: '作业窗口', text: operationWindow.value },
  { icon: '🌱', title: '田间提醒', text: fieldReminder.value }
])

const keypointInfo = computed(() => {
  const weather = currentWeather.value
  const precipitation = toFiniteNumber(weather?.precipitation?.localIntensity)
  const windSpeedValue = toFiniteNumber(weather?.wind?.speed)
  const temperatureValue = toFiniteNumber(weather?.temperature)
  const humidityValue = toFiniteNumber(weather?.humidity)

  let icon = '📡'
  let badge = ''
  let subtitle = '气象条件整体平稳'
  let accentClass = 'insight-card--callout-mild'
  const actions = []

  if (precipitation !== null && precipitation > 0.3) {
    icon = '🌧️'
    badge = '降水关注'
    subtitle = '注意当前的降水过程'
    accentClass = 'insight-card--callout-rain'
    actions.push('提前疏通排水沟渠', '适度推迟采收与喷药作业')
  } else if (windSpeedValue !== null && windSpeedValue > 8) {
    icon = '💨'
    badge = '大风提醒'
    subtitle = '风力较大，注意防护'
    accentClass = 'insight-card--callout-wind'
    actions.push('加固棚膜及遮阳网', '谨慎安排无人机等高空作业')
  } else if (temperatureValue !== null && temperatureValue >= 34) {
    icon = '🔥'
    badge = '高温防护'
    subtitle = '午后体感偏热'
    accentClass = 'insight-card--callout-heat'
    actions.push('避开午后高温时段', '及时补水降温')
  } else if (temperatureValue !== null && temperatureValue <= 5) {
    icon = '❄️'
    badge = '低温防寒'
    subtitle = '注意保温措施'
    accentClass = 'insight-card--callout-cold'
    actions.push('检查棚室保温设施', '加强幼苗防寒防冻')
  } else if (humidityValue !== null && humidityValue >= 85) {
    icon = '💧'
    badge = '湿度偏高'
    subtitle = '加强病害预防'
    accentClass = 'insight-card--callout-humid'
    actions.push('保持通风换气', '关注病虫害巡查')
  }

  const message =
    weather?.forecastKeypoint ||
    weather?.precipitationDescription ||
    (badge
      ? '结合以上提示及时调整田间管理，降低天气带来的影响。'
      : '当前无特别天气提示，可结合实时数据安排日常作业。')

  return {
    icon,
    badge,
    subtitle,
    message,
    actions,
    accentClass
  }
})

const airQualitySummary = computed(() => {
  const air = currentWeather.value?.airQuality || {}
  const aqiValue = toFiniteNumber(air.aqi)
  const descriptor = describeAqiLevel(aqiValue)

  let accentClass = 'insight-card--air-clean'
  if (aqiValue === null) {
    accentClass = 'insight-card--air-unknown'
  } else if (aqiValue > 200) {
    accentClass = 'insight-card--air-poor'
  } else if (aqiValue > 150) {
    accentClass = 'insight-card--air-fair'
  } else if (aqiValue > 100) {
    accentClass = 'insight-card--air-moderate'
  } else if (aqiValue > 50) {
    accentClass = 'insight-card--air-light'
  }

  const metricsConfig = [
    { key: 'pm25', label: 'PM2.5', unit: 'μg/m³' },
    { key: 'pm10', label: 'PM10', unit: 'μg/m³' },
    { key: 'o3', label: 'O₃', unit: 'μg/m³' },
    { key: 'so2', label: 'SO₂', unit: 'μg/m³' },
    { key: 'no2', label: 'NO₂', unit: 'μg/m³' },
    { key: 'co', label: 'CO', unit: 'mg/m³' }
  ]

  const metrics = metricsConfig.map(item => {
    const value = toFiniteNumber(air?.[item.key])
    return {
      label: item.label,
      value: value === null ? '—' : `${formatNumber(value)} ${item.unit}`,
      severity: describePollutantLevel(value, item.key)
    }
  })

  return {
    aqi: aqiValue === null ? '—' : formatNumber(aqiValue),
    subtitle:
      aqiValue === null
        ? '暂无空气质量数据'
        : descriptor?.label || '空气质量监测',
    badge: descriptor?.badge || '',
    metrics,
    accentClass
  }
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

const describeHumidityLevel = value => {
  if (value === null) {
    return ''
  }
  if (value < 40) return '偏干'
  if (value <= 70) return '舒适'
  if (value <= 85) return '偏潮'
  return '高湿'
}

const describeWindLevel = value => {
  if (value === null) {
    return ''
  }
  if (value < 1) return '静风'
  if (value < 5) return '微风'
  if (value < 10) return '和风'
  if (value < 17) return '强风'
  return '大风'
}

const describeAqiLevel = value => {
  if (value === null) {
    return null
  }
  if (value <= 50) {
    return { badge: '优', label: '空气清新' }
  }
  if (value <= 100) {
    return { badge: '良', label: '户外活动适宜' }
  }
  if (value <= 150) {
    return { badge: '轻度', label: '敏感人群需注意' }
  }
  if (value <= 200) {
    return { badge: '中度', label: '减少户外活动' }
  }
  return { badge: '重度', label: '建议暂停户外' }
}

const describePollutantLevel = (value, key) => {
  if (value === null) {
    return 'unknown'
  }

  const thresholdsMap = {
    pm25: [35, 75, 115],
    pm10: [50, 150, 250],
    o3: [160, 215, 265],
    so2: [50, 150, 475],
    no2: [100, 200, 700],
    co: [4, 14, 24]
  }

  const [good, fair, moderate] = thresholdsMap[key] || [50, 150, 250]

  if (value <= good) {
    return 'good'
  }
  if (value <= fair) {
    return 'fair'
  }
  if (value <= moderate) {
    return 'moderate'
  }
  return 'poor'
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

.location-select :deep(.el-input__wrapper) {
  box-shadow: 0 12px 24px rgba(56, 161, 105, 0.18);
  border-radius: 14px;
  padding: 6px 14px;
}

.weather-location-dropdown {
  border-radius: 14px !important;
  box-shadow: 0 18px 40px rgba(30, 136, 118, 0.18) !important;
  overflow: hidden;
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

.summary-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}

.metric-card {
  position: relative;
  overflow: hidden;
  border: none;
}

.metric-card :deep(.el-card__body) {
  padding: 22px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-icon {
  font-size: 24px;
  filter: drop-shadow(0 8px 14px rgba(0, 0, 0, 0.12));
}

.metric-titles {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-label {
  font-size: 15px;
  font-weight: 600;
  color: #0b3d2e;
}

.metric-sub-label {
  font-size: 12px;
  color: rgba(11, 61, 46, 0.65);
}

.metric-badge {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: #0b3d2e;
  background: rgba(255, 255, 255, 0.7);
  padding: 4px 10px;
  border-radius: 999px;
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #0b3d2e;
}

.metric-detail,
.metric-footer {
  font-size: 13px;
  color: #4b636e;
  line-height: 1.6;
}

.metric-footer {
  font-weight: 500;
  color: #0f4c3a;
}

.metric-card--moisture {
  background: linear-gradient(160deg, rgba(178, 235, 242, 0.4), rgba(230, 255, 251, 0.9));
}

.metric-card--pressure {
  background: linear-gradient(155deg, rgba(255, 245, 234, 0.4), rgba(255, 253, 245, 0.92));
}

.metric-card--wind {
  background: linear-gradient(150deg, rgba(225, 245, 254, 0.45), rgba(232, 248, 245, 0.92));
}

.metric-card--air {
  background: linear-gradient(150deg, rgba(240, 244, 255, 0.45), rgba(236, 253, 245, 0.92));
}

.weather-insights {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}

.insight-card {
  border-radius: 18px;
  overflow: hidden;
  border: none;
  box-shadow: 0 16px 36px rgba(11, 61, 46, 0.12);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.94), rgba(233, 246, 242, 0.92));
}

.insight-card :deep(.el-card__header) {
  border-bottom: none;
  padding: 18px 22px 12px;
  background: transparent;
}

.insight-card :deep(.el-card__body) {
  padding: 22px 24px 24px;
}

.insight-card--suggestion {
  background: linear-gradient(140deg, rgba(15, 76, 58, 0.12), rgba(33, 150, 83, 0.08));
}

.insight-card--suggestion :deep(.el-card__body) {
  padding: 22px 24px;
}

.insight-card--callout :deep(.el-card__body) {
  padding: 24px 26px;
}

.insight-card--callout-mild {
  background: linear-gradient(150deg, rgba(224, 247, 250, 0.72), rgba(240, 255, 244, 0.92));
}

.insight-card--callout-rain {
  background: linear-gradient(150deg, rgba(179, 229, 252, 0.75), rgba(232, 248, 255, 0.94));
}

.insight-card--callout-wind {
  background: linear-gradient(150deg, rgba(213, 245, 255, 0.72), rgba(233, 246, 255, 0.92));
}

.insight-card--callout-heat {
  background: linear-gradient(150deg, rgba(255, 224, 178, 0.74), rgba(255, 241, 213, 0.94));
}

.insight-card--callout-cold {
  background: linear-gradient(150deg, rgba(207, 216, 255, 0.74), rgba(240, 247, 255, 0.94));
}

.insight-card--callout-humid {
  background: linear-gradient(150deg, rgba(200, 230, 201, 0.72), rgba(232, 245, 233, 0.94));
}

.insight-card--air-clean {
  background: linear-gradient(160deg, rgba(224, 255, 247, 0.78), rgba(240, 255, 250, 0.98));
}

.insight-card--air-light {
  background: linear-gradient(160deg, rgba(224, 242, 255, 0.76), rgba(240, 248, 255, 0.95));
}

.insight-card--air-moderate {
  background: linear-gradient(160deg, rgba(255, 243, 224, 0.8), rgba(255, 253, 243, 0.96));
}

.insight-card--air-fair {
  background: linear-gradient(160deg, rgba(255, 224, 178, 0.78), rgba(255, 239, 213, 0.95));
}

.insight-card--air-poor {
  background: linear-gradient(160deg, rgba(254, 228, 228, 0.82), rgba(255, 243, 243, 0.97));
}

.insight-card--air-unknown {
  background: linear-gradient(160deg, rgba(236, 239, 241, 0.82), rgba(248, 250, 252, 0.96));
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
  display: flex;
  align-items: center;
  gap: 14px;
  color: #0b3d2e;
}

.card-header--callout,
.card-header--air {
  justify-content: flex-start;
}

.card-icon {
  font-size: 28px;
  line-height: 1;
  filter: drop-shadow(0 10px 18px rgba(11, 61, 46, 0.16));
}

.card-heading {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1 1 auto;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #063a2b;
}

.card-subtitle {
  font-size: 12px;
  letter-spacing: 0.2px;
  color: rgba(6, 58, 43, 0.75);
}

.card-badge {
  margin-left: auto;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(6, 58, 43, 0.14);
  color: #063a2b;
}

.insight-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.callout-body {
  gap: 18px;
}

.insight-text {
  font-size: 15px;
  color: #29434e;
  line-height: 1.75;
  font-weight: 500;
}

.callout-actions {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  color: #375a63;
}

.callout-actions li {
  list-style: disc;
}

.air-quality {
  display: grid;
  grid-template-columns: minmax(120px, 160px) 1fr;
  gap: 16px 20px;
  align-items: stretch;
}

.aq-overview {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(6, 58, 43, 0.1);
  color: #063a2b;
  min-height: 120px;
}

.aq-overview-value {
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
}

.aq-overview-label {
  font-size: 13px;
  margin-top: 6px;
  letter-spacing: 1px;
  text-transform: uppercase;
  opacity: 0.7;
}

.aq-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px 14px;
}

.aq-pill {
  padding: 14px 16px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border: 1px solid rgba(6, 58, 43, 0.08);
  background: rgba(255, 255, 255, 0.5);
}

.aq-pill-label {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: rgba(7, 54, 41, 0.65);
}

.aq-pill-value {
  font-size: 16px;
  font-weight: 600;
}

.aq-pill--good {
  background: rgba(33, 150, 83, 0.12);
  color: #0f5132;
}

.aq-pill--fair {
  background: rgba(255, 193, 7, 0.18);
  color: #79590b;
}

.aq-pill--moderate {
  background: rgba(255, 152, 0, 0.18);
  color: #8a4f08;
}

.aq-pill--poor {
  background: rgba(244, 67, 54, 0.16);
  color: #b71c1c;
}

.aq-pill--unknown {
  background: rgba(96, 125, 139, 0.16);
  color: #37474f;
}

@media (max-width: 880px) {
  .air-quality {
    grid-template-columns: 1fr;
  }

  .aq-overview {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
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
