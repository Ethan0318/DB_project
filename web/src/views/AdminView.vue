<template>
  <div class="app-shell" style="padding: 24px;">
    <div class="panel">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2>Admin Console</h2>
          <div class="text-muted">Manage users, roles, and see basic analytics.</div>
        </div>
        <el-button text @click="goBack">Back</el-button>
      </div>

      <h3 style="margin-top: 12px;">Analytics</h3>
      <div class="analytics-grid">
        <div v-for="item in analyticsCards" :key="item.label" class="card">
          <div class="text-muted">{{ item.label }}</div>
          <div class="value">{{ item.value }}</div>
        </div>
      </div>

      <h3 style="margin-top: 16px;">Users</h3>
      <el-table :data="users" style="width: 100%">
        <el-table-column prop="email" label="Email" />
        <el-table-column prop="nickname" label="Nickname" />
        <el-table-column prop="phone" label="Phone" />
        <el-table-column prop="roles" label="Role">
          <template #default="scope">
            <el-select
              v-model="userRole[scope.row.id]"
              placeholder="Role"
              style="width: 140px;"
              @change="(val) => updateRole(scope.row.id, val)"
            >
              <el-option label="ADMIN" value="ADMIN" />
              <el-option label="EDITOR" value="EDITOR" />
              <el-option label="VIEWER" value="VIEWER" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="Last Login">
          <template #default="scope">{{ formatTime(scope.row.lastLoginAt) }}</template>
        </el-table-column>
      </el-table>

      <h3 style="margin-top: 16px;">Survey Responses</h3>
      <el-table :data="surveyResponses" style="width: 100%">
        <el-table-column prop="userId" label="User" width="120" />
        <el-table-column prop="rating" label="Rating" width="100" />
        <el-table-column prop="feedback" label="Feedback" />
        <el-table-column prop="createdAt" label="Time" width="200">
          <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <h3 style="margin-top: 16px;">Operation Logs</h3>
      <el-table :data="opLogs" style="width: 100%">
        <el-table-column prop="userId" label="User" width="120" />
        <el-table-column prop="action" label="Action" width="160" />
        <el-table-column prop="detail" label="Detail" />
        <el-table-column prop="createdAt" label="Time" width="200">
          <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'

const router = useRouter()

const users = ref<any[]>([])
const analytics = reactive<any>({})
const surveyResponses = ref<any[]>([])
const opLogs = ref<any[]>([])
const userRole = reactive<Record<number, string>>({})

const loadUsers = async () => {
  const { data } = await http.get('/api/admin/users')
  users.value = data.data || []
  users.value.forEach((u: any) => {
    userRole[u.id] = Array.from(u.roles || [])[0] || ''
  })
}

const loadAnalytics = async () => {
  const { data } = await http.get('/api/admin/analytics')
  Object.assign(analytics, data.data || {})
}

const loadSurvey = async () => {
  const { data } = await http.get('/api/admin/survey')
  surveyResponses.value = data.data || []
}

const loadLogs = async () => {
  const { data } = await http.get('/api/admin/oplogs', { params: { limit: 200 } })
  opLogs.value = data.data || []
}

const updateRole = async (userId: number, role: string) => {
  await http.put(`/api/admin/users/${userId}/role`, { roleCode: role })
}

const analyticsCards = computed(() => [
  { label: 'Users', value: analytics.userCount || 0 },
  { label: 'Docs', value: analytics.docCount || 0 },
  { label: 'Comments', value: analytics.commentCount || 0 },
  { label: 'Tasks', value: analytics.taskCount || 0 },
  { label: 'Notifications', value: analytics.notificationCount || 0 },
  { label: 'Active Logs', value: analytics.activeUsers || 0 }
])

const goBack = () => router.push('/docs')

const formatTime = (raw: string) => (raw ? new Date(raw).toLocaleString() : '')

onMounted(async () => {
  await loadUsers()
  await loadAnalytics()
  await loadSurvey()
  await loadLogs()
})
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin: 8px 0 16px;
}

.card {
  border: 1px solid #e5e7eb;
  padding: 10px;
  border-radius: 6px;
}

.value {
  font-size: 20px;
  font-weight: 600;
}
</style>
