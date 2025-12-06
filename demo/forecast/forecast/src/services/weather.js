import apiClient from './http'

export const fetchRealtimeWeather = params =>
  apiClient.get('/api/weather/realtime', {
    params
  })

export const fetchWeatherDataset = params =>
  apiClient.get('/api/datasets/weather-records', {
    params
  })

export const fetchWeatherRegions = () =>
  apiClient.get('/api/datasets/weather-records/regions')
