import apiClient from './http'

const updatePassword = payload => apiClient.put('/api/profile/password', payload)

export default {
  updatePassword
}
