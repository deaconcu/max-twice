<template>
  <div>
    <!-- 浮层AI查询窗口 -->
    <Teleport to="body">
      <div
        v-if="showFloatingQuery"
        class="floating-ai-query"
        :style="{
          position: 'fixed',
          left: floatingQueryPosition.left + 'px',
          top: floatingQueryPosition.top + 'px',
          width: floatingQueryPosition.width + 'px',
          zIndex: 9999
        }"
      >
        <v-card class="pa-4" elevation="8">
          <v-textarea
            v-model="question"
            label="向AI提问"
            rows="3"
            auto-grow
            variant="outlined"
            hide-details
          />
          <div class="d-flex justify-end mt-3 gap-2">
            <v-btn size="small" @click="closeFloatingQuery">取消</v-btn>
            <v-btn 
              size="small" 
              color="primary" 
              @click="submitQuery"
            >
              提交查询
            </v-btn>
          </div>
        </v-card>
      </div>
    </Teleport>

    <!-- 复制成功提示 -->
    <v-snackbar v-model="showCopySuccess" timeout="2000" color="success">
      查询内容已复制到剪贴板
    </v-snackbar>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const emit = defineEmits<{
  query: [{ question: string; context: string; selectedText: string }]
}>()

// 响应式状态
const selectedText = ref('')
const contextText = ref('')
const question = ref('')
const showFloatingQuery = ref(false)
const showCopySuccess = ref(false)
const floatingQueryPosition = ref({ left: 0, top: 0, width: 0 })

// 当前选择状态
let currentSelection: {
  range: Range
  highlightedElements: HTMLElement[]
  contentArea: HTMLElement
} | null = null

// 拖拽状态
let isDragging = false
let dragStartY = 0
let dragHandle: HTMLElement | null = null
let dragType: 'top' | 'bottom' = 'top'

// 初始化
onMounted(() => {
  document.addEventListener('mouseup', handleTextSelection)
})

onUnmounted(() => {
  document.removeEventListener('mouseup', handleTextSelection)
  clearSelection()
})

// 处理文本选择
const handleTextSelection = () => {
  const selection = window.getSelection()
  if (!selection || selection.isCollapsed) return

  const range = selection.getRangeAt(0)
  const text = range.toString().trim()
  if (!text) return

  // 检查是否在可选择区域内
  const contentArea = document.querySelector('.ai-selectable-content') as HTMLElement
  if (!contentArea || !contentArea.contains(range.commonAncestorContainer)) return

  selectedText.value = text
  contextText.value = text
  
  // 清除之前的选择
  clearSelection()

  // 创建新的选择
  createSelection(range, contentArea)
}

// 创建选择区域
const createSelection = (range: Range, contentArea: HTMLElement) => {
  // 高亮选中的元素
  const highlightedElements = highlightRange(range)
  
  // 创建拖拽手柄
  const handles = createDragHandles(highlightedElements, contentArea)
  
  // 保存当前选择状态
  currentSelection = {
    range,
    highlightedElements,
    contentArea
  }

  // 显示浮层查询窗口
  showFloatingQueryWindow(highlightedElements, contentArea)
}

// 高亮范围内的元素
const highlightRange = (range: Range): HTMLElement[] => {
  const elements: HTMLElement[] = []
  const walker = document.createTreeWalker(
    range.commonAncestorContainer,
    NodeFilter.SHOW_ELEMENT,
    {
      acceptNode: (node) => {
        const element = node as HTMLElement
        if (range.intersectsNode(element) && isContentElement(element)) {
          return NodeFilter.FILTER_ACCEPT
        }
        return NodeFilter.FILTER_REJECT
      }
    }
  )

  let node
  while (node = walker.nextNode()) {
    const element = node as HTMLElement
    element.classList.add('ai-context-highlight')
    elements.push(element)
  }

  return elements
}

// 判断是否是内容元素
const isContentElement = (element: HTMLElement): boolean => {
  const validTags = ['P', 'DIV', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI', 'SPAN']
  return validTags.includes(element.tagName) && 
         element.textContent?.trim() !== '' &&
         !element.className.includes('ai-')
}

// 创建拖拽手柄
const createDragHandles = (elements: HTMLElement[], contentArea: HTMLElement) => {
  if (elements.length === 0) return

  const firstElement = elements[0]
  const lastElement = elements[elements.length - 1]
  
  // 创建顶部手柄
  const topHandle = createHandle('top', firstElement, contentArea)
  // 创建底部手柄
  const bottomHandle = createHandle('bottom', lastElement, contentArea)

  return { topHandle, bottomHandle }
}

// 创建单个手柄
const createHandle = (type: 'top' | 'bottom', element: HTMLElement, contentArea: HTMLElement): HTMLElement => {
  const handle = document.createElement('div')
  handle.className = `context-resize-handle ${type}`
  handle.innerHTML = type === 'top' ? '↑' : '↓'
  
  // 定位手柄
  positionHandle(handle, element, contentArea, type)
  
  // 添加拖拽事件
  handle.addEventListener('mousedown', (e) => startDrag(e, handle, type))
  
  contentArea.appendChild(handle)
  return handle
}

// 定位手柄
const positionHandle = (handle: HTMLElement, element: HTMLElement, contentArea: HTMLElement, type: 'top' | 'bottom') => {
  const contentRect = contentArea.getBoundingClientRect()
  const elementRect = element.getBoundingClientRect()
  
  const left = elementRect.left - contentRect.left - 12
  const top = type === 'top' 
    ? elementRect.top - contentRect.top - 12
    : elementRect.bottom - contentRect.top - 12

  handle.style.cssText = `
    position: absolute;
    left: ${left}px;
    top: ${top}px;
    width: 24px;
    height: 24px;
    background: #1976d2;
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: ns-resize;
    user-select: none;
    font-size: 12px;
    z-index: 1000;
  `
}

// 开始拖拽
const startDrag = (e: MouseEvent, handle: HTMLElement, type: 'top' | 'bottom') => {
  e.preventDefault()
  isDragging = true
  dragStartY = e.clientY
  dragHandle = handle
  dragType = type

  document.addEventListener('mousemove', handleDrag)
  document.addEventListener('mouseup', endDrag)
}

// 处理拖拽
const handleDrag = (e: MouseEvent) => {
  if (!isDragging || !currentSelection) return

  const deltaY = e.clientY - dragStartY
  const threshold = 50 // 拖拽50px触发扩展

  if (Math.abs(deltaY) > threshold) {
    if (dragType === 'top' && deltaY < 0) {
      // 向上扩展
      expandUp()
    } else if (dragType === 'bottom' && deltaY > 0) {
      // 向下扩展
      expandDown()
    }
    
    // 重置拖拽起点
    dragStartY = e.clientY
  }

  // 更新手柄位置跟随鼠标
  if (dragHandle) {
    const contentRect = currentSelection.contentArea.getBoundingClientRect()
    const newTop = e.clientY - contentRect.top - 12
    dragHandle.style.top = `${newTop}px`
  }
}

// 结束拖拽
const endDrag = () => {
  isDragging = false
  dragHandle = null
  
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', endDrag)

  // 重新定位手柄到正确位置
  if (currentSelection) {
    repositionHandles()
  }
}

// 向上扩展
const expandUp = () => {
  if (!currentSelection) return

  const firstElement = currentSelection.highlightedElements[0]
  const newElements = findElementsToExpand(firstElement, 'up')
  
  if (newElements.length > 0) {
    // 高亮新元素
    newElements.forEach(el => {
      el.classList.add('ai-context-highlight')
      currentSelection!.highlightedElements.unshift(el)
      contextText.value = el.textContent + '\n' + contextText.value
    })
    
    repositionHandles()
    updateFloatingQueryPosition()
  }
}

// 向下扩展
const expandDown = () => {
  if (!currentSelection) return

  const lastElement = currentSelection.highlightedElements[currentSelection.highlightedElements.length - 1]
  const newElements = findElementsToExpand(lastElement, 'down')
  
  if (newElements.length > 0) {
    // 高亮新元素
    newElements.forEach(el => {
      el.classList.add('ai-context-highlight')
      currentSelection!.highlightedElements.push(el)
      contextText.value = contextText.value + '\n' + el.textContent
    })
    
    repositionHandles()
    updateFloatingQueryPosition()
  }
}

// 查找要扩展的元素
const findElementsToExpand = (currentElement: HTMLElement, direction: 'up' | 'down'): HTMLElement[] => {
  const elements: HTMLElement[] = []
  let current = direction === 'up' 
    ? currentElement.previousElementSibling as HTMLElement
    : currentElement.nextElementSibling as HTMLElement

  // 收集连续的同类型元素
  while (current) {
    if (!isContentElement(current) || current.classList.contains('ai-context-highlight')) {
      break
    }

    elements.push(current)

    // 如果是标题，只扩展一个
    if (current.tagName.startsWith('H')) {
      break
    }

    // 检查下一个元素
    const next = direction === 'up'
      ? current.previousElementSibling as HTMLElement  
      : current.nextElementSibling as HTMLElement

    // 如果下一个元素类型不同，停止扩展
    if (!next || next.tagName !== current.tagName) {
      break
    }

    current = next
  }

  return direction === 'up' ? elements.reverse() : elements
}

// 重新定位手柄
const repositionHandles = () => {
  if (!currentSelection) return

  const elements = currentSelection.highlightedElements
  const contentArea = currentSelection.contentArea
  
  // 重新定位顶部手柄
  const topHandle = contentArea.querySelector('.context-resize-handle.top') as HTMLElement
  if (topHandle) {
    positionHandle(topHandle, elements[0], contentArea, 'top')
  }

  // 重新定位底部手柄
  const bottomHandle = contentArea.querySelector('.context-resize-handle.bottom') as HTMLElement
  if (bottomHandle) {
    positionHandle(bottomHandle, elements[elements.length - 1], contentArea, 'bottom')
  }
}

// 显示浮层查询窗口
const showFloatingQueryWindow = (elements: HTMLElement[], contentArea: HTMLElement) => {
  const lastElement = elements[elements.length - 1]
  const contentRect = contentArea.getBoundingClientRect()
  const elementRect = lastElement.getBoundingClientRect()

  floatingQueryPosition.value = {
    left: contentRect.left,
    top: elementRect.bottom + window.scrollY + 10,
    width: contentRect.width
  }

  showFloatingQuery.value = true
}

// 更新浮层位置
const updateFloatingQueryPosition = () => {
  if (!currentSelection || !showFloatingQuery.value) return
  
  const elements = currentSelection.highlightedElements
  const contentArea = currentSelection.contentArea
  showFloatingQueryWindow(elements, contentArea)
}

// 关闭浮层查询
const closeFloatingQuery = () => {
  showFloatingQuery.value = false
  clearSelection()
}

// 提交查询
const submitQuery = async () => {
  const queryContent = `问题：${question.value}\n\n上下文：\n${contextText.value}\n\n选中内容：\n"${selectedText.value}"`

  try {
    await navigator.clipboard.writeText(queryContent)
    
    emit('query', {
      question: question.value,
      context: contextText.value,
      selectedText: selectedText.value
    })

    showCopySuccess.value = true
    setTimeout(() => {
      showCopySuccess.value = false
      closeFloatingQuery()
    }, 1500)
  } catch (error) {
    console.error('复制到剪贴板失败:', error)
  }
}

// 清除选择
const clearSelection = () => {
  // 清除高亮
  document.querySelectorAll('.ai-context-highlight').forEach(el => {
    el.classList.remove('ai-context-highlight')
  })
  
  // 清除手柄
  document.querySelectorAll('.context-resize-handle').forEach(el => el.remove())
  
  // 重置状态
  currentSelection = null
  selectedText.value = ''
  contextText.value = ''
  question.value = ''
}
</script>

<style scoped>
.floating-ai-query {
  pointer-events: auto;
}

:deep(.ai-context-highlight) {
  background-color: rgba(25, 118, 210, 0.1);
  border-left: 3px solid #1976d2;
  padding-left: 8px;
  margin: 2px 0;
}

.context-resize-handle {
  transition: background-color 0.2s ease;
}

.context-resize-handle:hover {
  background-color: #1565c0 !important;
  transform: scale(1.1);
}
</style>
