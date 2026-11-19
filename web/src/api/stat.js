import request from '@/utils/request'

export function platformOverview () {
  return request({ url: '/api/stat/overview', method: 'get' })
}

export function organizerOverview () {
  return request({ url: '/api/stat/organizer', method: 'get' })
}

export function categoryStat () {
  return request({ url: '/api/stat/category', method: 'get' })
}

export function trendStat (days) {
  return request({ url: '/api/stat/trend', method: 'get', params: { days } })
}

export function topActivities (limit) {
  return request({ url: '/api/stat/top', method: 'get', params: { limit } })
}
