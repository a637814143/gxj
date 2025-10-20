import apiClient from './http'

const extractData = response => {
  if (!response) {
    return []
  }
  if (Array.isArray(response)) {
    return response
  }
  if (response?.data) {
    return response.data
  }
  return []
}

export const fetchRegions = async () => {
  const { data } = await apiClient.get('/api/base/regions')
  return extractData(data)
}

export const fetchCrops = async () => {
  const { data } = await apiClient.get('/api/base/crops')
  return extractData(data)
}

export const fetchModels = async () => {
  const { data } = await apiClient.get('/api/forecast/models')
  return extractData(data)
}

const parseNumber = value => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : null
}

const normalizeSeries = series => {
  if (!Array.isArray(series)) {
    return []
  }
  return series
    .filter(item => item && typeof item === 'object')
    .map(item => ({
      period:
        item.period !== undefined && item.period !== null
          ? String(item.period)
          : '',
      value: typeof item.value === 'number' ? item.value : parseNumber(item.value),
      lowerBound:
        typeof item.lowerBound === 'number'
          ? item.lowerBound
          : parseNumber(item.lowerBound),
      upperBound:
        typeof item.upperBound === 'number'
          ? item.upperBound
          : parseNumber(item.upperBound),
    }))
}

export const executeForecast = async payload => {
  const { data } = await apiClient.post('/api/forecast/predict', payload)
  const response = data?.data ?? data ?? {}

  return {
    runId: response.runId ?? null,
    metadata: response.metadata && typeof response.metadata === 'object' ? response.metadata : null,
    history: normalizeSeries(response.history),
    forecast: normalizeSeries(response.forecast),
    metrics: response.metrics && typeof response.metrics === 'object' ? response.metrics : null,
  }
}

export default {
  fetchRegions,
  fetchCrops,
  fetchModels,
  executeForecast,
}
