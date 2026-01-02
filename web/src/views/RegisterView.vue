<template>
  <div class="app-shell" style="justify-content: center; align-items: center; padding: 40px">
    <div class="panel" style="max-width: 440px; width: 100%">
      <h2>Create Account</h2>
      <p class="text-muted">Start collaborating with your team.</p>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="Nickname">
          <el-input v-model="nickname" placeholder="Your name" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="email" placeholder="you@example.com" />
        </el-form-item>
        <el-form-item label="Phone (optional)">
          <el-input v-model="phone" placeholder="138****" />
        </el-form-item>
        <el-form-item label="Password">
          <el-input v-model="password" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button class="gradient-button" type="primary" style="width: 100%" :loading="submitting" @click="submit">
            Register
          </el-button>
        </el-form-item>
        <div class="text-muted">
          <router-link to="/login">Already have an account?</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const phone = ref('')
const password = ref('')
const nickname = ref('')

const submitting = ref(false)

const submit = async () => {
  if (!email.value.trim() && !phone.value.trim()) {
    ElMessage.error('请输入邮箱或手机号')
    return
  }
  if (!password.value.trim() || !nickname.value.trim()) {
    ElMessage.error('请填写密码和昵称')
    return
  }
  submitting.value = true
  try {
    await auth.register(email.value.trim(), password.value, nickname.value.trim(), phone.value || undefined)
    await auth.fetchMe()
    ElMessage.success('注册成功')
    router.push('/docs')
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '注册失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
