<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import TurndownService from 'turndown'
import { gfm } from 'turndown-plugin-gfm'

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
const ARTICLE_SELECTOR = '.full-article'
const articleLeft = ref(0)
const selectedBlockEl = ref<HTMLElement | null>(null)
const regionStartEl = ref<HTMLElement | null>(null)
const regionEndEl = ref<HTMLElement | null>(null)
const indicatorTop = ref(0)
const indicatorLeft = ref(0)
const indicatorHeight = ref(0)
const dragging = ref<'top' | 'bottom' | null>(null)
const initialStartEl = ref<HTMLElement | null>(null)
const ownerKey = '__TextSelectionAIOwner__'
const myId = Math.random().toString(36).slice(2)

// 内部缓存的引用 Markdown 内容
const mdRegion = ref('')
const buildMdRegion = () => {
  const html = collectRegionHtml()
  let md = contextText.value
  if (html) {
    try {
      const clean = normalizeHtml(html)
      const service = new TurndownService({
        headingStyle: 'atx',
        codeBlockStyle: 'fenced',
        bulletListMarker: '-',
        hr: '---'
      })
      service.use(gfm)
      md = service.turndown(clean)
    } catch (_) {}
  }
  mdRegion.value = md || ''
}

// 向父组件发出的查询事件载荷
// 复制到剪贴板（Markdown）；使用 turndown 将 HTML 转为 Markdown
// 转换前会规范化表格 HTML，避免换行/样式导致的 Markdown 解析混乱
const localTip = ref('')
let tipTimer: number | null = null
const onCopy = async () => {
  buildMdRegion()
  const prompt = question.value || `请解释上面引用中的内容：${selectedText.value}`
  const content = `${mdRegion.value}\n\n${prompt}`
  try {
    await navigator.clipboard.writeText(content)
    localTip.value = '上下文和问题均已复制，您可以去 AI 引擎粘贴提问了'
  } catch (_) {
    localTip.value = '复制失败，请重试'
  }
  if (tipTimer) window.clearTimeout(tipTimer)
  tipTimer = window.setTimeout(() => { localTip.value = '' }, 3000)
}

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
  if ((globalThis as any)[ownerKey] === myId) (globalThis as any)[ownerKey] = null
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
  const startNode = range.startContainer
  const base = startNode.nodeType === 1
    ? (startNode as Element)
    : ((startNode.parentElement as Element | null))
  const articleEl = getArticleEl()

  let cur = base as HTMLElement | null
  let candidate: HTMLElement | null = null
  while (cur && cur !== articleEl) {
    if (isValidBlock(cur)) candidate = cur
    cur = cur.parentElement as HTMLElement | null
  }
  if (candidate) return candidate

  const el = base && 'closest' in base
    ? ((base as HTMLElement).closest('p,li,ul,ol,h1,h2,h3,h4,h5,h6,blockquote,pre,div,table') as HTMLElement | null)
    : null
  if (el && articleEl && el === articleEl) return null
  return el
}

const isValidBlock = (el: Element | null) => {
  if (!el) return false
  const tag = el.tagName.toLowerCase()
  return ['p','li','ul','ol','h1','h2','h3','h4','h5','h6','blockquote','pre','div','table'].includes(tag)
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

// 计算共同祖先（不越过 boundary）
const getCommonAncestor = (a: HTMLElement, b: HTMLElement, boundary: HTMLElement | null) => {
  const seen = new Set<HTMLElement>()
  let cur: HTMLElement | null = a
  while (cur && cur !== boundary) {
    seen.add(cur)
    cur = cur.parentElement
  }
  cur = b
  while (cur && cur !== boundary) {
    if (seen.has(cur)) return cur
    cur = cur.parentElement
  }
  return boundary || document.body
}

// 将元素提升为共同祖先的直接子元素
const liftToChildOf = (el: HTMLElement, ancestor: HTMLElement) => {
  let cur: HTMLElement | null = el
  while (cur && cur.parentElement !== ancestor) cur = cur.parentElement as HTMLElement | null
  return cur || el
}

// 从任意节点向上寻找最近的 table 元素（若存在则优先返回整个表格）
const nearestTable = (node: Node | null) => {
  if (!node) return null
  let el: HTMLElement | null = node.nodeType === 1 ? (node as HTMLElement) : (node.parentElement as HTMLElement | null)
  while (el) {
    if (el.tagName && el.tagName.toLowerCase() === 'table') return el
    el = el.parentElement as HTMLElement | null
  }
  return null
}

// 从任意节点向上寻找最近的列表（ul/ol）元素（若存在则优先返回整个列表）
const nearestList = (node: Node | null) => {
  if (!node) return null
  let el: HTMLElement | null = node.nodeType === 1 ? (node as HTMLElement) : (node.parentElement as HTMLElement | null)
  while (el) {
    if (el.tagName) {
      const t = el.tagName.toLowerCase()
      if (t === 'ul' || t === 'ol') return el
    }
    el = el.parentElement as HTMLElement | null
  }
  return null
}

// 从任意节点向上寻找最近的块级段落元素（优先 table 与 ul/ol 整块）
const blockOfNode = (node: Node | null) => {
  if (!node) return null
  const listEl = nearestList(node)
  if (listEl) return listEl
  const tableEl = nearestTable(node)
  if (tableEl) return tableEl
  const base = node.nodeType === 1 ? (node as Element) : (node.parentElement as Element | null)
  const articleEl = getArticleEl()
  let cur = base as HTMLElement | null
  let candidate: HTMLElement | null = null
  while (cur && cur !== articleEl) {
    if (isValidBlock(cur)) candidate = cur
    cur = cur.parentElement as HTMLElement | null
  }
  if (candidate) return candidate
  return base && 'closest' in base
    ? ((base as HTMLElement).closest('p,li,ul,ol,h1,h2,h3,h4,h5,h6,blockquote,pre,div,table') as HTMLElement | null)
    : null
}

// 收集引用区域的纯文本内容
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

// 收集引用区域的 HTML（用于 turndown 转换）
const collectRegionHtml = () => {
  return collectRegionHtmlNormalized()
}

// 规范化 HTML 以提高 Markdown 转换质量
const collectRegionHtmlNormalized = () => {
  if (!regionStartEl.value || !regionEndEl.value) return ''
  const parts: string[] = []
  let cur: HTMLElement | null = regionStartEl.value
  const doc = document.implementation.createHTMLDocument('norm')
  while (cur) {
    const clone = cur.cloneNode(true) as HTMLElement
    clone.querySelectorAll('colgroup, col').forEach((n) => n.remove())
    clone.querySelectorAll('[style]').forEach((el) => (el as HTMLElement).removeAttribute('style'))
    clone.querySelectorAll('td, th').forEach((cell) => {
      cell.querySelectorAll('br').forEach((br) => br.replaceWith(doc.createTextNode(' ')))
      const text = (cell.textContent || '').replace(/\s+/g, ' ').trim()
      cell.textContent = text
    })
    parts.push(clone.outerHTML)
    if (cur === regionEndEl.value) break
    cur = cur.nextElementSibling as HTMLElement | null
  }
  return parts.join('\n')
}

// 转换前的 HTML 规范化：移除 colgroup/col 与内联 style，提升表格 Markdown 转换质量
const normalizeHtml = (raw: string) => {
  try {
    const doc = new DOMParser().parseFromString(raw, 'text/html')
    doc.querySelectorAll('colgroup, col').forEach((n) => n.remove())
    doc.querySelectorAll('[style]')
      .forEach((el) => (el as HTMLElement).removeAttribute('style'))
    return doc.body.innerHTML
  } catch {
    return raw
  }
}

// 更新引用竖线与手柄位置；面板跟随引用下边界
const updateIndicator = () => {
  const startEl = regionStartEl.value || selectedBlockEl.value || findSelectedBlock()
  const endEl = regionEndEl.value || startEl
  if (startEl && endEl) {
    const startRect = startEl.getBoundingClientRect()
    const endRect = endEl.getBoundingClientRect()
    indicatorTop.value = startRect.top + window.scrollY
    const listEl = nearestList(startEl)
    const baseLeft = listEl ? articleLeft.value : (startRect.left + window.scrollX)
    indicatorLeft.value = baseLeft - 20
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
  if ((globalThis as any)[ownerKey] && (globalThis as any)[ownerKey] !== myId) { visible.value = false; return }
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
  const articleEl = getArticleEl()
  if (!articleEl || !articleEl.contains(range.startContainer) || !articleEl.contains(range.endContainer)) {
    visible.value = false
    return
  }
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
  left.value = rect.left + rect.width / 2
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
  const curr = (globalThis as any)[ownerKey]
  if (curr && curr !== myId) return
  ;(globalThis as any)[ownerKey] = myId
  panelOpen.value = true
  visible.value = false
  question.value = `请解释上面引用中的内容：${selectedText.value}`
  articleLeft.value = computeArticleLeft()
  const sel = window.getSelection()
  const range = sel && sel.rangeCount > 0 ? sel.getRangeAt(0) : null

  // 起止块（优先 table，其次常规块）
  const startBlock = blockOfNode(range?.startContainer || null)
  const endBlock = blockOfNode(range?.endContainer || null)

  if (startBlock && endBlock) {
    // 若不在同一父节点下，提升到共同祖先（限定在文章容器内）后作为兄弟块处理
    const article = getArticleEl()
    const common = getCommonAncestor(startBlock, endBlock, article)
    const startLifted = liftToChildOf(startBlock, common)
    const endLifted = liftToChildOf(endBlock, common)

    const order = startLifted.compareDocumentPosition(endLifted)
    const startFirst = !(order & Node.DOCUMENT_POSITION_PRECEDING)

    regionStartEl.value = startFirst ? startLifted : endLifted
    regionEndEl.value = startFirst ? endLifted : startLifted
    selectedBlockEl.value = regionStartEl.value
    initialStartEl.value = regionStartEl.value
  } else {
    selectedBlockEl.value = startBlock || findSelectedBlock()
    regionStartEl.value = selectedBlockEl.value
    regionEndEl.value = selectedBlockEl.value
    initialStartEl.value = selectedBlockEl.value
  }

  updateIndicator()
  contextText.value = collectRegionText()
  buildMdRegion()
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
  if ((globalThis as any)[ownerKey] === myId) (globalThis as any)[ownerKey] = null
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
      buildMdRegion()
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
      buildMdRegion()
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
// 清空：仅清空问题输入
const onClear = () => {
  question.value = ''
}
</script>

<template>
  <div v-if="visible && !panelOpen">
    <v-btn class="ai-button" size="small" color="primary" variant="flat" rounded="lg" @click="onClick" :style="{ top: top + 'px', left: left + 'px' }">
      创建一个问题
    </v-btn>
  </div>
  <teleport to="body" v-if="panelOpen">
    <div class="ai-indicator" :style="{ top: indicatorTop + 'px', left: indicatorLeft + 'px', height: indicatorHeight + 'px' }" />
    <div class="ai-tag" :style="{ top: (indicatorTop - 40) + 'px', left: (indicatorLeft - 43) + 'px' }">上下文</div>
    <div class="ai-handle ai-handle--top" :style="{ top: (indicatorTop - 10) + 'px', left: (indicatorLeft - 28) + 'px' }" @mousedown.stop="onDragStartTop">
      <div class="ai-grip-line" />
      <div class="ai-grip-line" />
      <div class="ai-grip-line" />
    </div>
    <div class="ai-handle ai-handle--bottom" :style="{ top: (indicatorTop + indicatorHeight - 8) + 'px', left: (indicatorLeft - 28) + 'px' }" @mousedown.stop="onDragStartBottom">
      <div class="ai-grip-line" />
      <div class="ai-grip-line" />
      <div class="ai-grip-line" />
    </div>
    <v-card class="ai-panel rounded-xl" elevation="3" :style="{ top: top + 'px', left: articleLeft + 'px' }">
      <v-card-text>
        <div class="panel-header">
          <div class="title text-body-1">在这里输入问题</div>
          <div class="actions">
            <v-btn density="comfortable" variant="text" color="default" @click="onClear">
              清空
            </v-btn>
            <v-btn density="comfortable" variant="text" color="default" @click="onCopy">
              复制
            </v-btn>
            <v-btn density="comfortable" variant="text" color="default" @click="closePanel">
              关闭
            </v-btn>
          </div>
        </div>
        <div class="ai-hint">
          用于选择一段上下文并提出一个问题；默认问题是让 AI 解释您选中的内容，您也可以修改问题后再点击复制，默认输出格式是Markdown。
        </div>
        <v-textarea
          v-model="question"
          variant="outlined"
          auto-grow
          rows="2"
          density="comfortable"
          placeholder="请输入问题…"
          class="mt-2"
          hide-details
          rounded="lg"
        />
        <div v-if="localTip" class="text-body-2 text-center mt-4 text-success-darken-1">{{ localTip }}</div>
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
  gap: 4px;
}
.ai-button {
  position: fixed;
  transform: translate(-50%, 0);
  z-index: 900;
}
.ai-indicator {
  position: absolute;
  width: 4px;
  background: rgb(var(--v-theme-success));
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
  border: 2px solid rgb(var(--v-theme-success));
  position: absolute;
  width: 640px;
  z-index: 900;
}
.ai-tag {
  position: absolute;
  padding: 2px 8px;
  background: rgb(var(--v-theme-success));
  color: #fff;
  border-radius: 999px;
  font-size: 10px;
  z-index: 900;
  pointer-events: none;
}
.ai-hint {
  font-size: 12px;
  color: #666;
  margin-top: 6px;
}
.ai-grip-line {
  width: 8px;
  height: 2px;
  background: #9e9e9e;
}

</style>
