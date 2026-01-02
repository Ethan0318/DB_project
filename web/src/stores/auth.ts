import { defineStore } from 'pinia'
import http from '@/api/http'

interface UserProfile {
  id: number
  email: string
  nickname?: string
  avatarUrl?: string
  phone?: string
  bio?: string
  roles?: string[]
}

const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const normalizeUser = (user: any): UserProfile | null => {
  if (!user) return null
  if (user.avatarUrl && user.avatarUrl.startsWith('/uploads')) {
    user.avatarUrl = `${apiBase}${user.avatarUrl}`
  }
  return user
}

const readToken = () => sessionStorage.getItem('token') || localStorage.getItem('token') || ''
const readUser = () => sessionStorage.getItem('user') || localStorage.getItem('user') || ''
const writeToken = (token: string) => {
  sessionStorage.setItem('token', token)
}
const writeUser = (user: UserProfile) => {
  sessionStorage.setItem('user', JSON.stringify(user))
}
const clearStored = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '' as string,
    user: null as UserProfile | null,
    isReady: false
  }),
  getters: {
    isAdmin: (state) => !!state.user?.roles?.includes('ADMIN')
  },
  actions: {
    restore() {
      const token = readToken()
      const user = readUser()
      this.token = token || ''
      this.user = user ? normalizeUser(JSON.parse(user)) : null
      this.isReady = true
    },
    async login(account: string, password: string) {
      const { data } = await http.post('/api/auth/login', { account, password })
      if (data.code !== 0) {
        throw new Error(data.message || '登录失败')
      }
      this.token = data.data?.token || ''
      this.user = normalizeUser(data.data?.user)
      if (this.token) {
        writeToken(this.token)
      }
      if (this.user) {
        writeUser(this.user)
      }
      if (!this.token) {
        throw new Error('登录失败，未获取到令牌')
      }
    },
    async register(email: string, password: string, nickname: string, phone?: string) {
      const { data } = await http.post('/api/auth/register', { email, phone, password, nickname })
      if (data.code !== 0) {
        throw new Error(data.message || '注册失败')
      }
      this.token = data.data?.token || ''
      this.user = normalizeUser(data.data?.user)
      if (this.token) {
        writeToken(this.token)
      }
      if (this.user) {
        writeUser(this.user)
      }
    },
    async fetchMe() {
      if (!this.token) {
        return null
      }
      try {
        const { data } = await http.get('/api/users/me')
        this.user = normalizeUser(data.data)
        if (this.user) {
          writeUser(this.user)
        }
        return this.user
      } catch (err) {
        this.logout()
        throw err
      }
    },
    logout() {
      this.token = ''
      this.user = null
      clearStored()
    }
  }
})
