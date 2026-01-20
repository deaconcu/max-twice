<script setup lang="ts">
import { ref, computed, inject } from 'vue'
import { useMutation } from '@/composables/useMutation'
import { postApi } from '@/api'
import { PostType, ContentState } from '@/enums'
import TipTapEditor from '@/components/common/TipTapEditor.vue'

// 注入全局 showSnackbar
const showSnackbar = inject<(message: string, type: string) => void>('showSnackbar')

interface Props {
  nodeId?: number
  pathText?: string
}

const props = withDefaults(defineProps<Props>(), {
  nodeId: 0,
  pathText: '',
})

const emit = defineEmits<Emits>()

type Emits = (e: 'load-data', data: any[]) => void

const dialog = defineModel<boolean>({ default: false })

const articleContent = ref('')

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

// 验证草稿是否有效（保存草稿时需要）
const isDraftValid = computed(() => {
  return contentLength.value > 0 && contentLength.value <= MAX_LENGTH
})

const draftId = ref<number | null>(null) // 草稿ID

// 创建帖子（新增）
const { execute: executeCreate, loading: creating } = useMutation(
  (data: { nodeId: number; content: string; type: number; state: number }) =>
    postApi.createPost(data),
  {
    onSuccess: (response, payload) => {
      const isDraft = payload.state === ContentState.DRAFT
      if (isDraft) {
        draftId.value = response.data.id
        showSnackbar?.('草稿保存成功', 'success')
      } else {
        showSnackbar?.('文章发布成功', 'success')
        dialog.value = false
        articleContent.value = ''
        draftId.value = null
        emit('load-data', [])
      }
    },
  }
)

// 更新帖子（更新草稿或发布草稿）
const { execute: executeUpdate, loading: updating } = useMutation(
  (data: { id: number; content: string; state?: number }) =>
    postApi.updatePost(data.id, { content: data.content, state: data.state }),
  {
    onSuccess: (_, payload) => {
      const isPublishing = payload.state === ContentState.SUBMITTED
      if (isPublishing) {
        showSnackbar?.('文章发布成功', 'success')
        dialog.value = false
        articleContent.value = ''
        draftId.value = null
        emit('load-data', [])
      } else {
        showSnackbar?.('草稿保存成功', 'success')
      }
    },
  }
)

const submitting = computed(() => creating.value || updating.value)

// 保存草稿
const saveDraft = async () => {
  if (!isDraftValid.value) {
    showSnackbar?.('内容不能为空', 'warning')
    return
  }

  if (draftId.value) {
    // 更新已有草稿
    await executeUpdate({
      id: draftId.value,
      content: articleContent.value,
    })
  } else {
    // 创建新草稿
    await executeCreate({
      nodeId: props.nodeId,
      content: articleContent.value,
      type: PostType.ARTICLE,
      state: ContentState.DRAFT,
    })
  }
}

// 发布文章
const submitArticle = async () => {
  if (!isFormValid.value) {
    return
  }

  if (draftId.value) {
    // 发布已有草稿（更新状态为待审核）
    await executeUpdate({
      id: draftId.value,
      content: articleContent.value,
      state: ContentState.SUBMITTED,
    })
  } else {
    // 直接发布（创建待审核帖子）
    await executeCreate({
      nodeId: props.nodeId,
      content: articleContent.value,
      type: PostType.ARTICLE,
      state: ContentState.SUBMITTED,
    })
  }
}

// 关闭对话框
const closeDialog = () => {
  dialog.value = false
  articleContent.value = ''
  draftId.value = null
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
            variant="tonal"
            color="primary"
            class="px-4"
            :loading="submitting"
            :disabled="!isDraftValid"
            @click="saveDraft"
          >
            保存草稿
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
