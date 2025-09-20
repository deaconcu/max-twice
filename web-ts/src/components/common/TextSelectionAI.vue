<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

// 文本选择 AI 组件：
// - 用户选中文本后显示 AI 按钮
// - 点击按钮后在文章左侧对齐位置展示浮层
// - 显示引用区域竖线与可拖拽手柄，按段落吸附扩展引用范围

// 可见性与定位
const visible = ref(false)
const panelOpen = ref(false)
const top = ref(0)
const left = ref(0)
// 当前选中文本与上下文片段
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
const dragging = ref<'top' | 'bottom' | null>(null)
const initialStartEl = ref<HTMLElement | null>(null)

// 向父组件发出的查询事件载荷
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
  initialStartEl.value = null
  indicatorTop.value = 0
  indicatorLeft.value = 0
  indicatorHeight.value = 0
}

// 计算文章容器左边界（用于左对齐浮层）
const computeArticleLeft = () => {
  const el = document.querySelector(ARTICLE_SELECTOR) as Element | null
  return el ? el.getBoundingClientRect().left + window.scrollX : window.scrollX
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

// 更新引用竖线与手柄位置；面板跟随引用下边界
const updateIndicator = () => {
  const startEl = regionStartEl.value || selectedBlockEl.value || findSelectedBlock()
  const endEl = regionEndEl.value || startEl
  if (startEl && endEl) {
    const startRect = startEl.getBoundingClientRect()
    const endRect = endEl.getBoundingClientRect()
    indicatorTop.value = startRect.top + window.scrollY
    indicatorLeft.value = startRect.left + window.scrollX - 20
    indicatorHeight.value = Math.max(12, endRect.bottom - startRect.top)
    if (panelOpen.value) top.value = indicatorTop.value + indicatorHeight.value + 8
    return
  }
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return
  const rect = sel.getRangeAt(0).getBoundingClientRect()
  indicatorTop.value = rect.top + window.scrollY
  indicatorLeft.value = articleLeft.value - 20
  indicatorHeight.value = Math.max(12, rect.height)
  if (panelOpen.value) top.value = indicatorTop.value + indicatorHeight.value + 8
}

// 侦听浏览器选区并在选区底部显示 AI 按钮
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
  top.value = rect.bottom + 8
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
    articleLeft.value = computeArticleLeft()
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
  initialStartEl.value = selectedBlockEl.value
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
  window.removeEventListener('mousemove', onDragMove as any)
  window.removeEventListener('mouseup', onDragEnd as any)
})

const getBlocksInSameParent = (el: HTMLElement | null) => {
  if (!el || !el.parentElement) return [] as HTMLElement[]
  const children = Array.from(el.parentElement.children) as HTMLElement[]
  return children.filter(isValidBlock) as HTMLElement[]
}

const findBlockByYInList = (list: HTMLElement[], y: number) => {
  if (!list.length) return null
  for (const el of list) {
    const r = el.getBoundingClientRect()
    if (y >= r.top && y <= r.bottom) return el
  }
  let nearest: HTMLElement | null = null
  let best = Infinity
  for (const el of list) {
    const r = el.getBoundingClientRect()
    const dy = Math.min(Math.abs(y - r.top), Math.abs(y - r.bottom))
    if (dy < best) { best = dy; nearest = el }
  }
  return nearest
}

// 拖拽中：按段落吸附，限制上下手柄不可越过初始段落
const onDragMove = (e: MouseEvent) => {
  if (!dragging.value) return
  if (dragging.value === 'top') {
    const list = getBlocksInSameParent(regionEndEl.value || selectedBlockEl.value)
    let target = findBlockByYInList(list, e.clientY)
    if (target && initialStartEl.value) {
      const initIdx = list.indexOf(initialStartEl.value)
      const tIdx = list.indexOf(target)
      if (initIdx >= 0 && tIdx > initIdx) target = initialStartEl.value
    }
    if (target && target !== regionStartEl.value) {
      regionStartEl.value = target
      updateIndicator()
      contextText.value = collectRegionText()
    }
  } else if (dragging.value === 'bottom') {
    const list = getBlocksInSameParent(regionStartEl.value || selectedBlockEl.value)
    let target = findBlockByYInList(list, e.clientY)
    if (target && initialStartEl.value) {
      const initIdx = list.indexOf(initialStartEl.value)
      const tIdx = list.indexOf(target)
      if (initIdx >= 0 && tIdx < initIdx) target = initialStartEl.value
    }
    if (target && target !== regionEndEl.value) {
      regionEndEl.value = target
      updateIndicator()
      contextText.value = collectRegionText()
    }
  }
}

const onDragEnd = () => {
  if (!dragging.value) return
  dragging.value = null
  window.removeEventListener('mousemove', onDragMove as any)
  window.removeEventListener('mouseup', onDragEnd as any)
}

// 开始拖拽（上手柄）
const onDragStartTop = (e: MouseEvent) => {
  e.preventDefault(); e.stopPropagation()
  dragging.value = 'top'
  window.addEventListener('mousemove', onDragMove as any)
  window.addEventListener('mouseup', onDragEnd as any)
}

// 开始拖拽（下手柄）
const onDragStartBottom = (e: MouseEvent) => {
  e.preventDefault(); e.stopPropagation()
  dragging.value = 'bottom'
  window.addEventListener('mousemove', onDragMove as any)
  window.addEventListener('mouseup', onDragEnd as any)
}
</script>

<template>
  <div v-show="visible">
    <template v-if="!panelOpen">
      <v-btn class="ai-button" size="small" color="primary" variant="flat" rounded="lg" @click="onClick" :style="{ top: top + 'px', left: left + 'px' }">
        AI
      </v-btn>
    </template>
  </div>
  <teleport to="body" v-if="panelOpen && visible">
    <div class="ai-indicator" :style="{ top: indicatorTop + 'px', left: indicatorLeft + 'px', height: indicatorHeight + 'px' }" />
    <div class="ai-handle ai-handle--top" :style="{ top: (indicatorTop - 10) + 'px', left: (indicatorLeft - 28) + 'px' }" @mousedown.stop="onDragStartTop">
      <div class="ai-grip-line" />
      <div class="ai-grip-line" />
      <div class="ai-grip-line" />
    </div>
    <div class="ai-handle ai-handle--bottom" :style="{ top: (indicatorTop + indicatorHeight - 10) + 'px', left: (indicatorLeft - 28) + 'px' }" @mousedown.stop="onDragStartBottom">
      <div class="ai-grip-line" />
      <div class="ai-grip-line" />
      <div class="ai-grip-line" />
    </div>
    <v-card class="ai-panel" elevation="8" :style="{ top: top + 'px', left: articleLeft + 'px' }">
      <v-card-text>
        <div class="panel-header">
          <div class="title">AI 助手</div>
          <div class="actions">
            <v-btn density="comfortable" variant="text" color="default" @click="closePanel">关闭</v-btn>
          </div>
        </div>
        <v-text-field
          v-model="question"
          variant="outlined"
          density="comfortable"
          placeholder="请输入问题并按回车发送"
          hide-details
          class="mt-2"
          @keyup.enter="emit('query', {
            question: question,
            context: contextText,
            selectedText: selectedText
          })"
        />
      </v-card-text>
    </v-card>
  </teleport>
</template>

<style scoped>
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  font-weight: 600;
}
.actions {
  display: flex;
  gap: 8px;
}
.ai-button {
  position: fixed;
  transform: translate(-50%, 0);
  z-index: 900;
}
.ai-indicator {
  position: absolute;
  width: 6px;
  background: #9e9e9e;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.06);
  border-radius: 3px;
  z-index: 900;
  pointer-events: none;
}
.ai-handle {
  position: absolute;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  background: #fff;
  border: 2px solid #9e9e9e;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: ns-resize;
  z-index: 900;
  gap: 2px;
  flex-direction: column;
}
.ai-panel {
  position: absolute;
  width: 640px;
  z-index: 900;
}
.ai-grip-line {
  width: 12px;
  height: 2px;
  background: #9e9e9e;
}

</style>
