<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { useNotifySocket } from '@/ws/notify'

const auth = useAuthStore()
const notifications = useNotificationStore()
const { connect } = useNotifySocket()

onMounted(async () => {
  auth.restore()
  if (auth.token) {
    await notifications.refresh()
    connect()
  }
})

watch(
  () => auth.token,
  async (token) => {
    if (token) {
      await notifications.refresh()
      connect()
    }
  }
)
</script>
