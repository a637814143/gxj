import request from './request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function logout(token) {
  return request({
    url: '/auth/logout',
    method: 'post',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
}

export function refreshToken(token) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
}

export function getUserInfo(token) {
  return request({
    url: '/users/profile',
    method: 'get',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
}
