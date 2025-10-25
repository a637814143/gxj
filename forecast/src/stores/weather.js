import { defineStore } from 'pinia'
import { fetchRealtimeWeather } from '../services/weather'

const STORAGE_KEY = 'cropyield-weather-location'

export const LOCATION_PRESETS = [
  { id: 'guangzhou', label: '广州农服站', longitude: 113.2644, latitude: 23.1291 },
  { id: 'beijing', label: '北京试验田', longitude: 116.4074, latitude: 39.9042 },
  { id: 'chengdu', label: '成都基地', longitude: 104.0665, latitude: 30.5723 },
  { id: 'wuhan', label: '武汉示范区', longitude: 114.3054, latitude: 30.5931 }
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
    isLoading: false,
    error: null,
    lastUpdated: 0,
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
      } catch (error) {
        console.error('获取天气数据失败', error)
        this.error = error?.response?.data?.message || error?.message || '天气数据获取失败'
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
