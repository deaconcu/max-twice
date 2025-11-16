<template>
  <DefaultLayout>
    <div class="roadmap-create-page">
      <!-- 返回按钮 -->
      <v-btn variant="text" color="grey-darken-2" class="mb-4" @click="handleBack">
        <v-icon icon="mdi-arrow-left" class="mr-1" />
        返回路径列表
      </v-btn>

      <!-- 页面标题 -->
      <div class="mb-6">
        <div class="d-flex align-center">
          <v-avatar color="primary" size="48" class="mr-3">
            <v-icon icon="mdi-briefcase-outline" color="white" size="24" />
          </v-avatar>
          <div>
            <h1 class="text-h5 font-weight-bold text-grey-darken-4">
              {{ copyId ? '复制学习路径' : '创建学习路径' }}
            </h1>
            <p class="text-body-2 text-grey-darken-2">为 {{ careerName }} 创建新的学习路径</p>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <v-row v-if="loading">
        <v-col cols="12" class="text-center py-12">
          <v-progress-circular indeterminate color="primary" size="64" />
          <p class="text-body-2 text-grey mt-4">加载中...</p>
        </v-col>
      </v-row>

      <v-row v-else class="ma-0">
        <!-- 左侧：流程图编辑器 -->
        <v-col cols="12" lg="9" class="pa-0">
          <v-card border rounded="xl" class="flow-editor-card">
            <v-card-title class="pa-4 d-flex align-center justify-space-between">
              <span class="text-body-1">路径编辑器</span>
              <div class="d-flex align-center gap-2">
                <v-btn size="small" variant="outlined" color="error" @click="deleteSelectedNodes">
                  <v-icon icon="mdi-delete" size="18" class="mr-1" />
                  删除节点
                </v-btn>
                <v-btn size="small" variant="outlined" color="warning" @click="resetAll">
                  <v-icon icon="mdi-refresh" size="18" class="mr-1" />
                  重置
                </v-btn>
                <v-btn size="small" variant="flat" color="primary" @click="showSave">
                  <v-icon icon="mdi-content-save" size="18" class="mr-1" />
                  保存
                </v-btn>
              </div>
            </v-card-title>
            <v-card-text class="pa-0">
              <div class="flow-editor">
                <VueFlow
                  :nodes="nodes"
                  :edges="edges"
                  :min-zoom="0.9"
                  :max-zoom="1.1"
                  fit-view-on-init
                  :snap-to-grid="true"
                  :snap-grid="[20, 20]"
                  @connect="onConnect"
                >
                  <Background variant="dots" pattern-color="#bdbdbd" :gap="30" :size="2" />
                  <MiniMap />
                </VueFlow>
              </div>
            </v-card-text>
          </v-card>

          <!-- 提示信息 -->
          <v-card border rounded="xl" class="tips-card mt-4">
            <v-card-text class="pa-4">
              <div class="d-flex align-center">
                <v-icon icon="mdi-information" color="info" class="mr-2" />
                <div class="text-body-2 text-grey-darken-2">
                  <strong>操作提示:</strong>
                  从右侧选择课程添加到画布 → 拖动节点调整位置 → 连接节点创建学习路径 →
                  点击节点可选中删除
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 右侧：课程列表 -->
        <v-col cols="12" lg="3" class="pa-0 pl-6">
          <v-card border rounded="xl" class="course-list-card sticky-card">
            <v-card-title class="pa-4 pb-3">
              <div class="d-flex align-center">
                <v-icon icon="mdi-book-multiple" color="primary" class="mr-2" />
                <span class="text-h6 font-weight-bold">可用课程</span>
              </div>
            </v-card-title>
            <v-card-text class="pa-4 pt-0">
              <v-text-field
                v-model="searchText"
                placeholder="搜索课程..."
                variant="outlined"
                density="compact"
                hide-details
                clearable
                class="mb-3"
              >
                <template #prepend-inner>
                  <v-icon icon="mdi-magnify" size="18" />
                </template>
              </v-text-field>

              <v-btn block variant="outlined" color="primary" class="mb-4" @click="addCustomNode">
                <v-icon icon="mdi-plus" size="18" class="mr-1" />
                添加自定义节点
              </v-btn>

              <v-divider class="mb-3" />

              <div class="course-list">
                <v-chip
                  v-for="course in filteredCourses"
                  :key="course.id"
                  class="ma-1 course-chip"
                  variant="outlined"
                  @click="addCourseNode(course)"
                >
                  <v-icon icon="mdi-plus-circle" size="16" class="mr-1" />
                  {{ course.name }}
                </v-chip>
                <div v-if="filteredCourses.length === 0" class="text-center py-4">
                  <p class="text-body-2 text-grey">未找到匹配的课程</p>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>

    <!-- 保存对话框 -->
    <v-dialog v-model="showSaveDialog" max-width="600px" persistent>
      <v-card rounded="xl" border>
        <v-card-title class="pa-6">
          <div class="d-flex align-center">
            <v-icon icon="mdi-content-save" color="primary" size="32" class="mr-3" />
            <span class="text-h6 font-weight-bold">保存学习路径</span>
          </div>
        </v-card-title>
        <v-card-text class="px-6 pb-0">
          <v-text-field
            v-model="roadmapDescription"
            label="路径描述 *"
            placeholder="例如: Vue 3 + TypeScript 全栈开发路线"
            variant="outlined"
            clearable
            required
            hint="请输入简洁明了的路径描述"
            persistent-hint
          />
        </v-card-text>
        <v-card-actions class="px-6 pb-6 pt-4">
          <v-spacer />
          <v-btn variant="outlined" rounded="lg" :disabled="saving" @click="showSaveDialog = false">
            取消
          </v-btn>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            :disabled="!roadmapDescription.trim() || saving"
            :loading="saving"
            @click="saveRoadmap"
          >
            保存路径
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { MiniMap } from '@vue-flow/minimap'
import { Position } from '@vue-flow/core'
import type { Node, Edge, Connection } from '@vue-flow/core'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'

const router = useRouter()
const route = useRoute()

// 从路由获取参数
const careerId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : 0
})
const copyId = ref(route.query.copy ? Number(route.query.copy) : null)

// 状态管理
const loading = ref(false)
const saving = ref(false)
const showSaveDialog = ref(false)
const roadmapDescription = ref('')
const careerName = ref('前端工程师') // TODO: 从 API 获取

// Mock 数据 - 可用课程列表
const availableCourses = ref([
  { id: 1, name: 'HTML 基础' },
  { id: 2, name: 'CSS 基础' },
  { id: 3, name: 'JavaScript 基础' },
  { id: 4, name: 'Vue 3 基础' },
  { id: 5, name: 'TypeScript' },
  { id: 6, name: 'Pinia 状态管理' },
  { id: 7, name: 'Vue Router' },
  { id: 8, name: 'Vite 构建工具' },
  { id: 9, name: 'Node.js 基础' },
  { id: 10, name: 'Express 框架' },
  { id: 11, name: '数据库 MySQL' },
  { id: 12, name: '项目实战' },
])

const searchText = ref('')
const filteredCourses = computed(() => {
  if (!searchText.value.trim()) return availableCourses.value
  const lower = searchText.value.toLowerCase()
  return availableCourses.value.filter((c) => c.name.toLowerCase().includes(lower))
})

// 节点和边
const nodes = ref<Node[]>([
  {
    id: '0',
    type: 'default',
    data: { label: careerName.value },
    position: { x: 400, y: 100 },
    targetPosition: Position.Bottom,
    style: {
      background: '#616161',
      color: '#ffffff',
      border: '2px solid #9e9e9e',
      borderRadius: '12px',
      padding: '10px',
      fontWeight: '600',
      fontSize: '14px',
    },
  },
])

const edges = ref<Edge[]>([])

let nodeIdCounter = 1

// 添加课程节点
const addCourseNode = (course: { id: number; name: string }) => {
  // 检查是否已存在
  if (nodes.value.find((n) => n.id === course.id.toString())) {
    return
  }

  // 计算位置 (随机位置,用户可以拖动调整)
  const x = 200 + Math.random() * 400
  const y = 200 + Math.random() * 300

  nodes.value.push({
    id: course.id.toString(),
    type: 'default',
    data: { label: course.name },
    position: { x, y },
    sourcePosition: Position.Top,
    targetPosition: Position.Bottom,
    style: {
      background: '#fafafa',
      color: '#424242',
      border: '2px solid #bdbdbd',
      borderRadius: '12px',
      padding: '10px',
      fontWeight: '500',
      fontSize: '13px',
    },
  })
}

// 添加自定义节点
const addCustomNode = () => {
  const nodeName = prompt('请输入节点名称:')
  if (!nodeName?.trim()) return

  const newId = `custom-${nodeIdCounter++}`
  const x = 300 + Math.random() * 300
  const y = 250 + Math.random() * 200

  nodes.value.push({
    id: newId,
    type: 'default',
    data: { label: nodeName.trim() },
    position: { x, y },
    sourcePosition: Position.Top,
    targetPosition: Position.Bottom,
    style: {
      background: '#e3f2fd',
      color: '#1976d2',
      border: '2px solid #90caf9',
      borderRadius: '12px',
      padding: '10px',
      fontWeight: '500',
      fontSize: '13px',
    },
  })
}

// 删除选中的节点
const deleteSelectedNodes = () => {
  const selectedNodes = nodes.value.filter((n) => n.selected && n.id !== '0')
  if (selectedNodes.length === 0) {
    alert('请先选中要删除的节点 (根节点不能删除)')
    return
  }

  if (!confirm(`确定要删除 ${selectedNodes.length} 个节点吗?`)) return

  const selectedIds = new Set(selectedNodes.map((n) => n.id))
  nodes.value = nodes.value.filter((n) => !selectedIds.has(n.id))
  edges.value = edges.value.filter((e) => !selectedIds.has(e.source) && !selectedIds.has(e.target))
}

// 处理连接
const onConnect = (connection: Connection) => {
  // 不允许连接到根节点
  if (connection.target === '0') return

  // 检查是否已存在相同的连接
  const exists = edges.value.find(
    (e) => e.source === connection.source && e.target === connection.target
  )
  if (exists) return

  edges.value.push({
    id: `${connection.source}-${connection.target}`,
    source: connection.source ?? '',
    target: connection.target ?? '',
    type: 'default',
    animated: true,
    style: {
      stroke: '#78909c',
      strokeWidth: 2,
    },
  })
}

// 返回列表
const handleBack = () => {
  if (nodes.value.length > 1 || edges.value.length > 0) {
    if (!confirm('有未保存的更改,确定要离开吗?')) return
  }
  void router.push(`/career/${careerId.value}`)
}

// 显示保存对话框
const showSave = () => {
  if (nodes.value.length <= 1) {
    alert('请至少添加一个学习节点')
    return
  }
  showSaveDialog.value = true
}

// 保存路径
const saveRoadmap = async () => {
  if (!roadmapDescription.value.trim()) {
    alert('请输入路径描述')
    return
  }

  saving.value = true

  // 序列化数据
  const data = {
    description: roadmapDescription.value.trim(),
    nodes: nodes.value.map((n) => ({
      id: n.id,
      name: n.data.label,
      position: n.position,
    })),
    edges: edges.value.map((e) => ({
      source: e.source,
      target: e.target,
    })),
  }

  // 模拟保存
  setTimeout(() => {
    console.log('保存路径:', data)
    saving.value = false
    showSaveDialog.value = false
    void router.push(`/career/${careerId.value}`)
  }, 1000)
}

// 重置
const resetAll = () => {
  if (!confirm('确定要重置所有内容吗? 此操作不可撤销。')) return

  nodes.value = [
    {
      id: '0',
      type: 'default',
      data: { label: careerName.value },
      position: { x: 400, y: 100 },
      targetPosition: Position.Bottom,
      style: {
        background: '#616161',
        color: '#ffffff',
        border: '2px solid #9e9e9e',
        borderRadius: '12px',
        padding: '10px',
        fontWeight: '600',
        fontSize: '14px',
      },
    },
  ]
  edges.value = []
  roadmapDescription.value = ''
}

// 如果是复制模式,加载数据
onMounted(() => {
  if (copyId.value) {
    loading.value = true
    setTimeout(() => {
      roadmapDescription.value = 'Vue 3 + TypeScript 全栈开发路线 (副本)'
      // 添加一些示例节点
      addCourseNode({ id: 1, name: 'HTML 基础' })
      addCourseNode({ id: 2, name: 'CSS 基础' })
      addCourseNode({ id: 3, name: 'JavaScript 基础' })
      loading.value = false
    }, 500)
  }
})
</script>

<style scoped>
.roadmap-create-page {
  /* 使用 DefaultLayout 的默认 padding */
}

.flow-editor-card,
.course-list-card,
.tips-card {
  background-color: #ffffff;
  border: 1px solid #edeff1;
}

.flow-editor {
  height: calc(100vh - 380px);
  min-height: 400px;
  background: #ffffff;
  position: relative;
}

.sticky-card {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.course-list {
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

.course-list::-webkit-scrollbar {
  width: 4px;
}

.course-list::-webkit-scrollbar-track {
  background: transparent;
}

.course-list::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.course-chip {
  cursor: pointer;
  transition: all 0.2s ease;
}

.course-chip:hover {
  background-color: rgba(var(--v-theme-primary), 0.08);
  border-color: rgb(var(--v-theme-primary));
}

.gap-2 {
  gap: 8px;
}

/* Vue Flow 节点样式 */
:deep(.vue-flow__node) {
  cursor: move;
}

:deep(.vue-flow__node.selected) {
  box-shadow: 0 0 0 2px rgb(var(--v-theme-primary));
}

/* 移动端 */
@media (max-width: 1280px) {
  .sticky-card {
    position: static;
    max-height: none;
  }

  .flow-editor {
    height: 400px;
  }
}
</style>
