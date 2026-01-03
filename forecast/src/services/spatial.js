import apiClient from './http'

const parseNumber = value => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : null
}

const normalizeGeoJson = geoJson => {
  if (!geoJson) {
    return null
  }
  if (typeof geoJson === 'string') {
    try {
      return JSON.parse(geoJson)
    } catch (error) {
      console.warn('无法解析空间 GeoJSON：', error)
      return null
    }
  }
  return geoJson
}

const normalizeRegion = region => {
  if (!region || typeof region !== 'object') {
    return null
  }

  const name = region.name || region.label || region.regionName
  if (!name) {
    return null
  }

  const production = parseNumber(region.production)
  const forecastMin = parseNumber(region.forecastMin ?? region.forecast_min ?? region.lowerBound)
  const forecastMax = parseNumber(region.forecastMax ?? region.forecast_max ?? region.upperBound)
  const riskValue = parseNumber(region.risk ?? region.riskScore)

  const [lng, lat] = Array.isArray(region.center)
    ? region.center
    : [parseNumber(region.longitude), parseNumber(region.latitude)]

  return {
    name,
    production: production ?? 0,
    forecastMin: forecastMin ?? production ?? 0,
    forecastMax: forecastMax ?? production ?? 0,
    risk: Math.min(1, Math.max(0, riskValue ?? 0)),
    center: Number.isFinite(lng) && Number.isFinite(lat) ? [lng, lat] : null,
    childKey: region.childKey || region.childId || region.child_code || null
  }
}

const normalizeHeatmapPoint = point => {
  if (!point || typeof point !== 'object') {
    return null
  }

  const riskValue = parseNumber(point.risk ?? point.value?.[2] ?? point.riskScore) ?? 0
  const lng = parseNumber(point.longitude ?? point.value?.[0])
  const lat = parseNumber(point.latitude ?? point.value?.[1])

  if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
    return null
  }

  return {
    name: point.name || point.label || '',
    value: [lng, lat, Math.min(1, Math.max(0, riskValue))]
  }
}

const normalizeMapDefinition = map => {
  if (!map || typeof map !== 'object') {
    return null
  }

  const key = map.key || map.id || map.code
  if (!key) {
    return null
  }

  const regions = Array.isArray(map.regions) ? map.regions.map(normalizeRegion).filter(Boolean) : []
  const heatmapPoints = Array.isArray(map.heatmapPoints || map.riskPoints)
    ? (map.heatmapPoints || map.riskPoints).map(normalizeHeatmapPoint).filter(Boolean)
    : []

  return {
    key,
    parentKey: map.parentKey || map.parentId || map.parent_code || null,
    mapKey: map.mapKey || map.map_code || key,
    label: map.label || map.name || key,
    description: map.description || map.desc || '',
    geoJson: normalizeGeoJson(map.geoJson || map.geojson || map.geometry),
    regions,
    heatmapPoints
  }
}

export const fetchSpatialMaps = async () => {
  const endpoint =
    typeof import.meta?.env?.VITE_SPATIAL_MAPS_ENDPOINT === 'string' && import.meta.env.VITE_SPATIAL_MAPS_ENDPOINT.trim()
      ? import.meta.env.VITE_SPATIAL_MAPS_ENDPOINT.trim()
      : '/spatial/maps'

  const { data } = await apiClient.get(endpoint)
  const payload = data?.data ?? data ?? {}

  const items = Array.isArray(payload.maps) ? payload.maps : Array.isArray(payload) ? payload : []
  const rootKey = typeof payload.rootKey === 'string' ? payload.rootKey : null

  return {
    maps: items.map(normalizeMapDefinition).filter(Boolean),
    rootKey
  }
}

export default {
  fetchSpatialMaps
}
