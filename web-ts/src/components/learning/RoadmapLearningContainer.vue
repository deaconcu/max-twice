<script setup lang="ts">
import { ref, computed, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { progressServiceV1 } from '@/services/api/v1/apiServiceV1'
import RoadmapLearningCard from './RoadmapLearningCard.vue'
import RoadmapDetail from '@/components/roadmap/RoadmapDetail.vue'
import dagre from 'dagre'
import { UserRoadmapState } from '@/types/enums'
import type { UserRoadmap, ProcessedUserRoadmap } from '@/types/userRoadmap'
import type { Profession } from '@/types/profession'
import type { 
  ExtendedNodeData, 
  ExtendedFlowNode, 
  FlowEdge 
} from '@/types/flow'
import { Position } from '@vue-flow/core'

// 使用全局流程图类型的别名
type NodeData = ExtendedNodeData
type NodeBase = ExtendedFlowNode
type Edge = FlowEdge

interface Props {
  searchQuery?: string
  selectedStatus?: string
}

const props = withDefaults(defineProps<Props>(), {
  searchQuery: '',
  selectedStatus: 'all'
})

const { t } = useI18n()
const showSnackbar = inject<(message: string) => void>('showSnackbar')
const router = useRouter()

// 响应式数据
const roadmaps = ref<ProcessedUserRoadmap[]>([])
const loading = ref<boolean>(true)
const error = ref<string | null>(null)

// RoadmapDetail 浮层状态
const showRoadmapDetail = ref<boolean>(false)
const selectedRoadmap = ref<ProcessedUserRoadmap | null>(null)

// 自动布局函数
const applyAutoLayout = (nodeList: NodeBase[], edgeList: Edge[], direction = 'BT'): NodeBase[] => {
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({
    rankdir: direction,
    nodesep: 180,
    ranksep: 80,
    marginx: 20,
    marginy: 20,
  })

  const nodeWidth = 100
  const nodeHeight = 36

  // 添加节点和边到 dagre 图
  nodeList.forEach((node) => {
    dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight })
  })
  edgeList.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target)
  })

  // 计算布局
  dagre.layout(dagreGraph)

  // 更新节点位置
  return nodeList.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id)
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
const parseContent = (content: string | any, profession: Profession | null = null): { nodes: NodeBase[], edges: Edge[] } => {
  try {
    const data = typeof content === 'string' ? JSON.parse(content) : content

    if (!data || typeof data !== 'object') {
      return { nodes: [], edges: [] }
    }

    const nodes = (data.nodes || []).map((node: any, index: number): NodeBase => {
      // 如果是根节点（id=0），设置特殊处理
      if (node.id === 0 || node.id === '0') {
        return {
          id: String(node.id),
          type: 'default',
          data: {
            label: profession?.name || '当前职业',
            link: null,
            ...node.data,
          },
          position: node.position || { x: 0, y: 0 },
          targetPosition: Position.Bottom,
        }
      }

      const completed = node.finished || node.completed || false
      const progress = node.progress || 0

      return {
        id: String(node.id || index),
        type: 'default',
        data: {
          label: node.name || node.label || `节点 ${node.id || index}`,
          link: `/read?courseId=${node.id || index}`,
          completed,
          progress,
          current: node.current || node.data?.current || false,
          courseId: node.id,
          ...node.data,
        },
        position: node.position || { x: 0, y: 0 },
        sourcePosition: Position.Top,
        targetPosition: Position.Bottom,
        class: completed ? 'completed-course' : progress > 0 ? 'progress-course' : '',
        style: progress > 0 ? { '--progress': `${progress}%` } : {},
      }
    })

    const edges = (data.edges || []).map((edge: any): Edge => ({
      id: `${edge.source}-${edge.target}`,
      source: String(edge.source),
      target: String(edge.target),
      type: edge.type || 'bezier',
      animated: edge.animated || true,
      label: edge.label,
    }))

    return { nodes, edges }
  } catch {
    return { nodes: [], edges: [] }
  }
}

// 筛选后的路线图数据
const filteredRoadmaps = computed((): ProcessedUserRoadmap[] => {
  return roadmaps.value.filter((roadmap) => {
    // 状态筛选
    if (props.selectedStatus !== 'all' && roadmap.state !== Number(props.selectedStatus)) {
      return false
    }
    // 搜索筛选
    if (props.searchQuery) {
      const query = props.searchQuery.toLowerCase()
      return (
        roadmap.title?.toLowerCase().includes(query) ||
        roadmap.description?.toLowerCase().includes(query) ||
        roadmap.creator?.name?.toLowerCase().includes(query)
      )
    }
    return true
  })
})

// 加载路线图数据
const loadRoadmapData = async (): Promise<void> => {
  loading.value = true
  error.value = null
  
  try {
    const response = await progressServiceV1.getUserRoadmaps()
    
    if (response.code === 200 && Array.isArray(response.data)) {
      roadmaps.value = (response.data as UserRoadmap[]).map((userRoadmap: UserRoadmap): ProcessedUserRoadmap => {
        const { roadmap } = userRoadmap
        const { nodes, edges } = parseContent(roadmap.content, roadmap.profession)

        // 布局计算
        const layoutedNodes = applyAutoLayout(nodes, edges, 'BT')

        // 计算路线图的真实进度
        const calculateRoadmapProgress = (nodes: NodeBase[]): number => {
          let totalProgress = 0
          let totalCourses = 0

          nodes.forEach((node) => {
            if (node.data && node.data.type === 'course') {
              totalCourses++
              if (node.data.completed) {
                totalProgress += 100
              } else if (node.data.progress) {
                totalProgress += parseFloat(String(node.data.progress))
              }
            }
          })

          return totalCourses > 0 ? totalProgress / totalCourses : 0
        }

        const completedNodes = nodes.filter((node) => node.data.completed).length
        const totalNodes = nodes.length
        const realProgress = calculateRoadmapProgress(nodes)

        return {
          id: roadmap.id,
          title: roadmap.description || `学习路线图 ${roadmap.id}`,
          description: roadmap.description || '暂无描述',
          creator: roadmap.creator,
          createdAt: roadmap.createdAt,
          addedDate: userRoadmap.startedAt,
          vote: roadmap.vote || 0,
          upvoted: roadmap.upvoted || false,
          progress: realProgress,
          completedNodes,
          totalNodes,
          lastActivity: getRelativeTime(userRoadmap.updatedAt),
          state: userRoadmap.state,
          startedAt: userRoadmap.startedAt,
          completedAt: userRoadmap.completedAt,
          tags: extractTags(roadmap.description),
          profession: roadmap.profession,
          nodes: layoutedNodes,
          edges,
          content: roadmap.content || '',
        }
      })
    }
  } catch (err) {
    console.error('Error loading roadmap data:', err)
    error.value = t('learning.loadFailed')
    showSnackbar?.(t('learning.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 获取相对时间
const getRelativeTime = (dateString?: string): string => {
  if (!dateString) return t('learning.timeAgo.unknownTime')

  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 60) {
    return t('learning.timeAgo.minutesAgo', { minutes: diffMins })
  } else if (diffHours < 24) {
    return t('learning.timeAgo.hoursAgo', { hours: diffHours })
  } else {
    return t('learning.timeAgo.daysAgo', { days: diffDays })
  }
}

// 从描述中提取标签
const extractTags = (description?: string): string[] => {
  if (!description) return []

  const commonTags = ['前端', 'Vue.js', 'JavaScript', 'React', 'Python', '数据科学', '机器学习', 'Java', 'Spring']
  return commonTags.filter((tag) => description.includes(tag)).slice(0, 3)
}

// 获取状态文本
const getStatusText = (state: string): string => {
  const roadmapStateTexts: Record<string, string> = {
    [UserRoadmapState.NOT_STARTED]: '未开始',
    [UserRoadmapState.IN_PROGRESS]: '进行中',
    [UserRoadmapState.COMPLETED]: '已完成',
  }
  return roadmapStateTexts[state] || t('learning.unknownStatus', '未知状态')
}

// 打开路线图详情
const openRoadmapDetail = (roadmap: any): void => {
  selectedRoadmap.value = roadmap
  showRoadmapDetail.value = true
}

// 路线图投票功能
const handleVoteRoadmap = async (roadmap: any, event?: Event): Promise<void> => {
  if (event) {
    event.stopPropagation()
  }
  try {
    // TODO: 调用后端API
    roadmap.upvoted = !roadmap.upvoted
    roadmap.vote += roadmap.upvoted ? 1 : -1
    showSnackbar?.(roadmap.upvoted ? t('learning.votedSuccess') : t('learning.unvotedSuccess'))
  } catch (error) {
    console.error('投票失败:', error)
    showSnackbar?.(t('learning.operationFailed'))
  }
}

const moveRoadmapUp = (roadmap: any, event?: Event): void => {
  if (event) {
    event.stopPropagation()
  }
  showSnackbar?.(t('learning.roadmapMoveUp'))
}

const moveRoadmapDown = (roadmap: any, event?: Event): void => {
  if (event) {
    event.stopPropagation()
  }
  showSnackbar?.(t('learning.roadmapMoveDown'))
}

const closeRoadmap = async (roadmap: any, event?: Event): Promise<void> => {
  if (event) {
    event.stopPropagation()
  }

  try {
    const response = await progressServiceV1.deleteRoadmapProgress(roadmap.id)

    if (response.code === 200) {
      showSnackbar?.(t('common.success'))
      roadmaps.value = roadmaps.value.filter(r => r.id !== roadmap.id)
    } else {
      showSnackbar?.(t('learning.operationFailed'))
    }
  } catch (error) {
    console.error('取消学习失败:', error)
    showSnackbar?.(t('learning.operationFailed'))
  }
}

onMounted(() => {
  loadRoadmapData()
})

// 暴露给父组件的数据和方法
defineExpose({
  roadmaps: filteredRoadmaps,
  loading,
  error,
  refresh: loadRoadmapData
})
</script>

<template>
  <div>
    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="text-grey-darken-2 mt-4">{{ t('learning.loadingRoadmaps') }}</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="text-center py-8">
      <v-alert type="error" variant="tonal">{{ error }}</v-alert>
      <v-btn color="primary" variant="flat" class="mt-4" @click="loadRoadmapData">
        {{ t('common.retry') }}
      </v-btn>
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredRoadmaps.length === 0" class="text-center py-8">
      <v-card flat color="grey-lighten-5" rounded="lg">
        <v-card-text class="py-8">
          <v-icon
            icon="mdi-map-search"
            size="64"
            color="grey-lighten-1"
            class="mb-4"
          ></v-icon>
          <h3 class="text-h6 text-grey-darken-2 mb-2">
            {{
              selectedStatus === 'all'
                ? t('learning.noRoadmaps')
                : t('learning.noStatusRoadmaps', { status: getStatusText(selectedStatus) })
            }}
          </h3>
          <p class="text-body-2 text-grey-darken-1">
            {{
              selectedStatus === 'all'
                ? t('learning.browseRoadmapsDesc')
                : t('learning.tryOtherStatus')
            }}
          </p>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            class="mt-4"
            @click="router.push('/roadmap')"
          >
            <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
            {{ t('learning.browseRoadmaps') }}
          </v-btn>
        </v-card-text>
      </v-card>
    </div>

    <!-- 路线图列表 -->
    <div v-else>
      <RoadmapLearningCard
        v-for="roadmap in filteredRoadmaps"
        :key="roadmap.id"
        :roadmap="roadmap as any"
        @open-detail="openRoadmapDetail"
        @vote="handleVoteRoadmap"
        @move-up="moveRoadmapUp"
        @move-down="moveRoadmapDown"
        @close="closeRoadmap"
      />
    </div>

    <!-- RoadmapDetail 浮层 -->
    <RoadmapDetail
      v-if="selectedRoadmap"
      v-model="showRoadmapDetail"
      :roadmap="selectedRoadmap as any"
      @close="showRoadmapDetail = false"
    />
  </div>
</template>

<style scoped>


/* 状态标签样式 */
.status-badge-container {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
}

.status-badge {
  border-radius: 12px !important;
  font-weight: 600 !important;
  text-transform: none !important;
  letter-spacing: 0.5px !important;
}

/* 关闭按钮样式 */
.close-btn {
  min-width: auto !important;
  width: 24px !important;
  height: 24px !important;
  border-radius: 50% !important;
  padding: 0 !important;
  transition: all 0.2s ease !important;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.08) !important;
  transform: scale(1.1) !important;
}

/* 时间信息样式 */
.time-info {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  padding: 8px 12px;
  margin-top: 8px;
}

/* 描述文字样式 - 最多三行 */
.description-text {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  max-height: calc(1.4em * 3);
}

/* 路线图卡片样式 */
.roadmap-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.roadmap-card:hover {
  transform: translateY(-4px);
  border-color: #4db6ac !important;
}

.flat-card {
  box-shadow: none !important;
  border: 1px solid #b2dfdb !important;
  background: white !important;
  transition: all 0.3s ease !important;
  border-radius: 12px !important;
}

.flat-card:hover {
  border-color: #4db6ac !important;
  transform: translateY(-4px) !important;
  box-shadow: none !important;
}

.flat-avatar {
  box-shadow: none !important;
}

.flat-action-icon {
  border-radius: 6px !important;
  transition: all 0.2s ease !important;
  min-width: auto !important;
  padding: 6px 8px !important;
  height: 32px !important;
}

.flat-action-icon :deep(.v-btn__content) {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 4px !important;
  height: 100% !important;
}

.flat-action-icon :deep(.v-icon) {
  margin: 0 !important;
  vertical-align: middle !important;
}

.flat-action-icon:hover {
  background: rgba(178, 223, 219, 0.15) !important;
  transform: scale(1.05) !important;
}

/* 投票动画效果 */
.vote-animation {
  animation: voteUp 0.3s ease;
}

@keyframes voteUp {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .roadmap-card .d-flex {
    flex-direction: column !important;
  }

  .roadmap-card .d-flex > div:last-child {
    width: 100% !important;
    min-width: 100% !important;
    height: 200px !important;
  }
}
</style>