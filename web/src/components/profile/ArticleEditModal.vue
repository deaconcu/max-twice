<template>
  <v-dialog
    :model-value="modelValue"
    width="800"
    height="1200px"
    persistent
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <v-card rounded="xl">
      <v-card-item class="px-4 py-3">
        <v-card-title class="d-flex align-center justify-space-between pa-0">
          <div class="d-flex align-center">
            <v-icon size="small" class="px-4" icon="mdi-account"></v-icon>
            <span class="ps-2 font-weight-medium">{{
              isEditMode ? t('addArticle.editArticle') : t('addArticle.createArticle')
            }}</span>
          </div>
          <v-btn icon="mdi-close" variant="text" size="small" @click="handleCancel"></v-btn>
        </v-card-title>
      </v-card-item>
      <div class="overflow-y-auto dialog-content">
        <TipTapEditor
          ref="editorRef"
          :model-value="getCurrentArticle?.preview || getCurrentArticle?.content || ''"
          :editable="true"
          :placeholder="t('tiptap.placeholder')"
          @update:model-value="updateContentLength"
        />
      </div>
      <div class="px-4 pb-4 pt-4 action-bottom">
        <div class="d-flex align-center justify-space-between">
          <div class="d-flex align-center gap-2">
            <span class="text-caption text-grey"
              >{{ contentLength }} {{ t('addArticle.characters') }}</span
            >
            <span v-if="contentLength < MIN_LENGTH" class="text-caption text-warning">
              （{{ t('addArticle.needMore', { n: MIN_LENGTH - contentLength }) }}）
            </span>
            <span v-else-if="contentLength > MAX_LENGTH" class="text-caption text-error">
              （{{ t('addArticle.exceeded', { n: contentLength - MAX_LENGTH }) }}）
            </span>
          </div>
          <div class="d-flex gap-2">
            <v-btn variant="text" @click="handleCancel">
              {{ t('common.cancel') }}
            </v-btn>
            <!-- 创建模式 或 编辑草稿模式 -->
            <template v-if="!isEditMode || getCurrentArticle?.state === ContentState.DRAFT">
              <v-btn
                color="primary"
                variant="tonal"
                :loading="savingDraft"
                :disabled="!isDraftValid"
                @click="handleSave"
              >
                {{ t('addArticle.saveDraft') }}
              </v-btn>
              <v-btn
                color="primary"
                variant="flat"
                :loading="publishing"
                :disabled="!isFormValid"
                @click="handlePublish"
              >
                {{ t('addArticle.publish') }}
              </v-btn>
            </template>
            <!-- 编辑模式 - 如果不是草稿，只显示"保存" -->
            <v-btn v-else color="primary" variant="flat" :loading="savingDraft" @click="handleSave">
              {{ t('addArticle.save') }}
            </v-btn>
          </div>
        </div>
      </div>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed, inject } from 'vue'
import TipTapEditor from '@/components/common/TipTapEditor.vue'
import { ContentState, PostType } from '@/enums'
import { postApi } from '@/api'
import { useMutation } from '@/composables/useMutation'
import { useI18n } from '@/composables/useI18n'

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  nodeId: 0,
})

const emit = defineEmits<Emits>()

const { t } = useI18n()

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

interface Article {
  id: number
  preview?: string
  content?: string
  state?: number
  node?: { id: number; name: string }
  course?: string
  courseId?: number
}

interface Props {
  modelValue: boolean
  article: Article | null
  loading?: boolean
  nodeId?: number // 创建新文章时需要的 nodeId
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success', article: Article | null): void // 传递更新后的文章数据
  (e: 'cancel'): void
}

// 编辑器引用和字符统计
const editorRef = ref<InstanceType<typeof TipTapEditor> | null>(null)
const contentLength = ref(0)
const currentArticle = ref<Article | null>(null) // 当前编辑的文章（用于创建后转编辑模式）

// 字符限制
const MIN_LENGTH = 100
const MAX_LENGTH = 10000

// 更新字符计数
const updateContentLength = () => {
  if (editorRef.value?.editor) {
    const text = editorRef.value.editor.getText()
    contentLength.value = text.length
  }
}

// 验证表单是否有效（发布）
const isFormValid = computed(() => {
  return contentLength.value >= MIN_LENGTH && contentLength.value <= MAX_LENGTH
})

// 验证草稿是否有效（保存草稿）
const isDraftValid = computed(() => {
  return contentLength.value > 0 && contentLength.value <= MAX_LENGTH
})

// 判断是否是编辑模式
const isEditMode = computed(() => props.article != null || currentArticle.value != null)

// 获取当前文章
const getCurrentArticle = computed(() => props.article || currentArticle.value)

// 创建帖子
const { execute: executeCreate, loading: creating } = useMutation(
  (data: { nodeId: number; content: string; type: number; state: number }) =>
    postApi.createPost(data as any),
  {
    successMessage: t('posting.operationSuccess'),
    onSuccess: (result, payload) => {
      const isDraft = payload.state === ContentState.DRAFT
      if (isDraft) {
        // 保存草稿：保存返回的文章数据，转为编辑模式，通知父组件添加到列表
        currentArticle.value = result as Article
        emit('success', result as Article)
        // 不关闭窗口
      } else {
        // 发布文章：关闭窗口，不更新列表（文章待审核，列表不变）
        emit('update:modelValue', false)
        emit('success', null)
        currentArticle.value = null
      }
    },
  }
)

// 更新帖子
const { execute: executeUpdate, loading: updating } = useMutation(
  (data: { id: number; content: string; state?: number }) =>
    postApi.updatePost(data.id, { content: data.content, state: data.state as any }),
  {
    successMessage: t('posting.operationSuccess'),
    onSuccess: (result, payload) => {
      console.log('executeUpdate onSuccess', { result, payload })
      const isPublishing = payload.state === ContentState.SUBMITTED
      if (isPublishing) {
        // 发布文章：关闭窗口，通知父组件更新该文章状态
        console.log('Publishing, emitting success with:', result)
        emit('update:modelValue', false)
        emit('success', result as Article)
        currentArticle.value = null
      } else {
        // 保存草稿：更新当前文章数据，通知父组件更新列表中的数据
        console.log('Saving draft, emitting success with:', result)
        if (result) {
          currentArticle.value = result as Article
        }
        emit('success', result as Article)
        // 不关闭窗口
      }
    },
  }
)

const submitting = computed(() => creating.value || updating.value)
const savingDraft = ref(false) // 保存草稿的 loading 状态
const publishing = ref(false) // 发布文章的 loading 状态

// 保存文章
const handleSave = async () => {
  if (!editorRef.value?.editor) {
    return
  }

  const content = editorRef.value.editor.getHTML()
  const article = getCurrentArticle.value

  savingDraft.value = true
  try {
    // 编辑模式（包括创建后转换的编辑模式）
    if (article) {
      await executeUpdate({
        id: article.id,
        content,
        state: ContentState.DRAFT,
      })
      return
    }

    // 创建模式
    if (!props.nodeId) {
      showSnackbar?.(t('addArticle.missingNodeId'), 'error')
      return
    }
    if (!isDraftValid.value) {
      showSnackbar?.(t('addArticle.contentEmpty'), 'warning')
      return
    }

    // 创建新草稿
    await executeCreate({
      nodeId: props.nodeId,
      content,
      type: PostType.ARTICLE,
      state: ContentState.DRAFT,
    })
  } finally {
    savingDraft.value = false
  }
}

// 发布文章
const handlePublish = async () => {
  if (!editorRef.value?.editor) {
    return
  }

  const content = editorRef.value.editor.getHTML()
  const article = getCurrentArticle.value

  if (!isFormValid.value) {
    showSnackbar?.(
      t('addArticle.contentLengthError', { min: MIN_LENGTH, max: MAX_LENGTH }),
      'warning'
    )
    return
  }

  publishing.value = true
  try {
    // 编辑模式（包括创建后转换的编辑模式）
    if (article) {
      await executeUpdate({
        id: article.id,
        content,
        state: ContentState.SUBMITTED,
      })
      return
    }

    // 创建模式
    if (!props.nodeId) {
      showSnackbar?.(t('addArticle.missingNodeId'), 'error')
      return
    }

    // 直接发布
    await executeCreate({
      nodeId: props.nodeId,
      content,
      type: PostType.ARTICLE,
      state: ContentState.SUBMITTED,
    })
  } finally {
    publishing.value = false
  }
}

// 取消编辑
const handleCancel = () => {
  emit('update:modelValue', false)
  emit('cancel')
}

// 监听对话框打开，初始化字符计数
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal) {
      // 对话框打开时，延迟更新字符计数（等待编辑器初始化）
      nextTick(() => {
        setTimeout(() => {
          updateContentLength()
        }, 100)
      })
    } else {
      // 对话框关闭时，重置状态
      setTimeout(() => {
        currentArticle.value = null
        contentLength.value = 0
        if (editorRef.value?.editor) {
          editorRef.value.editor.commands.setContent('')
        }
      }, 300)
    }
  }
)
</script>

<style scoped>
.dialog-content {
  height: calc(1200px - 120px);
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.dialog-content::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

/* 移除 TipTap 编辑器的边框 */
:deep(.tiptap-editor) {
  border: none !important;
}

.action-bottom {
  background-color: #fff;
  border-top: 1px solid #e0e0e0;
}

.gap-2 {
  gap: 8px;
}
</style>
