import { useAuthStore } from '@/stores/auth'

export interface WsMessage {
  type: string
  docId?: number
  senderId?: number
  senderName?: string
  payload?: Record<string, any>
  timestamp?: number
}

export const useCollabSocket = () => {
  const auth = useAuthStore()
  let socket: WebSocket | null = null

  const connect = (docId: number, onMessage: (msg: WsMessage) => void) => {
    if (!auth.token) return
    const wsUrl = (import.meta.env.VITE_WS_BASE || 'ws://localhost:8080') + `/ws?token=${auth.token}`
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
