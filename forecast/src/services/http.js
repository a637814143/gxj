import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const resolveBaseURL = () => {
  const configured = import.meta.env.VITE_API_BASE_URL
  if (typeof configured === 'string' && configured.trim()) {
    return configured.trim()
  }
  if (typeof window !== 'undefined') {
    const { protocol, hostname } = window.location
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return `${protocol}//${hostname}:8080`
    }
    return `${protocol}//${window.location.host}`
  }
  return ''
}

const apiClient = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 120000  // 增加到120秒（2分钟），确保LSTM有足够时间
})

let isRefreshing = false
const refreshQueue = []

let routerInstance
const redirectToLogin = () => {
  const pushToLogin = router => {
    const currentRoute = router.currentRoute?.value
    const redirect = currentRoute && currentRoute.name !== 'login' ? currentRoute.fullPath : undefined
    router.push({
      name: 'login',
      ...(redirect ? { query: { redirect } } : {})
    }).catch(() => {})
  }

  if (routerInstance) {
    pushToLogin(routerInstance)
    return
  }

  import('../router').then(module => {
    routerInstance = module.default
    pushToLogin(routerInstance)
  })
}

const processQueue = (error, token) => {
  while (refreshQueue.length) {
    const { resolve, reject } = refreshQueue.shift()
    if (error) {
      reject(error)
    } else {
      resolve(token)
    }
  }
}

apiClient.interceptors.request.use(
  config => {
    const authStore = useAuthStore()
    if (authStore?.authorizationHeader && !config.headers.Authorization) {
      config.headers.Authorization = authStore.authorizationHeader
    }
    return config
  },
  error => Promise.reject(error)
)

apiClient.interceptors.response.use(
  response => response,
  async error => {
    const { response, config } = error
    if (!response || !config) {
      return Promise.reject(error)
    }

    const status = response.status
    const authStore = useAuthStore()

    const isAuthEndpoint = config.url?.includes('/api/auth/login') || config.url?.includes('/api/auth/refresh')

    if (status === 401 && !config._retry && !isAuthEndpoint && authStore?.refreshToken) {
      config._retry = true

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          refreshQueue.push({
            resolve: token => {
              config.headers.Authorization = `${authStore.tokenType || 'Bearer'} ${token}`
              resolve(apiClient(config))
            },
            reject
          })
        })
      }

      isRefreshing = true

      try {
        const newToken = await authStore.refreshAccessToken()
        processQueue(null, newToken)
        config.headers.Authorization = `${authStore.tokenType || 'Bearer'} ${newToken}`
        return apiClient(config)
      } catch (refreshError) {
        processQueue(refreshError, null)
        authStore.logout()
        redirectToLogin()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    if (status === 401 && isAuthEndpoint) {
      authStore?.logout()
      redirectToLogin()
    }

    return Promise.reject(error)
  }
)

export default apiClient
