import request from '@/utils/request'

export function listCategories () {
  return request({ url: '/api/category/list', method: 'get' })
}

/** 管理员维护用：含停用的分类 */
export function listAllCategories () {
  return request({ url: '/api/category/all', method: 'get' })
}

export function createCategory (data) {
  return request({ url: '/api/category', method: 'post', data })
}

export function updateCategory (id, data) {
  return request({ url: `/api/category/${id}`, method: 'put', data })
}

export function deleteCategory (id) {
  return request({ url: `/api/category/${id}`, method: 'delete' })
}
