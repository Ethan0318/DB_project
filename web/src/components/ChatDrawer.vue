<template>
  <el-drawer v-model="visible" title="Document Chat" size="30%" :with-header="true">
    <div>
      <div v-for="msg in messages" :key="msg.id" class="chat-bubble">
        <div class="text-muted">{{ msg.senderName || `User ${msg.senderId}` }} · {{ formatTime(msg.createdAt) }}</div>
        <div v-if="msg.fileUrl" class="file-row">
          <el-icon><Paperclip /></el-icon>
          <a :href="msg.fileUrl" target="_blank" rel="noreferrer">{{ msg.fileName || 'File' }}</a>
        </div>
        <div v-else>{{ msg.content }}</div>
      </div>
    </div>
    <div style="margin-top: 12px">
      <el-input v-model="draft" type="textarea" rows="2" placeholder="Send a message" />
      <div style="display: flex; gap: 8px; margin-top: 8px; align-items: center;">
        <input ref="fileInput" type="file" style="display:none" @change="handleFile" />
        <el-button @click="openFile">Attach</el-button>
        <el-button class="gradient-button" type="primary" @click="send">Send</el-button>
      </div>
      <div v-if="pendingFile" class="text-muted" style="margin-top: 4px;">待发送：{{ pendingFile?.name }}</div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Paperclip } from '@element-plus/icons-vue'

interface ChatMessageItem {
  id?: number
  senderId: number
  senderName?: string
  content: string
  createdAt: string
  fileUrl?: string
  fileName?: string
}

const props = defineProps<{ modelValue: boolean; messages: ChatMessageItem[] }>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'send', content: string, file?: File): void
}>()

const draft = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
const pendingFile = ref<File | null>(null)
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const send = () => {
  if (pendingFile.value) {
    emit('send', draft.value.trim(), pendingFile.value)
    draft.value = ''
    pendingFile.value = null
    if (fileInput.value) fileInput.value.value = ''
    return
  }
  if (!draft.value.trim()) return
  emit('send', draft.value.trim())
  draft.value = ''
}

const openFile = () => fileInput.value?.click()

const handleFile = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0] || null
  pendingFile.value = file
}

const formatTime = (raw: string) => (raw ? new Date(raw).toLocaleString() : '')
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 12px;
  margin-bottom: 4px;
}

.file-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
