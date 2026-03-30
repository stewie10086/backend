import { defineStore } from 'pinia'
import { api } from '@/api/client'

const STORAGE_TOKEN_KEY = 'booking.auth.token'
const STORAGE_USER_KEY = 'booking.auth.user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    token: localStorage.getItem(STORAGE_TOKEN_KEY) || '',
    user: null
  }),
  getters: {
    isAuthenticated: (s) => Boolean(s.token)
  },
  actions: {
    loadUserFromStorage() {
      const raw = localStorage.getItem(STORAGE_USER_KEY)
      if (!raw) return null
      try {
        return JSON.parse(raw)
      } catch {
        return null
      }
    },
    setUser(user) {
      this.user = user ?? null
      if (this.user) localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(this.user))
      else localStorage.removeItem(STORAGE_USER_KEY)
    },
    setToken(token) {
      this.token = token || ''
      if (this.token) localStorage.setItem(STORAGE_TOKEN_KEY, this.token)
      else localStorage.removeItem(STORAGE_TOKEN_KEY)
    },
    async bootstrap() {
      this.initialized = true
      // 优先用本地 user 让路由权限判定可用（无后端也能跑）
      const localUser = this.loadUserFromStorage()
      if (localUser) this.user = localUser

      if (!this.token) {
        this.setUser(null)
        return
      }

      // 有后端时再用 /me 覆盖本地数据；无后端则保留本地 user
      try {
        const me = await api.getMe()
        this.setUser(me.user ?? null)
      } catch {
        if (!this.user) {
          this.setUser(null)
        }
      }
    },
    async login({ email, password }) {
      const res = await api.login({ email, password })
      this.setToken(res.token)
      this.setUser(res.user)
      return res
    },
    async loginByEmailCode({ email, verificationCode }) {
      const res = await api.loginByEmailCode({ email, verificationCode })
      this.setToken(res.token)
      this.setUser(res.user)
      return res
    },
    async register({ name, email, password, verificationCode }) {
      const res = await api.register({ name, email, password, verificationCode })
      this.setToken(res.token)
      this.setUser(res.user)
      return res
    },
    async logout() {
      try {
        await api.logout()
      } finally {
        this.setToken('')
        this.setUser(null)
        this.initialized = true
      }
    },
    defaultHomeRoute() {
      const role = this.user?.role
      if (role === 'Admin') return { name: 'admin.dashboard' }
      if (role === 'Specialist') return { name: 'specialist.dashboard' }
      if (role === 'Customer') return { name: 'customer.specialists' }
      return { name: 'login' }
    }
  }
})
