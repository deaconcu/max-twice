<template>
  <v-app>
    <router-view />

    <!-- 全局 Snackbar 提示 -->
    <v-snackbar
      v-for="(item, index) in snackbars"
      :key="index"
      v-model="item.visible"
      :color="getSnackbarColor(item.type)"
      :timeout="4000"
      location="top"
      rounded="lg"
      variant="tonal"
    >
      <div class="d-flex align-center">
        <v-icon :icon="getSnackbarIcon(item.type)" class="mr-3"></v-icon>
        <span>{{ item.text }}</span>
      </div>
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { provide, ref, onMounted } from 'vue'
import type { Ref } from 'vue'
import { setGlobalSnackbar } from '@/composables/utils'

interface Snackbar {
  text: string
  visible: boolean
  type: string
}

const snackbars: Ref<Snackbar[]> = ref([])

/**
 * 显示 Snackbar 提示
 */
const showSnackbar = (message: string, type = 'info'): void => {
  const newSnackbar: Snackbar = {
    text: message,
    visible: true,
    type,
  }
  snackbars.value.push(newSnackbar)

  setTimeout(() => {
    snackbars.value = snackbars.value.filter((snack) => snack !== newSnackbar)
  }, 4000)
}

/**
 * 根据类型获取颜色
 */
const getSnackbarColor = (type: string): string => {
  switch (type) {
    case 'success':
      return 'success'
    case 'error':
      return 'error'
    case 'warning':
      return 'warning'
    case 'info':
    default:
      return 'info'
  }
}

/**
 * 根据类型获取图标
 */
const getSnackbarIcon = (type: string): string => {
  switch (type) {
    case 'success':
      return 'mdi-check-circle'
    case 'error':
      return 'mdi-alert-circle'
    case 'warning':
      return 'mdi-alert'
    case 'info':
    default:
      return 'mdi-information'
  }
}

// 提供给子组件使用
provide('showSnackbar', showSnackbar)

// 设置全局 snackbar 给 utils 使用
onMounted(() => {
  setGlobalSnackbar(showSnackbar)
})
</script>

<style>
/* 全局重置 - 只重置 body，避免影响 Vuetify 组件 */
body {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: rgb(var(--v-theme-background));
  color: rgb(var(--v-theme-on-background));
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

/* 简约风格全局样式 */
.v-card {
  border: 1px solid rgb(var(--v-theme-border)) !important;
  background-color: rgb(var(--v-theme-surface)) !important;
}

.v-card.no-border {
  border: none !important;
}

/* Vuetify 按钮文本不转换大写 */
.v-btn {
  text-transform: none !important;
  letter-spacing: normal !important;
}

/* 自定义 Vuetify 圆角大小 */
.rounded-lg {
  border-radius: 14px !important;
}

.rounded-md {
  border-radius: 6px !important;
}

/* Tooltip 样式修复 */
.v-tooltip .v-overlay__content {
  background-color: rgba(var(--v-theme-on-surface), 0.9) !important;
  color: rgb(var(--v-theme-surface)) !important;
  padding: 4px 8px !important;
  font-size: 12px !important;
}

/* 统一的卡片 hover 效果 */
.v-card.hoverable {
  transition: all 0.3s ease;
  cursor: pointer;
}

.v-card.hoverable:hover {
  transform: translateY(-4px);
  background-color: rgb(var(--v-theme-surface-variant)) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
</style>
