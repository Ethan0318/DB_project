import { useNotificationStore } from '@/stores/notification'
import { useAuthStore } from '@/stores/auth'

let socket: WebSocket | null = null
const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const wsBase = (import.meta.env.VITE_WS_BASE || apiBase).replace(/^http/, 'ws')

export const useNotifySocket = () => {
  const notificationStore = useNotificationStore()
  const auth = useAuthStore()

  const connect = () => {
    if (!auth.token) return
    if (socket && socket.readyState === WebSocket.OPEN) return
    const wsUrl = `${wsBase}/ws?token=${auth.token}`
    socket = new WebSocket(wsUrl)
    socket.onmessage = (event) => {
      const msg = JSON.parse(event.data)
      if (msg.type === 'notification') {
        notificationStore.push({
          id: msg.payload.id,
          type: msg.payload.type,
          payload: msg.payload.payload,
          readFlag: 0,
          createdAt: msg.payload.createdAt
        })
        if (msg.payload.type === 'DOC_SHARE') {
          window.dispatchEvent(new CustomEvent('doc-shared', { detail: msg.payload }))
        }
      }
    }
    socket.onclose = () => {
      setTimeout(() => connect(), 3000)
    }
  }

  const close = () => {
    socket?.close()
    socket = null
  }

  return { connect, close }
}
