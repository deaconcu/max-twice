<template>
  <div class="roadmap-editor">
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
      :min-zoom="1.1"
      :max-zoom="1.1"
      @node-mouse-enter="onNodeMouseEnter"
      @node-mouse-leave="onNodeMouseLeave"
      @pane-click="onPaneClick"
    >
      <Background pattern-color="#e0e0e0" :gap="20" :size="1" variant="dots" />

      <template #node-phantom>
        <Handle id="top" type="target" :position="Position.Top" />
        <Handle id="bottom" type="source" :position="Position.Bottom" />
        <div style="width: 0; height: 0" />
      </template>

      <template #node-topic="{ id, data }">
        <Handle id="top" type="target" :position="Position.Top" />
        <Handle id="bottom" type="source" :position="Position.Bottom" />
        <Handle id="left" type="source" :position="Position.Left" />
        <Handle id="left-in" type="target" :position="Position.Left" />
        <Handle id="right" type="source" :position="Position.Right" />
        <Handle id="right-in" type="target" :position="Position.Right" />
        <div class="node-wrapper">
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
              { 'node-topic--selected': selectedNodeForBinding === id },
            ]"
          >
            <input
              v-if="editingNodeId === id"
              :ref="(el) => bindEditingInput(el as HTMLInputElement | null)"
              v-model.trim="editingLabel"
              :maxlength="editingMaxLength"
              class="node-label-input"
              @keydown.enter.prevent="commitLabelEdit"
              @keydown.esc.prevent="cancelLabelEdit"
              @blur="commitLabelEdit"
              @click.stop
            />
            <span v-else>{{ data.label }}</span>
          </div>
          <div v-if="hoveredNodeId === id && editingNodeId !== id" class="node-actions">
            <!-- __start 只能在后面插入 -->
            <v-tooltip v-if="id === '__start'" text="在后面插入" location="top" :open-delay="100">
              <template #activator="{ props: tipProps }">
                <button
                  v-bind="tipProps"
                  class="node-action-btn"
                  @click="insertTrunkAtIndex(0)"
                >
                  <v-icon icon="mdi-table-row-plus-after" size="16" />
                </button>
              </template>
            </v-tooltip>
            <!-- __end 只能在前面插入 -->
            <v-tooltip v-else-if="id === '__end'" text="在前面插入" location="top" :open-delay="100">
              <template #activator="{ props: tipProps }">
                <button
                  v-bind="tipProps"
                  class="node-action-btn"
                  @click="insertTrunkAtIndex(modelValue.length)"
                >
                  <v-icon icon="mdi-table-row-plus-before" size="16" />
                </button>
              </template>
            </v-tooltip>
            <!-- 普通节点完整菜单 -->
            <template v-else>
              <v-tooltip
                v-if="data.nodeType === 'group' || data.nodeType === 'note'"
                text="重命名"
                location="top"
                :open-delay="100"
              >
                <template #activator="{ props: tipProps }">
                  <button
                    v-bind="tipProps"
                    class="node-action-btn"
                    @click="startLabelEdit(id, data.label)"
                  >
                    <v-icon icon="mdi-pencil-outline" size="16" />
                  </button>
                </template>
              </v-tooltip>
              <v-tooltip text="在前面插入" location="top" :open-delay="100">
                <template #activator="{ props: tipProps }">
                  <button v-bind="tipProps" class="node-action-btn" @click="insertBefore(id)">
                    <v-icon icon="mdi-table-row-plus-before" size="16" />
                  </button>
                </template>
              </v-tooltip>
              <v-tooltip text="在后面插入" location="top" :open-delay="100">
                <template #activator="{ props: tipProps }">
                  <button v-bind="tipProps" class="node-action-btn" @click="insertAfter(id)">
                    <v-icon icon="mdi-table-row-plus-after" size="16" />
                  </button>
                </template>
              </v-tooltip>
              <v-tooltip v-if="!hasChildren(id)" text="设为课程节点" location="top" :open-delay="100">
                <template #activator="{ props: tipProps }">
                  <button v-bind="tipProps" class="node-action-btn" @click="bindAsCourse(id)">
                    <v-icon icon="mdi-book-outline" size="16" />
                  </button>
                </template>
              </v-tooltip>
              <v-tooltip v-if="!hasChildren(id)" text="设为节点" location="top" :open-delay="100">
                <template #activator="{ props: tipProps }">
                  <button v-bind="tipProps" class="node-action-btn" @click="bindAsNode(id)">
                    <v-icon icon="mdi-file-document-outline" size="16" />
                  </button>
                </template>
              </v-tooltip>
              <v-tooltip
                v-if="!hasChildren(id) && data.nodeType !== 'note'"
                text="设为说明节点"
                location="top"
                :open-delay="100"
              >
                <template #activator="{ props: tipProps }">
                  <button v-bind="tipProps" class="node-action-btn" @click="bindAsNote(id)">
                    <v-icon icon="mdi-note-text-outline" size="16" />
                  </button>
                </template>
              </v-tooltip>
              <v-tooltip
                v-if="!hasChildren(id) && canBranch(id)"
                text="创建子路径"
                location="top"
                :open-delay="100"
              >
                <template #activator="{ props: tipProps }">
                  <button v-bind="tipProps" class="node-action-btn" @click="createBranch(id)">
                    <v-icon icon="mdi-source-branch" size="16" />
                  </button>
                </template>
              </v-tooltip>
              <v-tooltip
                v-else-if="hasChildren(id)"
                text="移除子路径"
                location="top"
                :open-delay="100"
              >
                <template #activator="{ props: tipProps }">
                  <button v-bind="tipProps" class="node-action-btn" @click="removeBranch(id)">
                    <v-icon icon="mdi-source-branch-remove" size="16" />
                  </button>
                </template>
              </v-tooltip>
              <v-tooltip text="删除节点" location="top" :open-delay="100">
                <template #activator="{ props: tipProps }">
                  <button v-bind="tipProps" class="node-action-btn" @click="deleteNode(id)">
                    <v-icon icon="mdi-trash-can-outline" size="16" />
                  </button>
                </template>
              </v-tooltip>
            </template>
          </div>
        </div>
      </template>
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { VueFlow, Handle, Position } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import type { Edge as VFEdge, Node as VFNode } from '@vue-flow/core'
import { useI18n } from '@/composables/useI18n'

/* ========== 类型定义 ========== */
export interface RoadmapNode {
  id: string // 'c{courseId}' | 'n{nodeId}' | 'g_{tmp}'（group）
  label: string
  nodeType?: 'course' | 'node' | 'group' | 'note'
  courseId?: number
  children?: RoadmapNode[]
}

export interface BindPayload {
  type: 'course' | 'node'
  id: number
  label: string
  rootNodeId?: number // course 时使用 rootNodeId 作为 vue-flow id
}

const props = defineProps<{
  modelValue: RoadmapNode[]
  roleName: string
}>()

const emit = defineEmits<{
  'update:modelValue': [RoadmapNode[]]
  'request-bind': [{ nodeId: string; type: 'course' | 'node' }]
  'cancel-bind': []
  'show-message': [message: string, type?: string]
}>()

const { t } = useI18n()

/* ========== 内部状态 ========== */
const selectedNodeForBinding = ref<string | null>(null)
const hoveredNodeId = ref<string | null>(null)
const editingNodeId = ref<string | null>(null)
const editingLabel = ref('')

let tmpSeq = 0
const genGroupId = () => `g_${Date.now()}_${++tmpSeq}`

/* ========== 常量 ========== */
const NODE_W = 160
const NODE_H = 36
const COL_GAP = 40
const ROW_GAP = 8
const PATH_GAP = 60
const TRUNK_GAP = 60
const TRUNK_INTERNODE_GAP = TRUNK_GAP - NODE_H - ROW_GAP // 等高时保持原视觉间距
const EXTEND = 40
const MAX_DEPTH = 4

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

/* ========== 树查找/操作 ========== */
function findNode(
  trunk: RoadmapNode[],
  id: string,
  depth = 1
): { node: RoadmapNode; depth: number } | null {
  for (const n of trunk) {
    if (n.id === id) return { node: n, depth }
    if (n.children) {
      const sub = findNode(n.children, id, depth + 1)
      if (sub) return sub
    }
  }
  return null
}

function nodeExists(trunk: RoadmapNode[], id: string): boolean {
  return findNode(trunk, id) !== null
}

function insertSibling(
  trunk: RoadmapNode[],
  targetId: string,
  newNode: RoadmapNode,
  position: 'before' | 'after'
): RoadmapNode[] {
  const idx = trunk.findIndex((n) => n.id === targetId)
  if (idx >= 0) {
    const result = [...trunk]
    result.splice(position === 'before' ? idx : idx + 1, 0, newNode)
    return result
  }
  return trunk.map((n) => {
    if (!n.children) return n
    const newChildren = insertSibling(n.children, targetId, newNode, position)
    return newChildren === n.children ? n : { ...n, children: newChildren }
  })
}

function setChildrenOf(
  trunk: RoadmapNode[],
  targetId: string,
  children: RoadmapNode[] | undefined
): RoadmapNode[] {
  const idx = trunk.findIndex((n) => n.id === targetId)
  if (idx >= 0) {
    const result = [...trunk]
    if (children === undefined || children.length === 0) {
      const { children: _, ...rest } = result[idx]
      result[idx] = rest
    } else {
      result[idx] = { ...result[idx], children }
    }
    return result
  }
  return trunk.map((n) => {
    if (!n.children) return n
    const newChildren = setChildrenOf(n.children, targetId, children)
    return newChildren === n.children ? n : { ...n, children: newChildren }
  })
}

function deleteNodeFromTree(trunk: RoadmapNode[], id: string): RoadmapNode[] {
  const idx = trunk.findIndex((n) => n.id === id)
  if (idx >= 0) {
    return trunk.filter((_, i) => i !== idx)
  }
  return trunk.map((n) => {
    if (!n.children) return n
    const newChildren = deleteNodeFromTree(n.children, id)
    if (newChildren === n.children) return n
    return newChildren.length ? { ...n, children: newChildren } : (() => {
      const { children: _c, ...rest } = n
      return rest
    })()
  })
}

function setLabelOf(
  trunk: RoadmapNode[],
  targetId: string,
  label: string
): RoadmapNode[] {
  const idx = trunk.findIndex((n) => n.id === targetId)
  if (idx >= 0) {
    const result = [...trunk]
    result[idx] = { ...result[idx], label }
    return result
  }
  return trunk.map((n) => {
    if (!n.children) return n
    const newChildren = setLabelOf(n.children, targetId, label)
    return newChildren === n.children ? n : { ...n, children: newChildren }
  })
}

function replaceNodeAt(
  trunk: RoadmapNode[],
  targetId: string,
  newNode: RoadmapNode
): RoadmapNode[] {
  const idx = trunk.findIndex((n) => n.id === targetId)
  if (idx >= 0) {
    const result = [...trunk]
    // 绑定为 course/node 后丢弃原 children（一个具体的课程/节点不应再有子路径）
    result[idx] = newNode
    return result
  }
  return trunk.map((n) => {
    if (!n.children) return n
    const newChildren = replaceNodeAt(n.children, targetId, newNode)
    return newChildren === n.children ? n : { ...n, children: newChildren }
  })
}

/* ========== 渲染：trunk → VueFlow nodes/edges ========== */
// 通过隐藏 div 测量节点真实高度（基于实际 label 文本与 .node-topic 样式）
let measureEl: HTMLDivElement | null = null
const heightCache = new Map<string, number>()

const ensureMeasureEl = (): HTMLDivElement | null => {
  if (typeof document === 'undefined') return null
  if (measureEl && document.body.contains(measureEl)) return measureEl
  const el = document.createElement('div')
  // 与 .node-topic 一致的关键样式
  el.style.cssText = [
    'position:absolute',
    'visibility:hidden',
    'pointer-events:none',
    'top:-9999px',
    'left:-9999px',
    `width:${NODE_W}px`,
    'padding:8px 12px',
    'font-size:13px',
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

const renderResult = computed<{ vfNodes: VFNode[]; vfEdges: VFEdge[] }>(() => {
  const trunk = props.modelValue
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

  // 递归放置一个分支节点（含子孙）
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
      vfNodes.push({
        id: node.id,
        type: 'topic',
        position: { x: COL_X[myCol], y },
        data: { label: node.label, nodeType: node.nodeType },
      })
      colBot[myCol] = y + measureHeight(node.label) + ROW_GAP
      return y
    }

    const childCol = childCols[0]
    if (colBot[childCol] > 10) colBot[childCol] += PATH_GAP

    const startIdx = vfNodes.length
    const childYs: number[] = []
    node.children!.forEach((c) => {
      childYs.push(placeBranchNode(c, childCols, side))
    })

    const childMid = (childYs[0] + childYs[childYs.length - 1]) / 2
    const myY = Math.max(childMid, colBot[myCol])
    const shift = myY - childMid
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
      data: { label: node.label, nodeType: node.nodeType ?? 'group' },
    })
    colBot[myCol] = myY + measureHeight(node.label) + ROW_GAP

    return myY
  }

  // 主干节点
  const placeTrunkNode = (node: RoadmapNode) => {
    // 选更空的一侧：取该侧 3 列中最大的 colBot 作为代表
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
      const childYs: number[] = []
      node.children.forEach((c) => {
        childYs.push(placeBranchNode(c, subCols, side))
      })
      const childMid = (childYs[0] + childYs[childYs.length - 1]) / 2
      const trunkMinY = colBot.center + (colBot.center > 10 ? TRUNK_INTERNODE_GAP : 0)
      let trunkY = Math.max(childMid, trunkMinY)
      const shift = trunkY - childMid
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
        data: { label: node.label, nodeType: node.nodeType ?? 'group' },
      })
      colBot.center = trunkY + measureHeight(node.label) + ROW_GAP
      addBranchEdges(node.id, node.children, side)
    } else {
      const y = colBot.center + (colBot.center > 10 ? TRUNK_INTERNODE_GAP : 0)
      const finalY = Math.max(y, colBot.center)
      vfNodes.push({
        id: node.id,
        type: 'topic',
        position: { x: COL_X.center, y: finalY },
        data: { label: node.label, nodeType: node.nodeType },
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
    data: { label: startLabel, nodeType: 'note' },
  })
  colBot.center = startY + measureHeight(startLabel) + ROW_GAP

  // trunk
  props.modelValue.forEach((n) => placeTrunkNode(n))

  // __end
  const endY = colBot.center + TRUNK_INTERNODE_GAP
  vfNodes.push({
    id: '__end',
    type: 'topic',
    position: { x: COL_X.center, y: endY },
    data: { label: props.roleName, nodeType: 'note' },
  })
  const endH = measureHeight(props.roleName)

  // phantoms
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

  // 主干 edges
  const trunkChain = [
    '__phantom_top',
    '__start',
    ...trunk.map((n) => n.id),
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
        strokeWidth: 3,
        ...(dashed ? { strokeDasharray: '8,5' } : {}),
      },
    })
  }

  return { vfNodes, vfEdges }
})

const vfNodes = computed(() => renderResult.value.vfNodes)
const vfEdges = computed(() => renderResult.value.vfEdges)

/* ========== 取消选中 ========== */
const onNodeMouseEnter = (event: { node: VFNode }) => {
  hoveredNodeId.value = event.node.id
}
const onNodeMouseLeave = () => {
  hoveredNodeId.value = null
}

const onPaneClick = () => {
  if (selectedNodeForBinding.value) {
    selectedNodeForBinding.value = null
    emit('cancel-bind')
  }
}

/* ========== 操作 ========== */
const hasChildren = (id: string): boolean => {
  const f = findNode(props.modelValue, id)
  return !!f?.node.children?.length
}

const canBranch = (id: string): boolean => {
  const f = findNode(props.modelValue, id)
  if (!f) return false
  return f.depth < MAX_DEPTH
}

const insertBefore = (id: string) => {
  const newId = genGroupId()
  const newNode: RoadmapNode = { id: newId, label: '新节点' }
  emit('update:modelValue', insertSibling(props.modelValue, id, newNode, 'before'))
}

const insertAfter = (id: string) => {
  const newId = genGroupId()
  const newNode: RoadmapNode = { id: newId, label: '新节点' }
  emit('update:modelValue', insertSibling(props.modelValue, id, newNode, 'after'))
}

// 在主干指定索引位置插入（用于 __start 后插 / __end 前插）
const insertTrunkAtIndex = (index: number) => {
  const newNode: RoadmapNode = { id: genGroupId(), label: '新节点' }
  const result = [...props.modelValue]
  result.splice(index, 0, newNode)
  emit('update:modelValue', result)
}

const deleteNode = (id: string) => {
  emit('update:modelValue', deleteNodeFromTree(props.modelValue, id))
  if (selectedNodeForBinding.value === id) {
    selectedNodeForBinding.value = null
    emit('cancel-bind')
  }
}

const createBranch = (id: string) => {
  const f = findNode(props.modelValue, id)
  if (!f) return
  if (f.depth >= MAX_DEPTH) {
    emit('show-message', '已达最大分支层级', 'warning')
    return
  }
  if (f.node.children?.length) {
    emit('show-message', '该节点已有子路径', 'warning')
    return
  }
  const c1: RoadmapNode = { id: genGroupId(), label: '新节点' }
  const c2: RoadmapNode = { id: genGroupId(), label: '新节点' }
  emit('update:modelValue', setChildrenOf(props.modelValue, id, [c1, c2]))
  // 创建子路径后自动激活当前节点的重命名（提示用户给组合节点起名）
  startLabelEdit(id, f.node.label)
}

const removeBranch = (id: string) => {
  emit('update:modelValue', setChildrenOf(props.modelValue, id, undefined))
}

const bindAsCourse = (id: string) => {
  selectedNodeForBinding.value = id
  emit('request-bind', { nodeId: id, type: 'course' })
}

const bindAsNode = (id: string) => {
  selectedNodeForBinding.value = id
  emit('request-bind', { nodeId: id, type: 'node' })
}

const bindAsNote = (id: string) => {
  const f = findNode(props.modelValue, id)
  if (!f) return
  // 切换为说明节点：清除 courseId、children，nodeType 设为 note
  const newNode: RoadmapNode = {
    id: f.node.id,
    label: f.node.label,
    nodeType: 'note',
  }
  emit('update:modelValue', replaceNodeAt(props.modelValue, id, newNode))
  // 自动激活重命名让用户输入说明文本
  startLabelEdit(id, f.node.label)
}

/* ========== 重命名 ========== */
const LABEL_MAX_GROUP = 20
const LABEL_MAX_NOTE = 50

const editingMaxLength = computed(() => {
  const id = editingNodeId.value
  if (!id) return LABEL_MAX_GROUP
  const f = findNode(props.modelValue, id)
  return f?.node.nodeType === 'note' ? LABEL_MAX_NOTE : LABEL_MAX_GROUP
})

const startLabelEdit = (id: string, currentLabel: string) => {
  editingNodeId.value = id
  editingLabel.value = currentLabel
}

const editingInputEl = ref<HTMLInputElement | null>(null)
const bindEditingInput = (el: HTMLInputElement | null) => {
  // 只在首次绑定（el 从无到有）时聚焦并全选；后续重渲染保持光标位置不变
  if (el && editingInputEl.value !== el) {
    editingInputEl.value = el
    queueMicrotask(() => {
      el.focus()
      el.select()
    })
  } else if (!el) {
    editingInputEl.value = null
  }
}

const commitLabelEdit = () => {
  if (!editingNodeId.value) return
  const id = editingNodeId.value
  const max = editingMaxLength.value
  const label = (editingLabel.value.trim() || '新节点').slice(0, max)
  emit('update:modelValue', setLabelOf(props.modelValue, id, label))
  editingNodeId.value = null
  editingLabel.value = ''
}

const cancelLabelEdit = () => {
  editingNodeId.value = null
  editingLabel.value = ''
}

/* ========== 父组件接口 ========== */
const applyBinding = (oldId: string, payload: BindPayload) => {
  const newId =
    payload.type === 'course'
      ? `c${payload.rootNodeId ?? payload.id}`
      : `n${payload.id}`

  if (newId !== oldId && nodeExists(props.modelValue, newId)) {
    emit(
      'show-message',
      payload.type === 'course'
        ? t('roadmapCreate.messages.courseAlreadyAdded')
        : t('roadmapCreate.messages.nodeAlreadyAdded'),
      'warning'
    )
    return
  }
  const newNode: RoadmapNode = {
    id: newId,
    label: payload.label,
    nodeType: payload.type,
    ...(payload.type === 'course' && payload.id ? { courseId: payload.id } : {}),
  }
  emit('update:modelValue', replaceNodeAt(props.modelValue, oldId, newNode))
  selectedNodeForBinding.value = newId
}

const isNodeAddedById = (id: string | number, type: 'course' | 'node'): boolean => {
  const prefix = type === 'course' ? 'c' : 'n'
  return nodeExists(props.modelValue, `${prefix}${id}`)
}

const cancelSelection = () => {
  selectedNodeForBinding.value = null
}

defineExpose({
  applyBinding,
  isNodeAddedById,
  cancelSelection,
  getSelectedNodeId: () => selectedNodeForBinding.value,
})
</script>

<style scoped>
.roadmap-editor {
  width: 100%;
  height: 100%;
  position: relative;
}

:deep(.vue-flow__node) {
  overflow: visible;
}

:deep(.node-wrapper) {
  position: relative;
  display: inline-block;
}

:deep(.node-actions) {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  display: grid;
  grid-template-columns: repeat(4, 26px);
  gap: 4px;
  z-index: 10;
  justify-content: center;
}

:deep(.node-action-btn) {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgb(var(--v-theme-primary));
  color: rgb(var(--v-theme-on-primary));
  border: 1px solid rgba(0, 0, 0, 0.15);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  box-shadow: 0 2px 6px rgba(var(--v-theme-primary), 0.35);
  transition:
    background 0.15s ease,
    color 0.15s ease,
    transform 0.1s ease,
    filter 0.15s ease;
}

:deep(.node-action-btn:hover) {
  filter: brightness(0.9);
  transform: scale(1.08);
}

:deep(.node-topic) {
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: break-word;
  width: 160px;
  text-align: center;
  position: relative;
}

/* 说明节点：浅黄便签风格 */
:deep(.node-topic--note) {
  background: #fff8d8;
  color: #5a4a1a;
  border: 1.5px dashed #d4b85a;
  font-weight: 500;
  font-style: italic;
}

:deep(.node-topic--group) {
  background: #fff;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  font-weight: 600;
}

/* 占位节点：未绑定课程/节点/组合的新节点，用虚线边框区分 */
:deep(.node-topic--placeholder) {
  background: #fff;
  color: #1a1a1a;
  border: 1.5px dashed #1a1a1a;
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

:deep(.node-topic--selected) {
  outline: 3px solid #ff9800;
  outline-offset: 2px;
  box-shadow: 0 0 0 6px rgba(255, 152, 0, 0.15);
}

:deep(.node-label-input) {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  text-align: center;
  font-size: inherit;
  font-weight: inherit;
  color: inherit;
  padding: 0;
  font-family: inherit;
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
