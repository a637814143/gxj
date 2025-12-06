import { defineStore } from 'pinia'
import { fetchRealtimeWeather, fetchDailyForecast } from '../services/weather'

const STORAGE_KEY = 'cropyield-weather-location'

export const LOCATION_PRESETS = [
  { id: 'kunming', label: '昆明市', longitude: 102.8329, latitude: 24.8801 },
  { id: 'qujing', label: '曲靖市', longitude: 103.7963, latitude: 25.4839 },
  { id: 'yuxi', label: '玉溪市', longitude: 102.5439, latitude: 24.3500 },
  { id: 'baoshan', label: '保山市', longitude: 99.1617, latitude: 25.1120 },
  { id: 'zhaotong', label: '昭通市', longitude: 103.7172, latitude: 27.3383 },
  { id: 'lijiang', label: '丽江市', longitude: 100.2253, latitude: 26.8721 },
  { id: 'puer', label: '普洱市', longitude: 100.9669, latitude: 22.8252 },
  { id: 'lincang', label: '临沧市', longitude: 100.0869, latitude: 23.8866 },
  { id: 'chuxiong', label: '楚雄彝族自治州', longitude: 101.546, latitude: 25.0445 },
  { id: 'honghe', label: '红河哈尼族彝族自治州', longitude: 103.3842, latitude: 23.3668 },
  { id: 'wenshan', label: '文山壮族苗族自治州', longitude: 104.25, latitude: 23.363 },
  { id: 'xishuangbanna', label: '西双版纳傣族自治州', longitude: 100.7979, latitude: 22.005 },
  { id: 'dali', label: '大理白族自治州', longitude: 100.2676, latitude: 25.6065 },
  { id: 'dehong', label: '德宏傣族景颇族自治州', longitude: 98.5894, latitude: 24.4367 },
  { id: 'nujiang', label: '怒江傈僳族自治州', longitude: 98.8567, latitude: 25.823 },
  { id: 'diqing', label: '迪庆藏族自治州', longitude: 99.7078, latitude: 27.8251 }
]

const readStoredLocation = () => {
  if (typeof window === 'undefined') {
    return null
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed.id === 'string') {
      return parsed.id
    }
  } catch (error) {
    console.warn('读取天气位置缓存失败', error)
  }
  return null
}

const persistLocation = id => {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ id }))
  } catch (error) {
    console.warn('保存天气位置缓存失败', error)
  }
}

const resolveLocationById = id => LOCATION_PRESETS.find(item => item.id === id) || null

export const useWeatherStore = defineStore('weather', {
  state: () => ({
    selectedLocationId: LOCATION_PRESETS[0]?.id || null,
    current: null,
    forecast: [],
    isLoading: false,
    error: null,
    lastUpdated: 0,
    forecastUpdated: 0,
    forecastError: null,
    initialized: false,
    refreshTimer: null
  }),
  getters: {
    selectedLocation(state) {
      return resolveLocationById(state.selectedLocationId) || LOCATION_PRESETS[0]
    }
  },
  actions: {
    initialize() {
      if (this.initialized) {
        return
      }
      const storedId = typeof window !== 'undefined' ? readStoredLocation() : null
      if (storedId && resolveLocationById(storedId)) {
        this.selectedLocationId = storedId
      }
      this.initialized = true
      this.fetchWeather().catch(() => {})
    },
    async fetchWeather() {
      const location = this.selectedLocation
      if (!location) {
        this.current = null
        return
      }
      this.isLoading = true
      this.error = null
      try {
        const { data } = await fetchRealtimeWeather({
          longitude: location.longitude,
          latitude: location.latitude
        })
        const payload = data?.data
        if (!payload) {
          throw new Error('天气数据响应不完整')
        }
        this.current = payload
        this.lastUpdated = Date.now()

        try {
          const { data: forecastResponse } = await fetchDailyForecast({
            longitude: location.longitude,
            latitude: location.latitude
          })
          const payloadData = forecastResponse?.data
          const daily = Array.isArray(payloadData?.daily) ? payloadData.daily : []
          this.forecast = daily
          this.forecastUpdated = resolveForecastTimestamp(payloadData?.fetchedAt)
          this.forecastError = null
        } catch (forecastError) {
          console.warn('获取未来天气预报失败', forecastError)
          this.forecastError =
            forecastError?.response?.data?.message || forecastError?.message || '未来天气预报获取失败'
        }
      } catch (error) {
        console.error('获取天气数据失败', error)
        this.error = error?.response?.data?.message || error?.message || '天气数据获取失败'
        this.forecast = []
        this.forecastUpdated = 0
        this.forecastError =
          error?.response?.data?.message || error?.message || this.forecastError || '未来天气预报获取失败'
      } finally {
        this.isLoading = false
      }
    },
    setLocation(id) {
      if (!id || id === this.selectedLocationId) {
        return
      }
      if (!resolveLocationById(id)) {
        console.warn('尝试设置不存在的天气位置', id)
        return
      }
      this.selectedLocationId = id
      persistLocation(id)
      this.fetchWeather().catch(() => {})
    },
    startAutoRefresh(interval = 5 * 60 * 1000) {
      if (typeof window === 'undefined') {
        return
      }
      this.stopAutoRefresh()
      this.refreshTimer = window.setInterval(() => {
        this.fetchWeather().catch(() => {})
      }, interval)
    },
    stopAutoRefresh() {
      if (this.refreshTimer) {
        window.clearInterval(this.refreshTimer)
        this.refreshTimer = null
      }
    }
  }
})

const resolveForecastTimestamp = value => {
  if (!value) {
    return Date.now()
  }
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? Date.now() : time
}
