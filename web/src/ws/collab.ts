import { useAuthStore } from '@/stores/auth'

export interface WsMessage {
  type: string
  docId?: number
  senderId?: number
  senderName?: string
  payload?: Record<string, any>
  timestamp?: number
}

const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const wsBase = (import.meta.env.VITE_WS_BASE || apiBase).replace(/^http/, 'ws')

export const useCollabSocket = () => {
  const auth = useAuthStore()
  let socket: WebSocket | null = null

  const connect = (docId: number, onMessage: (msg: WsMessage) => void) => {
    if (!auth.token) return
    const wsUrl = `${wsBase}/ws?token=${auth.token}`
    socket = new WebSocket(wsUrl)
    socket.onopen = () => {
      send({ type: 'join', docId })
    }
    socket.onmessage = (event) => {
      onMessage(JSON.parse(event.data))
    }
    socket.onclose = () => {
      setTimeout(() => connect(docId, onMessage), 3000)
    }
  }

  const send = (msg: WsMessage) => {
    if (!socket || socket.readyState !== WebSocket.OPEN) return
    socket.send(JSON.stringify(msg))
  }

  const close = () => {
    socket?.close()
    socket = null
  }

  return { connect, send, close }
}
