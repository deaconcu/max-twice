<template>
  <div class="breadcrumb-nav">
    <v-btn icon="mdi-arrow-left" size="small" @click="$emit('go-back')" />
    <span>{{ courseName }} / {{ pathText }}</span>
    <v-spacer />
    <v-btn :color="isLearning ? 'success' : 'primary'" @click="$emit('toggle-learning')">
      {{ isLearning ? '学习中' : '开始学习' }}
    </v-btn>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  courseName: string
  pathText: string
  isLearning: boolean
}>()

defineEmits<{
  'toggle-learning': []
  'go-back': []
}>()
</script>

<style scoped>
.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px;
  background: white;
  border-radius: 8px;
}
</style>
