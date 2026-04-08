/**
 * TipTap 数学公式扩展
 * 使用 KaTeX 渲染 LaTeX 公式
 */

import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import { defineComponent, h, ref, watch, onMounted } from 'vue'
import { NodeViewWrapper } from '@tiptap/vue-3'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import i18n from '@/i18n'

/**
 * 数学公式 Vue 组件
 */
const MathBlockComponent = defineComponent({
  name: 'MathBlockComponent',
  props: {
    node: {
      type: Object,
      required: true,
    },
    updateAttributes: {
      type: Function,
      required: true,
    },
    selected: {
      type: Boolean,
      default: false,
    },
    deleteNode: {
      type: Function,
      required: true,
    },
    editor: {
      type: Object,
      required: true,
    },
  },
  setup(props) {
    const renderedHtml = ref('')
    const hasError = ref(false)

    const renderMath = () => {
      const formula = props.node.attrs.formula || ''
      const displayMode = props.node.attrs.displayMode ?? true

      if (!formula.trim()) {
        renderedHtml.value = `<span class="math-placeholder">${i18n.global.t('editor.mathPlaceholder')}</span>`
        hasError.value = false
        return
      }

      try {
        renderedHtml.value = katex.renderToString(formula, {
          displayMode,
          throwOnError: false,
        })
        hasError.value = false
      } catch (e) {
        renderedHtml.value = `<span class="math-error">${formula}</span>`
        hasError.value = true
      }
    }

    const handleClick = () => {
      // 触发自定义事件，通知编辑器打开编辑对话框
      const event = new CustomEvent('tiptap-edit-math', {
        detail: {
          formula: props.node.attrs.formula,
          displayMode: props.node.attrs.displayMode,
          updateAttributes: props.updateAttributes,
          deleteNode: props.deleteNode,
        },
      })
      window.dispatchEvent(event)
    }

    onMounted(renderMath)
    watch(() => props.node.attrs.formula, renderMath)
    watch(() => props.node.attrs.displayMode, renderMath)

    return () => {
      const displayMode = props.node.attrs.displayMode ?? true
      return h(
        NodeViewWrapper,
        {
          class: [
            'math-node',
            displayMode ? 'math-block' : 'math-inline',
            { 'math-selected': props.selected },
            { 'math-error-container': hasError.value },
          ],
          onClick: handleClick,
        },
        () => h('span', { innerHTML: renderedHtml.value })
      )
    }
  },
})

/**
 * 数学公式扩展
 */
export const MathBlock = Node.create({
  name: 'mathBlock',

  group: 'inline',

  inline: true,

  atom: true,

  addAttributes() {
    return {
      formula: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-formula') || '',
        renderHTML: (attributes) => ({
          'data-formula': attributes.formula,
        }),
      },
      displayMode: {
        default: true,
        parseHTML: (element) => element.getAttribute('data-display-mode') !== 'false',
        renderHTML: (attributes) => ({
          'data-display-mode': attributes.displayMode ? 'true' : 'false',
        }),
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'span[data-type="math-block"]',
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return ['span', mergeAttributes(HTMLAttributes, { 'data-type': 'math-block' })]
  },

  addNodeView() {
    return VueNodeViewRenderer(MathBlockComponent)
  },

  addCommands() {
    return {
      insertMath:
        (options: { formula: string; displayMode?: boolean }) =>
        ({ commands }) => {
          return commands.insertContent({
            type: this.name,
            attrs: {
              formula: options.formula,
              displayMode: options.displayMode ?? true,
            },
          })
        },
    }
  },
})
