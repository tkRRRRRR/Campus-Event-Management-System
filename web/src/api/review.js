import request from '@/utils/request'

export function submitReview (activityId, data) {
  return request({ url: `/api/review/${activityId}`, method: 'post', data })
}

export function reviewPage (activityId, params) {
  return request({ url: `/api/review/activity/${activityId}`, method: 'get', params })
}

export function reviewStat (activityId) {
  return request({ url: `/api/review/activity/${activityId}/stat`, method: 'get' })
}

export function myReviews (params) {
  return request({ url: '/api/review/my', method: 'get', params })
}

export function deleteReview (reviewId) {
  return request({ url: `/api/review/${reviewId}`, method: 'delete' })
}
