/**
 * TipTap Mermaid 图表扩展
 * 使用 Mermaid 渲染流程图等
 */

import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer, NodeViewWrapper } from '@tiptap/vue-3'
import { defineComponent, h, ref, watch, onMounted, nextTick } from 'vue'
import mermaid from 'mermaid'
import i18n from '@/i18n'

// 初始化 Mermaid
let mermaidInitialized = false
const initMermaid = () => {
  if (!mermaidInitialized) {
    mermaid.initialize({
      startOnLoad: false,
      theme: 'default',
      securityLevel: 'loose',
    })
    mermaidInitialized = true
  }
}

// 全局 ID 计数器
let mermaidIdCounter = 0

/**
 * Mermaid 图表 Vue 组件
 */
const MermaidBlockComponent = defineComponent({
  name: 'MermaidBlockComponent',
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
  },
  setup(props) {
    const renderedHtml = ref('')
    const hasError = ref(false)

    const renderMermaid = async () => {
      const code = props.node.attrs.code || ''

      if (!code.trim()) {
        renderedHtml.value = `<div class="mermaid-placeholder">${i18n.global.t('editor.mermaidPlaceholder')}</div>`
        hasError.value = false
        return
      }

      initMermaid()

      try {
        const id = `mermaid-editor-${mermaidIdCounter++}`
        const { svg } = await mermaid.render(id, code.trim())
        renderedHtml.value = svg
        hasError.value = false
      } catch (e: any) {
        renderedHtml.value = `<pre class="mermaid-error">${code}</pre>`
        hasError.value = true
      }
    }

    const handleClick = () => {
      // 触发自定义事件，通知编辑器打开编辑对话框
      const event = new CustomEvent('tiptap-edit-mermaid', {
        detail: {
          code: props.node.attrs.code,
          updateAttributes: props.updateAttributes,
          deleteNode: props.deleteNode,
        },
      })
      window.dispatchEvent(event)
    }

    onMounted(() => {
      nextTick(renderMermaid)
    })

    watch(
      () => props.node.attrs.code,
      () => {
        nextTick(renderMermaid)
      }
    )

    return () => {
      return h(
        NodeViewWrapper,
        {
          class: [
            'mermaid-node',
            { 'mermaid-selected': props.selected },
            { 'mermaid-error-container': hasError.value },
          ],
          onClick: handleClick,
        },
        () => h('div', { innerHTML: renderedHtml.value })
      )
    }
  },
})

/**
 * Mermaid 图表扩展
 */
export const MermaidBlock = Node.create({
  name: 'mermaidBlock',

  group: 'block',

  atom: true,

  addAttributes() {
    return {
      code: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-code') || '',
        renderHTML: (attributes) => ({
          'data-code': attributes.code,
        }),
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'div[data-type="mermaid-block"]',
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'mermaid-block' })]
  },

  addNodeView() {
    return VueNodeViewRenderer(MermaidBlockComponent)
  },

  addCommands() {
    return {
      insertMermaid:
        (options: { code: string }) =>
        ({ commands }) => {
          return commands.insertContent({
            type: this.name,
            attrs: {
              code: options.code,
            },
          })
        },
    }
  },
})
