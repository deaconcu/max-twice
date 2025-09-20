<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

const visible = ref(false)
const panelOpen = ref(false)
const top = ref(0)
const left = ref(0)
const selectedText = ref('')
const contextText = ref('')
const question = ref('')
const ARTICLE_SELECTOR = '.article'
const articleLeft = ref(0)
const selectedBlockEl = ref<HTMLElement | null>(null)
const regionStartEl = ref<HTMLElement | null>(null)
const regionEndEl = ref<HTMLElement | null>(null)
const indicatorTop = ref(0)
const indicatorLeft = ref(0)
const indicatorHeight = ref(0)

interface QueryPayload {
  question: string
  context: string
  selectedText: string
}

const emit = defineEmits<{
  (e: 'query', payload: QueryPayload): void
}>()

const hide = () => {
  visible.value = false
  panelOpen.value = false
  selectedText.value = ''
  contextText.value = ''
  question.value = ''
  selectedBlockEl.value = null
  regionStartEl.value = null
  regionEndEl.value = null
  indicatorTop.value = 0
  indicatorLeft.value = 0
  indicatorHeight.value = 0
}

const computeArticleLeft = () => {
  const el = document.querySelector(ARTICLE_SELECTOR) as Element | null
  return el ? el.getBoundingClientRect().left : 0
}

const findSelectedBlock = () => {
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return null
  const range = sel.getRangeAt(0)
  const node = range.commonAncestorContainer
  const base = node.nodeType === 1 ? (node as Element) : (node.parentElement as Element | null)
  const el = base && 'closest' in base ? (base as HTMLElement).closest('p,li,h1,h2,h3,h4,h5,h6,blockquote,pre,div') as HTMLElement | null : null
  return el
}

const isValidBlock = (el: Element | null) => {
  if (!el) return false
  const tag = el.tagName.toLowerCase()
  return ['p','li','h1','h2','h3','h4','h5','h6','blockquote','pre','div'].includes(tag)
}

const findPrevBlock = (el: HTMLElement | null) => {
  if (!el) return null
  let cur: Element | null = el.previousElementSibling
  while (cur && !isValidBlock(cur)) cur = cur.previousElementSibling
  return cur as HTMLElement | null
}

const findNextBlock = (el: HTMLElement | null) => {
  if (!el) return null
  let cur: Element | null = el.nextElementSibling
  while (cur && !isValidBlock(cur)) cur = cur.nextElementSibling
  return cur as HTMLElement | null
}

const getArticleEl = () => document.querySelector(ARTICLE_SELECTOR) as HTMLElement | null

const collectRegionText = () => {
  if (!regionStartEl.value || !regionEndEl.value) return contextText.value
  const article = getArticleEl()
  if (!article) return contextText.value
  const parts: string[] = []
  let cur: HTMLElement | null = regionStartEl.value
  while (cur) {
    parts.push((cur.textContent || '').replace(/\s+/g, ' ').trim())
    if (cur === regionEndEl.value) break
    cur = cur.nextElementSibling as HTMLElement | null
  }
  return parts.join(' ').trim()
}

const updateIndicator = () => {
  const startEl = regionStartEl.value || selectedBlockEl.value || findSelectedBlock()
  const endEl = regionEndEl.value || startEl
  if (startEl && endEl) {
    const startRect = startEl.getBoundingClientRect()
    const endRect = endEl.getBoundingClientRect()
    indicatorTop.value = Math.max(0, startRect.top)
    indicatorLeft.value = Math.max(0, startRect.left - 20)
    indicatorHeight.value = Math.max(12, endRect.bottom - startRect.top)
    return
  }
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return
  const rect = sel.getRangeAt(0).getBoundingClientRect()
  indicatorTop.value = Math.max(0, rect.top)
  indicatorLeft.value = Math.max(0, articleLeft.value - 20)
  indicatorHeight.value = Math.max(12, rect.height)
}

const update = () => {
  if (panelOpen.value) return
  const sel = window.getSelection()
  if (!sel || sel.isCollapsed || sel.rangeCount === 0) {
    visible.value = false
    return
  }
  const text = sel.toString().trim()
  if (!text) {
    visible.value = false
    return
  }
  const range = sel.getRangeAt(0)
  const rect = range.getBoundingClientRect()
  if (!rect || (rect.width === 0 && rect.height === 0)) {
    visible.value = false
    return
  }
  selectedText.value = text
  const node = range.commonAncestorContainer
  const container = node.nodeType === 1 ? (node as Element) : (node.parentElement as Element | null)
  const raw = (container?.textContent || '').replace(/\s+/g, ' ').trim()
  const idx = raw.indexOf(text)
  const start = Math.max(0, idx - 300)
  const end = Math.min(raw.length, idx + text.length + 300)
  contextText.value = raw.slice(start, end)
  top.value = Math.max(0, rect.bottom + 8)
  left.value = Math.max(0, rect.left + rect.width / 2)
  visible.value = true
}

const onSelectionChange = () => {
  if (panelOpen.value) return
  update()
}
const onMouseUp = () => {
  if (panelOpen.value) return
  setTimeout(update, 0)
}
const onKeyUp = () => {
  if (panelOpen.value) return
  setTimeout(update, 0)
}
const onScroll = () => {
  if (panelOpen.value) {
    updateIndicator()
    return
  }
  visible.value = false
}
const onResize = () => {
  if (panelOpen.value) {
    articleLeft.value = computeArticleLeft()
    updateIndicator()
    return
  }
  visible.value = false
}

const onClick = (e: MouseEvent) => {
  e.stopPropagation()
  if (!selectedText.value) return
  panelOpen.value = true
  question.value = selectedText.value
  articleLeft.value = computeArticleLeft()
  selectedBlockEl.value = findSelectedBlock()
  regionStartEl.value = selectedBlockEl.value
  regionEndEl.value = selectedBlockEl.value
  updateIndicator()
}

const closePanel = () => hide()

onMounted(() => {
  document.addEventListener('selectionchange', onSelectionChange)
  document.addEventListener('mouseup', onMouseUp)
  document.addEventListener('keyup', onKeyUp)
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  document.removeEventListener('selectionchange', onSelectionChange)
  document.removeEventListener('mouseup', onMouseUp)
  document.removeEventListener('keyup', onKeyUp)
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onResize)
})
const expandUp = () => {
  const prev = findPrevBlock(regionStartEl.value as HTMLElement | null)
  if (prev) {
    regionStartEl.value = prev
    updateIndicator()
    contextText.value = collectRegionText()
  }
}

const expandDown = () => {
  const next = findNextBlock(regionEndEl.value as HTMLElement | null)
  if (next) {
    regionEndEl.value = next
    updateIndicator()
    contextText.value = collectRegionText()
  }
}
</script>

<template>
  <div
    v-show="visible"
    :style="{
      position: 'fixed',
      top: top + 'px',
      left: (panelOpen ? articleLeft : left) + 'px',
      transform: panelOpen ? 'none' : 'translate(-50%, 0)',
      zIndex: 3000 as any,
      pointerEvents: 'auto'
    }"
  >
    <template v-if="!panelOpen">
      <v-btn size="small" color="primary" variant="flat" rounded="lg" @click="onClick">
        AI
      </v-btn>
    </template>
    <template v-else>
      <div :style="{ position: 'fixed', top: indicatorTop + 'px', left: indicatorLeft + 'px', width: '6px', height: indicatorHeight + 'px', background: '#ff3b30', boxShadow: '0 0 0 1px rgba(0,0,0,0.06)', borderRadius: '3px', zIndex: 2147483647 as any, pointerEvents: 'none' }" />
      <v-btn size="x-small" variant="tonal" density="comfortable" :style="{ position: 'fixed', top: (indicatorTop - 18) + 'px', left: (indicatorLeft - 12) + 'px', zIndex: 2147483647 as any }" @click.stop="expandUp">上</v-btn>
      <v-btn size="x-small" variant="tonal" density="comfortable" :style="{ position: 'fixed', top: (indicatorTop + indicatorHeight + 4) + 'px', left: (indicatorLeft - 12) + 'px', zIndex: 2147483647 as any }" @click.stop="expandDown">下</v-btn>
      <v-card elevation="8" :style="{ width: '640px' }">
        <v-card-text>
          <div class="panel-header">
            <div class="title">AI 助手</div>
            <div class="actions">
              <v-btn density="comfortable" variant="text" color="default" @click="closePanel">关闭</v-btn>
            </div>
          </div>
          <v-text-field v-model="question" variant="outlined" density="comfortable" placeholder="请输入问题并按回车发送" hide-details class="mt-2" @keyup.enter="emit('query', { question: question, context: contextText, selectedText: selectedText })" />
        </v-card-text>
      </v-card>
    </template>
  </div>
</template>

<style scoped>
.panel-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-weight: 600; }
.actions { display: flex; gap: 8px; }
</style>
