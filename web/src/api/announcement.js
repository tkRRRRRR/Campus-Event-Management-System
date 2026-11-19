import request from '@/utils/request'

export function publishedAnnouncements () {
  return request({ url: '/api/announcement/list', method: 'get' })
}

export function announcementPage (params) {
  return request({ url: '/api/announcement/page', method: 'get', params })
}

export function createAnnouncement (data) {
  return request({ url: '/api/announcement', method: 'post', data })
}

export function updateAnnouncement (id, data) {
  return request({ url: `/api/announcement/${id}`, method: 'put', data })
}

export function changeAnnouncementStatus (id, status) {
  return request({ url: `/api/announcement/${id}/status`, method: 'put', params: { status } })
}

export function deleteAnnouncement (id) {
  return request({ url: `/api/announcement/${id}`, method: 'delete' })
}
