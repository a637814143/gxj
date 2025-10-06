import { getAllCropData, getDataStatistics, uploadCropData, getDistinctRegions, getDistinctCropTypes, getDistinctYears } from '@/api/cropData'

const state = {
  cropDataList: [],
  statistics: {},
  regions: [],
  cropTypes: [],
  years: []
}

const mutations = {
  SET_CROP_DATA_LIST: (state, list) => {
    state.cropDataList = list
  },
  SET_STATISTICS: (state, statistics) => {
    state.statistics = statistics
  },
  SET_REGIONS: (state, regions) => {
    state.regions = regions
  },
  SET_CROP_TYPES: (state, cropTypes) => {
    state.cropTypes = cropTypes
  },
  SET_YEARS: (state, years) => {
    state.years = years
  }
}

const actions = {
  // 获取作物数据
  getCropData({ commit }, params) {
    return new Promise((resolve, reject) => {
      getAllCropData(params).then(response => {
        commit('SET_CROP_DATA_LIST', response)
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 获取统计数据
  getStatistics({ commit }, params) {
    return new Promise((resolve, reject) => {
      getDataStatistics(params).then(response => {
        commit('SET_STATISTICS', response)
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 上传作物数据
  uploadCropData(context, file) {
    return new Promise((resolve, reject) => {
      uploadCropData(file).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 获取地区列表
  getRegions({ commit }) {
    return new Promise((resolve, reject) => {
      getDistinctRegions().then(response => {
        commit('SET_REGIONS', response)
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 获取作物类型列表
  getCropTypes({ commit }) {
    return new Promise((resolve, reject) => {
      getDistinctCropTypes().then(response => {
        commit('SET_CROP_TYPES', response)
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 获取年份列表
  getYears({ commit }) {
    return new Promise((resolve, reject) => {
      getDistinctYears().then(response => {
        commit('SET_YEARS', response)
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  }
}

const getters = {
  cropDataList: state => state.cropDataList,
  statistics: state => state.statistics,
  regions: state => state.regions,
  cropTypes: state => state.cropTypes,
  years: state => state.years
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
