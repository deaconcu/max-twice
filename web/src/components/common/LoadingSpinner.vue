<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

interface Props {
  size?: number | string
  delay?: number // 延迟显示时间（毫秒）
}

const props = withDefaults(defineProps<Props>(), {
  size: 40,
  delay: 300, // 默认延迟 300ms
})

// 控制是否显示 loading
const visible = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

onMounted(() => {
  // 延迟显示，防止闪现
  timer = setTimeout(() => {
    visible.value = true
  }, props.delay)
})

onBeforeUnmount(() => {
  // 清理定时器
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
})
</script>

<template>
  <div v-if="visible" class="loading-spinner">
    <v-progress-circular indeterminate color="primary" :size="size" />
  </div>
</template>

<style scoped>
.loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}

/* 移动端调整 */
@media (max-width: 600px) {
  .loading-spinner {
    padding: 32px 16px;
    min-height: 150px;
  }
}
</style>
