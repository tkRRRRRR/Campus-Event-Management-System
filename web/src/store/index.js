import Vue from 'vue'
import Vuex from 'vuex'
import { login } from '@/api/auth'
import { getInfo } from '@/api/user'

Vue.use(Vuex)

const TOKEN_KEY = 'campus_token'
const USER_KEY = 'campus_user'

function readUser () {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch (e) {
    return null
  }
}

export default new Vuex.Store({
  state: {
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser(),
    unreadCount: 0
  },
  getters: {
    token: state => state.token,
    user: state => state.user,
    userId: state => (state.user ? state.user.id : null),
    role: state => (state.user ? state.user.role : ''),
    isLogin: state => !!state.token,
    isAdmin: state => !!state.user && state.user.role === 'ADMIN',
    isOrganizer: state => !!state.user && (state.user.role === 'ORGANIZER' || state.user.role === 'ADMIN'),
    unreadCount: state => state.unreadCount
  },
  mutations: {
    SET_TOKEN (state, token) {
      state.token = token
      localStorage.setItem(TOKEN_KEY, token)
    },
    SET_USER (state, user) {
      state.user = user
      localStorage.setItem(USER_KEY, JSON.stringify(user || null))
    },
    SET_UNREAD (state, count) {
      state.unreadCount = count
    },
    CLEAR_LOGIN (state) {
      state.token = ''
      state.user = null
      state.unreadCount = 0
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  },
  actions: {
    async login ({ commit }, form) {
      const data = await login(form)
      commit('SET_TOKEN', data.token)
      const info = await getInfo()
      commit('SET_USER', info)
      return data
    },
    async loadUser ({ commit, state }) {
      if (!state.token) return null
      const info = await getInfo()
      commit('SET_USER', info)
      return info
    },
    logout ({ commit }) {
      commit('CLEAR_LOGIN')
    }
  }
})
