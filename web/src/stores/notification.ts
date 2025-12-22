import { defineStore } from 'pinia'
import http from '@/api/http'

export interface NotificationItem {
  id: number
  type: string
  payload: string
  readFlag: number
  createdAt: string
}

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    items: [] as NotificationItem[]
  }),
  getters: {
    unreadCount: (state) => state.items.filter((n) => n.readFlag === 0).length
  },
  actions: {
    async refresh() {
      const { data } = await http.get('/api/notifications')
      this.items = data.data || []
    },
    push(item: NotificationItem) {
      this.items.unshift(item)
    },
    async markRead(id: number) {
      await http.put(`/api/notifications/${id}/read`)
      const target = this.items.find((n) => n.id === id)
      if (target) {
        target.readFlag = 1
      }
    },
    async markAllRead() {
      await http.put('/api/notifications/read-all')
      this.items.forEach((n) => (n.readFlag = 1))
    }
  }
})
