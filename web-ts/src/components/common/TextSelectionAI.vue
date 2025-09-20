<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

interface SelectionInfo {
  text: string
  range: Range
  rect: DOMRect
}

interface ContextInfo {
  text: string
  element: Element
  startOffset: number
  endOffset: number
}

const emit = defineEmits<{
  (e: 'query', data: { question: string; context: string; selectedText: string }): void
}>()

// 状态管理
const showButton = ref(false)
const showQueryPanel = ref(false)
const showInlineQuery = ref(false)  // 新增：显示内联查询区域
const buttonPosition = ref({ top: '0px', left: '0px' })
const selectedText = ref('')
const contextText = ref('')
const question = ref('')
const currentSelection = ref<SelectionInfo | null>(null)
const currentContext = ref<ContextInfo | null>(null)
const contextElement = ref<HTMLElement | null>(null)
const initialContextBounds = ref<{ topElement: HTMLElement; bottomElement: HTMLElement } | null>(null)

// 检测文本选择
const handleSelectionChange = () => {
  console.log('selection changed')
  const selection = window.getSelection()

  // 如果有活动的内联查询，不要因为选择变化而隐藏
  if (showInlineQuery.value) {
    console.log('有活动的内联查询，跳过选择变化处理')
    return
  }

  if (!selection || selection.rangeCount === 0) {
    hideButton()
    return
  }
  console.log('selection changed1')

  const text = selection.toString().trim()

  if (text.length < 2) {
    hideButton()
    return
  }
  console.log('selection changed2')

  // 检查选择是否在当前页面内容区域
  const range = selection.getRangeAt(0)
  const container = range.commonAncestorContainer
  const contentArea = document.querySelector('.ai-selectable-content')

  if (!contentArea?.contains(container)) {
    hideButton()
    return
  }
  console.log('selection changed3')

  // 获取选择区域的位置
  const rect = range.getBoundingClientRect()

  currentSelection.value = {
    text,
    range: range.cloneRange(),
    rect
  }

  selectedText.value = text

  // 定位按钮位置
  buttonPosition.value = {
    top: `${rect.bottom + window.scrollY + 8}px`,
    left: `${rect.left + window.scrollX}px`
  }

  showButton.value = true
}

// 隐藏按钮
const hideButton = () => {
  showButton.value = false
  // 只有在没有活动查询面板和内联查询时才清除高亮
  if (!showQueryPanel.value && !showInlineQuery.value) {
    clearHighlight()
  }
}

// 生成AI提问
const generateAIQuery = async () => {
  if (!currentSelection.value) return

  // 隐藏按钮
  showButton.value = false

  // 提取上下文
  extractContext()

  // 生成默认问题
  question.value = `请解释：${selectedText.value}`

  // 直接显示内容下方的查询区域，不弹框
  await nextTick()
  showQueryArea()
}

// 显示内联查询区域
const showQueryArea = () => {
  showInlineQuery.value = true
  highlightContext()
  addQueryDisplay()
}

// 添加查询显示到上下文下方
const addQueryDisplay = () => {
  if (!currentContext.value) return

  // 移除已存在的查询显示
  document.querySelectorAll('.ai-query-display').forEach(el => el.remove())

  const element = currentContext.value.element as HTMLElement

  // 创建查询显示元素
  const queryDisplay = document.createElement('div')
  queryDisplay.className = 'ai-query-display'
  // 添加防止被高亮的标记
  queryDisplay.setAttribute('data-ai-ignore', 'true')
  queryDisplay.innerHTML = `
    <div class="query-content">
      <span class="query-text">${question.value}</span>
      <button class="copy-btn" onclick="this.closest('.ai-query-display').dispatchEvent(new CustomEvent('copy-query'))">
        复制
      </button>
      <button class="close-btn" onclick="this.closest('.ai-query-display').dispatchEvent(new CustomEvent('close-query'))">
        ×
      </button>
    </div>
  `

  // 添加事件监听
  queryDisplay.addEventListener('copy-query', handleCopyQuery)
  queryDisplay.addEventListener('close-query', closeInlineQuery)

  // 插入到上下文元素后面
  element.parentNode?.insertBefore(queryDisplay, element.nextSibling)
}

// 更新高亮边框样式 - 改为管理引用线
const updateHighlightBorders = () => {
  // 移除现有的引用线
  document.querySelectorAll('.ai-quote-line').forEach(el => el.remove())

  // 获取所有高亮元素，但排除查询显示框
  const highlightedElements = Array.from(document.querySelectorAll('.ai-context-highlight'))
    .filter(el => !el.classList.contains('ai-query-display')) as HTMLElement[]

  if (highlightedElements.length === 0) return

  // 计算引用线的位置和高度
  const firstElement = highlightedElements[0] as HTMLElement
  const lastElement = highlightedElements[highlightedElements.length - 1] as HTMLElement

  // 创建引用线元素
  const quoteLine = document.createElement('div')
  quoteLine.className = 'ai-quote-line'

  // 使用相对定位，添加到第一个元素中
  quoteLine.style.position = 'absolute'
  quoteLine.style.top = '0'
  quoteLine.style.height = `${(lastElement.offsetTop + lastElement.offsetHeight) - firstElement.offsetTop}px`

  // 添加到第一个元素中，而不是父容器
  firstElement.appendChild(quoteLine)
}

// 处理内联查询复制
const handleCopyQuery = async () => {
  try {
    await navigator.clipboard.writeText(question.value)

    // 显示复制成功反馈
    const copyBtn = document.querySelector('.ai-query-display .copy-btn') as HTMLElement
    if (copyBtn) {
      const originalText = copyBtn.textContent
      copyBtn.textContent = '已复制'
      copyBtn.style.backgroundColor = '#4caf50'
      setTimeout(() => {
        copyBtn.textContent = originalText
        copyBtn.style.backgroundColor = ''
      }, 1500)
    }

    emit('query', {
      question: question.value,
      context: contextText.value,
      selectedText: selectedText.value
    })
  } catch (error) {
    console.error('复制失败:', error)
  }
}

// 关闭内联查询
const closeInlineQuery = () => {
  showInlineQuery.value = false
  document.querySelectorAll('.ai-query-display').forEach(el => el.remove())
  clearHighlight()
  currentSelection.value = null
  currentContext.value = null
  selectedText.value = ''
  contextText.value = ''
  question.value = ''
}

// 提取上下文
const extractContext = () => {
  if (!currentSelection.value) return

  const range = currentSelection.value.range
  const container = range.commonAncestorContainer

  // 查找包含选择内容的段落或文章容器
  let contextEl = container.nodeType === Node.TEXT_NODE
    ? container.parentElement
    : container as Element

  // 向上查找合适的上下文容器
  while (contextEl && !['P', 'DIV', 'ARTICLE', 'SECTION'].includes(contextEl.tagName)) {
    contextEl = contextEl.parentElement
  }

  if (!contextEl) {
    // fallback: 使用选择内容周围的文本
    contextEl = container.parentElement || document.body
  }

  contextText.value = contextEl.textContent || ''

  currentContext.value = {
    text: contextText.value,
    element: contextEl,
    startOffset: 0,
    endOffset: contextText.value.length
  }

  contextElement.value = contextEl as HTMLElement
}

// 高亮上下文
const highlightContext = () => {
  if (!currentContext.value) return

  const element = currentContext.value.element as HTMLElement

  // 添加高亮样式
  element.classList.add('ai-context-highlight')

  // 记录初始边界（上下文的初始上下界）
  initialContextBounds.value = {
    topElement: element,
    bottomElement: element
  }

  // 更新边框样式
  updateHighlightBorders()

  // 添加可拖拽手柄
  addResizeHandles(element)
}

// 添加调整手柄
const addResizeHandles = (element: HTMLElement) => {
  // 移除已存在的手柄
  document.querySelectorAll('.context-resize-handle').forEach(el => el.remove())

  // 找到最外层的可选择内容容器
  const contentArea = document.querySelector('.ai-selectable-content') as HTMLElement
  if (!contentArea) return

  // 确保容器有相对定位
  if (getComputedStyle(contentArea).position === 'static') {
    contentArea.style.position = 'relative'
  }

  const topHandle = document.createElement('div')
  topHandle.className = 'context-resize-handle top'
  topHandle.title = '拖拽调整范围'
  topHandle.innerHTML = '<i class="mdi mdi-arrow-up-down-bold"></i>'

  const bottomHandle = document.createElement('div')
  bottomHandle.className = 'context-resize-handle bottom'
  bottomHandle.title = '拖拽调整范围'
  bottomHandle.innerHTML = '<i class="mdi mdi-arrow-up-down-bold"></i>'

  // 获取第一个和最后一个高亮元素的位置来定位按钮
  const highlightedElements = document.querySelectorAll('.ai-context-highlight')
  if (highlightedElements.length > 0) {
    const firstElement = highlightedElements[0] as HTMLElement
    const lastElement = highlightedElements[highlightedElements.length - 1] as HTMLElement

    const contentRect = contentArea.getBoundingClientRect()
    const firstRect = firstElement.getBoundingClientRect()
    const lastRect = lastElement.getBoundingClientRect()

    // 设置按钮位置（相对于内容容器）
    topHandle.style.top = `${firstRect.top - contentRect.top - 12}px`
    topHandle.style.left = '-48px'

    bottomHandle.style.top = `${lastRect.bottom - contentRect.top - 12}px`
    bottomHandle.style.left = '-48px'
  }

  contentArea.appendChild(topHandle)
  contentArea.appendChild(bottomHandle)

  // 添加拖拽事件
  addDragEvents(topHandle, 'up')
  addDragEvents(bottomHandle, 'down')
}

// 拖拽事件处理 - 按钮跟随鼠标，到达新位置时扩展
const addDragEvents = (handle: HTMLElement, handleType: 'up' | 'down') => {
  let isDragging = false
  let startY = 0
  let lastTargetElement: HTMLElement | null = null // 记录上次触发扩展的目标元素

  const handleMouseDown = (e: MouseEvent) => {
    isDragging = true
    startY = e.clientY
    lastTargetElement = null // 重置目标元素
    e.preventDefault()

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)

    handle.classList.add('dragging')
  }

  const handleMouseMove = (e: MouseEvent) => {
    if (!isDragging) return

    const currentY = e.clientY

    // 使用transform跟随鼠标，避免频繁修改top导致卡顿
    const deltaY = currentY - startY
    handle.style.transform = `translateY(${deltaY}px)`

    // 大幅降低检测频率，避免卡顿
    if (Math.abs(deltaY) % 30 === 0) { // 每移动30px检测一次
      // 检查鼠标位置下的目标元素
      const targetElement = findTargetElement(currentY)

      // 检查是否需要扩展
      if (targetElement && targetElement !== lastTargetElement) {
        if (shouldExpand(targetElement, handleType)) {
          console.log(`${handleType}按钮到达新位置，扩展到:`, targetElement.tagName)
          expandToTarget(targetElement, handleType)
          lastTargetElement = targetElement
        }
      }

      // 检查是否需要收缩（基于拖拽方向）
      if (handleType === 'up' && deltaY > 50) {
        // 上按钮向下拖拽超过阈值：收缩上边界
        console.log('上按钮向下拖拽，收缩上边界')
        shrinkContextFromTop()
      } else if (handleType === 'down' && deltaY < -50) {
        // 下按钮向上拖拽超过阈值：收缩下边界
        console.log('下按钮向上拖拽，收缩下边界')
        shrinkContextFromBottom()
      }
    }
  }

  const handleMouseUp = () => {
    isDragging = false
    handle.classList.remove('dragging')

    // 鼠标停止拖动时，按钮回到引用边界的正确位置
    repositionHandle(handle, handleType)

    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  handle.addEventListener('mousedown', handleMouseDown)
}

// 重新定位按钮到正确位置
const repositionHandle = (handle: HTMLElement, handleType: 'up' | 'down') => {
  const contentArea = document.querySelector('.ai-selectable-content') as HTMLElement
  if (!contentArea) return

  const highlightedElements = Array.from(document.querySelectorAll('.ai-context-highlight'))
    .filter(el => !el.classList.contains('ai-query-display')) as HTMLElement[]

  if (highlightedElements.length === 0) return

  const contentRect = contentArea.getBoundingClientRect()
  const handleHeight = 24

  if (handleType === 'up') {
    // 上按钮定位到第一个高亮元素
    const firstElement = highlightedElements[0]
    const firstRect = firstElement.getBoundingClientRect()
    handle.style.top = `${firstRect.top + firstRect.height / 2 - contentRect.top - handleHeight / 2}px`
  } else {
    // 下按钮定位到最后一个高亮元素
    const lastElement = highlightedElements[highlightedElements.length - 1]
    const lastRect = lastElement.getBoundingClientRect()
    handle.style.top = `${lastRect.top + lastRect.height / 2 - contentRect.top - handleHeight / 2}px`
  }

  // 清除transform，使用直接定位
  handle.style.transform = ''
}

// 找到鼠标位置下的目标元素
const findTargetElement = (mouseY: number): HTMLElement | null => {
  const elementAtMouse = document.elementFromPoint(window.innerWidth / 2, mouseY) as HTMLElement
  if (!elementAtMouse) return null

  // 查找合适的文本容器
  let targetElement = elementAtMouse
  while (targetElement && !['P', 'DIV', 'ARTICLE', 'SECTION', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI'].includes(targetElement.tagName)) {
    targetElement = targetElement.parentElement as HTMLElement
    if (!targetElement || targetElement === document.body) break
  }

  if (!targetElement || targetElement === document.body) return null

  // 检查是否在可选择区域内
  const contentArea = document.querySelector('.ai-selectable-content')
  if (!contentArea?.contains(targetElement)) return null

  return targetElement
}

// 判断是否应该扩展到目标元素
const shouldExpand = (targetElement: HTMLElement, handleType: 'up' | 'down'): boolean => {
  // 如果已经高亮，不需要扩展
  if (targetElement.classList.contains('ai-context-highlight')) return false

  const highlightedElements = Array.from(document.querySelectorAll('.ai-context-highlight'))
    .filter(el => !el.classList.contains('ai-query-display')) as HTMLElement[]

  if (highlightedElements.length === 0) return false

  const targetRect = targetElement.getBoundingClientRect()

  if (handleType === 'up') {
    // 上按钮：只有当目标元素在当前高亮区域之上时才扩展
    const currentTopElement = highlightedElements[0]
    return targetRect.top < currentTopElement.getBoundingClientRect().top
  } else {
    // 下按钮：只有当目标元素在当前高亮区域之下时才扩展
    const currentBottomElement = highlightedElements[highlightedElements.length - 1]
    return targetRect.bottom > currentBottomElement.getBoundingClientRect().bottom
  }
}

// 扩展到目标元素
const expandToTarget = (targetElement: HTMLElement, handleType: 'up' | 'down') => {
  if (handleType === 'up') {
    // 向上扩展：只扩展一个相邻元素
    expandContextUp()
  } else {
    // 向下扩展：只扩展一个相邻元素
    expandContextDown()
  }
}

// 从顶部收缩上下文
const shrinkContextFromTop = () => {
  if (!currentContext.value || !initialContextBounds.value) return

  // 找到所有高亮元素，但排除查询显示框
  const highlightedElements = Array.from(document.querySelectorAll('.ai-context-highlight'))
    .filter(el => !el.classList.contains('ai-query-display')) as HTMLElement[]

  if (highlightedElements.length <= 1) {
    console.log('收缩失败：已经是最小范围')
    return
  }

  // 检查收缩后是否会超过初始下界
  const firstElement = highlightedElements[0]
  const secondElement = highlightedElements[1]

  const initialBottomRect = initialContextBounds.value.bottomElement.getBoundingClientRect()
  const secondRect = secondElement.getBoundingClientRect()

  if (secondRect.bottom > initialBottomRect.bottom) {
    console.log('从顶部收缩失败：不能超过初始下界')
    return
  }

  // 移除第一个高亮元素
  firstElement.classList.remove('ai-context-highlight')

  // 从上下文文本中移除第一个元素的文本
  const firstElementText = firstElement.textContent || ''
  if (contextText.value.startsWith(firstElementText)) {
    contextText.value = contextText.value.substring(firstElementText.length).replace(/^\n/, '')
  }

  // 更新当前上下文元素为下一个元素
  currentContext.value = {
    ...currentContext.value,
    element: secondElement,
    text: contextText.value
  }

  // 移动上按钮到新的第一个元素
  moveTopHandleToElement(secondElement)

  // 更新边框样式
  updateHighlightBorders()

  console.log('从顶部收缩成功，移除元素:', firstElement.tagName)
}

// 从底部收缩上下文
const shrinkContextFromBottom = () => {
  if (!currentContext.value || !initialContextBounds.value) return

  // 找到所有高亮元素，但排除查询显示框
  const highlightedElements = Array.from(document.querySelectorAll('.ai-context-highlight'))
    .filter(el => !el.classList.contains('ai-query-display')) as HTMLElement[]

  if (highlightedElements.length <= 1) {
    console.log('收缩失败：已经是最小范围')
    return
  }

  // 检查收缩后是否会超过初始上界
  const lastElement = highlightedElements[highlightedElements.length - 1]
  const secondLastElement = highlightedElements[highlightedElements.length - 2]

  const initialTopRect = initialContextBounds.value.topElement.getBoundingClientRect()
  const secondLastRect = secondLastElement.getBoundingClientRect()

  if (secondLastRect.top < initialTopRect.top) {
    console.log('从底部收缩失败：不能超过初始上界')
    return
  }

  // 移除最后一个高亮元素
  lastElement.classList.remove('ai-context-highlight')

  // 从上下文文本中移除最后一个元素的文本
  const lastElementText = lastElement.textContent || ''
  if (contextText.value.endsWith(lastElementText)) {
    contextText.value = contextText.value.substring(0, contextText.value.length - lastElementText.length).replace(/\n$/, '')
  }

  // 移动下按钮到新的最后一个元素
  moveBottomHandleToElement(secondLastElement)

  // 更新边框样式
  updateHighlightBorders()

  // 确保查询框保持在最下方
  ensureQueryDisplayAtBottom()

  console.log('从底部收缩成功，移除元素:', lastElement.tagName)
}

// 向上扩展上下文
const expandContextUp = () => {
  if (!currentContext.value) return

  const element = currentContext.value.element as HTMLElement
  const prevElement = element.previousElementSibling as HTMLElement

  if (prevElement && !prevElement.classList.contains('ai-context-highlight')) {
    prevElement.classList.add('ai-context-highlight')
    contextText.value = prevElement.textContent + '\n' + contextText.value

    // 更新当前上下文元素为新扩展的元素
    currentContext.value = {
      ...currentContext.value,
      element: prevElement,
      text: contextText.value
    }

    // 更新边框样式
    updateHighlightBorders()

    // 只移动上按钮到新的顶部元素，保持下按钮在原位置
    moveTopHandleToElement(prevElement)
    console.log('向上扩展成功，新元素:', prevElement.tagName)
  } else {
    console.log('向上扩展失败，原因:', !prevElement ? '没有上一个元素' : '元素已经高亮')
  }
}

// 移动上按钮到指定元素
const moveTopHandleToElement = (element: HTMLElement) => {
  const topHandle = document.querySelector('.context-resize-handle.top') as HTMLElement
  if (!topHandle) return

  const contentArea = document.querySelector('.ai-selectable-content') as HTMLElement
  if (!contentArea) return

  const contentRect = contentArea.getBoundingClientRect()
  const elementRect = element.getBoundingClientRect()

  // 更新按钮位置
  topHandle.style.top = `${elementRect.top - contentRect.top - 12}px`
}

// 移动下按钮到指定元素
const moveBottomHandleToElement = (element: HTMLElement) => {
  const bottomHandle = document.querySelector('.context-resize-handle.bottom') as HTMLElement
  if (!bottomHandle) return

  const contentArea = document.querySelector('.ai-selectable-content') as HTMLElement
  if (!contentArea) return

  const contentRect = contentArea.getBoundingClientRect()
  const elementRect = element.getBoundingClientRect()

  // 更新按钮位置
  bottomHandle.style.top = `${elementRect.bottom - contentRect.top - 12}px`
}

// 向下扩展上下文
const expandContextDown = () => {
  if (!currentContext.value) return

  // 找到当前高亮区域的最后一个元素，但要跳过查询显示框
  const highlightedElements = document.querySelectorAll('.ai-context-highlight')
  let lastElement: HTMLElement | null = null

  // 从后往前找到最后一个真正的内容元素（不是查询框）
  for (let i = highlightedElements.length - 1; i >= 0; i--) {
    const element = highlightedElements[i] as HTMLElement
    if (!element.classList.contains('ai-query-display')) {
      lastElement = element
      break
    }
  }

  if (!lastElement) return

  let nextElement = lastElement.nextElementSibling as HTMLElement

  // 跳过所有AI相关的元素（查询框等）- 更严格的检查
  while (nextElement && (
    nextElement.classList.contains('ai-query-display') ||
    nextElement.classList.contains('ai-context-highlight') ||
    nextElement.className.includes('ai-') ||
    nextElement.tagName === 'STYLE' ||
    nextElement.tagName === 'SCRIPT' ||
    nextElement.querySelector('.ai-query-display') // 检查子元素是否包含查询框
  )) {
    nextElement = nextElement.nextElementSibling as HTMLElement
  }

  if (nextElement) {
    // 确保不是AI相关元素才添加高亮
    if (!nextElement.className.includes('ai-') &&
        !nextElement.querySelector('.ai-query-display')) {
      nextElement.classList.add('ai-context-highlight')
      contextText.value = contextText.value + '\n' + nextElement.textContent

      // 更新边框样式
      updateHighlightBorders()

      // 移动下按钮到新扩展的元素
      moveBottomHandleToElement(nextElement)

      // 确保查询框保持在最下方
      ensureQueryDisplayAtBottom()

      console.log('向下扩展成功，新元素:', nextElement.tagName)
    } else {
      console.log('向下扩展失败，下一个元素是AI相关元素')
    }
  } else {
    console.log('向下扩展失败，没有找到合适的下一个元素')
  }
}

// 确保查询显示框在最下方
const ensureQueryDisplayAtBottom = () => {
  const queryDisplay = document.querySelector('.ai-query-display') as HTMLElement
  if (!queryDisplay) return

  // 找到所有高亮元素中真正的最后一个（排除查询框本身）
  const highlightedElements = document.querySelectorAll('.ai-context-highlight')
  let realLastElement: HTMLElement | null = null

  for (let i = highlightedElements.length - 1; i >= 0; i--) {
    const element = highlightedElements[i] as HTMLElement
    if (!element.classList.contains('ai-query-display')) {
      realLastElement = element
      break
    }
  }

  if (realLastElement) {
    // 将查询框移动到真正的最后一个元素后面
    realLastElement.parentNode?.insertBefore(queryDisplay, realLastElement.nextSibling)
  }
}

// 清除高亮
const clearHighlight = () => {
  document.querySelectorAll('.ai-context-highlight').forEach(el => {
    el.classList.remove('ai-context-highlight')
  })
  document.querySelectorAll('.context-resize-handle').forEach(el => el.remove())
  document.querySelectorAll('.ai-query-display').forEach(el => el.remove())
  document.querySelectorAll('.ai-quote-line').forEach(el => el.remove())
}

// 提交查询 - 改为复制功能
const submitQuery = async () => {
  const queryContent = `问题：${question.value}\n\n上下文：\n${contextText.value}\n\n选中内容：\n"${selectedText.value}"`

  try {
    await navigator.clipboard.writeText(queryContent)

    emit('query', {
      question: question.value,
      context: contextText.value,
      selectedText: selectedText.value
    })

    // 显示复制成功提示
    showCopySuccess.value = true
    setTimeout(() => {
      showCopySuccess.value = false
      closePanel()
    }, 1500)
  } catch (error) {
    console.error('复制失败:', error)
    // 如果API复制失败，使用fallback方法
    fallbackCopy(queryContent)
  }
}

// Fallback复制方法
const fallbackCopy = (text: string) => {
  const textArea = document.createElement('textarea')
  textArea.value = text
  textArea.style.position = 'fixed'
  textArea.style.left = '-999999px'
  textArea.style.top = '-999999px'
  document.body.appendChild(textArea)
  textArea.focus()
  textArea.select()

  try {
    document.execCommand('copy')
    showCopySuccess.value = true
    setTimeout(() => {
      showCopySuccess.value = false
      closePanel()
    }, 1500)
  } catch (error) {
    console.error('Fallback复制也失败了:', error)
  }

  document.body.removeChild(textArea)
}

// 添加复制成功状态
const showCopySuccess = ref(false)

// 关闭面板
const closePanel = () => {
  showQueryPanel.value = false
  clearHighlight()
  currentSelection.value = null
  currentContext.value = null
  selectedText.value = ''
  contextText.value = ''
  question.value = ''
}

// 生命周期
onMounted(() => {
  document.addEventListener('selectionchange', handleSelectionChange)
})

onUnmounted(() => {
  document.removeEventListener('selectionchange', handleSelectionChange)
  clearHighlight()
})
</script>

<template>
  <!-- 浮动的AI提问按钮 -->
  <Teleport to="body">
    <div
      v-if="showButton"
      class="ai-query-button"
      :style="buttonPosition"
    >
      <v-btn
        color="primary"
        size="small"
        rounded="lg"
        prepend-icon="mdi-content-copy"
        @click="generateAIQuery"
      >
        复制内容查询
      </v-btn>
    </div>

    <!-- AI提问面板 -->
    <div v-if="showQueryPanel" class="ai-query-panel">
      <v-card rounded="xl" elevation="8" class="pa-4">
        <div class="d-flex align-center justify-space-between mb-3">
          <h3 class="text-h6 font-weight-bold">
            <v-icon icon="mdi-content-copy" color="primary" class="mr-2"></v-icon>
            复制内容查询
          </h3>
          <v-btn
            icon="mdi-close"
            variant="text"
            size="small"
            @click="closePanel"
          ></v-btn>
        </div>

        <div class="mb-3">
          <div class="text-body-2 text-grey-darken-1 mb-2">选中内容：</div>
          <div class="selected-text-preview pa-2 bg-blue-lighten-5 rounded">
            "{{ selectedText }}"
          </div>
        </div>

        <div class="mb-3">
          <div class="text-body-2 text-grey-darken-1 mb-2">
            上下文范围（可拖拽调整）：
          </div>
          <div class="context-preview pa-2 bg-grey-lighten-4 rounded text-body-2">
            {{ contextText.slice(0, 200) }}{{ contextText.length > 200 ? '...' : '' }}
          </div>
        </div>

        <v-textarea
          v-model="question"
          label="查询问题（将与上下文一起复制）"
          variant="outlined"
          rows="2"
          rounded="lg"
          class="mb-3"
          hint="编辑问题内容，点击复制后可在其他AI网站进行查询"
        ></v-textarea>

        <!-- 复制成功提示 -->
        <v-alert
          v-if="showCopySuccess"
          type="success"
          variant="tonal"
          class="mb-3"
          text="内容已复制到剪贴板！可以到其他AI网站粘贴查询了"
        ></v-alert>

        <div class="d-flex justify-end" style="gap: 8px;">
          <v-btn
            variant="outlined"
            rounded="lg"
            @click="closePanel"
          >
            取消
          </v-btn>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            prepend-icon="mdi-content-copy"
            @click="submitQuery"
            :disabled="showCopySuccess"
          >
            {{ showCopySuccess ? '已复制' : '复制查询内容' }}
          </v-btn>
        </div>
      </v-card>
    </div>
  </Teleport>
</template>

<style scoped>
.ai-query-button {
  position: absolute;
  z-index: 1000;
  animation: fadeInUp 0.3s ease;
}

.ai-query-panel {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 1001;
  min-width: 500px;
  max-width: 80vw;
  animation: fadeInScale 0.3s ease;
}

.selected-text-preview {
  font-style: italic;
  border-left: 3px solid rgb(var(--v-theme-primary));
}

.context-preview {
  max-height: 100px;
  overflow-y: auto;
  line-height: 1.5;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}
</style>

<style>
/* 全局样式 - 上下文高亮 */
.ai-context-highlight {
  position: relative !important;
  transition: all 0.3s ease !important;
}

/* 引用线容器样式 */
.ai-quote-line {
  position: absolute;
  left: -16px;  /* 从-8px改为-16px，增加间距 */
  width: 4px;
  background: linear-gradient(to bottom, rgba(25, 118, 210, 0.8), rgba(25, 118, 210, 0.6));
  border-radius: 2px;
  z-index: 10;
  box-shadow: 0 1px 3px rgba(25, 118, 210, 0.3);
  transition: all 0.3s ease;
}

/* 新滑块控制器样式 */
.context-resize-handle {
  position: absolute;
  left: -48px;  /* 离竖线更远，增加间距 */
  width: 24px;
  height: 24px;  /* 增加高度到24px，变成正方形 */
  background: #2196f3;  /* 蓝色背景 */
  border: 1px solid #1976d2;
  border-radius: 4px;
  cursor: grab;
  z-index: 1001;
  user-select: none;
  box-shadow: 0 2px 4px rgba(33, 150, 243, 0.3);
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 图标样式 */
.context-resize-handle i {
  font-size: 16px;  /* 从12px增加到16px */
  color: #ffffff;  /* 白色图标 */
}

.context-resize-handle:hover {
  background: #1976d2;  /* 深蓝色悬停 */
  border-color: #1565c0;
  transform: scale(1.05);
  box-shadow: 0 3px 6px rgba(33, 150, 243, 0.4);
}

.context-resize-handle:active,
.context-resize-handle.dragging {
  cursor: grabbing;
  background: #1565c0;  /* 更深蓝色激活 */
  border-color: #0d47a1;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.2);
  transform: scale(1.02);
}

.context-resize-handle.top {
  top: -4px;
}

.context-resize-handle.bottom {
  bottom: -4px;
}

/* 内联查询显示样式 */
.ai-query-display {
  margin: 16px 0;
  padding: 12px 16px;
  background: linear-gradient(135deg, #e3f2fd 0%, #ffffff 100%);
  border: 1px solid #2196f3;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.2);
  position: relative;
  z-index: 1000;
}

.ai-query-display .query-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-query-display .query-text {
  flex: 1;
  font-size: 14px;
  color: #1565c0;
  font-weight: 500;
  line-height: 1.4;
}

.ai-query-display .copy-btn,
.ai-query-display .close-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.ai-query-display .copy-btn {
  background: #2196f3;
  color: white;
}

.ai-query-display .copy-btn:hover {
  background: #1976d2;
  transform: scale(1.05);
}

.ai-query-display .close-btn {
  background: #f5f5f5;
  color: #666;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 16px;
  line-height: 1;
}

.ai-query-display .close-btn:hover {
  background: #e0e0e0;
  color: #333;
}
</style>