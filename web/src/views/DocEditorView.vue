<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <el-button text @click="goBack">Back</el-button>
        <el-button text @click="goNotifications">Notifications</el-button>
      </div>
      <div style="margin-top: 12px;">
        <div class="text-muted">Online</div>
        <PresenceList :members="presenceMembers" />
      </div>
      <div style="margin-top: 16px;">
        <el-button class="gradient-button" type="primary" @click="commentDrawer = true">Comments</el-button>
        <el-button @click="chatDrawer = true">Chat</el-button>
        <el-button @click="openShare">Share</el-button>
        <el-button @click="openHistory">History</el-button>
      </div>
      <div style="margin-top: auto;">
        <el-button text @click="logout">Logout</el-button>
      </div>
    </aside>

    <main class="content">
      <div class="editor-shell">
        <input v-model="title" class="editor-title" @change="saveTitle" />
        <div style="display: flex; justify-content: space-between; align-items: center; margin: 8px 0 16px;">
          <div class="toolbar">
            <el-button size="small" @click="cmd('toggleBold')">Bold</el-button>
            <el-button size="small" @click="cmd('toggleItalic')">Italic</el-button>
            <el-button size="small" @click="cmd('toggleUnderline')">Underline</el-button>
            <el-button size="small" @click="cmd('toggleHeading', { level: 2 })">H2</el-button>
            <el-button size="small" @click="cmd('toggleBulletList')">List</el-button>
            <el-button size="small" @click="cmd('toggleBlockquote')">Quote</el-button>
            <el-button size="small" @click="cmd('toggleCodeBlock')">Code</el-button>
            <el-button size="small" @click="setLink">Link</el-button>
            <el-button size="small" @click="createAnchor">Comment</el-button>
          </div>
          <div class="save-state">{{ saveStatus }}</div>
        </div>
        <EditorContent v-if="editor" :editor="editor" />
      </div>
    </main>
  </div>

  <el-drawer v-model="commentDrawer" title="Comments" size="30%">
    <CommentPanel
      :comments="comments"
      :anchor-preview="anchorPreview"
      @submit="submitComment"
    />
  </el-drawer>

  <ChatDrawer v-model="chatDrawer" :messages="chatMessages" @send="sendChat" />

  <el-dialog v-model="shareDialog" title="Share Document" width="420px">
    <el-form label-position="top">
      <el-form-item label="User">
        <el-select v-model="shareUserId" filterable remote placeholder="Search user" :remote-method="searchUser">
          <el-option v-for="user in shareOptions" :key="user.id" :label="user.label" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="Permission">
        <el-select v-model="sharePerm" placeholder="Permission">
          <el-option label="VIEW" value="VIEW" />
          <el-option label="EDIT" value="EDIT" />
          <el-option label="ADMIN" value="ADMIN" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="shareDialog = false">Cancel</el-button>
      <el-button class="gradient-button" type="primary" @click="shareDoc">Share</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="historyDialog" title="Snapshots" width="520px">
    <el-table :data="snapshots">
      <el-table-column prop="createdAt" label="Created">
        <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="Actions" width="120">
        <template #default="scope">
          <el-button size="small" @click="restoreSnapshot(scope.row.id)">Restore</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog v-model="draftDialog" title="Restore Draft" width="420px">
    <div class="text-muted">A local draft is newer than the server copy. Restore it?</div>
    <template #footer>
      <el-button @click="discardDraft">Discard</el-button>
      <el-button class="gradient-button" type="primary" @click="restoreDraft">Restore</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useCollabSocket } from '@/ws/collab'
import { RemoteCursorExtension, updateRemoteCursors, RemoteCursor } from '@/editor/remoteCursor'
import PresenceList from '@/components/PresenceList.vue'
import CommentPanel from '@/components/CommentPanel.vue'
import ChatDrawer from '@/components/ChatDrawer.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const docId = Number(route.params.id)

const title = ref('')
const saveStatus = ref('Saved')
const comments = ref<any[]>([])
const chatMessages = ref<any[]>([])
const snapshots = ref<any[]>([])
const presenceMembers = ref<any[]>([])

const commentDrawer = ref(false)
const chatDrawer = ref(false)
const shareDialog = ref(false)
const historyDialog = ref(false)
const draftDialog = ref(false)

const shareUserId = ref<number | null>(null)
const sharePerm = ref('VIEW')
const shareOptions = ref<any[]>([])

const anchorPreview = ref('')
let anchorPayload: string | null = null

const remoteCursorMap = new Map<number, RemoteCursor>()
const myColor = computed(() => pickColor(auth.user?.id || 0))

const { connect, send, close } = useCollabSocket()

let saveTimer: any = null
let broadcastTimer: any = null
let cursorTimer: any = null
let isApplyingRemote = false
let lastRemoteUpdate = 0
const pendingContent = ref<string | null>(null)

const editor = useEditor({
  content: '',
  extensions: [
    StarterKit,
    Underline,
    Link.configure({ openOnClick: false }),
    Placeholder.configure({ placeholder: 'Start typing...' }),
    RemoteCursorExtension
  ],
  onUpdate: ({ editor }) => {
    if (isApplyingRemote) return
    const html = editor.getHTML()
    const now = Date.now()
    localStorage.setItem(`draft_doc_${docId}`, JSON.stringify({ content: html, updatedAt: now }))
    scheduleSave(html)
    scheduleBroadcast(html)
  },
  onSelectionUpdate: ({ editor }) => {
    const { from, to } = editor.state.selection
    scheduleCursor(from, to)
  }
})

const loadDoc = async () => {
  const { data } = await http.get(`/api/docs/${docId}`)
  title.value = data.data.doc.title
  const serverContent = data.data.content || ''
  const serverUpdated = data.data.updatedAt ? new Date(data.data.updatedAt).getTime() : 0
  const draftRaw = localStorage.getItem(`draft_doc_${docId}`)
  if (draftRaw) {
    const draft = JSON.parse(draftRaw)
    if (draft.updatedAt > serverUpdated) {
      draftDialog.value = true
    }
  }
  if (editor.value) {
    editor.value.commands.setContent(serverContent)
  } else {
    pendingContent.value = serverContent
  }
}

const loadComments = async () => {
  const { data } = await http.get(`/api/docs/${docId}/comments`)
  comments.value = data.data || []
}

const loadChat = async () => {
  const { data } = await http.get(`/api/docs/${docId}/chat`)
  chatMessages.value = data.data || []
}

const connectSocket = () => {
  connect(docId, (msg) => {
    if (msg.type === 'presence') {
      presenceMembers.value = msg.payload?.members || []
      const memberIds = new Set(presenceMembers.value.map((m: any) => m.userId))
      remoteCursorMap.forEach((_value, key) => {
        if (!memberIds.has(key)) {
          remoteCursorMap.delete(key)
        }
      })
      if (editor.value) {
        updateRemoteCursors(editor.value, Array.from(remoteCursorMap.values()))
      }
    }
    if (msg.type === 'content_update') {
      if (msg.senderId === auth.user?.id) return
      const updatedAt = msg.payload?.updatedAt || 0
      if (updatedAt <= lastRemoteUpdate) return
      lastRemoteUpdate = updatedAt
      isApplyingRemote = true
      editor.value?.commands.setContent(msg.payload?.content || '')
      isApplyingRemote = false
    }
    if (msg.type === 'cursor_update') {
      if (!msg.payload) return
      if (msg.senderId === auth.user?.id) return
      const cursor: RemoteCursor = {
        userId: msg.senderId || 0,
        name: msg.payload.name || `User${msg.senderId}`,
        color: msg.payload.color || '#2a7cf7',
        from: msg.payload.from || 0,
        to: msg.payload.to || 0
      }
      remoteCursorMap.set(cursor.userId, cursor)
      if (editor.value) {
        updateRemoteCursors(editor.value, Array.from(remoteCursorMap.values()))
      }
    }
    if (msg.type === 'chat_message') {
      chatMessages.value.push({
        id: msg.payload?.id,
        senderId: msg.senderId,
        senderName: msg.senderName,
        content: msg.payload?.content,
        createdAt: msg.payload?.createdAt
      })
    }
  })
}

const scheduleSave = (html: string) => {
  saveStatus.value = 'Saving...'
  clearTimeout(saveTimer)
  saveTimer = setTimeout(async () => {
    await http.put(`/api/docs/${docId}/content`, { content: html, clientUpdatedAt: Date.now() })
    saveStatus.value = 'Saved'
  }, 2000)
}

const scheduleBroadcast = (html: string) => {
  clearTimeout(broadcastTimer)
  broadcastTimer = setTimeout(() => {
    send({
      type: 'content_update',
      docId,
      senderId: auth.user?.id,
      senderName: auth.user?.nickname,
      payload: { content: html, updatedAt: Date.now() }
    })
  }, 800)
}

const scheduleCursor = (from: number, to: number) => {
  clearTimeout(cursorTimer)
  cursorTimer = setTimeout(() => {
    send({
      type: 'cursor_update',
      docId,
      senderId: auth.user?.id,
      senderName: auth.user?.nickname,
      payload: {
        from,
        to,
        name: auth.user?.nickname || auth.user?.email,
        color: myColor.value
      }
    })
  }, 200)
}

const cmd = (command: string, attrs?: any) => {
  if (!editor.value) return
  ;(editor.value as any).chain().focus()[command](attrs).run()
}

const setLink = () => {
  const url = window.prompt('Enter URL')
  if (!url || !editor.value) return
  editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

const createAnchor = () => {
  if (!editor.value) return
  const { from, to } = editor.value.state.selection
  const text = editor.value.state.doc.textBetween(from, to, ' ')
  if (!text) return
  anchorPreview.value = text
  anchorPayload = JSON.stringify({ from, to, text })
  commentDrawer.value = true
}

const submitComment = async (payload: { content: string; parentId?: number; atUserId?: number }) => {
  await http.post(`/api/docs/${docId}/comments`, {
    content: payload.content,
    anchor: anchorPayload,
    parentId: payload.parentId,
    atUserId: payload.atUserId
  })
  anchorPreview.value = ''
  anchorPayload = null
  await loadComments()
}

const sendChat = (content: string) => {
  send({
    type: 'chat_message',
    docId,
    senderId: auth.user?.id,
    senderName: auth.user?.nickname,
    payload: { content }
  })
}

const saveTitle = async () => {
  await http.put(`/api/docs/${docId}`, { title: title.value })
}

const openShare = async () => {
  shareDialog.value = true
  await searchUser('')
}

const searchUser = async (keyword: string) => {
  const { data } = await http.get('/api/users/search', { params: { keyword } })
  shareOptions.value = (data.data || []).map((user: any) => ({
    id: user.id,
    label: user.nickname ? `${user.nickname} (${user.email})` : user.email
  }))
}

const shareDoc = async () => {
  if (!shareUserId.value) return
  await http.post(`/api/docs/${docId}/acl`, { userId: shareUserId.value, perm: sharePerm.value })
  shareDialog.value = false
}

const openHistory = async () => {
  const { data } = await http.get(`/api/docs/${docId}/snapshots`)
  snapshots.value = data.data || []
  historyDialog.value = true
}

const restoreSnapshot = async (snapshotId: number) => {
  await http.post(`/api/docs/${docId}/snapshots/${snapshotId}/restore`)
  historyDialog.value = false
  await loadDoc()
}

const restoreDraft = () => {
  const draftRaw = localStorage.getItem(`draft_doc_${docId}`)
  if (!draftRaw || !editor.value) return
  const draft = JSON.parse(draftRaw)
  editor.value.commands.setContent(draft.content)
  draftDialog.value = false
}

const discardDraft = () => {
  localStorage.removeItem(`draft_doc_${docId}`)
  draftDialog.value = false
}

const goBack = () => {
  router.push('/docs')
}

const goNotifications = () => {
  router.push('/notifications')
}

const logout = () => {
  auth.logout()
  router.push('/login')
}

const formatTime = (raw: string) => (raw ? new Date(raw).toLocaleString() : '')

function pickColor(seed: number) {
  const palette = ['#2a7cf7', '#ff6b6b', '#6bcf63', '#f7b32b', '#8b5cf6']
  return palette[Math.abs(seed) % palette.length]
}

onMounted(async () => {
  await auth.fetchMe()
  await loadDoc()
  await loadComments()
  await loadChat()
  connectSocket()
})

watch(editor, (instance) => {
  if (instance && pendingContent.value !== null) {
    instance.commands.setContent(pendingContent.value)
    pendingContent.value = null
  }
})

onBeforeUnmount(() => {
  close()
})
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
