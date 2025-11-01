<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import type { Ref } from 'vue'
import { useRouter } from 'vue-router'
import { progressServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useFetch } from '@/composables/useFetch'
import RoadmapVueFlow from '@/components/common/RoadmapVueFlow.vue'
import RoadmapDetail from '@/components/roadmap/RoadmapDetail.vue'
import dagre from 'dagre'
import { UserProgressState } from '@/types/enums'
import { useUserStore } from '@/stores/user'
import type { Course } from '@/types/course'
import type { FlowNode, FlowEdge } from '@/types/flow'
import type { ProcessedUserRoadmap } from '@/types/userRoadmap'
import { Position } from '@vue-flow/core'

// 扩展课程信息
interface LearningCourse extends Course {
  courseId: number
  title: string // 添加title字段
  totalLessons: number
  completedLessons: number
  category: string
  difficulty: string
  estimatedTime: string
  lastActivity: string
  instructor: string
}

interface LearningData {
  totalProgress: number
  completedNodes: number
  totalNodes: number
  roadmaps: ProcessedUserRoadmap[]
  courses: LearningCourse[]
  recentActivities: any[]
}

// Props
const props = defineProps<{
  userId?: number
}>()

const router = useRouter()
const showSnackbar = inject('showSnackbar') as (message: string, type?: string) => void
const userStore = useUserStore()

// 当前操作的用户ID
const targetUserId = computed(() => props.userId || userStore.currentUser?.id)

// 学习进度相关数据
const learningData: Ref<LearningData> = ref({
  totalProgress: 18,
  completedNodes: 2938,
  totalNodes: 29380,
  roadmaps: [],
  courses: [],
  recentActivities: [],
})

const selectedLearningTab: Ref<string> = ref('roadmaps')

// RoadmapDetail 浮层状态
const showRoadmapDetail: Ref<boolean> = ref(false)
const selectedRoadmap: Ref<ProcessedUserRoadmap | null> = ref(null)

// 解析路线图内容
const parseRoadmapContent = (content: string | object): { nodes: FlowNode[]; edges: FlowEdge[] } => {
  try {
    const data = typeof content === 'string' ? JSON.parse(content) : content
    console.log('解析路线图内容数据:', data)

    if (!data || typeof data !== 'object') {
      console.warn('路线图内容数据无效:', data)
      return { nodes: [], edges: [] }
    }

    const nodes = (data.nodes || []).map((node: any, index: number): FlowNode => {
      return {
        id: String(node.id || index),
        type: 'default',
        data: {
          label: node.label || node.data?.label || `节点 ${node.id || index}`,
          link: `/read?courseId=${node.id || index}`,
          completed: node.completed || node.data?.completed || false,
          current: node.current || node.data?.current || false,
          ...node.data,
        },
        position: node.position || { x: 0, y: 0 },
        sourcePosition: Position.Top,
        targetPosition: Position.Bottom,
      }
    })

    const edges = (data.edges || []).map((edge: any): FlowEdge => ({
      id: `${edge.source}-${edge.target}`,
      source: String(edge.source),
      target: String(edge.target),
      type: edge.type || 'bezier',
      animated: edge.animated || true,
      label: edge.label,
    }))

    return { nodes, edges }
  } catch (err) {
    console.error('解析路线图内容失败:', err, '原始内容:', content)
    return { nodes: [], edges: [] }
  }
}

// 自动布局函数
const applyAutoLayout = (nodeList: FlowNode[], edgeList: FlowEdge[], direction: string = 'BT'): FlowNode[] => {
  console.log('Applying auto layout with direction:', direction)
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

  nodeList.forEach((node) => {
    dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight })
  })
  edgeList.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target)
  })

  dagre.layout(dagreGraph)

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

// 帮助函数
const getDifficultyColor = (difficulty: string): string => {
  switch (difficulty) {
    case 'beginner':
      return 'success'
    case 'intermediate':
      return 'warning'
    case 'advanced':
      return 'error'
    default:
      return 'grey'
  }
}

const getCategoryIcon = (category: string): string => {
  switch (category) {
    case 'frontend':
      return 'mdi-web'
    case 'datascience':
      return 'mdi-chart-line'
    case 'ai':
      return 'mdi-brain'
    default:
      return 'mdi-book'
  }
}

const calculateTotalLessons = (course: any): number => {
  const description = course.description || ''
  return Math.max(10, Math.floor(description.length / 10))
}

const calculateCompletedLessons = (userCourse: any): number => {
  const totalLessons = calculateTotalLessons(userCourse.course)
  const progress = userCourse.progressPercent || 0
  return Math.floor((progress / 100) * totalLessons)
}

const calculateOverallProgress = (roadmaps: ProcessedUserRoadmap[], courses: LearningCourse[]): number => {
  const allItems = [...roadmaps, ...courses]
  if (allItems.length === 0) return 0

  const totalProgress = allItems.reduce((sum, item) => sum + (item.progress || 0), 0)
  return Math.round(totalProgress / allItems.length)
}

const calculateCompletedNodes = (roadmaps: ProcessedUserRoadmap[]): number => {
  return roadmaps.reduce((sum, roadmap) => {
    const progress = roadmap.progress || 0
    const totalSteps = (roadmap as any).totalSteps || 5
    return sum + Math.floor((progress / 100) * totalSteps)
  }, 0)
}

const calculateTotalNodes = (roadmaps: ProcessedUserRoadmap[]): number => {
  return roadmaps.reduce((sum, roadmap) => sum + ((roadmap as any).totalSteps || 5), 0)
}

const generateRecentActivities = (items: (ProcessedUserRoadmap | LearningCourse)[]): any[] => {
  const activities: any[] = []

  items.forEach((item) => {
    if (item.progress > 0) {
      activities.push({
        type: 'totalSteps' in item ? 'roadmap' : 'course',
        title: item.title,
        action: item.progress === 100 ? '已完成学习' : '正在学习中',
        time: item.lastActivity,
      })
    }
  })

  return activities.slice(0, 5)
}

const getRelativeTime = (dateString: string): string => {
  if (!dateString) return '未知时间'

  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 60) {
    return `${diffMins}分钟前`
  } else if (diffHours < 24) {
    return `${diffHours}小时前`
  } else {
    return `${diffDays}天前`
  }
}

const formatDate = (dateString: string): string => {
  if (!dateString) return '未知日期'
  return new Date(dateString).toLocaleDateString('zh-CN')
}

const extractTags = (description: string): string[] => {
  if (!description) return []

  const commonTags = [
    '前端',
    'Vue.js',
    'JavaScript',
    'React',
    'Python',
    '数据科学',
    '机器学习',
    'Java',
    'Spring',
  ]
  return commonTags.filter((tag) => description.includes(tag)).slice(0, 3)
}

const getCategoryFromDescription = (description: string): string => {
  if (!description) return 'other'
  const desc = description.toLowerCase()

  if (
    desc.includes('前端') ||
    desc.includes('vue') ||
    desc.includes('react') ||
    desc.includes('javascript')
  )
    return 'frontend'
  if (desc.includes('数据') || desc.includes('python') || desc.includes('分析'))
    return 'datascience'
  if (desc.includes('ai') || desc.includes('机器学习') || desc.includes('深度学习')) return 'ai'
  if (desc.includes('java') || desc.includes('spring') || desc.includes('后端')) return 'backend'

  return 'other'
}

const getDifficultyFromStatus = (state: number): string => {
  switch (state) {
    case UserProgressState.NOT_STARTED:
      return 'beginner'
    case UserProgressState.IN_PROGRESS:
      return 'intermediate'
    case UserProgressState.COMPLETED:
      return 'advanced'
    default:
      return 'beginner'
  }
}

const getEstimatedTime = (description: string): string => {
  if (!description) return '未知'

  const { length } = description
  if (length < 50) return '1-2小时'
  if (length < 100) return '3-5小时'
  if (length < 200) return '1-2天'
  return '3-7天'
}

const generateRoadmapHTML = (nodes: FlowNode[]): string => {
  if (!nodes || nodes.length === 0) {
    return '<p>暂无学习路径内容</p>'
  }

  const stepsHTML = nodes
    .map((node, index) => {
      const isCompleted = node.data.completed || false
      const isCurrent = node.data.current || false
      const stepClass = isCompleted ? 'completed' : isCurrent ? 'current' : ''

      return `
      <div class="path-step ${stepClass}">
        <div class="step-number">${index + 1}</div>
        <div class="step-content">
          <h4>${node.data.label || `步骤 ${index + 1}`}</h4>
          <p>${node.data.description || '学习相关内容'}</p>
          <div class="step-duration">预计时间: 1-2周</div>
        </div>
      </div>
    `
    })
    .join('')

  return `
    <h3>学习路径</h3>
    <div class="learning-path">
      ${stepsHTML}
    </div>
  `
}

const formatDateTime = (dateString: string): string => {
  if (!dateString) return '未知时间'
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const calculateDuration = (startTime: string): string => {
  if (!startTime) return '未知'

  const start = new Date(startTime)
  const now = new Date()
  const diffMs = now.getTime() - start.getTime()
  const diffDays = Math.floor(diffMs / 86400000)
  const diffHours = Math.floor((diffMs % 86400000) / 3600000)

  if (diffDays > 0) {
    return `${diffDays}天${diffHours}小时`
  } else {
    return `${diffHours}小时`
  }
}

const getStatusColor = (state: number): string => {
  switch (state) {
    case UserProgressState.NOT_STARTED:
      return 'grey'
    case UserProgressState.IN_PROGRESS:
      return 'primary'
    case UserProgressState.COMPLETED:
      return 'success'
    default:
      return 'grey'
  }
}

const getStatusIcon = (state: number): string => {
  switch (state) {
    case UserProgressState.NOT_STARTED:
      return 'mdi-circle-outline'
    case UserProgressState.IN_PROGRESS:
      return 'mdi-play-circle'
    case UserProgressState.COMPLETED:
      return 'mdi-check-circle'
    default:
      return 'mdi-circle-outline'
  }
}

const getStatusText = (state: string): string => {
  const stateTexts: Record<string, string> = {
    [UserProgressState.NOT_STARTED]: '未开始',
    [UserProgressState.IN_PROGRESS]: '进行中',
    [UserProgressState.COMPLETED]: '已完成',
  }
  return stateTexts[state] || '未知状态'
}

const handleNodeClick = ({ event, node }: { event: any; node: FlowNode }): void => {
  console.log('Node clicked:', event, node)

  if (node.id === '0') {
    return
  }

  if (node.data.link) {
    window.open(node.data.link, '_blank')
  }
}

const handleVoteRoadmap = async (roadmap: ProcessedUserRoadmap, event?: Event): Promise<void> => {
  if (event) {
    event.stopPropagation()
  }
  try {
    roadmap.upvoted = !roadmap.upvoted
    roadmap.vote += roadmap.upvoted ? 1 : -1
    showSnackbar(roadmap.upvoted ? '点赞成功！' : '取消点赞')
  } catch (error) {
    console.error('投票失败:', error)
    showSnackbar('操作失败，请重试')
  }
}

const closeRoadmap = (roadmap: ProcessedUserRoadmap, event?: Event): void => {
  if (event) {
    event.stopPropagation()
  }
  // TODO
  //if (confirm('确定要退出这个学习路线图吗？')) {
  //  showSnackbar('已退出学习路线图')
  //}
}

const openRoadmapDetail = (roadmap: ProcessedUserRoadmap): void => {
  selectedRoadmap.value = roadmap
  showRoadmapDetail.value = true
}

const openCourse = (courseId: number): void => {
  const url = router.resolve({ path: '/read', query: { courseId: courseId.toString() } }).href
  window.open(url, '_blank')
}

// 使用 useFetch 加载学习进度数据
useFetch({
  fetchFn: async () => {
    const userId = targetUserId.value
    if (!userId) return { roadmaps: [], courses: [] }

    // 并行加载路线图和课程数据
    const [roadmapsResponse, coursesResponse] = await Promise.all([
      progressServiceV1.getUserRoadmaps(),
      progressServiceV1.getAllCourseProgress(),
    ])

    return {
      roadmaps: roadmapsResponse.data || [],
      courses: coursesResponse.data || [],
    }
  },
  immediate: true,
  onSuccess: (data) => {
    const { roadmaps: rawRoadmaps, courses: rawCourses } = data

    // 处理路线图数据
    const processedRoadmaps: ProcessedUserRoadmap[] = rawRoadmaps.map((userRoadmap: any) => {
      const roadmap = userRoadmap.roadmap || {}

      // 解析并应用布局
      const { nodes, edges } = parseRoadmapContent(roadmap.content || '{}')
      const layoutedNodes = nodes.length > 0 ? applyAutoLayout(nodes, edges) : []

      // 计算进度相关指标
      const totalNodes = nodes.length
      const completedNodes = nodes.filter((n: FlowNode) => n.data?.completed).length
      const progress = totalNodes > 0 ? Math.round((completedNodes / totalNodes) * 100) : 0

      return {
        id: userRoadmap.id || roadmap.id,
        roadmapId: roadmap.id,
        title: roadmap.description || '未命名路线图',
        profession: roadmap.profession,
        description: roadmap.description || '',
        progress,
        state: userRoadmap.state || UserProgressState.NOT_STARTED,
        nodes: layoutedNodes,
        edges,
        completedNodes,
        totalNodes,
        tags: extractTags(roadmap.description || ''),
        upvoted: false,
        vote: 0,
        lastActivity: getRelativeTime(userRoadmap.updatedAt || userRoadmap.createdAt),
        startedAt: userRoadmap.startedAt,
        completedAt: userRoadmap.completedAt,
      } as ProcessedUserRoadmap
    })

    // 处理课程数据
    const processedCourses: LearningCourse[] = rawCourses.map((userCourse: any) => {
      const course = userCourse.course || {}
      const totalLessons = calculateTotalLessons(course)
      const completedLessons = calculateCompletedLessons(userCourse)
      const progress = userCourse.progressPercent || 0

      return {
        id: course.id,
        courseId: course.id,
        title: course.name || '未命名课程',
        name: course.name,
        description: course.description || '',
        progress,
        totalLessons,
        completedLessons,
        category: getCategoryFromDescription(course.description || ''),
        difficulty: getDifficultyFromStatus(userCourse.state),
        estimatedTime: getEstimatedTime(course.description || ''),
        lastActivity: getRelativeTime(userCourse.updatedAt || userCourse.createdAt),
        instructor: course.author || '未知',
      } as LearningCourse
    })

    // 更新 learningData
    learningData.value = {
      roadmaps: processedRoadmaps,
      courses: processedCourses,
      totalProgress: calculateOverallProgress(processedRoadmaps, processedCourses),
      completedNodes: calculateCompletedNodes(processedRoadmaps),
      totalNodes: calculateTotalNodes(processedRoadmaps),
      recentActivities: generateRecentActivities([...processedRoadmaps, ...processedCourses]),
    }
  },
  onError: (error) => {
    console.error('加载学习进度失败:', error)
    showSnackbar('加载学习进度失败，请重试')
  },
})
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="text-h5 font-weight-bold mb-2 text-grey-darken-2">我的学习</h2>
      <p class="text-body-2 text-grey-darken-2">管理您收藏的学习路线图和课程</p>
    </div>

    <!-- 统计卡片 -->
    <v-row class="mb-6">
      <v-col cols="3">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="text-center pa-4">
            <v-icon icon="mdi-chart-line" color="#424242" size="32" class="mb-2"></v-icon>
            <h3 class="text-h4 font-weight-bold mb-1 text-grey-darken-2">
              {{ learningData.totalProgress }}%
            </h3>
            <p class="text-body-2 mb-0 text-grey-darken-2">总体进度</p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="3">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="text-center pa-4">
            <v-icon icon="mdi-map" color="#424242" size="32" class="mb-2"></v-icon>
            <h3 class="text-h4 font-weight-bold mb-1 text-grey-darken-2">
              {{ learningData.roadmaps.length }}
            </h3>
            <p class="text-body-2 mb-0 text-grey-darken-2">学习路线图</p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="3">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="text-center pa-4">
            <v-icon icon="mdi-book-multiple" color="#424242" size="32" class="mb-2"></v-icon>
            <h3 class="text-h4 font-weight-bold mb-1 text-grey-darken-2">
              {{ learningData.courses.length }}
            </h3>
            <p class="text-body-2 mb-0 text-grey-darken-2">正在学习课程</p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="3">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="text-center pa-4">
            <v-icon icon="mdi-trophy" color="#424242" size="32" class="mb-2"></v-icon>
            <h3 class="text-h4 font-weight-bold mb-1 text-grey-darken-2">7</h3>
            <p class="text-body-2 mb-0 text-grey-darken-2">连续学习天数</p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- 标签页切换 -->
    <div class="mb-4">
      <v-btn-toggle
        v-model="selectedLearningTab"
        variant="outlined"
        color="#424242"
        rounded="lg"
        density="comfortable"
        class="learning-tab-toggle"
      >
        <v-btn value="roadmaps" size="default">
          <v-icon icon="mdi-map" class="mr-2" size="16"></v-icon>
          学习路线图
        </v-btn>
        <v-btn value="courses" size="default">
          <v-icon icon="mdi-book-multiple" class="mr-2" size="16"></v-icon>
          课程
        </v-btn>
      </v-btn-toggle>
    </div>

    <!-- 学习路线图标签页 -->
    <div v-if="selectedLearningTab === 'roadmaps'">
      <div v-if="learningData.roadmaps.length === 0" class="text-center py-8">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="py-8">
            <v-icon icon="mdi-map-search" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
            <h3 class="text-h6 text-grey-darken-2 mb-2">暂无学习路线图</h3>
            <p class="text-body-2 text-grey-darken-1">去课程路线页面添加感兴趣的学习路线图吧！</p>
            <v-btn
              color="primary"
              variant="flat"
              rounded="lg"
              class="mt-4"
              @click="router.push('/roadmap')"
            >
              <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
              浏览路线图
            </v-btn>
          </v-card-text>
        </v-card>
      </div>

      <div v-else>
        <div v-for="roadmap in learningData.roadmaps" :key="roadmap.id" class="mb-4">
          <v-card
            variant="flat"
            class="flat-card roadmap-card position-relative"
            @click="openRoadmapDetail(roadmap)"
          >
            <!-- 学习状态标签 -->
            <div class="status-badge-container">
              <div class="d-flex align-center">
                <!--
                <v-chip
                  :color="getStatusColor(roadmap.state)"
                  variant="flat"
                  size="small"
                  class="status-badge"
                >
                  <v-icon :icon="getStatusIcon(roadmap.state)" class="mr-1" size="14"></v-icon>
                  {{ getStatusText(roadmap.state) }}
                </v-chip>-->

                <!-- 进行中状态的关闭按钮 -->
                <v-btn
                  v-if="roadmap.state === UserProgressState.IN_PROGRESS"
                  variant="text"
                  size="x-small"
                  class="ml-2 close-btn"
                  color="grey-darken-2"
                  @click="closeRoadmap(roadmap, $event)"
                >
                  <v-icon size="16">mdi-close</v-icon>
                  <v-tooltip activator="parent" location="bottom"> 退出学习 </v-tooltip>
                </v-btn>
              </div>
            </div>

            <div class="d-flex align-stretch roadmap-card-content">
              <!-- 左侧信息区域 -->
              <div class="d-flex flex-column flex-grow-1 pt-2 roadmap-left-content">
                <!-- 标题 -->
                <div class="px-4 pt-2 pb-1">
                  <h3 class="text-h5 font-weight-normal mb-3 text-grey-darken-2">
                    {{ roadmap.profession?.name || roadmap.title }}
                  </h3>
                </div>

                <v-card-text class="text-body-2 flex-grow-1 pt-1 pb-2">
                  <!-- 学习进度信息 -->
                  <div class="mb-3">
                    <div class="d-flex justify-space-between text-body-2 mb-2">
                      <span class="text-grey-darken-3">完成进度</span>
                      <span class="text-primary font-weight-bold">{{ roadmap.progress }}%</span>
                    </div>
                    <v-progress-linear
                      :model-value="roadmap.progress"
                      color="primary"
                      background-color="grey-lighten-3"
                      height="8"
                      rounded="lg"
                    >
                    </v-progress-linear>
                  </div>

                  <div class="d-flex flex-wrap align-center mb-3">
                    <v-chip size="small" color="success" variant="tonal" class="mr-2 mb-1">
                      <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                      {{ roadmap.completedNodes }}/{{ roadmap.totalNodes }} 节点
                    </v-chip>
                    <v-chip size="small" color="info" variant="tonal" class="mr-2 mb-1">
                      <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                      {{ roadmap.lastActivity }}
                    </v-chip>
                    <v-chip
                      v-for="tag in roadmap.tags"
                      :key="tag"
                      size="small"
                      color="grey-lighten-1"
                      variant="tonal"
                      class="mr-2 mb-1"
                    >
                      {{ tag }}
                    </v-chip>
                  </div>

                  <!-- 时间信息 -->
                  <div class="time-info text-caption text-grey-darken-2">
                    <div v-if="roadmap.startedAt" class="mb-1">
                      <v-icon icon="mdi-calendar-start" size="12" class="mr-1"></v-icon>
                      开始时间: {{ formatDateTime(roadmap.startedAt) }}
                    </div>
                    <div v-if="roadmap.completedAt" class="mb-1">
                      <v-icon icon="mdi-calendar-check" size="12" class="mr-1"></v-icon>
                      完成时间: {{ formatDateTime(roadmap.completedAt) }}
                    </div>
                    <div v-if="!roadmap.completedAt && roadmap.startedAt" class="mb-1">
                      <v-icon icon="mdi-timer-sand" size="12" class="mr-1"></v-icon>
                      学习时长: {{ calculateDuration(roadmap.startedAt) }}
                    </div>
                  </div>
                </v-card-text>

                <!-- 操作按钮区域 -->
                <div class="px-4 py-2 d-flex justify-space-between border-t">
                  <div class="d-flex align-center">
                    <v-btn
                      variant="text"
                      size="small"
                      class="flat-action-icon"
                      color="primary"
                      @click="handleVoteRoadmap(roadmap, $event)"
                    >
                      <v-icon size="20" :class="{ 'vote-animation': roadmap.upvoted }">
                        {{ roadmap.upvoted ? 'mdi-thumb-up' : 'mdi-thumb-up-outline' }}
                      </v-icon>
                      <span class="ml-1 text-body-2">{{ roadmap.vote || 0 }}</span>
                      <v-tooltip activator="parent" location="top">
                        {{ roadmap.upvoted ? '已点赞' : '投票支持' }}
                      </v-tooltip>
                    </v-btn>

                    <v-btn variant="text" size="small" class="flat-action-icon" color="primary">
                      <v-icon size="20">mdi-comment-outline</v-icon>
                      <span class="ml-1 text-body-2">{{ (roadmap as any).comment || 0 }}</span>
                      <v-tooltip activator="parent" location="top">查看评论</v-tooltip>
                    </v-btn>
                  </div>
                </div>
              </div>

              <!-- 右侧VueFlow图表区域 -->
              <div class="d-flex align-center roadmap-right-content">
                <div
                  class="vue-flow-preview vue-flow-container"
                  @click="openRoadmapDetail(roadmap)"
                >
                  <RoadmapVueFlow
                    :nodes="roadmap.nodes as any"
                    :edges="roadmap.edges || []"
                    :readonly="true"
                    :show-background="true"
                    background-pattern="#aaa"
                    :min-zoom="0.3"
                    :max-zoom="0.8"
                    :snap-to-grid="true"
                    :snap-grid="[20, 20]"
                    @node-click="handleNodeClick"
                  />
                </div>
              </div>
            </div>
          </v-card>
        </div>
      </div>
    </div>

    <!-- 课程标签页 -->
    <div v-if="selectedLearningTab === 'courses'">
      <div v-if="learningData.courses.length === 0" class="text-center py-8">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="py-8">
            <v-icon icon="mdi-book-search" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
            <h3 class="text-h6 text-grey-darken-2 mb-2">暂无学习课程</h3>
            <p class="text-body-2 text-grey-darken-1">去课程中心添加感兴趣的课程吧！</p>
            <v-btn
              color="primary"
              variant="flat"
              rounded="lg"
              class="mt-4"
              @click="router.push('/course/list')"
            >
              <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
              浏览课程
            </v-btn>
          </v-card-text>
        </v-card>
      </div>

      <div v-else>
        <v-row>
          <v-col v-for="course in learningData.courses" :key="course.id" cols="12" md="6">
            <v-card
              flat
              color="grey-lighten-5"
              rounded="lg"
              class="course-card mb-3"
              @click="openCourse(course.id)"
            >
              <v-card-text class="pa-4">
                <div class="d-flex align-start justify-space-between mb-3">
                  <div class="d-flex align-center">
                    <v-avatar color="teal-lighten-4" size="32" class="mr-3">
                      <v-icon
                        :icon="getCategoryIcon(course.category)"
                        color="teal-darken-2"
                        size="16"
                      ></v-icon>
                    </v-avatar>
                    <div>
                      <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-1">
                        {{ course.title }}
                      </h4>
                      <p class="text-body-2 text-grey-darken-2 mb-0">{{ course.description }}</p>
                    </div>
                  </div>
                  <v-chip
                    variant="flat"
                    :color="getDifficultyColor(course.difficulty)"
                    size="small"
                  >
                    {{
                      course.difficulty === 'beginner'
                        ? '初级'
                        : course.difficulty === 'intermediate'
                          ? '中级'
                          : '高级'
                    }}
                  </v-chip>
                </div>

                <div class="mb-3">
                  <div class="d-flex justify-space-between text-body-2 mb-2">
                    <span class="text-grey-darken-3">课时进度</span>
                    <span class="text-primary font-weight-bold">{{ course.progress }}%</span>
                  </div>
                  <v-progress-linear
                    :model-value="course.progress"
                    color="primary"
                    background-color="grey-lighten-3"
                    height="8"
                    rounded="lg"
                  >
                  </v-progress-linear>
                </div>

                <div
                  class="d-flex justify-space-between align-center text-body-2 text-grey-darken-2"
                >
                  <span>{{ course.completedLessons }}/{{ course.totalLessons }} 课时</span>
                  <span>{{ course.lastActivity }}</span>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </div>
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

<style>
@import '@vue-flow/core/dist/style.css';
@import '@vue-flow/core/dist/theme-default.css';
/* 从 Self.vue 复制的样式 */
.roadmap-card,
.course-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.roadmap-card-content {
  min-height: 240px;
}

.roadmap-left-content {
  min-width: 0;
  flex: 1;
}

.roadmap-right-content {
  width: 400px;
  min-width: 400px;
}

.vue-flow-container {
  width: 100%;
  height: 100%;
}

.roadmap-card:hover,
.course-card:hover {
  transform: translateY(-4px);
  border-color: #4db6ac !important;
}

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

.time-info {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  padding: 8px 12px;
  margin-top: 8px;
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

.flat-action-icon {
  border-radius: 6px !important;
  transition: all 0.2s ease !important;
  min-width: auto !important;
  padding: 6px 8px !important;
  height: 32px !important;
}

.flat-action-icon:hover {
  background: rgba(178, 223, 219, 0.15) !important;
  transform: scale(1.05) !important;
}

.border-t {
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.vue-flow-preview {
  border-radius: 0 !important;
  overflow: hidden !important;
  border: none !important;
  background: linear-gradient(135deg, #fafafa 0%, #f5f7ff 100%) !important;
  margin: 0 !important;
  padding: 0 !important;
}

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
</style>