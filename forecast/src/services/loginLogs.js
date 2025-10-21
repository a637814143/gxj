import apiClient from './http'

export async function fetchLoginLogSummary() {
  const { data } = await apiClient.get('/api/system/login-logs/summary')
  return data?.data ?? null
}

export async function fetchLoginLogs(params) {
  const { data } = await apiClient.get('/api/system/login-logs', {
    params
  })
  return data?.data ?? null
}

export async function fetchLoginLog(id) {
  const { data } = await apiClient.get(`/api/system/login-logs/${id}`)
  return data?.data ?? null
}

export async function createLoginLog(payload) {
  const { data } = await apiClient.post('/api/system/login-logs', payload)
  return data?.data ?? null
}

export async function updateLoginLog(id, payload) {
  const { data } = await apiClient.put(`/api/system/login-logs/${id}`, payload)
  return data?.data ?? null
}

export async function deleteLoginLog(id) {
  await apiClient.delete(`/api/system/login-logs/${id}`)
}
