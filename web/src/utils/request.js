import axios from 'axios'
import { Message } from 'element-ui'
import store from '@/store'
import router from '@/router'

const service = axios.create({
  baseURL: process.env.VUE_APP_API_BASE || '',
  timeout: 15000
})

service.interceptors.request.use(
  config => {
    const token = store.getters.token
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      store.commit('CLEAR_LOGIN')
      router.replace('/login')
      Message.warning(res.msg || '登录已失效，请重新登录')
      return Promise.reject(new Error(res.msg))
    }
    Message.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  error => {
    const status = error.response && error.response.status
    if (status === 401) {
      store.commit('CLEAR_LOGIN')
      router.replace('/login')
      Message.warning('登录已失效，请重新登录')
    } else if (error.code === 'ECONNABORTED') {
      Message.error('请求超时，请检查网络后重试')
    } else {
      Message.error('网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default service

/** 拼接图片/头像地址：后端返回的是 /upload/xxx 相对路径 */
export function resolveUrl (url) {
  if (!url) return ''
  if (/^(http|https|data:)/.test(url)) return url
  return (process.env.VUE_APP_FILE_BASE || '') + url
}
