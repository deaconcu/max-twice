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
          title="撤销"
          @click="editor.chain().focus().undo().run()"
        />
        <v-btn
          :disabled="!editor.can().redo()"
          variant="text"
          size="small"
          icon="mdi-redo"
          title="重做"
          @click="editor.chain().focus().redo().run()"
        />
        <v-btn
          variant="text"
          size="small"
          icon="mdi-eraser"
          title="清除格式"
          @click="clearFormatting"
        />

        <v-divider vertical class="mx-2" />

        <!-- 文本样式 -->
        <v-btn
          :color="editor.isActive('bold') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-bold"
          title="粗体"
          @click="editor.chain().focus().toggleBold().run()"
        />
        <v-btn
          :color="editor.isActive('italic') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-italic"
          title="斜体"
          @click="editor.chain().focus().toggleItalic().run()"
        />
        <v-btn
          :color="editor.isActive('underline') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-underline"
          title="下划线"
          @click="editor.chain().focus().toggleUnderline().run()"
        />
        <v-btn
          :color="editor.isActive('strike') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-strikethrough"
          title="删除线"
          @click="editor.chain().focus().toggleStrike().run()"
        />
        <v-btn
          :color="editor.isActive('highlight') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-marker"
          title="高亮"
          @click="editor.chain().focus().toggleHighlight().run()"
        />

        <v-divider vertical class="mx-2" />

        <!-- 颜色选择器 -->
        <input
          type="color"
          :value="editor.getAttributes('textStyle').color || '#000000'"
          class="color-input"
          title="文本颜色"
          @input="
            editor
              .chain()
              .focus()
              .setColor(($event.target as HTMLInputElement).value)
              .run()
          "
        />

        <v-divider vertical class="mx-2" />

        <!-- 标题 -->
        <v-btn
          :color="editor.isActive('heading', { level: 1 }) ? 'primary' : undefined"
          variant="text"
          size="small"
          title="一级标题"
          @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
        >
          H1
        </v-btn>
        <v-btn
          :color="editor.isActive('heading', { level: 2 }) ? 'primary' : undefined"
          variant="text"
          size="small"
          title="二级标题"
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
        >
          H2
        </v-btn>
        <v-btn
          :color="editor.isActive('heading', { level: 3 }) ? 'primary' : undefined"
          variant="text"
          size="small"
          title="三级标题"
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
          title="下标"
          @click="editor.chain().focus().toggleSubscript().run()"
        />
        <v-btn
          :color="editor.isActive('superscript') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-superscript"
          title="上标"
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
          title="左对齐"
          @click="editor.chain().focus().setTextAlign('left').run()"
        />
        <v-btn
          :color="editor.isActive({ textAlign: 'center' }) ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-align-center"
          title="居中对齐"
          @click="editor.chain().focus().setTextAlign('center').run()"
        />
        <v-btn
          :color="editor.isActive({ textAlign: 'right' }) ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-align-right"
          title="右对齐"
          @click="editor.chain().focus().setTextAlign('right').run()"
        />
        <v-btn
          :color="editor.isActive({ textAlign: 'justify' }) ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-align-justify"
          title="两端对齐"
          @click="editor.chain().focus().setTextAlign('justify').run()"
        />

        <v-divider vertical class="mx-2" />

        <!-- 列表 -->
        <v-btn
          :color="editor.isActive('bulletList') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-list-bulleted"
          title="无序列表"
          @click="editor.chain().focus().toggleBulletList().run()"
        />
        <v-btn
          :color="editor.isActive('orderedList') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-list-numbered"
          title="有序列表"
          @click="editor.chain().focus().toggleOrderedList().run()"
        />
        <v-btn
          :disabled="!editor.can().sinkListItem('listItem')"
          variant="text"
          size="small"
          icon="mdi-format-indent-increase"
          title="增加缩进"
          @click="editor.chain().focus().sinkListItem('listItem').run()"
        />
        <v-btn
          :disabled="!editor.can().liftListItem('listItem')"
          variant="text"
          size="small"
          icon="mdi-format-indent-decrease"
          title="减少缩进"
          @click="editor.chain().focus().liftListItem('listItem').run()"
        />

        <v-divider vertical class="mx-2" />

        <!-- 插入操作 -->
        <v-btn
          :color="editor.isActive('link') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-link"
          title="插入链接"
          @click="toggleLink"
        />
        <v-btn variant="text" size="small" icon="mdi-image" title="插入图片" @click="addImage" />
        <v-btn
          :color="editor.isActive('code') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-code-tags"
          title="行内代码"
          @click="editor.chain().focus().toggleCode().run()"
        />
        <v-btn
          :color="editor.isActive('codeBlock') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-code-braces"
          title="代码块"
          @click="editor.chain().focus().toggleCodeBlock().run()"
        />
        <v-btn
          :color="editor.isActive('blockquote') ? 'primary' : undefined"
          variant="text"
          size="small"
          icon="mdi-format-quote-close"
          title="引用"
          @click="editor.chain().focus().toggleBlockquote().run()"
        />
        <v-btn
          variant="text"
          size="small"
          icon="mdi-minus"
          title="分隔线"
          @click="editor.chain().focus().setHorizontalRule().run()"
        />
        <v-menu location="bottom" offset="4">
          <template #activator="{ props }">
            <v-btn v-bind="props" variant="text" size="small" icon="mdi-table" title="表格" />
          </template>
          <v-card rounded="lg" class="table-menu" width="180">
            <div class="table-menu-items py-1">
              <div
                class="table-menu-item"
                @click="
                  editor
                    .chain()
                    .focus()
                    .insertTable({ rows: 3, cols: 3, withHeaderRow: true })
                    .run()
                "
              >
                <v-icon icon="mdi-table-plus" size="18" class="table-menu-icon" />
                <span>插入表格</span>
              </div>
              <v-divider class="my-1" />
              <div
                class="table-menu-item"
                :class="{ 'table-menu-item--disabled': !editor.can().addRowAfter() }"
                @click="editor.can().addRowAfter() && editor.chain().focus().addRowAfter().run()"
              >
                <v-icon icon="mdi-table-row-plus-after" size="18" class="table-menu-icon" />
                <span>在下方插入行</span>
              </div>
              <div
                class="table-menu-item"
                :class="{ 'table-menu-item--disabled': !editor.can().addColumnAfter() }"
                @click="
                  editor.can().addColumnAfter() && editor.chain().focus().addColumnAfter().run()
                "
              >
                <v-icon icon="mdi-table-column-plus-after" size="18" class="table-menu-icon" />
                <span>在右侧插入列</span>
              </div>
              <v-divider class="my-1" />
              <div
                class="table-menu-item"
                :class="{ 'table-menu-item--disabled': !editor.can().deleteRow() }"
                @click="editor.can().deleteRow() && editor.chain().focus().deleteRow().run()"
              >
                <v-icon icon="mdi-table-row-remove" size="18" class="table-menu-icon" />
                <span>删除当前行</span>
              </div>
              <div
                class="table-menu-item"
                :class="{ 'table-menu-item--disabled': !editor.can().deleteColumn() }"
                @click="editor.can().deleteColumn() && editor.chain().focus().deleteColumn().run()"
              >
                <v-icon icon="mdi-table-column-remove" size="18" class="table-menu-icon" />
                <span>删除当前列</span>
              </div>
              <div
                class="table-menu-item"
                :class="{ 'table-menu-item--disabled': !editor.can().deleteTable() }"
                @click="editor.can().deleteTable() && editor.chain().focus().deleteTable().run()"
              >
                <v-icon icon="mdi-table-remove" size="18" class="table-menu-icon" />
                <span>删除表格</span>
              </div>
            </div>
          </v-card>
        </v-menu>
        <v-btn
          variant="text"
          size="small"
          icon="mdi-video"
          title="插入视频"
          @click="openVideoDialog"
        />
        <v-btn
          variant="text"
          size="small"
          icon="mdi-math-integral"
          title="数学公式"
          @click="openMathDialog"
        />
        <v-btn
          variant="text"
          size="small"
          icon="mdi-chart-timeline-variant"
          title="Mermaid 图表"
          @click="openMermaidDialog"
        />
      </div>
    </div>

    <!-- 编辑器内容区 -->
    <EditorContent :editor="editor" class="editor-content" />

    <!-- 隐藏的文件上传input -->
    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      style="display: none"
      @change="handleFileUpload"
    />

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
      <v-card rounded="xl">
        <v-card-title class="d-flex align-center justify-space-between pa-4">
          <span class="text-h6">{{ t('editor.insertImage') }}</span>
          <v-btn icon="mdi-close" variant="text" size="small" @click="imageDialog = false" />
        </v-card-title>

        <v-card-text class="pa-4">
          <!-- 上传按钮 -->
          <v-btn
            variant="tonal"
            color="primary"
            block
            class="mb-2"
            :loading="uploadingImage"
            @click="triggerFileUpload"
          >
            <v-icon icon="mdi-upload" class="mr-2" />
            {{ t('editor.uploadImage') }}
          </v-btn>

          <!-- 图片说明 -->
          <div class="text-caption mb-1 d-flex align-center">
            <v-icon icon="mdi-information-outline" size="16" color="grey" class="mr-1" />
            <span class="text-grey mr-1">{{ t('editor.imageHintLabel') }}</span>
            <span class="text-grey">{{ t('editor.imageHint') }}</span>
          </div>

          <v-divider class="my-4">
            <span class="text-caption text-grey">{{ t('editor.or') }}</span>
          </v-divider>

          <v-text-field
            v-model="imageUrl"
            :label="t('editor.imageUrl')"
            :placeholder="t('editor.imageUrlPlaceholder')"
            variant="outlined"
            density="comfortable"
            hide-details
            class="mt-2"
            @keyup.enter="setImage"
          >
            <template #prepend-inner>
              <v-icon icon="mdi-link-variant" size="20" color="grey" />
            </template>
          </v-text-field>
        </v-card-text>

        <v-card-actions class="pa-4">
          <v-spacer />
          <v-btn variant="tonal" color="grey" @click="imageDialog = false">{{
            t('common.cancel')
          }}</v-btn>
          <v-btn color="primary" variant="tonal" :disabled="!imageUrl.trim()" @click="setImage">
            {{ t('editor.insert') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 数学公式对话框 -->
    <v-dialog v-model="mathDialog" max-width="600">
      <v-card rounded="xl">
        <v-card-title class="d-flex align-center justify-space-between pa-4">
          <span class="text-h6">{{
            mathEditMode ? t('editor.editMath') : t('editor.insertMath')
          }}</span>
          <v-btn icon="mdi-close" variant="text" size="small" @click="mathDialog = false" />
        </v-card-title>

        <v-card-text class="pa-4">
          <div class="text-caption text-grey mb-3">
            {{ t('editor.mathHint') }}
          </div>

          <v-radio-group v-model="mathDisplayMode" inline class="mb-3">
            <v-radio :label="t('editor.inlineMath')" :value="false" />
            <v-radio :label="t('editor.blockMath')" :value="true" />
          </v-radio-group>

          <v-textarea
            v-model="mathFormula"
            :label="t('editor.latexFormula')"
            :placeholder="mathDisplayMode ? 'E = mc^2' : 'x^2 + y^2 = r^2'"
            variant="outlined"
            density="comfortable"
            rows="3"
            hide-details
            class="math-input"
          />

          <div class="text-caption text-grey mt-3">
            <strong>{{ t('editor.mathExample') }}</strong>
            <code class="mx-1">x^2</code> {{ t('editor.mathSuperscript') }}，
            <code class="mx-1">x_i</code> {{ t('editor.mathSubscript') }}，
            <code class="mx-1">\frac{a}{b}</code> {{ t('editor.mathFraction') }}，
            <code class="mx-1">\sqrt{x}</code> {{ t('editor.mathSqrt') }}
          </div>
        </v-card-text>

        <v-card-actions class="pa-4">
          <v-btn v-if="mathEditMode" variant="tonal" color="error" @click="deleteMath">
            {{ t('common.delete') }}
          </v-btn>
          <v-spacer />
          <v-btn variant="tonal" color="grey" @click="mathDialog = false">{{
            t('common.cancel')
          }}</v-btn>
          <v-btn color="primary" variant="tonal" :disabled="!mathFormula.trim()" @click="saveMath">
            {{ mathEditMode ? t('common.save') : t('editor.insert') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Mermaid 图表对话框 -->
    <v-dialog v-model="mermaidDialog" max-width="700">
      <v-card rounded="xl">
        <v-card-title class="d-flex align-center justify-space-between pa-4">
          <span class="text-h6">{{
            mermaidEditMode ? t('editor.editMermaid') : t('editor.insertMermaid')
          }}</span>
          <v-btn icon="mdi-close" variant="text" size="small" @click="mermaidDialog = false" />
        </v-card-title>

        <v-card-text class="pa-4">
          <div class="text-caption text-grey mb-3">
            {{ t('editor.mermaidHint') }}
          </div>

          <v-textarea
            v-model="mermaidCode"
            :label="t('editor.mermaidCode')"
            placeholder="graph TD
    A[Start] --> B{Decision}
    B -->|Yes| C[Execute]
    B -->|No| D[End]"
            variant="outlined"
            density="comfortable"
            rows="8"
            hide-details
            class="mermaid-input"
          />

          <div class="text-caption text-grey mt-3">
            <strong>{{ t('editor.mermaidTypes') }}</strong>
            <code class="mx-1">graph TD</code> {{ t('editor.mermaidFlowchart') }}，
            <code class="mx-1">sequenceDiagram</code> {{ t('editor.mermaidSequence') }}，
            <code class="mx-1">classDiagram</code> {{ t('editor.mermaidClass') }}，
            <code class="mx-1">pie</code> {{ t('editor.mermaidPie') }}
          </div>
        </v-card-text>

        <v-card-actions class="pa-4">
          <v-btn v-if="mermaidEditMode" variant="tonal" color="error" @click="deleteMermaid">
            {{ t('common.delete') }}
          </v-btn>
          <v-spacer />
          <v-btn variant="tonal" color="grey" @click="mermaidDialog = false">{{
            t('common.cancel')
          }}</v-btn>
          <v-btn
            color="primary"
            variant="tonal"
            :disabled="!mermaidCode.trim()"
            @click="saveMermaid"
          >
            {{ mermaidEditMode ? t('common.save') : t('editor.insert') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 视频插入对话框 -->
    <v-dialog v-model="videoDialog" max-width="500">
      <v-card rounded="xl">
        <v-card-title class="d-flex align-center justify-space-between pa-4">
          <span class="text-h6">{{ t('editor.insertVideo') }}</span>
          <v-btn icon="mdi-close" variant="text" size="small" @click="videoDialog = false" />
        </v-card-title>

        <v-card-text class="pa-4">
          <v-text-field
            v-model="videoUrl"
            :label="t('editor.videoUrl')"
            :placeholder="t('editor.videoUrlPlaceholder')"
            :error-messages="videoUrlError"
            variant="outlined"
            density="comfortable"
            autofocus
            @keyup.enter="insertVideo"
            @input="videoUrlError = ''"
          >
            <template #prepend-inner>
              <v-icon icon="mdi-link-variant" size="20" color="grey" />
            </template>
          </v-text-field>

          <div class="text-caption text-grey mt-2">
            <strong>{{ t('editor.supportedPlatforms') }}</strong>
            <div class="mt-1">YouTube、Bilibili、Vimeo、{{ t('editor.directVideoLink') }}</div>
          </div>
        </v-card-text>

        <v-card-actions class="pa-4">
          <v-spacer />
          <v-btn variant="tonal" color="grey" @click="videoDialog = false">{{
            t('common.cancel')
          }}</v-btn>
          <v-btn color="primary" variant="tonal" :disabled="!videoUrl.trim()" @click="insertVideo">
            {{ t('editor.insert') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onBeforeUnmount, onMounted, inject } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { useI18n } from '@/composables/useI18n'
import { useMutation } from '@/composables/useMutation'
import { getTipTapExtensions } from '@/config/tiptap'
import { parseVideoUrl } from '@/config/tiptap-video'
import { imageApi } from '@/api'

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

// 注入全局 showSnackbar
const showSnackbar = inject<(message: string, type: string) => void>('showSnackbar')

// 文件上传相关
const fileInput = ref<HTMLInputElement>()

// 使用 useMutation 上传图片
const { execute: uploadImage, loading: uploadingImage } = useMutation(
  (file: File) => imageApi.upload(file, 'post'),
  {
    showToast: false,
  }
)

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

// 数学公式对话框
const mathDialog = ref(false)
const mathFormula = ref('')
const mathDisplayMode = ref(true) // true: 块级, false: 行内

// Mermaid 图表对话框
const mermaidDialog = ref(false)
const mermaidCode = ref('')

// 视频对话框
const videoDialog = ref(false)
const videoUrl = ref('')
const videoUrlError = ref('')

// 编辑模式（用于区分新建和编辑）
const mathEditMode = ref(false)
const mermaidEditMode = ref(false)
let mathUpdateAttributes: ((attrs: Record<string, unknown>) => void) | null = null
let mathDeleteNode: (() => void) | null = null
let mermaidUpdateAttributes: ((attrs: Record<string, unknown>) => void) | null = null
let mermaidDeleteNode: (() => void) | null = null

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
 * 打开数学公式对话框
 */
const openMathDialog = () => {
  mathFormula.value = ''
  mathDisplayMode.value = true
  mathEditMode.value = false
  mathUpdateAttributes = null
  mathDeleteNode = null
  mathDialog.value = true
}

/**
 * 保存数学公式（插入或更新）
 */
const saveMath = () => {
  if (!mathFormula.value.trim()) {
    mathDialog.value = false
    return
  }

  if (mathEditMode.value && mathUpdateAttributes) {
    // 编辑模式：更新已有节点
    mathUpdateAttributes({
      formula: mathFormula.value.trim(),
      displayMode: mathDisplayMode.value,
    })
  } else {
    // 新建模式：插入新节点
    ;(editor.value?.commands as any).insertMath({
      formula: mathFormula.value.trim(),
      displayMode: mathDisplayMode.value,
    })
  }

  mathDialog.value = false
}

/**
 * 删除数学公式
 */
const deleteMath = () => {
  if (mathEditMode.value && mathDeleteNode) {
    mathDeleteNode()
  }
  mathDialog.value = false
}

/**
 * 打开 Mermaid 对话框
 */
const openMermaidDialog = () => {
  mermaidCode.value = ''
  mermaidEditMode.value = false
  mermaidUpdateAttributes = null
  mermaidDeleteNode = null
  mermaidDialog.value = true
}

/**
 * 保存 Mermaid 图表（插入或更新）
 */
const saveMermaid = () => {
  if (!mermaidCode.value.trim()) {
    mermaidDialog.value = false
    return
  }

  if (mermaidEditMode.value && mermaidUpdateAttributes) {
    // 编辑模式：更新已有节点
    mermaidUpdateAttributes({
      code: mermaidCode.value.trim(),
    })
  } else {
    // 新建模式：插入新节点
    ;(editor.value?.commands as any).insertMermaid({
      code: mermaidCode.value.trim(),
    })
  }

  mermaidDialog.value = false
}

/**
 * 删除 Mermaid 图表
 */
const deleteMermaid = () => {
  if (mermaidEditMode.value && mermaidDeleteNode) {
    mermaidDeleteNode()
  }
  mermaidDialog.value = false
}

/**
 * 打开视频对话框
 */
const openVideoDialog = () => {
  videoUrl.value = ''
  videoUrlError.value = ''
  videoDialog.value = true
}

/**
 * 插入视频
 */
const insertVideo = () => {
  if (!videoUrl.value.trim()) {
    videoDialog.value = false
    return
  }

  // 验证 URL 是否能被解析
  const parsed = parseVideoUrl(videoUrl.value)
  if (!parsed) {
    videoUrlError.value = t('editor.videoUrlInvalid')
    return
  }

  editor.value?.commands.insertVideo({
    src: videoUrl.value.trim(),
  })

  videoDialog.value = false
}

/**
 * 触发文件上传
 */
const triggerFileUpload = () => {
  fileInput.value?.click()
}

/**
 * 处理文件上传
 */
const handleFileUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  // 验证文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    showSnackbar?.(t('editor.imageTypeError'), 'error')
    return
  }

  // 验证文件大小 (5MB)
  if (file.size > 5 * 1024 * 1024) {
    showSnackbar?.(t('editor.imageSizeError'), 'error')
    return
  }

  try {
    // 上传图片
    const response = await uploadImage(file)

    // 如果返回 null，说明上传失败
    if (!response?.fileUrl) {
      showSnackbar?.(t('editor.imageUploadFailed'), 'error')
      return
    }

    // 插入图片到编辑器
    editor.value?.chain().focus().setImage({ src: response.fileUrl }).run()
    imageDialog.value = false
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : t('editor.imageUploadFailed')
    showSnackbar?.(errorMessage, 'error')
  } finally {
    // 清空 input
    if (target) target.value = ''
  }
}

/**
 * 处理双击编辑数学公式
 */
const handleEditMath = (event: Event) => {
  const customEvent = event as CustomEvent
  const { formula, displayMode, updateAttributes, deleteNode } = customEvent.detail

  mathFormula.value = formula || ''
  mathDisplayMode.value = displayMode ?? true
  mathEditMode.value = true
  mathUpdateAttributes = updateAttributes
  mathDeleteNode = deleteNode
  mathDialog.value = true
}

/**
 * 处理双击编辑 Mermaid 图表
 */
const handleEditMermaid = (event: Event) => {
  const customEvent = event as CustomEvent
  const { code, updateAttributes, deleteNode } = customEvent.detail

  mermaidCode.value = code || ''
  mermaidEditMode.value = true
  mermaidUpdateAttributes = updateAttributes
  mermaidDeleteNode = deleteNode
  mermaidDialog.value = true
}

/**
 * 组件挂载时添加事件监听
 */
onMounted(() => {
  window.addEventListener('tiptap-edit-math', handleEditMath)
  window.addEventListener('tiptap-edit-mermaid', handleEditMermaid)
})

/**
 * 组件卸载时销毁编辑器
 */
onBeforeUnmount(() => {
  editor.value?.destroy()
  window.removeEventListener('tiptap-edit-math', handleEditMath)
  window.removeEventListener('tiptap-edit-mermaid', handleEditMermaid)
})

/**
 * 暴露给父组件的方法和属性
 */
defineExpose({
  editor,
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

/* 图片样式 */
.editor-content :deep(.ProseMirror img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
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

/* 图片插入对话框样式 */
.image-guide {
  padding: 12px 0;
  background-color: rgb(var(--v-theme-grey-lighten-5));
  border-radius: 8px;
}

.guide-header {
  display: flex;
  align-items: center;
  padding: 0 12px;
}

.image-guide p {
  padding: 0 12px;
}

.image-hosts {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 0 12px;
}

/* 表格下拉菜单 */
.table-menu {
  border: 1px solid rgb(var(--v-theme-border));
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.table-menu-items {
  padding: 0;
}

.table-menu-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  font-size: 13px;
  color: rgb(var(--v-theme-on-surface));
}

.table-menu-item:hover {
  background-color: rgb(var(--v-theme-surface-variant));
}

.table-menu-item--disabled {
  opacity: 0.4;
  pointer-events: none;
}

.table-menu-icon {
  margin-right: 10px;
  color: rgb(var(--v-theme-on-surface-variant));
}

/* 数学公式和 Mermaid 输入框 */
.math-input :deep(.v-field__input),
.mermaid-input :deep(.v-field__input) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  font-size: 14px !important;
}

/* 数学公式节点样式 */
.editor-content :deep(.math-node) {
  display: inline-block;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.15s ease;
}

.editor-content :deep(.math-block) {
  display: block;
  text-align: center;
  padding: 8px;
  margin: 4px 0;
}

.editor-content :deep(.math-inline) {
  padding: 2px 4px;
}

.editor-content :deep(.math-selected) {
  background-color: rgba(var(--v-theme-on-surface), 0.04);
  outline: 1px solid rgba(var(--v-theme-on-surface), 0.15);
}

.editor-content :deep(.math-placeholder) {
  color: rgb(var(--v-theme-on-surface-variant));
  font-style: italic;
}

.editor-content :deep(.math-error) {
  color: rgb(var(--v-theme-error));
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
}

/* Mermaid 节点样式 */
.editor-content :deep(.mermaid-node) {
  display: block;
  padding: 16px;
  margin: 16px 0;
  background-color: rgb(var(--v-theme-surface-variant));
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: center;
}

.editor-content :deep(.mermaid-node svg) {
  max-width: 100%;
  height: auto;
}

.editor-content :deep(.mermaid-selected) {
  outline: 1px solid rgba(var(--v-theme-on-surface), 0.15);
}

.editor-content :deep(.mermaid-placeholder) {
  color: rgb(var(--v-theme-on-surface-variant));
  font-style: italic;
  padding: 32px;
}

.editor-content :deep(.mermaid-error) {
  color: rgb(var(--v-theme-error));
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  text-align: left;
  white-space: pre-wrap;
  background-color: rgba(var(--v-theme-error), 0.1);
  padding: 12px;
  border-radius: 4px;
}

/* 视频节点样式 */
.editor-content :deep(.video-node) {
  display: block;
  margin: 16px 0;
  border-radius: 8px;
  overflow: hidden;
  background-color: rgb(var(--v-theme-surface-variant));
}

.editor-content :deep(.video-selected) {
  outline: 2px solid rgb(var(--v-theme-primary));
}

.editor-content :deep(.video-wrapper) {
  position: relative;
  width: 100%;
  padding-bottom: 56.25%; /* 16:9 宽高比 */
}

.editor-content :deep(.video-iframe),
.editor-content :deep(.video-player) {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: none;
}
</style>
