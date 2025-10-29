<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import type { Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/common/PageHeader.vue'
import PageFooter from '@/components/common/PageFooter.vue'
import ErrorPage from '@/components/common/ErrorPage.vue'

const route = useRoute()
const router = useRouter()
const scrollY: Ref<number> = ref(0)

const updateScroll = (): void => {
  scrollY.value = window.scrollY
}

onMounted(() => {
  window.addEventListener('scroll', updateScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', updateScroll)
})

// 从路由参数获取错误类型和自定义消息
const errorType = computed(() => route.query.type as string || '404')
const customMessage = computed(() => route.query.message as string)

// 计算错误码
const errorCode = computed(() => {
  const type = errorType.value
  // 支持数字错误码（如 1201, 1208）和标准 HTTP 错误码
  if (!isNaN(Number(type))) {
    return Number(type)
  }
  // 默认返回 404
  return 404
})

// 计算错误消息
const errorMessage = computed(() => {
  // 优先使用自定义消息
  if (customMessage.value) {
    return customMessage.value
  }

  // 根据错误码返回默认消息
  switch (errorCode.value) {
    case 401:
      return '请先登录后再访问'
    case 403:
      return '抱歉，您没有权限访问此页面'
    case 404:
      return '抱歉，您访问的页面不存在'
    case 500:
      return '服务器内部错误，请稍后重试'
    case 1201:
      return '课程不存在或已被删除'
    case 1208:
      return '该课程已被屏蔽，无法访问'
    default:
      return '发生了一些错误'
  }
})

// 是否显示重试按钮（某些错误类型不需要重试按钮）
const showRetry = computed(() => {
  return ![401, 403, 404].includes(errorCode.value)
})

// 重试操作
const handleRetry = () => {
  // 刷新当前页面或返回上一页
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

// 返回首页
const handleBackHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="page-container">
    <!-- 顶部标题栏 -->
    <div class="header-container" :class="{ compact: scrollY > 20 }">
      <PageHeader />
    </div>

    <!-- 错误内容 -->
    <div class="content main-content-size">
      <ErrorPage
        :error-code="errorCode"
        :error-message="errorMessage"
        :show-retry="showRetry"
        :show-back-home="true"
        @retry="handleRetry"
        @back-home="handleBackHome"
      />
    </div>

    <!-- 底部 -->
    <PageFooter />
  </div>
</template>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  min-height: 97vh;
}

.header-container {
  position: sticky;
  top: 0;
  z-index: 1000;
  background-color: white;
  padding: 16px 0;
  transition: all 0.3s ease;
}

.header-container::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100vw;
  height: 1px;
  background-color: #eef;
}

.header-container.compact {
  padding: 8px 0;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.main-content-size {
  width: 1600px;
  height: 1800px;
}
</style>
