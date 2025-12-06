import apiClient from './http'

export const fetchReportOverview = () => apiClient.get('/api/report')

export const generateReport = payload => apiClient.post('/api/report/generate', payload)

export const fetchReportDetail = reportId => apiClient.get(`/api/report/${reportId}`)

export const fetchCrops = () => apiClient.get('/api/base/crops')

export const fetchRegions = () => apiClient.get('/api/base/regions')
