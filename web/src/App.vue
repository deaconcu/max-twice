<template>
  <v-app>
    <router-view v-slot="{ Component }">
      <keep-alive :include="cachedComponents">
        <component :is="Component" />
      </keep-alive>
    </router-view>

    <!-- 全局 Snackbar 提示 - 只显示队列中的最后一个（最新的） -->
    <v-snackbar
      v-if="currentSnackbar"
      :model-value="true"
      :timeout="2500"
      location="top"
      rounded="xl"
      variant="flat"
      border
      transition="slide-y-transition"
      :class="`snackbar-${currentSnackbar.type}`"
      @update:model-value="onSnackbarClose"
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

<script setup lang="ts">
import { provide, ref, onMounted, computed } from 'vue'
import type { Ref } from 'vue'
import { setGlobalSnackbar } from '@/composables/utils'
import { useRouter } from 'vue-router'

interface Snackbar {
  text: string
  visible: boolean
  type: string
}

const snackbars: Ref<Snackbar[]> = ref([])
const router = useRouter()

// 当前显示的 snackbar（队列中的最后一个，即最新的）
const currentSnackbar = computed(() => snackbars.value[snackbars.value.length - 1] || null)

// Keep-alive 缓存管理
const CACHE_TIMEOUT = 12 * 60 * 60 * 1000 // 12小时（毫秒）
const cachedComponents = ref<string[]>([
  'ProfilePage',
  'ReadPageRouter',
  'CareerListPage',
  'CourseListPage',
])
const cacheTimestamps = new Map<string, number>()

// 监听路由变化，更新缓存时间戳
router.afterEach((to) => {
  const componentName = to.matched[0]?.components?.default?.name
  if (componentName && cachedComponents.value.includes(componentName)) {
    cacheTimestamps.set(componentName, Date.now())
  }
})

// 定期检查并清理过期缓存（每5分钟检查一次）
const checkExpiredCache = () => {
  const now = Date.now()
  const expiredComponents: string[] = []

  cacheTimestamps.forEach((timestamp, componentName) => {
    if (now - timestamp > CACHE_TIMEOUT) {
      expiredComponents.push(componentName)
    }
  })

  if (expiredComponents.length > 0) {
    // 从缓存列表中移除过期组件
    cachedComponents.value = cachedComponents.value.filter(
      (name) => !expiredComponents.includes(name)
    )
    // 清理时间戳记录
    expiredComponents.forEach((name) => cacheTimestamps.delete(name))

    console.log(`[Keep-alive] 清理过期缓存: ${expiredComponents.join(', ')}`)

    // 下次访问时重新添加到缓存
    setTimeout(() => {
      expiredComponents.forEach((name) => {
        if (!cachedComponents.value.includes(name)) {
          cachedComponents.value.push(name)
        }
      })
    }, 100)
  }
}

// 启动定期检查
let cleanupInterval: number | null = null
onMounted(() => {
  cleanupInterval = window.setInterval(checkExpiredCache, 5 * 60 * 1000) // 每5分钟检查

  // 初始化所有组件的时间戳
  cachedComponents.value.forEach((name) => {
    cacheTimestamps.set(name, Date.now())
  })
})

// 清理定时器
if (import.meta.hot) {
  import.meta.hot.dispose(() => {
    if (cleanupInterval) {
      clearInterval(cleanupInterval)
    }
  })
}

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

/**
 * 处理 snackbar 关闭事件（由 v-snackbar 的 timeout 或用户手动关闭触发）
 */
const onSnackbarClose = (visible: boolean) => {
  if (!visible && snackbars.value.length > 0) {
    // 移除队列中的最后一个（当前显示的）
    snackbars.value.pop()
  }
}

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
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid rgb(var(--v-theme-grey-lighten-2)) !important;
}
</style>
