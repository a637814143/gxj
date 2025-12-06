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

const normalizeOption = item => {
  if (!item || typeof item !== 'object') {
    return null
  }
  const id = item.id ?? item.regionId ?? item.cropId
  const name = item.name ?? item.regionName ?? item.cropName
  if (!id || !name) {
    return null
  }
  return {
    id,
    name,
    count: item.count ?? item.recordCount ?? null
  }
}

export const fetchRegions = async () => {
  const { data } = await apiClient.get('/api/datasets/weather-records/regions')
  const payload = extractData(data)
  return payload
    .map(normalizeOption)
    .filter(Boolean)
}

export const fetchRegionCrops = async regionId => {
  if (!regionId) {
    return []
  }
  const { data } = await apiClient.get(`/api/datasets/weather-records/regions/${regionId}/crops`)
  const payload = extractData(data)
  return payload
    .map(normalizeOption)
    .filter(Boolean)
}

export const fetchModels = async () => {
  const { data } = await apiClient.get('/api/forecast/models')
  return extractData(data)
}

export const fetchForecastHistory = async params => {
  const { data } = await apiClient.get('/api/forecast/history', { params })
  const payload = data?.data ?? data ?? {}
  const items = Array.isArray(payload.items) ? payload.items : []
  const total = typeof payload.total === 'number' ? payload.total : 0
  const page = typeof payload.page === 'number' ? payload.page : 1
  const size = typeof payload.size === 'number' ? payload.size : params?.size ?? 5
  return { items, total, page, size }
}

export const deleteForecastRun = async runId => {
  if (runId === undefined || runId === null) {
    throw new Error('缺少预测运行编号')
  }
  await apiClient.delete(`/api/forecast/history/${runId}`)
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
    forecastResultId: response.forecastResultId ?? null,
    metadata: response.metadata && typeof response.metadata === 'object' ? response.metadata : null,
    history: normalizeSeries(response.history),
    forecast: normalizeSeries(response.forecast),
    metrics: response.metrics && typeof response.metrics === 'object' ? response.metrics : null,
  }
}

export default {
  fetchRegions,
  fetchRegionCrops,
  fetchModels,
  fetchForecastHistory,
  deleteForecastRun,
  executeForecast,
}
