<template>
  <DefaultLayout>
    <v-container fluid class="roadmap-detail-page">
      <!-- 返回按钮 -->
      <v-btn variant="text" color="grey-darken-2" class="mb-4" @click="handleBack">
        <v-icon icon="mdi-arrow-left" class="mr-1" />
        返回路径列表
      </v-btn>

      <!-- 加载状态 -->
      <v-row v-if="loading">
        <v-col cols="12" class="text-center py-12">
          <v-progress-circular indeterminate color="primary" size="64" />
          <p class="text-body-2 text-grey mt-4">加载中...</p>
        </v-col>
      </v-row>

      <v-row v-else-if="roadmap" class="ma-0">
        <!-- 左侧：路径信息和流程图 -->
        <v-col cols="12" lg="8" class="pa-0">
          <!-- 路径信息卡片 -->
          <v-card rounded="xl" class="roadmap-info-card mb-4 no-border">
            <v-card-text class="pa-6">
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
                      <v-icon v-if="!roadmap.creator?.avatar" icon="mdi-account" color="grey" size="14" />
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
                  <div class="d-flex align-center gap-4">
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-account-group" size="18" color="grey" class="mr-1" />
                      <span class="text-body-2 text-grey-darken-2">
                        {{ roadmap.learnerCount ?? 0 }} 人学习
                      </span>
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-comment-outline" size="18" color="grey" class="mr-1" />
                      <span class="text-body-2 text-grey-darken-2">
                        {{ roadmap.commentCount ?? 0 }} 评论
                      </span>
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-circle-medium" size="18" color="grey" class="mr-1" />
                      <span class="text-body-2 text-grey-darken-2">
                        {{ roadmap.nodeCount }} 个节点
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="d-flex align-center gap-2">
                <v-btn
                  :color="roadmap.upvoted ? 'primary' : 'grey-darken-2'"
                  :variant="roadmap.upvoted ? 'flat' : 'outlined'"
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
                  rounded="lg"
                  @click="handleStartLearning"
                >
                  <v-icon
                    :icon="roadmap.learning ? 'mdi-check' : 'mdi-play'"
                    size="18"
                    class="mr-1"
                  />
                  {{ roadmap.learning ? '正在学习' : '开始学习' }}
                </v-btn>
                <v-btn color="grey-darken-2" variant="outlined" rounded="lg" @click="handleCopy">
                  <v-icon icon="mdi-content-copy" size="18" class="mr-1" />
                  复制路径
                </v-btn>
              </div>
            </v-card-text>
          </v-card>

          <!-- 流程图 -->
          <v-card border rounded="xl" class="flow-card">
            <v-card-title class="pa-4 d-flex align-center justify-space-between">
              <span class="text-body-1">学习路线图</span>
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
                  :min-zoom="0.9"
                  :max-zoom="1.2"
                  :default-zoom="1.2"
                  @node-click="handleNodeClick"
                >
                  <Background pattern-color="#bdbdbd" :gap="30" :size="2" variant="dots" />
                  <Controls :show-zoom="false" />
                </VueFlow>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 右侧：评论区 -->
        <v-col cols="12" lg="4" class="pa-0 pl-6">
          <v-card rounded="xl" class="comments-card sticky-card no-border">
            <v-card-title class="pa-6 pb-4">
              <div class="d-flex align-center">
                <v-icon icon="mdi-comment-multiple" color="primary" class="mr-2" />
                <span class="text-h6 font-weight-bold">评论</span>
                <v-chip size="small" color="primary" variant="tonal" class="ml-2">
                  {{ comments.length }}
                </v-chip>
              </div>
            </v-card-title>

            <!-- 发表评论 -->
            <v-card-text class="pa-6 pt-0 pb-4">
              <v-textarea
                v-model="newComment"
                placeholder="写下你的评论..."
                variant="outlined"
                rows="3"
                hide-details
                class="mb-3"
              />
              <v-btn
                color="primary"
                variant="flat"
                rounded="lg"
                :disabled="!newComment.trim() || submittingComment"
                :loading="submittingComment"
                @click="submitComment"
              >
                <v-icon icon="mdi-send" size="18" class="mr-1" />
                发表评论
              </v-btn>
            </v-card-text>

            <!-- 评论列表 -->
            <v-card-text class="pa-6 pt-4 comments-list">
              <div v-for="comment in comments" :key="comment.id" class="comment-item mb-4">
                <div class="d-flex">
                  <v-avatar size="36" color="grey-lighten-2" class="mr-3">
                    <v-icon icon="mdi-account" color="grey" size="20" />
                  </v-avatar>
                  <div class="flex-grow-1">
                    <div class="d-flex align-center justify-space-between mb-1">
                      <span class="text-body-2 font-weight-medium text-grey-darken-3">
                        {{ comment.creatorName }}
                      </span>
                      <span class="text-caption text-grey">
                        {{ getTimeDisplay(comment.createdAt) }}
                      </span>
                    </div>
                    <p class="text-body-2 text-grey-darken-2 mb-2">
                      {{ comment.content }}
                    </p>
                    <v-btn
                      size="x-small"
                      variant="tonal"
                      color="grey"
                      @click="likeComment(comment)"
                    >
                      <v-icon icon="mdi-thumb-up-outline" size="14" class="mr-1" />
                      <span class="text-caption">{{ comment.upvoteCount }}</span>
                    </v-btn>
                  </div>
                </div>
              </div>

              <div v-if="comments.length === 0" class="text-center py-8">
                <v-icon icon="mdi-comment-outline" size="48" color="grey-lighten-2" class="mb-2" />
                <p class="text-body-2 text-grey">还没有评论</p>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
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
import { roadmapApi, commentApi } from '@/api'
import { ObjectType } from '@/enums'
import type { Roadmap } from '@/types/roadmap'
import type { Comment } from '@/types/comment'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'

const router = useRouter()
const route = useRoute()

// 从路由获取参数
const careerId = computed(() => {
  const id = route.params.careerId
  return typeof id === 'string' ? parseInt(id, 10) : 0
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

// 使用 useFetch 加载评论列表
const {
  data: commentsData,
  loading: loadingComments,
  refresh: refreshComments,
} = useFetch<Comment[]>({
  fetchFn: () => commentApi.getComments(roadmapId.value, ObjectType.ROADMAP),
  immediate: true,
  defaultValue: [],
})

const comments = computed(() => commentsData.value ?? [])
const newComment = ref('')

// 使用 useMutation 创建评论
const { execute: executeCreateComment, loading: submittingComment } = useMutation(
  (content: string) =>
    commentApi.createComment(roadmapId.value, ObjectType.ROADMAP, null, null, content),
  {
    successMessage: '评论发表成功',
    onSuccess: () => {
      newComment.value = ''
      void refreshComments()
    },
  }
)

// 发表评论
const submitComment = async (): Promise<void> => {
  if (!newComment.value.trim()) return
  await executeCreateComment(newComment.value.trim())
}

/**
 * 解析路线图内容
 */
const parseContent = (
  content: string | object
): { nodes: Node[]; edges: Edge[] } => {
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

    const edges = (data.edges || []).map((edge: any): Edge => ({
      id: `${edge.source}-${edge.target}`,
      source: edge.source.toString(),
      target: edge.target.toString(),
      type: 'default',
      animated: true,
      style: {
        stroke: '#78909c',
        strokeWidth: 2,
      },
    }))

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

// 返回列表
const handleBack = (): void => {
  void router.push(`/career/${careerId.value}`)
}

// 投票
const handleVote = (): void => {
  roadmap.value.upvoted = !roadmap.value.upvoted
  roadmap.value.vote += roadmap.value.upvoted ? 1 : -1
}

// 开始学习
const handleStartLearning = (): void => {
  roadmap.value.learning = !roadmap.value.learning
}

// 复制路径
const handleCopy = (): void => {
  void router.push(`/career/${careerId.value}/roadmap/create?copy=${roadmapId.value}`)
}

// 点赞评论
const likeComment = (comment: Comment): void => {
  comment.upvoteCount = (comment.upvoteCount || 0) + 1
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
  padding-top: 0;
  padding-bottom: 48px;
}

.roadmap-info-card,
.flow-card,
.comments-card {
  background-color: #ffffff;
  border: 1px solid #edeff1;
}

.flow-card {
  min-height: 1000px;
}

.vue-flow-container {
  height: 1000px;
  background: #ffffff;
  position: relative;
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

.sticky-card {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.comments-list {
  overflow-y: auto;
  flex: 1;
}

.comments-list::-webkit-scrollbar {
  width: 4px;
}

.comments-list::-webkit-scrollbar-track {
  background: transparent;
}

.comments-list::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.comments-list::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

.comment-item {
  padding-bottom: 16px;
  border-bottom: 1px solid #edeff1;
}

.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.gap-4 {
  gap: 16px;
}

.gap-2 {
  gap: 8px;
}

/* 移动端 */
@media (max-width: 1280px) {
  .sticky-card {
    position: static;
    max-height: none;
  }

  .vue-flow-container {
    height: 400px;
  }
}
</style>
