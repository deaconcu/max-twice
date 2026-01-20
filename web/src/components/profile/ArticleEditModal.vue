<template>
  <v-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" width="800" height="1200px" persistent>
    <v-card rounded="xl">
      <v-card-item>
        <v-card-title class="d-flex align-center justify-space-between">
          <div class="d-flex align-center">
            <v-icon size="small" class="px-4" icon="mdi-account"></v-icon>
            <span class="ps-2 font-weight-medium">编辑文章</span>
          </div>
          <v-btn icon="mdi-close" variant="text" size="small" @click="handleCancel"></v-btn>
        </v-card-title>
      </v-card-item>
      <div class="overflow-y-auto dialog-content">
        <TipTapEditor
          ref="editorRef"
          :model-value="article?.preview || ''"
          :editable="true"
          placeholder="在这里编写您的文章内容..."
          class="px-3"
          @update="updateContentLength"
        />
      </div>
      <div class="px-6 pb-6 pt-4 action-bottom">
        <div class="d-flex align-center justify-space-between">
          <div class="text-caption text-grey">
            {{ contentLength }} 字符
          </div>
          <div class="d-flex gap-2">
            <v-btn variant="text" @click="handleCancel">
              取消
            </v-btn>
            <!-- 如果是草稿，显示"保存草稿"和"发布文章" -->
            <template v-if="article?.state === ContentState.DRAFT">
              <v-btn
                color="primary"
                variant="tonal"
                :loading="loading"
                @click="handleSave"
              >
                保存草稿
              </v-btn>
              <v-btn
                color="primary"
                variant="flat"
                :loading="loading"
                @click="handlePublish"
              >
                发布文章
              </v-btn>
            </template>
            <!-- 如果不是草稿，只显示"保存" -->
            <v-btn
              v-else
              color="primary"
              variant="flat"
              :loading="loading"
              @click="handleSave"
            >
              保存
            </v-btn>
          </div>
        </div>
      </div>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import TipTapEditor from '@/components/common/TipTapEditor.vue'
import { ContentState } from '@/enums'

interface Article {
  id: number
  preview: string
  state?: number
  node?: { id: number; name: string }
  course?: string
  courseId?: number
}

interface Props {
  modelValue: boolean
  article: Article | null
  loading?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', data: { id: number; content: string }): void
  (e: 'publish', data: { id: number; content: string }): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
})

const emit = defineEmits<Emits>()

// 编辑器引用和字符统计
const editorRef = ref<InstanceType<typeof TipTapEditor> | null>(null)
const contentLength = ref(0)

// 更新字符计数
const updateContentLength = () => {
  if (editorRef.value?.editor) {
    const text = editorRef.value.editor.getText()
    contentLength.value = text.length
  }
}

// 保存文章
const handleSave = () => {
  if (!props.article || !editorRef.value?.editor) {
    return
  }

  const content = editorRef.value.editor.getHTML()

  emit('save', {
    id: props.article.id,
    content,
  })
}

// 发布文章
const handlePublish = () => {
  if (!props.article || !editorRef.value?.editor) {
    return
  }

  const content = editorRef.value.editor.getHTML()

  emit('publish', {
    id: props.article.id,
    content,
  })
}

// 取消编辑
const handleCancel = () => {
  emit('update:modelValue', false)
}
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