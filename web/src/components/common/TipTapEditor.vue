<template>
  <div class="tiptap-editor">
    <!-- 工具栏 -->
    <div v-if="editor" class="editor-toolbar">
      <!-- 文本样式 -->
      <v-btn-group variant="outlined" density="compact">
        <v-btn
          :color="editor.isActive('bold') ? 'primary' : undefined"
          size="small"
          icon="mdi-format-bold"
          @click="editor.chain().focus().toggleBold().run()"
        />
        <v-btn
          :color="editor.isActive('italic') ? 'primary' : undefined"
          size="small"
          icon="mdi-format-italic"
          @click="editor.chain().focus().toggleItalic().run()"
        />
        <v-btn
          :color="editor.isActive('strike') ? 'primary' : undefined"
          size="small"
          icon="mdi-format-strikethrough"
          @click="editor.chain().focus().toggleStrike().run()"
        />
        <v-btn
          :color="editor.isActive('code') ? 'primary' : undefined"
          size="small"
          icon="mdi-code-tags"
          @click="editor.chain().focus().toggleCode().run()"
        />
      </v-btn-group>

      <v-divider vertical class="mx-2" />

      <!-- 标题 -->
      <v-btn-group variant="outlined" density="compact">
        <v-btn
          :color="editor.isActive('heading', { level: 1 }) ? 'primary' : undefined"
          size="small"
          @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
        >
          H1
        </v-btn>
        <v-btn
          :color="editor.isActive('heading', { level: 2 }) ? 'primary' : undefined"
          size="small"
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
        >
          H2
        </v-btn>
        <v-btn
          :color="editor.isActive('heading', { level: 3 }) ? 'primary' : undefined"
          size="small"
          @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
        >
          H3
        </v-btn>
      </v-btn-group>

      <v-divider vertical class="mx-2" />

      <!-- 列表 -->
      <v-btn-group variant="outlined" density="compact">
        <v-btn
          :color="editor.isActive('bulletList') ? 'primary' : undefined"
          size="small"
          icon="mdi-format-list-bulleted"
          @click="editor.chain().focus().toggleBulletList().run()"
        />
        <v-btn
          :color="editor.isActive('orderedList') ? 'primary' : undefined"
          size="small"
          icon="mdi-format-list-numbered"
          @click="editor.chain().focus().toggleOrderedList().run()"
        />
      </v-btn-group>

      <v-divider vertical class="mx-2" />

      <!-- 其他格式 -->
      <v-btn-group variant="outlined" density="compact">
        <v-btn
          :color="editor.isActive('blockquote') ? 'primary' : undefined"
          size="small"
          icon="mdi-format-quote-close"
          @click="editor.chain().focus().toggleBlockquote().run()"
        />
        <v-btn
          :color="editor.isActive('codeBlock') ? 'primary' : undefined"
          size="small"
          icon="mdi-code-braces"
          @click="editor.chain().focus().toggleCodeBlock().run()"
        />
        <v-btn
          size="small"
          icon="mdi-minus"
          @click="editor.chain().focus().setHorizontalRule().run()"
        />
      </v-btn-group>

      <v-divider vertical class="mx-2" />

      <!-- 链接和图片 -->
      <v-btn-group variant="outlined" density="compact">
        <v-btn
          :color="editor.isActive('link') ? 'primary' : undefined"
          size="small"
          icon="mdi-link"
          @click="toggleLink"
        />
        <v-btn size="small" icon="mdi-image" @click="addImage" />
      </v-btn-group>

      <v-divider vertical class="mx-2" />

      <!-- 撤销/重做 -->
      <v-btn-group variant="outlined" density="compact">
        <v-btn
          :disabled="!editor.can().undo()"
          size="small"
          icon="mdi-undo"
          @click="editor.chain().focus().undo().run()"
        />
        <v-btn
          :disabled="!editor.can().redo()"
          size="small"
          icon="mdi-redo"
          @click="editor.chain().focus().redo().run()"
        />
      </v-btn-group>

      <v-spacer />

      <!-- 字数统计 -->
      <div class="word-count text-caption text-medium-emphasis">
        {{ wordCount }} {{ t('editor.words') }}
      </div>
    </div>

    <!-- 编辑器内容区 -->
    <EditorContent :editor="editor" class="editor-content" />

    <!-- 链接编辑对话框 -->
    <v-dialog v-model="linkDialog" max-width="500">
      <v-card>
        <v-card-title>{{ t('editor.insertLink') }}</v-card-title>
        <v-card-text>
          <v-text-field
            v-model="linkUrl"
            :label="t('editor.linkUrl')"
            :placeholder="t('editor.linkUrlPlaceholder')"
            variant="outlined"
            density="comfortable"
            autofocus
            @keyup.enter="setLink"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="linkDialog = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="primary" @click="setLink">{{ t('common.confirm') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 图片编辑对话框 -->
    <v-dialog v-model="imageDialog" max-width="500">
      <v-card>
        <v-card-title>{{ t('editor.insertImage') }}</v-card-title>
        <v-card-text>
          <v-text-field
            v-model="imageUrl"
            :label="t('editor.imageUrl')"
            :placeholder="t('editor.imageUrlPlaceholder')"
            variant="outlined"
            density="comfortable"
            autofocus
            @keyup.enter="setImage"
          />
          <!-- TODO: 添加图片上传功能 -->
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="imageDialog = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="primary" @click="setImage">{{ t('common.confirm') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { useI18n } from '@/composables/useI18n'
import { getTipTapExtensions } from '@/config/tiptap'

interface Props {
  modelValue?: string
  placeholder?: string
  editable?: boolean
  autofocus?: boolean | 'start' | 'end' | number
  minHeight?: string
}

type Emits = (e: 'update:modelValue', value: string) => void

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '请在这里输入内容...',
  editable: true,
  autofocus: false,
  minHeight: '300px',
})

const emit = defineEmits<Emits>()
const { t } = useI18n()

// 编辑器实例
const editor = useEditor({
  extensions: getTipTapExtensions(props.placeholder),
  content: props.modelValue,
  editable: props.editable,
  autofocus: props.autofocus,
  onUpdate: () => {
    if (editor.value) {
      emit('update:modelValue', editor.value.getHTML())
    }
  },
})

// 链接对话框
const linkDialog = ref(false)
const linkUrl = ref('')

// 图片对话框
const imageDialog = ref(false)
const imageUrl = ref('')

// 字数统计
const wordCount = computed(() => {
  if (!editor.value) return 0
  const text = editor.value.getText()
  return text.length
})

/**
 * 监听 modelValue 变化（外部更新）
 */
watch(
  () => props.modelValue,
  (value) => {
    if (editor.value && editor.value.getHTML() !== value) {
      editor.value.commands.setContent(value, false)
    }
  }
)

/**
 * 监听 editable 变化
 */
watch(
  () => props.editable,
  (value) => {
    if (editor.value) {
      editor.value.setEditable(value)
    }
  }
)

/**
 * 切换链接
 */
const toggleLink = () => {
  const previousUrl = editor.value?.getAttributes('link').href
  if (previousUrl) {
    // 如果已有链接，则移除
    editor.value?.chain().focus().unsetLink().run()
  } else {
    // 打开链接编辑对话框
    linkUrl.value = ''
    linkDialog.value = true
  }
}

/**
 * 设置链接
 */
const setLink = () => {
  if (!linkUrl.value) {
    linkDialog.value = false
    return
  }

  // 自动添加 http:// 前缀
  let url = linkUrl.value
  if (!/^https?:\/\//i.test(url)) {
    url = `https://${url}`
  }

  editor.value?.chain().focus().setLink({ href: url }).run()
  linkDialog.value = false
}

/**
 * 添加图片
 */
const addImage = () => {
  imageUrl.value = ''
  imageDialog.value = true
}

/**
 * 设置图片
 */
const setImage = () => {
  if (!imageUrl.value) {
    imageDialog.value = false
    return
  }

  editor.value?.chain().focus().setImage({ src: imageUrl.value }).run()
  imageDialog.value = false
}

/**
 * 组件卸载时销毁编辑器
 */
onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped>
.tiptap-editor {
  border: 1px solid rgb(var(--v-theme-border));
  border-radius: 8px;
  overflow: hidden;
  background-color: rgb(var(--v-theme-surface));
}

/* 工具栏 */
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid rgb(var(--v-theme-border));
  background-color: rgb(var(--v-theme-surface));
  flex-wrap: wrap;
}

.word-count {
  padding: 0 8px;
  white-space: nowrap;
}

/* 编辑器内容区 */
.editor-content {
  min-height: v-bind(minHeight);
  max-height: 600px;
  overflow-y: auto;
}

/* 编辑器内容样式 */
.editor-content :deep(.ProseMirror) {
  padding: 16px;
  outline: none;
  min-height: v-bind(minHeight);
}

/* 占位符 */
.editor-content :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: rgb(var(--v-theme-on-surface-variant));
  pointer-events: none;
  height: 0;
}

/* 标题样式 */
.editor-content :deep(.ProseMirror h1) {
  font-size: 2em;
  font-weight: bold;
  margin: 0.5em 0;
}

.editor-content :deep(.ProseMirror h2) {
  font-size: 1.5em;
  font-weight: bold;
  margin: 0.5em 0;
}

.editor-content :deep(.ProseMirror h3) {
  font-size: 1.25em;
  font-weight: bold;
  margin: 0.5em 0;
}

/* 列表样式 */
.editor-content :deep(.ProseMirror ul),
.editor-content :deep(.ProseMirror ol) {
  padding-left: 2em;
  margin: 0.5em 0;
}

/* 引用样式 */
.editor-content :deep(.ProseMirror blockquote) {
  border-left: 4px solid rgb(var(--v-theme-primary));
  padding-left: 1em;
  margin: 1em 0;
  color: rgb(var(--v-theme-on-surface-variant));
}

/* 代码块样式 */
.editor-content :deep(.ProseMirror pre) {
  background-color: rgb(var(--v-theme-surface-variant));
  border-radius: 8px;
  padding: 1em;
  margin: 1em 0;
  overflow-x: auto;
}

.editor-content :deep(.ProseMirror code) {
  background-color: rgb(var(--v-theme-surface-variant));
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
}

/* 链接样式 */
.editor-content :deep(.ProseMirror .tiptap-link) {
  color: rgb(var(--v-theme-primary));
  text-decoration: underline;
  cursor: pointer;
}

/* 图片样式 */
.editor-content :deep(.ProseMirror .tiptap-image) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 1em 0;
}

/* 水平线样式 */
.editor-content :deep(.ProseMirror hr) {
  border: none;
  border-top: 2px solid rgb(var(--v-theme-border));
  margin: 2em 0;
}

/* 选中样式 */
.editor-content :deep(.ProseMirror ::selection) {
  background-color: rgba(var(--v-theme-primary), 0.2);
}

/* 响应式调整 */
@media (max-width: 600px) {
  .editor-toolbar {
    padding: 8px;
    gap: 4px;
  }

  .editor-content {
    min-height: 200px;
  }

  .editor-content :deep(.ProseMirror) {
    padding: 12px;
    min-height: 200px;
  }
}
</style>
