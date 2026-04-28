<template>
  <div class="roadmap-demo">
    <VueFlow
      :nodes="nodes"
      :edges="edges"
      fit-view-on-init
      :nodes-draggable="false"
      :nodes-connectable="false"
      :elements-selectable="false"
      :min-zoom="0.2"
      :max-zoom="1.5"
      :zoom-on-scroll="true"
    >
      <Background pattern-color="#e0e0e0" :gap="20" :size="1" variant="dots" />
      <Controls :show-interactive="false" />
      <template #node-root="{ data }">
        <div class="node-root">{{ data.label }}</div>
      </template>
      <template #node-end="{ data }">
        <Handle id="top" type="target" :position="Position.Top" />
        <div class="node-end">{{ data.label }}</div>
      </template>
      <template #node-section="{ data }">
        <Handle id="top"      type="target" :position="Position.Top" />
        <Handle id="bottom"   type="source" :position="Position.Bottom" />
        <Handle id="left"     type="source" :position="Position.Left" />
        <Handle id="right"    type="source" :position="Position.Right" />
        <Handle id="left-in"  type="target" :position="Position.Left" />
        <Handle id="right-in" type="target" :position="Position.Right" />
        <div class="node-section">{{ data.label }}</div>
      </template>
      <template #node-topic="{ data }">
        <Handle id="top"       type="target" :position="Position.Top" />
        <Handle id="bottom"    type="source" :position="Position.Bottom" />
        <Handle id="left"      type="target" :position="Position.Left" />
        <Handle id="right"     type="target" :position="Position.Right" />
        <Handle id="left-out"  type="source" :position="Position.Left" />
        <Handle id="right-out" type="source" :position="Position.Right" />
        <div class="node-topic">{{ data.label }}</div>
      </template>
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { VueFlow, Handle } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { Position } from '@vue-flow/core'
import type { Node, Edge } from '@vue-flow/core'

// ─── 列定义 ───────────────────────────────────────────────
const NODE_W   = 160
const NODE_H   = 36
const COL_GAP  = 24    // 列间距
const ROW      = NODE_H + 8   // 同列连续节点的步进
const PATH_GAP = 20    // 路径与路径之间的额外间距

const COLS = ['left2', 'left1', 'center', 'right1', 'right2'] as const
type Col = typeof COLS[number]

const COL_X: Record<Col, number> = {
  left2:  300 - (NODE_W + COL_GAP) * 2,
  left1:  300 - NODE_W - COL_GAP,
  center: 300,
  right1: 300 + NODE_W + COL_GAP,
  right2: 300 + (NODE_W + COL_GAP) * 2,
}

// 每列当前的最低 y（下一个节点不能高于此值）
const colBot: Record<Col, number> = {
  left2: 0, left1: 0, center: 0, right1: 0, right2: 0,
}

// ─── 数据定义 ──────────────────────────────────────────────
interface ChildDef  { id: string; label: string }
interface TopicDef  { id: string; label: string; children?: ChildDef[] }
interface SectionDef { id: string; label: string; side: 'left' | 'right'; topics: TopicDef[] }

const sections: SectionDef[] = [
  { id: 'root', label: '从这里开始', side: 'right', topics: [] },
  {
    id: 'internet', label: '互联网基础', side: 'right',
    topics: [
      { id: 'http',    label: 'HTTP / HTTPS' },
      { id: 'dns',     label: 'DNS & 域名' },
      { id: 'browser', label: '浏览器原理' },
    ],
  },
  {
    id: 'html', label: 'HTML', side: 'left',
    topics: [
      { id: 'html-semantic', label: '语义化标签' },
      { id: 'html-form',     label: '表单与验证' },
      { id: 'html-a11y',     label: '无障碍访问' },
    ],
  },
  {
    id: 'css', label: 'CSS', side: 'right',
    topics: [
      { id: 'css-basic', label: '选择器 & 盒模型' },
      { id: 'css-flex',  label: 'Flexbox', children: [
        { id: 'css-flex-container', label: 'Container 属性' },
        { id: 'css-flex-item',      label: 'Item 属性' },
        { id: 'css-flex-align',     label: '对齐方式' },
      ]},
      { id: 'css-grid',  label: 'Grid' },
      { id: 'css-resp',  label: '响应式设计', children: [
        { id: 'css-resp-mq',     label: 'Media Query' },
        { id: 'css-resp-mobile', label: '移动端适配' },
      ]},
      { id: 'css-anim',  label: '动画与过渡' },
    ],
  },
  {
    id: 'js', label: 'JavaScript', side: 'left',
    topics: [
      { id: 'js-basic',  label: 'ES6+ 基础' },
      { id: 'js-dom',    label: 'DOM 操作' },
      { id: 'js-async',  label: 'Promise / Async' },
      { id: 'js-ts',     label: 'TypeScript' },
      { id: 'js-module', label: 'ESM 模块化' },
    ],
  },
  {
    id: 'tools', label: '工具链', side: 'right',
    topics: [
      { id: 'tools-git',  label: 'Git' },
      { id: 'tools-npm',  label: 'npm / pnpm' },
      { id: 'tools-vite', label: 'Vite' },
      { id: 'tools-lint', label: 'ESLint' },
    ],
  },
  {
    id: 'fw', label: '前端框架', side: 'left',
    topics: [
      { id: 'fw-vue',    label: 'Vue 3' },
      { id: 'fw-react',  label: 'React' },
      { id: 'fw-state',  label: '状态管理' },
      { id: 'fw-router', label: '路由管理' },
      { id: 'fw-ssr',    label: 'SSR / Nuxt' },
    ],
  },
  { id: 'end', label: '前端工程师', side: 'right', topics: [] },
]

// ─── 布局核心 ──────────────────────────────────────────────
const nodes: Node[] = []
const edges: Edge[] = []

interface PlacedNode {
  id: string
  label: string
  type: 'root' | 'end' | 'section' | 'topic'
  col: Col
  y: number
  side?: 'left' | 'right'
}

/**
 * 把一个节点 + 它的子路径 放到指定列。
 *
 * 规则：
 *   1. 节点的 y = 它的子路径区域的中心
 *   2. 子路径起始 y = 子路径列当前最低值 + PATH_GAP
 *   3. 节点 y 不能小于父列当前最低值（不和上方节点重叠）
 *   4. 如果节点被推下来，子路径整体跟着平移
 *   5. 同时推进父列和子列的 colBot
 *
 * @param parentCol  节点所在列
 * @param childCol   子节点所在列（可能未使用）
 * @returns 节点的 y 坐标
 */
function placeNodeWithChildren(
  id: string,
  label: string,
  type: 'topic' | 'section',
  parentCol: Col,
  childCol: Col | null,
  side: 'left' | 'right',
  children: ChildDef[] | undefined,
): number {
  const childCount = children?.length ?? 0

  if (childCount === 0 || childCol === null) {
    // 无子路径：节点 y = 父列最低值
    const y = colBot[parentCol]
    nodes.push({
      id, type,
      position: { x: COL_X[parentCol], y },
      data: { label, side },
    })
    colBot[parentCol] = y + ROW
    return y
  }

  // 有子路径：
  // 1. 子路径理想起始 y = 子列最低值 + PATH_GAP
  const childIdealStart = colBot[childCol] + PATH_GAP
  // 2. 节点理想 y = 子路径区域中心
  const halfSpan = ((childCount - 1) / 2) * ROW
  const nodeIdealY = childIdealStart + halfSpan
  // 3. 节点实际 y = max(理想 y, 父列最低值)
  const nodeY = Math.max(nodeIdealY, colBot[parentCol])
  // 4. 子路径跟着节点平移：子路径中心 = 节点 y
  const childStart = nodeY - halfSpan

  // 生成节点
  nodes.push({
    id, type,
    position: { x: COL_X[parentCol], y: nodeY },
    data: { label, side },
  })

  // 生成子节点
  children!.forEach((child, ci) => {
    nodes.push({
      id: child.id, type: 'topic',
      position: { x: COL_X[childCol], y: childStart + ci * ROW },
      data: { label: child.label, side },
    })
  })

  // 推进列
  colBot[parentCol] = nodeY + ROW
  colBot[childCol] = childStart + childCount * ROW

  return nodeY
}

/**
 * 处理一个 section
 */
function placeSection(sec: SectionDef, prevSecId: string | null) {
  const branchCol: Col = sec.side === 'right' ? 'right1' : 'left1'
  const childCol: Col  = sec.side === 'right' ? 'right2' : 'left2'

  // 新 section 的 sub-path 与上一条 sub-path 之间留出间隔
  if (colBot[branchCol] > 0) colBot[branchCol] += PATH_GAP
  if (colBot[childCol]  > 0) colBot[childCol]  += PATH_GAP

  const sHandle    = sec.side === 'right' ? 'right'   : 'left'
  const tInHandle  = sec.side === 'right' ? 'left'    : 'right'
  const tOutHandle = sec.side === 'right' ? 'left-out' : 'right-out'
  const sInHandle  = sec.side === 'right' ? 'right-in' : 'left-in'

  // ── 1. 先放 topic 和它们的 children，记录每个 topic 的 y ──
  const topicYs: number[] = []
  const placedFromIdx = nodes.length  // 记录起点，便于事后整体平移
  sec.topics.forEach((topic) => {
    const y = placeNodeWithChildren(
      topic.id, topic.label, 'topic',
      branchCol, topic.children?.length ? childCol : null,
      sec.side, topic.children,
    )
    topicYs.push(y)
  })

  // ── 2. 计算 section y：必须不低于 center 列；topics 中心需与 section 对齐 ──
  let sectionY: number
  if (topicYs.length > 0) {
    const mid = (topicYs[0] + topicYs[topicYs.length - 1]) / 2
    if (mid >= colBot.center) {
      // topics 自然位置已在主干下方：section y = mid
      sectionY = mid
    } else {
      // topics 整体上移到了主干上方：把 topics + children 整体下移补齐
      const shift = colBot.center - mid
      for (let i = placedFromIdx; i < nodes.length; i++) {
        nodes[i].position.y += shift
      }
      for (let i = 0; i < topicYs.length; i++) topicYs[i] += shift
      colBot[branchCol] += shift
      colBot[childCol]  += shift
      sectionY = colBot.center
    }
  } else {
    sectionY = colBot.center
  }

  // ── 3. 生成 section 节点 ──
  nodes.push({
    id: sec.id,
    type: sec.id === 'root' ? 'root' : sec.id === 'end' ? 'end' : 'section',
    position: { x: COL_X.center, y: sectionY },
    data: { label: sec.label },
  })
  colBot.center = sectionY + ROW

  // ── 4. 主干连线 ──
  if (prevSecId) {
    edges.push({
      id: `trunk-${prevSecId}-${sec.id}`,
      source: prevSecId, target: sec.id,
      sourceHandle: 'bottom', targetHandle: 'top',
      type: 'straight',
      style: { stroke: '#9e9e9e', strokeWidth: 2 },
      markerEnd: undefined,
    })
  }

  // ── 5. section 与 topic 的边、topic 之间的边、topic 与子节点的边 ──
  sec.topics.forEach((topic, ti) => {
    // section → 第一个 topic
    if (ti === 0) {
      edges.push({
        id: `branch-${sec.id}-${topic.id}`,
        source: sec.id, target: topic.id,
        sourceHandle: sHandle, targetHandle: tInHandle,
        type: 'default',
        style: { stroke: '#bdbdbd', strokeWidth: 1.5, strokeDasharray: '5,4' },
        markerEnd: undefined,
      })
    } else {
      // topic → topic（垂直）
      edges.push({
        id: `chain-${topic.id}`,
        source: sec.topics[ti - 1].id, target: topic.id,
        sourceHandle: 'bottom', targetHandle: 'top',
        type: 'default',
        style: { stroke: '#bdbdbd', strokeWidth: 1.5 },
        markerEnd: undefined,
      })
    }

    // 最后一个 topic → 回到 section
    if (ti === sec.topics.length - 1) {
      edges.push({
        id: `return-${topic.id}-${sec.id}`,
        source: topic.id, target: sec.id,
        sourceHandle: tOutHandle, targetHandle: sInHandle,
        type: 'default',
        style: { stroke: '#bdbdbd', strokeWidth: 1.5, strokeDasharray: '5,4' },
        markerEnd: undefined,
      })
    }

    // topic 的子节点
    if (topic.children?.length) {
      topic.children.forEach((child, ci) => {
        if (ci === 0) {
          edges.push({
            id: `cb-${topic.id}-${child.id}`,
            source: topic.id, target: child.id,
            sourceHandle: sHandle, targetHandle: tInHandle,
            type: 'default',
            style: { stroke: '#bdbdbd', strokeWidth: 1.5, strokeDasharray: '5,4' },
            markerEnd: undefined,
          })
        } else {
          edges.push({
            id: `cc-${child.id}`,
            source: topic.children![ci - 1].id, target: child.id,
            sourceHandle: 'bottom', targetHandle: 'top',
            type: 'default',
            style: { stroke: '#bdbdbd', strokeWidth: 1.5 },
            markerEnd: undefined,
          })
        }
      })
      const lastChild = topic.children[topic.children.length - 1]
      edges.push({
        id: `cr-${lastChild.id}-${topic.id}`,
        source: lastChild.id, target: topic.id,
        sourceHandle: tOutHandle, targetHandle: sHandle,
        type: 'default',
        style: { stroke: '#bdbdbd', strokeWidth: 1.5, strokeDasharray: '5,4' },
        markerEnd: undefined,
      })
    }
  })
}

// ─── 执行布局 ──────────────────────────────────────────────
sections.forEach((sec, si) => {
  placeSection(sec, si > 0 ? sections[si - 1].id : null)
})

// 抑制未使用变量警告
void COLS
</script>

<style scoped>
.roadmap-demo {
  width: 100%;
  height: 100%;
}

:deep(.node-root),
:deep(.node-end) {
  background: #1a1a1a;
  color: #fff;
  border-radius: 8px;
  padding: 8px 0;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  width: 160px;
  text-align: center;
  letter-spacing: 0.5px;
}

:deep(.node-section) {
  background: #fff;
  color: #1a1a1a;
  border: 2px solid #1a1a1a;
  border-radius: 6px;
  padding: 8px 0;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  width: 160px;
  text-align: center;
}

:deep(.node-topic) {
  background: #fff;
  color: #424242;
  border: 1.5px solid #d0d0d0;
  border-radius: 6px;
  padding: 8px 0;
  font-size: 13px;
  font-weight: 400;
  white-space: nowrap;
  width: 160px;
  text-align: center;
}

:deep(.vue-flow__node:hover .node-section),
:deep(.vue-flow__node:hover .node-topic) {
  border-color: #424242;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  cursor: pointer;
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
