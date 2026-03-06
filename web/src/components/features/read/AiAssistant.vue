<script setup lang="ts">
import { ref, computed } from 'vue'

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
const userQuestion = ref('请解释引用内容')
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
    content += `【主题】${props.nodeTitle}\n`
    if (props.nodeDescription) {
      content += `${props.nodeDescription}\n`
    }
    content += '\n'
  }
  if (props.selectedText) {
    content += `【引用内容】\n${props.selectedText}\n\n`
  }
  if (userQuestion.value) {
    content += `【我的问题】\n${userQuestion.value}`
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
  <v-card class="ai-assistant-card no-border" rounded="lg">
    <v-card-title class="pa-4">
      <div class="d-flex align-center justify-space-between w-100">
        <div class="d-flex align-center">
          <v-icon
            icon="mdi-chat-question-outline"
            color="primary"
            :size="$vuetify.display.mobile ? 20 : 24"
            class="mr-2"
          ></v-icon>
          <span class="text-body-1 text-md-h6">不懂就问</span>
        </div>
        <v-btn
          :icon="isExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down'"
          variant="text"
          color="grey-darken-1"
          size="x-small"
          @click="isExpanded = !isExpanded"
        ></v-btn>
      </div>
    </v-card-title>

    <v-expand-transition>
      <v-card-text v-show="isExpanded" class="pa-4 pt-0">
        <!-- 选中的文本预览 -->
        <div v-if="selectedText" class="selected-text-preview mb-3">
          <div
            class="d-flex align-center justify-space-between"
            :class="{ 'cursor-pointer': needExpand }"
            @click="needExpand && (isQuoteExpanded = !isQuoteExpanded)"
          >
            <div class="d-flex align-center text-caption text-grey-darken-1">
              <v-icon icon="mdi-format-quote-close" size="14" class="mr-1"></v-icon>
              <span>已引用内容</span>
              <span class="text-grey ml-1">({{ selectedText.length }}字)</span>
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
          在文章中选中您不太理解的内容
        </div>

        <!-- 问题输入框 -->
        <v-textarea
          v-model="userQuestion"
          label="输入您的问题"
          variant="outlined"
          density="compact"
          rows="2"
          hide-details
          class="mb-3"
          @focus="($event.target as HTMLTextAreaElement)?.select()"
        ></v-textarea>

        <!-- AI 引擎链接 - 点击复制并跳转 -->
        <div class="text-body-2 text-grey-darken-2 mb-2">
          去问AI（自动复制已引用内容和问题）
        </div>
        <div class="d-flex flex-wrap" style="gap: 8px">
          <v-chip
            v-for="e in aiEngines"
            :key="e.name"
            color="grey-darken-1"
            variant="tonal"
            rounded="lg"
            :size="$vuetify.display.mobile ? 'x-small' : 'small'"
            class="text-caption text-md-body-2 cursor-pointer"
            :prepend-icon="e.icon"
            :text="e.name"
            @click="copyAndGo(e)"
          />
          <v-chip
            color="primary"
            :variant="copySuccess ? 'flat' : 'outlined'"
            rounded="lg"
            :size="$vuetify.display.mobile ? 'x-small' : 'small'"
            class="text-caption text-md-body-2 cursor-pointer"
            :prepend-icon="copySuccess ? 'mdi-check' : 'mdi-content-copy'"
            :text="copySuccess ? '已复制' : '手动复制'"
            @click="copyOnly"
          />
        </div>
      </v-card-text>
    </v-expand-transition>
  </v-card>
</template>

<style scoped>
.ai-assistant-card {
  background-color: white;
  border: 1px solid rgb(var(--v-theme-border));
}

.ai-assistant-card .v-card-title {
  font-size: 0.9375rem;
  font-weight: 600;
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
</style>
