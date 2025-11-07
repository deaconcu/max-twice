<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { Position } from '@vue-flow/core'
import type { Node, Edge } from '@vue-flow/core'
import dagre from 'dagre'
import AppHeader from '@/components/layout/AppHeader.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'

const router = useRouter()
const route = useRoute()

// 从路由获取参数
const professionId = ref(Number(route.params.professionId) || 1)
const roadmapId = ref(Number(route.params.roadmapId) || 1)

// 状态管理
const loading = ref(true)

// Mock 数据 - 路径详情
const roadmap = ref({
  id: 1,
  professionId: 1,
  description: 'Vue 3 + TypeScript 全栈开发路线',
  creator: {
    id: 1,
    username: 'techmaster',
    avatar: null
  },
  vote: 156,
  upvoted: true,
  comment: 23,
  pinned: true,
  learning: true,
  learners: 450,
  state: 1,
  createdAt: '2024-01-15',
  updatedAt: '2024-01-20',
  nodeCount: 12,
  nodes: [
    { id: '1', name: 'HTML 基础', description: '学习HTML基础语法和常用标签' },
    { id: '2', name: 'CSS 基础', description: '学习CSS样式和布局' },
    { id: '3', name: 'JavaScript 基础', description: 'JavaScript核心语法' },
    { id: '4', name: 'Vue 3 基础', description: 'Vue 3组合式API' },
    { id: '5', name: 'TypeScript', description: 'TypeScript类型系统' },
    { id: '6', name: 'Pinia 状态管理', description: '学习Pinia状态管理' },
    { id: '7', name: 'Vue Router', description: 'Vue路由管理' },
    { id: '8', name: 'Vite 构建工具', description: '使用Vite构建项目' },
    { id: '9', name: 'Node.js 基础', description: 'Node.js服务端开发' },
    { id: '10', name: 'Express 框架', description: 'Express Web框架' },
    { id: '11', name: '数据库 MySQL', description: 'MySQL数据库操作' },
    { id: '12', name: '项目实战', description: '完整项目开发实战' }
  ],
  edges: [
    { source: '0', target: '1' },
    { source: '1', target: '2' },
    { source: '2', target: '3' },
    { source: '3', target: '4' },
    { source: '3', target: '5' },
    { source: '4', target: '6' },
    { source: '4', target: '7' },
    { source: '5', target: '8' },
    { source: '6', target: '9' },
    { source: '7', target: '9' },
    { source: '8', target: '9' },
    { source: '9', target: '10' },
    { source: '10', target: '11' },
    { source: '11', target: '12' }
  ]
})

// Mock 数据 - 评论列表
const comments = ref([
  {
    id: 1,
    user: {
      username: 'student01',
      avatar: null
    },
    content: '这个学习路径非常清晰,按照这个路线学习进步很快!',
    createdAt: '2024-01-20',
    likes: 12
  },
  {
    id: 2,
    user: {
      username: 'developer',
      avatar: null
    },
    content: '建议增加一些项目实战的内容,这样学习效果会更好。',
    createdAt: '2024-01-19',
    likes: 8
  },
  {
    id: 3,
    user: {
      username: 'frontender',
      avatar: null
    },
    content: '跟着这个路线学了一个月,现在已经可以独立开发项目了!',
    createdAt: '2024-01-18',
    likes: 15
  }
])

const newComment = ref('')

// 自动布局函数
const applyAutoLayout = (nodeList: Node[], edgeList: Edge[], direction = 'TB'): Node[] => {
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({
    rankdir: direction,
    nodesep: 150,
    ranksep: 50, // 缩短线的长度,从100改为50
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
  // 创建根节点
  const nodes: Node[] = [
    {
      id: '0',
      type: 'default',
      data: {
        label: '前端工程师',
      },
      position: { x: 0, y: 0 },
      sourcePosition: Position.Bottom,
      style: {
        background: '#616161',
        color: '#ffffff',
        border: '2px solid #9e9e9e',
        borderRadius: '12px',
        padding: '10px',
        fontWeight: '600',
        fontSize: '14px'
      }
    }
  ]

  // 添加学习节点
  roadmap.value.nodes.forEach((node) => {
    nodes.push({
      id: node.id,
      type: 'default',
      data: {
        label: node.name,
      },
      position: { x: 0, y: 0 },
      sourcePosition: Position.Bottom,
      targetPosition: Position.Top,
      style: {
        background: '#fafafa',
        color: '#424242',
        border: '2px solid #bdbdbd',
        borderRadius: '12px',
        padding: '10px',
        fontWeight: '500',
        fontSize: '13px'
      }
    })
  })

  // 应用自动布局
  const edges: Edge[] = roadmap.value.edges.map((edge) => ({
    id: `${edge.source}-${edge.target}`,
    source: edge.source,
    target: edge.target,
    type: 'default',
    animated: true,
    style: {
      stroke: '#78909c',
      strokeWidth: 2
    }
  }))

  return applyAutoLayout(nodes, edges, 'TB')
})

const flowEdges = computed<Edge[]>(() => {
  return roadmap.value.edges.map((edge) => ({
    id: `${edge.source}-${edge.target}`,
    source: edge.source,
    target: edge.target,
    type: 'default', // 改用 default 类型会自动使用贝塞尔曲线
    animated: true,
    style: {
      stroke: '#78909c',
      strokeWidth: 2
    }
  }))
})

// 返回上一页
const backToList = (): void => {
  router.back()
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
  router.push(`/roadmap/${professionId.value}/create?copy=${roadmapId.value}`)
}

// 发表评论
const submitComment = (): void => {
  if (!newComment.value.trim()) return

  comments.value.unshift({
    id: comments.value.length + 1,
    user: {
      username: 'current_user',
      avatar: null
    },
    content: newComment.value,
    createdAt: new Date().toISOString().split('T')[0],
    likes: 0
  })

  newComment.value = ''
}

// 点赞评论
const likeComment = (comment: any): void => {
  comment.likes++
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

onMounted(() => {
  // 模拟加载
  setTimeout(() => {
    loading.value = false
  }, 500)
})
</script>

<template>
  <div class="roadmap-detail-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
          <!-- 返回按钮 -->
          <v-btn
            variant="text"
            color="grey-darken-2"
            class="mb-4"
            @click="backToList"
          >
            <v-icon icon="mdi-arrow-left" class="mr-1"></v-icon>
            返回
          </v-btn>

          <v-row v-if="loading" class="py-12">
            <v-col cols="12" class="text-center">
              <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
              <p class="text-body-2 text-grey mt-4">加载中...</p>
            </v-col>
          </v-row>

          <v-row v-else>
            <!-- 左侧：路径信息和流程图 -->
            <v-col cols="12" lg="8">
              <!-- 路径信息卡片 -->
              <v-card rounded="lg" class="roadmap-info-card mb-4 no-border">
                <v-card-text class="pa-0 py-6">
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
                          <v-icon icon="mdi-pin" size="14" class="mr-1"></v-icon>
                          置顶
                        </v-chip>
                        <v-chip
                          v-if="roadmap.learning"
                          color="success"
                          size="small"
                          variant="flat"
                        >
                          <v-icon icon="mdi-school" size="14" class="mr-1"></v-icon>
                          学习中
                        </v-chip>
                      </div>
                      <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-3">
                        {{ roadmap.description }}
                      </h1>

                      <!-- 创建者信息 -->
                      <div class="d-flex align-center mb-3">
                        <v-avatar size="40" color="grey-lighten-2" class="mr-3">
                          <v-icon v-if="!roadmap.creator.avatar" icon="mdi-account" color="grey"></v-icon>
                        </v-avatar>
                        <div>
                          <div class="text-body-2 font-weight-medium text-grey-darken-3">
                            {{ roadmap.creator.username }}
                          </div>
                          <div class="text-caption text-grey">
                            创建于 {{ getTimeDisplay(roadmap.createdAt) }}
                          </div>
                        </div>
                      </div>

                      <!-- 统计信息 -->
                      <div class="d-flex align-center gap-4">
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-account-group" size="18" color="grey" class="mr-1"></v-icon>
                          <span class="text-body-2 text-grey-darken-2">
                            {{ roadmap.learners }} 人学习
                          </span>
                        </div>
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-comment-outline" size="18" color="grey" class="mr-1"></v-icon>
                          <span class="text-body-2 text-grey-darken-2">
                            {{ roadmap.comment }} 评论
                          </span>
                        </div>
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-circle-medium" size="18" color="grey" class="mr-1"></v-icon>
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
                      <v-icon :icon="roadmap.upvoted ? 'mdi-heart' : 'mdi-heart-outline'" size="18" class="mr-1"></v-icon>
                      {{ roadmap.vote }}
                    </v-btn>
                    <v-btn
                      :color="roadmap.learning ? 'success' : 'primary'"
                      :variant="roadmap.learning ? 'outlined' : 'flat'"
                      rounded="lg"
                      @click="handleStartLearning"
                    >
                      <v-icon :icon="roadmap.learning ? 'mdi-check' : 'mdi-play'" size="18" class="mr-1"></v-icon>
                      {{ roadmap.learning ? '正在学习' : '开始学习' }}
                    </v-btn>
                    <v-btn
                      color="grey-darken-2"
                      variant="outlined"
                      rounded="lg"
                      @click="handleCopy"
                    >
                      <v-icon icon="mdi-content-copy" size="18" class="mr-1"></v-icon>
                      复制路径
                    </v-btn>
                  </div>
                </v-card-text>
              </v-card>

              <!-- 流程图 -->
              <v-card border rounded="lg" class="flow-card">
                <v-card-title class="pa-6 pb-4">
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-sitemap" color="primary" class="mr-2"></v-icon>
                    <span class="text-h6 font-weight-bold">学习路线图</span>
                  </div>
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
                      :zoom-on-scroll="false"
                      :zoom-on-pinch="false"
                      :zoom-on-double-click="false"
                      @node-click="handleNodeClick"
                    >
                      <Background pattern-color="#666" :gap="20" variant="dots" />
                      <Controls :show-zoom="false" />
                    </VueFlow>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>

            <!-- 右侧：评论区 -->
            <v-col cols="12" lg="4">
              <v-card rounded="lg" class="comments-card sticky-card no-border pa-4">
                <v-card-title class="pa-0 pb-4">
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-comment-multiple" color="primary" class="mr-2"></v-icon>
                    <span class="text-h6 font-weight-bold">评论</span>
                    <v-chip size="small" color="primary" variant="tonal" class="ml-2">
                      {{ comments.length }}
                    </v-chip>
                  </div>
                </v-card-title>

                <!-- 发表评论 -->
                <v-card-text class="pa-0 pb-4">
                  <v-textarea
                    v-model="newComment"
                    placeholder="写下你的评论..."
                    variant="outlined"
                    rows="3"
                    hide-details
                    class="mb-3"
                  ></v-textarea>
                  <v-btn
                    color="primary"
                    variant="flat"
                    rounded="lg"
                    :disabled="!newComment.trim()"
                    @click="submitComment"
                  >
                    <v-icon icon="mdi-send" size="18" class="mr-1"></v-icon>
                    发表评论
                  </v-btn>
                </v-card-text>

                <!-- 评论列表 -->
                <v-card-text class="pa-0 pt-4 comments-list">
                  <div
                    v-for="comment in comments"
                    :key="comment.id"
                    class="comment-item mb-4"
                  >
                    <div class="d-flex">
                      <v-avatar size="36" color="grey-lighten-2" class="mr-3">
                        <v-icon v-if="!comment.user.avatar" icon="mdi-account" color="grey" size="20"></v-icon>
                      </v-avatar>
                      <div class="flex-grow-1">
                        <div class="d-flex align-center justify-space-between mb-1">
                          <span class="text-body-2 font-weight-medium text-grey-darken-3">
                            {{ comment.user.username }}
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
                          variant="text"
                          color="grey-darken-2"
                          @click="likeComment(comment)"
                        >
                          <v-icon icon="mdi-thumb-up-outline" size="14" class="mr-1"></v-icon>
                          {{ comment.likes }}
                        </v-btn>
                      </div>
                    </div>
                  </div>

                  <div v-if="comments.length === 0" class="text-center py-8">
                    <v-icon icon="mdi-comment-outline" size="48" color="grey-lighten-2" class="mb-2"></v-icon>
                    <p class="text-body-2 text-grey">还没有评论</p>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

  </div>
</template>

<style scoped>
.roadmap-detail-page {
  min-height: 100vh;
  background-color: #FFFFFF;
}

.main-content {
  margin-left: max(160px, calc((100vw - 1550px) / 2));
  padding: 80px 40px 40px 40px;
  max-width: 1550px;
  width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
}

@media (min-width: 2229px) {
  .main-content {
    margin-left: max(160px, calc((100vw - 1550px) / 2));
    padding: 80px 40px 40px 40px;
    width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
    max-width: 1550px;
  }
}

.roadmap-info-card,
.flow-card,
.comments-card {
  background-color: #FFFFFF;
  border: 1px solid #E5E5E5;
  border-radius: 16px;
}

.flow-card {
  min-height: 1200px;
}

.vue-flow-container {
  height: 1200px;
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
  border-bottom: 1px solid #E5E5E5;
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
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    max-width: 100%;
    padding: 80px 20px 80px 20px;
  }

  .sticky-card {
    position: static;
    max-height: none;
  }

  .vue-flow-container {
    height: 400px;
  }
}
</style>
