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
import { computed } from 'vue'

interface Props {
  size?: number
  color?: string
  strokeWidth?: number
}

const props = withDefaults(defineProps<Props>(), {
  size: 100,
  color: '#3aa876',
  strokeWidth: 1.5,
})

// 正二十面体的 12 个顶点
const phi = (1 + Math.sqrt(5)) / 2
const scale = 1 / Math.sqrt(1 + phi * phi)
const baseVertices = [
  [0, 1, phi],
  [0, -1, phi],
  [0, 1, -phi],
  [0, -1, -phi],
  [1, phi, 0],
  [-1, phi, 0],
  [1, -phi, 0],
  [-1, -phi, 0],
  [phi, 0, 1],
  [-phi, 0, 1],
  [phi, 0, -1],
  [-phi, 0, -1],
].map(([x, y, z]) => [x * scale, y * scale, z * scale])

// 30 条棱
const edges: [number, number][] = [
  [0, 1],
  [0, 4],
  [0, 5],
  [0, 8],
  [0, 9],
  [3, 2],
  [3, 6],
  [3, 7],
  [3, 10],
  [3, 11],
  [1, 8],
  [1, 9],
  [1, 6],
  [1, 7],
  [4, 5],
  [4, 8],
  [4, 10],
  [5, 9],
  [5, 11],
  [8, 10],
  [8, 6],
  [9, 11],
  [9, 7],
  [6, 10],
  [6, 7],
  [7, 11],
  [2, 4],
  [2, 5],
  [2, 10],
  [2, 11],
]

// 20 个面
const faces: [number, number, number][] = [
  [0, 8, 1],
  [0, 4, 8],
  [0, 5, 4],
  [0, 9, 5],
  [0, 1, 9],
  [1, 8, 6],
  [8, 4, 10],
  [4, 5, 2],
  [5, 9, 11],
  [9, 1, 7],
  [6, 8, 10],
  [10, 4, 2],
  [2, 5, 11],
  [11, 9, 7],
  [7, 1, 6],
  [3, 10, 6],
  [3, 2, 10],
  [3, 11, 2],
  [3, 7, 11],
  [3, 6, 7],
]

// 每条棱属于哪两个面
const edgeToFaces = new Map<string, number[]>()
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

// 固定角度
const angleX = 0.7
const angleY = 0.6
const angleZ = 0

function rotatePoint(point: number[]): number[] {
  const [x, y, z] = point

  const cosX = Math.cos(angleX)
  const sinX = Math.sin(angleX)
  const y1 = y * cosX - z * sinX
  const z1 = y * sinX + z * cosX

  const cosY = Math.cos(angleY)
  const sinY = Math.sin(angleY)
  const x2 = x * cosY + z1 * sinY
  const z2 = -x * sinY + z1 * cosY

  const cosZ = Math.cos(angleZ)
  const sinZ = Math.sin(angleZ)
  const x3 = x2 * cosZ - y1 * sinZ
  const y3 = x2 * sinZ + y1 * cosZ

  return [x3, y3, z2]
}

// 旋转后的 3D 顶点
const rotatedVertices = computed(() => {
  return baseVertices.map((v) => rotatePoint(v))
})

// 投影到 2D
const projectedVertices = computed(() => {
  const s = props.size * 0.4
  const center = props.size / 2
  return rotatedVertices.value.map((v) => ({
    x: center + v[0] * s,
    y: center - v[1] * s,
  }))
})

// 计算面的法向量 z 分量
function faceNormalZ(faceIdx: number): number {
  const [i, j, k] = faces[faceIdx]
  const v0 = rotatedVertices.value[i]
  const v1 = rotatedVertices.value[j]
  const v2 = rotatedVertices.value[k]

  const cx = (v0[0] + v1[0] + v2[0]) / 3
  const cy = (v0[1] + v1[1] + v2[1]) / 3
  const cz = (v0[2] + v1[2] + v2[2]) / 3

  const ax = v1[0] - v0[0]
  const ay = v1[1] - v0[1]
  const az = v1[2] - v0[2]
  const bx = v2[0] - v0[0]
  const by = v2[1] - v0[1]
  const bz = v2[2] - v0[2]

  const nx = ay * bz - az * by
  const ny = az * bx - ax * bz
  const nz = ax * by - ay * bx

  const dot = nx * cx + ny * cy + nz * cz
  return dot > 0 ? nz : -nz
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
</script>
