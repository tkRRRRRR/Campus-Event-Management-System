import request from '@/utils/request'

export function pageActivities (params) {
  return request({ url: '/api/activity/page', method: 'get', params })
}

export function getActivity (id) {
  return request({ url: `/api/activity/${id}`, method: 'get' })
}

export function createActivity (data) {
  return request({ url: '/api/activity', method: 'post', data })
}

export function updateActivity (id, data) {
  return request({ url: `/api/activity/${id}`, method: 'put', data })
}

export function cancelActivity (id) {
  return request({ url: `/api/activity/${id}/cancel`, method: 'put' })
}

export function myActivities (params) {
  return request({ url: '/api/activity/my', method: 'get', params })
}

export function auditPage (params) {
  return request({ url: '/api/activity/audit/page', method: 'get', params })
}

export function auditActivity (id, data) {
  return request({ url: `/api/activity/${id}/audit`, method: 'put', data })
}

/** 上传活动封面，formData 里字段名为 file */
export function uploadActivityCover (formData) {
  return request({
    url: '/api/activity/cover',
    method: 'post',
    data: formData,
    timeout: 30000
  })
}
