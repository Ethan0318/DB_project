<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h3>Collab Edit</h3>
        <el-button size="small" @click="openCreate">New</el-button>
      </div>
      <el-input v-model="keyword" placeholder="Search docs" clearable @change="loadDocs" />
      <div style="margin-top: 16px;">
        <div class="text-muted">Tags</div>
        <el-menu :default-active="String(selectedTagId || 'all')" class="el-menu-vertical">
          <el-menu-item index="all" @click="selectTag(null)">All</el-menu-item>
          <el-menu-item v-for="tag in tags" :key="tag.id" :index="String(tag.id)" @click="selectTag(tag.id)">
            {{ tag.name }}
          </el-menu-item>
        </el-menu>
        <el-button text size="small" @click="tagDialog = true">+ New tag</el-button>
      </div>
      <div style="margin-top: auto; padding-top: 24px;">
        <el-button text @click="goNotifications">Notifications ({{ unreadCount }})</el-button>
        <el-button text @click="logout">Logout</el-button>
      </div>
    </aside>

    <main class="content">
      <div class="panel">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <h2>Your Documents</h2>
            <div class="text-muted">Collaborate in real time.</div>
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-avatar :size="36" :src="auth.user?.avatarUrl">
              {{ auth.user?.nickname?.slice(0, 1) || 'U' }}
            </el-avatar>
            <div>
              <div>{{ auth.user?.nickname || auth.user?.email }}</div>
              <div class="text-muted">{{ auth.user?.email }}</div>
            </div>
          </div>
        </div>

        <el-table :data="docs" style="width: 100%; margin-top: 16px">
          <el-table-column prop="title" label="Title" />
          <el-table-column prop="updatedAt" label="Updated">
            <template #default="scope">
              {{ formatTime(scope.row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="tagId" label="Tag">
            <template #default="scope">
              {{ tagName(scope.row.tagId) }}
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="160">
            <template #default="scope">
              <el-button size="small" @click="openDoc(scope.row.id)">Open</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </main>
  </div>

  <el-dialog v-model="createDialog" title="New Document" width="420px">
    <el-form label-position="top">
      <el-form-item label="Title">
        <el-input v-model="newTitle" placeholder="Doc title" />
      </el-form-item>
      <el-form-item label="Tag">
        <el-select v-model="newTagId" placeholder="Select tag" clearable>
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialog = false">Cancel</el-button>
      <el-button class="gradient-button" type="primary" @click="createDoc">Create</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="tagDialog" title="New Tag" width="360px">
    <el-form label-position="top">
      <el-form-item label="Tag name">
        <el-input v-model="newTag" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="tagDialog = false">Cancel</el-button>
      <el-button class="gradient-button" type="primary" @click="createTag">Create</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const auth = useAuthStore()
const notificationStore = useNotificationStore()

const docs = ref<any[]>([])
const tags = ref<any[]>([])
const keyword = ref('')
const selectedTagId = ref<number | null>(null)

const createDialog = ref(false)
const newTitle = ref('')
const newTagId = ref<number | null>(null)

const tagDialog = ref(false)
const newTag = ref('')

const loadDocs = async () => {
  const { data } = await http.get('/api/docs', {
    params: {
      keyword: keyword.value || undefined,
      tagId: selectedTagId.value || undefined,
      sort: 'updated'
    }
  })
  docs.value = data.data || []
}

const loadTags = async () => {
  const { data } = await http.get('/api/tags')
  tags.value = data.data || []
}

const selectTag = (tagId: number | null) => {
  selectedTagId.value = tagId
  loadDocs()
}

const openCreate = () => {
  createDialog.value = true
}

const createDoc = async () => {
  if (!newTitle.value.trim()) return
  const { data } = await http.post('/api/docs', {
    title: newTitle.value,
    tagId: newTagId.value || undefined
  })
  createDialog.value = false
  newTitle.value = ''
  newTagId.value = null
  router.push(`/docs/${data.data.id}`)
}

const createTag = async () => {
  if (!newTag.value.trim()) return
  await http.post('/api/tags', { name: newTag.value })
  newTag.value = ''
  tagDialog.value = false
  loadTags()
}

const openDoc = (id: number) => {
  router.push(`/docs/${id}`)
}

const tagName = (tagId?: number) => {
  if (!tagId) return '-'
  const tag = tags.value.find((t) => t.id === tagId)
  return tag ? tag.name : '-'
}

const formatTime = (raw: string) => (raw ? new Date(raw).toLocaleString() : '')

const logout = () => {
  auth.logout()
  router.push('/login')
}

const goNotifications = () => {
  router.push('/notifications')
}

const unreadCount = computed(() => notificationStore.unreadCount)

onMounted(async () => {
  await auth.fetchMe()
  await loadTags()
  await loadDocs()
})
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
