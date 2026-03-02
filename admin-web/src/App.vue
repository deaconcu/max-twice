<script setup lang="ts">
import { RouterView } from 'vue-router'
import { ref, computed, watch, onMounted, provide } from 'vue'
import type { Ref } from 'vue'
import { setGlobalSnackbar } from '@/composables/utils'

interface Snackbar {
  text: string
  visible: boolean
  type: string
}

const snackbars: Ref<Snackbar[]> = ref([])

// 当前显示的 snackbar（队列中的最后一个，即最新的）
const currentSnackbar = computed(() => snackbars.value[snackbars.value.length - 1] || null)

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
}

// 提供给子组件使用
provide('showSnackbar', showSnackbar)

/**
 * 监听当前 snackbar 的 visible 变化，自动清理队列
 */
watch(
  () => currentSnackbar.value?.visible,
  (newVisible) => {
    if (newVisible === false && snackbars.value.length > 0) {
      // visible 变为 false 时，延迟移除消息（等动画结束）
      setTimeout(() => {
        if (snackbars.value.length > 0) {
          snackbars.value.pop()
        }
      }, 300)
    }
  }
)

/**
 * 根据类型获取图标背景色
 */
const getSnackbarIconBg = (type: string): string => {
  switch (type) {
    case 'success':
      return 'success'
    case 'error':
      return 'error'
    case 'warning':
      return 'warning'
    case 'info':
    default:
      return 'primary'
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

// 设置全局 snackbar 给 utils 使用
onMounted(() => {
  setGlobalSnackbar(showSnackbar)
})
</script>

<template>
  <v-app>
    <router-view />

    <!-- 全局 Snackbar 提示 - 只显示队列中的最后一个（最新的） -->
    <v-snackbar
      v-if="currentSnackbar"
      v-model="currentSnackbar.visible"
      :timeout="2500"
      location="top"
      rounded="xl"
      variant="flat"
      border
      transition="slide-y-transition"
      :class="`snackbar-${currentSnackbar.type}`"
      style="z-index: 9999"
    >
      <div class="d-flex align-center pa-1">
        <v-avatar :color="getSnackbarIconBg(currentSnackbar.type)" size="32" class="mr-3">
          <v-icon :icon="getSnackbarIcon(currentSnackbar.type)" size="18" color="white"></v-icon>
        </v-avatar>
        <span class="text-body-1 font-weight-medium">{{ currentSnackbar.text }}</span>
      </div>
    </v-snackbar>
  </v-app>
</template>

<style scoped>
/* Admin全局样式 */
</style>
