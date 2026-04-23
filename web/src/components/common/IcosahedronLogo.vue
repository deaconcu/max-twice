<template>
  <svg
    :viewBox="`0 0 ${size} ${size}`"
    :width="size"
    :height="size"
    :style="{ transform: `scale(${breathScale})` }"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
  >
    <!-- 背面棱 - 浅色，先画 -->
    <line
      v-for="edge in backEdges"
      :key="`back-edge-${edge.i}-${edge.j}`"
      :x1="edge.x1"
      :y1="edge.y1"
      :x2="edge.x2"
      :y2="edge.y2"
      :stroke="isFocusEdge(edge.i, edge.j) && focusIntensity > 0.05 ? focusColor : color"
      :stroke-width="strokeWidth * 0.75"
      :opacity="
        isFocusEdge(edge.i, edge.j) ? 0.25 + 0.15 * focusIntensity : 0.25
      "
    />
    <!-- 焦点面：红色三角形填充 + 脉冲（正面/背面都画，背面更淡） -->
    <polygon
      v-if="focusFaceTriangle"
      :points="focusFaceTriangle"
      :fill="focusColor"
      :opacity="focusFillOpacity"
    />
    <!-- 正面棱 - 实线，后画（覆盖） -->
    <line
      v-for="edge in frontEdges"
      :key="`front-edge-${edge.i}-${edge.j}`"
      :x1="edge.x1"
      :y1="edge.y1"
      :x2="edge.x2"
      :y2="edge.y2"
      :stroke="
        isFocusEdge(edge.i, edge.j) && focusIntensity > 0.05 ? focusColor : color
      "
      :stroke-width="
        isFocusEdge(edge.i, edge.j)
          ? strokeWidth * (1 + 0.4 * focusIntensity)
          : strokeWidth
      "
    />
  </svg>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface Props {
  size?: number
  color?: string
  focusColor?: string
  strokeWidth?: number
  speed?: number
}

const props = withDefaults(defineProps<Props>(), {
  size: 100,
  color: '#3aa876',
  focusColor: '#e53935',
  strokeWidth: 1.5,
  speed: 120,
})

// 顶点半径随 size 缩放
const vertexRadius = computed(() => Math.max(1.6, props.size * 0.028))

// 正二十面体的 12 个顶点
// 使用黄金比例 φ = (1 + √5) / 2
const phi = (1 + Math.sqrt(5)) / 2

// 顶点坐标（归一化到单位球）
const scale = 1 / Math.sqrt(1 + phi * phi)
const baseVertices = [
  // 3 个互相垂直的黄金矩形
  // XY 平面
  [0, 1, phi],
  [0, -1, phi],
  [0, 1, -phi],
  [0, -1, -phi],
  // XZ 平面
  [1, phi, 0],
  [-1, phi, 0],
  [1, -phi, 0],
  [-1, -phi, 0],
  // YZ 平面
  [phi, 0, 1],
  [-phi, 0, 1],
  [phi, 0, -1],
  [-phi, 0, -1],
].map(([x, y, z]) => [x * scale, y * scale, z * scale])

// 30 条棱（连接距离为 2 的顶点对）
const edges: [number, number][] = [
  // 顶部五边形周围
  [0, 1], [0, 4], [0, 5], [0, 8], [0, 9],
  // 底部五边形周围
  [3, 2], [3, 6], [3, 7], [3, 10], [3, 11],
  // 上层连接
  [1, 8], [1, 9], [1, 6], [1, 7],
  [4, 5], [4, 8], [4, 10],
  [5, 9], [5, 11],
  // 中层连接
  [8, 10], [8, 6],
  [9, 11], [9, 7],
  [6, 10], [6, 7],
  [7, 11],
  // 下层连接
  [2, 4], [2, 5], [2, 10], [2, 11],
]

// 20 个面（每个面是正三角形，顶点顺序确保从外部看是逆时针）
const faces: [number, number, number][] = [
  // 顶部 5 个面（围绕顶点 0）
  [0, 8, 1],
  [0, 4, 8],
  [0, 5, 4],
  [0, 9, 5],
  [0, 1, 9],
  // 上中层 5 个面
  [1, 8, 6],
  [8, 4, 10],
  [4, 5, 2],
  [5, 9, 11],
  [9, 1, 7],
  // 下中层 5 个面
  [6, 8, 10],
  [10, 4, 2],
  [2, 5, 11],
  [11, 9, 7],
  [7, 1, 6],
  // 底部 5 个面（围绕顶点 3）
  [3, 10, 6],
  [3, 2, 10],
  [3, 11, 2],
  [3, 7, 11],
  [3, 6, 7],
]

// 每条棱属于哪两个面
const edgeToFaces: Map<string, number[]> = new Map()
edges.forEach(([i, j]) => {
  const key = `${Math.min(i, j)}-${Math.max(i, j)}`
  const belongFaces: number[] = []
  faces.forEach((face, faceIdx) => {
    if (face.includes(i) && face.includes(j)) {
      belongFaces.push(faceIdx)
    }
  })
  edgeToFaces.set(key, belongFaces)
})

const angleX = ref(0.5)
const angleY = ref(0)
const angleZ = ref(0)

// hover 状态
const isHovered = ref(false)

// 呼吸效果（弱化：幅度更小，周期更慢）
const breathScale = ref(1)
const breathDirection = ref(1)

// 焦点面：每个周期淡入淡出，进入休息期时切换到下一面
const focusFace = ref<number>(-1)
const focusFillOpacity = ref(0)
const focusIntensity = ref(0) // 0 ~ 1 包络（含淡入/保持/淡出/休息）
let focusCycleStart = 0 // 当前周期的起始时间（秒）

// 随机旋转速度（在 setup 内生成，确保多个实例独立）
const speedX = (Math.random() - 0.5) * 2
const speedY = (Math.random() - 0.5) * 2 + 1
const speedZ = (Math.random() - 0.5) * 2

let animationId: number | null = null
let lastTime = 0

function rotatePoint(point: number[], ax: number, ay: number, az: number): number[] {
  const [x, y, z] = point

  // 绕 X 轴
  const cosX = Math.cos(ax)
  const sinX = Math.sin(ax)
  const y1 = y * cosX - z * sinX
  const z1 = y * sinX + z * cosX

  // 绕 Y 轴
  const cosY = Math.cos(ay)
  const sinY = Math.sin(ay)
  const x2 = x * cosY + z1 * sinY
  const z2 = -x * sinY + z1 * cosY

  // 绕 Z 轴
  const cosZ = Math.cos(az)
  const sinZ = Math.sin(az)
  const x3 = x2 * cosZ - y1 * sinZ
  const y3 = x2 * sinZ + y1 * cosZ

  return [x3, y3, z2]
}

// 旋转后的 3D 顶点
const rotatedVertices = computed(() => {
  return baseVertices.map((v) => rotatePoint(v, angleX.value, angleY.value, angleZ.value))
})

// 投影到 2D
const projectedVertices = computed(() => {
  const s = props.size * 0.4
  const center = props.size / 2
  return rotatedVertices.value.map((v) => ({
    x: center + v[0] * s,
    y: center - v[1] * s,
    z: v[2],
  }))
})

// 计算面的法向量 z 分量（自动修正朝向）
function faceNormalZ(faceIdx: number): number {
  const [i, j, k] = faces[faceIdx]
  const v0 = rotatedVertices.value[i]
  const v1 = rotatedVertices.value[j]
  const v2 = rotatedVertices.value[k]

  // 面的中心点
  const cx = (v0[0] + v1[0] + v2[0]) / 3
  const cy = (v0[1] + v1[1] + v2[1]) / 3
  const cz = (v0[2] + v1[2] + v2[2]) / 3

  // 两条边向量
  const ax = v1[0] - v0[0]
  const ay = v1[1] - v0[1]
  const az = v1[2] - v0[2]
  const bx = v2[0] - v0[0]
  const by = v2[1] - v0[1]
  const bz = v2[2] - v0[2]

  // 叉积得到法向量
  const nx = ay * bz - az * by
  const ny = az * bx - ax * bz
  const nz = ax * by - ay * bx

  // 法向量应该朝外（与中心点方向一致）
  // 如果法向量和中心点方向相反，则翻转
  const dot = nx * cx + ny * cy + nz * cz
  const correctedNz = dot > 0 ? nz : -nz

  return correctedNz
}

// 判断一条棱是否是正面棱
function isEdgeFront(i: number, j: number): boolean {
  const key = `${Math.min(i, j)}-${Math.max(i, j)}`
  const belongFaces = edgeToFaces.get(key) || []
  return belongFaces.some((faceIdx) => faceNormalZ(faceIdx) < 0)
}

// 所有棱的 2D 坐标
const allEdges = computed(() => {
  return edges.map(([i, j]) => ({
    i,
    j,
    x1: projectedVertices.value[i].x,
    y1: projectedVertices.value[i].y,
    x2: projectedVertices.value[j].x,
    y2: projectedVertices.value[j].y,
    isFront: isEdgeFront(i, j),
  }))
})

const frontEdges = computed(() => allEdges.value.filter((e) => e.isFront))
const backEdges = computed(() => allEdges.value.filter((e) => !e.isFront))

// 顶点按深度（z）拆为正面 / 背面
// 正面：z < 0（朝向观察者，因为投影时 y 轴翻转了，z 越小越靠前）
const frontVertices = computed(() =>
  projectedVertices.value
    .map((p, i) => ({ ...p, i }))
    .filter((p) => p.z <= 0)
)
const backVertices = computed(() =>
  projectedVertices.value
    .map((p, i) => ({ ...p, i }))
    .filter((p) => p.z > 0)
)

// 焦点面相关：当前焦点面的顶点 / 棱集合
const focusVertexSet = computed<Set<number>>(() => {
  if (focusFace.value < 0) return new Set()
  return new Set(faces[focusFace.value])
})

const focusEdgeKeySet = computed<Set<string>>(() => {
  const set = new Set<string>()
  if (focusFace.value < 0) return set
  const [a, b, c] = faces[focusFace.value]
  const pairs: [number, number][] = [
    [a, b],
    [b, c],
    [c, a],
  ]
  pairs.forEach(([i, j]) => {
    set.add(`${Math.min(i, j)}-${Math.max(i, j)}`)
  })
  return set
})

function isFocusVertex(i: number): boolean {
  return focusVertexSet.value.has(i)
}

function isFocusEdge(i: number, j: number): boolean {
  return focusEdgeKeySet.value.has(`${Math.min(i, j)}-${Math.max(i, j)}`)
}

// 焦点面三角形 SVG points 字符串；不论正面背面都返回（背面会用更淡的 opacity）
const focusFaceTriangle = computed<string | null>(() => {
  if (focusFace.value < 0) return null
  const [a, b, c] = faces[focusFace.value]
  const pa = projectedVertices.value[a]
  const pb = projectedVertices.value[b]
  const pc = projectedVertices.value[c]
  return `${pa.x},${pa.y} ${pb.x},${pb.y} ${pc.x},${pc.y}`
})

// 焦点面是否当前朝向观察者（正面）
const isFocusFaceFront = computed(() => {
  if (focusFace.value < 0) return false
  return faceNormalZ(focusFace.value) < 0
})

// 当前是否处于"焦点高亮"状态（用于减慢旋转）
const isFocusing = computed(() => focusFace.value >= 0)

function pickFocusFace() {
  // 找出所有正面面
  const candidates: number[] = []
  for (let i = 0; i < faces.length; i++) {
    if (faceNormalZ(i) < 0) candidates.push(i)
  }
  if (candidates.length === 0) {
    focusFace.value = -1
    return
  }
  // 避免连续两次选到同一个
  let pick = candidates[Math.floor(Math.random() * candidates.length)]
  if (candidates.length > 1 && pick === focusFace.value) {
    pick = candidates[(candidates.indexOf(pick) + 1) % candidates.length]
  }
  focusFace.value = pick
}

function animate(time: number) {
  if (!isVisible) {
    lastTime = 0
    return
  }
  if (lastTime) {
    const delta = (time - lastTime) / 1000

    // 速度策略：
    // - hover：×0.2
    // - 非 focus（休息期 intensity=0）：×1.3 略快
    // - focus 高亮中：随 intensity 减速，最低 ×0.35
    let speedMultiplier: number
    if (isHovered.value) {
      speedMultiplier = 0.2
    } else {
      // intensity 0 → 1.3，intensity 1 → 0.35（线性插值）
      speedMultiplier = 1.3 - 0.95 * focusIntensity.value
    }

    const baseSpeed = ((props.speed * Math.PI) / 180) * delta * speedMultiplier
    angleX.value += baseSpeed * speedX
    angleY.value += baseSpeed * speedY
    angleZ.value += baseSpeed * speedZ

    // focus 时整体放大（替代恒定呼吸效果）
    // intensity 0 → scale 1.0，intensity 1 → scale 1.12
    breathScale.value = 1 + 0.12 * focusIntensity.value

    // 焦点面填充脉冲：节奏 = 淡入(0.4s) → 保持(1.0s) → 淡出(0.4s) → 休息(1.5s)
    // 总周期 3.3s；切面发生在淡出结束、休息开始的那一刻，避免亮着的时候被切走
    if (focusFace.value >= 0) {
      const cycle = 3.3
      const fadeIn = 0.4
      const hold = 1.0
      const fadeOut = 0.4
      const tNow = time / 1000
      let t = tNow - focusCycleStart

      // 周期结束 → 进入新周期，先切面再算包络
      if (t >= cycle) {
        focusCycleStart = tNow
        pickFocusFace()
        t = 0
      }

      let envelope: number
      if (t < fadeIn) {
        envelope = t / fadeIn
      } else if (t < fadeIn + hold) {
        envelope = 1
      } else if (t < fadeIn + hold + fadeOut) {
        envelope = 1 - (t - fadeIn - hold) / fadeOut
      } else {
        envelope = 0 // 休息
      }

      const base = 0.55 * envelope
      focusFillOpacity.value = isFocusFaceFront.value ? base : base * 0.4
      focusIntensity.value = envelope
    } else {
      focusFillOpacity.value = 0
      focusIntensity.value = 0
    }
  }
  lastTime = time
  animationId = requestAnimationFrame(animate)
}

let isVisible = true
function handleVisibilityChange() {
  isVisible = !document.hidden
  if (isVisible) {
    lastTime = 0
    animationId = requestAnimationFrame(animate)
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
  animationId = requestAnimationFrame(animate)
  // 启动焦点：第一次挑面，周期由 animate 内部驱动
  pickFocusFace()
  focusCycleStart = performance.now() / 1000
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>
