<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h3>Collab Edit</h3>
        <el-button size="small" @click="openCreate">New</el-button>
      </div>
      <div class="text-muted" style="margin-top: 8px;">Quick Filters</div>
      <el-input v-model="keyword" placeholder="Search title / content" clearable @change="loadDocs" />
      <el-select
        v-model="sort"
        placeholder="Sort"
        style="margin-top: 8px; width: 100%;"
        @change="loadDocs"
      >
        <el-option label="Updated time" value="updated" />
        <el-option label="Title" value="title" />
      </el-select>
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
      <div style="margin-top: auto; padding-top: 24px; display: flex; flex-direction: column; gap: 6px;">
        <el-button text @click="goNotifications">Notifications ({{ unreadCount }})</el-button>
        <el-button text @click="goProfile">Profile</el-button>
        <el-button v-if="auth.isAdmin" text @click="goAdmin">Admin</el-button>
        <el-button text @click="openSurvey">Satisfaction Survey</el-button>
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
              <div class="text-muted">{{ auth.user?.email || auth.user?.phone }}</div>
            </div>
          </div>
        </div>

        <div class="filters">
          <el-select
            v-model="selectedAuthorId"
            filterable
            remote
            reserve-keyword
            placeholder="Author"
            :remote-method="searchAuthors"
            :loading="authorLoading"
            clearable
            style="width: 200px"
            @change="loadDocs"
          >
            <el-option v-for="item in authorOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            start-placeholder="From"
            end-placeholder="To"
            style="width: 280px"
            @change="loadDocs"
          />
          <el-button @click="openImport">Import Markdown</el-button>
          <el-button :disabled="selectedDocIds.length === 0" @click="batchDialog = true">Batch Export</el-button>
          <el-switch v-model="groupView" active-text="Group by tag" />
        </div>

        <template v-if="!groupView">
          <el-table
            :data="docs"
            style="width: 100%; margin-top: 8px"
            @selection-change="onSelectionChange"
          >
            <el-table-column type="selection" width="45" />
            <el-table-column prop="title" label="Title" />
            <el-table-column prop="updatedAt" label="Updated" width="180">
              <template #default="scope">
                {{ formatTime(scope.row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="tagId" label="Tag" width="140">
              <template #default="scope">
                {{ tagName(scope.row.tagId) }}
              </template>
            </el-table-column>
            <el-table-column label="Actions" width="200">
              <template #default="scope">
                <el-button size="small" @click="openDoc(scope.row.id)">Open</el-button>
                <el-button size="small" text @click="exportSingle(scope.row.id, 'html')">Export</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <template v-else>
          <div class="grouped">
            <div v-for="group in groupedDocs" :key="group.id" class="group-card">
              <div class="group-header">
                <span>{{ group.name }}</span>
                <span class="text-muted">{{ group.items.length }} docs</span>
              </div>
              <el-table
                :data="group.items"
                size="small"
                @selection-change="(rows) => onGroupSelectionChange(rows, group.id)"
              >
                <el-table-column type="selection" width="45" />
                <el-table-column prop="title" label="Title" />
                <el-table-column prop="updatedAt" label="Updated" width="170">
                  <template #default="scope">
                    {{ formatTime(scope.row.updatedAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="Actions" width="160">
                  <template #default="scope">
                    <el-button size="small" @click="openDoc(scope.row.id)">Open</el-button>
                    <el-button size="small" text @click="exportSingle(scope.row.id, 'html')">Export</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </template>
      </div>
    </main>
  </div>

  <input ref="importInput" type="file" accept=".md,.markdown" style="display: none" @change="handleImport" />

  <el-dialog v-model="createDialog" title="New Document" width="460px">
    <el-form label-position="top">
      <el-form-item label="Title">
        <el-input v-model="newTitle" placeholder="Doc title" />
      </el-form-item>
      <el-form-item label="Tag">
        <el-select v-model="newTagId" placeholder="Select tag" clearable>
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="Template">
        <el-select v-model="selectedTemplateId" placeholder="Choose template" clearable>
          <el-option v-for="tpl in templates" :key="tpl.id" :label="tpl.name" :value="tpl.id" />
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

  <el-dialog v-model="batchDialog" title="Batch Export" width="420px">
    <div class="text-muted">Selected {{ selectedDocIds.length }} docs</div>
    <el-radio-group v-model="batchFormat" style="margin-top: 12px">
      <el-radio-button label="html">HTML</el-radio-button>
      <el-radio-button label="markdown">Markdown</el-radio-button>
    </el-radio-group>
    <template #footer>
      <el-button @click="batchDialog = false">Cancel</el-button>
      <el-button class="gradient-button" type="primary" @click="batchExport">Export ZIP</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="surveyDialog" title="Satisfaction Survey" width="420px">
    <el-form label-position="top">
      <el-form-item label="Rating (1-5)">
        <el-rate v-model="surveyRating" :max="5" allow-half />
      </el-form-item>
      <el-form-item label="Feedback">
        <el-input v-model="surveyFeedback" type="textarea" rows="3" placeholder="Any suggestions?" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="surveyDialog = false">Cancel</el-button>
      <el-button class="gradient-button" type="primary" @click="submitSurvey">Submit</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const auth = useAuthStore()
const notificationStore = useNotificationStore()

const docs = ref<any[]>([])
const tags = ref<any[]>([])
const templates = ref<any[]>([])
const keyword = ref('')
const sort = ref('updated')
const selectedTagId = ref<number | null>(null)
const selectedAuthorId = ref<number | null>(null)
const dateRange = ref<[Date, Date] | null>(null)
const authorOptions = ref<{ value: number; label: string }[]>([])
const authorLoading = ref(false)
const groupView = ref(false)

const createDialog = ref(false)
const newTitle = ref('')
const newTagId = ref<number | null>(null)
const selectedTemplateId = ref<number | null>(null)

const tagDialog = ref(false)
const newTag = ref('')

const batchDialog = ref(false)
const batchFormat = ref<'html' | 'markdown'>('html')
const selectedDocIds = ref<number[]>([])

const surveyDialog = ref(false)
const surveyRating = ref(5)
const surveyFeedback = ref('')

const importInput = ref<HTMLInputElement | null>(null)

const groupedDocs = computed(() => {
  const tagMap = new Map<number, string>()
  tags.value.forEach((t: any) => tagMap.set(t.id, t.name))
  const bucket = new Map<string, { id: string; name: string; items: any[] }>()
  docs.value.forEach((d: any) => {
    const id = d.tagId ? String(d.tagId) : 'untagged'
    if (!bucket.has(id)) {
      bucket.set(id, {
        id,
        name: d.tagId ? tagMap.get(d.tagId) || 'Unknown' : 'Uncategorized',
        items: []
      })
    }
    bucket.get(id)!.items.push(d)
  })
  // keep tag order then untagged at end
  const ordered: { id: string; name: string; items: any[] }[] = []
  tags.value.forEach((t: any) => {
    const g = bucket.get(String(t.id))
    if (g) ordered.push(g)
  })
  if (bucket.has('untagged')) ordered.push(bucket.get('untagged')!)
  return ordered
})

const loadDocs = async () => {
  const params: any = {
    keyword: keyword.value || undefined,
    tagId: selectedTagId.value || undefined,
    sort: sort.value,
    authorId: selectedAuthorId.value || undefined
  }
  if (dateRange.value && dateRange.value.length === 2) {
    params.fromDate = dateRange.value[0].toISOString().slice(0, 10)
    params.toDate = dateRange.value[1].toISOString().slice(0, 10)
  }
  const { data } = await http.get('/api/docs', { params })
  docs.value = data.data || []
}

const loadTags = async () => {
  const { data } = await http.get('/api/tags')
  tags.value = data.data || []
}

const loadTemplates = async () => {
  const { data } = await http.get('/api/docs/templates')
  templates.value = data.data || []
}

const searchAuthors = async (q: string) => {
  authorLoading.value = true
  const { data } = await http.get('/api/users/search', { params: { keyword: q } })
  authorOptions.value = (data.data || []).map((u: any) => ({
    value: u.id,
    label: u.nickname ? `${u.nickname} (${u.email})` : u.email
  }))
  authorLoading.value = false
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
    tagId: newTagId.value || undefined,
    templateId: selectedTemplateId.value || undefined
  })
  createDialog.value = false
  newTitle.value = ''
  newTagId.value = null
  selectedTemplateId.value = null
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

const goNotifications = () => router.push('/notifications')
const goProfile = () => router.push('/profile')
const goAdmin = () => router.push('/admin')

const unreadCount = computed(() => notificationStore.unreadCount)

const onSelectionChange = (rows: any[]) => {
  selectedDocIds.value = rows.map((r) => r.id)
}

const onGroupSelectionChange = (rows: any[], groupId: string) => {
  const keep = new Set(selectedDocIds.value)
  const affected = docs.value.filter((d: any) => (d.tagId ? String(d.tagId) : 'untagged') === groupId)
  affected.forEach((d: any) => keep.delete(d.id))
  rows.forEach((r: any) => keep.add(r.id))
  selectedDocIds.value = Array.from(keep)
}

const openImport = () => {
  importInput.value?.click()
}

const handleImport = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  if (selectedTagId.value) {
    formData.append('tagId', String(selectedTagId.value))
  }
  const { data } = await http.post('/api/docs/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  target.value = ''
  router.push(`/docs/${data.data.id}`)
}

const exportSingle = async (id: number, format: 'html' | 'markdown') => {
  const { data } = await http.get(`/api/docs/${id}/export`, {
    params: { format },
    responseType: 'blob'
  })
  downloadBlob(data, `doc_${id}.${format === 'markdown' ? 'md' : 'html'}`)
}

const batchExport = async () => {
  const { data } = await http.post(
    '/api/docs/export/batch',
    { docIds: selectedDocIds.value, format: batchFormat.value },
    { responseType: 'blob' }
  )
  batchDialog.value = false
  downloadBlob(data, 'docs.zip')
}

const downloadBlob = (blobData: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blobData)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  window.URL.revokeObjectURL(url)
}

const openSurvey = () => {
  surveyDialog.value = true
}

const submitSurvey = async () => {
  await http.post('/api/survey', {
    rating: Math.round(surveyRating.value),
    feedback: surveyFeedback.value
  })
  surveyDialog.value = false
  surveyFeedback.value = ''
}

onMounted(async () => {
  if (auth.token) {
    await auth.fetchMe()
  }
  await loadTags()
  await loadTemplates()
  await loadDocs()
  window.addEventListener('doc-shared', loadDocs)
})

onUnmounted(() => {
  window.removeEventListener('doc-shared', loadDocs)
})
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
  flex-wrap: wrap;
}

.grouped {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 12px;
}

.group-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0 8px;
}
</style>
