const baseRealtime = {
  temperature: 26.4,
  apparentTemperature: 27.1,
  humidity: 68,
  pressure: 1010.6,
  visibility: 12.3,
  dewPoint: 19.2,
  skyCondition: '多云',
  precipitation: {
    localIntensity: 0.2,
    nearestIntensity: 0.6,
    nearestDistance: 18.4
  },
  wind: {
    speed: 3.6,
    direction: 135
  },
  sunrise: '06:24',
  sunset: '19:28',
  forecastKeypoint: '午后有阵雨发展，注意排水与设施防护。',
  precipitationDescription: '预计午后有短时阵雨，雨势不大但需及时排水。',
  airQuality: {
    aqi: 62,
    description: '良',
    category: '良',
    pm25: 28,
    pm10: 45,
    o3: 132,
    so2: 8,
    no2: 20,
    co: 0.6
  }
}

const forecastTemplates = [
  { conditionDay: '多云', conditionNight: '阵雨', conditionText: '多云', tempMax: 26, tempMin: 18, windDirection: 135, windSpeed: 3.2, windScale: '3-4', precipProbability: 40, relativeHumidity: 72, sunshineHours: 5.6 },
  { conditionDay: '阵雨', conditionNight: '小雨', conditionText: '阵雨', tempMax: 25, tempMin: 18, windDirection: 160, windSpeed: 3.6, windScale: '3-4', precipProbability: 55, relativeHumidity: 78, sunshineHours: 4.1 },
  { conditionDay: '阴', conditionNight: '小雨', conditionText: '阴', tempMax: 24, tempMin: 17, windDirection: 120, windSpeed: 2.8, windScale: '2-3', precipProbability: 35, relativeHumidity: 75, sunshineHours: 3.5 },
  { conditionDay: '多云', conditionNight: '多云', conditionText: '多云', tempMax: 27, tempMin: 18, windDirection: 90, windSpeed: 2.4, windScale: '2-3', precipProbability: 20, relativeHumidity: 68, sunshineHours: 6.5 },
  { conditionDay: '晴', conditionNight: '多云', conditionText: '晴', tempMax: 29, tempMin: 19, windDirection: 70, windSpeed: 2.1, windScale: '2-3', precipProbability: 10, relativeHumidity: 60, sunshineHours: 8.2 },
  { conditionDay: '晴', conditionNight: '晴', conditionText: '晴', tempMax: 30, tempMin: 20, windDirection: 85, windSpeed: 2.6, windScale: '2-3', precipProbability: 5, relativeHumidity: 55, sunshineHours: 8.6 },
  { conditionDay: '多云', conditionNight: '阵雨', conditionText: '多云', tempMax: 28, tempMin: 19, windDirection: 110, windSpeed: 3.0, windScale: '3-4', precipProbability: 30, relativeHumidity: 65, sunshineHours: 6.1 },
  { conditionDay: '阵雨', conditionNight: '小雨', conditionText: '阵雨', tempMax: 26, tempMin: 18, windDirection: 140, windSpeed: 3.5, windScale: '3-4', precipProbability: 50, relativeHumidity: 74, sunshineHours: 4.8 },
  { conditionDay: '小雨', conditionNight: '小雨', conditionText: '小雨', tempMax: 24, tempMin: 17, windDirection: 160, windSpeed: 3.8, windScale: '3-4', precipProbability: 60, relativeHumidity: 80, sunshineHours: 3.2 },
  { conditionDay: '阴', conditionNight: '阴', conditionText: '阴', tempMax: 25, tempMin: 17, windDirection: 130, windSpeed: 2.9, windScale: '2-3', precipProbability: 25, relativeHumidity: 70, sunshineHours: 4.0 },
  { conditionDay: '多云', conditionNight: '多云', conditionText: '多云', tempMax: 27, tempMin: 18, windDirection: 100, windSpeed: 2.5, windScale: '2-3', precipProbability: 20, relativeHumidity: 66, sunshineHours: 5.7 },
  { conditionDay: '晴', conditionNight: '晴', conditionText: '晴', tempMax: 30, tempMin: 20, windDirection: 90, windSpeed: 2.2, windScale: '2-3', precipProbability: 10, relativeHumidity: 58, sunshineHours: 8.9 },
  { conditionDay: '晴间多云', conditionNight: '多云', conditionText: '晴间多云', tempMax: 29, tempMin: 19, windDirection: 95, windSpeed: 2.4, windScale: '2-3', precipProbability: 15, relativeHumidity: 60, sunshineHours: 7.4 },
  { conditionDay: '多云', conditionNight: '阵雨', conditionText: '多云', tempMax: 28, tempMin: 19, windDirection: 120, windSpeed: 2.8, windScale: '2-3', precipProbability: 35, relativeHumidity: 68, sunshineHours: 6.0 },
  { conditionDay: '阵雨', conditionNight: '小雨', conditionText: '阵雨', tempMax: 26, tempMin: 18, windDirection: 140, windSpeed: 3.1, windScale: '3-4', precipProbability: 45, relativeHumidity: 72, sunshineHours: 4.5 }
]

const pad = value => String(value).padStart(2, '0')

const formatDate = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`

export const getRealtimeFallback = location => {
  const locationName = location?.label || '示例监测站'
  const now = new Date()
  return {
    ...baseRealtime,
    locationName,
    observationTime: now.toISOString(),
    wind: { ...baseRealtime.wind },
    precipitation: { ...baseRealtime.precipitation },
    airQuality: { ...baseRealtime.airQuality },
    forecastKeypoint: `${locationName}午后有阵雨发展，注意排水与设施防护。`
  }
}

export const getDailyForecastFallback = () => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  return forecastTemplates.map((item, index) => {
    const target = new Date(today)
    target.setDate(today.getDate() + index)

    return {
      date: formatDate(target),
      conditionDay: item.conditionDay,
      conditionNight: item.conditionNight,
      conditionText: item.conditionText,
      tempMax: item.tempMax,
      tempMin: item.tempMin,
      windDirection: item.windDirection,
      windSpeed: item.windSpeed,
      windScale: item.windScale,
      precipProbability: item.precipProbability,
      relativeHumidity: item.relativeHumidity,
      sunrise: item.sunrise ?? '06:24',
      sunset: item.sunset ?? '19:28',
      sunshineHours: item.sunshineHours,
      uvIndex: item.uvIndex ?? 6
    }
  })
}

export const getAirQualityFallback = () => ({ ...baseRealtime.airQuality })
