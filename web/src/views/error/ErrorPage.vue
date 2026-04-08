<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import AppHeader from '@/components/layout/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const code = computed(() => route.params.code as string)
// 优先从 history state 读取 message，这样不会显示在 URL 中
const customMessage = computed(() => {
  const stateMessage = history.state?.message
  const queryMessage = route.query.message as string | undefined
  return stateMessage || queryMessage
})

const errorConfig = computed(() => {
  switch (code.value) {
    case '403':
      return {
        icon: 'mdi-lock-outline',
        color: 'error',
        title: t('error.403.title'),
        message: customMessage.value || t('error.403.message'),
      }
    case '404':
      return {
        icon: 'mdi-file-question-outline',
        color: 'warning',
        title: t('error.404.title'),
        message: customMessage.value || t('error.404.message'),
      }
    case '500':
      return {
        icon: 'mdi-alert-circle-outline',
        color: 'error',
        title: t('error.500.title'),
        message: customMessage.value || t('error.500.message'),
      }
    case '1309':
      return {
        icon: 'mdi-cancel',
        color: 'warning',
        title: t('error.1309.title'),
        message: customMessage.value || t('error.1309.message'),
      }
    default:
      return {
        icon: 'mdi-alert-outline',
        color: 'grey',
        title: t('error.default.title'),
        message: customMessage.value || t('error.default.message'),
      }
  }
})

const goHome = () => {
  void router.push('/')
}

const goBack = () => {
  // 获取历史记录长度
  const historyLength = window.history.length

  // 如果历史记录少于等于2（当前页+入口页），或者是从错误页面跳转过来的，直接回首页
  if (historyLength <= 2) {
    goHome()
  } else {
    // 回退2步，跳过出错的页面
    router.go(-2)
  }
}
</script>

<template>
  <div class="error-page">
    <AppHeader />

    <div class="error-container">
      <div class="error-content">
        <!-- 错误图标 -->
        <v-icon
          :icon="errorConfig.icon"
          size="120"
          :color="errorConfig.color"
          class="mb-6"
        ></v-icon>

        <!-- 错误码 -->
        <div class="error-code mb-4" :class="`text-${errorConfig.color}`">
          {{ code }}
        </div>

        <!-- 错误标题 -->
        <h1 class="error-title mb-3">
          {{ errorConfig.title }}
        </h1>

        <!-- 错误描述 -->
        <p class="error-message mb-8">
          {{ errorConfig.message }}
        </p>

        <!-- 操作按钮 -->
        <div class="error-actions">
          <v-btn variant="outlined" prepend-icon="mdi-arrow-left" @click="goBack">
            {{ t('common.goBack') }}
          </v-btn>
          <v-btn color="primary" variant="flat" prepend-icon="mdi-home" @click="goHome">
            {{ t('common.goHome') }}
          </v-btn>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  background-color: rgb(var(--v-theme-background));
}

.error-container {
  padding: 200px 40px 200px 40px;
  text-align: center;
}

.error-content {
  max-width: 600px;
  margin: 0 auto;
}

.error-code {
  font-size: 72px;
  font-weight: 700;
  line-height: 1;
}

.error-title {
  font-size: 24px;
  font-weight: 600;
  color: rgb(var(--v-theme-on-background));
}

.error-message {
  font-size: 15px;
  color: rgb(var(--v-theme-on-surface-variant));
  line-height: 1.6;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}
</style>
