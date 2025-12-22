import { defineStore } from 'pinia'
import http from '@/api/http'

interface UserProfile {
  id: number
  email: string
  nickname?: string
  avatarUrl?: string
  phone?: string
  bio?: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '' as string,
    user: null as UserProfile | null,
    isReady: false
  }),
  actions: {
    restore() {
      const token = localStorage.getItem('token')
      const user = localStorage.getItem('user')
      this.token = token || ''
      this.user = user ? JSON.parse(user) : null
      this.isReady = true
    },
    async login(email: string, password: string) {
      const { data } = await http.post('/api/auth/login', { email, password })
      this.token = data.data.token
      this.user = data.data.user
      localStorage.setItem('token', this.token)
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    async register(email: string, password: string, nickname: string) {
      const { data } = await http.post('/api/auth/register', { email, password, nickname })
      this.token = data.data.token
      this.user = data.data.user
      localStorage.setItem('token', this.token)
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    async fetchMe() {
      const { data } = await http.get('/api/users/me')
      this.user = data.data
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
