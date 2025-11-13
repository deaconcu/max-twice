<template>
  <div class="tiptap-editor" :class="{ 'no-border': noBorder }">
    <!-- 工具栏 -->
    <div v-if="editor" class="editor-toolbar">
      <!-- 行1：基本操作和文本格式 -->
      <div class="toolbar-row">
        <!-- 撤销/重做/清除格式 -->
        <v-btn
          :disabled="!editor.can().undo()"
          variant="text"
          size="small"
          icon="mdi-undo"
          @click="editor.chain().focus().undo().run()"
        />
        <v-btn
          :disabled="!editor.can().redo()"
          variant="text"
          size="small"
          icon="mdi-redo"
          @click="editor.chain().focus().redo().run()"
        />
        <v-btn variant="text" size="small" icon="mdi-eraser" @click="clearFormatting" />

        <v-divider vertical class="mx-2" />

        <!-- 文本样式 -->
        <v-btn
          :color="editor.isActive('bold') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-bold"
          @click="editor.chain().focus().toggleBold().run()"
        />
        <v-btn
          :color="editor.isActive('italic') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-italic"
          @click="editor.chain().focus().toggleItalic().run()"
        />
        <v-btn
          :color="editor.isActive('underline') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-underline"
          @click="editor.chain().focus().toggleUnderline().run()"
        />
        <v-btn
          :color="editor.isActive('strike') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-strikethrough"
          @click="editor.chain().focus().toggleStrike().run()"
        />
        <v-btn
          :color="editor.isActive('highlight') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-marker"
          @click="editor.chain().focus().toggleHighlight().run()"
        />

        <v-divider vertical class="mx-2" />

        <!-- 颜色选择器 -->
        <input
          type="color"
          :value="editor.getAttributes('textStyle').color || '#000000'"
          class="color-input"
          title="文本颜色"
          @input="editor.chain().focus().setColor(($event.target as HTMLInputElement).value).run()"
        />

        <v-divider vertical class="mx-2" />

        <!-- 标题 -->
        <v-btn
          :color="editor.isActive('heading', { level: 1 }) ? 'primary' : undefined"
          variant="text"
          size="small"
          @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
        >
          H1
        </v-btn>
        <v-btn
          :color="editor.isActive('heading', { level: 2 }) ? 'primary' : undefined"
          variant="text"
          size="small"
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
        >
          H2
        </v-btn>
        <v-btn
          :color="editor.isActive('heading', { level: 3 }) ? 'primary' : undefined"
          variant="text"
          size="small"
          @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
        >
          H3
        </v-btn>

        <v-divider vertical class="mx-2" />

        <!-- 上标/下标 -->
        <v-btn
          :color="editor.isActive('subscript') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-subscript"
          @click="editor.chain().focus().toggleSubscript().run()"
        />
        <v-btn
          :color="editor.isActive('superscript') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-superscript"
          @click="editor.chain().focus().toggleSuperscript().run()"
        />
      </div>

      <!-- 行2：对齐、列表、插入和操作 -->
      <div class="toolbar-row mt-2">
        <!-- 文本对齐 -->
        <v-btn
          :color="editor.isActive({ textAlign: 'left' }) ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-align-left"
          @click="editor.chain().focus().setTextAlign('left').run()"
        />
        <v-btn
          :color="editor.isActive({ textAlign: 'center' }) ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-align-center"
          @click="editor.chain().focus().setTextAlign('center').run()"
        />
        <v-btn
          :color="editor.isActive({ textAlign: 'right' }) ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-align-right"
          @click="editor.chain().focus().setTextAlign('right').run()"
        />
        <v-btn
          :color="editor.isActive({ textAlign: 'justify' }) ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-align-justify"
          @click="editor.chain().focus().setTextAlign('justify').run()"
        />

        <v-divider vertical class="mx-2" />

        <!-- 列表 -->
        <v-btn
          :color="editor.isActive('bulletList') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-list-bulleted"
          @click="editor.chain().focus().toggleBulletList().run()"
        />
        <v-btn
          :color="editor.isActive('orderedList') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-list-numbered"
          @click="editor.chain().focus().toggleOrderedList().run()"
        />
        <v-btn
          :disabled="!editor.can().sinkListItem('listItem')"
          variant="text"
          size="small"
          icon="mdi-format-indent-increase"
          @click="editor.chain().focus().sinkListItem('listItem').run()"
        />
        <v-btn
          :disabled="!editor.can().liftListItem('listItem')"
          variant="text"
          size="small"
          icon="mdi-format-indent-decrease"
          @click="editor.chain().focus().liftListItem('listItem').run()"
        />

        <v-divider vertical class="mx-2" />

        <!-- 插入操作 -->
        <v-btn
          :color="editor.isActive('link') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-link"
          @click="toggleLink"
        />
        <v-btn variant="text" size="small" icon="mdi-image" @click="addImage" />
        <v-btn
          :color="editor.isActive('code') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-code-tags"
          @click="editor.chain().focus().toggleCode().run()"
        />
        <v-btn
          :color="editor.isActive('codeBlock') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-code-braces"
          @click="editor.chain().focus().toggleCodeBlock().run()"
        />
        <v-btn
          :color="editor.isActive('blockquote') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-quote-close"
          @click="editor.chain().focus().toggleBlockquote().run()"
        />
        <v-btn
          variant="text"
          size="small"
          icon="mdi-minus"
          @click="editor.chain().focus().setHorizontalRule().run()"
        />
        <v-btn
          variant="text"
          size="small"
          icon="mdi-table"
          @click="editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()"
        />
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
  noBorder?: boolean
}

type Emits = (e: 'update:modelValue', value: string) => void

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '请在这里输入内容...',
  editable: true,
  autofocus: false,
  minHeight: '300px',
  noBorder: false,
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
 * 清除格式
 */
const clearFormatting = () => {
  editor.value?.chain().focus().unsetAllMarks().run()
  editor.value?.chain().focus().clearNodes().run()
}

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
  background-color: rgb(var(--v-theme-surface));
}

.tiptap-editor.no-border {
  border: none;
  border-radius: 0;
}

/* 工具栏 */
.editor-toolbar {
  padding: 0 12px 12px 12px;
  background-color: rgb(var(--v-theme-surface));
  position: sticky;
  top: 0;
  z-index: 10;
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.toolbar-row :deep(.v-btn) {
  min-width: 36px !important;
  width: 36px !important;
  height: 36px !important;
  border-radius: 4px !important;
}

.toolbar-row :deep(.v-btn:hover) {
  background-color: rgba(var(--v-theme-on-surface), 0.08) !important;
}

.toolbar-row :deep(.v-btn--variant-text.text-primary) {
  background-color: rgb(var(--v-theme-primary)) !important;
  color: white !important;
}

.toolbar-row :deep(.v-btn--variant-text.text-primary .v-icon) {
  color: white !important;
}

.toolbar-row :deep(.v-btn--variant-text.text-primary .v-btn__content) {
  color: white !important;
}

.toolbar-row :deep(.v-divider) {
  height: 20px !important;
  align-self: center !important;
}

.color-input {
  height: 32px;
  width: 42px;
  border: 1px solid rgb(var(--v-theme-border));
  border-radius: 4px;
  cursor: pointer;
}

.word-count {
  padding: 0 8px;
  white-space: nowrap;
}

/* 编辑器内容区 */
.editor-content {
  min-height: v-bind(minHeight);
}

/* 编辑器内容样式 */
.editor-content :deep(.ProseMirror) {
  padding: 16px 26px;
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

/* 高亮样式 */
.editor-content :deep(.ProseMirror mark) {
  background-color: #faf594;
  border-radius: 0.4rem;
  padding: 0.1rem 0.3rem;
}

/* 下划线样式 */
.editor-content :deep(.ProseMirror u) {
  text-decoration: underline;
}

/* 上标/下标 */
.editor-content :deep(.ProseMirror sup) {
  vertical-align: super;
  font-size: smaller;
}

.editor-content :deep(.ProseMirror sub) {
  vertical-align: sub;
  font-size: smaller;
}

/* 表格样式 */
.editor-content :deep(.ProseMirror table) {
  border-collapse: collapse;
  margin: 1em 0;
  overflow: hidden;
  table-layout: fixed;
  width: 100%;
}

.editor-content :deep(.ProseMirror table td),
.editor-content :deep(.ProseMirror table th) {
  border: 1px solid rgb(var(--v-theme-border));
  padding: 8px;
  min-width: 1em;
  vertical-align: top;
}

.editor-content :deep(.ProseMirror table th) {
  background-color: rgb(var(--v-theme-surface-variant));
  font-weight: bold;
  text-align: left;
}

/* 选中样式 */
.editor-content :deep(.ProseMirror ::selection) {
  background-color: rgba(var(--v-theme-primary), 0.2);
}

/* 响应式调整 */
@media (max-width: 600px) {
  .editor-toolbar {
    padding: 8px;
  }

  .toolbar-row {
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
