import apiClient from './http'

export const fetchRealtimeWeather = params =>
  apiClient.get('/api/weather/realtime', {
    params
  })
