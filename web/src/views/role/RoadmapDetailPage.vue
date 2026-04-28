<template>
  <DefaultLayout>
    <div class="roadmap-detail-page">
      <!-- 加载状态 -->
      <LoadingSpinner v-if="loading" />

      <div v-else-if="roadmap">
        <div class="content-layout pt-4 pt-md-5">
          <!-- 左侧：Header + 流程图 -->
          <div class="main-content">
            <!-- 职业信息 -->
            <div class="back-button-wrapper mb-2 mb-md-2 sticky-header">
              <div class="d-flex align-center justify-space-between">
                <div class="d-flex align-center">
                  <!-- 职业信息 -->
                  <div v-if="roadmap?.role" class="d-flex align-center">
                    <v-avatar
                      color="primary"
                      :size="$vuetify.display.mobile ? 36 : 40"
                      class="mr-3 flex-shrink-0"
                    >
                      <v-icon
                        icon="mdi-briefcase-outline"
                        color="white"
                        :size="$vuetify.display.mobile ? 18 : 20"
                      />
                    </v-avatar>
                    <span class="text-subtitle-1 text-md-h6 font-weight-bold text-grey-darken-4">
                      {{ roadmap.role.name }}
                    </span>
                  </div>
                </div>

                <!-- 点赞和收藏按钮 -->
                <div class="d-flex align-center gap-2">
                  <v-btn
                    :color="roadmap.learning ? 'success' : 'primary'"
                    :variant="roadmap.learning ? 'outlined' : 'flat'"
                    :size="$vuetify.display.mobile ? 'small' : 'default'"
                    rounded="lg"
                    @click="handleStartLearning"
                  >
                    <v-icon
                      :icon="roadmap.learning ? 'mdi-check' : 'mdi-play'"
                      size="18"
                      class="mr-1"
                    />
                    {{
                      roadmap.learning ? t('roadmapCard.learning') : t('roadmapCard.startLearning')
                    }}
                  </v-btn>

                  <!-- 分隔线 -->
                  <v-divider vertical class="mx-1 align-self-center" style="height: 28px" />

                  <v-btn
                    :color="roadmap.liked ? 'primary' : 'grey-darken-1'"
                    variant="tonal"
                    :size="$vuetify.display.mobile ? 'small' : 'default'"
                    rounded="lg"
                    @click="handleVote"
                  >
                    <v-icon
                      :icon="roadmap.liked ? 'mdi-heart' : 'mdi-heart-outline'"
                      size="18"
                      class="mr-1"
                    />
                    {{ roadmap.likeCount }}
                  </v-btn>
                  <v-tooltip location="bottom">
                    <template #activator="{ props }">
                      <v-btn
                        v-bind="props"
                        :icon="roadmap.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
                        :color="roadmap.bookmarked ? 'primary' : 'grey-darken-1'"
                        variant="tonal"
                        density="comfortable"
                        :size="$vuetify.display.mobile ? 'small' : 'default'"
                        rounded="lg"
                        @click="handleToggleBookmark"
                      />
                    </template>
                    {{
                      roadmap.bookmarked
                        ? t('roadmapDetail.unbookmark')
                        : t('roadmapDetail.bookmark')
                    }}
                  </v-tooltip>
                </div>
              </div>
            </div>

            <!-- 流程图 -->
            <v-card border rounded="xl" class="flow-card">
              <v-card-title class="pa-3 pa-sm-4 d-flex align-center justify-space-between">
                <span class="text-body-1 font-weight-bold ps-2">{{
                  t('roadmapDetail.learningPath')
                }}</span>
                <v-btn
                  color="grey-darken-2"
                  variant="outlined"
                  size="default"
                  rounded="lg"
                  @click="handleCopy"
                >
                  <v-icon icon="mdi-content-copy" size="18" class="mr-1" />
                  <span class="d-none d-sm-inline">{{ t('roadmapDetail.copyPath') }}</span>
                  <span class="d-sm-none">{{ t('common.copy') }}</span>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-0">
                <div class="vue-flow-container">
                  <VueFlow
                    :nodes="flowNodes"
                    :edges="flowEdges"
                    fit-view-on-init
                    :nodes-draggable="false"
                    :nodes-connectable="false"
                    :elements-selectable="false"
                    :min-zoom="0.7"
                    :max-zoom="1.2"
                    :default-zoom="1.0"
                    :zoom-on-scroll="true"
                    @node-click="handleNodeClick"
                    @nodes-initialized="onNodesInitialized"
                  >
                    <Background pattern-color="#bdbdbd" :gap="30" :size="2" variant="dots" />
                    <Controls :show-interactive="false" />
                  </VueFlow>
                </div>
              </v-card-text>
            </v-card>
          </div>

          <!-- 右侧：路径信息和评论区 -->
          <div class="right-sidebar pt-1">
            <!-- 路径信息卡片 -->
            <v-card rounded="0" class="roadmap-info-card mb-0 no-border">
              <v-card-text class="px-0 pt-0 pb-2 pb-sm-2">
                <!-- 标题和状态 -->
                <div class="d-flex align-center justify-space-between mb-4">
                  <div class="flex-grow-1">
                    <div class="d-flex align-center mb-2">
                      <v-chip
                        v-if="roadmap.pinned"
                        color="warning"
                        size="small"
                        variant="flat"
                        class="mr-2"
                      >
                        <v-icon icon="mdi-pin" size="14" class="mr-1" />
                        {{ t('roadmapCard.pin') }}
                      </v-chip>
                    </div>

                    <!-- 创建者信息 -->
                    <div class="d-flex align-center mb-3">
                      <UserAvatar
                        :name="roadmap.creator?.name || t('common.anonymous')"
                        :avatar-url="roadmap.creator?.avatar"
                        size="24"
                        rounded="lg"
                        class="mr-2"
                      />
                      <span class="text-body-2 text-grey-darken-3">
                        {{ roadmap.creator?.name }}
                      </span>
                      <span class="text-caption text-grey mx-2">·</span>
                      <span class="text-caption text-grey">
                        {{ getTimeDisplay(roadmap.createdAt) }}
                      </span>
                    </div>

                    <!-- 描述 -->
                    <div class="text-body-1 text-grey-darken-4 mb-3">
                      {{ roadmap.description }}
                    </div>

                    <!-- 统计信息 -->
                    <div class="d-flex flex-wrap align-center gap-4 gap-md-5">
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-account-group" size="18" color="grey" class="mr-2" />
                        <span class="text-body-2 text-grey-darken-2">
                          {{ roadmap.learnerCount ?? 0 }}
                          <span class="d-none d-sm-inline">{{ t('roadmapCard.learners') }}</span>
                        </span>
                      </div>
                      <div class="d-flex align-center d-none d-sm-flex">
                        <v-icon icon="mdi-comment-outline" size="18" color="grey" class="mr-2" />
                        <span class="text-body-2 text-grey-darken-2">
                          {{ roadmap.commentCount ?? 0 }} {{ t('roleDetail.comments') }}
                        </span>
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-graph-outline" size="18" color="grey" class="mr-2" />
                        <span class="text-body-2 text-grey-darken-2">
                          {{ roadmap.nodeCount }}
                          <span class="d-none d-sm-inline">{{ t('roleDetail.nodeCountUnit') }}</span
                          >{{ t('roadmap.nodes') }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </v-card-text>
            </v-card>

            <!-- 评论区 -->
            <CommentSection
              :post-id="roadmapId"
              :object-type="ObjectType.ROADMAP"
              :comment-count="roadmap.commentCount"
              :target-comment-id="targetCommentId"
              :target-sub-comment-id="targetSubCommentId"
            />
          </div>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { Position } from '@vue-flow/core'
import type { Node, Edge } from '@vue-flow/core'
import dagre from 'dagre'
import { useRoadmapDetailQuery } from '@/queries/roadmap'
import { useUpvoteMutation, useBookmarkToggleMutation } from '@/queries/interaction'
import { useStartRoadmapMutation, useCancelRoadmapMutation } from '@/queries/progress'
import { ObjectType, VoteType } from '@/enums'
import type { Roadmap } from '@/types/roadmap'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import CommentSection from '@/components/common/CommentSection.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useI18n } from '@/composables/useI18n'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()

// 使用 useVueFlow 获取节点实际尺寸
const { getNodes, setNodes, fitView } = useVueFlow()

// 是否已完成基于实际尺寸的布局
const layoutApplied = ref(false)

// 从路由获取参数
const roleId = computed(() => {
  // roleId 不再从路由获取，从 roadmap 数据中获取
  return roadmap.value?.roleId || 0
})
const roadmapId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : 0
})

// 目标评论ID（从 URL 获取）
const targetCommentId = computed(() => {
  if (route.query.commentId) {
    return Number(route.query.commentId)
  }
  return null
})

// 目标子评论ID（从 URL 获取）
const targetSubCommentId = computed(() => {
  if (route.query.subCommentId) {
    return Number(route.query.subCommentId)
  }
  return null
})

// 加载路径详情
const {
  data: roadmapData,
  isLoading: loading,
  error: fetchError,
} = useRoadmapDetailQuery(roadmapId)

// 加载失败时跳转到 404
watch(fetchError, (err) => {
  if (err) {
    router.replace({ path: '/error/404', state: { message: t('roadmapDetail.notFound') } })
  }
}, { immediate: true })

const roadmap = computed(() => roadmapData.value)

/**
 * 解析路线图内容
 */
const parseContent = (content: string | object): { nodes: Node[]; edges: Edge[] } => {
  try {
    const data = typeof content === 'string' ? JSON.parse(content) : content
    const isLearning = roadmap.value?.learning || false // 用户是否正在学习这个路线图

    const nodes = (data.nodes || []).map((node: any): Node => {
      // 如果是根节点（id=0 或 "0"），设置为职业名称
      const nodeId = String(node.id)
      if (nodeId === '0') {
        return {
          id: nodeId,
          type: 'default',
          data: {
            label: roadmap.value?.role?.name || t('roadmapDetail.currentRole'),
            ...node.data,
          },
          position: node.position || { x: 0, y: 0 },
          sourcePosition: undefined,
          targetPosition: Position.Left,
          style: {
            background: '#616161',
            color: '#ffffff',
            border: '2px solid #9e9e9e',
            borderRadius: '12px',
            padding: '10px',
            fontWeight: '600',
            fontSize: '14px',
          },
        }
      }

      // 提取进度信息
      const completed = node.finished || node.completed || false
      const progress = Number(node.progress) || 0
      const isCourseRoot = node.isCourseRoot || false

      // 如果用户正在学习，即使进度为0也显示进度条样式
      const shouldShowProgress = isLearning && !completed

      // 构建显示的标签（包含进度百分比和类型标识）
      let displayLabel = node.name

      // 添加类型标识
      if (isCourseRoot) {
        displayLabel = `${t('roadmapDetail.courseLabel')} ${displayLabel}`
      } else {
        displayLabel = `${t('roadmapDetail.nodeLabel')} ${displayLabel}`
      }

      // 添加进度百分比
      if (shouldShowProgress) {
        displayLabel = `${displayLabel} (${Math.round(progress)}%)`
      }

      // 根据是否为课程根节点选择不同的样式
      const nodeStyle = isCourseRoot
        ? {
            background: '#fafafa',
            color: '#424242',
            border: '2px solid #bdbdbd',
            borderRadius: '12px',
            padding: '10px',
            fontWeight: '500',
            fontSize: '13px',
          }
        : {
            background: '#f1f8e9',
            color: '#33691e',
            border: '2px solid #aed581',
            borderRadius: '12px',
            padding: '10px',
            fontWeight: '500',
            fontSize: '13px',
          }

      return {
        id: nodeId,
        type: 'default',
        data: {
          label: displayLabel,
          isCourseRoot: isCourseRoot,
          courseId: node.courseId,
          progress: progress,
          ...node.data,
        },
        position: node.position || { x: 0, y: 0 },
        sourcePosition: Position.Right,
        targetPosition: Position.Left,
        class: completed ? 'completed-course' : shouldShowProgress ? 'progress-course' : '',
        style: {
          ...nodeStyle,
          ...(shouldShowProgress ? { '--progress': `${progress}%` } : {}),
        },
      }
    })

    const edges = (data.edges || []).map(
      (edge: any): Edge => ({
        id: `${edge.source}-${edge.target}`,
        source: edge.source.toString(),
        target: edge.target.toString(),
        type: 'default',
        animated: true,
        style: {
          stroke: '#78909c',
          strokeWidth: 2,
        },
      })
    )

    return { nodes, edges }
  } catch (error) {
    console.error('解析路线图内容失败:', error)
    return { nodes: [], edges: [] }
  }
}

// 自动布局函数
const applyAutoLayout = (
  nodeList: Node[],
  edgeList: Edge[],
  direction = 'LR',
  nodeSizes?: Map<string, { width: number; height: number }>
): Node[] => {
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({
    rankdir: direction,
    nodesep: 25, // 上下间距
    ranksep: 150, // 左右间距
    marginx: 20,
    marginy: 20,
  })

  const defaultNodeWidth = 120
  const defaultNodeHeight = 40

  // 添加节点和边到 dagre 图
  nodeList.forEach((node) => {
    const size = nodeSizes?.get(node.id)
    const width = size?.width || defaultNodeWidth
    const height = size?.height || defaultNodeHeight
    dagreGraph.setNode(node.id.toString(), { width, height })
  })
  edgeList.forEach((edge) => {
    dagreGraph.setEdge(edge.source.toString(), edge.target.toString())
  })

  // 计算布局
  dagre.layout(dagreGraph)

  // 更新节点位置
  return nodeList.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id.toString())
    const size = nodeSizes?.get(node.id)
    const width = size?.width || defaultNodeWidth
    const height = size?.height || defaultNodeHeight
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - width / 2,
        y: nodeWithPosition.y - height / 2,
      },
    }
  })
}

// 准备 Vue Flow 的节点和边
const flowNodes = computed<Node[]>(() => {
  if (!roadmap.value?.content) return []

  const { nodes, edges } = parseContent(roadmap.value.content)

  // 应用自动布局 - 使用 LR (Left to Right) 让叶子节点在左边，根节点在右边
  return applyAutoLayout(nodes, edges, 'LR')
})

const flowEdges = computed<Edge[]>(() => {
  if (!roadmap.value?.content) return []

  const { edges } = parseContent(roadmap.value.content)
  return edges
})

// 节点初始化完成后，使用实际尺寸重新布局
const onNodesInitialized = () => {
  if (layoutApplied.value) return

  // 获取所有节点的实际尺寸
  const currentNodes = getNodes.value
  if (currentNodes.length === 0) return

  // 构建尺寸映射
  const nodeSizes = new Map<string, { width: number; height: number }>()
  let hasValidDimensions = false

  currentNodes.forEach((node) => {
    if (node.dimensions?.width && node.dimensions?.height) {
      nodeSizes.set(node.id, {
        width: node.dimensions.width,
        height: node.dimensions.height,
      })
      hasValidDimensions = true
    }
  })

  // 如果有有效的尺寸数据，重新计算布局
  if (hasValidDimensions && roadmap.value?.content) {
    const { nodes, edges } = parseContent(roadmap.value.content)
    const layoutedNodes = applyAutoLayout(nodes, edges, 'LR', nodeSizes)

    // 更新节点位置
    setNodes(layoutedNodes)
    layoutApplied.value = true

    // 重新适配视图
    nextTick(() => {
      fitView({ padding: 0.2 })
    })
  }
}

// 投票
const { mutate: upvoteMutate } = useUpvoteMutation()

const handleVote = (): void => {
  if (!roadmap.value) return
  upvoteMutate(
    { objectId: roadmapId.value, objectType: ObjectType.ROADMAP, type: VoteType.LIKE },
    {
      onSuccess: (result) => {
        if (result && roadmap.value) {
          roadmap.value.liked = result.liked || false
          roadmap.value.likeCount = result.likeCount || 0
        }
      },
    }
  )
}

// 开始/取消学习
const { mutate: startRoadmap } = useStartRoadmapMutation()
const { mutate: cancelRoadmap } = useCancelRoadmapMutation()

const handleStartLearning = (): void => {
  if (!roadmap.value) return

  if (roadmap.value.learning) {
    cancelRoadmap(roadmapId.value, {
      onSuccess: (result) => {
        if (result && roadmap.value) roadmap.value.learning = result.learning
      },
    })
  } else {
    startRoadmap(roadmapId.value, {
      onSuccess: (result) => {
        if (result && roadmap.value) roadmap.value.learning = result.learning
      },
    })
  }
}

// 复制路径
const handleCopy = (): void => {
  void router.push(`/role/${roleId.value}/roadmap/create?copy=${roadmapId.value}`)
}

// 切换收藏状态
const { mutate: bookmarkMutate, isPending: bookmarking } = useBookmarkToggleMutation()

const handleToggleBookmark = () => {
  if (!roadmap.value) return
  bookmarkMutate(
    { contentType: 'roadmap', contentId: roadmapId.value },
    {
      onSuccess: (result) => {
        if (result !== null && roadmap.value) {
          roadmap.value.bookmarked = result
        }
      },
    }
  )
}

// 获取时间显示
const getTimeDisplay = (date: string | undefined): string => {
  if (!date) return ''
  const now = new Date()
  const created = new Date(date)
  const days = Math.floor((now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24))

  if (days === 0) return t('roleDetail.today')
  if (days === 1) return t('roleDetail.yesterday')
  if (days < 7) return t('roleDetail.daysAgo', { days })
  if (days < 30) return t('roleDetail.weeksAgo', { weeks: Math.floor(days / 7) })
  return date
}

// 节点点击
const handleNodeClick = ({ node }: { node: Node }): void => {
  // 根节点不处理
  if (node.id === '0') return

  // 根据节点类型跳转不同的页面
  const nodeData = node.data
  if (nodeData.isCourseRoot && nodeData.courseId) {
    // 课程根节点：跳转课程页面
    window.open(`/read?courseId=${nodeData.courseId}`, '_blank')
  } else {
    // 普通节点：跳转节点页面
    window.open(`/read?nodeId=${node.id}`, '_blank')
  }
}
</script>

<style scoped>
.roadmap-detail-page {
  /* 使用 DefaultLayout 的默认 padding */
}

/* 内容布局 */
.content-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

@media (min-width: 1280px) {
  .content-layout {
    flex-direction: row;
    gap: 48px;
    align-items: flex-start;
  }
}

.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 左侧 header 和流程图 - 整体 sticky */
@media (min-width: 1280px) {
  .main-content {
    position: sticky;
    top: 76px;
    align-self: flex-start;
    height: calc(100vh - 96px);
    display: flex;
    flex-direction: column;
  }

  .sticky-header {
    flex-shrink: 0;
    background: rgb(var(--v-theme-background));
    padding-bottom: 16px;
  }

  .flow-card {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  .flow-card .v-card-text {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }
}

/* 右侧评论区 */
.right-sidebar {
  width: 100%;
}

@media (min-width: 1280px) {
  .right-sidebar {
    width: 360px;
    flex-shrink: 0;
  }
}

.roadmap-info-card,
.flow-card {
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-outline));
}

.vue-flow-container {
  height: 600px;
  background: rgb(var(--v-theme-surface));
  position: relative;
}

@media (min-width: 960px) {
  .vue-flow-container {
    height: 800px;
  }
}

@media (min-width: 1280px) {
  .vue-flow-container {
    height: 100%;
    min-height: 0;
  }
}

.gap-3 {
  gap: 12px;
}

.gap-4 {
  gap: 16px;
}

.gap-2 {
  gap: 8px;
}

/* Vue Flow 节点样式 */
:deep(.vue-flow__node) {
  cursor: pointer;
  transition: all 0.2s ease;
}

:deep(.vue-flow__node:hover) {
  transform: scale(1.05);
}

/* 根节点样式 */
:deep(.vue-flow__node[data-id='0']) {
  cursor: default !important;
}

:deep(.vue-flow__node[data-id='0']:hover) {
  transform: none;
}

/* 隐藏连接点 */
:deep(.vue-flow__handle) {
  width: 0 !important;
  height: 0 !important;
  border: none !important;
  background: transparent !important;
}

/* 已完成课程样式 */
:deep(.vue-flow__node.completed-course) {
  background: #e8f5e9 !important;
  border-color: #4caf50 !important;
  color: #2e7d32 !important;
}

/* 已完成课程的勾选标记 */
:deep(.vue-flow__node.completed-course::after) {
  content: '✓';
  position: absolute;
  top: -6px;
  right: -6px;
  width: 16px;
  height: 16px;
  background: #4caf50;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: bold;
  border: 2px solid white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

/* 进行中课程的进度条 */
:deep(.vue-flow__node.progress-course) {
  background: linear-gradient(
    to right,
    #beffb4 var(--progress, 0%),
    #fafafa var(--progress, 0%)
  ) !important;
  border-color: #81c784 !important;
}

/* 隐藏根节点的 source handle */
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle.source),
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle-right),
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle-bottom) {
  display: none !important;
}
</style>
