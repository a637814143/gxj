import { getPredictionResults, runPrediction, getPredictionModels } from '@/api/prediction'

const state = {
  predictionResults: [],
  predictionModels: [],
  currentModel: null,
  scenarios: []
}

const mutations = {
  SET_PREDICTION_RESULTS: (state, results) => {
    state.predictionResults = results
  },
  SET_PREDICTION_MODELS: (state, models) => {
    state.predictionModels = models
  },
  SET_CURRENT_MODEL: (state, model) => {
    state.currentModel = model
  },
  SET_SCENARIOS: (state, scenarios) => {
    state.scenarios = scenarios
  }
}

const actions = {
  // 获取预测结果
  getPredictionResults({ commit }, params) {
    return new Promise((resolve, reject) => {
      getPredictionResults(params).then(response => {
        const { data } = response
        commit('SET_PREDICTION_RESULTS', data)
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 运行预测
  runPrediction({ commit }, params) {
    return new Promise((resolve, reject) => {
      runPrediction(params).then(response => {
        const { data } = response
        commit('SET_PREDICTION_RESULTS', data)
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 获取预测模型
  getPredictionModels({ commit }) {
    return new Promise((resolve, reject) => {
      getPredictionModels().then(response => {
        const { data } = response
        commit('SET_PREDICTION_MODELS', data)
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 设置当前模型
  setCurrentModel({ commit }, model) {
    commit('SET_CURRENT_MODEL', model)
  },

  // 获取情景模拟
  getScenarios({ commit }, params) {
    return new Promise((resolve, reject) => {
      getPredictionResults({ ...params, action: 'scenarios' }).then(response => {
        const { data } = response
        commit('SET_SCENARIOS', data)
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  }
}

const getters = {
  predictionResults: state => state.predictionResults,
  predictionModels: state => state.predictionModels,
  currentModel: state => state.currentModel,
  scenarios: state => state.scenarios
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
