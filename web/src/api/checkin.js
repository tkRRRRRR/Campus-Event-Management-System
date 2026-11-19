import request from '@/utils/request'

export function generateCode (activityId) {
  return request({ url: `/api/checkin/${activityId}/code`, method: 'post' })
}

export function currentCode (activityId) {
  return request({ url: `/api/checkin/${activityId}/code`, method: 'get' })
}

export function checkinByCode (activityId, code) {
  return request({ url: `/api/checkin/${activityId}`, method: 'post', data: { code } })
}

export function manualCheckin (registrationId) {
  return request({ url: `/api/checkin/manual/${registrationId}`, method: 'post' })
}

export function checkinList (activityId, params) {
  return request({ url: `/api/checkin/${activityId}/list`, method: 'get', params })
}

export function checkinStat (activityId) {
  return request({ url: `/api/checkin/${activityId}/stat`, method: 'get' })
}

export function finishActivity (activityId) {
  return request({ url: `/api/checkin/${activityId}/finish`, method: 'post' })
}
