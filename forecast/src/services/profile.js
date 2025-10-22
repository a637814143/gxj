import apiClient from './http'

export async function fetchProfile() {
  const { data } = await apiClient.get('/api/auth/profile')
  return data?.data ?? null
}

export async function updateProfile(payload) {
  const { data } = await apiClient.put('/api/auth/profile', payload)
  return data?.data ?? null
}

export async function changePassword(payload) {
  await apiClient.put('/api/auth/profile/password', payload)
}
