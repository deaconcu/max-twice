<template>
  <div class="roadmap-demo" :style="{ height: graphHeight + 'px' }">
    <VueFlow
      :nodes="nodes"
      :edges="edges"
      :nodes-draggable="false"
      :nodes-connectable="false"
      :elements-selectable="false"
      :zoom-on-scroll="false"
      :zoom-on-pinch="false"
      :zoom-on-double-click="false"
      :prevent-scrolling="false"
      :min-zoom="1.1"
      :max-zoom="1.1"
    >
      <Background pattern-color="#e0e0e0" :gap="20" :size="1" variant="dots" />
      <template #node-root="{ data }">
        <Handle id="top" type="target" :position="Position.Top" />
        <div class="node-root">{{ data.label }}</div>
        <Handle id="bottom" type="source" :position="Position.Bottom" />
      </template>
      <template #node-end="{ data }">
        <Handle id="top" type="target" :position="Position.Top" />
        <div class="node-end">{{ data.label }}</div>
        <Handle id="bottom" type="source" :position="Position.Bottom" />
      </template>
      <template #node-phantom>
        <Handle id="bottom" type="source" :position="Position.Bottom" />
        <Handle id="top" type="target" :position="Position.Top" />
        <div style="width: 0; height: 0" />
      </template>
      <template #node-section="{ data }">
        <Handle id="top" type="target" :position="Position.Top" />
        <Handle id="bottom" type="source" :position="Position.Bottom" />
        <Handle id="left" type="source" :position="Position.Left" />
        <Handle id="right" type="source" :position="Position.Right" />
        <Handle id="left-in" type="target" :position="Position.Left" />
        <Handle id="right-in" type="target" :position="Position.Right" />
        <div class="node-section">{{ data.label }}</div>
      </template>
      <template #node-topic="{ data }">
        <Handle id="top" type="target" :position="Position.Top" />
        <Handle id="bottom" type="source" :position="Position.Bottom" />
        <Handle id="left" type="target" :position="Position.Left" />
        <Handle id="right" type="target" :position="Position.Right" />
        <Handle id="left-out" type="source" :position="Position.Left" />
        <Handle id="right-out" type="source" :position="Position.Right" />
        <div
          :class="[
            'node-topic',
            data.hasChildren
              ? 'node-topic--group'
              : data.nodeType === 'course'
                ? 'node-topic--course'
                : 'node-topic--node',
          ]"
        >
          <span v-if="data.done" class="node-done-badge">✓</span>
          {{ data.label }}
        </div>
      </template>
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { VueFlow, Handle, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Position } from '@vue-flow/core'
import type { Node, Edge } from '@vue-flow/core'

const { onViewportChange, setViewport, dimensions } = useVueFlow()
const BOUND = 20
const initialized = ref(false)

// 内容边界（节点布局完成后才计算，所以用 computed 延迟到 nodes 填充后）
const contentBounds = computed(() => {
  if (nodes.length === 0) return { left: 0, right: 0, top: 0, bottom: 0 }
  const xs = nodes.map((n) => n.position.x)
  const ys = nodes.map((n) => n.position.y)
  return {
    left: Math.min(...xs),
    right: Math.max(...xs) + NODE_W,
    top: Math.min(...ys),
    bottom: Math.max(...ys) + NODE_H,
  }
})

// 视口宽度就绪后居中
watch(
  () => dimensions.value.width,
  (vw) => {
    if (!vw || initialized.value) return
    initialized.value = true
    const b = contentBounds.value
    // viewport x 是屏幕坐标：屏幕位置 = nodeX * zoom + x
    // 居中：x = (vw - contentWidth * zoom) / 2 - left * zoom
    const centerX = (vw - (b.right - b.left) * ZOOM) / 2 - b.left * ZOOM
    setViewport({ x: centerX, y: 0, zoom: ZOOM })
  },
  { immediate: true }
)

// 锁定 y=0，水平边界夹紧（屏幕像素坐标）
onViewportChange(({ x, y, zoom }) => {
  const vw = dimensions.value.width
  if (!vw) return

  const b = contentBounds.value
  // 内容左边界在屏幕上的位置 = b.left * zoom + x，不能小于 -BOUND
  // => x >= -b.left * zoom - BOUND
  const minX = -b.right * zoom + vw - BOUND
  const maxX = -b.left * zoom + BOUND

  const clampedX = Math.min(maxX, Math.max(minX, x))
  const needClamp = clampedX !== x || y !== 0
  if (needClamp) setViewport({ x: clampedX, y: 0, zoom })
})

// ─── 列定义 ───────────────────────────────────────────────
const NODE_W = 160
const NODE_H = 36
const COL_GAP = 40 // 列间距
const ROW = NODE_H + 8 // 同列连续节点的步进
const PATH_GAP = 20 // 路径与路径之间的额外间距
const TRUNK_GAP = 20 // 主干节点之间的最小额外间距

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

// 每列当前的最低 y（下一个节点不能高于此值）
const colBot: Record<Col, number> = {
  left3: 10,
  left2: 10,
  left1: 10,
  center: 40,
  right1: 10,
  right2: 10,
  right3: 10,
}

// ─── 数据定义 ──────────────────────────────────────────────
interface ChildDef {
  id: string
  label: string
  done?: boolean
  nodeType?: 'course' | 'node'
  children?: ChildDef[]
}
interface TopicDef {
  id: string
  label: string
  done?: boolean
  nodeType?: 'course' | 'node'
  children?: ChildDef[]
}
interface SectionDef {
  id: string
  label: string
  side: 'left' | 'right'
  topics: TopicDef[]
}

const sections: SectionDef[] = [
  { id: 'root', label: '从这里开始', side: 'right', topics: [] },
  {
    id: 'internet',
    label: '互联网基础',
    side: 'right',
    topics: [
      {
        id: 'http',
        label: 'HTTP / HTTPS',
        done: true,
        children: [
          { id: 'http-methods', label: '请求方法', done: true, nodeType: 'node' },
          { id: 'http-status', label: '状态码', done: true, nodeType: 'node' },
          { id: 'http-headers', label: '请求头', nodeType: 'node' },
        ],
      },
      { id: 'dns', label: 'DNS & 域名', done: true },
      {
        id: 'browser',
        label: '浏览器原理',
        children: [
          { id: 'browser-render', label: '渲染流程' },
          { id: 'browser-event', label: '事件循环' },
        ],
      },
      { id: 'hosting', label: '域名 & 托管' },
    ],
  },
  {
    id: 'html',
    label: 'HTML',
    side: 'left',
    topics: [
      {
        id: 'html-semantic',
        label: '语义化标签',
        children: [
          { id: 'html-sem-tags', label: '常用语义标签' },
          { id: 'html-sem-struct', label: '页面结构' },
        ],
      },
      {
        id: 'html-form',
        label: '表单与验证',
        children: [
          { id: 'html-form-input', label: '表单控件' },
          { id: 'html-form-validate', label: '原生验证' },
          { id: 'html-form-submit', label: '提交方式' },
        ],
      },
      { id: 'html-a11y', label: '无障碍访问' },
      { id: 'html-seo', label: 'SEO 基础' },
      { id: 'html-meta', label: 'Meta 标签' },
    ],
  },
  {
    id: 'css',
    label: 'CSS',
    side: 'right',
    topics: [
      { id: 'css-basic', label: '选择器 & 盒模型' },
      {
        id: 'css-flex',
        label: 'Flexbox',
        children: [
          { id: 'css-flex-container', label: 'Container 属性' },
          { id: 'css-flex-item', label: 'Item 属性' },
          { id: 'css-flex-align', label: '对齐方式' },
        ],
      },
      {
        id: 'css-grid',
        label: 'Grid',
        children: [
          { id: 'css-grid-template', label: 'Grid Template' },
          { id: 'css-grid-area', label: 'Grid Area' },
        ],
      },
      {
        id: 'css-resp',
        label: '响应式设计',
        children: [
          { id: 'css-resp-mq', label: 'Media Query' },
          { id: 'css-resp-mobile', label: '移动端适配' },
        ],
      },
      { id: 'css-anim', label: '动画与过渡' },
      { id: 'css-var', label: 'CSS 变量' },
    ],
  },
  {
    id: 'js',
    label: 'JavaScript',
    side: 'left',
    topics: [
      { id: 'js-basic', label: 'ES6+ 基础' },
      { id: 'js-dom', label: 'DOM 操作' },
      {
        id: 'js-async',
        label: 'Promise / Async',
        children: [
          { id: 'js-async-promise', label: 'Promise' },
          { id: 'js-async-await', label: 'Async / Await' },
          { id: 'js-async-rx', label: 'RxJS 基础' },
        ],
      },
      {
        id: 'js-ts',
        label: 'TypeScript',
        children: [
          { id: 'js-ts-type', label: '类型系统' },
          { id: 'js-ts-generic', label: '泛型' },
        ],
      },
      { id: 'js-module', label: 'ESM 模块化' },
      { id: 'js-fetch', label: 'Fetch / Ajax' },
    ],
  },
  {
    id: 'tools',
    label: '工具链',
    side: 'right',
    topics: [
      {
        id: 'tools-git',
        label: 'Git',
        children: [
          { id: 'tools-git-branch', label: '分支管理' },
          { id: 'tools-git-pr', label: 'PR & Code Review' },
        ],
      },
      { id: 'tools-npm', label: 'npm / pnpm' },
      { id: 'tools-vite', label: 'Vite / Webpack' },
      { id: 'tools-lint', label: 'ESLint & Prettier' },
      { id: 'tools-test', label: '单元测试 Vitest' },
    ],
  },
  {
    id: 'fw',
    label: '前端框架',
    side: 'left',
    topics: [
      {
        id: 'fw-vue',
        label: 'Vue 3',
        children: [
          { id: 'fw-vue-comp', label: 'Composition API' },
          { id: 'fw-vue-pinia', label: 'Pinia' },
          { id: 'fw-vue-router', label: 'Vue Router' },
        ],
      },
      {
        id: 'fw-react',
        label: 'React',
        children: [
          { id: 'fw-react-hooks', label: 'Hooks' },
          { id: 'fw-react-redux', label: 'Redux / Zustand' },
        ],
      },
      { id: 'fw-ssr', label: 'SSR / Nuxt' },
    ],
  },
  {
    id: 'perf',
    label: '性能优化',
    side: 'right',
    topics: [
      { id: 'perf-load', label: '加载优化' },
      { id: 'perf-render', label: '渲染优化' },
      { id: 'perf-cache', label: '缓存策略' },
      { id: 'perf-lazy', label: '懒加载 & 代码分割' },
    ],
  },
  {
    id: 'security',
    label: '安全',
    side: 'left',
    topics: [
      { id: 'sec-xss', label: 'XSS 防护' },
      { id: 'sec-csrf', label: 'CSRF 防护' },
      { id: 'sec-https', label: 'HTTPS & CSP' },
      { id: 'sec-auth', label: '认证 & 授权' },
    ],
  },
  {
    id: 'deploy',
    label: '部署 & DevOps',
    side: 'right',
    topics: [
      { id: 'deploy-ci', label: 'CI / CD' },
      { id: 'deploy-docker', label: 'Docker 基础' },
      { id: 'deploy-cdn', label: 'CDN & 静态托管' },
      { id: 'deploy-monitor', label: '监控 & 日志' },
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
 * 递归放置一个节点及其所有后代。
 *
 * @param id        节点 id
 * @param label     节点 label
 * @param type      节点类型
 * @param cols      列序列（自身所在列 + 各级子孙所在列）
 * @param side      所在侧
 * @param children  子节点（递归结构）
 * @returns         本节点的 y 坐标
 */
function placeNodeWithChildren(
  id: string,
  label: string,
  type: 'topic' | 'section',
  cols: Col[],
  side: 'left' | 'right',
  children: ChildDef[] | undefined,
  depth = 1,
  done = false,
  nodeType: 'course' | 'node' = 'course'
): number {
  const parentCol = cols[0]
  const childCols = cols.slice(1)
  const childCount = children?.length ?? 0
  const hasChildren = childCount > 0 && childCols.length > 0

  if (!hasChildren) {
    const y = colBot[parentCol]
    nodes.push({
      id,
      type,
      position: { x: COL_X[parentCol], y },
      data: { label, side, depth, done, hasChildren: false, nodeType },
    })
    colBot[parentCol] = y + ROW
    return y
  }

  const childCol = childCols[0]
  if (colBot[childCol] > 0) colBot[childCol] += PATH_GAP

  const childStartIdx = nodes.length
  const childYs: number[] = []
  children!.forEach((child) => {
    const cy = placeNodeWithChildren(
      child.id,
      child.label,
      'topic',
      childCols,
      side,
      child.children,
      depth + 1,
      child.done ?? false,
      child.nodeType ?? 'node'
    )
    childYs.push(cy)
  })
  const childMid = (childYs[0] + childYs[childYs.length - 1]) / 2
  const nodeY = Math.max(childMid, colBot[parentCol])

  const shift = nodeY - childMid
  if (shift > 0) {
    for (let i = childStartIdx; i < nodes.length; i++) {
      nodes[i].position.y += shift
    }
    childCols.forEach((c) => {
      colBot[c] += shift
    })
  }

  nodes.push({
    id,
    type,
    position: { x: COL_X[parentCol], y: nodeY },
    data: { label, side, depth, done, hasChildren: true, nodeType },
  })

  // 推进父列
  colBot[parentCol] = nodeY + ROW

  return nodeY
}

/**
 * 处理一个 section
 */
function placeSection(sec: SectionDef, prevSecId: string | null) {
  // 列序列：从 section 一侧的 1 列到 3 列
  const subCols: Col[] =
    sec.side === 'right' ? ['right1', 'right2', 'right3'] : ['left1', 'left2', 'left3']
  const branchCol: Col = subCols[0]

  // 新 section 的 sub-path 与上一条 sub-path 之间留出间隔
  subCols.forEach((c) => {
    if (colBot[c] > 0) colBot[c] += PATH_GAP
  })

  const sHandle = sec.side === 'right' ? 'right' : 'left'
  const tInHandle = sec.side === 'right' ? 'left' : 'right'
  const tOutHandle = sec.side === 'right' ? 'left-out' : 'right-out'
  const sInHandle = sec.side === 'right' ? 'right-in' : 'left-in'

  // ── 1. 先放 topic 和它们的 children，记录每个 topic 的 y ──
  const topicYs: number[] = []
  const placedFromIdx = nodes.length // 记录起点，便于事后整体平移
  sec.topics.forEach((topic) => {
    const y = placeNodeWithChildren(
      topic.id,
      topic.label,
      'topic',
      subCols,
      sec.side,
      topic.children,
      1,
      topic.done ?? false,
      topic.nodeType ?? 'course'
    )
    topicYs.push(y)
  })

  // ── 2. 计算 section y：必须不低于 center 列；topics 中心需与 section 对齐 ──
  let sectionY: number
  if (topicYs.length > 0) {
    const mid = (topicYs[0] + topicYs[topicYs.length - 1]) / 2
    if (mid >= colBot.center) {
      sectionY = mid
    } else {
      // topics 整体在主干上方：把所有刚放下的节点（含子孙）整体下移
      const shift = colBot.center - mid
      for (let i = placedFromIdx; i < nodes.length; i++) {
        nodes[i].position.y += shift
      }
      for (let i = 0; i < topicYs.length; i++) topicYs[i] += shift
      subCols.forEach((c) => {
        colBot[c] += shift
      })
      sectionY = colBot.center
    }
  } else {
    sectionY = colBot.center
  }
  const trunkMin = colBot.center + (prevSecId ? TRUNK_GAP : 0)
  if (sectionY < trunkMin) {
    const shift = trunkMin - sectionY
    for (let i = placedFromIdx; i < nodes.length; i++) {
      nodes[i].position.y += shift
    }
    subCols.forEach((c) => {
      colBot[c] += shift
    })
    sectionY = trunkMin
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
      source: prevSecId,
      target: sec.id,
      sourceHandle: 'bottom',
      targetHandle: 'top',
      type: 'straight',
      style: { stroke: '#666', strokeWidth: 3 },
      markerEnd: undefined,
    })
  }

  // ── 5. section 与 topic 的边、topic 之间的边、topic 与子节点的边 ──
  sec.topics.forEach((topic, ti) => {
    // section → 第一个 topic
    if (ti === 0) {
      edges.push({
        id: `branch-${sec.id}-${topic.id}`,
        source: sec.id,
        target: topic.id,
        sourceHandle: sHandle,
        targetHandle: tInHandle,
        type: 'default',
        style: { stroke: '#888', strokeWidth: 1.5, strokeDasharray: '8,4' },
        markerEnd: undefined,
      })
    } else {
      // topic → topic（垂直）
      edges.push({
        id: `chain-${topic.id}`,
        source: sec.topics[ti - 1].id,
        target: topic.id,
        sourceHandle: 'bottom',
        targetHandle: 'top',
        type: 'default',
        style: { stroke: '#888', strokeWidth: 1.5 },
        markerEnd: undefined,
      })
    }

    // 最后一个 topic → 回到 section
    if (ti === sec.topics.length - 1) {
      edges.push({
        id: `return-${topic.id}-${sec.id}`,
        source: topic.id,
        target: sec.id,
        sourceHandle: tOutHandle,
        targetHandle: sInHandle,
        type: 'default',
        style: { stroke: '#888', strokeWidth: 1.5, strokeDasharray: '8,4' },
        markerEnd: undefined,
      })
    }

    // topic 的子节点（递归）
    if (topic.children?.length) {
      addChildEdges(topic.id, topic.children, sHandle, tInHandle, tOutHandle)
    }
  })
}

/**
 * 递归生成 parent → children 之间的边
 */
function addChildEdges(
  parentId: string,
  children: ChildDef[],
  sHandle: string,
  tInHandle: string,
  tOutHandle: string
) {
  children.forEach((child, ci) => {
    if (ci === 0) {
      // 父 → 第一个子（虚线，水平进入）
      edges.push({
        id: `cb-${parentId}-${child.id}`,
        source: parentId,
        target: child.id,
        sourceHandle: sHandle,
        targetHandle: tInHandle,
        type: 'default',
        style: { stroke: '#888', strokeWidth: 1.5, strokeDasharray: '8,4' },
        markerEnd: undefined,
      })
    } else {
      // 同级链：上一个子 → 当前子（实线，垂直）
      edges.push({
        id: `cc-${child.id}`,
        source: children[ci - 1].id,
        target: child.id,
        sourceHandle: 'bottom',
        targetHandle: 'top',
        type: 'default',
        style: { stroke: '#888', strokeWidth: 1.5 },
        markerEnd: undefined,
      })
    }
    // 递归：当前子若还有子节点，继续生成
    if (child.children?.length) {
      addChildEdges(child.id, child.children, sHandle, tInHandle, tOutHandle)
    }
  })
  // 最后一个子 → 父（虚线返回）
  const lastChild = children[children.length - 1]
  edges.push({
    id: `cr-${lastChild.id}-${parentId}`,
    source: lastChild.id,
    target: parentId,
    sourceHandle: tOutHandle,
    targetHandle: sHandle,
    type: 'default',
    style: { stroke: '#888', strokeWidth: 1.5, strokeDasharray: '8,4' },
  })
}

// ─── 执行布局 ──────────────────────────────────────────────
sections.forEach((sec, si) => {
  placeSection(sec, si > 0 ? sections[si - 1].id : null)
})

// ─── 首尾虚线延伸 ──────────────────────────────────────────
const rootNode = nodes.find((n) => n.id === 'root')
const endNode = nodes.find((n) => n.id === 'end')
const EXTEND = 40 // 延伸长度

if (rootNode) {
  nodes.push({
    id: '__phantom_top',
    type: 'phantom',
    position: { x: COL_X.center + NODE_W / 2, y: rootNode.position.y - EXTEND },
    data: {},
  })
  edges.push({
    id: 'extend-top',
    source: '__phantom_top',
    target: 'root',
    sourceHandle: 'bottom',
    targetHandle: 'top',
    type: 'straight',
    style: { stroke: '#666', strokeWidth: 3, strokeDasharray: '8,5' },
  })
}

if (endNode) {
  nodes.push({
    id: '__phantom_bottom',
    type: 'phantom',
    position: { x: COL_X.center + NODE_W / 2, y: endNode.position.y + NODE_H + EXTEND },
    data: {},
  })
  edges.push({
    id: 'extend-bottom',
    source: 'end',
    target: '__phantom_bottom',
    sourceHandle: 'bottom',
    targetHandle: 'top',
    type: 'straight',
    style: { stroke: '#666', strokeWidth: 3, strokeDasharray: '8,5' },
  })
}

// 根据节点计算图的实际尺寸（最大 x/y + 节点尺寸 + padding），乘以 zoom 保证容器够高
const ZOOM = 1.1
const PADDING = 40
const graphHeight = (Math.max(...nodes.map((n) => n.position.y)) + NODE_H + PADDING) * ZOOM
const graphWidth = Math.max(...nodes.map((n) => n.position.x)) + NODE_W + PADDING

// 抑制未使用变量警告
void COLS
</script>

<style scoped>
.roadmap-demo {
  min-width: 100%;
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
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  white-space: nowrap;
  width: 160px;
  text-align: center;
  position: relative;
}

/* 组合：白底黑边，与 section 保持一致 */
:deep(.node-topic--group) {
  background: #fff;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  font-weight: 600;
}

/* 课程：偏粉红 */
:deep(.node-topic--course) {
  background: #fee2e8;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  font-weight: 500;
}

/* 节点：偏橙粉 */
:deep(.node-topic--node) {
  background: #feeadf;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  font-weight: 400;
}

/* 已完成角标 */
:deep(.node-done-badge) {
  position: absolute;
  top: -6px;
  left: -6px;
  width: 16px;
  height: 16px;
  background: #16a34a;
  color: #fff;
  border-radius: 50%;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid #fff;
}

/* depth class 已废弃，清除旧样式 */
:deep(.node-topic--depth-1),
:deep(.node-topic--depth-2),
:deep(.node-topic--depth-3),
:deep(.node-topic--done) {
  all: unset;
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
