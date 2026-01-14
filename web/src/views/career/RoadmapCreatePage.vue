<template>
  <DefaultLayout>
    <div class="roadmap-create-page">
      <!-- 页面标题 -->
      <div class="mb-6 mb-md-8">
        <div class="d-flex align-center title-row">
          <!-- 返回按钮 -->
          <v-btn
            icon="mdi-arrow-left"
            variant="flat"
            color="grey-lighten-5"
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            class="back-button mr-3 mr-md-4 flex-shrink-0"
            @click="handleBack"
          ></v-btn>

          <!-- 图标和标题 -->
          <div class="d-flex align-center" style="min-width: 0">
            <v-avatar
              color="primary"
              :size="$vuetify.display.mobile ? 40 : 48"
              class="mr-3 flex-shrink-0"
            >
              <v-icon
                icon="mdi-briefcase-outline"
                color="white"
                :size="$vuetify.display.mobile ? 20 : 24"
              />
            </v-avatar>
            <div style="min-width: 0">
              <h1 class="text-h6 text-md-h5 font-weight-bold text-grey-darken-4 text-truncate">
                {{ copyId ? '复制学习路径' : '创建学习路径' }}
              </h1>
              <p class="text-caption text-sm-body-2 text-grey-darken-2 text-truncate">
                为 {{ careerName }} 创建新的学习路径
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <LoadingSpinner v-if="loading" />

      <div v-else class="content-layout">
        <!-- 左侧：流程图编辑器 -->
        <div class="main-content">
          <v-card border rounded="xl" class="flow-editor-card">
            <v-card-title
              class="pa-3 pa-sm-4 d-flex flex-row align-center justify-space-between ga-2 ga-sm-3"
            >
              <div class="d-flex align-center">
                <span class="text-h6 font-weight-bold me-4">路径编辑器</span>
                <v-chip v-if="nodes.length > 1" size="small" color="primary" variant="tonal">
                  {{ nodes.length - 1 }} 个节点
                </v-chip>
              </div>
              <div class="d-flex flex-wrap align-center gap-2">
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="error"
                  rounded="lg"
                  @click="deleteSelectedNodes"
                >
                  <v-icon
                    icon="mdi-delete"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1"
                  />
                  <span class="d-none d-sm-inline">删除选中</span>
                </v-btn>
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="warning"
                  rounded="lg"
                  @click="resetAll"
                >
                  <v-icon
                    icon="mdi-refresh"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1"
                  />
                  <span class="d-none d-sm-inline">重置画布</span>
                </v-btn>
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="info"
                  rounded="lg"
                  @click="applyAutoLayout"
                >
                  <v-icon
                    icon="mdi-auto-fix"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1"
                  />
                  <span class="d-none d-sm-inline">自动布局</span>
                </v-btn>
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="primary"
                  rounded="lg"
                  @click="showSave"
                >
                  <v-icon
                    icon="mdi-content-save"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1"
                  />
                  <span class="d-none d-sm-inline">保存</span>
                </v-btn>
              </div>
            </v-card-title>
            <v-card-text class="pa-0">
              <div class="flow-editor">
                <VueFlow
                  :nodes="nodes"
                  :edges="edges"
                  :min-zoom="0.7"
                  :max-zoom="1.1"
                  :zoom-on-scroll="false"
                  fit-view-on-init
                  :snap-to-grid="true"
                  :snap-grid="[20, 20]"
                  :nodes-selectable="true"
                  :edges-selectable="true"
                  :selection-mode="'partial'"
                  :multi-selection-key-code="'Shift'"
                  @connect="onConnect"
                  @nodes-change="onNodesChange"
                  @edges-change="onEdgesChange"
                >
                  <Background variant="dots" pattern-color="#bdbdbd" :gap="30" :size="2" />
                  <MiniMap v-if="$vuetify.display.mdAndUp" />
                  <Controls :show-interactive="false" />
                </VueFlow>
              </div>
            </v-card-text>
          </v-card>
        </div>

        <!-- 右侧：工具面板 -->
        <div class="right-sidebar">
          <!-- 课程搜索区 -->
          <v-card rounded="xl" class="course-search-card sticky-card no-border" elevation="0">
            <v-card-text class="pa-4">
              <!-- 标题和统计 -->
              <div class="d-flex align-center justify-space-between mb-3">
                <div class="d-flex align-center">
                  <v-icon icon="mdi-book-multiple" color="primary" size="20" class="mr-2" />
                  <span class="text-subtitle-1 font-weight-bold text-grey-darken-4">添加课程</span>
                </div>
                <div class="d-flex align-center gap-2">
                  <a
                    href="/courses"
                    target="_blank"
                    class="text-body-2 text-primary text-decoration-none"
                    style="white-space: nowrap"
                  >
                    查看全部课程
                  </a>
                </div>
              </div>

              <!-- 搜索框 -->
              <v-text-field
                v-model="searchText"
                placeholder="搜索课程名称..."
                variant="outlined"
                density="comfortable"
                hide-details
                class="mb-3"
                rounded="lg"
                autocomplete="off"
                @keydown.enter="handleSearch"
                @click:clear="searchText = ''; availableCourses = []"
              >
                <template #append-inner>
                  <v-btn icon size="small" variant="text" @click="handleSearch">
                    <v-icon icon="mdi-magnify" size="20" />
                  </v-btn>
                </template>
              </v-text-field>

              <!-- 加载状态 -->
              <div v-if="coursesLoading" class="text-center py-8">
                <v-progress-circular indeterminate color="primary" size="40" width="3" />
                <p class="text-body-2 text-grey-darken-1 mt-3">搜索中...</p>
              </div>

              <!-- 空状态 -->
              <div v-else-if="!searchText.trim()" class="empty-state text-center py-8">
                <div class="empty-icon-wrapper mb-3">
                  <v-icon icon="mdi-magnify" size="56" color="grey-lighten-1" />
                </div>
                <p class="text-body-2 text-grey-darken-1 mb-1">开始搜索课程</p>
                <p class="text-caption text-grey">输入课程名称进行搜索</p>
              </div>

              <!-- 课程列表 -->
              <div v-else class="course-list-wrapper">
                <div v-if="filteredCourses.length > 0" class="course-list">
                  <div
                    v-for="course in filteredCourses"
                    :key="course.id"
                    class="course-item"
                  >
                    <v-tooltip location="left" max-width="300">
                      <template #activator="{ props }">
                        <div class="course-name" v-bind="props" @click="goToCourseDetail(course.id)">
                          <v-icon icon="mdi-book-outline" size="16" class="mr-1" />
                          {{ course.name }}
                        </div>
                      </template>
                      <div class="tooltip-content">
                        <div class="text-subtitle-2 mb-1">{{ course.name }}</div>
                        <div class="text-caption text-grey-lighten-1 mb-2">
                          {{ categoryStore.getCourseFullCategoryText(course.mainCategory, course.subCategory) }}
                        </div>
                        <div class="text-caption">{{ course.description || '暂无简介' }}</div>
                      </div>
                    </v-tooltip>
                    <v-btn
                      icon
                      size="x-small"
                      color="primary"
                      variant="flat"
                      @click.stop="addCourseNode(course)"
                    >
                      <v-icon size="14">mdi-plus</v-icon>
                    </v-btn>
                  </div>
                </div>
                <div v-else class="text-center py-6">
                  <v-icon icon="mdi-book-off-outline" size="48" color="grey-lighten-1" class="mb-2" />
                  <p class="text-body-2 text-grey">未找到相关课程</p>
                </div>
              </div>

              <!-- 操作指南 -->
              <div class="tips-section">
                <div class="tips-header">
                  <v-icon icon="mdi-information-outline" size="16" class="mr-1" color="grey-darken-1" />
                  <span class="text-caption text-grey-darken-1">操作指南</span>
                </div>
                <div class="tips-list-simple">
                  <div class="tip-simple">点击课程添加到画布</div>
                  <div class="tip-simple">拖动节点调整位置</div>
                  <div class="tip-simple">连接节点设计路径</div>
                  <div class="tip-simple">按住 Shift 可多选节点</div>
                  <div class="tip-simple">选中后可删除节点</div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </div>
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
            :rules="roadmapDescriptionRules"
            :counter="roadmapDescriptionMaxLength"
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

    <!-- 统一确认对话框 -->
    <ConfirmDialog
      v-model="confirmDialogVisible"
      :title="confirmDialogConfig.title"
      :message="confirmDialogConfig.message"
      :confirm-text="confirmDialogConfig.confirmText"
      :cancel-text="confirmDialogConfig.cancelText"
      :confirm-color="confirmDialogConfig.confirmColor"
      :icon="confirmDialogConfig.icon"
      :icon-color="confirmDialogConfig.iconColor"
      :icon-foreground="confirmDialogConfig.iconForeground"
      @confirm="confirmDialogConfig.onConfirm"
    />
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, inject, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { MiniMap } from '@vue-flow/minimap'
import { Controls } from '@vue-flow/controls'
import { Position } from '@vue-flow/core'
import type { Node, Edge, Connection } from '@vue-flow/core'
import dagre from 'dagre'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { useCategoryStore } from '@/stores'
import { courseApi } from '@/api/modules/course'
import type { Course } from '@/types/course'

const router = useRouter()
const route = useRoute()
const categoryStore = useCategoryStore()

// 注入全局 snackbar
const showSnackbar = inject('showSnackbar') as (message: string, type?: string) => void

// 获取 VueFlow 实例
const { fitView, setCenter } = useVueFlow()

// 验证规则
const roadmapDescriptionRules = useValidationRules('roadmap-description')
const roadmapDescriptionMaxLength = useMaxLength('roadmap-description')

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

// 确认对话框状态
const confirmDialogVisible = ref(false)
const confirmDialogConfig = ref({
  title: '',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
  confirmColor: 'error',
  icon: 'mdi-alert-circle-outline',
  iconColor: 'error-lighten-4',
  iconForeground: 'error',
  onConfirm: () => {},
})

// 可用课程列表
const availableCourses = ref<Course[]>([])
const coursesLoading = ref(false)
const searchText = ref('')

// 搜索课程
const searchCourses = async () => {
  if (!searchText.value.trim()) {
    availableCourses.value = []
    return
  }

  coursesLoading.value = true
  try {
    const response = await courseApi.searchCourses(searchText.value.trim())
    if (response.code === 200) {
      availableCourses.value = response.data
    } else {
      showSnackbar('搜索课程失败', 'error')
    }
  } catch (error) {
    console.error('搜索课程失败:', error)
    showSnackbar('搜索课程失败', 'error')
  } finally {
    coursesLoading.value = false
  }
}

// 手动搜索方法（点击搜索按钮或按下回车触发）
const handleSearch = () => {
  searchCourses()
}

// 监听搜索文本变化，清空旧的搜索结果
watch(searchText, () => {
  availableCourses.value = []
})

const filteredCourses = computed(() => availableCourses.value)

// 节点和边
const nodes = ref<Node[]>([
  {
    id: '0',
    type: 'default',
    data: { label: careerName.value },
    position: { x: 400, y: 100 },
    targetPosition: Position.Bottom, // 入口在底部，只能被其他节点指向
    // 没有 sourcePosition，表示不能作为连接起点
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

// 添加课程节点
const addCourseNode = (course: Course) => {
  // 检查是否已存在
  if (nodes.value.find((n) => n.id === course.id.toString())) {
    showSnackbar('该课程已添加', 'warning')
    return
  }

  // 计算位置
  let x: number
  let y: number

  if (nodes.value.length === 1) {
    // 第一个课程节点：放在根节点下方居中
    const rootNode = nodes.value[0]
    x = rootNode.position.x
    y = rootNode.position.y + 100
  } else {
    // 找到 y 坐标最大的节点（最下面的节点）
    const bottomNode = nodes.value.reduce((lowest, node) => {
      return node.position.y > lowest.position.y ? node : lowest
    })

    // 计算所有节点的 x 坐标中心位置
    const sumX = nodes.value.reduce((sum, node) => sum + node.position.x, 0)
    const centerX = sumX / nodes.value.length

    // 新节点位置：x 为所有节点中心，y 在最下面节点下方 60px
    x = centerX
    y = bottomNode.position.y + 60
  }

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

  // 聚焦到新节点（使用 nextTick 确保 DOM 更新后再聚焦）
  setTimeout(() => {
    setCenter(x, y, { zoom: 1, duration: 300 })
  }, 50)
}

/**
 * 跳转到课程详情页
 */
const goToCourseDetail = (courseId: number) => {
  window.open(`/courses/${courseId}`, '_blank')
}

// 删除选中的节点和边
const deleteSelectedNodes = () => {
  const selectedNodes = nodes.value.filter((n) => n.selected && n.id !== '0')
  const selectedEdges = edges.value.filter((e) => e.selected)

  const totalSelected = selectedNodes.length + selectedEdges.length

  if (totalSelected === 0) {
    showSnackbar('请先选中要删除的节点或连接线 (根节点不能删除)', 'warning')
    return
  }

  const itemsText = []
  if (selectedNodes.length > 0) itemsText.push(`${selectedNodes.length} 个节点`)
  if (selectedEdges.length > 0) itemsText.push(`${selectedEdges.length} 条连接线`)

  confirmDialogConfig.value = {
    title: '删除确认',
    message: `确定要删除 ${itemsText.join('和')} 吗？此操作不可撤销。`,
    confirmText: '删除',
    cancelText: '取消',
    confirmColor: 'error',
    icon: 'mdi-delete-outline',
    iconColor: 'error-lighten-4',
    iconForeground: 'error',
    onConfirm: () => {
      // 删除选中的节点
      const selectedNodeIds = new Set(selectedNodes.map((n) => n.id))
      nodes.value = nodes.value.filter((n) => !selectedNodeIds.has(n.id))

      // 删除选中的边，以及与被删除节点相关的边
      const selectedEdgeIds = new Set(selectedEdges.map((e) => e.id))
      edges.value = edges.value.filter(
        (e) => !selectedEdgeIds.has(e.id) && !selectedNodeIds.has(e.source) && !selectedNodeIds.has(e.target)
      )

      showSnackbar(`已删除 ${itemsText.join('和')}`, 'success')
    },
  }
  confirmDialogVisible.value = true
}

// 处理连接
const onConnect = (connection: Connection) => {
  // 不允许从根节点出发的连接（根节点只有入口，没有出口）
  if (connection.source === '0') return

  // 允许连接到根节点（根节点作为终点）
  // if (connection.target === '0') return  // 删除这行限制

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

// 处理节点变化（包括选中状态和位置）
const onNodesChange = (changes: any[]) => {
  changes.forEach((change) => {
    if (change.type === 'select') {
      const node = nodes.value.find((n) => n.id === change.id)
      if (node) {
        node.selected = change.selected
      }
    } else if (change.type === 'position') {
      // 拖动过程中和拖动结束时都更新位置
      const node = nodes.value.find((n) => n.id === change.id)
      if (node && change.position) {
        node.position = change.position
      }
    }
  })
}

// 处理边变化（包括选中状态）
const onEdgesChange = (changes: any[]) => {
  changes.forEach((change) => {
    if (change.type === 'select') {
      const edge = edges.value.find((e) => e.id === change.id)
      if (edge) {
        edge.selected = change.selected
      }
    }
  })
}

// 返回上一页
const handleBack = () => {
  if (nodes.value.length > 1 || edges.value.length > 0) {
    confirmDialogConfig.value = {
      title: '确认离开',
      message: '有未保存的更改，确定要离开吗？',
      confirmText: '离开',
      cancelText: '取消',
      confirmColor: 'warning',
      icon: 'mdi-alert-outline',
      iconColor: 'warning-lighten-4',
      iconForeground: 'warning',
      onConfirm: () => {
        router.back()
      },
    }
    confirmDialogVisible.value = true
  } else {
    router.back()
  }
}

// 显示保存对话框
const showSave = () => {
  if (nodes.value.length <= 1) {
    showSnackbar('请至少添加一个学习节点', 'warning')
    return
  }
  showSaveDialog.value = true
}

// 保存路径
const saveRoadmap = async () => {
  if (!roadmapDescription.value.trim()) {
    showSnackbar('请输入路径描述', 'warning')
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
    showSnackbar('路径保存成功', 'success')
    router.back()
  }, 1000)
}

// 自动布局
const applyAutoLayout = () => {
  if (nodes.value.length <= 1) {
    showSnackbar('请至少添加一个节点才能使用自动布局', 'warning')
    return
  }

  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({
    rankdir: 'BT', // Bottom to Top - 根节点在上方
    nodesep: 150,
    ranksep: 80,
    marginx: 20,
    marginy: 20,
  })

  const nodeWidth = 120
  const nodeHeight = 40

  // 添加节点到 dagre 图
  nodes.value.forEach((node) => {
    dagreGraph.setNode(node.id.toString(), { width: nodeWidth, height: nodeHeight })
  })

  // 添加边到 dagre 图
  edges.value.forEach((edge) => {
    dagreGraph.setEdge(edge.source.toString(), edge.target.toString())
  })

  // 计算布局
  dagre.layout(dagreGraph)

  // 更新节点位置
  nodes.value = nodes.value.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id.toString())
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - nodeWidth / 2,
        y: nodeWithPosition.y - nodeHeight / 2,
      },
    }
  })

  // 布局完成后，调用 fitView 聚焦到所有节点
  setTimeout(() => {
    fitView({ padding: 0.2, duration: 300 })
  }, 50)

  showSnackbar('自动布局完成', 'success')
}

// 重置
const resetAll = () => {
  confirmDialogConfig.value = {
    title: '重置画布',
    message: '确定要重置吗？此操作会删除所有节点和路径，不可撤销。',
    confirmText: '重置',
    cancelText: '取消',
    confirmColor: 'warning',
    icon: 'mdi-refresh',
    iconColor: 'warning-lighten-4',
    iconForeground: 'warning',
    onConfirm: () => {
      nodes.value = [
        {
          id: '0',
          type: 'default',
          data: { label: careerName.value },
          position: { x: 400, y: 100 },
          targetPosition: Position.Bottom, // 入口在底部，只能被其他节点指向
          // 没有 sourcePosition，表示不能作为连接起点
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
      showSnackbar('已重置所有内容', 'success')
    },
  }
  confirmDialogVisible.value = true
}

// 如果是复制模式,加载数据
onMounted(() => {
  if (copyId.value) {
    loading.value = true
    setTimeout(() => {
      roadmapDescription.value = 'Vue 3 + TypeScript 全栈开发路线 (副本)'
      // 添加一些示例节点（如果需要）
      loading.value = false
    }, 500)
  }
})
</script>

<style scoped>
.roadmap-create-page {
  /* 使用 DefaultLayout 的默认 padding */
}

/* 宽屏时向左延伸，让后退按钮露出到页面外 */
@media (min-width: 1800px) {
  .title-row {
    margin-left: -56px;
  }
}

/* 内容布局 */
.content-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (min-width: 1280px) {
  .content-layout {
    flex-direction: row;
    gap: 24px;
    height: calc(100vh - 56px - 40px - 80px);
  }
}

.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

@media (min-width: 1280px) {
  .flow-editor-card {
    display: flex;
    flex-direction: column;
    flex: 1;
  }

  .flow-editor-card .v-card-text {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
}

/* 右侧课程列表 */
.right-sidebar {
  width: 100%;
}

@media (min-width: 1280px) {
  .right-sidebar {
    width: 320px;
    flex-shrink: 0;
  }
}

.flow-editor-card {
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-outline));
}

/* 右侧面板卡片样式 */
.course-search-card {
  background-color: rgb(var(--v-theme-surface));
  transition: box-shadow 0.2s ease;
}

/* 搜索框样式 */
.search-field :deep(.v-field) {
  border-radius: 12px;
  background-color: transparent !important;
}

/* 空状态图标动画 */
.empty-icon-wrapper {
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-8px);
  }
}

/* 课程列表样式 */
.course-list-wrapper {
  max-height: 350px;
  overflow-y: auto;
}

@media (min-width: 1280px) {
  .course-list-wrapper {
    max-height: calc(100vh - 520px);
  }
}

.course-list-wrapper::-webkit-scrollbar {
  width: 4px;
}

.course-list-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

.course-list-wrapper::-webkit-scrollbar-thumb {
  background-color: rgba(var(--v-theme-on-surface), 0.1);
  border-radius: 2px;
}

.course-list-wrapper::-webkit-scrollbar-thumb:hover {
  background-color: rgba(var(--v-theme-on-surface), 0.2);
}

.course-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.course-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 12px 0;
  background: transparent;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  gap: 12px;
}

.course-item:hover {
  transform: translateY(-2px);
}

.course-name {
  flex: 1;
  display: flex;
  align-items: center;
  cursor: pointer;
  color: rgb(var(--v-theme-grey-darken-3));
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s ease;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-name:hover {
  color: rgb(var(--v-theme-primary));
}

.course-name .v-icon {
  flex-shrink: 0;
  color: rgb(var(--v-theme-grey-darken-1));
}

.course-item:hover .course-name .v-icon {
  color: rgb(var(--v-theme-primary));
}

/* 操作指南样式 */
.tips-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgb(var(--v-theme-grey-lighten-4));
}

.tips-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  opacity: 0.7;
}

.tips-list-simple {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tip-simple {
  font-size: 12px;
  line-height: 1.5;
  color: rgb(var(--v-theme-grey-darken-1));
  padding-left: 12px;
  position: relative;
}

.tip-simple::before {
  content: '·';
  position: absolute;
  left: 0;
  font-size: 16px;
  line-height: 1.2;
  color: rgb(var(--v-theme-grey));
}

.flow-editor {
  height: 500px;
  min-height: 400px;
  background: rgb(var(--v-theme-surface));
  position: relative;
}

@media (min-width: 1280px) {
  .flow-editor {
    height: 100%;
    min-height: 400px;
  }
}

.sticky-card {
  display: flex;
  flex-direction: column;
}

@media (min-width: 1280px) {
  .sticky-card {
    position: sticky;
    top: 75px;
    max-height: calc(100vh - 95px);
    overflow: hidden;
  }
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

/* Vue Flow 边（连接线）样式 */
:deep(.vue-flow__edge) {
  cursor: pointer;
}

:deep(.vue-flow__edge.selected) {
  z-index: 1000;
}

:deep(.vue-flow__edge.selected .vue-flow__edge-path) {
  stroke: rgb(var(--v-theme-primary)) !important;
  stroke-width: 3px !important;
}
</style>
