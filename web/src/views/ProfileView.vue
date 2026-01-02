<template>
  <div class="app-shell" style="justify-content: center; align-items: center;">
    <div class="panel" style="max-width: 640px; width: 100%;">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2>Your Profile</h2>
          <div class="text-muted">Update contact info and avatar.</div>
        </div>
        <el-button text @click="goBack">Back</el-button>
      </div>
      <div style="display: flex; gap: 16px; align-items: center; margin: 16px 0;">
        <el-avatar :size="80" :src="avatarSrc">
          {{ profile.nickname?.slice(0, 1) || 'U' }}
        </el-avatar>
        <div>
          <el-upload
            action="/"
            :show-file-list="false"
            :http-request="handleAvatarUpload"
            accept="image/*"
          >
            <el-button>Upload Avatar</el-button>
          </el-upload>
        </div>
      </div>
      <el-form label-position="top">
        <el-form-item label="Nickname">
          <el-input v-model="profile.nickname" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="profile.email" placeholder="you@example.com" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="profile.phone" placeholder="138****" />
        </el-form-item>
        <el-form-item label="Bio">
          <el-input v-model="profile.bio" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button class="gradient-button" type="primary" @click="save">Save</el-button>
          <el-button text @click="goBack">Back</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const profile = reactive<any>({
  nickname: '',
  email: '',
  phone: '',
  bio: '',
  avatarUrl: ''
})
const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const avatarSrc = computed(() =>
  profile.avatarUrl && profile.avatarUrl.startsWith('/uploads')
    ? `${apiBase}${profile.avatarUrl}`
    : profile.avatarUrl
)

const load = async () => {
  const { data } = await http.get('/api/users/me')
  Object.assign(profile, data.data || {})
}

const save = async () => {
  const payload: any = {}
  if (profile.nickname !== undefined) payload.nickname = profile.nickname
  if (profile.email && profile.email.trim()) payload.email = profile.email.trim()
  if (profile.phone && profile.phone.trim()) payload.phone = profile.phone.trim()
  if (profile.bio !== undefined) payload.bio = profile.bio
  await http.put('/api/users/me', payload)
  await auth.fetchMe()
}

const handleAvatarUpload = async (options: any) => {
  const formData = new FormData()
  formData.append('file', options.file)
  const { data } = await http.post('/api/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  profile.avatarUrl = data.data.avatarUrl
  await auth.fetchMe()
  options.onSuccess?.(data, options.file)
}

const goBack = () => {
  router.push('/docs')
}

onMounted(async () => {
  await load()
})
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
