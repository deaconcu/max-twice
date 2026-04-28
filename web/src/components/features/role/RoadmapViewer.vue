<template>
  <div class="roadmap-viewer" :style="containerStyle">
    <VueFlow
      :nodes="vfNodes"
      :edges="vfEdges"
      :nodes-draggable="false"
      :nodes-connectable="false"
      :elements-selectable="false"
      :zoom-on-scroll="false"
      :zoom-on-pinch="false"
      :zoom-on-double-click="false"
      :prevent-scrolling="false"
      :min-zoom="ZOOM"
      :max-zoom="ZOOM"
      @node-click="onNodeClick"
    >
      <Background pattern-color="#e0e0e0" :gap="20" :size="1" variant="dots" />

      <template #node-phantom>
        <Handle id="top" type="target" :position="Position.Top" />
        <Handle id="bottom" type="source" :position="Position.Bottom" />
        <div style="width: 0; height: 0" />
      </template>

      <template #node-topic="{ data }">
        <Handle id="top" type="target" :position="Position.Top" />
        <Handle id="bottom" type="source" :position="Position.Bottom" />
        <Handle id="left" type="source" :position="Position.Left" />
        <Handle id="left-in" type="target" :position="Position.Left" />
        <Handle id="right" type="source" :position="Position.Right" />
        <Handle id="right-in" type="target" :position="Position.Right" />
        <div
          class="node-topic"
          :class="[
            data.nodeType === 'course'
              ? 'node-topic--course'
              : data.nodeType === 'node'
                ? 'node-topic--node'
                : data.nodeType === 'note'
                  ? 'node-topic--note'
                  : data.nodeType === 'group'
                    ? 'node-topic--group'
                    : 'node-topic--placeholder',
            { 'node-topic--clickable': data.clickable },
          ]"
        >
          {{ data.label }}
        </div>
      </template>
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount, nextTick, watch } from 'vue'
import { VueFlow, Handle, Position, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import type { Edge as VFEdge, Node as VFNode } from '@vue-flow/core'
import { useI18n } from '@/composables/useI18n'

/* ========== 类型定义（与编辑器对齐） ========== */
interface RoadmapNode {
  id: string
  label: string
  nodeType?: 'course' | 'node' | 'group' | 'note'
  courseId?: number
  children?: RoadmapNode[]
}

interface SerializedNode {
  t: 'c' | 'n' | 'g' | 'o'
  id?: number
  label?: string
  children?: SerializedNode[]
}

const props = withDefaults(
  defineProps<{
    content: string | object | null | undefined
    roleName: string
    fitParent?: boolean
  }>(),
  { fitParent: false }
)

const { t } = useI18n()

/* ========== 反序列化（与编辑器一致） ========== */
let loadGroupSeq = 0
const deserialize = (nodes: SerializedNode[]): RoadmapNode[] => {
  return nodes.map((n) => {
    let id: string
    let nodeType: 'course' | 'node' | 'group' | 'note' | undefined
    let courseId: number | undefined
    if (n.t === 'c' && n.id != null) {
      id = `c${n.id}`
      nodeType = 'course'
      courseId = n.id
    } else if (n.t === 'n' && n.id != null) {
      id = `n${n.id}`
      nodeType = 'node'
    } else if (n.t === 'o') {
      id = `g_load_${++loadGroupSeq}`
      nodeType = 'note'
    } else {
      id = `g_load_${++loadGroupSeq}`
      nodeType = n.children?.length ? 'group' : undefined
    }
    const out: RoadmapNode = { id, label: n.label ?? '', nodeType }
    if (courseId !== undefined) out.courseId = courseId
    if (n.children?.length) out.children = deserialize(n.children)
    return out
  })
}

const trunk = computed<RoadmapNode[]>(() => {
  const raw = props.content
  if (!raw) return []
  try {
    const data = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (data && data.v === 2 && Array.isArray(data.trunk)) {
      loadGroupSeq = 0
      return deserialize(data.trunk)
    }
    return []
  } catch (e) {
    console.error('解析路线图内容失败:', e)
    return []
  }
})

/* ========== 常量（与编辑器一致） ========== */
const NODE_W = 200
const NODE_H = 36
const COL_GAP = 40
const ROW_GAP = 8
const PATH_GAP = 32
const TRUNK_GAP = 52
const TRUNK_INTERNODE_GAP = TRUNK_GAP - NODE_H - ROW_GAP
const EXTEND = 40

const COLS = ['left3', 'left2', 'left1', 'center', 'right1', 'right2', 'right3'] as const
type Col = (typeof COLS)[number]

const COL_X: Record<Col, number> = {
  left3: 300 - (NODE_W + COL_GAP) * 3,
  left2: 300 - (NODE_W + COL_GAP) * 2,
  left1: 300 - NODE_W - COL_GAP,
  center: 300,
  right1: 300 + NODE_W + COL_GAP,
  right2: 300 + (NODE_W + COL_GAP) * 2,
  right3: 300 + (NODE_W + COL_GAP) * 3,
}

/* ========== 高度测量 ========== */
let measureEl: HTMLDivElement | null = null
const heightCache = new Map<string, number>()

const ensureMeasureEl = (): HTMLDivElement | null => {
  if (typeof document === 'undefined') return null
  if (measureEl && document.body.contains(measureEl)) return measureEl
  const el = document.createElement('div')
  el.style.cssText = [
    'position:absolute',
    'visibility:hidden',
    'pointer-events:none',
    'top:-9999px',
    'left:-9999px',
    `width:${NODE_W}px`,
    'padding:8px 12px',
    'font-size:14px',
    'line-height:1.5',
    'box-sizing:border-box',
    'white-space:normal',
    'word-break:break-word',
    'overflow-wrap:break-word',
    'border:1.5px solid transparent',
    'border-radius:6px',
  ].join(';')
  document.body.appendChild(el)
  measureEl = el
  return el
}

const measureHeight = (label: string): number => {
  const cached = heightCache.get(label)
  if (cached !== undefined) return cached
  const el = ensureMeasureEl()
  if (!el) return NODE_H
  el.textContent = label
  const h = Math.max(NODE_H, el.offsetHeight)
  heightCache.set(label, h)
  return h
}

onBeforeUnmount(() => {
  if (measureEl && measureEl.parentNode) {
    measureEl.parentNode.removeChild(measureEl)
  }
  measureEl = null
  heightCache.clear()
})

/* ========== 渲染：trunk → VueFlow nodes/edges（与编辑器一致） ========== */
const renderResult = computed<{ vfNodes: VFNode[]; vfEdges: VFEdge[] }>(() => {
  const trunkNodes = trunk.value
  const vfNodes: VFNode[] = []
  const vfEdges: VFEdge[] = []

  const colBot: Record<Col, number> = {
    left3: 10,
    left2: 10,
    left1: 10,
    center: 10,
    right1: 10,
    right2: 10,
    right3: 10,
  }

  const addBranchEdges = (parentId: string, children: RoadmapNode[], side: 'left' | 'right') => {
    const sHandle = side === 'right' ? 'right' : 'left'
    const tInHandle = side === 'right' ? 'left-in' : 'right-in'
    const tOutHandle = side === 'right' ? 'left' : 'right'
    const sInHandle = side === 'right' ? 'right-in' : 'left-in'

    children.forEach((child, i) => {
      if (i === 0) {
        vfEdges.push({
          id: `${parentId}-${child.id}`,
          source: parentId,
          target: child.id,
          sourceHandle: sHandle,
          targetHandle: tInHandle,
          type: 'default',
          style: { stroke: '#888', strokeWidth: 1.5, strokeDasharray: '8,4' },
        })
      } else {
        vfEdges.push({
          id: `${children[i - 1].id}-${child.id}`,
          source: children[i - 1].id,
          target: child.id,
          sourceHandle: 'bottom',
          targetHandle: 'top',
          type: 'default',
          style: { stroke: '#888', strokeWidth: 1.5 },
        })
      }
      if (child.children?.length) {
        addBranchEdges(child.id, child.children, side)
      }
    })

    const last = children[children.length - 1]
    vfEdges.push({
      id: `${last.id}-${parentId}`,
      source: last.id,
      target: parentId,
      sourceHandle: tOutHandle,
      targetHandle: sInHandle,
      type: 'default',
      style: { stroke: '#888', strokeWidth: 1.5, strokeDasharray: '8,4' },
    })
  }

  const isClickable = (n: RoadmapNode) => n.nodeType === 'course' || n.nodeType === 'node'

  const placeBranchNode = (
    node: RoadmapNode,
    cols: Col[],
    side: 'left' | 'right'
  ): number => {
    const myCol = cols[0]
    const childCols = cols.slice(1)
    const hasKids = !!node.children?.length && childCols.length > 0

    if (!hasKids) {
      const y = colBot[myCol]
      const h = measureHeight(node.label)
      vfNodes.push({
        id: node.id,
        type: 'topic',
        position: { x: COL_X[myCol], y },
        data: { label: node.label, nodeType: node.nodeType, clickable: isClickable(node), courseId: node.courseId },
      })
      colBot[myCol] = y + h + ROW_GAP
      return y + h / 2
    }

    const childCol = childCols[0]
    if (colBot[childCol] > 10) colBot[childCol] += PATH_GAP

    const startIdx = vfNodes.length
    const childCenters: number[] = []
    node.children!.forEach((c) => {
      childCenters.push(placeBranchNode(c, childCols, side))
    })

    const childMid = (childCenters[0] + childCenters[childCenters.length - 1]) / 2
    const myH = measureHeight(node.label)
    const desiredTop = childMid - myH / 2
    const myY = Math.max(desiredTop, colBot[myCol])
    const shift = myY - desiredTop
    if (shift > 0) {
      for (let i = startIdx; i < vfNodes.length; i++) vfNodes[i].position.y += shift
      childCols.forEach((c) => {
        colBot[c] += shift
      })
    }

    vfNodes.push({
      id: node.id,
      type: 'topic',
      position: { x: COL_X[myCol], y: myY },
      data: { label: node.label, nodeType: node.nodeType ?? 'group', clickable: isClickable(node), courseId: node.courseId },
    })
    colBot[myCol] = myY + myH + ROW_GAP

    return myY + myH / 2
  }

  const placeTrunkNode = (node: RoadmapNode) => {
    const rightMax = Math.max(colBot.right1, colBot.right2, colBot.right3)
    const leftMax = Math.max(colBot.left1, colBot.left2, colBot.left3)
    const side: 'left' | 'right' = rightMax <= leftMax ? 'right' : 'left'
    const subCols: Col[] =
      side === 'right' ? ['right1', 'right2', 'right3'] : ['left1', 'left2', 'left3']

    if (node.children?.length) {
      subCols.forEach((c) => {
        if (colBot[c] > 10) colBot[c] += PATH_GAP
      })
      const startIdx = vfNodes.length
      const childCenters: number[] = []
      node.children.forEach((c) => {
        childCenters.push(placeBranchNode(c, subCols, side))
      })
      const childMid = (childCenters[0] + childCenters[childCenters.length - 1]) / 2
      const myH = measureHeight(node.label)
      const trunkMinY = colBot.center + (colBot.center > 10 ? TRUNK_INTERNODE_GAP : 0)
      const desiredTop = childMid - myH / 2
      const trunkY = Math.max(desiredTop, trunkMinY)
      const shift = trunkY - desiredTop
      if (shift > 0) {
        for (let i = startIdx; i < vfNodes.length; i++) vfNodes[i].position.y += shift
        subCols.forEach((c) => {
          colBot[c] += shift
        })
      }

      vfNodes.push({
        id: node.id,
        type: 'topic',
        position: { x: COL_X.center, y: trunkY },
        data: { label: node.label, nodeType: node.nodeType ?? 'group', clickable: isClickable(node), courseId: node.courseId },
      })
      colBot.center = trunkY + myH + ROW_GAP
      addBranchEdges(node.id, node.children, side)
    } else {
      const y = colBot.center + (colBot.center > 10 ? TRUNK_INTERNODE_GAP : 0)
      const finalY = Math.max(y, colBot.center)
      vfNodes.push({
        id: node.id,
        type: 'topic',
        position: { x: COL_X.center, y: finalY },
        data: { label: node.label, nodeType: node.nodeType, clickable: isClickable(node), courseId: node.courseId },
      })
      colBot.center = finalY + measureHeight(node.label) + ROW_GAP
    }
  }

  // __start
  const startY = 20
  const startLabel = t('roadmapCreate.startLearningHere')
  vfNodes.push({
    id: '__start',
    type: 'topic',
    position: { x: COL_X.center, y: startY },
    data: { label: startLabel, nodeType: 'note', clickable: false },
  })
  colBot.center = startY + measureHeight(startLabel) + ROW_GAP

  trunkNodes.forEach((n) => placeTrunkNode(n))

  // __end
  const endY = colBot.center + TRUNK_INTERNODE_GAP
  vfNodes.push({
    id: '__end',
    type: 'topic',
    position: { x: COL_X.center, y: endY },
    data: { label: props.roleName, nodeType: 'note', clickable: false },
  })
  const endH = measureHeight(props.roleName)

  vfNodes.push({
    id: '__phantom_top',
    type: 'phantom',
    position: { x: COL_X.center + NODE_W / 2, y: startY - EXTEND },
    data: {},
  })
  vfNodes.push({
    id: '__phantom_bottom',
    type: 'phantom',
    position: { x: COL_X.center + NODE_W / 2, y: endY + endH + EXTEND },
    data: {},
  })

  const trunkChain = [
    '__phantom_top',
    '__start',
    ...trunkNodes.map((n) => n.id),
    '__end',
    '__phantom_bottom',
  ]
  for (let i = 0; i < trunkChain.length - 1; i++) {
    const src = trunkChain[i]
    const tgt = trunkChain[i + 1]
    const dashed = src.startsWith('__phantom') || tgt.startsWith('__phantom')
    vfEdges.push({
      id: `${src}-${tgt}`,
      source: src,
      target: tgt,
      sourceHandle: 'bottom',
      targetHandle: 'top',
      type: 'straight',
      style: {
        stroke: '#666',
        strokeWidth: 2,
        ...(dashed ? { strokeDasharray: '8,5' } : {}),
      },
    })
  }

  return { vfNodes, vfEdges }
})

const vfNodes = computed(() => renderResult.value.vfNodes)
const vfEdges = computed(() => renderResult.value.vfEdges)

/* ========== 视口控制：固定高度、锁 Y、X 夹紧 ========== */
const ZOOM = 1.1
const BOUND = 20 // 内容左右两端允许超出容器的最大像素
const PADDING = 40 // 容器底部留白
const TOP_BIAS = 1 / 3 // fitParent 模式下，内容矮于容器时顶部留白占剩余空间的比例（1/3 = 居中偏上）

const { onViewportChange, setViewport, dimensions, vueFlowRef } = useVueFlow()

// 内容包围盒（基于真实节点位置 + 测量高度）
const contentBounds = computed(() => {
  const nodes = vfNodes.value
  if (!nodes.length) return { left: 0, right: 0, top: 0, bottom: 0 }
  let minX = Infinity
  let maxX = -Infinity
  let minY = Infinity
  let maxY = -Infinity
  for (const n of nodes) {
    const x = n.position.x
    const y = n.position.y
    const w = NODE_W
    const h = typeof n.data?.label === 'string' ? measureHeight(n.data.label) : NODE_H
    if (x < minX) minX = x
    if (x + w > maxX) maxX = x + w
    if (y < minY) minY = y
    if (y + h > maxY) maxY = y + h
  }
  return { left: minX, right: maxX, top: minY, bottom: maxY }
})

// 容器高度：fitParent 模式撑满父容器，否则按内容高度
const graphHeight = computed(() => {
  const b = contentBounds.value
  if (b.bottom <= b.top) return 0
  return Math.ceil((b.bottom - b.top + PADDING) * ZOOM)
})

const containerStyle = computed(() => {
  if (props.fitParent) {
    return { height: '100%' }
  }
  return { height: graphHeight.value + 'px' }
})

// 计算 X 视口位置：内容比容器窄时居中锁死，否则在范围内夹紧
const computeViewportX = (rawX: number, vw: number): number => {
  const b = contentBounds.value
  const contentW = (b.right - b.left) * ZOOM
  const centerX = (vw - contentW) / 2 - b.left * ZOOM
  if (contentW <= vw) {
    // 内容窄于容器：始终居中
    return centerX
  }
  // 内容宽于容器：允许左右拖动，两侧各超出 BOUND
  const minX = -b.right * ZOOM + vw - BOUND
  const maxX = -b.left * ZOOM + BOUND
  return Math.min(maxX, Math.max(minX, rawX))
}

// 计算 Y 视口位置：fitParent 模式下，内容矮于容器时居中偏上，否则锁顶/夹紧
const computeViewportY = (rawY: number, vh: number): number => {
  const b = contentBounds.value
  if (!props.fitParent) {
    // 非 fitParent：容器贴合内容，Y 锁顶
    return -b.top * ZOOM
  }
  const contentH = (b.bottom - b.top) * ZOOM
  if (contentH <= vh) {
    // 内容矮于容器：居中偏上（顶部留 1/3 剩余空间，底部留 2/3）
    const free = vh - contentH
    return free * TOP_BIAS - b.top * ZOOM
  }
  // 内容高于容器：允许上下拖动，两侧各超出 BOUND
  const minY = -b.bottom * ZOOM + vh - BOUND
  const maxY = -b.top * ZOOM + BOUND
  return Math.min(maxY, Math.max(minY, rawY))
}

// 视口尺寸就绪后初始化居中
const initialized = ref(false)
watch(
  () => [dimensions.value.width, dimensions.value.height, vfNodes.value.length] as const,
  ([vw, vh, count]) => {
    if (!vw || !count || initialized.value) return
    initialized.value = true
    setViewport({
      x: computeViewportX(0, vw),
      y: computeViewportY(0, vh),
      zoom: ZOOM,
    })
  },
  { immediate: true }
)

// 拖动时：Y 居中/夹紧、X 夹紧（或居中）
onViewportChange(({ x, y, zoom }) => {
  const vw = dimensions.value.width
  const vh = dimensions.value.height
  if (!vw) return
  const clampedX = computeViewportX(x, vw)
  const targetY = computeViewportY(y, vh)
  if (clampedX !== x || y !== targetY) {
    setViewport({ x: clampedX, y: targetY, zoom })
  }
})

void vueFlowRef // 保留引用以便后续扩展

/* ========== 节点点击：跳转 ========== */
const onNodeClick = ({ node }: { node: VFNode }) => {
  const data = node.data
  if (!data?.clickable) return
  if (data.nodeType === 'course' && data.courseId) {
    window.open(`/courses/${data.courseId}`, '_blank')
  } else if (data.nodeType === 'node') {
    const m = String(node.id).match(/^n(\d+)$/)
    if (m) window.open(`/read?nodeId=${m[1]}`, '_blank')
  }
}
</script>

<style scoped>
.roadmap-viewer {
  width: 100%;
  height: 100%;
  position: relative;
}

:deep(.vue-flow__node) {
  overflow: visible;
}

:deep(.node-topic) {
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 14px;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: break-word;
  width: 200px;
  text-align: center;
  position: relative;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

:deep(.node-topic--note) {
  background: #fff8d8;
  color: #5a4a1a;
  border: 1.5px dashed #d4b85a;
  font-weight: 500;
}

:deep(.node-topic--group) {
  background: #fff;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  font-weight: 600;
}

:deep(.node-topic--placeholder) {
  background: #fff;
  color: #9e9e9e;
  border: 1.5px dashed #bdbdbd;
  font-weight: 500;
}

:deep(.node-topic--course) {
  background: #fee2e8;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  font-weight: 500;
}

:deep(.node-topic--node) {
  background: #feeadf;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  font-weight: 400;
}

:deep(.node-topic--clickable) {
  cursor: pointer;
}

:deep(.node-topic--clickable:hover) {
  border-color: #424242;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

:deep(.vue-flow__handle) {
  opacity: 0;
  pointer-events: none;
  width: 0;
  height: 0;
  min-width: 0;
  min-height: 0;
  border: none;
}
</style>
