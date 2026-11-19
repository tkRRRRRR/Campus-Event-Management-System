import request from '@/utils/request'

export function listConfigs () {
  return request({ url: '/api/config/list', method: 'get' })
}

export function updateConfig (id, configValue) {
  return request({ url: `/api/config/${id}`, method: 'put', data: { configValue } })
}
