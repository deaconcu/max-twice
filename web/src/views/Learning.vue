<script setup>
import { ref, onMounted, inject, computed } from 'vue';
import { useRouter } from 'vue-router';
import { learnService, userService } from '@/services/learnService';
import { useUserStore } from "@/stores/user";
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import RoadmapDetail from '@/components/RoadmapDetail.vue';
import RightSidebar from '@/components/RightSidebar.vue';
import dagre from 'dagre';
import { usePlatformStats } from '@/composables/usePlatformStats';

const showSnackbar = inject('showSnackbar');
const user = useUserStore();
const router = useRouter();

// 使用平台统计数据
const { stats: platformStats, isLoading: statsLoading, error: statsError, refresh: refreshStats } = usePlatformStats();

// 响应式数据
const learningData = ref({
  totalProgress: 18,
  completedNodes: 2938,
  totalNodes: 29380,
  roadmaps: [], // 用户收藏的roadmap posts (VueFlow图)
  courses: [],    // 用户正在学习的课程
  recentActivities: []
});

const selectedTab = ref('roadmaps'); // 默认显示学习路线图
const searchQuery = ref('');
const selectedNavTab = ref('learning'); // 导航栏当前选中项
const selectedStatus = ref('all'); // 状态筛选: all, NOT_STARTED, IN_PROGRESS, COMPLETED

// RoadmapDetail 浮层状态
const showRoadmapDetail = ref(false);
const selectedRoadmap = ref(null);

// 自动布局函数 - 完全参考 RoadmapFlow 的实现
const applyAutoLayout = (nodeList, edgeList, direction = 'BT') => {
  console.log("Applying auto layout with direction:", direction);
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({
    rankdir: direction,
    nodesep: 180,
    ranksep: 80,
    marginx: 20,
    marginy: 20
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
        y: nodeWithPosition.y - nodeHeight / 2
      }
    }
  })
}

// 解析 content 字段 - 完全参考 RoadmapFlow 的实现
function parseContent(content) {
  try {
    const data = typeof content === 'string' ? JSON.parse(content) : content
    console.log('解析内容数据:', data) // 调试信息
    
    // 确保数据结构正确
    if (!data || typeof data !== 'object') {
      console.warn('内容数据无效:', data)
      return { nodes: [], edges: [] }
    }
    
    const nodes = (data.nodes || []).map((node, index) => {
      const completed = node.finished || node.completed || false;
      const progress = node.progress || 0;
      
      return {
        id: String(node.id || index),
        type: 'default',
        data: {
          type: 'course', // 标记为课程节点
          label: node.name || node.label || `节点 ${node.id || index}`, // 适配新的name字段
          link: '/read?courseId=' + (node.id || index),
          completed: completed, // 适配新的finished字段
          progress: progress, // 使用新的progress字段
          current: node.current || node.data?.current || false,
          courseId: node.id, // 保存课程ID
          ...node.data
        },
        // 先设置初始位置，稍后会用 dagre 重新计算
        position: node.position || { x: 0, y: 0 },
        // 设置连接点位置 - 参考 RoadmapFlow
        sourcePosition: 'top',    // source 在上面
        targetPosition: 'bottom',  // target 在下面
        // 为节点添加自定义class和样式
        class: completed ? 'completed-course' : (progress > 0 ? 'progress-course' : ''),
        style: progress > 0 ? { '--progress': `${progress}%` } : {}
      }
    })

    const edges = (data.edges || []).map((edge, index) => ({
      id: `${edge.source}-${edge.target}`,
      source: String(edge.source),
      target: String(edge.target),
      type: edge.type || 'bezier',
      animated: edge.animated || true,
      label: edge.label
      // 移除内联样式，让CSS接管样式控制
    }))

    return { nodes, edges }
  } catch (err) {
    console.error('解析课程内容失败:', err, '原始内容:', content)
    return { nodes: [], edges: [] }
  }
}

// 筛选后的学习数据
const filteredLearningData = computed(() => {
  const filtered = {
    ...learningData.value,
    roadmaps: learningData.value.roadmaps.filter(roadmap => {
      // 状态筛选
      if (selectedStatus.value !== 'all' && roadmap.status !== selectedStatus.value) {
        return false;
      }
      // 搜索筛选
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase();
        return roadmap.title?.toLowerCase().includes(query) || 
               roadmap.description?.toLowerCase().includes(query) ||
               roadmap.author?.toLowerCase().includes(query);
      }
      return true;
    }),
    courses: learningData.value.courses.filter(course => {
      // 状态筛选
      if (selectedStatus.value !== 'all' && course.status !== selectedStatus.value) {
        return false;
      }
      // 搜索筛选
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase();
        return course.title?.toLowerCase().includes(query) || 
               course.description?.toLowerCase().includes(query);
      }
      return true;
    })
  };
  return filtered;
});

onMounted(() => {
  loadLearningData();
});

const loadLearningData = async () => {
  try {
    // 并行加载用户学习的路线图数据和课程数据
    const [roadmapResponse, courseResponse] = await Promise.all([
      learnService.getUserRoadmaps(),
      learnService.getUserCourseList()
    ]);
    
    console.log('获取用户学习路线图数据:', roadmapResponse)
    console.log('获取用户学习课程数据:', courseResponse)
    
    let roadmaps = [];
    let courses = [];
    
    // 处理路线图数据
    if (roadmapResponse.code === 200 && Array.isArray(roadmapResponse.data)) {
      roadmaps = roadmapResponse.data.map(userRoadmap => {
        const roadmap = userRoadmap.roadmap
        const { nodes, edges } = parseContent(roadmap.content)
        
        // 首先进行布局计算 - 参考 RoadmapFlow
        const layoutedNodes = applyAutoLayout(nodes, edges, 'BT')
        
        // 计算路线图的真实进度：已完成课程数 + 未完成课程的进度总和
        const calculateRoadmapProgress = (nodes) => {
          let totalProgress = 0;
          let totalCourses = 0;
          
          nodes.forEach(node => {
            // 所有节点都是课程节点（后端返回的都是课程）
            if (node.data && node.data.type === 'course') {
              totalCourses++;
              if (node.data.completed) {
                // 已完成课程贡献100%
                totalProgress += 100;
              } else if (node.data.progress) {
                // 未完成课程贡献其当前进度
                totalProgress += parseFloat(node.data.progress);
              }
            }
          });
          
          return totalCourses > 0 ? totalProgress / totalCourses : 0;
        };
        
        // 计算完成的节点数和总节点数（保持原有逻辑用于其他地方）
        const completedNodes = nodes.filter(node => node.data.completed).length;
        const totalNodes = nodes.length;
        const realProgress = calculateRoadmapProgress(nodes);
        
        return {
          id: roadmap.id,
          title: roadmap.description || `学习路线图 ${roadmap.id}`,
          description: roadmap.description || '暂无描述',
          author: roadmap.creator?.name || '未知用户',
          createdAt: roadmap.createdAt,
          addedDate: userRoadmap.startedAt,
          vote: roadmap.vote || 0,
          upvoted: roadmap.upvoted || false,
          progress: realProgress, // 已经是0-100的范围
          completedNodes: completedNodes,
          totalNodes: totalNodes,
          lastActivity: getRelativeTime(userRoadmap.updatedAt),
          status: userRoadmap.status,
          startedAt: userRoadmap.startedAt,
          completedAt: userRoadmap.completedAt,
          tags: extractTags(roadmap.description),
          profession: roadmap.profession,
          nodes: layoutedNodes,
          edges: edges
        }
      })
    }
    
    // 处理课程数据
    if (courseResponse.code === 200 && Array.isArray(courseResponse.data)) {
      courses = courseResponse.data.map(userCourse => {
        return {
          id: userCourse.id,
          courseId: userCourse.course.id,
          title: userCourse.course.name,
          name: userCourse.course.name,
          description: userCourse.course.description,
          progress: userCourse.progressPercent ? userCourse.progressPercent / 100 : 0,
          status: userCourse.status, // NOT_STARTED, IN_PROGRESS, COMPLETED
          startedAt: userCourse.startedAt,
          completedAt: userCourse.completedAt,
          createdAt: userCourse.createdAt,
          updatedAt: userCourse.updatedAt,
          lastActivity: getRelativeTime(userCourse.updatedAt),
          // 根据状态设置其他属性
          category: getCategoryFromDescription(userCourse.course.description),
          difficulty: getDifficultyFromStatus(userCourse.status),
          estimatedTime: getEstimatedTime(userCourse.course.description)
        }
      })
    }
    
    learningData.value = {
      totalProgress: calculateOverallProgress(roadmaps),
      completedNodes: roadmaps.reduce((sum, r) => sum + r.completedNodes, 0),
      totalNodes: roadmaps.reduce((sum, r) => sum + r.totalNodes, 0),
      roadmaps: roadmaps,
      courses: courses,
      recentActivities: generateRecentActivities([...roadmaps, ...courses])
    }

    console.log("learningData:", learningData.value) // 调试信息
  } catch (error) {
    console.error('Error loading learning data:', error);
    showSnackbar('加载学习数据失败');
  }
};

// 计算总体进度
function calculateOverallProgress(roadmaps) {
  if (roadmaps.length === 0) return 0
  const totalProgress = roadmaps.reduce((sum, roadmap) => sum + roadmap.progress, 0)
  return Math.round(totalProgress / roadmaps.length)
}

// 获取相对时间
function getRelativeTime(dateString) {
  if (!dateString) return '未知时间'
  
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now - date
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

// 从描述中提取标签
function extractTags(description) {
  if (!description) return []
  
  // 简单的标签提取逻辑，可以根据实际需求改进
  const commonTags = ['前端', 'Vue.js', 'JavaScript', 'React', 'Python', '数据科学', '机器学习', 'Java', 'Spring']
  return commonTags.filter(tag => description.includes(tag)).slice(0, 3)
}

// 生成最近活动数据
function generateRecentActivities(items) {
  const activities = []
  
  items.forEach(item => {
    if (item.status === 'IN_PROGRESS') {
      activities.push({
        type: item.nodes ? 'roadmap' : 'course', // 有nodes的是roadmap，否则是course
        title: item.title || item.name,
        action: '正在学习中',
        time: item.lastActivity
      })
    }
    
    if (item.status === 'COMPLETED') {
      activities.push({
        type: item.nodes ? 'roadmap' : 'course',
        title: item.title || item.name,
        action: '已完成学习',
        time: getRelativeTime(item.completedAt)
      })
    }
  })
  
  return activities.slice(0, 5) // 返回最近5个活动
}

// 根据课程描述推断分类
function getCategoryFromDescription(description) {
  if (!description) return 'other'
  const desc = description.toLowerCase()
  
  if (desc.includes('前端') || desc.includes('vue') || desc.includes('react') || desc.includes('javascript')) return 'frontend'
  if (desc.includes('数据') || desc.includes('python') || desc.includes('分析')) return 'datascience'
  if (desc.includes('ai') || desc.includes('机器学习') || desc.includes('深度学习')) return 'ai'
  if (desc.includes('java') || desc.includes('spring') || desc.includes('后端')) return 'backend'
  
  return 'other'
}

// 根据状态推断难度
function getDifficultyFromStatus(status) {
  switch (status) {
    case 'NOT_STARTED': return 'beginner'
    case 'IN_PROGRESS': return 'intermediate'
    case 'COMPLETED': return 'advanced'
    default: return 'beginner'
  }
}

// 根据描述估算学习时间
function getEstimatedTime(description) {
  if (!description) return '未知'
  
  // 简单的估算逻辑，可根据实际需求调整
  const length = description.length
  if (length < 50) return '1-2小时'
  if (length < 100) return '3-5小时'
  if (length < 200) return '1-2天'
  return '3-7天'
}

const getDifficultyColor = (difficulty) => {
  switch (difficulty) {
    case 'beginner': return 'success';
    case 'intermediate': return 'warning';
    case 'advanced': return 'error';
    default: return 'grey';
  }
};

const getCategoryIcon = (category) => {
  switch (category) {
    case 'frontend': return 'mdi-web';
    case 'datascience': return 'mdi-chart-line';
    case 'ai': return 'mdi-brain';
    default: return 'mdi-book';
  }
};

const getActivityIcon = (type) => {
  switch (type) {
    case 'course': return 'mdi-play-circle';
    case 'roadmap': return 'mdi-map';
    case 'achievement': return 'mdi-trophy';
    default: return 'mdi-information';
  }
};

const openCourse = (course) => {
  // 使用课程的courseId来跳转
  const courseId = course.courseId || course.id;
  const url = router.resolve({ path: '/read', query: { courseId: courseId } }).href;
  window.open(url, '_blank');
};

const openRoadmap = (roadmapId) => {
  // 跳转到RoadmapFlow页面，使用路线图所属的专业ID
  // 这里可能需要根据实际的路由设计调整
  router.push(`/roadmap/${roadmapId}`);
};

// 打开路线图详情浮层
const openRoadmapDetail = (roadmap) => {
  selectedRoadmap.value = roadmap;
  showRoadmapDetail.value = true;
};

// 处理节点点击 - 参考 RoadmapCard 的实现
const handleNodeClick = ({event, node}) => {
  console.log('Node clicked:', event, node);
  // 阻止事件冒泡，避免触发卡片点击
  event?.stopPropagation?.();
  
  // 根节点不跳转
  if (node.id === '0') {
    return
  }
  
  if (node.data.link) {
    window.open(node.data.link, '_blank')
  }
};

// 路线图投票功能
const handleVoteRoadmap = async (roadmap, event) => {
  if (event) {
    event.stopPropagation();
  }
  try {
    // 这里应该调用后端API
    roadmap.upvoted = !roadmap.upvoted;
    roadmap.vote += roadmap.upvoted ? 1 : -1;
    showSnackbar(roadmap.upvoted ? '点赞成功！' : '取消点赞');
  } catch (error) {
    console.error('投票失败:', error);
    showSnackbar('操作失败，请重试');
  }
};

// 上移路线图
const moveRoadmapUp = (roadmap, event) => {
  if (event) {
    event.stopPropagation();
  }
  // TODO: 实现上移逻辑
  showSnackbar('路线图上移');
};

// 下移路线图
const moveRoadmapDown = (roadmap, event) => {
  if (event) {
    event.stopPropagation();
  }
  // TODO: 实现下移逻辑
  showSnackbar('路线图下移');
};

// 关闭/退出学习路线图
const closeRoadmap = (roadmap, event) => {
  if (event) {
    event.stopPropagation();
  }
  // TODO: 实现关闭逻辑，例如显示确认对话框
  if (confirm('确定要退出这个学习路线图吗？')) {
    // 这里可以调用后端API来退出学习
    showSnackbar('已退出学习路线图');
  }
};

// 关闭/退出学习课程
const closeCourse = (course, event) => {
  if (event) {
    event.stopPropagation();
  }
  // TODO: 实现关闭逻辑，例如显示确认对话框
  if (confirm('确定要退出这个课程吗？')) {
    // 这里可以调用后端API来退出学习
    showSnackbar('已退出学习课程');
  }
};

// 根据用户名获取头像颜色
const getAvatarColor = (name) => {
  const colors = ['primary', 'success', 'info', 'warning', 'orange', 'purple', 'pink', 'indigo'];
  if (!name) return 'primary';
  
  // 使用名字的第一个字符来确定颜色
  const charCode = name.charCodeAt(0);
  return colors[charCode % colors.length];
};

// 格式化日期
const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('zh-CN');
};

// 格式化日期时间
const formatDateTime = (dateString) => {
  if (!dateString) return '未知时间'
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 计算学习时长
const calculateDuration = (startTime) => {
  if (!startTime) return '未知'
  
  const start = new Date(startTime)
  const now = new Date()
  const diffMs = now - start
  const diffDays = Math.floor(diffMs / 86400000)
  const diffHours = Math.floor((diffMs % 86400000) / 3600000)
  
  if (diffDays > 0) {
    return `${diffDays}天${diffHours}小时`
  } else {
    return `${diffHours}小时`
  }
};

// 获取状态颜色
const getStatusColor = (status) => {
  switch (status) {
    case 'NOT_STARTED': return 'grey'
    case 'IN_PROGRESS': return 'primary'
    case 'COMPLETED': return 'success'
    default: return 'grey'
  }
};

// 获取状态图标
const getStatusIcon = (status) => {
  switch (status) {
    case 'NOT_STARTED': return 'mdi-circle-outline'
    case 'IN_PROGRESS': return 'mdi-play-circle'
    case 'COMPLETED': return 'mdi-check-circle'
    default: return 'mdi-circle-outline'
  }
};

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'NOT_STARTED': return '未开始'
    case 'IN_PROGRESS': return '进行中'
    case 'COMPLETED': return '已完成'
    default: return '未知状态'
  }
};
</script>

<template>
  <v-container fluid>
    <v-row class="mt-2">
      
      <v-col cols="9" class="pr-8">
        <div class="mb-8">
          <!-- 页面标题区域 -->
          <v-row justify="start" class="mb-4">
            <v-col cols="12">
              <div class="d-flex align-center justify-space-between mb-3">
                <div class="d-flex align-center">
                  <v-avatar color="teal-lighten-4" size="40" class="mr-3">
                    <v-icon icon="mdi-school" color="teal-darken-2" size="20"></v-icon>
                  </v-avatar>
                  <div>
                    <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">正在学习</h1>
                    <p class="text-body-2 text-grey-darken-2 mb-0">继续您的学习之旅</p>
                  </div>
                </div>
                
                <!-- 导航栏 -->
                <div class="d-flex align-center">
                  <v-btn-toggle v-model="selectedNavTab" variant="text" color="primary" class="nav-toggle">
                    <v-btn value="learning" class="nav-btn" :class="{ 'nav-btn-active': selectedNavTab === 'learning' }">
                      正在学习
                    </v-btn>
                    <v-btn value="career" class="nav-btn" @click="router.push('/career')">
                      职业中心
                    </v-btn>
                    <v-btn value="courses" class="nav-btn" @click="router.push('/course/list')">
                      课程中心
                    </v-btn>
                  </v-btn-toggle>
                </div>
              </div>
            </v-col>
          </v-row>

          <!-- 搜索和筛选区域 -->
          <v-row justify="start" align="center" class="mb-4">
            <v-col cols="4" class="d-flex justify-start">
              <v-btn-toggle v-model="selectedTab" variant="outlined" color="primary" rounded="lg" density="comfortable">
                <v-btn value="roadmaps" size="default">
                  <v-icon icon="mdi-map" class="mr-2" size="16"></v-icon>
                  学习路线图
                </v-btn>
                <v-btn value="courses" size="default">
                  <v-icon icon="mdi-book-multiple" class="mr-2" size="16"></v-icon>
                  课程
                </v-btn>
              </v-btn-toggle>
            </v-col>
            
            <v-col cols="5" class="d-flex justify-start">
              <!-- 状态筛选按钮 -->
              <v-btn-toggle v-model="selectedStatus" rounded="lg" color="grey-darken-2" variant="text"
                density="compact" mandatory class="status-filter">
                <v-btn value="all" size="small" class="me-1 rounded-lg text-body-2">
                  <v-icon icon="mdi-format-list-bulleted" class="mr-1" size="14"></v-icon>
                  全部
                </v-btn>
                <v-btn value="NOT_STARTED" size="small" class="me-1 rounded-lg text-body-2">
                  <v-icon icon="mdi-circle-outline" class="mr-1" size="14"></v-icon>
                  未开始
                </v-btn>
                <v-btn value="IN_PROGRESS" size="small" class="me-1 rounded-lg text-body-2">
                  <v-icon icon="mdi-play-circle" class="mr-1" size="14"></v-icon>
                  正在学习
                </v-btn>
                <v-btn value="COMPLETED" size="small" class="me-1 rounded-lg text-body-2">
                  <v-icon icon="mdi-check-circle" class="mr-1" size="14"></v-icon>
                  已完成
                </v-btn>
              </v-btn-toggle>
            </v-col>
            <v-col cols="3" class="d-flex justify-end">
              <v-text-field 
                v-model="searchQuery"
                hide-details="auto" 
                density="compact" 
                class="search-input" 
                rounded="lg"
                placeholder="搜索学习内容..." 
                variant="outlined">
                <template v-slot:prepend-inner>
                  <v-icon icon="mdi-magnify" color="grey-lighten-1" size="18"></v-icon>
                </template>
              </v-text-field>
            </v-col>
          </v-row>
        </div>

        <!-- 学习路线图标签页 -->
        <div v-if="selectedTab === 'roadmaps'">
          <div v-if="filteredLearningData.roadmaps.length === 0" class="text-center py-8">
            <v-card flat color="grey-lighten-5" rounded="lg">
              <v-card-text class="py-8">
                <v-icon icon="mdi-map-search" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
                <h3 class="text-h6 text-grey-darken-2 mb-2">
                  {{ selectedStatus === 'all' ? '暂无学习路线图' : `暂无${getStatusText(selectedStatus)}的学习路线图` }}
                </h3>
                <p class="text-body-2 text-grey-darken-1">
                  {{ selectedStatus === 'all' ? '去课程路线页面添加感兴趣的学习路线图吧！' : '尝试切换其他状态或去添加新的学习路线图' }}
                </p>
                <v-btn color="primary" variant="flat" rounded="lg" class="mt-4" @click="router.push('/roadmap')">>
                  <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
                  浏览路线图
                </v-btn>
              </v-card-text>
            </v-card>
          </div>

          <div v-else>
            <div v-for="roadmap in filteredLearningData.roadmaps" :key="roadmap.id" class="mb-4">
              <v-card @click="openRoadmapDetail(roadmap)" variant="flat" class="flat-card roadmap-card position-relative">
                <!-- 学习状态标签 -->
                <div class="status-badge-container">
                  <div class="d-flex align-center">
                    <v-chip 
                      :color="getStatusColor(roadmap.status)" 
                      variant="flat" 
                      size="small"
                      class="status-badge">
                      <v-icon :icon="getStatusIcon(roadmap.status)" class="mr-1" size="14"></v-icon>
                      {{ getStatusText(roadmap.status) }}
                    </v-chip>
                    
                    <!-- 进行中状态的关闭按钮 -->
                    <v-btn 
                      v-if="roadmap.status === 'IN_PROGRESS'"
                      variant="text" 
                      size="x-small" 
                      class="ml-2 close-btn"
                      color="grey-darken-2"
                      @click="closeRoadmap(roadmap, $event)">
                      <v-icon size="16">mdi-close</v-icon>
                      <v-tooltip activator="parent" location="bottom">
                        退出学习
                      </v-tooltip>
                    </v-btn>
                  </div>
                </div>
                
                <div class="d-flex align-stretch" style="min-height: 240px;">
                  <!-- 左侧信息区域 -->
                  <div class="d-flex flex-column flex-grow-1 pt-2" style="min-width: 0; flex: 1;">
                    <!-- 标题 -->
                    <div class="px-4 pt-2 pb-1">
                      <h3 class="text-h5 font-weight-normal mb-3 text-grey-darken-2">{{ roadmap.profession?.name || roadmap.title }}</h3>
                    </div>

                    <!-- 用户信息和描述并排 -->
                    <div class="px-4 pb-2">
                      <div class="d-flex align-start">
                        <v-avatar :color="getAvatarColor(roadmap.author)" size="32" class="mr-3 flat-avatar flex-shrink-0">
                          <span class="text-white text-caption">{{ roadmap.author?.charAt(0) || 'U' }}</span>
                        </v-avatar>
                        <div class="flex-grow-1 min-width-0">
                          <div class="text-body-2 text-grey-darken-2 mb-1">{{ roadmap.author || '未知用户' }} · {{ formatDate(roadmap.createdAt) }}</div>
                          <div class="text-body-2 text-grey-darken-3 description-text">{{ roadmap.description || '暂无描述' }}</div>
                        </div>
                      </div>
                    </div>

                    <v-card-text class="text-body-2 flex-grow-1 pt-1 pb-2">
                      
                      <!-- 学习进度信息 -->
                      <div class="mb-3">
                        <div class="d-flex justify-space-between text-body-2 mb-2">
                          <span class="text-grey-darken-3">完成进度</span>
                          <span class="text-primary font-weight-bold">{{ parseFloat(roadmap.progress.toFixed(2)) }}%</span>
                        </div>
                        <v-progress-linear 
                          :model-value="roadmap.progress" 
                          color="primary" 
                          background-color="grey-lighten-3" 
                          height="8" 
                          rounded="lg">
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
                        <v-chip v-for="tag in roadmap.tags" :key="tag" size="small" color="grey-lighten-1" variant="tonal" class="mr-2 mb-1">
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
                        <v-btn variant="text" size="small" class="flat-action-icon" 
                          :color="roadmap.upvoted ? 'red-darken-2' : 'primary'"
                          @click="handleVoteRoadmap(roadmap, $event)">
                          <v-icon size="20" :class="{ 'vote-animation': roadmap.upvoted }">
                            {{ roadmap.upvoted ? 'mdi-thumb-up' : 'mdi-thumb-up-outline' }}
                          </v-icon>
                          <span class="ml-1 text-body-2">{{ roadmap.vote || 0 }}</span>
                          <v-tooltip activator="parent" location="top">
                            {{ roadmap.upvoted ? '已点赞' : '投票支持' }}
                          </v-tooltip>
                        </v-btn>

                        <v-btn variant="text" size="small" class="flat-action-icon" color="info">
                          <v-icon size="20">mdi-comment-outline</v-icon>
                          <span class="ml-1 text-body-2">{{ roadmap.comment || 0 }}</span>
                          <v-tooltip activator="parent" location="top">查看评论</v-tooltip>
                        </v-btn>
                      </div>

                      <div class="d-flex align-center">
                        <v-btn variant="text" size="small" class="flat-action-icon" color="success">
                          <v-icon size="20">mdi-school</v-icon>
                          <v-tooltip activator="parent" location="top">继续学习</v-tooltip>
                        </v-btn>

                        <!-- 上下移动按钮 -->
                        <v-btn variant="text" size="small" class="flat-action-icon ml-1" color="grey-darken-2"
                          @click="moveRoadmapUp(roadmap, $event)">
                          <v-icon size="18">mdi-arrow-up</v-icon>
                          <v-tooltip activator="parent" location="top">上移</v-tooltip>
                        </v-btn>

                        <v-btn variant="text" size="small" class="flat-action-icon ml-1" color="grey-darken-2"
                          @click="moveRoadmapDown(roadmap, $event)">
                          <v-icon size="18">mdi-arrow-down</v-icon>
                          <v-tooltip activator="parent" location="top">下移</v-tooltip>
                        </v-btn>
                      </div>
                    </div>
                  </div>

                  <!-- 右侧VueFlow图表区域 -->
                  <div class="d-flex align-center" style="width: 400px; min-width: 400px; ">
                    <div class="vue-flow-preview" style="width: 100%; height: 100%;">
                      <VueFlow 
                        v-if="roadmap.nodes && roadmap.nodes.length > 0"
                        :nodes="roadmap.nodes" 
                        :edges="roadmap.edges || []" 
                        fit-view-on-init 
                        :min-zoom="0.3"
                        :max-zoom="0.8" 
                        :snap-to-grid="true" 
                        :snap-grid="[20, 20]" 
                        :zoom-on-scroll="false"
                        :pan-on-scroll="false" 
                        :pan-on-drag="false" 
                        :nodes-draggable="false" 
                        :nodes-connectable="false"
                        :elements-selectable="true" 
                        @node-click="handleNodeClick"
                        class="vue-flow-readonly"
                        >
                        <Background pattern-color="#aaa" :gap="20" />
                      </VueFlow>
                      <div v-else class="d-flex align-center justify-center h-100 text-grey-darken-2">
                        <div class="text-center">
                          <v-icon icon="mdi-map-outline" size="48" class="mb-2"></v-icon>
                          <div class="text-body-2">暂无学习路径</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </v-card>
            </div>
          </div>
        </div>

        <!-- 课程标签页 -->
        <div v-if="selectedTab === 'courses'">
          <div v-if="filteredLearningData.courses.length === 0" class="text-center py-8">
            <v-card flat color="grey-lighten-5" rounded="lg">
              <v-card-text class="py-8">
                <v-icon icon="mdi-book-search" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
                <h3 class="text-h6 text-grey-darken-2 mb-2">
                  {{ selectedStatus === 'all' ? '暂无学习课程' : `暂无${getStatusText(selectedStatus)}的课程` }}
                </h3>
                <p class="text-body-2 text-grey-darken-1">
                  {{ selectedStatus === 'all' ? '去课程中心添加感兴趣的课程吧！' : '尝试切换其他状态或去添加新的课程' }}
                </p>
                <v-btn color="primary" variant="flat" rounded="lg" class="mt-4" @click="router.push('/course/list')">
                  <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
                  浏览课程
                </v-btn>
              </v-card-text>
            </v-card>
          </div>

          <div v-else>
            <v-row>
              <v-col v-for="course in filteredLearningData.courses" :key="course.id" cols="12" md="6">
                <v-card 
                  flat 
                  color="grey-lighten-5" 
                  rounded="lg" 
                  class="course-card mb-3"
                  @click="openCourse(course)">
                  <v-card-text class="pa-4">
                    <div class="d-flex align-start justify-space-between mb-3">
                      <div class="d-flex align-start flex-grow-1 mr-3">
                        <v-avatar color="teal-lighten-4" size="32" class="mr-3 flex-shrink-0" style="margin-top: 5px;">
                          <v-icon :icon="getCategoryIcon(course.category)" color="teal-darken-2" size="16"></v-icon>
                        </v-avatar>
                        <div class="flex-grow-1 min-width-0">
                          <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-1">{{ course.title }}</h4>
                          <p class="text-body-2 text-grey-darken-2 mb-0 course-description-text">{{ course.description }}</p>
                        </div>
                      </div>
                      <div class="d-flex align-start flex-shrink-0">
                        <v-chip 
                          variant="flat" 
                          :color="getDifficultyColor(course.difficulty)"
                          size="small"
                          class="course-difficulty-chip mr-2">
                          {{ course.difficulty === 'beginner' ? '初级' : course.difficulty === 'intermediate' ? '中级' : '高级' }}
                        </v-chip>
                        <v-btn 
                          variant="text" 
                          size="x-small" 
                          class="course-close-btn"
                          color="grey-darken-2"
                          @click="closeCourse(course, $event)">
                          <v-icon size="16">mdi-close</v-icon>
                          <v-tooltip activator="parent" location="bottom">
                            退出学习
                          </v-tooltip>
                        </v-btn>
                      </div>
                    </div>

                    <div class="mb-3">
                      <div class="d-flex justify-space-between text-body-2 mb-2">
                        <span class="text-grey-darken-3">课时进度</span>
                        <span class="text-primary font-weight-bold">{{ parseFloat(course.progress.toFixed(2)) }}%</span>
                      </div>
                      <v-progress-linear 
                        :model-value="course.progress" 
                        color="primary" 
                        background-color="grey-lighten-3" 
                        height="8" 
                        rounded="lg">
                      </v-progress-linear>
                    </div>

                    <div class="d-flex justify-space-between align-center text-body-2 text-grey-darken-2">
                      <span>{{ getStatusText(course.status) }}</span>
                      <span>{{ course.lastActivity }}</span>
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </div>
        </div>
      </v-col>

      <!-- 右侧边栏 -->
      <v-col cols="3">
        <RightSidebar :exclude-modules="['learning']"/>
      </v-col>
    </v-row>
  </v-container>

    <!-- RoadmapDetail 浮层 -->
    <RoadmapDetail 
      v-if="selectedRoadmap"
      v-model="showRoadmapDetail" 
      :roadmap="selectedRoadmap"
      @close="showRoadmapDetail = false" />
</template>

<style scoped>
.text-h7 {
  font-size: 1.15rem;
}

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

/* 课程描述文字样式 - 最多五行 */
.course-description-text {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  max-height: calc(1.4em * 5);
  word-break: break-word;
  overflow-wrap: break-word;
}

/* 课程难度标签样式 - 设置最小宽度 */
.course-difficulty-chip {
  min-width: 50px !important;
  justify-content: center !important;
  text-align: center !important;
}

/* 课程关闭按钮样式 */
.course-close-btn {
  min-width: auto !important;
  width: 24px !important;
  height: 24px !important;
  border-radius: 50% !important;
  padding: 0 !important;
  transition: all 0.2s ease !important;
}

/* 渐变背景样式 */
.gradient-primary {
  background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%) !important;
}

/* 排名徽章颜色 */
.v-chip.text-gold {
  background: #ffd700 !important;
  color: #8b6914 !important;
}

.v-chip.text-silver {
  background: #c0c0c0 !important;
  color: #6a6a6a !important;
}

.v-chip.text-bronze {
  background: #cd7f32 !important;
  color: #8b5a2b !important;
}

/* 新增样式 */
.data-item {
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.data-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 跳转图标样式 */
.jump-icon {
  position: absolute;
  top: 2px;
  right: 2px;
  opacity: 0.7;
  transition: all 0.2s ease;
}

.data-item-clickable:hover .jump-icon {
  opacity: 1;
  transform: scale(1.2);
}

/* 可点击的数据项样式 */
.data-item-clickable {
  cursor: pointer;
}

/* 静态数据项样式 - 覆盖默认悬停效果 */
.data-item-static:hover {
  transform: none;
  box-shadow: none;
}

.vision-card {
  border: 2px solid #ffebee;
}

.vision-content {
  background: linear-gradient(135deg, #ffebee 0%, #fce4ec 100%);
  border: 1px solid #f8bbd9;
}

.stat-card {
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.stat-card:hover {
  transform: translateX(4px);
  border-color: rgba(76, 175, 80, 0.3);
}

.progress-item {
  transition: all 0.2s ease;
}

.progress-item:hover {
  transform: scale(1.05);
}

.ranking-item {
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.ranking-item:hover {
  transform: translateX(4px);
  border-color: rgba(25, 118, 210, 0.3);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.rank-chip {
  min-width: 50px !important;
}

/* 滚动列表样式 */
.scrollable-career-list,
.scrollable-course-list {
  max-height: 140px; /* 约2.5个项目的高度：每项50px * 2.5 + 间距 */
  overflow-y: auto;
  scrollbar-width: none; /* Firefox 隐藏滚动条 */
  -ms-overflow-style: none; /* IE 隐藏滚动条 */
}

/* Webkit 浏览器隐藏滚动条 */
.scrollable-career-list::-webkit-scrollbar,
.scrollable-course-list::-webkit-scrollbar {
  display: none;
}

/* 学习项目样式 */
.career-item,
.course-item {
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.career-item:hover,
.course-item:hover {
  transform: translateX(2px);
  border-color: rgba(76, 175, 80, 0.3);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 紧凑间距样式 */
.pa-0-5 {
  padding: 2px !important;
}

.course-close-btn:hover {
  background: rgba(0, 0, 0, 0.08) !important;
  transform: scale(1.1) !important;
}

.search-input {
  width: 100%;
}

/* 导航栏样式 */
.nav-toggle {
  background: rgba(255, 255, 255, 0.8) !important;
  border-radius: 8px !important;
  border: 1px solid #e0e0e0 !important;
}

.nav-btn {
  padding: 8px 16px !important;
  font-size: 0.875rem !important;
  font-weight: 500 !important;
  text-transform: none !important;
  border-radius: 6px !important;
  margin: 4px 4px 3px 4px !important;
  transition: all 0.2s ease !important;
  color: #666 !important;
}

.nav-btn:hover {
  transform: translateY(-2px);
  background: rgba(25, 118, 210, 0.08) !important;
  color: #1976d2 !important;
}

.nav-btn-active {
  background: #1976d2 !important;
  color: white !important;
}

.nav-btn-active:hover {
  background: #1565c0 !important;
  color: white !important;
}

.learning-card {
  cursor: pointer;
  transition: all 0.2s ease;
}

.learning-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.course-card {
  cursor: pointer;
  transition: all 0.2s ease;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

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

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

/* Vue Flow 预览样式 - 完全参考 RoadmapCard */
.vue-flow-preview {
  border-radius: 0 !important;
  overflow: hidden !important;
  border: none !important;
  background: linear-gradient(135deg, #fafafa 0%, #f5f7ff 100%) !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* 只读模式下隐藏连接点 - 参考 RoadmapCard */
.vue-flow-readonly :deep(.vue-flow__handle) {
  width: 0 !important;
  height: 0 !important;
  border: none !important;
  background: transparent !important;
}

/* 只读模式下美化默认节点 - 参考 RoadmapCard */
.vue-flow-readonly :deep(.vue-flow__node) {
  border-radius: 12px !important;
  background: #fafafa  !important;
  border: 3px solid #1976d2 !important;
  color: #1976d2 !important;
  font-weight: 500 !important;
  font-size: 0.85rem !important;
  transition: all 0.2s ease;
  cursor: pointer !important;
  padding: 6px 8px !important;
  align-items: center;
  justify-content: center;
}

/* 根节点特殊样式 - 参考 RoadmapCard */
.vue-flow-readonly :deep(.vue-flow__node[data-id="0"]) {
  background: #1976d2  !important;
  border: 3px solid #1976d2 !important;
  color: #ffffff !important;
  font-weight: 600 !important;
}

/* 已完成课程样式 */
.vue-flow-readonly :deep(.vue-flow__node.completed-course) {
  background: #e8f5e9 !important;
  border-color: #4caf50 !important;
  color: #2e7d32 !important;
}

/* 已完成课程标识 */
.vue-flow-readonly :deep(.vue-flow__node.completed-course::after) {
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
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

/* 有进度课程的背景进度填充 */
.vue-flow-readonly :deep(.vue-flow__node.progress-course) {
  background: linear-gradient(to right, #88ee76 var(--progress, 0%), #fafafa var(--progress, 0%)) !important;
  border: 3px solid #2fae19 !important;
  color: #333 !important;
}

.vue-flow-readonly :deep(.vue-flow__node[data-id="0"]:hover) {
  background: #1976d2  !important;
  border: 3px solid #1976d2 !important;
  color: #ffffff !important;
  font-weight: 600 !important;
}

.vue-flow-readonly :deep(.vue-flow__node:hover) {
  background: #e3f2fd  !important;
  border-color: #1976d2 !important;
  transform: translateY(-5px);
  color: #0d47a1 !important;
}

/* VueFlow 节点样式覆盖 */
:deep(.vue-flow__node) {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  font-size: 12px;
  font-weight: 500;
}

:deep(.vue-flow__node-default) {
  background: white;
  border: 2px solid #e0e0e0;
  padding: 8px 12px;
  min-width: 120px;
  text-align: center;
}

:deep(.vue-flow__node.selected) {
  border-color: #1976d2;
  box-shadow: 0 4px 12px rgba(25, 118, 210, 0.3);
}

/* 边样式 - 参考 RoadmapFlow */
:deep(.vue-flow__edge-path) {
  stroke: #9e9e9e;
  stroke-width: 2px;
}

:deep(.vue-flow__edge.selected .vue-flow__edge-path) {
  stroke: #1976d2;
  stroke-width: 3px;
}

/* 确保节点和边可以被选中 - 参考 RoadmapFlow */
:deep(.vue-flow__node) {
  cursor: pointer !important;
}

:deep(.vue-flow__edge) {
  cursor: pointer !important;
}

:deep(.vue-flow__controls) {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.vue-flow__controls-button) {
  border: none;
  border-radius: 4px;
  background: transparent;
  transition: background-color 0.2s;
}

:deep(.vue-flow__controls-button:hover) {
  background: #f5f5f5;
}

/* 投票动画效果 */
.vote-animation {
  animation: voteUp 0.3s ease;
}

@keyframes voteUp {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .vue-flow-container {
    height: 250px !important;
  }
  
  .vue-flow-preview {
    height: 200px !important;
  }
  
  :deep(.vue-flow__node-default) {
    min-width: 100px;
    font-size: 11px;
    padding: 6px 8px;
  }

  .roadmap-card .d-flex {
    flex-direction: column !important;
  }

  .roadmap-card .d-flex > div:last-child {
    width: 100% !important;
    min-width: 100% !important;
    height: 200px !important;
  }
}

/* 改善字体渲染和清晰度 */
* {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* 增强文字对比度和清晰度 */
.text-grey-darken-1,
.text-grey-darken-2,
.text-grey-darken-3,
.text-grey-darken-4 {
  font-weight: 500 !important;
}

/* 确保主要文字有足够的对比度 */
h1,
h2,
h3,
h4,
h5,
h6 {
  font-weight: 700 !important;
  letter-spacing: -0.01em;
}

/* 增加hover效果但保持flat风格 */
.v-btn:hover {
  transform: translateY(-1px);
  transition: transform 0.2s ease;
}

.v-chip:hover {
  transform: scale(1.02);
  transition: transform 0.2s ease;
}

/* 自定义滚动条样式 */
:deep(.v-responsive) {
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.1) transparent;
}

:deep(.v-responsive)::-webkit-scrollbar {
  width: 4px;
}

:deep(.v-responsive)::-webkit-scrollbar-track {
  background: transparent;
}

:deep(.v-responsive)::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

@keyframes shimmer {
  0% {
    background-position: -200px 0;
  }
  100% {
    background-position: calc(200px + 100%) 0;
  }
}

.skeleton-shimmer {
  background: linear-gradient(90deg, #fafafa 25%, #f0f0f0 50%, #fafafa 75%);
  background-size: 200px 100%;
  animation: shimmer 2.5s infinite;
}
</style>
