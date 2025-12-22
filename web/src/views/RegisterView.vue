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
        <el-form-item label="Password">
          <el-input v-model="password" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button class="gradient-button" type="primary" style="width: 100%" @click="submit">
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
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const nickname = ref('')

const submit = async () => {
  await auth.register(email.value, password.value, nickname.value)
  await auth.fetchMe()
  router.push('/docs')
}
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
