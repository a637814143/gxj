import apiClient from './http'

const fetchProfile = () => apiClient.get('/api/profile/me')

const updateProfile = payload => apiClient.put('/api/profile/me', payload)

const updatePassword = payload => apiClient.put('/api/profile/password', payload)

export default {
  fetchProfile,
  updateProfile,
  updatePassword
}
