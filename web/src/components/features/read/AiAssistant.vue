<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  selectedText?: string
}

interface Emits {
  (e: 'update:selectedText', value: string): void
}

const props = withDefaults(defineProps<Props>(), {
  selectedText: '',
})

const emit = defineEmits<Emits>()

// 答疑助手状态
const userQuestion = ref('请解释引用内容')
const copySuccess = ref(false)
const isExpanded = ref(true)
const isQuoteExpanded = ref(false)

// 是否需要展开按钮（超过约2行）
const needExpand = computed(() => props.selectedText.length > 60)

// AI 引擎列表
const aiEngines = [
  { name: 'ChatGPT', href: 'https://chatgpt.com', color: 'grey-darken-1', icon: 'mdi-robot' },
  { name: 'Claude', href: 'https://claude.ai', color: 'grey-darken-1', icon: 'mdi-alpha-c-circle-outline' },
  { name: 'Gemini', href: 'https://gemini.google.com', color: 'grey-darken-1', icon: 'mdi-google' },
  { name: 'DeepSeek', href: 'https://chat.deepseek.com', color: 'grey-darken-1', icon: 'mdi-radar' },
]

// 复制到剪贴板
const copyToClipboard = async () => {
  if (!props.selectedText && !userQuestion.value) return

  let content = ''
  if (props.selectedText) {
    content += `【引用内容】\n${props.selectedText}\n\n`
  }
  if (userQuestion.value) {
    content += `【我的问题】\n${userQuestion.value}`
  }

  try {
    await navigator.clipboard.writeText(content.trim())
    copySuccess.value = true
    setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch (err) {
    console.error('复制失败:', err)
  }
}

// 清除选中内容
const clearSelection = () => {
  emit('update:selectedText', '')
  userQuestion.value = ''
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
            icon="mdi-robot-excited"
            color="primary"
            :size="$vuetify.display.mobile ? 20 : 24"
            class="mr-2"
          ></v-icon>
          <span class="text-body-1 text-md-h6">答疑助手</span>
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

        <!-- 操作按钮 -->
        <div class="d-flex ga-2 mb-4">
          <v-btn
            :color="copySuccess ? 'success' : 'primary'"
            variant="flat"
            rounded="lg"
            size="small"
            :disabled="!selectedText && !userQuestion"
            :prepend-icon="copySuccess ? 'mdi-check' : 'mdi-content-copy'"
            @click="copyToClipboard"
          >
            {{ copySuccess ? '已复制' : '复制' }}
          </v-btn>
          <v-btn
            v-if="selectedText || userQuestion"
            variant="tonal"
            rounded="lg"
            size="small"
            color="grey"
            @click="clearSelection"
          >
            清除
          </v-btn>
        </div>

        <!-- AI 引擎链接 -->
        <div class="d-flex flex-wrap" style="gap: 8px">
          <div class="text-body-2 w-100 text-grey-darken-2 font-weight-bold">
            常用 AI 引擎：
          </div>
          <v-chip
            v-for="e in aiEngines"
            :key="e.name"
            :href="e.href"
            target="_blank"
            rel="noopener"
            :color="e.color"
            variant="tonal"
            rounded="lg"
            :size="$vuetify.display.mobile ? 'x-small' : 'small'"
            class="text-caption text-md-body-2"
            :prepend-icon="e.icon"
            :text="e.name"
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
