<script setup lang="ts">
import { ref } from 'vue'
import { postServiceV1 } from '@/services/api/v1/apiServiceV1'
import Tiptap from './TiptapInput.vue'
import { useI18n } from 'vue-i18n'
import { PostType } from '@/types/enums'
import { postContentRules } from '@/utils/validationRules'
import { POST_VALIDATION } from '@/types/validation'
import { useMutation } from '@/composables/useMutation'

interface Props {
  nodeId: number
  pathText: string
}

interface Emits {
  (e: 'loadData', data: any[]): void
}

interface EditorRef {
  editor: {
    getHTML(): string
    getText(): string
  }
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()

const dialog = defineModel<boolean>({ type: Boolean })
const editorRef = ref<EditorRef | null>(null)
const formValid = ref(false)
const contentLength = ref(0)

const updateContentLength = () => {
  if (editorRef.value?.editor) {
    const text = editorRef.value.editor.getText()
    contentLength.value = text.length

    // 验证内容长度
    formValid.value =
      text.length >= POST_VALIDATION.CONTENT_MIN_LENGTH &&
      text.length <= POST_VALIDATION.CONTENT_MAX_LENGTH
  }
}

// 使用 useMutation 提交文章
const { execute: submitAddArticle, loading: submitting } = useMutation(
  () => {
    if (!editorRef.value) {
      throw new Error('Editor ref is null')
    }

    const data = {
      content: editorRef.value.editor.getHTML(),
      nodeId: props.nodeId,
      type: PostType.ARTICLE,
    }

    console.log(`request: ${JSON.stringify(data)}`)
    return postServiceV1.createPost(data)
  },
  {
    successMessage: '发布成功',
    onSuccess: (result) => {
      console.log('Form submitted successfully')
      console.log(`response: ${JSON.stringify(result)}`)
      emit('loadData', [])
      dialog.value = false
    }
  }
)
</script>

<template>
  <v-dialog v-model="dialog" width="800" height="1200px" persistent>
    <v-card rounded="xl">
      <v-card-item class="border-b-sm">
        <v-card-title class="d-flex align-center justify-space-between">
          <div class="d-flex align-center">
            <v-icon size="small" class="px-4" icon="mdi-account"></v-icon>
            <span class="ps-2 font-weight-medium">{{ t('addArticle.createArticle') }}</span>
          </div>
          <v-btn icon="mdi-close" variant="text" size="small" @click="dialog = false"></v-btn>
        </v-card-title>
      </v-card-item>
      <div class="overflow-y-scroll dialog-content">
        <Tiptap ref="editorRef" :path-text="props.pathText" class="px-6" @update="updateContentLength" />
      </div>
      <div class="px-6 pb-6 pt-4 action-bottom">
        <div class="d-flex align-center justify-space-between">
          <div class="text-caption text-grey">
            {{ contentLength }} / {{ POST_VALIDATION.CONTENT_MAX_LENGTH }}
            <span v-if="contentLength < POST_VALIDATION.CONTENT_MIN_LENGTH" class="text-error">
              (至少 {{ POST_VALIDATION.CONTENT_MIN_LENGTH }} 字符)
            </span>
          </div>
          <div class="d-flex gap-2">
            <v-btn variant="text" @click="dialog = false">
              {{ t('common.cancel') }}
            </v-btn>
            <v-btn
              color="primary"
              variant="flat"
              :disabled="!formValid"
              :loading="submitting"
              @click="submitAddArticle"
            >
              {{ t('addArticle.submitArticle') }}
            </v-btn>
          </div>
        </div>
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.dialog-content {
  height: calc(1200px - 120px);
}

.action-bottom {
  background-color: #fff;
  border-top: 1px solid #e0e0e0;
}

.gap-2 {
  gap: 8px;
}
</style>