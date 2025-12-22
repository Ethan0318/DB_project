<template>
  <div class="app-shell">
    <aside class="sidebar">
      <el-button text @click="goBack">Back</el-button>
      <div style="margin-top: 16px;">
        <div class="text-muted">Filters</div>
        <el-select v-model="filterType" placeholder="Type" clearable>
          <el-option label="DOC_SHARE" value="DOC_SHARE" />
          <el-option label="MENTION" value="MENTION" />
          <el-option label="REPLY" value="REPLY" />
        </el-select>
        <el-select v-model="filterRead" placeholder="Read" clearable style="margin-top: 8px;">
          <el-option label="Unread" :value="0" />
          <el-option label="Read" :value="1" />
        </el-select>
        <el-button style="margin-top: 8px;" @click="load">Apply</el-button>
        <el-button text @click="markAll">Mark all read</el-button>
      </div>
    </aside>
    <main class="content">
      <div class="panel">
        <h2>Notifications</h2>
        <el-table :data="notifications">
          <el-table-column prop="type" label="Type" width="120" />
          <el-table-column prop="payload" label="Payload" />
          <el-table-column prop="createdAt" label="Time" width="200">
            <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="Status" width="120">
            <template #default="scope">{{ scope.row.readFlag === 0 ? 'Unread' : 'Read' }}</template>
          </el-table-column>
          <el-table-column label="Actions" width="120">
            <template #default="scope">
              <el-button size="small" @click="markRead(scope.row.id)">Mark</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const notificationStore = useNotificationStore()

const notifications = ref<any[]>([])
const filterType = ref<string | null>(null)
const filterRead = ref<number | null>(null)

const load = async () => {
  const { data } = await http.get('/api/notifications', {
    params: {
      type: filterType.value || undefined,
      readFlag: filterRead.value === null ? undefined : filterRead.value
    }
  })
  notifications.value = data.data || []
}

const markRead = async (id: number) => {
  await notificationStore.markRead(id)
  await load()
}

const markAll = async () => {
  await notificationStore.markAllRead()
  await load()
}

const goBack = () => router.push('/docs')

const formatTime = (raw: string) => (raw ? new Date(raw).toLocaleString() : '')

onMounted(load)
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
