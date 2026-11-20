import request from '@/utils/request'

export function registerActivity (activityId) {
  return request({ url: `/api/registration/${activityId}`, method: 'post' })
}

export function cancelRegistration (activityId) {
  return request({ url: `/api/registration/${activityId}`, method: 'delete' })
}

export function myRegistrations (params) {
  return request({ url: '/api/registration/my', method: 'get', params })
}

export function memberPage (activityId, params) {
  return request({ url: `/api/registration/activity/${activityId}`, method: 'get', params })
}

export function auditRegistration (registrationId, data) {
  return request({ url: `/api/registration/${registrationId}/audit`, method: 'put', data })
}

/** 导出报名名单（xlsx 二进制流） */
export function exportMembers (activityId, params) {
  return request({
    url: `/api/registration/activity/${activityId}/export`,
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 30000
  })
}
