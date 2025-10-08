import request from './request'

const AUTH_BASE_PATH = '/api/auth'

export function login(data) {
  return request({
    url: `${AUTH_BASE_PATH}/login`,
    method: 'post',
    data
  })
}

export function logout(token) {
  return request({
    url: `${AUTH_BASE_PATH}/logout`,
    method: 'post',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
}

export function refreshToken(token) {
  return request({
    url: `${AUTH_BASE_PATH}/refresh`,
    method: 'post',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
}

export function getUserInfo(token) {
  return request({
    url: '/api/users/profile',
    method: 'get',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
}
