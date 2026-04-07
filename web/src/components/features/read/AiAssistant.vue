<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from '@/composables/useI18n'

const { t } = useI18n()

interface Props {
  selectedText?: string
  nodeTitle?: string
  nodeDescription?: string
}

interface Emits {
  (e: 'update:selectedText', value: string): void
}

const props = withDefaults(defineProps<Props>(), {
  selectedText: '',
  nodeTitle: '',
  nodeDescription: '',
})

const emit = defineEmits<Emits>()

// 状态
const userQuestion = ref('')
const isExpanded = ref(true)
const isQuoteExpanded = ref(false)
const copySuccess = ref(false)

// 是否需要展开按钮（超过约2行）
const needExpand = computed(() => props.selectedText.length > 60)

// AI 引擎列表
const aiEngines = [
  { name: 'ChatGPT', href: 'https://chatgpt.com', icon: 'mdi-robot' },
  { name: 'Claude', href: 'https://claude.ai', icon: 'mdi-alpha-c-circle-outline' },
  { name: 'Gemini', href: 'https://gemini.google.com', icon: 'mdi-google' },
  { name: 'DeepSeek', href: 'https://chat.deepseek.com', icon: 'mdi-radar' },
]

// 构建复制内容
const buildContent = () => {
  let content = ''
  if (props.nodeTitle) {
    content += `【${t('aiAssistant.topic')}】${props.nodeTitle}\n`
    if (props.nodeDescription) {
      content += `${props.nodeDescription}\n`
    }
    content += '\n'
  }
  if (props.selectedText) {
    content += `【${t('aiAssistant.quotedContent')}】\n${props.selectedText}\n\n`
  }
  if (userQuestion.value) {
    content += `【${t('aiAssistant.myQuestion')}】\n${userQuestion.value}`
  }
  return content.trim()
}

// 复制并跳转到AI引擎
const copyAndGo = async (engine: { name: string; href: string; icon: string }) => {
  const content = buildContent()

  if (content) {
    try {
      await navigator.clipboard.writeText(content)
    } catch (err) {
      console.error('复制失败:', err)
    }
  }

  // 跳转到AI引擎
  window.open(engine.href, '_blank', 'noopener')
}

// 仅复制到剪贴板
const copyOnly = async () => {
  const content = buildContent()

  if (content) {
    try {
      await navigator.clipboard.writeText(content)
      copySuccess.value = true
      setTimeout(() => {
        copySuccess.value = false
      }, 2000)
    } catch (err) {
      console.error('复制失败:', err)
    }
  }
}

// 清除引用
const clearQuote = () => {
  emit('update:selectedText', '')
}
</script>

<template>
  <div class="ai-assistant-section">
    <div class="sidebar-header">
      <v-icon icon="mdi-chat-question-outline" size="18" class="mr-2"></v-icon>
      <span class="sidebar-title">{{ t('aiAssistant.title') }}</span>
      <v-spacer></v-spacer>
      <v-btn
        :icon="isExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down'"
        variant="text"
        color="grey-darken-1"
        size="x-small"
        @click="isExpanded = !isExpanded"
      ></v-btn>
    </div>

    <v-expand-transition>
      <div v-show="isExpanded" class="assistant-content">
        <!-- 选中的文本预览 -->
        <div v-if="selectedText" class="selected-text-preview mb-3">
          <div
            class="d-flex align-center justify-space-between"
            :class="{ 'cursor-pointer': needExpand }"
            @click="needExpand && (isQuoteExpanded = !isQuoteExpanded)"
          >
            <div class="d-flex align-center text-caption text-grey-darken-1">
              <v-icon icon="mdi-format-quote-close" size="14" class="mr-1"></v-icon>
              <span>{{ t('aiAssistant.quoted') }}</span>
              <span class="text-grey ml-1">({{ t('aiAssistant.charCount', { count: selectedText.length }) }})</span>
            </div>
            <div class="d-flex align-center">
              <v-btn
                icon="mdi-close"
                variant="text"
                size="x-small"
                color="grey"
                @click.stop="clearQuote"
              ></v-btn>
              <v-icon
                v-if="needExpand"
                :icon="isQuoteExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down'"
                size="16"
                color="grey"
              ></v-icon>
            </div>
          </div>
          <!-- 引用内容 -->
          <div
            class="selected-text-content text-body-2 mt-0"
            :class="{ 'selected-text-collapsed': needExpand && !isQuoteExpanded }"
          >
            {{ selectedText }}
          </div>
        </div>

        <!-- 未选中时的提示 -->
        <div v-else class="d-flex align-center text-body-2 text-grey-darken-2 mb-3">
          <v-icon icon="mdi-cursor-text" size="16" class="mr-1"></v-icon>
          {{ t('aiAssistant.selectHint') }}
        </div>

        <!-- 问题输入框 -->
        <v-textarea
          v-model="userQuestion"
          :label="t('aiAssistant.inputQuestion')"
          :placeholder="t('aiAssistant.defaultQuestion')"
          variant="outlined"
          density="compact"
          rows="2"
          hide-details
          class="mb-3 question-input"
          @focus="($event.target as HTMLTextAreaElement)?.select()"
        ></v-textarea>

        <!-- AI 引擎链接 - 点击复制并跳转 -->
        <div class="d-flex flex-wrap align-center text-caption" style="gap: 8px">
          <a
            href="#"
            class="link"
            :class="{ 'text-success': copySuccess }"
            @click.prevent="copyOnly"
          >
            {{ copySuccess ? t('common.copied') : t('aiAssistant.copyContent') }}
          </a>
          <a
            v-for="e in aiEngines"
            :key="e.name"
            href="#"
            class="link"
            @click.prevent="copyAndGo(e)"
          >
            {{ e.name }}
          </a>
        </div>
      </div>
    </v-expand-transition>
  </div>
</template>

<style scoped>
.ai-assistant-section {
  padding-bottom: 24px;
  border-bottom: 1px solid rgb(var(--v-theme-border));
  margin-bottom: 24px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  padding-bottom: 12px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.assistant-content {
  padding-top: 8px;
}

.selected-text-preview {
  background-color: #f5f5f5;
  border-radius: 8px;
  padding: 8px 12px 12px 12px;
}

.selected-text-content {
  color: #616161;
  line-height: 1.6;
  max-height: 150px;
  overflow-y: auto;
  transition: max-height 0.3s ease;
}

.selected-text-content.selected-text-collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-height: 48px;
}

.question-input :deep(textarea) {
  font-size: 14px;
}
</style>
