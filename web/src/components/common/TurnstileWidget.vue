<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'

interface Props {
  siteKey?: string
  theme?: 'light' | 'dark' | 'auto'
  size?: 'normal' | 'compact'
  appearance?: 'always' | 'execute' | 'interaction-only'
  retry?: 'auto' | 'never'
}

const props = withDefaults(defineProps<Props>(), {
  // 从环境变量读取，默认使用测试 key（始终通过）
  siteKey: import.meta.env.VITE_TURNSTILE_SITE_KEY || '1x00000000000000000000AA',
  theme: 'auto',
  size: 'normal',
  appearance: 'always',
  retry: 'auto',
})

const emit = defineEmits<{
  (e: 'verify', token: string): void
  (e: 'error'): void
  (e: 'expire'): void
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const widgetId = ref<string | null>(null)
const isLoading = ref(true)
let checkInterval: ReturnType<typeof setInterval> | null = null

// 加载 Turnstile 脚本
const loadScript = (): Promise<void> => {
  return new Promise((resolve, reject) => {
    if (window.turnstile) {
      resolve()
      return
    }

    const script = document.createElement('script')
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'
    script.async = true
    script.defer = true
    script.onload = () => {
      resolve()
    }
    script.onerror = () => {
      reject(new Error('Failed to load Turnstile script'))
    }
    document.head.appendChild(script)
  })
}

// 检查 iframe 是否出现
const checkIframeLoaded = () => {
  if (checkInterval) {
    clearInterval(checkInterval)
  }

  let attempts = 0
  checkInterval = setInterval(() => {
    attempts++
    const iframe = containerRef.value?.querySelector('iframe')
    if (iframe) {
      isLoading.value = false
      clearInterval(checkInterval!)
      checkInterval = null
    } else if (attempts > 50) {
      // 5秒超时
      isLoading.value = false
      clearInterval(checkInterval!)
      checkInterval = null
    }
  }, 100)
}

// 渲染 widget
const renderWidget = () => {
  if (!containerRef.value || !window.turnstile) return

  // 如果已经有 widget，先移除
  if (widgetId.value) {
    window.turnstile.remove(widgetId.value)
  }

  isLoading.value = true

  widgetId.value = window.turnstile.render(containerRef.value, {
    sitekey: props.siteKey,
    theme: props.theme,
    size: props.size,
    appearance: props.appearance,
    retry: props.retry,
    callback: (token: string) => {
      isLoading.value = false
      emit('verify', token)
    },
    'error-callback': () => {
      isLoading.value = false
      emit('error')
    },
    'expired-callback': () => {
      emit('expire')
    },
  })

  // 开始检查 iframe
  checkIframeLoaded()
}

// 重置 widget
const reset = () => {
  if (widgetId.value && window.turnstile) {
    isLoading.value = true
    window.turnstile.reset(widgetId.value)
    checkIframeLoaded()
  }
}

// 暴露 reset 方法给父组件
defineExpose({ reset })

onMounted(async () => {
  try {
    await loadScript()
    renderWidget()
  } catch (error) {
    console.error('Turnstile load error:', error)
    isLoading.value = false
    emit('error')
  }
})

onUnmounted(() => {
  if (widgetId.value && window.turnstile) {
    window.turnstile.remove(widgetId.value)
  }
  if (checkInterval) {
    clearInterval(checkInterval)
  }
})

// 监听 siteKey 变化，重新渲染
watch(
  () => props.siteKey,
  () => {
    renderWidget()
  }
)
</script>

<template>
  <div class="turnstile-wrapper">
    <div v-if="isLoading" class="turnstile-loading">
      <div class="loading-spinner"></div>
      <span class="loading-text">加载验证中...</span>
    </div>
    <div ref="containerRef" class="turnstile-container" :style="{ visibility: isLoading ? 'hidden' : 'visible', position: isLoading ? 'absolute' : 'static' }" />
  </div>
</template>

<style scoped>
.turnstile-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 65px;
}

.turnstile-container {
  display: flex;
  justify-content: center;
}

.turnstile-loading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e0e0e0;
  border-top-color: #666;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  color: #999;
  font-size: 14px;
}
</style>
