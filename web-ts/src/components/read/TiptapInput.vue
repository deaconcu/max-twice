<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref } from 'vue'
  import { Editor, EditorContent } from '@tiptap/vue-3'
  import { Color } from '@tiptap/extension-color'
  import Placeholder from '@tiptap/extension-placeholder'
  import TextStyle from '@tiptap/extension-text-style'
  import ListItem from '@tiptap/extension-list-item'
  import StarterKit from '@tiptap/starter-kit'
  import Highlight from '@tiptap/extension-highlight'
  import Link from '@tiptap/extension-link'
  import Underline from '@tiptap/extension-underline'
  import Subscript from '@tiptap/extension-subscript'
  import Superscript from '@tiptap/extension-superscript'
  import Image from '@tiptap/extension-image'
  import Table from '@tiptap/extension-table'
  import TableCell from '@tiptap/extension-table-cell'
  import TableHeader from '@tiptap/extension-table-header'
  import TableRow from '@tiptap/extension-table-row'
  import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
  import TextAlign from '@tiptap/extension-text-align'
  import { Mathematics } from '@tiptap-pro/extension-mathematics'
  import { all, createLowlight } from 'lowlight'
  import css from 'highlight.js/lib/languages/css'
  import js from 'highlight.js/lib/languages/javascript'
  import ts from 'highlight.js/lib/languages/typescript'
  import html from 'highlight.js/lib/languages/xml'
  import { aiServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { marked } from 'marked'
  import { useI18n } from 'vue-i18n'

  const lowlight = createLowlight(all)
  lowlight.register('html', html)
  lowlight.register('css', css)
  lowlight.register('js', js)
  lowlight.register('ts', ts)

  interface Props {
    pathText?: string
    content?: string
  }

  const props = withDefaults(defineProps<Props>(), {
    pathText: '',
    content: '',
  })

  const editor = ref<Editor | null>(null)
  const { t } = useI18n()

  onMounted(() => {
    editor.value = new Editor({
      extensions: [
        Color.configure({ types: [TextStyle.name, ListItem.name] }),
        Mathematics,
        TextStyle.configure({ types: [ListItem.name] }),
        StarterKit.configure({ codeBlock: false }),
        Placeholder.configure({ placeholder: t('tiptap.placeholder') }),
        Underline,
        Superscript,
        Subscript,
        Table.configure({ resizable: true }),
        TableRow,
        TableHeader,
        TableCell,
        Highlight.configure({ multicolor: true }),
        Link.configure({ openOnClick: false, defaultProtocol: 'https' }),
        CodeBlockLowlight.configure({ lowlight }),
        Image,
        TextAlign.configure({ types: ['heading', 'paragraph'] }),
      ],
      content: props.content,
    })
  })

  onBeforeUnmount(() => {
    if (editor.value) {
      editor.value.destroy()
    }
  })

  defineExpose({ editor })

  const clearFormatting = (): void => {
    editor.value?.chain().focus().unsetAllMarks().run()
    editor.value?.chain().focus().clearNodes().run()
  }

  const setLink = (): void => {
    const previousUrl = editor.value?.getAttributes('link').href
    // TODO: 实现一个安全的 URL 输入对话框
    const url = previousUrl || 'https://'
    if (url === '') {
      editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
      return
    }
    editor.value?.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
  }

  const addImage = (): void => {
    // TODO: 实现一个安全的图片 URL 输入对话框
    const url = 'https://'
    if (url && url !== 'https://') {
      editor.value?.chain().focus().setImage({ src: url }).run()
    }
  }

  const generate = async (): Promise<void> => {
    try {
      const response = await aiServiceV1.chat(
        `我在写一本教材，我当前在[${
          props.pathText
        }]这个目录下，请用容易让人理解的方式，给出一篇文章，返回的内容要带markdown的格式，不要有和内容无关的语言，请根据实际情况调整长度，文章要清楚明白`,
        'openai/gpt-4o-mini'
      )
      if (response.code === 200 && editor.value) {
        const html = marked(response.data)
        editor.value.commands.setContent(html)
      }
    } catch {
      // 错误处理
    }
  }
</script>

<template>
  <div v-if="editor" class="editor-container">
    <div class="control-group pb-2 pt-6 sticky-toolbar">
      <div class="button-group">
        <!-- 操作 -->
        <button
          :disabled="!editor.can().chain().focus().undo().run()"
          @click="editor.chain().focus().undo().run()"
        >
          <v-icon icon="mdi-arrow-u-left-top"></v-icon>
        </button>

        <button
          :disabled="!editor.can().chain().focus().redo().run()"
          @click="editor.chain().focus().redo().run()"
        >
          <v-icon icon="mdi-arrow-u-right-top"></v-icon>
        </button>

        <button @click="clearFormatting">
          <v-icon icon="mdi-eraser"></v-icon>
        </button>

        <span class="border-e mx-1 my-2"></span>

        <!-- 基本文本格式 -->

        <!-- bold -->
        <button
          :disabled="!editor.can().chain().focus().toggleBold().run()"
          :class="{ 'is-active': editor.isActive('bold') }"
          @click="editor.chain().focus().toggleBold().run()"
        >
          <v-icon icon="mdi-format-bold"></v-icon>
        </button>

        <!-- Italic-->
        <button
          :disabled="!editor.can().chain().focus().toggleItalic().run()"
          :class="{ 'is-active': editor.isActive('italic') }"
          @click="editor.chain().focus().toggleItalic().run()"
        >
          <v-icon icon="mdi-format-italic"></v-icon>
        </button>

        <!-- underline -->
        <button
          :class="{ 'is-active': editor.isActive('underline') }"
          @click="editor.chain().focus().toggleUnderline().run()"
        >
          <v-icon icon="mdi-format-underline"></v-icon>
        </button>

        <!-- strike -->
        <button
          :disabled="!editor.can().chain().focus().toggleStrike().run()"
          :class="{ 'is-active': editor.isActive('strike') }"
          @click="editor.chain().focus().toggleStrike().run()"
        >
          <v-icon icon="mdi-format-strikethrough-variant"></v-icon>
        </button>

        <!-- highlight -->
        <button
          :class="{ 'is-active': editor.isActive('highlight') }"
          @click="editor.chain().focus().toggleHighlight().run()"
        >
          <v-icon icon="mdi-format-color-highlight"></v-icon>
        </button>

        <!-- Color -->
        <input
          type="color"
          :value="editor.getAttributes('textStyle').color || '#ffffff'"
          class="color-input"
          @input="editor.chain().focus().setColor(($event.target as HTMLInputElement).value).run()"
        />

        <!-- Heading -->
        <button
          :class="{ 'is-active': editor.isActive('heading', { level: 1 }) }"
          @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
        >
          <v-icon icon="mdi-format-header-1"></v-icon>
        </button>
        <button
          :class="{ 'is-active': editor.isActive('heading', { level: 2 }) }"
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
        >
          <v-icon icon="mdi-format-header-2"></v-icon>
        </button>
        <button
          :class="{ 'is-active': editor.isActive('heading', { level: 3 }) }"
          @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
        >
          <v-icon icon="mdi-format-header-3"></v-icon>
        </button>

        <!-- subscirpt -->
        <button
          :class="{ 'is-active': editor.isActive('subscript') }"
          @click="editor.chain().focus().toggleSubscript().run()"
        >
          <v-icon icon="mdi-format-subscript"></v-icon>
        </button>
        <!-- superscript-->
        <button
          :class="{ 'is-active': editor.isActive('superscript') }"
          @click="editor.chain().focus().toggleSuperscript().run()"
        >
          <v-icon icon="mdi-format-superscript"></v-icon>
        </button>

        <button @click="generate">
          <span class="text-body-1 font-weight-bold">AI</span>
        </button>
      </div>
      <div class="button-group">
        <!-- 对齐方式 -->

        <!-- Text Align -->
        <button
          :class="{ 'is-active': editor.isActive({ textAlign: 'left' }) }"
          @click="editor.chain().focus().setTextAlign('left').run()"
        >
          <v-icon icon="mdi-format-align-left"></v-icon>
        </button>
        <button
          :class="{ 'is-active': editor.isActive({ textAlign: 'center' }) }"
          @click="editor.chain().focus().setTextAlign('center').run()"
        >
          <v-icon icon="mdi-format-align-center"></v-icon>
        </button>
        <button
          :class="{ 'is-active': editor.isActive({ textAlign: 'right' }) }"
          @click="editor.chain().focus().setTextAlign('right').run()"
        >
          <v-icon icon="mdi-format-align-right"></v-icon>
        </button>
        <button
          :class="{ 'is-active': editor.isActive({ textAlign: 'justify' }) }"
          @click="editor.chain().focus().setTextAlign('justify').run()"
        >
          <v-icon icon="mdi-format-align-justify"></v-icon>
        </button>

        <span class="border-e mx-1 my-2"></span>

        <!-- 列表 -->

        <!-- bullet list -->
        <button
          :class="{ 'is-active': editor.isActive('bulletList') }"
          @click="editor.chain().focus().toggleBulletList().run()"
        >
          <v-icon icon="mdi-format-list-bulleted"></v-icon>
        </button>
        <!-- ordered list -->
        <button
          :class="{ 'is-active': editor.isActive('orderedList') }"
          @click="editor.chain().focus().toggleOrderedList().run()"
        >
          <v-icon icon="mdi-format-list-numbered"></v-icon>
        </button>

        <button
          :disabled="!editor.can().sinkListItem('listItem')"
          @click="editor.chain().focus().sinkListItem('listItem').run()"
        >
          <v-icon icon="mdi-format-indent-increase"></v-icon>
        </button>
        <button
          :disabled="!editor.can().liftListItem('listItem')"
          @click="editor.chain().focus().liftListItem('listItem').run()"
        >
          <v-icon icon="mdi-format-indent-decrease"></v-icon>
        </button>

        <span class="border-e mx-1 my-2"></span>

        <!-- 插入和操作 -->

        <!-- link -->
        <button :class="{ 'is-active': editor.isActive('link') }" @click="setLink">
          <v-icon icon="mdi-link"></v-icon>
        </button>

        <!-- image -->
        <button @click="addImage">
          <v-icon icon="mdi-image"></v-icon>
        </button>

        <!-- 缩进块 -->

        <!-- code -->
        <button
          :disabled="!editor.can().chain().focus().toggleCode().run()"
          :class="{ 'is-active': editor.isActive('code') }"
          @click="editor.chain().focus().toggleCode().run()"
        >
          <v-icon icon="mdi-code-tags"></v-icon>
        </button>

        <!-- code block -->
        <button
          :class="{ 'is-active': editor.isActive('codeBlock') }"
          @click="editor.chain().focus().toggleCodeBlock().run()"
        >
          <v-icon icon="mdi-code-block-tags"></v-icon>
        </button>

        <button
          :class="{ 'is-active': editor.isActive('blockquote') }"
          @click="editor.chain().focus().toggleBlockquote().run()"
        >
          <v-icon icon="mdi-format-quote-close"></v-icon>
        </button>

        <!-- Horizontal -->
        <button @click="editor.chain().focus().setHorizontalRule().run()">
          <v-icon icon="mdi-minus"></v-icon>
        </button>

        <button
          @click="
            editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
          "
        >
          <v-icon icon="mdi-table-large"></v-icon>
        </button>
      </div>

      <div class="button-group"></div>
    </div>
    <EditorContent :editor="editor" class="mb-4 editor-content" />
  </div>
</template>

<style lang="scss">
  @use '@/styles/style.scss' as *;

  .sticky-toolbar {
    position: sticky;
    top: 0px;
    background-color: #fff;
    z-index: 1;
  }

  .color-input {
    height: 32px;
    width: 42px;
  }

  .editor-content {
    min-height: 980px;
    border-left: 0px solid #ddd;
  }

  /* Basic editor styles */
  .tiptap {
    :first-child {
      margin-top: 0;
    }

    /* List styles */
    ul,
    ol {
      padding: 0 1rem;
      margin: 1.25rem 1rem 1.25rem 0.4rem;

      li p {
        margin-top: 0.25em;
        margin-bottom: 0.25em;
      }
    }

    /* Heading styles */
    h1,
    h2,
    h3,
    h4,
    h5,
    h6 {
      line-height: 1.1;
      margin-top: 2.5rem;
      text-wrap: pretty;
    }

    h1,
    h2 {
      margin-top: 3.5rem;
      margin-bottom: 1.5rem;
    }

    h1 {
      font-size: 1.4rem;
    }

    h2 {
      font-size: 1.2rem;
    }

    h3 {
      font-size: 1.1rem;
    }

    h4,
    h5,
    h6 {
      font-size: 1rem;
    }

    /* Code and preformatted text styles */
    code {
      background-color: var(--purple-light);
      border-radius: 0.4rem;
      color: var(--black);
      font-size: 0.85rem;
      padding: 0.25em 0.3em;
    }

    pre {
      background: var(--black);
      border-radius: 0.5rem;
      color: var(--white);
      font-family: 'JetBrainsMono', monospace;
      margin: 1.5rem 0;
      padding: 0.75rem 1rem;

      code {
        background: none;
        color: inherit;
        font-size: 0.8rem;
        padding: 0;
      }

      /* Code styling */
      .hljs-comment,
      .hljs-quote {
        color: #616161;
      }

      .hljs-variable,
      .hljs-template-variable,
      .hljs-attribute,
      .hljs-tag,
      .hljs-name,
      .hljs-regexp,
      .hljs-link,
      .hljs-name,
      .hljs-selector-id,
      .hljs-selector-class {
        color: #f98181;
      }

      .hljs-number,
      .hljs-meta,
      .hljs-built_in,
      .hljs-builtin-name,
      .hljs-literal,
      .hljs-type,
      .hljs-params {
        color: #fbbc88;
      }

      .hljs-string,
      .hljs-symbol,
      .hljs-bullet {
        color: #b9f18d;
      }

      .hljs-title,
      .hljs-section {
        color: #faf594;
      }

      .hljs-keyword,
      .hljs-selector-tag {
        color: #70cff8;
      }

      .hljs-emphasis {
        font-style: italic;
      }

      .hljs-strong {
        font-weight: 700;
      }
    }

    blockquote {
      border-left: 3px solid var(--gray-3);
      margin: 1.5rem 0;
      padding-left: 1rem;
    }

    hr {
      border: none;
      border-top: 1px solid var(--gray-2);
      cursor: pointer;
      margin: 2rem 0;

      &.ProseMirror-selectednode {
        border-top: 1px solid var(--purple);
      }
    }

    mark {
      background-color: #faf594;
      border-radius: 0.4rem;
      box-decoration-break: clone;
      padding: 0.1rem 0.3rem;
    }

    a {
      color: var(--teal-dark);
      cursor: pointer;

      &:hover {
        color: var(--blue-dark);
      }
    }

    p {
      margin-top: 1rem;
    }

    img {
      display: block;
      height: auto;
      margin: 1.5rem 0;
      max-width: 100%;

      &.ProseMirror-selectednode {
        outline: 3px solid var(--purple);
      }
    }

    table {
      border-collapse: collapse;
      margin: 0;
      overflow: hidden;
      table-layout: fixed;
      width: 100%;

      td,
      th {
        border: 1px solid var(--gray-3);
        box-sizing: border-box;
        min-width: 1em;
        padding: 6px 8px;
        position: relative;
        vertical-align: top;

        > * {
          margin-bottom: 0;
        }
      }

      th {
        background-color: var(--gray-1);
        font-weight: bold;
        text-align: left;
      }

      .selectedCell:after {
        background: var(--gray-2);
        content: '';
        left: 0;
        right: 0;
        top: 0;
        bottom: 0;
        pointer-events: none;
        position: absolute;
        z-index: 2;
      }

      .column-resize-handle {
        background-color: var(--purple);
        bottom: -2px;
        pointer-events: none;
        position: absolute;
        right: -2px;
        top: 0;
        width: 4px;
      }
    }

    .tableWrapper {
      margin: 1.5rem 0;
      overflow-x: auto;
    }

    &.resize-cursor {
      cursor: ew-resize;
      cursor: col-resize;
    }

    p.is-editor-empty:first-child::before {
      color: var(--gray-4);
      content: attr(data-placeholder);
      float: left;
      height: 0;
      pointer-events: none;
    }

    .Tiptap-mathematics-editor {
      background: #202020;
      color: #fff;
      font-family: monospace;
      padding: 0.2rem 0.5rem;
    }

    .Tiptap-mathematics-render {
      padding: 0 0.25rem;

      &--editable {
        cursor: pointer;
        transition: background 0.2s;

        &:hover {
          background: #eee;
        }
      }
    }

    .Tiptap-mathematics-editor,
    .Tiptap-mathematics-render {
      border-radius: 0.25rem;
      display: inline-block;
    }

    [aria-hidden='true'] {
      display: none;
    }
  }
</style>