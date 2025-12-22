<template>
  <el-drawer v-model="visible" title="Document Chat" size="30%" :with-header="true">
    <div>
      <div v-for="msg in messages" :key="msg.id" class="chat-bubble">
        <div class="text-muted">{{ msg.senderName || `User ${msg.senderId}` }} · {{ formatTime(msg.createdAt) }}</div>
        <div>{{ msg.content }}</div>
      </div>
    </div>
    <div style="margin-top: 12px">
      <el-input v-model="draft" type="textarea" rows="2" placeholder="Send a message" />
      <el-button class="gradient-button" type="primary" style="margin-top: 8px" @click="send">
        Send
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

interface ChatMessageItem {
  id?: number
  senderId: number
  senderName?: string
  content: string
  createdAt: string
}

const props = defineProps<{ modelValue: boolean; messages: ChatMessageItem[] }>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'send', content: string): void
}>()

const draft = ref('')
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const send = () => {
  if (!draft.value.trim()) return
  emit('send', draft.value.trim())
  draft.value = ''
}

const formatTime = (raw: string) => (raw ? new Date(raw).toLocaleString() : '')
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 12px;
  margin-bottom: 4px;
}
</style>
