<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'

interface Props {
  siteKey?: string
  theme?: 'light' | 'dark' | 'auto'
  size?: 'normal' | 'compact'
  appearance?: 'always' | 'execute' | 'interaction-only'
}

const props = withDefaults(defineProps<Props>(), {
  // 测试用 key（始终通过）
  siteKey: '1x00000000000000000000AA',
  theme: 'auto',
  size: 'normal',
  appearance: 'interaction-only',
})

const emit = defineEmits<{
  (e: 'verify', token: string): void
  (e: 'error'): void
  (e: 'expire'): void
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const widgetId = ref<string | null>(null)
const isLoaded = ref(false)

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
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Failed to load Turnstile script'))
    document.head.appendChild(script)
  })
}

// 渲染 widget
const renderWidget = () => {
  if (!containerRef.value || !window.turnstile) return

  // 如果已经有 widget，先移除
  if (widgetId.value) {
    window.turnstile.remove(widgetId.value)
  }

  widgetId.value = window.turnstile.render(containerRef.value, {
    sitekey: props.siteKey,
    theme: props.theme,
    size: props.size,
    appearance: props.appearance,
    callback: (token: string) => {
      emit('verify', token)
    },
    'error-callback': () => {
      emit('error')
    },
    'expired-callback': () => {
      emit('expire')
    },
  })
}

// 重置 widget
const reset = () => {
  if (widgetId.value && window.turnstile) {
    window.turnstile.reset(widgetId.value)
  }
}

// 暴露 reset 方法给父组件
defineExpose({ reset })

onMounted(async () => {
  try {
    await loadScript()
    isLoaded.value = true
    renderWidget()
  } catch (error) {
    console.error('Turnstile load error:', error)
    emit('error')
  }
})

onUnmounted(() => {
  if (widgetId.value && window.turnstile) {
    window.turnstile.remove(widgetId.value)
  }
})

// 监听 siteKey 变化，重新渲染
watch(() => props.siteKey, () => {
  if (isLoaded.value) {
    renderWidget()
  }
})
</script>

<template>
  <div ref="containerRef" class="turnstile-container" />
</template>

<style scoped>
.turnstile-container {
  display: flex;
  justify-content: center;
}
</style>
