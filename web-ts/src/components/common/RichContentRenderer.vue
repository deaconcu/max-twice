<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import renderMathInElement from 'katex/contrib/auto-render'
import 'katex/dist/katex.min.css'
import mermaid from 'mermaid'

type Emits = {
  (e: 'rendered', el: HTMLElement): void
}

const props = defineProps<{ html: string }>()
const emit = defineEmits<Emits>()

const rootEl = ref<HTMLElement | null>(null)
let mermaidInitialized = false
let mermaidIdCounter = 0

const normalizeMermaidDefinition = (raw: string) => {
  let normalized = raw
    .replace(/\r\n?/g, '\n')
    .replace(/\u00a0/g, ' ')
    .trim()

  const firstLineMatch = normalized.match(/^(graph\s+[^\s]+)(.*)$/i)
  if (firstLineMatch) {
    const graphLine = firstLineMatch[1]
    const rest = firstLineMatch[2]?.trimStart() ?? ''
    normalized = rest ? `${graphLine}\n${rest}` : graphLine
  }

  normalized = normalized
    .split('\n')
    .map((line) => line.trim())
    .join('\n')
  const lines = normalized
    .split(';')
    .map((segment) => segment.trim())
    .filter(Boolean)
  normalized = lines.join('\n')
  normalized = normalized.replace(/\n{2,}/g, '\n')

  const wrapLabelText = (value: string, open: string, close: string) =>
    value.replace(new RegExp(`\\${open}([^\\${close}]*)\\${close}`, 'g'), (match, inner) => {
      const trimmed = inner.trim()
      if (!trimmed) {
        return `${open}${close}`
      }
      if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
        return `${open}${trimmed}${close}`
      }
      const escaped = trimmed.replace(/"/g, '\\"')
      return `${open}"${escaped}"${close}`
    })

  normalized = wrapLabelText(normalized, '[', ']')
  normalized = wrapLabelText(normalized, '{', '}')

  return normalized
}

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

const ensureMermaidInitialized = () => {
  if (!mermaidInitialized) {
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'loose',
      theme: 'neutral',
      look: 'handDrawn',
      htmlLabels: false,
      fontFamily: 'Arial, sans-serif',
      flowchart: { htmlLabels: true, useMaxWidth: true },
      sequence: { noteFontFamily: 'Arial, sans-serif' }
    })
    mermaidInitialized = true
  }
}

const highlightCodeBlocks = (root: HTMLElement) => {
  root.querySelectorAll('pre code').forEach((block) => {
    const element = block as HTMLElement
    if (element.classList.contains('language-mermaid')) {
      return
    }
    hljs.highlightElement(element)
  })

  root.querySelectorAll(':not(pre) > code').forEach((inlineCode) => {
    const element = inlineCode as HTMLElement
    if (element.classList.contains('language-mermaid')) {
      return
    }
    hljs.highlightElement(element)
  })
}

const renderMathBlocks = (root: HTMLElement) => {
  renderMathInElement(root, {
    delimiters: [
      { left: '$$', right: '$$', display: true },
      { left: '\\[', right: '\\]', display: true },
      { left: '\\(', right: '\\)', display: false },
      { left: '$', right: '$', display: false}
    ],
    throwOnError: false
  })
}

const convertToMermaidContainers = (root: HTMLElement) => {
  const mermaidCodeBlocks = root.querySelectorAll('pre code.language-mermaid')
  mermaidCodeBlocks.forEach((block) => {
    const parentPre = block.parentElement
    const definition = block.textContent || ''
    const container = document.createElement('div')
    container.className = 'mermaid'
    container.textContent = normalizeMermaidDefinition(definition)
    if (parentPre) {
      parentPre.replaceWith(container)
    } else {
      block.replaceWith(container)
    }
  })

  const blockCandidates = root.querySelectorAll('p, div')
  blockCandidates.forEach((node) => {
    const element = node as HTMLElement
    if (element.classList.contains('mermaid') || element.dataset.processed) {
      return
    }
    if (element.childNodes.length !== 1 || element.firstChild?.nodeType !== 3) {
      return
    }
    const text = element.textContent?.trim()
    if (!text) return
    if (!/^(graph|sequenceDiagram|classDiagram|stateDiagram|erDiagram|journey|gantt|timeline|pie)/i.test(text)) {
      return
    }
    const container = document.createElement('div')
    container.className = 'mermaid'
    container.textContent = normalizeMermaidDefinition(text)
    element.replaceWith(container)
  })
}

const renderMermaidDiagrams = async (root: HTMLElement) => {
  ensureMermaidInitialized()
  convertToMermaidContainers(root)

  const mermaidElements = Array.from(root.querySelectorAll<HTMLElement>('.mermaid'))
  await Promise.all(
    mermaidElements.map(async (el) => {
      if (el.dataset.processed === 'true') {
        return
      }
      const definition = el.textContent?.trim()
      if (!definition) {
        return
      }
      const normalized = normalizeMermaidDefinition(definition)
      try {
        const { svg } = await mermaid.render(`mermaid-${mermaidIdCounter++}`, normalized)
        el.innerHTML = svg
        el.dataset.processed = 'true'
      } catch (error) {
        console.error('Mermaid render failed:', error, definition)
        el.dataset.processed = 'error'
        el.innerHTML = `<pre class="mermaid-error">${escapeHtml(normalized)}</pre>`
      }
    })
  )
}

const applyEnhancements = async () => {
  await nextTick()
  const el = rootEl.value
  if (!el) {
    return
  }

  highlightCodeBlocks(el)
  renderMathBlocks(el)
  await renderMermaidDiagrams(el)
  emit('rendered', el)
}

onMounted(() => {
  applyEnhancements()
})

watch(
  () => props.html,
  () => {
    applyEnhancements()
  }
)

defineExpose({ rootEl })
</script>

<template>
  <div ref="rootEl" class="rich-content" v-html="props.html"></div>
</template>

<style scoped>
.rich-content {
  width: 100%;
}

.rich-content :deep(.katex-display) {
  overflow-x: auto;
  overflow-y: hidden;
}

/* 行内公式样式 */
.rich-content :deep(.katex) {
  font-size: inherit;
  padding: 0px 4px 0 4px;
}

.rich-content :deep(.katex-html) {
  display: inline-block;
  vertical-align: baseline;
}

.rich-content :deep(.mermaid) {
  padding: 12px 0 0 0;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
}

.rich-content :deep(.mermaid > svg) {
  max-width: 100%;
}

.rich-content :deep(.mermaid-error) {
  background: #fff7f7;
  color: #c62828;
  border: 1px solid #ffcdd2;
  border-radius: 6px;
  padding: 12px;
  white-space: pre-wrap;
  font-size: 13px;
  line-height: 1.4;
}

.rich-content :deep(.mermaid svg) {
  overflow: visible;
}
.rich-content :deep(.mermaid foreignObject) {
  overflow: visible;
}
.rich-content :deep(.mermaid text) {
  font-family: Arial, sans-serif;
}
</style>
