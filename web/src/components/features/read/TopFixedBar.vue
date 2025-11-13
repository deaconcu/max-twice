<template>
  <div class="top-fixed-bar">
    <v-btn icon="mdi-arrow-left" size="small" @click="$emit('go-back')" />
    <span class="course-name">{{ courseName }}</span>
    <v-btn
      :color="isLearning ? 'success' : 'primary'"
      size="small"
      @click="$emit('toggle-learning')"
    >
      {{ isLearning ? '学习中' : '开始学习' }}
    </v-btn>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  courseName: string
  isLearning: boolean
}>()

defineEmits<{
  'toggle-learning': []
  'go-back': []
}>()
</script>

<style scoped>
.top-fixed-bar {
  position: fixed;
  top: 64px;
  left: 0;
  right: 0;
  height: 48px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  z-index: 100;
}
</style>
