<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">路线图管理</h2>

    <!-- ID查询 -->
    <v-card flat class="border mb-4">
      <v-card-text>
        <v-row align="center">
          <v-col cols="3">
            <v-text-field
              v-model.number="professionIdFilter"
              label="职业 ID"
              type="number"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="onFilterChange"
            ></v-text-field>
          </v-col>
          <v-col cols="3">
            <v-text-field
              v-model.number="creatorIdFilter"
              label="创建者 ID"
              type="number"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="onFilterChange"
            ></v-text-field>
          </v-col>
          <v-col cols="auto">
            <v-btn variant="tonal" size="default" @click="onFilterChange">
              <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
              筛选
            </v-btn>
            <v-btn
              v-if="professionIdFilter || creatorIdFilter"
              variant="text"
              size="default"
              @click="onResetFilter"
            >
              清除
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 路线图列表 -->
    <v-card flat class="border">
      <v-card-text>
        <!-- 状态标签 -->
        <v-tabs
          v-model="selectedStateIndex"
          color="primary"
          density="compact"
          @update:model-value="onStateChange"
          class="mb-4"
        >
          <v-tab
            v-for="(state, index) in stateOptions"
            :key="state.value"
            :value="index"
            class="text-none"
            size="small"
          >
            <v-icon :icon="state.icon" size="14" class="mr-1"></v-icon>
            {{ state.text }}
          </v-tab>
        </v-tabs>

        <!-- 首次加载状态 -->
        <div v-if="loading && roadmapList.length === 0" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!loading && roadmapList.length === 0" class="text-center py-12">
          <v-icon icon="mdi-map-marker-path" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">暂无路线图</p>
        </div>

        <!-- 列表 -->
        <div v-else>
          <div
            v-for="roadmap in roadmapList"
            :key="roadmap.id"
            v-intersect="{
              handler: (isIntersecting) => {
                if (
                  isIntersecting &&
                  roadmap === roadmapList[roadmapList.length - 1] &&
                  hasMoreData &&
                  !loading
                ) {
                  loadMore()
                }
              },
            }"
            class="list-item mb-3"
          >
            <div class="d-flex align-start">
              <!-- 操作区 -->
              <div class="action-area mr-4">
                <!-- 待审核 -->
                <div v-if="roadmap.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block :loading="roadmap.approving" @click="approveRoadmap(roadmap, 'APPROVE')">
                    通过
                  </v-btn>
                  <v-btn variant="tonal" color="error" size="small" block @click="rejectRoadmap(roadmap)">
                    拒绝
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(roadmap)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已通过 -->
                <div v-if="roadmap.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="warning" size="small" block @click="rejectRoadmap(roadmap)">
                    撤回
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(roadmap)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已拒绝 -->
                <div v-if="roadmap.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block :loading="roadmap.restoring" @click="approveRoadmap(roadmap, 'APPROVE')">
                    通过
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(roadmap)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已屏蔽 -->
                <div v-if="roadmap.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="info" size="small" block @click="unbanRoadmap(roadmap)">
                    解封
                  </v-btn>
                  <v-btn variant="tonal" color="warning" size="small" block @click="downgradeToRejected(roadmap)">
                    降级
                  </v-btn>
                </div>

                <!-- 编辑按钮 -->
                <div class="mt-2">
                  <v-btn variant="tonal" color="info" size="small" block @click="showEditModal(roadmap)">
                    编辑
                  </v-btn>
                </div>
              </div>

              <!-- 内容区 -->
              <div class="flex-grow-1">
                <!-- 标题行 -->
                <div class="d-flex align-center justify-space-between mb-2">
                  <div class="d-flex align-center">
                    <a
                      :href="systemConfigStore.getRoadmapUrl(roadmap.id)"
                      target="_blank"
                      class="text-body-1 font-weight-medium text-grey-darken-3 text-decoration-none"
                    >
                      {{ roadmap.profession?.name || '未知职业' }}
                      <v-icon icon="mdi-open-in-new" size="14" color="grey" class="ml-1"></v-icon>
                    </a>
                    <v-chip variant="flat" :color="getStateConfig(roadmap.state).color" size="x-small" class="ml-2">
                      {{ getStateConfig(roadmap.state).text }}
                    </v-chip>
                  </div>
                  <div class="d-flex align-center text-caption text-grey-darken-1">
                    <a v-if="roadmap.creator" :href="systemConfigStore.getUserUrl(roadmap.creator.id)" target="_blank" class="text-grey-darken-1">{{ roadmap.creator.name }}</a>
                    <span v-else>未知</span>
                    <span class="mx-1">·</span>
                    <span>{{ roadmap.createdAt || '未知时间' }}</span>
                    <span class="mx-1">·</span>
                    <span>ID: {{ roadmap.id }}</span>
                  </div>
                </div>

                <!-- 内容 -->
                <div class="content-wrapper d-flex">
                  <!-- 左侧描述 -->
                  <div class="content-left flex-grow-1">
                    <div class="text-body-2 text-grey-darken-1">
                      {{ roadmap.description || '暂无描述' }}
                    </div>

                    <!-- 拒绝/封禁原因 -->
                    <div v-if="(roadmap.state === ContentState.REJECTED || roadmap.state === ContentState.BANNED) && roadmap.reason" class="mt-2">
                      <span class="text-caption text-red-darken-2">{{ roadmap.state === ContentState.BANNED ? '封禁' : '拒绝' }}原因：{{ roadmap.reason }}</span>
                    </div>
                  </div>

                  <!-- 右侧图标 -->
                  <div class="graph-icon-area d-flex align-center justify-center" @click="showGraphDialog(roadmap)">
                    <v-icon icon="mdi-sitemap" size="24" color="grey-darken-1"></v-icon>
                    <v-tooltip activator="parent" location="top">查看路线图</v-tooltip>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多指示器 -->
        <div v-if="loading && roadmapList.length > 0" class="text-center py-4">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 没有更多 -->
        <div v-if="!hasMoreData && roadmapList.length > 0" class="text-center py-4 text-caption text-grey">
          没有更多了
        </div>
      </v-card-text>
    </v-card>

    <!-- 编辑描述对话框 -->
    <v-dialog v-model="showEditDialog" max-width="700px" persistent>
      <v-card rounded="lg" variant="flat">
        <v-card-title class="text-h6 font-weight-bold pa-6 pb-4">
          <v-icon icon="mdi-pencil-outline" color="blue-darken-2" class="mr-3"></v-icon>
          编辑路线图描述
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <v-form ref="editForm" v-model="editFormValid">
            <v-textarea
              v-model="editDescription"
              label="路线图描述"
              :rules="roadmapContentRules"
              :counter="roadmapContentMaxLength"
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              rows="6"
              clearable
              placeholder="请输入路线图描述..."
            ></v-textarea>
          </v-form>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn variant="outlined" color="grey" rounded="lg" @click="closeEditDialog">
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="primary"
            rounded="lg"
            :loading="updating"
            @click="updateRoadmapDescription"
          >
            <v-icon icon="mdi-content-save" class="mr-2"></v-icon>
            保存修改
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="`路线图 ID: ${currentRoadmap?.id || ''}`"
      :item-state="currentRoadmap?.state"
      item-type="路线图"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />

    <!-- 路线图预览弹窗 -->
    <v-dialog v-model="graphDialogVisible" max-width="1400px">
      <v-card rounded="lg" variant="flat">
        <v-card-title class="d-flex align-center justify-space-between py-2 px-4">
          <span class="text-subtitle-1 font-weight-medium">{{ graphRoadmap?.profession?.name || '路线图' }}</span>
          <v-btn icon variant="text" size="small" @click="graphDialogVisible = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text class="pa-0">
          <div class="graph-container">
            <div v-if="graphLoading" class="d-flex align-center justify-center h-100">
              <v-progress-circular indeterminate color="primary" size="32"></v-progress-circular>
              <span class="ml-2 text-grey-darken-1">加载中...</span>
            </div>
            <RoadmapVueFlow
              v-else
              :nodes="graphRoadmap?.parsedNodes || []"
              :edges="graphRoadmap?.parsedEdges || []"
              :readonly="true"
              :show-background="true"
            />
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import dagre from 'dagre'
import { Position } from '@vue-flow/core'
import type { Node, Edge } from '@vue-flow/core'
import { adminApi, roadmapApi } from '@/api'
import { ContentState } from '@/enums'
import type { Roadmap } from '@/types/roadmap.d'
import type { StateOption } from '@/types/common.d'
import RejectBanDialog from './RejectBanDialog.vue'
import RoadmapVueFlow from '../common/RoadmapVueFlow.vue'
import { useFetchForScroll } from '@/composables/useFetchForScroll'
import { useSystemConfigStore } from '@/stores'

const systemConfigStore = useSystemConfigStore()
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'

// 扩展 Roadmap 类型，包含解析后的 nodes 和 edges
interface RoadmapWithGraph extends Roadmap {
  parsedNodes?: Node[]
  parsedEdges?: Edge[]
}

const professionIdFilter = ref<number | null>(null)
const creatorIdFilter = ref<number | null>(null)
const selectedStateIndex = ref<number>(0)

// 验证规则
const roadmapContentRules = useValidationRules('roadmap-content')
const roadmapContentMaxLength = useMaxLength('roadmap-content')

const showEditDialog = ref<boolean>(false)
const editFormValid = ref<boolean>(false)
const currentRoadmap = ref<Roadmap | null>(null)
const editDescription = ref<string>('')
const editForm = ref(null)

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const dialogType = ref<'reject' | 'ban'>('reject')

// 路线图预览弹窗
const graphDialogVisible = ref<boolean>(false)
const graphRoadmap = ref<RoadmapWithGraph | null>(null)
const graphRoadmapId = ref<number>(0)

const { loading: graphLoading, execute: fetchGraphRoadmap } = useFetch({
  fetchFn: () => roadmapApi.getRoadmap(graphRoadmapId.value),
  immediate: false,
  onSuccess: (data) => {
    if (data) {
      graphRoadmap.value = processRoadmapData(data as Roadmap)
    }
  },
})

const showGraphDialog = (roadmap: RoadmapWithGraph): void => {
  graphDialogVisible.value = true
  graphRoadmap.value = null
  graphRoadmapId.value = roadmap.id
  fetchGraphRoadmap()
}

// 自动布局函数
const applyAutoLayout = (nodeList: Node[], edgeList: Edge[]): Node[] => {
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({
    rankdir: 'LR',
    nodesep: 60,
    ranksep: 120,
    marginx: 20,
    marginy: 20,
  })

  const nodeWidth = 80
  const nodeHeight = 28

  nodeList.forEach((node) => {
    dagreGraph.setNode(node.id.toString(), { width: nodeWidth, height: nodeHeight })
  })
  edgeList.forEach((edge) => {
    dagreGraph.setEdge(edge.source.toString(), edge.target.toString())
  })

  dagre.layout(dagreGraph)

  return nodeList.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id.toString())
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - nodeWidth / 2,
        y: nodeWithPosition.y - nodeHeight / 2,
      },
    }
  })
}

// 解析 content 字段
// 原始格式: [[edges], [nodeIds]]，edges: [[source, target], ...]，nodeIds: [id1, id2, ...]
// 或已解析格式: { nodes: [...], edges: [...] }
const parseContent = (content: string | object, professionName: string): { nodes: Node[]; edges: Edge[] } => {
  try {
    const data = typeof content === 'string' ? JSON.parse(content) : content
    if (!data) {
      return { nodes: [], edges: [] }
    }

    // 检查是否是已解析的格式 { nodes: [...], edges: [...] }
    if (data.nodes && Array.isArray(data.nodes)) {
      const nodes = (data.nodes || []).map((node: { id: number | string; name?: string; isCourseRoot?: boolean; position?: { x: number; y: number } }): Node => {
        if (node.id === 0 || node.id === '0') {
          return {
            id: String(node.id),
            type: 'default',
            data: { label: professionName || '职业' },
            position: node.position || { x: 0, y: 0 },
            targetPosition: Position.Left,
          }
        }

        const prefix = node.isCourseRoot ? '[C] ' : '[N] '
        const label = prefix + (node.name || `节点${node.id}`)

        return {
          id: String(node.id),
          type: 'default',
          data: { label, isCourseRoot: node.isCourseRoot },
          position: node.position || { x: 0, y: 0 },
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
          class: node.isCourseRoot ? 'course-node' : '',
        }
      })

      const edges = (data.edges || []).map((edge: { source: string | number; target: string | number; type?: string }): Edge => ({
        id: `${edge.source}-${edge.target}`,
        source: String(edge.source),
        target: String(edge.target),
        type: edge.type || 'bezier',
      }))

      return { nodes, edges }
    }

    // 原始格式: [[edges], [nodeIds]]
    if (Array.isArray(data) && data.length >= 2) {
      const edgeData = data[0] || []
      const nodeIds = data[1] || []

      // 解析节点
      const nodes: Node[] = nodeIds.map((nodeId: number): Node => {
        if (nodeId === 0) {
          return {
            id: '0',
            type: 'default',
            data: { label: professionName || '职业' },
            position: { x: 0, y: 0 },
            targetPosition: Position.Left,
          }
        }

        return {
          id: String(nodeId),
          type: 'default',
          data: { label: `节点${nodeId}` },
          position: { x: 0, y: 0 },
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
        }
      })

      // 解析边
      const edges: Edge[] = edgeData.map((edge: [number, number]): Edge => ({
        id: `${edge[0]}-${edge[1]}`,
        source: String(edge[0]),
        target: String(edge[1]),
        type: 'bezier',
      }))

      return { nodes, edges }
    }

    return { nodes: [], edges: [] }
  } catch {
    return { nodes: [], edges: [] }
  }
}

// 处理路线图数据，解析并布局
const processRoadmapData = (roadmap: Roadmap): RoadmapWithGraph => {
  if (!roadmap.content) {
    return { ...roadmap, parsedNodes: [], parsedEdges: [] }
  }

  const professionName = roadmap.profession?.name || '职业'
  const { nodes, edges } = parseContent(roadmap.content, professionName)
  const layoutedNodes = nodes.length > 0 ? applyAutoLayout(nodes, edges) : []

  return {
    ...roadmap,
    parsedNodes: layoutedNodes,
    parsedEdges: edges,
  }
}

// 状态选项
const stateOptions: StateOption[] = [
  {
    value: ContentState.SUBMITTED,
    text: '待审核',
    color: 'orange-lighten-4',
    icon: 'mdi-clock-outline',
  },
  {
    value: ContentState.PUBLISHED,
    text: '已通过',
    color: 'green-lighten-4',
    icon: 'mdi-check-circle',
  },
  {
    value: ContentState.REJECTED,
    text: '已拒绝',
    color: 'red-lighten-4',
    icon: 'mdi-close-circle',
  },
  {
    value: ContentState.BANNED,
    text: '已封禁',
    color: 'grey-lighten-2',
    icon: 'mdi-cancel',
  },
]

// 获取当前选中的状态
const getCurrentState = (): number =>
  stateOptions[selectedStateIndex.value]?.value || ContentState.SUBMITTED

// 根据状态获取配置
const getStateConfig = (state?: number): StateOption => {
  return stateOptions.find((option) => option.value === state) || stateOptions[0]
}

// 使用 useFetchForScroll 进行列表加载
const {
  items: rawRoadmapList,
  loading,
  hasMore: hasMoreData,
  loadMore,
  reset: resetList,
} = useFetchForScroll<Roadmap>({
  fetchFn: (params) => {
    const currentState = getCurrentState()
    return adminApi.getContentsByState('roadmap', currentState, params.lastId ?? undefined)
  },
  initialParams: {
    lastId: null,
  },
  immediate: true,
})

// 使用 computed 处理路线图数据，添加解析后的 nodes 和 edges
const roadmapList = computed<RoadmapWithGraph[]>(() => {
  return rawRoadmapList.value.map(processRoadmapData)
})

// 状态改变
const onStateChange = (): void => {
  resetList()
  loadMore()
}

// 筛选条件变化
const onFilterChange = (): void => {
  resetList()
  loadMore()
}

// 重置筛选
const onResetFilter = (): void => {
  professionIdFilter.value = null
  creatorIdFilter.value = null
  resetList()
}

// 显示编辑对话框
const showEditModal = (roadmap: Roadmap): void => {
  currentRoadmap.value = roadmap
  editDescription.value = roadmap.description || ''
  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = (): void => {
  showEditDialog.value = false
  currentRoadmap.value = null
  editDescription.value = ''
  editFormValid.value = false
}

// 使用 useMutation 更新路线图描述
const { execute: updateRoadmapDescription, loading: updating } = useMutation(
  () => adminApi.updateRoadmap(currentRoadmap.value!.id, editDescription.value || ''),
  {
    successMessage: '更新成功',
    onSuccess: (result) => {
      const index = rawRoadmapList.value.findIndex((r) => r.id === currentRoadmap.value!.id)
      if (index !== -1) {
        rawRoadmapList.value[index] = result
      }
      closeEditDialog()
    },
  }
)

// 使用 useMutation 批准路线图
const { execute: executeApprove } = useMutation(
  (payload: { id: number; action: string }) =>
    adminApi.operateContent('roadmap', payload.id, { action: payload.action }),
  {
    successMessage: '操作成功',
    onSuccess: (result, payload) => {
      const index = rawRoadmapList.value.findIndex((r) => r.id === payload.id)
      if (index !== -1) {
        const currentState = getCurrentState()
        if (currentState !== result.state) {
          rawRoadmapList.value.splice(index, 1)
        } else {
          rawRoadmapList.value[index] = result
        }
      }
    },
  }
)

// 批准路线图
const approveRoadmap = async (roadmap: Roadmap, action: string): Promise<void> => {
  await executeApprove({ id: roadmap.id, action })
}

// 显示拒绝对话框
const showRejectDialog = (roadmap: Roadmap): void => {
  currentRoadmap.value = roadmap
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 显示屏蔽对话框
const showBanDialog = (roadmap: Roadmap): void => {
  currentRoadmap.value = roadmap
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽操作
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (payload: { id: number; action: string; reason: string }) =>
    adminApi.operateContent('roadmap', payload.id, { action: payload.action, reason: payload.reason }),
  {
    successMessage: '操作成功',
    onSuccess: (result) => {
      const index = rawRoadmapList.value.findIndex((r) => r.id === currentRoadmap.value!.id)
      if (index !== -1) {
        const currentState = getCurrentState()
        if (currentState !== result.state) {
          rawRoadmapList.value.splice(index, 1)
        } else {
          rawRoadmapList.value[index] = result
        }
      }
      showReasonDialog.value = false
      currentRoadmap.value = null
    },
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string): Promise<void> => {
  if (!currentRoadmap.value) return

  const action = dialogType.value === 'reject' ? 'REJECT' : 'BAN'
  await executeRejectOrBan({
    id: currentRoadmap.value.id,
    action,
    reason,
  })
}

// 拒绝路线图（供按钮调用）
const rejectRoadmap = (roadmap: Roadmap): void => {
  showRejectDialog(roadmap)
}

// 取消屏蔽路线图
const unbanRoadmap = async (roadmap: Roadmap): Promise<void> => {
  await executeApprove({ id: roadmap.id, action: 'APPROVE' })
}

// 降级为拒绝
const downgradeToRejected = (roadmap: Roadmap): void => {
  showRejectDialog(roadmap)
}

// 屏蔽路线图（供按钮调用）
const showBanModal = (roadmap: Roadmap): void => {
  showBanDialog(roadmap)
}
</script>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.list-item {
  padding: 16px;
  border-radius: 8px;
  background-color: #fafafa;
}

.action-area {
  width: 70px;
  flex-shrink: 0;
}

.content-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background-color: white;
  overflow: hidden;
}

.content-left {
  padding: 12px;
  min-height: 65px;
}

.graph-icon-area {
  width: 60px;
  min-width: 60px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.graph-icon-area:hover {
  background-color: #f0f0f0;
}

.graph-container {
  width: 100%;
  height: 700px;
  background: #fafafa;
}
</style>
