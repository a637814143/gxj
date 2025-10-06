import request from './request'

export function getPredictionResults(params) {
  return request({
    url: '/prediction/results',
    method: 'get',
    params
  })
}

export function runPrediction(data) {
  return request({
    url: '/prediction/run',
    method: 'post',
    data
  })
}

export function getPredictionModels() {
  return request({
    url: '/prediction/models',
    method: 'get'
  })
}

export function createPredictionModel(data) {
  return request({
    url: '/prediction/models',
    method: 'post',
    data
  })
}

export function updatePredictionModel(id, data) {
  return request({
    url: `/prediction/models/${id}`,
    method: 'put',
    data
  })
}

export function deletePredictionModel(id) {
  return request({
    url: `/prediction/models/${id}`,
    method: 'delete'
  })
}

export function getPredictionResultById(id) {
  return request({
    url: `/prediction/results/${id}`,
    method: 'get'
  })
}

export function getPredictionResultsByRegion(regionCode) {
  return request({
    url: `/prediction/results/region/${regionCode}`,
    method: 'get'
  })
}

export function getPredictionResultsByCropType(cropType) {
  return request({
    url: `/prediction/results/crop-type/${cropType}`,
    method: 'get'
  })
}

export function getPredictionResultsByYear(year) {
  return request({
    url: `/prediction/results/year/${year}`,
    method: 'get'
  })
}

export function getPredictionResultsByRegionAndCrop(regionCode, cropType) {
  return request({
    url: `/prediction/results/region/${regionCode}/crop-type/${cropType}`,
    method: 'get'
  })
}

export function getPredictionResultsByRegionAndYear(regionCode, year) {
  return request({
    url: `/prediction/results/region/${regionCode}/year/${year}`,
    method: 'get'
  })
}

export function getPredictionResultsByRegionAndCropAndYear(regionCode, cropType, year) {
  return request({
    url: `/prediction/results/region/${regionCode}/crop-type/${cropType}/year/${year}`,
    method: 'get'
  })
}

export function getPredictionResultsByScenario(scenarioName) {
  return request({
    url: `/prediction/results/scenario/${scenarioName}`,
    method: 'get'
  })
}

export function runScenarioSimulation(data) {
  return request({
    url: '/prediction/scenario',
    method: 'post',
    data
  })
}

export function exportPredictionResults(params) {
  return request({
    url: '/prediction/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
