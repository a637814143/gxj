import request from '@/utils/request'

// 获取所有作物数据
export function getAllCropData() {
  return request({
    url: '/api/crop-data',
    method: 'get'
  })
}

// 根据ID获取作物数据
export function getCropDataById(id) {
  return request({
    url: `/api/crop-data/${id}`,
    method: 'get'
  })
}

// 创建作物数据
export function createCropData(data) {
  return request({
    url: '/api/crop-data',
    method: 'post',
    data
  })
}

// 更新作物数据
export function updateCropData(id, data) {
  return request({
    url: `/api/crop-data/${id}`,
    method: 'put',
    data
  })
}

// 删除作物数据
export function deleteCropData(id) {
  return request({
    url: `/api/crop-data/${id}`,
    method: 'delete'
  })
}

// 上传数据文件
export function uploadCropData(file, options = {}) {
  const formData = new FormData()
  formData.append('file', file)
  
  // 添加可选参数
  if (options.dataSource) formData.append('dataSource', options.dataSource)
  if (options.cleanData !== undefined) formData.append('cleanData', options.cleanData)
  if (options.validateData !== undefined) formData.append('validateData', options.validateData)
  if (options.description) formData.append('description', options.description)
  if (options.importType) formData.append('importType', options.importType)
  
  return request({
    url: '/api/crop-data/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 300000 // 5分钟超时
  })
}

// 下载CSV模板
export function downloadCsvTemplate() {
  return request({
    url: '/api/crop-data/template/csv',
    method: 'get',
    responseType: 'blob'
  })
}

// 获取支持的文件格式信息
export function getSupportedFormats() {
  return request({
    url: '/api/crop-data/import/formats',
    method: 'get'
  })
}

// 获取数据统计信息
export function getDataStatistics() {
  return request({
    url: '/api/crop-data/statistics',
    method: 'get'
  })
}

// 根据地区获取数据
export function getCropDataByRegion(regionCode) {
  return request({
    url: `/api/crop-data/region/${regionCode}`,
    method: 'get'
  })
}

// 根据作物类型获取数据
export function getCropDataByCropType(cropType) {
  return request({
    url: `/api/crop-data/crop-type/${cropType}`,
    method: 'get'
  })
}

// 根据年份获取数据
export function getCropDataByYear(year) {
  return request({
    url: `/api/crop-data/year/${year}`,
    method: 'get'
  })
}

// 获取不同的地区代码
export function getDistinctRegions() {
  return request({
    url: '/api/crop-data/regions',
    method: 'get'
  })
}

// 获取不同的作物类型
export function getDistinctCropTypes() {
  return request({
    url: '/api/crop-data/crop-types',
    method: 'get'
  })
}

// 获取不同的年份
export function getDistinctYears() {
  return request({
    url: '/api/crop-data/years',
    method: 'get'
  })
}

// 获取产量统计
export function getYieldStatistics(regionCode, year) {
  return request({
    url: `/api/crop-data/statistics/yield/region/${regionCode}/year/${year}`,
    method: 'get'
  })
}

// 获取产量趋势
export function getYieldTrend(regionCode, cropType, startYear, endYear) {
  return request({
    url: `/api/crop-data/statistics/trend/region/${regionCode}/crop-type/${cropType}`,
    method: 'get',
    params: {
      startYear,
      endYear
    }
  })
}

// 获取面积统计
export function getAreaStatistics(regionCode, year) {
  return request({
    url: `/api/crop-data/statistics/area/region/${regionCode}/year/${year}`,
    method: 'get'
  })
}