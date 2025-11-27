<script setup lang="ts">
interface Props {
  size?: number | string
}

withDefaults(defineProps<Props>(), {
  size: 64,
})
</script>

<template>
  <div class="loading-spinner">
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
