import { useNotificationStore } from '@/stores/notification'
import { useAuthStore } from '@/stores/auth'

let socket: WebSocket | null = null

export const useNotifySocket = () => {
  const notificationStore = useNotificationStore()
  const auth = useAuthStore()

  const connect = () => {
    if (!auth.token) return
    if (socket && socket.readyState === WebSocket.OPEN) return
    const wsUrl = (import.meta.env.VITE_WS_BASE || 'ws://localhost:8080') + `/ws?token=${auth.token}`
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
