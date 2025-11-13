<script setup lang="ts">
import { ref, computed } from 'vue'
import TipTapEditor from '@/components/common/TipTapEditor.vue'

interface Props {
  nodeId?: number
  pathText?: string
}

const props = withDefaults(defineProps<Props>(), {
  nodeId: 0,
  pathText: '',
})

interface Emits {
  (e: 'load-data', data: any[]): void
}

const emit = defineEmits<Emits>()

const dialog = defineModel<boolean>({ default: false })

const articleContent = ref('')
const submitting = ref(false)

// 字符限制
const MIN_LENGTH = 100
const MAX_LENGTH = 10000

// 计算内容长度（纯文本）
const contentLength = computed(() => {
  // 移除 HTML 标签，计算纯文本长度
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = articleContent.value
  const text = tempDiv.textContent || tempDiv.innerText || ''
  return text.trim().length
})

// 验证表单是否有效
const isFormValid = computed(() => {
  return contentLength.value >= MIN_LENGTH && contentLength.value <= MAX_LENGTH
})

// 提交文章
const submitArticle = () => {
  if (!isFormValid.value) {
    return
  }

  submitting.value = true

  // Mock 提交
  setTimeout(() => {
    console.log('提交文章:', {
      nodeId: props.nodeId,
      content: articleContent.value,
      contentLength: contentLength.value,
    })

    submitting.value = false
    dialog.value = false
    emit('load-data', [])

    // 清空内容
    articleContent.value = ''
  }, 500)
}

// 关闭对话框
const closeDialog = () => {
  dialog.value = false
  articleContent.value = ''
}
</script>

<template>
  <v-dialog v-model="dialog" width="750" persistent>
    <v-card rounded="xl" class="dialog-card">
      <!-- 头部 -->
      <v-card-title class="pa-4 d-flex align-center justify-space-between">
        <div class="d-flex align-center">
          <v-icon icon="mdi-note-plus-outline" color="primary" class="mr-2"></v-icon>
          <span class="text-h6 font-weight-bold">添加文章</span>
        </div>
        <v-btn icon="mdi-close" variant="text" size="small" @click="closeDialog"></v-btn>
      </v-card-title>

      <!-- 编辑器内容 -->
      <v-card-text class="pa-0 editor-container">
        <TipTapEditor
          v-model="articleContent"
          placeholder="请输入文章内容..."
          min-height="600px"
          no-border
        />
      </v-card-text>

      <!-- 底部操作 -->
      <v-card-actions class="pa-4 border-t d-flex justify-space-between align-center">
        <div class="text-caption">
          <span :class="contentLength < MIN_LENGTH ? 'text-error' : 'text-grey-darken-2'">
            {{ contentLength }} / {{ MAX_LENGTH }}
          </span>
          <span v-if="contentLength < MIN_LENGTH" class="text-error ml-2">
            (至少 {{ MIN_LENGTH }} 字符)
          </span>
        </div>

        <div class="d-flex gap-2">
          <v-btn variant="text" color="grey-darken-2" class="px-4" @click="closeDialog">
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="primary"
            class="px-6"
            :loading="submitting"
            :disabled="!isFormValid"
            @click="submitArticle"
          >
            发布文章
          </v-btn>
        </div>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.border-b {
  border-bottom: 1px solid rgb(var(--v-theme-border));
}

.border-t {
  border-top: 1px solid rgb(var(--v-theme-border));
}

.dialog-card {
  max-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

.editor-container {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.gap-2 {
  gap: 8px;
}
</style>
