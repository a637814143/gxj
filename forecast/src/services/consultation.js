import apiClient from './http'

export const fetchConsultations = params =>
  apiClient.get('/api/consultations', {
    params
  })

export const createConsultation = payload => apiClient.post('/api/consultations', payload)

export const fetchConsultationMessages = (consultationId, params) =>
  apiClient.get(`/api/consultations/${consultationId}/messages`, {
    params
  })

export const sendConsultationMessage = (consultationId, payload) =>
  apiClient.post(`/api/consultations/${consultationId}/messages`, payload)

export const updateConsultation = (consultationId, payload) =>
  apiClient.patch(`/api/consultations/${consultationId}`, payload)

export const markConsultationRead = consultationId =>
  apiClient.post(`/api/consultations/${consultationId}/read`)

export const closeConsultation = consultationId =>
  apiClient.post(`/api/consultations/${consultationId}/close`)
