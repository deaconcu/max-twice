<template>
  <DefaultLayout>
    <div class="roadmap-create-page">
      <!-- 页面标题 -->
      <div class="mb-6 mb-md-8">
        <div class="d-flex align-center title-row">
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
                {{ isEditMode ? '编辑学习路径' : (copyId ? '复制学习路径' : '创建学习路径') }}
              </h1>
              <p class="text-caption text-sm-body-2 text-grey-darken-2 text-truncate">
                {{ isEditMode ? '修改' : '为' }} {{ roleName }} {{ isEditMode ? '的学习路径' : '创建新的学习路径' }}
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
                  @click="applyAutoLayout(true)"
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
                <v-divider vertical class="mx-2 toolbar-divider" />
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="grey-darken-1"
                  rounded="lg"
                  @click="goToMyRoadmaps"
                >
                  <v-icon
                    icon="mdi-format-list-bulleted"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1"
                  />
                  <span class="d-none d-sm-inline">我创建的路线图</span>
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
          <!-- 课程/节点搜索区 -->
          <v-card class="course-search-card sticky-card no-border" elevation="0">
            <v-card-text class="pa-0 ps-4">
              <!-- 草稿描述显示 -->
              <div v-if="savedDraftDescription" class="draft-description-section mb-4">
                <div class="d-flex align-start justify-space-between">
                  <div class="flex-1" style="min-width: 0">
                    <div class="text-caption text-grey-darken-1 mb-1">路径描述</div>
                    <div class="text-body-2 font-weight-medium text-grey-darken-3 draft-description-text">
                      {{ savedDraftDescription }}
                    </div>
                  </div>
                  <v-btn
                    icon
                    size="small"
                    variant="text"
                    @click="showSaveDialog = true"
                  >
                    <v-icon icon="mdi-file-document-edit-outline" color="grey-darken-1" size="20" />
                  </v-btn>
                </div>
              </div>

              <!-- 分隔线 -->
              <v-divider v-if="savedDraftDescription" class="mt-6 mb-6" />

              <!-- Tab 切换 -->
              <v-tabs v-model="searchTab" color="primary" density="compact" class="mb-3">
                <v-tab value="course">
                  <v-icon icon="mdi-book-multiple" size="18" class="mr-1" />
                  添加课程
                </v-tab>
                <v-tab value="node">
                  <v-icon icon="mdi-file-tree-outline" size="18" class="mr-1" />
                  添加节点
                </v-tab>
              </v-tabs>

              <!-- 课程搜索 Tab -->
              <div v-show="searchTab === 'course'">
                <!-- 标题和统计 -->
                <div class="d-flex align-center justify-space-between mb-3">
                  <span class="text-subtitle-2 font-weight-bold text-grey-darken-4">搜索课程</span>
                  <a
                    href="/courses"
                    target="_blank"
                    class="text-caption text-primary text-decoration-none"
                  >
                    查看全部
                  </a>
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
                      <v-tooltip location="left" max-width="300" content-class="rounded-lg">
                        <template #activator="{ props }">
                          <div class="course-name" v-bind="props" @click="goToCourseDetail(course.id)">
                            <v-icon icon="mdi-book-outline" size="16" class="mr-1" />
                            {{ course.name }}
                          </div>
                        </template>
                        <div class="tooltip-content pa-1">
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
                        :disabled="isNodeAdded(course.rootNodeId)"
                        @click.stop="addCourseNode(course)"
                      >
                        <v-icon size="14">{{ isNodeAdded(course.rootNodeId) ? 'mdi-check' : 'mdi-plus' }}</v-icon>
                      </v-btn>
                    </div>
                  </div>
                  <div v-else class="text-center py-6">
                    <v-icon icon="mdi-book-off-outline" size="48" color="grey-lighten-1" class="mb-2" />
                    <p class="text-body-2 text-grey">未找到相关课程</p>
                  </div>
                </div>
              </div>

              <!-- 节点搜索 Tab -->
              <div v-show="searchTab === 'node'">
                <!-- 标题 -->
                <div class="mb-3">
                  <span class="text-subtitle-2 font-weight-bold text-grey-darken-4">搜索节点</span>
                </div>

                <!-- 搜索框 -->
                <v-text-field
                  v-model="nodeSearchText"
                  placeholder="搜索节点名称..."
                  variant="outlined"
                  density="comfortable"
                  hide-details
                  class="mb-3"
                  rounded="lg"
                  autocomplete="off"
                  @keydown.enter="handleNodeSearch"
                  @click:clear="nodeSearchText = ''; availableNodes = []"
                >
                  <template #append-inner>
                    <v-btn icon size="small" variant="text" @click="handleNodeSearch">
                      <v-icon icon="mdi-magnify" size="20" />
                    </v-btn>
                  </template>
                </v-text-field>

                <!-- 加载状态 -->
                <div v-if="nodesLoading" class="text-center py-8">
                  <v-progress-circular indeterminate color="success" size="40" width="3" />
                  <p class="text-body-2 text-grey-darken-1 mt-3">搜索中...</p>
                </div>

                <!-- 空状态 -->
                <div v-else-if="!nodeSearchText.trim()" class="empty-state text-center py-8">
                  <div class="empty-icon-wrapper mb-3">
                    <v-icon icon="mdi-magnify" size="56" color="grey-lighten-1" />
                  </div>
                  <p class="text-body-2 text-grey-darken-1 mb-1">开始搜索节点</p>
                  <p class="text-caption text-grey">输入节点名称进行搜索</p>
                </div>

                <!-- 节点列表 -->
                <div v-else class="course-list-wrapper">
                  <div v-if="filteredNodes.length > 0" class="course-list">
                    <div
                      v-for="node in filteredNodes"
                      :key="node.id"
                      class="course-item"
                    >
                      <v-tooltip location="left" max-width="300" content-class="rounded-lg">
                        <template #activator="{ props }">
                          <div class="node-content" v-bind="props">
                            <div class="course-name" @click="goToNodeDetail(node.id)">
                              <v-icon icon="mdi-file-document-outline" size="16" class="mr-1" color="success" />
                              {{ node.name }}
                            </div>
                            <div v-if="node.courseName" class="text-caption text-grey ps-6">
                              来自《{{ node.courseName }}》
                            </div>
                          </div>
                        </template>
                        <div class="tooltip-content pa-1">
                          <div class="text-subtitle-2 mb-1">{{ node.name }}</div>
                          <div v-if="node.courseName" class="text-caption text-success mb-2">
                            来自《{{ node.courseName }}》
                          </div>
                          <div class="text-caption">{{ node.description || '暂无描述' }}</div>
                        </div>
                      </v-tooltip>
                      <v-btn
                        icon
                        size="x-small"
                        color="success"
                        variant="flat"
                        :disabled="isNodeAdded(node.id)"
                        @click.stop="addNode(node)"
                      >
                        <v-icon size="14">{{ isNodeAdded(node.id) ? 'mdi-check' : 'mdi-plus' }}</v-icon>
                      </v-btn>
                    </div>
                  </div>
                  <div v-else class="text-center py-6">
                    <v-icon icon="mdi-file-document-off-outline" size="48" color="grey-lighten-1" class="mb-2" />
                    <p class="text-body-2 text-grey">未找到相关节点</p>
                  </div>
                </div>
              </div>

              <!-- 操作指南 -->
              <div class="tips-section">
                <div class="tips-header">
                  <v-icon icon="mdi-information-outline" size="16" class="mr-1" color="grey-darken-1" />
                  <span class="text-caption text-grey-darken-1">操作指南</span>
                </div>
                <div class="tips-list-simple">
                  <div class="tip-simple">点击课程/节点添加到画布</div>
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
    <v-dialog v-model="showSaveDialog" max-width="600px">
      <v-card rounded="xl" border>
        <!-- 关闭按钮 -->
        <v-btn
          icon="mdi-close"
          variant="text"
          size="small"
          class="dialog-close-btn"
          @click="showSaveDialog = false"
        />
        <v-card-title class="pa-6">
          <div class="d-flex align-center">
            <v-icon icon="mdi-content-save" color="primary" size="32" class="mr-3" />
            <span class="text-h6 font-weight-bold">保存学习路径</span>
          </div>
        </v-card-title>
        <v-card-text class="px-6 pb-0">
          <v-textarea
            v-model="roadmapDescription"
            label="路径描述 *"
            placeholder="例如: Vue 3 + TypeScript 全栈开发路线"
            :rules="roadmapDescriptionRules"
            :counter="roadmapDescriptionMaxLength"
            variant="outlined"
            clearable
            required
            rows="4"
            auto-grow
            hint="请输入简洁明了的路径描述"
            persistent-hint
          />

          <!-- 孤立节点提示 -->
          <v-alert
            v-if="hasIsolatedNodes"
            type="warning"
            variant="tonal"
            density="compact"
            class="mt-4"
          >
            检测到 {{ isolatedNodesCount }} 个未连接的节点。保存并发布时会自动删除这些节点。
          </v-alert>
        </v-card-text>
        <v-card-actions class="px-6 pb-6 pt-4">
          <v-spacer />
          <v-btn
            v-if="canSaveAsDraft"
            color="grey-darken-1"
            variant="flat"
            rounded="lg"
            :disabled="!roadmapDescription.trim() || saving"
            :loading="saving && saveType === 'draft'"
            @click="saveRoadmap('draft')"
          >
            保存为草稿
          </v-btn>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            :disabled="!roadmapDescription.trim() || saving"
            :loading="saving && saveType === 'publish'"
            @click="saveRoadmap('publish')"
          >
            保存并发布
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
import { useFetch } from '@/composables'
import { useCategoryStore } from '@/stores'
import { courseApi } from '@/api/modules/course'
import { roadmapApi } from '@/api/modules/roadmap'
import { searchApi } from '@/api/modules/search'
import type { Course } from '@/types/course'
import type { SearchResultItem } from '@/api/modules/search'

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
const roleId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : 0
})
const roadmapId = computed(() => {
  const id = route.params.roadmapId
  return id ? (typeof id === 'string' ? parseInt(id, 10) : Number(id)) : null
})
const isEditMode = computed(() => roadmapId.value !== null)
const copyId = ref(route.query.copy ? Number(route.query.copy) : null)

// 状态管理
const loading = ref(false)
const saving = ref(false)
const saveType = ref<'draft' | 'publish' | ''>('') // 保存类型
const showSaveDialog = ref(false)
const roadmapDescription = ref('')
const savedDraftDescription = ref('') // 已保存的草稿描述
const draftRoadmapId = ref<number | null>(null) // 草稿路线图ID
const roadmapState = ref<number | null>(null) // 路线图状态：0=草稿，1=审核中，2=已发布
const roleName = ref('前端工程师') // TODO: 从 API 获取

// Tab 切换状态
const searchTab = ref<'course' | 'node'>('course')
const nodeSearchText = ref('')

// 是否可以保存为草稿：只有草稿状态(0)或新建时可以保存为草稿，已发布(2)不能变回草稿
const canSaveAsDraft = computed(() => {
  return roadmapState.value === null || roadmapState.value === 0
})

// 计算孤立节点
const hasIsolatedNodes = computed(() => {
  if (nodes.value.length <= 1 || edges.value.length === 0) {
    return false
  }

  const connectedNodeIds = new Set<string>()
  edges.value.forEach((e) => {
    connectedNodeIds.add(e.source)
    connectedNodeIds.add(e.target)
  })

  return nodes.value.some((n) => !connectedNodeIds.has(n.id))
})

const isolatedNodesCount = computed(() => {
  if (!hasIsolatedNodes.value) {
    return 0
  }

  const connectedNodeIds = new Set<string>()
  edges.value.forEach((e) => {
    connectedNodeIds.add(e.source)
    connectedNodeIds.add(e.target)
  })

  return nodes.value.filter((n) => !connectedNodeIds.has(n.id)).length
})

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

// 节点搜索
const {
  data: availableNodes,
  loading: nodesLoading,
  execute: loadAvailableNodes,
} = useFetch<SearchResultItem[]>({
  fetchFn: () => searchApi.searchNodes(nodeSearchText.value.trim()),
  immediate: false,
  defaultValue: [],
  onError: (error) => {
    console.error('搜索节点失败:', error)
    showSnackbar('搜索节点失败', 'error')
  },
})

// 手动搜索节点
const handleNodeSearch = () => {
  if (!nodeSearchText.value.trim()) {
    availableNodes.value = []
    return
  }
  loadAvailableNodes()
}

// 监听节点搜索文本变化，清空旧的搜索结果
watch(nodeSearchText, () => {
  availableNodes.value = []
})

const filteredNodes = computed(() => availableNodes.value)

// 节点样式常量
const ROOT_NODE_STYLE = {
  background: '#616161',
  color: '#ffffff',
  border: '2px solid #9e9e9e',
  borderRadius: '12px',
  padding: '10px',
  fontWeight: '600',
  fontSize: '14px',
}

const COURSE_NODE_STYLE = {
  background: '#fafafa',
  color: '#424242',
  border: '2px solid #bdbdbd',
  borderRadius: '12px',
  padding: '10px',
  fontWeight: '500',
  fontSize: '13px',
}

// 普通节点样式（绿色，区别于课程节点）
const NODE_STYLE = {
  background: '#f1f8e9',
  color: '#33691e',
  border: '2px solid #aed581',
  borderRadius: '12px',
  padding: '10px',
  fontWeight: '500',
  fontSize: '13px',
}

const EDGE_STYLE = {
  stroke: '#78909c',
  strokeWidth: 2,
}

// 节点和边
const nodes = ref<Node[]>([
  {
    id: '0',
    type: 'default',
    data: { label: roleName.value },
    position: { x: 400, y: 100 },
    sourcePosition: undefined,
    targetPosition: Position.Left,
    style: ROOT_NODE_STYLE,
  },
])

const edges = ref<Edge[]>([])

// 计算新节点位置的公共方法
const calculateNodePosition = (): { x: number; y: number } => {
  let x: number
  let y: number

  if (nodes.value.length === 1) {
    // 第一个节点：放在根节点下方居中
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

  return { x, y }
}

// 添加课程节点
const addCourseNode = (course: Course) => {
  const nodeId = course.rootNodeId.toString()

  // 检查是否已存在
  if (nodes.value.find((n) => n.id === nodeId)) {
    showSnackbar('该课程已添加', 'warning')
    return
  }

  // 计算位置
  const { x, y } = calculateNodePosition()

  nodes.value.push({
    id: nodeId, // 使用 rootNodeId
    type: 'default',
    data: { label: `[课程] ${course.name}` }, // 课程前面加文字标识
    position: { x, y },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    style: COURSE_NODE_STYLE,
  })

  // 聚焦到新节点（使用 nextTick 确保 DOM 更新后再聚焦）
  setTimeout(() => {
    setCenter(x, y, { zoom: 1, duration: 300 })
  }, 50)
}

/**
 * 检查节点是否已添加
 */
const isNodeAdded = (nodeId: number | undefined): boolean => {
  if (!nodeId) return false
  return nodes.value.some((n) => n.id === nodeId.toString())
}

/**
 * 跳转到课程详情页
 */
const goToCourseDetail = (courseId: number) => {
  window.open(`/courses/${courseId}`, '_blank')
}

/**
 * 跳转到节点详情页
 */
const goToNodeDetail = (nodeId: number) => {
  window.open(`/read?nodeId=${nodeId}`, '_blank')
}

/**
 * 添加普通节点
 */
const addNode = (node: SearchResultItem) => {
  const nodeId = node.id.toString()

  // 检查是否已存在
  if (nodes.value.find((n) => n.id === nodeId)) {
    showSnackbar('该节点已添加', 'warning')
    return
  }

  // 计算位置
  const { x, y } = calculateNodePosition()

  nodes.value.push({
    id: nodeId,
    type: 'default',
    data: { label: `[节点] ${node.name}` }, // 节点前面加文字标识
    position: { x, y },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    style: NODE_STYLE, // 使用绿色节点样式
  })

  // 聚焦到新节点
  setTimeout(() => {
    setCenter(x, y, { zoom: 1, duration: 300 })
  }, 50)
}

/**
 * 跳转到我的路线图页面
 */
const goToMyRoadmaps = () => {
  router.push('/users/me?mode=creator&tab=roadmaps')
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

  // 检查该源节点是否已经有出口连接
  const hasSourceConnection = edges.value.find((e) => e.source === connection.source)
  if (hasSourceConnection) {
    showSnackbar('每个节点的出口只能连接一条线', 'warning')
    return
  }

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
    style: EDGE_STYLE,
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

// 显示保存对话框
const showSave = () => {
  if (nodes.value.length <= 1) {
    showSnackbar('请至少添加一个学习节点', 'warning')
    return
  }
  showSaveDialog.value = true
}

// 保存路径
const saveRoadmap = async (type: 'draft' | 'publish') => {
  if (!roadmapDescription.value.trim()) {
    showSnackbar('请输入路径描述', 'warning')
    return
  }

  // 验证至少有一个课程节点（除了根节点）
  if (nodes.value.length <= 1) {
    showSnackbar('请至少添加一个课程节点', 'warning')
    return
  }

  saving.value = true
  saveType.value = type

  try {
    // 序列化边数组：[[source, target], ...]
    const edgeArray = edges.value
      .map((e) => {
        const source = parseInt(e.source)
        const target = parseInt(e.target)
        if (isNaN(source) || isNaN(target)) {
          return null
        }
        return [source, target]
      })
      .filter((edge): edge is [number, number] => edge !== null)

    // 找出所有有连接的节点ID
    const connectedNodeIds = new Set<number>()
    edgeArray.forEach(([source, target]) => {
      connectedNodeIds.add(source)
      connectedNodeIds.add(target)
    })

    // 获取所有节点ID
    const allNodeIds = nodes.value
      .map((n) => {
        const id = parseInt(n.id)
        if (isNaN(id)) {
          return null
        }
        return id
      })
      .filter((id): id is number => id !== null)

    let nodeArray: number[]

    if (type === 'draft') {
      // 草稿模式：保留所有节点（包括孤立节点）
      nodeArray = allNodeIds
    } else {
      // 发布模式：只保留有连接的节点
      nodeArray = allNodeIds.filter((id) => connectedNodeIds.has(id))

      // 检查是否有有效节点
      if (nodeArray.length === 0) {
        showSnackbar('请添加课程并连接它们', 'warning')
        return
      }

      // 验证树结构：边数 = 节点数 - 1
      if (edgeArray.length !== nodeArray.length - 1) {
        showSnackbar(
          `路径结构不正确：${nodeArray.length} 个节点需要 ${nodeArray.length - 1} 条连接线，当前有 ${edgeArray.length} 条`,
          'error'
        )
        return
      }
    }

    // 后端期望的格式：[边数组, 节点ID数组]
    const content = JSON.stringify([edgeArray, nodeArray])

    console.log('保存路径数据:', {
      saveType: type,
      totalNodes: nodes.value.length,
      savedNodes: nodeArray.length,
      edges: edgeArray.length,
      removedNodes: nodes.value.length - nodeArray.length,
      edgeArray,
      nodeArray,
      content,
    })

    // 调用 API
    const state = type === 'draft' ? 0 : 1 // 0-草稿，1-提交审核
    let response

    if (draftRoadmapId.value) {
      // 已有草稿ID，调用更新接口
      response = await roadmapApi.updateRoadmap(
        draftRoadmapId.value,
        content,
        roadmapDescription.value.trim(),
        state
      )
    } else {
      // 首次创建
      response = await roadmapApi.createRoadmap(
        roleId.value,
        content,
        roadmapDescription.value.trim(),
        state
      )
    }

    if (response.code === 200) {
      const message = type === 'draft' ? '草稿保存成功' : '路径发布成功'
      showSnackbar(message, 'success')
      showSaveDialog.value = false

      // 草稿模式：保存描述和ID，留在当前页面
      if (type === 'draft') {
        savedDraftDescription.value = roadmapDescription.value.trim()
        if (response.data && response.data.id) {
          draftRoadmapId.value = response.data.id
        }
      } else {
        // 发布模式：返回列表页
        router.back()
      }
    } else {
      showSnackbar(response.message || '保存失败', 'error')
    }
  } catch (error) {
    console.error('保存路径失败:', error)
    showSnackbar('保存失败，请稍后重试', 'error')
  } finally {
    saving.value = false
    saveType.value = ''
  }
}

// 自动布局
const applyAutoLayout = (showMessage = false) => {
  if (nodes.value.length <= 1) {
    if (showMessage) {
      showSnackbar('请至少添加一个节点才能使用自动布局', 'warning')
    }
    return
  }

  // 先找出所有有连接关系的节点ID
  const connectedNodeIds = new Set<string>()
  edges.value.forEach((edge) => {
    connectedNodeIds.add(edge.source.toString())
    connectedNodeIds.add(edge.target.toString())
  })

  // 分离有连接的节点和无连接的节点
  const connectedNodes = nodes.value.filter((node) => connectedNodeIds.has(node.id.toString()))
  const unconnectedNodes = nodes.value.filter((node) => !connectedNodeIds.has(node.id.toString()))

  // 如果有连接的节点，使用 dagre 布局
  if (connectedNodes.length > 0) {
    const dagreGraph = new dagre.graphlib.Graph()
    dagreGraph.setDefaultEdgeLabel(() => ({}))
    dagreGraph.setGraph({
      rankdir: 'LR', // Left to Right - 叶子节点在左边，根节点在右边
      nodesep: 20,
      ranksep: 150,
      marginx: 20,
      marginy: 20,
    })

    const nodeWidth = 120
    const nodeHeight = 40

    // 只添加有连接的节点到 dagre 图
    connectedNodes.forEach((node) => {
      dagreGraph.setNode(node.id.toString(), { width: nodeWidth, height: nodeHeight })
    })

    // 添加边到 dagre 图
    edges.value.forEach((edge) => {
      dagreGraph.setEdge(edge.source.toString(), edge.target.toString())
    })

    // 计算布局
    dagre.layout(dagreGraph)

    // 更新有连接节点的位置
    connectedNodes.forEach((node) => {
      const nodeWithPosition = dagreGraph.node(node.id.toString())
      node.position = {
        x: nodeWithPosition.x - nodeWidth / 2,
        y: nodeWithPosition.y - nodeHeight / 2,
      }
    })
  }

  // 处理没有连接关系的节点
  if (unconnectedNodes.length > 0) {
    // 找到有连接的节点中 x 坐标最大的（最右边的节点）
    let rightX = 0
    let centerY = 400 // 默认中心位置

    if (connectedNodes.length > 0) {
      const rightNode = connectedNodes.reduce((rightmost, node) => {
        return node.position.x > rightmost.position.x ? node : rightmost
      })
      rightX = rightNode.position.x

      // 计算有连接节点的 y 坐标中心位置
      const sumY = connectedNodes.reduce((sum, node) => sum + node.position.y, 0)
      centerY = sumY / connectedNodes.length
    }

    // 网格布局参数
    const rows = 3 // 每列3个节点
    const horizontalSpacing = 200 // 水平间距
    const verticalSpacing = 100 // 垂直间距
    const startX = rightX + 200 // 在最右边节点右侧200px开始

    // 排列无连接的节点
    unconnectedNodes.forEach((node, index) => {
      const col = Math.floor(index / rows)
      const row = index % rows

      // 计算该列的起始 y 坐标，使这一列居中对齐
      const colHeight = Math.min(unconnectedNodes.length - col * rows, rows) * verticalSpacing
      const colStartY = centerY - colHeight / 2 + verticalSpacing / 2

      node.position = {
        x: startX + col * horizontalSpacing,
        y: colStartY + row * verticalSpacing,
      }
    })
  }

  // 重新构建完整的节点数组
  nodes.value = [...connectedNodes, ...unconnectedNodes]

  // 布局完成后，调用 fitView 聚焦到所有节点
  setTimeout(() => {
    fitView({ padding: 0.2, duration: 300 })
  }, 50)

  if (showMessage) {
    showSnackbar('自动布局完成', 'success')
  }
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
          data: { label: roleName.value },
          position: { x: 400, y: 100 },
          sourcePosition: undefined,
          targetPosition: Position.Left,
          style: ROOT_NODE_STYLE,
        },
      ]
      edges.value = []
      roadmapDescription.value = ''
      showSnackbar('已重置所有内容', 'success')
    },
  }
  confirmDialogVisible.value = true
}

// 编辑模式：加载已有路线图数据
const {
  data: roadmapData,
  loading: roadmapLoading,
} = useFetch({
  fetchFn: () => roadmapApi.getRoadmap(roadmapId.value!),
  immediate: isEditMode.value,
  defaultValue: null,
})

// 复制模式：加载要复制的路线图数据
const {
  data: copyRoadmapData,
  loading: copyRoadmapLoading,
} = useFetch({
  fetchFn: () => roadmapApi.getRoadmap(copyId.value!),
  immediate: !!copyId.value,
  defaultValue: null,
})

// 监听加载状态
watch(roadmapLoading, (isLoading) => {
  loading.value = isLoading
})

watch(copyRoadmapLoading, (isLoading) => {
  loading.value = isLoading
})

// 监听路线图数据加载完成
watch(roadmapData, (newData) => {
  if (newData && isEditMode.value) {
    console.log('加载的路线图数据:', newData)

    // 设置描述和状态
    roadmapDescription.value = newData.description || ''
    savedDraftDescription.value = newData.description || ''
    draftRoadmapId.value = newData.id
    roadmapState.value = newData.state // 保存路线图状态

    // 解析 content 并设置节点和边
    try {
      // content 是 JSON 字符串，格式为 {nodes: [], edges: []}
      const contentData = typeof newData.content === 'string'
        ? JSON.parse(newData.content)
        : newData.content

      console.log('解析后的 content:', contentData)

      if (contentData.nodes && contentData.edges) {
        // 设置边
        edges.value = contentData.edges.map((edge: any) => ({
          id: `${edge.source}-${edge.target}`,
          source: edge.source.toString(),
          target: edge.target.toString(),
          type: 'default',
          animated: true,
          style: EDGE_STYLE,
        }))

        // 设置节点
        nodes.value = contentData.nodes.map((node: any) => {
          if (node.id === '0' || node.id === 0) {
            return {
              id: '0',
              type: 'default',
              data: { label: roleName.value },
              position: { x: 0, y: 0 },
              sourcePosition: undefined,
              targetPosition: Position.Left,
              style: ROOT_NODE_STYLE,
            }
          } else {
            return {
              id: node.id.toString(),
              type: 'default',
              data: { label: node.name || `课程 ${node.id}` },
              position: { x: 0, y: 0 },
              sourcePosition: Position.Right,
              targetPosition: Position.Left,
              style: COURSE_NODE_STYLE,
            }
          }
        })

        console.log('设置的节点:', nodes.value.length, '设置的边:', edges.value.length)

        // 使用自动布局
        setTimeout(() => {
          applyAutoLayout()
        }, 100)
      }
    } catch (parseError) {
      console.error('解析路线图内容失败:', parseError)
      showSnackbar('加载路线图数据失败', 'error')
    }
  }
})

// 监听复制路线图数据加载完成
watch(copyRoadmapData, (newData) => {
  if (newData && copyId.value) {
    console.log('加载的复制路线图数据:', newData)

    // 设置描述（添加"副本"标识）
    roadmapDescription.value = `${newData.description || '未命名路线图'} (副本)`

    // 解析 content 并设置节点和边
    try {
      const contentData = typeof newData.content === 'string'
        ? JSON.parse(newData.content)
        : newData.content

      console.log('解析后的 content:', contentData)

      if (contentData.nodes && contentData.edges) {
        // 重新生成节点ID，避免ID冲突
        const idMap = new Map<string, string>()
        const newNodes = contentData.nodes.map((node: any) => {
          const newId = node.id === '0' || node.id === 0
            ? '0'
            : `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
          idMap.set(node.id.toString(), newId)

          if (node.id === '0' || node.id === 0) {
            return {
              id: '0',
              type: 'default',
              data: { label: roleName.value },
              position: { x: 0, y: 0 },
              sourcePosition: undefined,
              targetPosition: Position.Left,
              style: ROOT_NODE_STYLE,
            }
          } else {
            return {
              id: newId,
              type: 'default',
              data: { label: node.name || `课程 ${node.id}` },
              position: { x: 0, y: 0 },
              sourcePosition: Position.Right,
              targetPosition: Position.Left,
              style: COURSE_NODE_STYLE,
            }
          }
        })

        // 更新边的ID引用
        const newEdges = contentData.edges.map((edge: any) => ({
          id: `${idMap.get(edge.source.toString())}-${idMap.get(edge.target.toString())}`,
          source: idMap.get(edge.source.toString()) || edge.source.toString(),
          target: idMap.get(edge.target.toString()) || edge.target.toString(),
          type: 'default',
          animated: true,
          style: EDGE_STYLE,
        }))

        nodes.value = newNodes
        edges.value = newEdges

        console.log('设置的节点:', nodes.value.length, '设置的边:', edges.value.length)

        // 使用自动布局
        setTimeout(() => {
          applyAutoLayout()
        }, 100)
      }
    } catch (parseError) {
      console.error('解析复制路线图内容失败:', parseError)
      showSnackbar('加载路线图数据失败', 'error')
    }
  }
})

// 如果是复制模式,加载数据
onMounted(() => {
  if (copyId.value) {
    // 数据加载已由 useFetch 处理，这里不需要额外操作
  }
})
</script>

<style scoped>
.roadmap-create-page {
  /* 使用 DefaultLayout 的默认 padding */
}

/* 草稿描述区域样式 */
.draft-description-section {
  padding: 0;
}

/* 草稿描述文本样式 - 最多显示5行 */
.draft-description-text {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  line-height: 1.5;
}

/* 对话框关闭按钮 */
.dialog-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 1;
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
  padding: 8px 16px 8px 0;
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

/* 工具栏分隔符 */
.toolbar-divider {
  height: 24px;
  align-self: center;
}

/* 隐藏根节点的 source handle */
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle.source),
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle-right),
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle-bottom) {
  display: none !important;
}
</style>
