import { defineStore } from 'pinia'

const LOCAL_STORAGE_KEY = 'cropyield-auth'
const SESSION_STORAGE_KEY = 'cropyield-auth-session'
const SESSION_ID_KEY = 'cropyield-session-id'

// 生成唯一的会话ID
const generateSessionId = () => {
  return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

const readStorage = key => {
  try {
    const raw = window.localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch (error) {
    console.warn('Failed to read local storage', error)
    return null
  }
}

const writeStorage = (key, value) => {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
    console.warn('Failed to write local storage', error)
  }
}

const removeStorage = key => {
  try {
    window.localStorage.removeItem(key)
  } catch (error) {
    console.warn('Failed to remove local storage', error)
  }
}

const readSessionStorage = key => {
  try {
    const raw = window.sessionStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch (error) {
    console.warn('Failed to read session storage', error)
    return null
  }
}

const writeSessionStorage = (key, value) => {
  try {
    window.sessionStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
    console.warn('Failed to write session storage', error)
  }
}

const removeSessionStorage = key => {
  try {
    window.sessionStorage.removeItem(key)
  } catch (error) {
    console.warn('Failed to remove session storage', error)
  }
}

const deserializeAuthState = () => {
  const sessionState = readSessionStorage(SESSION_STORAGE_KEY)
  if (sessionState) {
    return sessionState
  }
  return readStorage(LOCAL_STORAGE_KEY)
}

let apiClientInstance
const getApiClient = async () => {
  if (!apiClientInstance) {
    const module = await import('../services/http')
    apiClientInstance = module.default
  }
  return apiClientInstance
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    token: null,
    tokenType: 'Bearer',
    refreshToken: null,
    expiresAt: 0,
    rememberMe: false,
    refreshTimer: null,
    user: null,
    isLoggingOut: false,
    sessionId: null,
    storageListener: null
  }),
  getters: {
    isAuthenticated: state => Boolean(state.token) && Date.now() < state.expiresAt,
    authorizationHeader: state => (state.token ? `${state.tokenType} ${state.token}` : null)
  },
  actions: {
    async initialize() {
      if (this.initialized) {
        return
      }
      
      // 生成当前标签页的会话ID
      this.sessionId = generateSessionId()
      
      // 设置storage事件监听，检测其他标签页的登录
      this.setupStorageListener()
      
      const stored = deserializeAuthState()
      if (stored && stored.token && stored.expiresAt && stored.expiresAt > Date.now()) {
        this.applyStoredState(stored)
        try {
          await this.fetchCurrentUser()
        } catch (error) {
          console.warn('Failed to fetch user info during init', error)
          this.clearSession()
        }
      } else {
        this.clearSession()
      }
      this.initialized = true
      this.scheduleRefresh()
    },
    
    setupStorageListener() {
      // 监听localStorage变化，检测其他标签页的登录
      this.storageListener = (event) => {
        // 只处理会话ID的变化
        if (event.key === SESSION_ID_KEY && event.newValue) {
          const newSessionId = event.newValue
          // 如果检测到新的会话ID，且不是当前会话，则强制登出
          if (newSessionId !== this.sessionId && this.isAuthenticated) {
            console.warn('检测到其他标签页登录，当前会话将被登出')
            this.forceLogout('其他标签页有新用户登录，当前会话已失效')
          }
        }
      }
      window.addEventListener('storage', this.storageListener)
    },
    
    removeStorageListener() {
      if (this.storageListener) {
        window.removeEventListener('storage', this.storageListener)
        this.storageListener = null
      }
    },
    
    applyStoredState(stored) {
      this.token = stored.token
      this.tokenType = stored.tokenType || 'Bearer'
      this.refreshToken = stored.refreshToken
      this.expiresAt = stored.expiresAt
      this.rememberMe = Boolean(stored.rememberMe)
      this.user = stored.user || null
    },
    
    persistState() {
      const payload = {
        token: this.token,
        tokenType: this.tokenType,
        refreshToken: this.refreshToken,
        expiresAt: this.expiresAt,
        rememberMe: this.rememberMe,
        user: this.user
      }
      if (this.rememberMe) {
        writeStorage(LOCAL_STORAGE_KEY, payload)
        removeSessionStorage(SESSION_STORAGE_KEY)
      } else {
        writeSessionStorage(SESSION_STORAGE_KEY, payload)
        removeStorage(LOCAL_STORAGE_KEY)
      }
      
      // 保存当前会话ID到localStorage，触发其他标签页的storage事件
      if (this.sessionId) {
        window.localStorage.setItem(SESSION_ID_KEY, this.sessionId)
      }
    },
    
    clearSession() {
      this.token = null
      this.tokenType = 'Bearer'
      this.refreshToken = null
      this.expiresAt = 0
      this.rememberMe = false
      this.user = null
      this.isLoggingOut = false
      if (this.refreshTimer) {
        window.clearTimeout(this.refreshTimer)
        this.refreshTimer = null
      }
      removeStorage(LOCAL_STORAGE_KEY)
      removeSessionStorage(SESSION_STORAGE_KEY)
      window.localStorage.removeItem(SESSION_ID_KEY)
    },
    
    beginLogout() {
      this.isLoggingOut = true
    },
    
    forceLogout(message) {
      this.clearSession()
      this.initialized = true
      
      // 跳转到登录页并显示提示信息
      const router = window.$router
      if (router) {
        router.push({
          path: '/login',
          query: { message: message || '会话已失效，请重新登录' }
        })
      } else {
        window.location.href = '/login'
      }
    },
    
    hasAnyRole(roles) {
      if (!Array.isArray(roles) || roles.length === 0) {
        return true
      }
      if (!this.user || !Array.isArray(this.user.roles)) {
        return false
      }
      return roles.some(role => this.user.roles.includes(role))
    },
    
    async register(payload) {
      const { rememberMe, ...requestBody } = payload
      const client = await getApiClient()
      const { data } = await client.post('/api/auth/register', requestBody)
      const response = data?.data
      if (!response) {
        throw new Error('注册响应数据不完整')
      }
      this.applyLoginResponse(response, rememberMe)
      return response
    },
    
    async login(payload) {
      const { rememberMe, loginMode, ...requestBody } = payload
      const client = await getApiClient()
      let endpoint = '/api/auth/login'
      if (loginMode === 'admin') {
        endpoint = '/api/auth/login/admin'
      } else if (loginMode === 'user') {
        endpoint = '/api/auth/login/user'
      }
      const { data } = await client.post(endpoint, requestBody)
      const response = data?.data
      if (!response) {
        throw new Error('登录响应数据不完整')
      }
      this.applyLoginResponse(response, rememberMe)
      return response
    },
    
    applyLoginResponse(response, rememberMe = false) {
      const tokens = response.tokens
      const user = response.user
      if (!tokens || !tokens.accessToken) {
        throw new Error('未获取到有效的访问令牌')
      }
      this.token = tokens.accessToken
      this.tokenType = tokens.tokenType || 'Bearer'
      this.refreshToken = tokens.refreshToken
      this.expiresAt = Date.now() + (tokens.expiresIn || 0)
      this.rememberMe = Boolean(rememberMe)
      this.user = user || null
      this.persistState()
      this.scheduleRefresh()
    },
    
    async fetchCurrentUser() {
      const client = await getApiClient()
      const { data} = await client.get('/api/auth/me')
      this.user = data?.data || null
      this.persistState()
      return this.user
    },
    
    async refreshAccessToken() {
      if (!this.refreshToken) {
        throw new Error('缺少刷新令牌')
      }
      const client = await getApiClient()
      const { data } = await client.post('/api/auth/refresh', { refreshToken: this.refreshToken })
      const tokens = data?.data
      if (!tokens || !tokens.accessToken) {
        throw new Error('刷新令牌失败')
      }
      this.token = tokens.accessToken
      this.tokenType = tokens.tokenType || 'Bearer'
      this.refreshToken = tokens.refreshToken
      this.expiresAt = Date.now() + (tokens.expiresIn || 0)
      this.persistState()
      this.scheduleRefresh()
      return this.token
    },
    
    scheduleRefresh() {
      if (this.refreshTimer) {
        window.clearTimeout(this.refreshTimer)
        this.refreshTimer = null
      }
      if (!this.token || !this.refreshToken || !this.expiresAt) {
        return
      }
      const now = Date.now()
      const refreshLead = 60 * 1000
      const timeout = Math.max(this.expiresAt - now - refreshLead, 10 * 1000)
      this.refreshTimer = window.setTimeout(async () => {
        try {
          await this.refreshAccessToken()
        } catch (error) {
          console.warn('自动刷新令牌失败', error)
          this.logout()
        }
      }, timeout)
    },
    
    logout() {
      this.removeStorageListener()
      this.clearSession()
      this.initialized = true
    }
  }
})
