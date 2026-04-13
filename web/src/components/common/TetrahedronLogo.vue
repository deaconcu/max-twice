<template>
  <svg :viewBox="`0 0 ${size} ${size}`" :width="size" :height="size">
    <!-- 背面棱 - 浅色，先画 -->
    <line
      v-for="edge in backEdges"
      :key="`back-${edge.i}-${edge.j}`"
      :x1="edge.x1"
      :y1="edge.y1"
      :x2="edge.x2"
      :y2="edge.y2"
      :stroke="color"
      :stroke-width="strokeWidth * 0.75"
      opacity="0.25"
    />
    <!-- 正面棱 - 实线，后画（覆盖） -->
    <line
      v-for="edge in frontEdges"
      :key="`front-${edge.i}-${edge.j}`"
      :x1="edge.x1"
      :y1="edge.y1"
      :x2="edge.x2"
      :y2="edge.y2"
      :stroke="color"
      :stroke-width="strokeWidth"
    />
  </svg>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface Props {
  size?: number
  color?: string
  strokeWidth?: number
  speed?: number
}

const props = withDefaults(defineProps<Props>(), {
  size: 100,
  color: '#3aa876',
  strokeWidth: 2,
  speed: 120,
})

// 正四面体顶点（单位球内）
const baseVertices = [
  [0, 1, 0], // 0: 顶点
  [-0.943, -0.333, 0], // 1: 底面左
  [0.471, -0.333, -0.816], // 2: 底面右后
  [0.471, -0.333, 0.816], // 3: 底面右前
]

// 6条棱
const edges: [number, number][] = [
  [0, 1],
  [0, 2],
  [0, 3],
  [1, 2],
  [2, 3],
  [3, 1],
]

// 4个面（顶点索引，逆时针方向朝外）
const faces: [number, number, number][] = [
  [0, 1, 3], // 前面
  [0, 3, 2], // 右面
  [0, 2, 1], // 左后面
  [1, 2, 3], // 底面
]

// 每条棱属于哪两个面
const edgeToFaces: Map<string, number[]> = new Map()
edges.forEach(([i, j], edgeIdx) => {
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

// 随机旋转速度（每个轴不同速度）
const speedX = (Math.random() - 0.5) * 2 // -1 到 1
const speedY = (Math.random() - 0.5) * 2 + 1 // 0 到 2，确保有主旋转
const speedZ = (Math.random() - 0.5) * 2

let animationId: number | null = null
let lastTime = 0

function rotatePoint(point: number[], ax: number, ay: number, az: number): number[] {
  let [x, y, z] = point

  // 绕 X 轴旋转
  const cosX = Math.cos(ax)
  const sinX = Math.sin(ax)
  const y1 = y * cosX - z * sinX
  const z1 = y * sinX + z * cosX

  // 绕 Y 轴旋转
  const cosY = Math.cos(ay)
  const sinY = Math.sin(ay)
  const x2 = x * cosY + z1 * sinY
  const z2 = -x * sinY + z1 * cosY

  // 绕 Z 轴旋转
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
  const scale = props.size * 0.35
  const center = props.size / 2
  return rotatedVertices.value.map((v) => ({
    x: center + v[0] * scale,
    y: center - v[1] * scale,
  }))
})

// 计算面的法向量 z 分量（判断面是否朝向观察者）
function faceNormalZ(faceIdx: number): number {
  const [i, j, k] = faces[faceIdx]
  const v0 = rotatedVertices.value[i]
  const v1 = rotatedVertices.value[j]
  const v2 = rotatedVertices.value[k]

  // 两条边向量
  const ax = v1[0] - v0[0]
  const ay = v1[1] - v0[1]
  const az = v1[2] - v0[2]
  const bx = v2[0] - v0[0]
  const by = v2[1] - v0[1]
  const bz = v2[2] - v0[2]

  // 叉积的 z 分量
  return ax * by - ay * bx
}

// 判断一条棱是否是正面棱（至少有一个面朝向观察者）
function isEdgeFront(edgeIdx: number): boolean {
  const [i, j] = edges[edgeIdx]
  const key = `${Math.min(i, j)}-${Math.max(i, j)}`
  const belongFaces = edgeToFaces.get(key) || []

  // 如果这条棱所属的任一面朝向观察者（法向量 z < 0），则是正面棱
  return belongFaces.some((faceIdx) => faceNormalZ(faceIdx) < 0)
}

// 所有棱的 2D 坐标
const allEdges = computed(() => {
  return edges.map(([i, j], idx) => ({
    i,
    j,
    x1: projectedVertices.value[i].x,
    y1: projectedVertices.value[i].y,
    x2: projectedVertices.value[j].x,
    y2: projectedVertices.value[j].y,
    isFront: isEdgeFront(idx),
  }))
})

const frontEdges = computed(() => allEdges.value.filter((e) => e.isFront))
const backEdges = computed(() => allEdges.value.filter((e) => !e.isFront))

function animate(time: number) {
  if (lastTime) {
    const delta = (time - lastTime) / 1000
    const baseSpeed = ((props.speed * Math.PI) / 180) * delta
    angleX.value += baseSpeed * speedX
    angleY.value += baseSpeed * speedY
    angleZ.value += baseSpeed * speedZ
  }
  lastTime = time
  animationId = requestAnimationFrame(animate)
}

onMounted(() => {
  animationId = requestAnimationFrame(animate)
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>
