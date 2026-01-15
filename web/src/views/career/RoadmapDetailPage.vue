<template>
  <DefaultLayout>
    <div class="roadmap-detail-page">
      <!-- 返回按钮和职业信息 -->
      <div class="back-button-wrapper mb-6 mb-md-8">
        <div class="d-flex align-center">
          <v-btn
            icon="mdi-arrow-left"
            variant="flat"
            color="grey-lighten-5"
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            class="back-button mr-3 mr-md-4 flex-shrink-0"
            @click="handleBack"
          ></v-btn>

          <!-- 职业信息 -->
          <div v-if="roadmap?.profession" class="d-flex align-center">
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
              {{ roadmap.profession.name }}
            </span>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <LoadingSpinner v-if="loading" />

      <div v-else-if="roadmap" class="content-layout">
        <!-- 左侧：流程图 -->
        <div class="main-content">
          <!-- 流程图 -->
          <v-card border rounded="xl" class="flow-card">
            <v-card-title class="pa-4 pa-sm-6 d-flex align-center justify-space-between">
              <span class="text-body-1 font-weight-bold">学习路线图</span>
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
                  :zoom-on-scroll="false"
                  @node-click="handleNodeClick"
                >
                  <Background pattern-color="#bdbdbd" :gap="30" :size="2" variant="dots" />
                  <Controls :show-interactive="false" />
                </VueFlow>
              </div>
            </v-card-text>
          </v-card>
        </div>

        <!-- 右侧：路径信息和评论区 -->
        <div class="right-sidebar">
          <!-- 路径信息卡片 -->
          <v-card rounded="xl" class="roadmap-info-card mb-4 no-border">
            <v-card-text class="px-0 pt-0 pb-4 pb-sm-5">
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
                      置顶
                    </v-chip>
                    <v-chip v-if="roadmap.learning" color="success" size="small" variant="flat">
                      <v-icon icon="mdi-school" size="14" class="mr-1" />
                      学习中
                    </v-chip>
                  </div>

                  <!-- 创建者信息 -->
                  <div class="d-flex align-center mb-3">
                    <v-avatar size="24" color="grey-lighten-2" class="mr-2">
                      <v-icon
                        v-if="!roadmap.creator?.avatar"
                        icon="mdi-account"
                        color="grey"
                        size="14"
                      />
                    </v-avatar>
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
                        <span class="d-none d-sm-inline">人学习</span>
                      </span>
                    </div>
                    <div class="d-flex align-center d-none d-sm-flex">
                      <v-icon icon="mdi-comment-outline" size="18" color="grey" class="mr-2" />
                      <span class="text-body-2 text-grey-darken-2">
                        {{ roadmap.commentCount ?? 0 }} 评论
                      </span>
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-graph-outline" size="18" color="grey" class="mr-2" />
                      <span class="text-body-2 text-grey-darken-2">
                        {{ roadmap.nodeCount }}
                        <span class="d-none d-sm-inline">个</span>节点
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="d-flex flex-wrap align-center gap-2">
                <v-btn
                  :color="roadmap.upvoted ? 'primary' : 'grey-darken-2'"
                  :variant="roadmap.upvoted ? 'flat' : 'outlined'"
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  rounded="lg"
                  @click="handleVote"
                >
                  <v-icon
                    :icon="roadmap.upvoted ? 'mdi-heart' : 'mdi-heart-outline'"
                    size="18"
                    class="mr-1"
                  />
                  {{ roadmap.vote }}
                </v-btn>
                <v-btn
                  :color="roadmap.learning ? 'success' : 'primary'"
                  :variant="roadmap.learning ? 'outlined' : 'flat'"
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  rounded="lg"
                  class="mr-3"
                  @click="handleStartLearning"
                >
                  <v-icon
                    :icon="roadmap.learning ? 'mdi-check' : 'mdi-play'"
                    size="18"
                    class="mr-1"
                  />
                  {{ roadmap.learning ? '正在学习' : '开始学习' }}
                </v-btn>
                <v-btn
                  color="grey-darken-2"
                  variant="outlined"
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  rounded="lg"
                  @click="handleCopy"
                >
                  <v-icon icon="mdi-content-copy" size="18" class="mr-1" />
                  <span class="d-none d-sm-inline">复制路径</span>
                  <span class="d-sm-none">复制</span>
                </v-btn>
              </div>
            </v-card-text>
          </v-card>

          <!-- 评论区 -->
          <CommentSection
            :post-id="roadmapId"
            :object-type="ObjectType.ROADMAP"
            :comment-count="roadmap.commentCount"
          />
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { Position } from '@vue-flow/core'
import type { Node, Edge } from '@vue-flow/core'
import dagre from 'dagre'
import { useFetch, useMutation } from '@/composables'
import { roadmapApi, progressApi } from '@/api'
import { ObjectType } from '@/enums'
import type { Roadmap } from '@/types/roadmap'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import CommentSection from '@/components/common/CommentSection.vue'

const router = useRouter()
const route = useRoute()

// 从路由获取参数
const careerId = computed(() => {
  // careerId 不再从路由获取，从 roadmap 数据中获取
  return roadmap.value?.professionId || 0
})
const roadmapId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : 0
})

// 使用 useFetch 加载路径详情
const {
  data: roadmapData,
  loading,
  error: fetchError,
} = useFetch<Roadmap>({
  fetchFn: () => roadmapApi.getRoadmap(roadmapId.value),
  immediate: true,
  defaultValue: null,
})

const roadmap = computed(() => roadmapData.value)

/**
 * 解析路线图内容
 */
const parseContent = (content: string | object): { nodes: Node[]; edges: Edge[] } => {
  try {
    const data = typeof content === 'string' ? JSON.parse(content) : content
    const nodes = (data.nodes || []).map((node: any): Node => {
      // 如果是根节点（id=0），设置为职业名称
      if (node.id === 0 || node.id === '0') {
        return {
          id: node.id.toString(),
          type: 'default',
          data: {
            label: roadmap.value?.profession?.name || '当前职业',
            ...node.data,
          },
          position: node.position || { x: 0, y: 0 },
          sourcePosition: Position.Top,
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
        }
      }

      return {
        id: node.id.toString(),
        type: 'default',
        data: {
          label: node.name,
          ...node.data,
        },
        position: node.position || { x: 0, y: 0 },
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
const applyAutoLayout = (nodeList: Node[], edgeList: Edge[], direction = 'TB'): Node[] => {
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({
    rankdir: direction,
    nodesep: 150,
    ranksep: 50,
    marginx: 20,
    marginy: 20,
  })

  const nodeWidth = 120
  const nodeHeight = 40

  // 添加节点和边到 dagre 图
  nodeList.forEach((node) => {
    dagreGraph.setNode(node.id.toString(), { width: nodeWidth, height: nodeHeight })
  })
  edgeList.forEach((edge) => {
    dagreGraph.setEdge(edge.source.toString(), edge.target.toString())
  })

  // 计算布局
  dagre.layout(dagreGraph)

  // 更新节点位置
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

// 准备 Vue Flow 的节点和边
const flowNodes = computed<Node[]>(() => {
  if (!roadmap.value?.content) return []

  const { nodes, edges } = parseContent(roadmap.value.content)

  // 应用自动布局 - 使用 BT (Bottom to Top) 让根节点在上面
  return applyAutoLayout(nodes, edges, 'BT')
})

const flowEdges = computed<Edge[]>(() => {
  if (!roadmap.value?.content) return []

  const { edges } = parseContent(roadmap.value.content)
  return edges
})

// 返回上一页
const handleBack = (): void => {
  router.back()
}

// 投票
const handleVote = (): void => {
  roadmap.value.upvoted = !roadmap.value.upvoted
  roadmap.value.vote += roadmap.value.upvoted ? 1 : -1
}

// 开始学习路线图的 mutation
const { execute: startRoadmapMutation } = useMutation(progressApi.startRoadmap, {
  successMessage: '已开始学习',
  showToast: true,
})

// 取消学习路线图的 mutation
const { execute: cancelRoadmapMutation } = useMutation(progressApi.cancelRoadmap, {
  successMessage: '已取消学习',
  showToast: true,
})

// 开始学习
const handleStartLearning = async (): Promise<void> => {
  if (!roadmap.value) return

  try {
    if (roadmap.value.learning) {
      // 取消学习
      const result = await cancelRoadmapMutation(roadmapId.value)
      if (result) {
        roadmap.value.learning = result.learning
      }
    } else {
      // 开始学习
      const result = await startRoadmapMutation(roadmapId.value)
      if (result) {
        roadmap.value.learning = result.learning
      }
    }
  } catch (error) {
    console.error('操作失败:', error)
  }
}

// 复制路径
const handleCopy = (): void => {
  void router.push(`/career/${careerId.value}/roadmap/create?copy=${roadmapId.value}`)
}

// 获取时间显示
const getTimeDisplay = (date: string): string => {
  const now = new Date()
  const created = new Date(date)
  const days = Math.floor((now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return date
}

// 节点点击
const handleNodeClick = ({ node }: { node: Node }): void => {
  // 根节点不处理
  if (node.id === '0') return

  // 可以跳转到课程详情
  console.log('点击节点:', node.data.label)
}
</script>

<style scoped>
.roadmap-detail-page {
  /* 使用 DefaultLayout 的默认 padding */
}

/* 宽屏时向左延伸，让后退按钮露出到页面外 */
@media (min-width: 1800px) {
  .back-button-wrapper {
    margin-left: -56px;
  }
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
    height: calc(100vh - 56px - 40px - 80px - 20px);
  }
}

.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

@media (min-width: 1280px) {
  .flow-card {
    display: flex;
    flex-direction: column;
    flex: 1;
  }

  .flow-card .v-card-text {
    flex: 1;
    display: flex;
    flex-direction: column;
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
    min-height: 600px;
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
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
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
</style>
