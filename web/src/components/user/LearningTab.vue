<script setup>
import { ref, computed, onMounted, inject } from 'vue';
import { useRouter } from 'vue-router';
import { progressServiceV1 } from '@/services/api/v1/apiServiceV1';
import { VueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import RoadmapDetail from '@/components/roadmap/RoadmapDetail.vue';
import dagre from 'dagre';
import { USER_ROADMAP_STATE} from '@/constants/statusConstants';
import { useUserStore } from "@/stores/user";

// Props
const props = defineProps({
  userId: {
    type: [String, Number],
    default: null
  }
});

const router = useRouter();
const showSnackbar = inject('showSnackbar');
const userStore = useUserStore();

// 当前操作的用户ID
const targetUserId = computed(() => props.userId || userStore.userId);

// 学习进度相关数据
const learningData = ref({
  totalProgress: 18,
  completedNodes: 2938,
  totalNodes: 29380,
  roadmaps: [],
  courses: [],
  recentActivities: []
});

const selectedLearningTab = ref('roadmaps');

// RoadmapDetail 浮层状态
const showRoadmapDetail = ref(false);
const selectedRoadmap = ref(null);

// 学习进度加载函数
const loadLearningProgress = async () => {
  try {
    const [roadmapResponse, courseResponse] = await Promise.all([
      progressServiceV1.getUserRoadmaps(targetUserId.value),
      progressServiceV1.getAllCourseProgress(targetUserId.value)
    ]);
    
    console.log('获取用户学习路线图数据:', roadmapResponse);
    console.log('获取用户学习课程数据:', courseResponse);
    
    let roadmaps = [];
    let courses = [];
    
    // 处理路线图数据
    if (roadmapResponse.code === 200 && Array.isArray(roadmapResponse.data)) {
      roadmaps = roadmapResponse.data.map(userRoadmap => {
        const roadmap = userRoadmap.roadmap;
        const { nodes, edges } = parseRoadmapContent(roadmap.content);
        
        const layoutedNodes = applyAutoLayout(nodes, edges, 'BT');
        
        const completedNodes = nodes.filter(node => node.data.completed).length;
        const totalNodes = nodes.length;
        const progress = totalNodes > 0 ? Math.round((completedNodes / totalNodes) * 100) : 0;
        
        return {
          id: roadmap.id,
          title: roadmap.description || `学习路线图 ${roadmap.id}`,
          description: roadmap.description || '暂无描述',
          author: roadmap.creator?.name || '未知用户',
          createdAt: roadmap.createdAt,
          addedDate: formatDate(userRoadmap.startedAt),
          vote: roadmap.vote || 0,
          upvoted: roadmap.upvoted || false,
          progress: userRoadmap.progressPercent || progress,
          completedNodes: completedNodes,
          totalNodes: totalNodes,
          lastActivity: getRelativeTime(userRoadmap.updatedAt),
          state: userRoadmap.state,
          startedAt: userRoadmap.startedAt,
          completedAt: userRoadmap.completedAt,
          tags: extractTags(roadmap.description),
          profession: roadmap.profession,
          nodes: layoutedNodes,
          edges: edges,
          content: generateRoadmapHTML(nodes)
        };
      });
    }
    
    // 处理课程数据
    if (courseResponse.code === 200 && Array.isArray(courseResponse.data)) {
      courses = courseResponse.data.map(userCourse => {
        return {
          id: userCourse.id,
          courseId: userCourse.course.id,
          title: userCourse.course.name,
          description: userCourse.course.description,
          progress: userCourse.progressPercent || 0,
          totalLessons: calculateTotalLessons(userCourse.course),
          completedLessons: calculateCompletedLessons(userCourse),
          category: getCategoryFromDescription(userCourse.course.description),
          difficulty: getDifficultyFromStatus(userCourse.state),
          estimatedTime: getEstimatedTime(userCourse.course.description),
          lastActivity: getRelativeTime(userCourse.updatedAt),
          instructor: userCourse.course.creator?.name || '未知讲师'
        };
      });
    }
    
    learningData.value = {
      totalProgress: calculateOverallProgress(roadmaps, courses),
      completedNodes: calculateCompletedNodes(roadmaps),
      totalNodes: calculateTotalNodes(roadmaps),
      roadmaps: roadmaps,
      courses: courses,
      recentActivities: generateRecentActivities([...roadmaps, ...courses])
    };

    console.log("learningData:", learningData.value);
  } catch (error) {
    console.error('Error loading learning data:', error);
    showSnackbar('加载学习数据失败');
  }
};

// 解析路线图内容
const parseRoadmapContent = (content) => {
  try {
    const data = typeof content === 'string' ? JSON.parse(content) : content;
    console.log('解析路线图内容数据:', data);
    
    if (!data || typeof data !== 'object') {
      console.warn('路线图内容数据无效:', data);
      return { nodes: [], edges: [] };
    }
    
    const nodes = (data.nodes || []).map((node, index) => {
      return {
        id: String(node.id || index),
        type: 'default',
        data: {
          label: node.label || node.data?.label || `节点 ${node.id || index}`,
          link: '/read?courseId=' + (node.id || index),
          completed: node.completed || node.data?.completed || false,
          current: node.current || node.data?.current || false,
          ...node.data
        },
        position: node.position || { x: 0, y: 0 },
        sourcePosition: 'top',
        targetPosition: 'bottom'
      };
    });

    const edges = (data.edges || []).map((edge, index) => ({
      id: `${edge.source}-${edge.target}`,
      source: String(edge.source),
      target: String(edge.target),
      type: edge.type || 'bezier',
      animated: edge.animated || true,
      label: edge.label
    }));

    return { nodes, edges };
  } catch (err) {
    console.error('解析路线图内容失败:', err, '原始内容:', content);
    return { nodes: [], edges: [] };
  }
};

// 自动布局函数
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
        y: nodeWithPosition.y - nodeHeight / 2
      }
    }
  })
};

// 帮助函数
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

const calculateTotalLessons = (course) => {
  const description = course.description || '';
  return Math.max(10, Math.floor(description.length / 10));
};

const calculateCompletedLessons = (userCourse) => {
  const totalLessons = calculateTotalLessons(userCourse.course);
  const progress = userCourse.progressPercent || 0;
  return Math.floor((progress / 100) * totalLessons);
};

const calculateOverallProgress = (roadmaps, courses) => {
  const allItems = [...roadmaps, ...courses];
  if (allItems.length === 0) return 0;
  
  const totalProgress = allItems.reduce((sum, item) => sum + (item.progress || 0), 0);
  return Math.round(totalProgress / allItems.length);
};

const calculateCompletedNodes = (roadmaps) => {
  return roadmaps.reduce((sum, roadmap) => {
    const progress = roadmap.progress || 0;
    const totalSteps = roadmap.totalSteps || 5;
    return sum + Math.floor((progress / 100) * totalSteps);
  }, 0);
};

const calculateTotalNodes = (roadmaps) => {
  return roadmaps.reduce((sum, roadmap) => sum + (roadmap.totalSteps || 5), 0);
};

const generateRecentActivities = (items) => {
  const activities = [];
  
  items.forEach(item => {
    if (item.progress > 0) {
      activities.push({
        type: item.totalSteps ? 'roadmap' : 'course',
        title: item.title,
        action: item.progress === 100 ? '已完成学习' : '正在学习中',
        time: item.lastActivity
      });
    }
  });
  
  return activities.slice(0, 5);
};

const getRelativeTime = (dateString) => {
  if (!dateString) return '未知时间';
  
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);
  
  if (diffMins < 60) {
    return `${diffMins}分钟前`;
  } else if (diffHours < 24) {
    return `${diffHours}小时前`;
  } else {
    return `${diffDays}天前`;
  }
};

const formatDate = (dateString) => {
  if (!dateString) return '未知日期';
  return new Date(dateString).toLocaleDateString('zh-CN');
};

const extractTags = (description) => {
  if (!description) return [];
  
  const commonTags = ['前端', 'Vue.js', 'JavaScript', 'React', 'Python', '数据科学', '机器学习', 'Java', 'Spring'];
  return commonTags.filter(tag => description.includes(tag)).slice(0, 3);
};

const getCategoryFromDescription = (description) => {
  if (!description) return 'other';
  const desc = description.toLowerCase();
  
  if (desc.includes('前端') || desc.includes('vue') || desc.includes('react') || desc.includes('javascript')) return 'frontend';
  if (desc.includes('数据') || desc.includes('python') || desc.includes('分析')) return 'datascience';
  if (desc.includes('ai') || desc.includes('机器学习') || desc.includes('深度学习')) return 'ai';
  if (desc.includes('java') || desc.includes('spring') || desc.includes('后端')) return 'backend';
  
  return 'other';
};

const getDifficultyFromStatus = (state) => {
  switch (state) {
    case USER_ROADMAP_STATE.NOT_STARTED: return 'beginner';
    case USER_ROADMAP_STATE.IN_PROGRESS: return 'intermediate';
    case USER_ROADMAP_STATE.COMPLETED: return 'advanced';
    default: return 'beginner';
  }
};

const getEstimatedTime = (description) => {
  if (!description) return '未知';
  
  const length = description.length;
  if (length < 50) return '1-2小时';
  if (length < 100) return '3-5小时';
  if (length < 200) return '1-2天';
  return '3-7天';
};

const generateRoadmapHTML = (nodes) => {
  if (!nodes || nodes.length === 0) {
    return '<p>暂无学习路径内容</p>';
  }
  
  const stepsHTML = nodes.map((node, index) => {
    const isCompleted = node.data.completed || false;
    const isCurrent = node.data.current || false;
    const stepClass = isCompleted ? 'completed' : isCurrent ? 'current' : '';
    
    return `
      <div class="path-step ${stepClass}">
        <div class="step-number">${index + 1}</div>
        <div class="step-content">
          <h4>${node.data.label || `步骤 ${index + 1}`}</h4>
          <p>${node.data.description || '学习相关内容'}</p>
          <div class="step-duration">预计时间: 1-2周</div>
        </div>
      </div>
    `;
  }).join('');
  
  return `
    <h3>学习路径</h3>
    <div class="learning-path">
      ${stepsHTML}
    </div>
  `;
};

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

const getStatusColor = (state) => {
  switch (state) {
    case USER_ROADMAP_STATE.NOT_STARTED: return 'grey'
    case USER_ROADMAP_STATE.IN_PROGRESS: return 'primary'
    case USER_ROADMAP_STATE.COMPLETED: return 'success'
    default: return 'grey'
  }
};

const getStatusIcon = (state) => {
  switch (state) {
    case USER_ROADMAP_STATE.NOT_STARTED: return 'mdi-circle-outline'
    case USER_ROADMAP_STATE.IN_PROGRESS: return 'mdi-play-circle'
    case USER_ROADMAP_STATE.COMPLETED: return 'mdi-check-circle'
    default: return 'mdi-circle-outline'
  }
};

const getStatusText = (state) => {
  const stateTexts = {
    [USER_ROADMAP_STATE.NOT_STARTED]: '未开始',
    [USER_ROADMAP_STATE.IN_PROGRESS]: '进行中',
    [USER_ROADMAP_STATE.COMPLETED]: '已完成'
  };
  return stateTexts[state] || '未知状态'
};

const handleNodeClick = ({event, node}) => {
  console.log('Node clicked:', event, node);
  
  if (node.id === '0') {
    return
  }
  
  if (node.data.link) {
    window.open(node.data.link, '_blank')
  }
};

const handleVoteRoadmap = async (roadmap, event) => {
  if (event) {
    event.stopPropagation();
  }
  try {
    roadmap.upvoted = !roadmap.upvoted;
    roadmap.vote += roadmap.upvoted ? 1 : -1;
    showSnackbar(roadmap.upvoted ? '点赞成功！' : '取消点赞');
  } catch (error) {
    console.error('投票失败:', error);
    showSnackbar('操作失败，请重试');
  }
};

const closeRoadmap = (roadmap, event) => {
  if (event) {
    event.stopPropagation();
  }
  if (confirm('确定要退出这个学习路线图吗？')) {
    showSnackbar('已退出学习路线图');
  }
};

const openRoadmapDetail = (roadmap) => {
  selectedRoadmap.value = roadmap;
  showRoadmapDetail.value = true;
};

const openCourse = (courseId) => {
  const url = router.resolve({ path: '/read', query: { courseId: courseId } }).href;
  window.open(url, '_blank');
};

// 组件挂载时加载数据
onMounted(() => {
  loadLearningProgress();
});
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="text-h5 font-weight-bold mb-2" style="color: #424242;">我的学习</h2>
      <p class="text-body-2" style="color: #424242;">管理您收藏的学习路线图和课程</p>
    </div>

    <!-- 统计卡片 -->
    <v-row class="mb-6">
      <v-col cols="3">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="text-center pa-4">
            <v-icon icon="mdi-chart-line" color="#424242" size="32" class="mb-2"></v-icon>
            <h3 class="text-h4 font-weight-bold mb-1" style="color: #424242;">{{ learningData.totalProgress }}%</h3>
            <p class="text-body-2 mb-0" style="color: #424242;">总体进度</p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="3">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="text-center pa-4">
            <v-icon icon="mdi-map" color="#424242" size="32" class="mb-2"></v-icon>
            <h3 class="text-h4 font-weight-bold mb-1" style="color: #424242;">{{ learningData.roadmaps.length }}</h3>
            <p class="text-body-2 mb-0" style="color: #424242;">学习路线图</p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="3">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="text-center pa-4">
            <v-icon icon="mdi-book-multiple" color="#424242" size="32" class="mb-2"></v-icon>
            <h3 class="text-h4 font-weight-bold mb-1" style="color: #424242;">{{ learningData.courses.length }}</h3>
            <p class="text-body-2 mb-0" style="color: #424242;">正在学习课程</p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="3">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="text-center pa-4">
            <v-icon icon="mdi-trophy" color="#424242" size="32" class="mb-2"></v-icon>
            <h3 class="text-h4 font-weight-bold mb-1" style="color: #424242;">7</h3>
            <p class="text-body-2 mb-0" style="color: #424242;">连续学习天数</p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- 标签页切换 -->
    <div class="mb-4">
      <v-btn-toggle v-model="selectedLearningTab" variant="outlined" color="#424242" rounded="lg" density="comfortable" class="learning-tab-toggle">
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
            <v-btn color="primary" variant="flat" rounded="lg" class="mt-4" @click="router.push('/roadmap')">
              <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
              浏览路线图
            </v-btn>
          </v-card-text>
        </v-card>
      </div>

      <div v-else>
        <div v-for="roadmap in learningData.roadmaps" :key="roadmap.id" class="mb-4">
          <v-card @click="openRoadmapDetail(roadmap)" variant="flat" class="flat-card roadmap-card position-relative">
            <!-- 学习状态标签 -->
            <div class="status-badge-container">
              <div class="d-flex align-center">
                <v-chip 
                  :color="getStatusColor(roadmap.state)" 
                  variant="flat" 
                  size="small"
                  class="status-badge">
                  <v-icon :icon="getStatusIcon(roadmap.state)" class="mr-1" size="14"></v-icon>
                  {{ getStatusText(roadmap.state) }}
                </v-chip>
                
                <!-- 进行中状态的关闭按钮 -->
                <v-btn 
                  v-if="roadmap.state === USER_ROADMAP_STATE.IN_PROGRESS"
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
                      color="primary"
                      @click="handleVoteRoadmap(roadmap, $event)">
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
                      <span class="ml-1 text-body-2">{{ roadmap.comment || 0 }}</span>
                      <v-tooltip activator="parent" location="top">查看评论</v-tooltip>
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
                    @click="openRoadmapDetail(roadmap)"
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
    <div v-if="selectedLearningTab === 'courses'">
      <div v-if="learningData.courses.length === 0" class="text-center py-8">
        <v-card flat color="grey-lighten-5" rounded="lg">
          <v-card-text class="py-8">
            <v-icon icon="mdi-book-search" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
            <h3 class="text-h6 text-grey-darken-2 mb-2">暂无学习课程</h3>
            <p class="text-body-2 text-grey-darken-1">去课程中心添加感兴趣的课程吧！</p>
            <v-btn color="primary" variant="flat" rounded="lg" class="mt-4" @click="router.push('/course/list')">
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
              @click="openCourse(course.id)">
              <v-card-text class="pa-4">
                <div class="d-flex align-start justify-space-between mb-3">
                  <div class="d-flex align-center">
                    <v-avatar color="teal-lighten-4" size="32" class="mr-3">
                      <v-icon :icon="getCategoryIcon(course.category)" color="teal-darken-2" size="16"></v-icon>
                    </v-avatar>
                    <div>
                      <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-1">{{ course.title }}</h4>
                      <p class="text-body-2 text-grey-darken-2 mb-0">{{ course.description }}</p>
                    </div>
                  </div>
                  <v-chip 
                    variant="flat" 
                    :color="getDifficultyColor(course.difficulty)"
                    size="small">
                    {{ course.difficulty === 'beginner' ? '初级' : course.difficulty === 'intermediate' ? '中级' : '高级' }}
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
                    rounded="lg">
                  </v-progress-linear>
                </div>

                <div class="d-flex justify-space-between align-center text-body-2 text-grey-darken-2">
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
      :roadmap="selectedRoadmap"
      @close="showRoadmapDetail = false" />
  </div>
</template>

<style scoped>
/* 从 Self.vue 复制的样式 */
.roadmap-card,
.course-card {
  cursor: pointer;
  transition: all 0.3s ease;
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

.vue-flow-readonly :deep(.vue-flow__handle) {
  width: 0 !important;
  height: 0 !important;
  border: none !important;
  background: transparent !important;
}

.vue-flow-readonly :deep(.vue-flow__node) {
  border-radius: 12px !important;
  background: #fafafa  !important;
  border: 3px solid #424242 !important;
  color: #424242 !important;
  font-weight: 500 !important;
  font-size: 0.85rem !important;
  transition: all 0.2s ease;
  cursor: pointer !important;
  padding: 6px 8px !important;
  align-items: center;
  justify-content: center;
}

.vue-flow-readonly :deep(.vue-flow__node[data-id="0"]) {
  background: #424242  !important;
  border: 3px solid #424242 !important;
  color: #ffffff !important;
  font-weight: 600 !important;
}

.vue-flow-readonly :deep(.vue-flow__node:hover) {
  background: #e3f2fd  !important;
  border-color: #424242 !important;
  transform: translateY(-5px);
  color: #424242 !important;
}

.vote-animation {
  animation: voteUp 0.3s ease;
}

@keyframes voteUp {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}
</style>