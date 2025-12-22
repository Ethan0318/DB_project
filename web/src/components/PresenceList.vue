<template>
  <div class="presence-list">
    <div v-for="member in members" :key="member.userId" class="presence-chip">
      <el-avatar :size="24" :src="member.avatarUrl">
        {{ member.nickname?.slice(0, 1) || 'U' }}
      </el-avatar>
      <span>{{ member.nickname || `User${member.userId}` }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Member {
  userId: number
  nickname?: string
  avatarUrl?: string
}

defineProps<{ members: Member[] }>()
</script>
