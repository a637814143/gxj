import apiClient from './http'

export const fetchRealtimeWeather = params =>
  apiClient.get('/api/weather/realtime', {
    params
  })

export const fetchDailyForecast = params =>
  apiClient.get('/api/weather/forecast/daily', {
    params
  })

export const fetchWeatherDataset = params =>
  apiClient.get('/api/datasets/weather-records', {
    params
  })
