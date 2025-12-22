<template>
  <div class="app-shell" style="justify-content: center; align-items: center; padding: 40px">
    <div class="panel" style="max-width: 420px; width: 100%">
      <h2>Welcome Back</h2>
      <p class="text-muted">Sign in to continue collaborating.</p>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="Email">
          <el-input v-model="email" placeholder="you@example.com" />
        </el-form-item>
        <el-form-item label="Password">
          <el-input v-model="password" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button class="gradient-button" type="primary" style="width: 100%" @click="submit">
            Login
          </el-button>
        </el-form-item>
        <div class="text-muted" style="display: flex; justify-content: space-between">
          <router-link to="/register">Create account</router-link>
          <el-button text @click="resetDialog = true">Forgot password?</el-button>
        </div>
      </el-form>
    </div>
  </div>

  <el-dialog v-model="resetDialog" title="Reset Password" width="400px">
    <el-form label-position="top">
      <el-form-item label="Email">
        <el-input v-model="resetEmail" placeholder="you@example.com" />
      </el-form-item>
      <el-form-item>
        <el-button @click="requestReset">Request Code</el-button>
      </el-form-item>
      <div v-if="resetCode" class="text-muted">Demo code: {{ resetCode }}</div>
      <el-form-item label="Code">
        <el-input v-model="confirmCode" placeholder="6-digit code" />
      </el-form-item>
      <el-form-item label="New Password">
        <el-input v-model="newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item>
        <el-button class="gradient-button" type="primary" @click="confirmReset">Reset</el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import http from '@/api/http'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const password = ref('')

const resetDialog = ref(false)
const resetEmail = ref('')
const resetCode = ref('')
const confirmCode = ref('')
const newPassword = ref('')

const submit = async () => {
  await auth.login(email.value, password.value)
  await auth.fetchMe()
  router.push('/docs')
}

const requestReset = async () => {
  const { data } = await http.post('/api/auth/reset/request', { email: resetEmail.value })
  resetCode.value = data.data.code
}

const confirmReset = async () => {
  await http.post('/api/auth/reset/confirm', {
    email: resetEmail.value,
    code: confirmCode.value,
    newPassword: newPassword.value
  })
  resetDialog.value = false
}
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
