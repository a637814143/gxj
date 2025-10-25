import axios from 'axios'

const resolveBaseURL = () => {
  const configured = import.meta.env.VITE_ML_API_BASE_URL
  if (typeof configured === 'string' && configured.trim()) {
    return configured.trim()
  }
  return 'http://localhost:5001'
}

const mlClient = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 15000
})

export const getHealthStatus = async () => {
  const { data } = await mlClient.get('/health')
  return data
}

export const previewTrainingData = async limit => {
  const { data } = await mlClient.get('/model/preview', {
    params: { limit }
  })
  return data
}

export const predictCropYield = async payload => {
  const { data } = await mlClient.post('/predict', payload)
  return data
}

export default {
  getHealthStatus,
  previewTrainingData,
  predictCropYield
}
