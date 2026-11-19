import request from '@/utils/request'

export function myNotices (params) {
  return request({ url: '/api/notice/my', method: 'get', params })
}

export function unreadCount () {
  return request({ url: '/api/notice/unread-count', method: 'get' })
}

export function readNotice (id) {
  return request({ url: `/api/notice/${id}/read`, method: 'put' })
}

export function readAllNotices () {
  return request({ url: '/api/notice/read-all', method: 'put' })
}

export function deleteNotice (id) {
  return request({ url: `/api/notice/${id}`, method: 'delete' })
}
