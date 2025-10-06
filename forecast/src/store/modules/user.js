import { login, logout, getUserInfo } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

const state = {
  token: getToken(),
  user: null,
  roles: []
}

// 初始化时从cookie读取token
console.log('User模块初始化 - 从cookie读取token:', getToken())

const mutations = {
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_USER: (state, user) => {
    state.user = user
  },
  SET_ROLES: (state, roles) => {
    state.roles = roles
  }
}

const actions = {
  // 用户登录
  login({ commit }, userInfo) {
    const { username, password } = userInfo
    return new Promise((resolve, reject) => {
      login({ username: username.trim(), password: password }).then(response => {
        // 后端直接返回LoginResponse对象，不需要从data字段中取
        console.log('登录响应:', response)
        console.log('Token:', response.token)
        console.log('Roles:', response.roles)
        
        // 先保存token到localStorage
        setToken(response.token)
        console.log('Token已保存到localStorage:', getToken())
        
        // 再更新Vuex状态
        commit('SET_TOKEN', response.token)
        commit('SET_USER', response)
        commit('SET_ROLES', response.roles)
        
        console.log('登录成功，状态已更新')
        console.log('当前Vuex token:', response.token)
        console.log('当前Vuex roles:', response.roles)
        
        // 验证状态是否正确设置
        setTimeout(() => {
          console.log('延迟验证 - localStorage token:', getToken())
        }, 50)
        
        resolve()
      }).catch(error => {
        console.error('登录错误:', error)
        reject(error)
      })
    })
  },

  // 获取用户信息
  getInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      getUserInfo(state.token).then(response => {
        const { data } = response
        if (!data) {
          reject('验证失败，请重新登录。')
        }
        const { roles } = data
        commit('SET_USER', data)
        commit('SET_ROLES', roles)
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 用户登出
  logout({ commit, state }) {
    return new Promise((resolve, reject) => {
      logout(state.token).then(() => {
        commit('SET_TOKEN', '')
        commit('SET_USER', null)
        commit('SET_ROLES', [])
        removeToken()
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 移除token
  resetToken({ commit }) {
    return new Promise(resolve => {
      commit('SET_TOKEN', '')
      commit('SET_USER', null)
      commit('SET_ROLES', [])
      removeToken()
      resolve()
    })
  }
}

const getters = {
  token: state => state.token,
  user: state => state.user,
  roles: state => state.roles,
  username: state => state.user ? state.user.username : '',
  realName: state => state.user ? state.user.realName : '',
  organization: state => state.user ? state.user.organization : '',
  regionCode: state => state.user ? state.user.regionCode : ''
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
