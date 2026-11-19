import request from '@/utils/request'

export function getInfo () {
  return request({ url: '/api/user/info', method: 'get' })
}

export function updateProfile (data) {
  return request({ url: '/api/user/profile', method: 'put', data })
}

export function changePassword (data) {
  return request({ url: '/api/user/password', method: 'put', data })
}

export function uploadAvatar (file) {
  const form = new FormData()
  form.append('file', file)
  return request({
    url: '/api/user/avatar',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function pageUsers (params) {
  return request({ url: '/api/user/page', method: 'get', params })
}

export function updateUserStatus (id, status) {
  return request({ url: `/api/user/${id}/status`, method: 'put', params: { status } })
}

export function updateUserRole (id, role) {
  return request({ url: `/api/user/${id}/role`, method: 'put', params: { role } })
}

export function resetUserPassword (id) {
  return request({ url: `/api/user/${id}/reset-password`, method: 'put' })
}
