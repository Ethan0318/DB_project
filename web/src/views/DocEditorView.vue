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
      <div style="margin-top: 16px; display: flex; flex-direction: column; gap: 8px;">
        <el-button class="gradient-button" type="primary" @click="commentDrawer = true">Comments</el-button>
        <el-button @click="chatDrawer = true">Chat</el-button>
        <el-button @click="openTasks">Tasks</el-button>
        <el-button :disabled="!canAdmin" @click="openShare">Share</el-button>
        <el-button @click="openHistory">History</el-button>
      </div>
      <div style="margin-top: auto;">
        <el-button text @click="logout">Logout</el-button>
      </div>
    </aside>

    <main class="content">
      <div class="editor-shell">
        <input v-model="title" class="editor-title" :disabled="!canEdit" @change="saveTitle" />
        <div class="presence-bar">
          <div v-for="m in presenceChips" :key="m.userId" class="presence-pill">
            <el-avatar :size="26" :src="m.avatarUrl">{{ m.nickname?.slice(0, 1) || 'U' }}</el-avatar>
            <span>{{ m.nickname || `User${m.userId}` }}</span>
          </div>
        </div>
        <div class="toolbar-row">
          <div class="toolbar">
            <el-button size="small" :disabled="!canEdit" @click="cmd('toggleBold')">Bold</el-button>
            <el-button size="small" :disabled="!canEdit" @click="cmd('toggleItalic')">Italic</el-button>
            <el-button size="small" :disabled="!canEdit" @click="cmd('toggleUnderline')">Underline</el-button>
            <el-button size="small" :disabled="!canEdit" @click="cmd('toggleHeading', { level: 2 })">H2</el-button>
            <el-button size="small" :disabled="!canEdit" @click="cmd('toggleBulletList')">List</el-button>
            <el-button size="small" :disabled="!canEdit" @click="cmd('toggleBlockquote')">Quote</el-button>
            <el-button size="small" :disabled="!canEdit" @click="cmd('toggleCodeBlock')">Code</el-button>
            <el-button size="small" :disabled="!canEdit" @click="setLink">Link</el-button>
            <el-popover v-model:visible="tablePopover" placement="bottom-start" width="220" trigger="click">
              <div class="table-grid">
                <div
                  v-for="r in 6"
                  :key="'r'+r"
                  class="table-row"
                >
                  <div
                    v-for="c in 6"
                    :key="'c'+c"
                    class="table-cell"
                    :class="{ active: r <= hoverRows && c <= hoverCols }"
                    @mouseenter="() => { hoverRows = r; hoverCols = c }"
                    @click="insertTable(r, c)"
                  ></div>
                </div>
                <div class="text-muted" style="margin-top: 6px;">{{ hoverRows }} x {{ hoverCols }} table</div>
              </div>
              <template #reference>
                <el-button size="small" :disabled="!canEdit">Insert Table</el-button>
              </template>
            </el-popover>
            <el-button size="small" :disabled="!canEdit" @click="addRow">Row +</el-button>
            <el-button size="small" :disabled="!canEdit" @click="addColumn">Col +</el-button>
            <el-button size="small" type="danger" :disabled="!canEdit" @click="deleteTable">Del Table</el-button>
            <el-button size="small" type="primary" @click="startMeeting">Start Meeting</el-button>
            <el-button size="small" @click="createAnchor">Comment</el-button>
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-button size="small" @click="exportDoc('html')">Export HTML</el-button>
            <el-button size="small" @click="exportDoc('markdown')">Export MD</el-button>
            <div class="save-state">{{ saveStatus }}</div>
          </div>
        </div>
        <EditorContent v-if="editor" :editor="editor" class="prose-editor" />
      </div>
    </main>
  </div>

  <el-drawer v-model="commentDrawer" title="Comments" size="30%">
    <CommentPanel :comments="comments" :anchor-preview="anchorPreview" @submit="submitComment" />
  </el-drawer>

  <ChatDrawer v-model="chatDrawer" :messages="chatMessages" @send="sendChat" />

  <el-drawer v-model="taskDrawer" title="Tasks" size="32%">
    <el-form label-position="top">
      <el-form-item label="Title">
        <el-input v-model="taskForm.title" placeholder="Task title" />
      </el-form-item>
      <el-form-item label="Description">
        <el-input v-model="taskForm.description" type="textarea" rows="2" />
      </el-form-item>
      <el-form-item label="Assignee">
        <el-select
          v-model="taskForm.assigneeId"
          filterable
          remote
          placeholder="Search user"
          :remote-method="searchUser"
        >
          <el-option v-for="user in userOptions" :key="user.id" :label="user.label" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="Due date">
        <el-date-picker v-model="taskForm.dueDate" type="date" placeholder="Select" style="width: 100%" />
      </el-form-item>
      <el-form-item>
        <el-button class="gradient-button" type="primary" @click="createTask">Create Task</el-button>
      </el-form-item>
    </el-form>
    <el-divider />
    <div v-for="task in tasks" :key="task.id" class="task-card">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <div><strong>{{ task.title }}</strong></div>
          <div class="text-muted">{{ task.description || 'No description' }}</div>
          <div class="text-muted">
            Assignee: {{ userLabel(task.assigneeId) || 'Unassigned' }} · Due: {{ formatDate(task.dueDate) || '-' }}
          </div>
        </div>
        <el-tag :type="task.status === 'DONE' ? 'success' : 'info'">{{ task.status }}</el-tag>
      </div>
      <div style="display: flex; gap: 8px; margin-top: 8px; align-items: center;">
        <el-select
          v-model="task.assigneeId"
          placeholder="Assignee"
          filterable
          remote
          :remote-method="searchUser"
          @change="(val) => updateTask(task.id, task.status, val, task.dueDate)"
        >
          <el-option v-for="user in userOptions" :key="user.id" :label="user.label" :value="user.id" />
        </el-select>
        <el-select
          v-model="task.status"
          placeholder="Status"
          style="width: 120px"
          @change="(val) => updateTask(task.id, val, task.assigneeId, task.dueDate)"
        >
          <el-option label="TODO" value="TODO" />
          <el-option label="DONE" value="DONE" />
        </el-select>
        <el-date-picker
          :model-value="task.dueDate ? new Date(task.dueDate) : null"
          type="date"
          placeholder="Due date"
          @change="(val) => updateTask(task.id, task.status, task.assigneeId, val)"
        />
      </div>
    </div>
  </el-drawer>

  <el-dialog v-model="shareDialog" title="Share Document" width="420px">
    <el-form label-position="top">
      <el-form-item label="User">
        <el-select v-model="shareUserId" filterable remote placeholder="Search user" :remote-method="searchUser">
          <el-option v-for="user in userOptions" :key="user.id" :label="user.label" :value="user.id" />
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
    <el-divider />
    <div class="text-muted" style="margin-bottom: 6px;">Current Access</div>
    <el-table :data="aclList" size="small">
      <el-table-column prop="userName" label="User">
        <template #default="scope">
          {{ scope.row.userName || scope.row.email || `User${scope.row.userId}` }}
        </template>
      </el-table-column>
      <el-table-column prop="perm" label="Permission" width="120">
        <template #default="scope">
          <el-select
            v-model="scope.row.perm"
            size="small"
            style="width: 100px"
            @change="(val) => updateAcl(scope.row.userId, val)"
          >
            <el-option label="VIEW" value="VIEW" />
            <el-option label="EDIT" value="EDIT" />
            <el-option label="ADMIN" value="ADMIN" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="120">
        <template #default="scope">
          <el-button size="small" text type="danger" @click="revokeAcl(scope.row.userId)">Revoke</el-button>
        </template>
      </el-table-column>
    </el-table>
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
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useCollabSocket } from '@/ws/collab'
import { 
  RemoteCursorExtension, 
  updateRemoteCursor, 
  removeRemoteCursor, 
  setRemoteCursors, 
  transformCursorsWithOperations,
  getRemoteCursors,
  transformPosition,
  RemoteCursor,
  TextOperation 
} from '@/editor/remoteCursor'
import { LineLengthLimitExtension } from '@/editor/lineLengthLimit'
import PresenceList from '@/components/PresenceList.vue'
import CommentPanel from '@/components/CommentPanel.vue'
import ChatDrawer from '@/components/ChatDrawer.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const docId = Number(route.params.id)

const title = ref('')
const saveStatus = ref('Saved')
const lastLocalHtml = ref('')
const lastSavedHtml = ref('')

const draftKey = () => `draft_doc_${docId}_${auth.user?.id || 'guest'}`
const comments = ref<any[]>([])
const chatMessages = ref<any[]>([])
const snapshots = ref<any[]>([])
const presenceMembers = ref<any[]>([])
const tasks = ref<any[]>([])
const docPerm = ref<'VIEW' | 'EDIT' | 'ADMIN'>('VIEW')
const canEdit = computed(() => docPerm.value === 'EDIT' || docPerm.value === 'ADMIN')
const canAdmin = computed(() => auth.isAdmin || docPerm.value === 'ADMIN')
const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const presenceChips = computed(() =>
  presenceMembers.value.map((m: any) => {
    const avatar = m.avatarUrl && m.avatarUrl.startsWith('/uploads') ? `${apiBase}${m.avatarUrl}` : m.avatarUrl
    return { ...m, avatarUrl: avatar, color: pickColor(m.userId || 0) }
  })
)

const commentDrawer = ref(false)
const chatDrawer = ref(false)
const taskDrawer = ref(false)
const shareDialog = ref(false)
const historyDialog = ref(false)
const draftDialog = ref(false)

const shareUserId = ref<number | null>(null)
const sharePerm = ref('VIEW')
const userOptions = ref<any[]>([])
const aclList = ref<any[]>([])

const taskForm = ref<{ title: string; description: string; assigneeId?: number; dueDate?: Date | null }>({
  title: '',
  description: '',
  assigneeId: undefined,
  dueDate: null
})
const tablePopover = ref(false)
const hoverRows = ref(3)
const hoverCols = ref(3)

const anchorPreview = ref('')
let anchorPayload: string | null = null

const remoteCursorMap = new Map<number, RemoteCursor>()
const myColor = computed(() => pickColor(auth.user?.id || 0))

// 行长度限制（字符数）
const MAX_LINE_LENGTH = 150

const { connect, send, close } = useCollabSocket()

let saveTimer: any = null
let broadcastTimer: any = null
let cursorTimer: any = null
let isApplyingRemote = false
let lastRemoteUpdate = 0
let lastContentUpdateAt = 0
const pendingContent = ref<string | null>(null)
const docReady = ref(false)
const awaitingSync = ref(false)
let lastCursorRefreshAt = 0

// 存储待广播的操作（用于 OT 转换）
let pendingOperations: TextOperation[] = []
// 上一次内容的文本长度（用于计算操作）
let lastTextLength = 0

const editor = useEditor({
  content: '',
  editable: canEdit.value,
  extensions: [
    StarterKit,
    Underline,
    Link.configure({ openOnClick: false }),
    Placeholder.configure({ placeholder: 'Start typing...' }),
    Table.configure({ resizable: false, HTMLAttributes: { class: 'doc-table' } }),
    TableRow,
    TableHeader,
    TableCell,
    RemoteCursorExtension,
    LineLengthLimitExtension.configure({ maxLength: MAX_LINE_LENGTH })
  ],
  onUpdate: ({ editor, transaction }) => {
    if (isApplyingRemote || !canEdit.value || !docReady.value) return
    
    // 从 transaction 中提取操作用于 OT 转换
    if (transaction.docChanged) {
      transaction.steps.forEach((step) => {
        const stepMap = step.getMap()
        stepMap.forEach((oldStart, oldEnd, newStart, newEnd) => {
          const oldLen = oldEnd - oldStart
          const newLen = newEnd - newStart
          
          if (oldLen === 0 && newLen > 0) {
            // 插入操作
            pendingOperations.push({
              type: 'insert',
              position: oldStart,
              length: newLen,
              userId: auth.user?.id
            })
          } else if (oldLen > 0 && newLen === 0) {
            // 删除操作
            pendingOperations.push({
              type: 'delete',
              position: oldStart,
              length: oldLen,
              userId: auth.user?.id
            })
          } else if (oldLen > 0 && newLen > 0) {
            // 替换 = 删除 + 插入
            pendingOperations.push({
              type: 'delete',
              position: oldStart,
              length: oldLen,
              userId: auth.user?.id
            })
            pendingOperations.push({
              type: 'insert',
              position: oldStart,
              length: newLen,
              userId: auth.user?.id
            })
          }
        })
      })
    }
    
    const html = editor.getHTML()
    lastLocalHtml.value = html
    lastTextLength = editor.state.doc.content.size
    const now = Date.now()
    localStorage.setItem(draftKey(), JSON.stringify({ content: html, updatedAt: now }))
    scheduleSave(html)
    scheduleBroadcast(html)
  },
  onSelectionUpdate: ({ editor }) => {
    const { from, to } = editor.state.selection
    scheduleCursor(from, to)
  }
})

watch(canEdit, (val) => {
  editor.value?.setEditable(val)
})

const normalizeHtml = (html: string) =>
  html
    .replace(/<p><br><\/p>/g, '<p></p>')
    .replace(/<p><br\/><\/p>/g, '<p></p>')
    .replace(/\s+/g, ' ')
    .trim()

const isEffectivelyEmpty = (html: string) =>
  html.replace(/<[^>]+>/g, '').replace(/\s+/g, '').length === 0

const loadDoc = async () => {
  try {
    const { data } = await http.get(`/api/docs/${docId}`)
    title.value = data.data.doc.title
    const serverContent = data.data.content || ''
    lastSavedHtml.value = serverContent
    lastLocalHtml.value = serverContent
    const serverUpdated = data.data.updatedAt ? new Date(data.data.updatedAt).getTime() : 0
    const draftRaw = localStorage.getItem(draftKey())
    if (draftRaw) {
      const draft = JSON.parse(draftRaw)
      const normalizedDraft = normalizeHtml(draft.content || '')
      const normalizedServer = normalizeHtml(serverContent)
      const bothEmpty = isEffectivelyEmpty(draft.content || '') && isEffectivelyEmpty(serverContent)
      const draftEmpty = isEffectivelyEmpty(draft.content || '')
      const serverEmpty = isEffectivelyEmpty(serverContent)
      const hasDiff = normalizedDraft !== normalizedServer && !bothEmpty
      if (draftEmpty && !serverEmpty) {
        localStorage.removeItem(draftKey())
      } else if (draft.updatedAt > serverUpdated && hasDiff) {
        draftDialog.value = true
      } else if (!hasDiff) {
        localStorage.removeItem(draftKey())
      }
    }
    if (editor.value) {
      isApplyingRemote = true
      editor.value.commands.setContent(serverContent, false)
      isApplyingRemote = false
      docReady.value = true
    } else {
      pendingContent.value = serverContent
    }
  } catch (err: any) {
    const status = err?.response?.status
    if (status === 403) {
      ElMessage.error('No permission to view this document')
    } else if (status === 404) {
      ElMessage.error('Document not found')
    } else {
      ElMessage.error('Failed to load document')
    }
    router.push('/docs')
  }
}

const loadPerm = async () => {
  const { data } = await http.get(`/api/docs/${docId}/acl/me`)
  docPerm.value = (data.data?.perm as 'VIEW' | 'EDIT' | 'ADMIN') || 'VIEW'
}

const loadComments = async () => {
  const { data } = await http.get(`/api/docs/${docId}/comments`)
  comments.value = data.data || []
}

const loadChat = async () => {
  const { data } = await http.get(`/api/docs/${docId}/chat`)
  chatMessages.value = (data.data || []).map((m: any) => ({
    ...m,
    fileUrl: m.content?.startsWith('[file]') ? m.content.split('|')[1] : undefined,
    fileName: m.content?.startsWith('[file]') ? m.content.substring(6, m.content.indexOf('|')) : undefined,
    content: m.content?.startsWith('[file]') ? '' : m.content
  }))
}

const loadTasks = async () => {
  const { data } = await http.get(`/api/docs/${docId}/tasks`)
  tasks.value = data.data || []
}

const connectSocket = () => {
  awaitingSync.value = true
  connect(docId, (msg) => {
    if (msg.docId && msg.docId !== docId) return
    if (msg.type === 'presence') {
      presenceMembers.value = msg.payload?.members || []
      const memberIds = new Set(presenceMembers.value.map((m: any) => m.userId))
      // 移除离开的用户的光标
      const removedUserIds: number[] = []
      remoteCursorMap.forEach((_value, key) => {
        if (!memberIds.has(key)) {
          removedUserIds.push(key)
        }
      })
      removedUserIds.forEach(userId => {
        remoteCursorMap.delete(userId)
        if (editor.value) {
          removeRemoteCursor(editor.value, userId)
        }
      })
      // 如果有移除操作，更新所有光标显示
      if (removedUserIds.length > 0 && editor.value) {
        setRemoteCursors(editor.value, Array.from(remoteCursorMap.values()))
      }
    }
    if (msg.type === 'sync_request') {
      const requesterId = msg.payload?.requesterId
      if (!requesterId || !docReady.value) return
      const content = editor.value?.getHTML() || pendingContent.value || ''
      send({
        type: 'content_update',
        docId,
        senderId: auth.user?.id,
        senderName: auth.user?.nickname,
        payload: { content, updatedAt: Date.now(), targetUserId: requesterId }
      })
    }
    if (msg.type === 'content_update') {
      if (msg.senderId === auth.user?.id) return
      const targetRaw = msg.payload?.targetUserId
      const targetUserId = targetRaw !== undefined && targetRaw !== null ? Number(targetRaw) : null
      if (targetUserId && targetUserId !== auth.user?.id) return
      if (targetUserId && !awaitingSync.value) return
      const updatedAt = msg.payload?.updatedAt || 0
      const contentTimestamp = msg.timestamp || Date.now()
      lastContentUpdateAt = contentTimestamp
      if (updatedAt <= lastRemoteUpdate) return
      if (targetUserId && awaitingSync.value) {
        const incoming = msg.payload?.content || ''
        const localHtml = editor.value?.getHTML() || pendingContent.value || ''
        if (!isEffectivelyEmpty(localHtml) && isEffectivelyEmpty(incoming)) {
          return
        }
        awaitingSync.value = false
      }
      lastRemoteUpdate = updatedAt
      isApplyingRemote = true
      
      // 保存当前选区位置
      const sel = editor.value?.state.selection
      const origFrom = sel?.from ?? 0
      const origTo = sel?.to ?? origFrom
      
      // 获取远程发送的操作列表（用于 OT 转换）
      const remoteOperations = (msg.payload?.operations || []) as TextOperation[]
      
      // 应用新内容
      editor.value?.commands.setContent(msg.payload?.content || '', false)
      
      // 获取新文档状态
      const newDocSize = editor.value?.state.doc.content.size || 0
      
      // 使用 OT 算法转换本地用户的选区位置
      if (sel && editor.value && remoteOperations.length > 0) {
        // 根据远程操作转换本地光标位置
        let transformedFrom = origFrom
        let transformedTo = origTo
        
        for (const op of remoteOperations) {
          // 使用 transformPosition 函数计算新位置
          transformedFrom = transformPosition(transformedFrom, op)
          transformedTo = transformPosition(transformedTo, op)
        }
        
        // 确保位置在文档范围内
        const safeFrom = Math.max(0, Math.min(transformedFrom, newDocSize))
        const safeTo = Math.max(0, Math.min(transformedTo, newDocSize))
        
        try {
          editor.value.commands.setTextSelection({ from: safeFrom, to: safeTo })
        } catch {
          try {
            editor.value.commands.setTextSelection(Math.max(1, newDocSize - 1))
          } catch {
            /* ignore */
          }
        }
      } else if (sel && editor.value) {
        // 没有操作信息时，简单地限制在文档范围内
        const safeFrom = Math.min(origFrom, newDocSize)
        const safeTo = Math.min(origTo, newDocSize)
        
        try {
          editor.value.commands.setTextSelection({ from: safeFrom, to: safeTo })
        } catch {
          try {
            editor.value.commands.setTextSelection(Math.max(1, newDocSize - 1))
          } catch {
            /* ignore */
          }
        }
      }
      
      // 使用 OT 算法转换远程光标位置
      if (editor.value && remoteOperations.length > 0) {
        // 使用收到的操作转换所有远程光标（排除发送者）
        transformCursorsWithOperations(editor.value, remoteOperations, msg.senderId)
        
        // 同步 remoteCursorMap
        const updatedCursors = getRemoteCursors(editor.value)
        remoteCursorMap.clear()
        updatedCursors.forEach(cursor => {
          remoteCursorMap.set(cursor.userId, cursor)
        })
      } else if (editor.value) {
        // 没有操作信息时，确保光标在有效范围内
        const docSize = newDocSize
        remoteCursorMap.forEach((cursor, userId) => {
          if (userId === msg.senderId) return
          const clampedFrom = Math.max(0, Math.min(cursor.from, docSize))
          const clampedTo = Math.max(0, Math.min(cursor.to, docSize))
          if (clampedFrom !== cursor.from || clampedTo !== cursor.to) {
            remoteCursorMap.set(userId, {
              ...cursor,
              from: clampedFrom,
              to: clampedTo
            })
          }
        })
        setRemoteCursors(editor.value, Array.from(remoteCursorMap.values()))
      }
      
      isApplyingRemote = false
      docReady.value = true
      // 请求其他用户更新他们的光标位置
      requestCursors()
    }
    if (msg.type === 'cursor_update') {
      if (!msg.payload) return
      if (msg.senderId === auth.user?.id) return
      const cursorTimestamp = msg.timestamp || Date.now()
      const docSize = editor.value?.state.doc.content.size || 0
      const cursor: RemoteCursor = {
        userId: msg.senderId || 0,
        name: msg.payload.name || `User${msg.senderId}`,
        color: msg.payload.color || '#2a7cf7',
        from: Math.min(Math.max(0, msg.payload.from || 0), docSize),
        to: Math.min(Math.max(0, msg.payload.to || 0), docSize),
        updatedAt: cursorTimestamp
      }
      remoteCursorMap.set(cursor.userId, cursor)
      if (editor.value) {
        updateRemoteCursor(editor.value, cursor)
      }
    }
    if (msg.type === 'cursor_request') {
      if (msg.senderId && msg.senderId === auth.user?.id) return
      if (!editor.value) return
      const { from, to } = editor.value.state.selection
      sendCursor(from, to)
    }
    if (msg.type === 'chat_message') {
      chatMessages.value.push({
        id: msg.payload?.id,
        senderId: msg.senderId,
        senderName: msg.senderName,
        content: msg.payload?.fileUrl ? '' : msg.payload?.content,
        fileUrl: msg.payload?.fileUrl,
        fileName: msg.payload?.fileName,
        createdAt: msg.payload?.createdAt
      })
    }
  })
  setTimeout(() => {
    if (editor.value) {
      const { from, to } = editor.value.state.selection
      sendCursor(from, to)
    }
  }, 400)
}

const doSave = async (html: string, silent?: boolean) => {
  try {
    await http.put(`/api/docs/${docId}/content`, { content: html, clientUpdatedAt: Date.now() })
    lastSavedHtml.value = html
    if (lastLocalHtml.value === html) {
      localStorage.removeItem(draftKey())
    }
    saveStatus.value = 'Saved'
  } catch (err) {
    saveStatus.value = 'Save failed'
    if (!silent) {
      ElMessage.error('Auto-save failed')
    }
  }
}

const scheduleSave = (html: string) => {
  saveStatus.value = 'Saving...'
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    void doSave(html, true)
  }, 2000)
}

const scheduleBroadcast = (html: string) => {
  clearTimeout(broadcastTimer)
  broadcastTimer = setTimeout(() => {
    // 复制并清空待发送的操作
    const operationsToSend = [...pendingOperations]
    pendingOperations = []
    
    send({
      type: 'content_update',
      docId,
      senderId: auth.user?.id,
      senderName: auth.user?.nickname,
      payload: { 
        content: html, 
        updatedAt: Date.now(),
        operations: operationsToSend  // 包含操作信息用于 OT 转换
      }
    })
    if (editor.value) {
      const { from, to } = editor.value.state.selection
      sendCursor(from, to)
    }
  }, 800)
}

const scheduleCursor = (from: number, to: number) => {
  clearTimeout(cursorTimer)
  cursorTimer = setTimeout(() => {
    sendCursor(from, to)
  }, 200)
}

const sendCursor = (from: number, to: number) => {
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
}

const requestCursors = () => {
  const now = Date.now()
  if (now - lastCursorRefreshAt < 300) return
  lastCursorRefreshAt = now
  send({
    type: 'cursor_request',
    docId,
    senderId: auth.user?.id
  })
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

const insertTable = (rows: number, cols: number) => {
  tablePopover.value = false
  editor.value?.chain().focus().insertTable({ rows, cols, withHeaderRow: true }).run()
}

const ensureTableAction = (run: () => boolean) => {
  const ok = run()
  if (!ok) {
    ElMessage.warning('请先选中表格')
  }
}

const addRow = () => editor.value && ensureTableAction(() => editor.value!.chain().focus().addRowAfter().run())
const addColumn = () => editor.value && ensureTableAction(() => editor.value!.chain().focus().addColumnAfter().run())
const deleteTable = () => editor.value && ensureTableAction(() => editor.value!.chain().focus().deleteTable().run())

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

const sendChat = (content: string, file?: File) => {
  if (file) {
    const form = new FormData()
    form.append('file', file)
    http
      .post(`/api/docs/${docId}/chat/upload`, form, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      .then(({ data }) => {
        send({
          type: 'chat_message',
          docId,
          senderId: auth.user?.id,
          senderName: auth.user?.nickname,
          payload: { fileUrl: data.data.url, fileName: data.data.name, content }
        })
      })
    return
  }
  send({
    type: 'chat_message',
    docId,
    senderId: auth.user?.id,
    senderName: auth.user?.nickname,
    payload: { content }
  })
}

const saveTitle = async () => {
  if (!canEdit.value) return
  await http.put(`/api/docs/${docId}`, { title: title.value })
}

const openShare = async () => {
  if (!canAdmin.value) {
    ElMessage.warning('Only doc admins can manage sharing')
    return
  }
  shareDialog.value = true
  await loadAclList()
  await searchUser('')
}

const searchUser = async (keyword: string) => {
  const { data } = await http.get('/api/users/search', { params: { keyword } })
  userOptions.value = (data.data || []).map((user: any) => ({
    id: user.id,
    label: user.nickname ? `${user.nickname} (${user.email})` : user.email
  }))
}

const shareDoc = async () => {
  if (!shareUserId.value) return
  await flushSave()
  await http.post(`/api/docs/${docId}/acl`, { userId: shareUserId.value, perm: sharePerm.value })
  shareUserId.value = null
  sharePerm.value = 'VIEW'
  await loadAclList()
}

const loadAclList = async () => {
  const { data } = await http.get(`/api/docs/${docId}/acl`)
  aclList.value = data.data || []
}

const updateAcl = async (userId: number, perm: string) => {
  if (!userId) return
  await http.post(`/api/docs/${docId}/acl`, { userId, perm })
  await loadAclList()
}

const revokeAcl = async (userId: number) => {
  if (!userId) return
  await http.delete(`/api/docs/${docId}/acl/${userId}`)
  await loadAclList()
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

const openTasks = async () => {
  taskDrawer.value = true
  await searchUser('')
  await loadTasks()
}

const createTask = async () => {
  if (!taskForm.value.title.trim()) return
  await http.post(`/api/docs/${docId}/tasks`, {
    title: taskForm.value.title,
    description: taskForm.value.description,
    assigneeId: taskForm.value.assigneeId,
    dueDate: taskForm.value.dueDate ? taskForm.value.dueDate.toISOString() : undefined
  })
  taskForm.value = { title: '', description: '', assigneeId: undefined, dueDate: null }
  await loadTasks()
}

const updateTask = async (taskId: number, status: string, assigneeId?: number, dueDate?: string | Date | null) => {
  await http.put(`/api/docs/${docId}/tasks/${taskId}`, {
    status,
    assigneeId,
    dueDate: dueDate instanceof Date ? dueDate.toISOString() : dueDate || undefined
  })
  await loadTasks()
}

const restoreDraft = () => {
  const draftRaw = localStorage.getItem(draftKey())
  if (!draftRaw || !editor.value) return
  const draft = JSON.parse(draftRaw)
  if (isEffectivelyEmpty(draft.content || '') && !isEffectivelyEmpty(lastSavedHtml.value)) {
    localStorage.removeItem(draftKey())
    draftDialog.value = false
    ElMessage.warning('Draft was empty and discarded')
    return
  }
  lastLocalHtml.value = draft.content
  editor.value.commands.setContent(draft.content)
  draftDialog.value = false
}

const discardDraft = () => {
  localStorage.removeItem(draftKey())
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

const exportDoc = async (format: 'html' | 'markdown') => {
  const { data } = await http.get(`/api/docs/${docId}/export`, {
    params: { format },
    responseType: 'blob'
  })
  downloadBlob(data, `doc_${docId}.${format === 'markdown' ? 'md' : 'html'}`)
}

const flushSave = async () => {
  if (!canEdit.value || !editor.value) return
  const html = editor.value.getHTML()
  if (!html || html === lastSavedHtml.value) return
  clearTimeout(saveTimer)
  saveStatus.value = 'Saving...'
  await doSave(html, true)
}

const beforeUnloadHandler = () => {
  if (!canEdit.value || !editor.value) return
  const html = editor.value.getHTML()
  if (!html || html === lastSavedHtml.value) return
  const payload = JSON.stringify({ content: html, clientUpdatedAt: Date.now() })
  const url = `${apiBase}/api/docs/${docId}/content`
  fetch(url, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(auth.token ? { Authorization: `Bearer ${auth.token}` } : {})
    },
    body: payload,
    keepalive: true
  }).catch(() => {})
}

const downloadBlob = (blobData: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blobData)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  window.URL.revokeObjectURL(url)
}

const formatTime = (raw: string) => (raw ? new Date(raw).toLocaleString() : '')
const formatDate = (raw?: string) => (raw ? new Date(raw).toLocaleDateString() : '')
const userLabel = (userId?: number) =>
  userOptions.value.find((u) => u.id === userId)?.label || (userId ? `User ${userId}` : '')

function pickColor(seed: number) {
  const palette = ['#2a7cf7', '#ff6b6b', '#6bcf63', '#f7b32b', '#8b5cf6']
  return palette[Math.abs(seed) % palette.length]
}

// 一键发起会议（使用浏览器 WebRTC 会议室 meet.jit.si）
const startMeeting = () => {
  const link = `https://meet.jit.si/collab-doc-${docId}`
  window.open(link, '_blank', 'noopener')
  sendChat(`Join meeting: ${link}`)
}

onMounted(async () => {
  if (auth.token) {
    await auth.fetchMe()
  }
  await loadPerm()
  await loadDoc()
  await loadComments()
  await loadChat()
  await loadTasks()
  connectSocket()
  window.addEventListener('beforeunload', beforeUnloadHandler)
})

watch(editor, (instance) => {
  if (instance && pendingContent.value !== null) {
    isApplyingRemote = true
    instance.commands.setContent(pendingContent.value, false)
    isApplyingRemote = false
    pendingContent.value = null
    docReady.value = true
  }
})

onBeforeUnmount(() => {
  void flushSave()
  window.removeEventListener('beforeunload', beforeUnloadHandler)
  close()
})
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}

.task-card {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 12px;
}

.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.presence-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 8px 0;
}

.presence-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
}

.table-grid {
  display: inline-block;
  padding: 6px;
}

.table-row {
  display: flex;
}

.table-cell {
  width: 24px;
  height: 24px;
  border: 1px solid #e5e7eb;
  margin: 2px;
  border-radius: 4px;
}

.table-cell.active {
  background: #cce5ff;
  border-color: #2a7cf7;
}

.editor-shell {
  position: relative;
}

:deep(.ProseMirror table),
:deep(.ProseMirror table.doc-table) {
  border-collapse: collapse;
  width: 100%;
  margin-top: 8px;
}

:deep(.ProseMirror th),
:deep(.ProseMirror td) {
  border: 1px solid #d9d9d9;
  padding: 6px;
  vertical-align: top;
}

:deep(.ProseMirror th p),
:deep(.ProseMirror td p) {
  margin: 0;
}

:deep(.ProseMirror th) {
  background: #f7f8fa;
  font-weight: 600;
}
</style>
