import Vue from 'vue'

const query = '(max-width: 768px)'
const mql = window.matchMedia ? window.matchMedia(query) : null

// 用一个可响应的对象承载断点状态，组件通过 computed 读取即可自动更新
const state = Vue.observable({ mobile: mql ? mql.matches : window.innerWidth <= 768 })

if (mql) {
  const handler = (e) => { state.mobile = e.matches }
  if (mql.addEventListener) mql.addEventListener('change', handler)
  else if (mql.addListener) mql.addListener(handler)
}

export const device = state

export function isMobile() {
  return state.mobile
}

export default { device, isMobile }
