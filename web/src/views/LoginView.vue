<template>
  <div class="app-shell" style="justify-content: center; align-items: center; padding: 40px">
    <div class="panel" style="max-width: 420px; width: 100%">
      <h2>Welcome Back</h2>
      <p class="text-muted">Sign in to continue collaborating.</p>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="Email or Phone">
          <el-input v-model="account" placeholder="you@example.com / 138****" />
        </el-form-item>
        <el-form-item label="Password">
          <el-input v-model="password" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button
            class="gradient-button"
            type="primary"
            style="width: 100%"
            :loading="loading"
            @click="submit"
          >
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
      <el-form-item label="Email or Phone">
        <el-input v-model="resetAccount" placeholder="you@example.com / 138****" />
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
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import http from '@/api/http'

const auth = useAuthStore()
const router = useRouter()

const account = ref('')
const password = ref('')
const loading = ref(false)

const resetDialog = ref(false)
const resetAccount = ref('')
const resetCode = ref('')
const confirmCode = ref('')
const newPassword = ref('')

const submit = async () => {
  if (!account.value.trim() || !password.value.trim()) {
    ElMessage.error('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(account.value.trim(), password.value)
    await auth.fetchMe()
    ElMessage.success('登录成功')
    router.push('/docs')
  } catch (err: any) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '服务器未响应，请稍后再试'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const requestReset = async () => {
  try {
    const { data } = await http.post('/api/auth/reset/request', { account: resetAccount.value })
    resetCode.value = data.data.code
    ElMessage.success('验证码已生成（演示环境直接显示）')
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '请求失败'
    ElMessage.error(msg)
  }
}

const confirmReset = async () => {
  try {
    await http.post('/api/auth/reset/confirm', {
      account: resetAccount.value,
      code: confirmCode.value,
      newPassword: newPassword.value
    })
    ElMessage.success('密码已重置，请使用新密码登录')
    resetDialog.value = false
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '重置失败'
    ElMessage.error(msg)
  }
}
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
