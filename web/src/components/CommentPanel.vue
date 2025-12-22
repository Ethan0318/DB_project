<template>
  <div>
    <div class="comment-card" v-if="anchorPreview">
      <div class="text-muted">Anchor</div>
      <div>{{ anchorPreview }}</div>
    </div>
    <el-form @submit.prevent>
      <el-form-item label="@ Mention">
        <el-select v-model="selectedAtUser" filterable clearable placeholder="Select user">
          <el-option v-for="user in mentionOptions" :key="user.id" :label="user.label" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="Comment">
        <el-input v-model="content" type="textarea" rows="3" placeholder="Write a comment" />
      </el-form-item>
      <el-form-item>
        <el-button class="gradient-button" type="primary" @click="submit">Send</el-button>
        <el-button text @click="resetReply" v-if="replyTo">Cancel reply</el-button>
      </el-form-item>
    </el-form>

    <div v-for="comment in comments" :key="comment.id" class="comment-card">
      <div class="text-muted">
        #{{ comment.id }} · User {{ comment.userId }} · {{ formatTime(comment.createdAt) }}
      </div>
      <div v-if="comment.parentId" class="text-muted">Reply to #{{ comment.parentId }}</div>
      <div>{{ comment.content }}</div>
      <div v-if="comment.anchor" class="text-muted">Anchor: {{ parseAnchor(comment.anchor) }}</div>
      <el-button text size="small" @click="reply(comment.id)">Reply</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import http from '@/api/http'

interface CommentItem {
  id: number
  userId: number
  content: string
  anchor?: string
  parentId?: number
  createdAt: string
}

interface MentionOption {
  id: number
  label: string
}

const props = defineProps<{ comments: CommentItem[]; anchorPreview?: string }>()
const emit = defineEmits<{
  (e: 'submit', payload: { content: string; parentId?: number; atUserId?: number }): void
}>()

const content = ref('')
const replyTo = ref<number | null>(null)
const mentionOptions = ref<MentionOption[]>([])
const selectedAtUser = ref<number | undefined>()

const fetchUsers = async () => {
  const { data } = await http.get('/api/users/search')
  mentionOptions.value = (data.data || []).map((user: any) => ({
    id: user.id,
    label: user.nickname ? `${user.nickname} (${user.email})` : user.email
  }))
}

fetchUsers()

const submit = () => {
  if (!content.value.trim()) return
  emit('submit', {
    content: content.value.trim(),
    parentId: replyTo.value || undefined,
    atUserId: selectedAtUser.value
  })
  content.value = ''
  replyTo.value = null
  selectedAtUser.value = undefined
}

const reply = (id: number) => {
  replyTo.value = id
}

const resetReply = () => {
  replyTo.value = null
}

const formatTime = (raw: string) => {
  return raw ? new Date(raw).toLocaleString() : ''
}

const parseAnchor = (raw?: string) => {
  if (!raw) return ''
  try {
    const anchor = JSON.parse(raw)
    return anchor.text || `${anchor.from}-${anchor.to}`
  } catch {
    return raw
  }
}
</script>

<style scoped>
.text-muted {
  color: var(--text-muted);
  font-size: 12px;
  margin-bottom: 4px;
}
</style>
