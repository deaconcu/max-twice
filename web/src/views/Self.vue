<script setup>
import { ref, onMounted, inject, watch, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { learnService } from '@/services/learnService'
import { userService } from '@/services/learnService'
import draggable from 'vuedraggable';
import { useUserStore } from "@/stores/user";
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import RoadmapDetail from '@/components/RoadmapDetail.vue';
import dagre from 'dagre';
import UserPosting from '../components/UserPosting.vue';
import Comment from '../components/Comment.vue';
import Tiptap from '../components/Tiptap.vue'
import RightSidebar from '@/components/RightSidebar.vue';


//const isLoggedIn = ref(false);
const route = useRoute();
const router = useRouter();
const user = useUserStore();

const showSnackbar = inject('showSnackbar');

const items = ref([
  { text: '个人信息', icon: 'mdi-information-outline', value: "info" },
  { text: '正在学习', icon: 'mdi-school-outline', value: "learning" },
  { text: '我关注的课程', icon: 'mdi-book-multiple-outline', value: "subscription" },
  { text: '我关注的人', icon: 'mdi-account-heart', value: "follow" },
  { text: '我创建的目录', icon: 'mdi-format-list-group', value: "contents" },
  { text: '我创建的文章', icon: 'mdi-file-document-outline', value: "article" },
])

const messages = ref([
  { text: '个人信息', name: 'ADMIN', date: "2025-03-05 22:55:22" },
  { text: '我关注的课程', name: 'ADMIN', date: "2025-03-05 21:55:22" },
  { text: '我关注的人', name: 'ADMIN', date: "2025-03-05 20:55:22" },
  { text: '我创建的目录', name: 'ADMIN', date: "2025-03-04 22:55:22" },
  { text: '我创建的文章', name: 'ADMIN', date: "2025-03-03 22:55:22" },
  { text: '我的消息', name: 'ADMIN', date: "2025-03-02 22:55:22" },
])

const follows = ref([
  { id: 1, name: 'Jaiden', description: 'why so serious' },
  { id: 2, name: '一只小鲤鱼', description: '我想去银河系游泳' },
])

const relatedLinks = ['高等数学', '概率论', 'C++编程实现', '软件测试']

if (!route.query || !route.query.tab) router.replace({ query: { tab: 'info' } })

const selected = computed(() => route.query.tab || 'info');

// 学习进度相关数据
const learningData = ref({
  totalProgress: 18,
  completedNodes: 2938,
  totalNodes: 29380,
  roadmaps: [], // roadmap posts that user added to learning
  courses: [],
  recentActivities: []
});

const selectedLearningTab = ref('roadmaps'); // 默认显示路线图

// RoadmapDetail 浮层状态
const showRoadmapDetail = ref(false);
const selectedRoadmap = ref(null);

const subscriptions = ref([]);
const subscriptionsCopy = ref([]);
const currPosting = ref(null);
const mainArea = ref('list');
const editorRef = ref(null);
const lastPage = ref('');

const years = ref([
  {
    color: 'cyan',
    year: '1960',
  },
  {
    color: 'green',
    year: '1970',
  },
  {
    color: 'pink',
    year: '1980',
  },
  {
    color: 'amber',
    year: '1990',
  },
  {
    color: 'orange',
    year: '2000',
  },
])

onMounted(() => {
  onTabChange(route.query.tab);
  // 检查是否有learningTab参数来设置学习子标签页
  if (route.query.learningTab) {
    selectedLearningTab.value = route.query.learningTab;
  }
});

watch(() => route.query.tab, (newValue, oldValue) => {
  //selected.value = newValue;
  onTabChange(newValue);
})

// 监听 learningTab 参数变化
watch(() => route.query.learningTab, (newValue) => {
  if (newValue && (newValue === 'roadmaps' || newValue === 'courses')) {
    selectedLearningTab.value = newValue;
  }
});

const onTabChange = (value) => {
  mainArea.value = 'list';
  if (value == "info") {
    loadUser();
  } else if (value == "learning") {
    loadLearningProgress();
  } else if (value == "subscription") {
    loadSubscription();
  } else if (value == "contents") {
    //loadContents(100000000);
  } else if (value == "article") {
    //loadArticle(100000000);
  } else if (value == "posting") {
    scrollPosition.value = window.scrollY;
    window.scrollTo(0, 0);
    //currPosting.value = posting;
  }
}

const scrollPosition = ref(0);
const listName = ref('');

const switchMainArea = (value, posting) => {
  lastPage.value = mainArea.value;
  if (value == "list") {
    mainArea.value = 'list';
    nextTick(() => {
      window.scrollTo(0, scrollPosition.value);
    });
  } else if (value == "edit") {
    mainArea.value = 'edit';
    currPosting.value = posting;
    scrollPosition.value = window.scrollY;
    window.scrollTo(0, 0);
  } else if (value == "detail") {
    mainArea.value = 'detail';
    currPosting.value = posting;
    scrollPosition.value = window.scrollY;
    window.scrollTo(0, 0);
  }
}

const switchToLastPage = () => {
  console.log("lastPage: " + lastPage.value);
  switchMainArea(lastPage.value, currPosting.value);
}

const info = ref({});

async function loadUser() {
  console.log("load user");
  try {
    let response = '';
    response = await userService.getSelf();

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      info.value = response.data;
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

async function PostUser() {
  console.log("post user");
  try {
    let response = '';
    response = await userService.postSelf(info.value.name, info.value.biography);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      showSnackbar("修改成功！")
      loadUser();
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

async function loadSubscription() {
  console.log("load subscription");
  try {
    let response = '';
    response = await userService.getSubscription(user.userId);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      subscriptions.value = response.data;
      subscriptionsCopy.value = JSON.parse(JSON.stringify(response.data));
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

async function saveSubscription() {
  console.log("save subscription");
  try {
    let response = '';
    const ids = subscriptions.value.map(item => item.id).join(',');
    response = await userService.putSubscription(ids);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      showSnackbar("修改成功！");
      user.setSubscription(response.data);
      loadSubscription();
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

const recoverSubscription = () => {
  subscriptions.value = JSON.parse(JSON.stringify(subscriptionsCopy.value));
}

const contentsList = ref([]);
const articleList = ref([]);

const lastContentsId = ref(0x7FFFFFFF);
const lastArticleId = ref(0x7FFFFFFF);

async function loadContents({ done }) {
  try {
    let response = '';
    response = await learnService.getUserContents(user.userId, lastContentsId.value);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      contentsList.value.push(...response.data);

      if (response.data.length > 0) {
        lastContentsId.value = response.data[response.data.length - 1].id;
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
}

async function loadArticle({ done }) {
  try {
    let response = '';
    response = await learnService.getUserArticle(user.userId, lastArticleId.value);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      articleList.value.push(...response.data);

      if (response.data.length > 0) {
        lastArticleId.value = response.data[response.data.length - 1].id;
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

const modifyPosting = async () => {
  try {
    console.log("begin post");

    const response = await learnService.putPosting(currPosting.value.id, editorRef.value.editor.getHTML());
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      currPosting.value.content = editorRef.value.editor.getHTML();
      switchToLastPage();
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const deletePosting = (id) => {
  contentsList.value = contentsList.value && contentsList.value.filter(item => item.id !== id);
  articleList.value = articleList.value && articleList.value.filter(item => item.id !== id);
}

const displayModifyName = ref(false);

const onModifyName = () => {
  displayModifyName.value = false;
  PostUser();
}

const displayModifyIntro = ref(false);

const onModifyIntro = () => {
  displayModifyIntro.value = false;
  PostUser();
}

const courseDescription = ref("");
const courseHoveringIndex = ref(-1);

const lastFolloweeId = ref("2100-01-01 00:00:01");
const followeeList = ref([]);
async function loadFollowee({ done }) {
  try {
    let response = '';
    response = await userService.getFolloweeList(user.userId, lastFolloweeId.value);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      followeeList.value.push(...response.data);

      if (response.data.length > 0) {
        lastFolloweeId.value = response.data[response.data.length - 1].createTime;
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

// 学习进度加载函数
const loadLearningProgress = async () => {
  try {
    // 并行加载用户学习的路线图数据和课程数据
    const [roadmapResponse, courseResponse] = await Promise.all([
      learnService.getUserRoadmaps(),
      learnService.getUserCourseList()
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
        
        // 首先进行布局计算
        const layoutedNodes = applyAutoLayout(nodes, edges, 'BT');
        
        // 计算完成的节点数和总节点数
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
          status: userRoadmap.status,
          startedAt: userRoadmap.startedAt,
          completedAt: userRoadmap.completedAt,
          tags: extractTags(roadmap.description),
          profession: roadmap.profession,
          nodes: layoutedNodes,
          edges: edges,
          content: generateRoadmapHTML(nodes) // 保留HTML内容以防需要
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
          difficulty: getDifficultyFromStatus(userCourse.status),
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

// 学习进度相关帮助函数
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

// 解析路线图内容 - 参考 Learning.vue 的实现
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

// 自动布局函数 - 参考 Learning.vue 的实现
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

// 其他帮助函数
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

const getDifficultyFromStatus = (status) => {
  switch (status) {
    case 'NOT_STARTED': return 'beginner';
    case 'IN_PROGRESS': return 'intermediate';
    case 'COMPLETED': return 'advanced';
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

// 根据用户名获取头像颜色 - 从 Learning.vue 复制
const getAvatarColor = (name) => {
  const colors = ['primary', 'success', 'info', 'warning', 'orange', 'purple', 'pink', 'indigo'];
  if (!name) return 'primary';
  
  const charCode = name.charCodeAt(0);
  return colors[charCode % colors.length];
};

// 格式化日期时间 - 从 Learning.vue 复制
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

// 计算学习时长 - 从 Learning.vue 复制
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

// 获取状态颜色 - 从 Learning.vue 复制
const getStatusColor = (status) => {
  switch (status) {
    case 'NOT_STARTED': return 'grey'
    case 'IN_PROGRESS': return 'primary'
    case 'COMPLETED': return 'success'
    default: return 'grey'
  }
};

// 获取状态图标 - 从 Learning.vue 复制
const getStatusIcon = (status) => {
  switch (status) {
    case 'NOT_STARTED': return 'mdi-circle-outline'
    case 'IN_PROGRESS': return 'mdi-play-circle'
    case 'COMPLETED': return 'mdi-check-circle'
    default: return 'mdi-circle-outline'
  }
};

// 获取状态文本 - 从 Learning.vue 复制
const getStatusText = (status) => {
  switch (status) {
    case 'NOT_STARTED': return '未开始'
    case 'IN_PROGRESS': return '进行中'
    case 'COMPLETED': return '已完成'
    default: return '未知状态'
  }
};

// 处理节点点击 - 从 Learning.vue 复制
const handleNodeClick = ({event, node}) => {
  console.log('Node clicked:', event, node);
  
  if (node.id === '0') {
    return
  }
  
  if (node.data.link) {
    window.open(node.data.link, '_blank')
  }
};

// 路线图投票功能 - 从 Learning.vue 复制
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

// 关闭/退出学习路线图 - 从 Learning.vue 复制
const closeRoadmap = (roadmap, event) => {
  if (event) {
    event.stopPropagation();
  }
  if (confirm('确定要退出这个学习路线图吗？')) {
    showSnackbar('已退出学习路线图');
  }
};

// 打开路线图详情浮层
const openRoadmapDetail = (roadmap) => {
  selectedRoadmap.value = roadmap;
  showRoadmapDetail.value = true;
};

const openCourse = (courseId) => {
  const url = router.resolve({ path: '/read', query: { courseId: courseId } }).href;
  window.open(url, '_blank');
};

const openRoadmap = (roadmapId) => {
  router.push({ name: 'roadmap', params: { professionId: roadmapId } });
};

</script>

<template>
  <v-container class="ma-0" fluid>
    <v-row no-gutters>
      <v-col cols="auto" class="pr-4 pt-6" style="width: 320px;">
        <!-- 更美观的左侧导航栏设计 -->
        <div class="sticky-left" style="position: sticky; top: 90px;">
          <!-- 用户信息卡片 -->
          <v-card class="user-profile-card mb-4" rounded="xl" elevation="0" color="grey-lighten-5">
            <v-card-text class="pa-5">
              <div class="text-center">
                <v-avatar size="64" class="mb-3 profile-avatar">
                  <v-img src="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg"></v-img>
                </v-avatar>
                <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ info?.name || '用户' }}</h3>
                <p class="text-body-2 text-grey-darken-2 mb-3">{{ info?.biography || '暂无简介' }}</p>
                
                <!-- 数据展示 -->
                <v-row class="ma-0" no-gutters>
                  <v-col cols="6" class="text-center">
                    <div class="stat-item">
                      <div class="text-h6 font-weight-bold text-primary">{{ user.subscription?.length || 0 }}</div>
                      <div class="text-caption text-grey-darken-1">关注课程</div>
                    </div>
                  </v-col>
                  <v-col cols="6" class="text-center">
                    <div class="stat-item">
                      <div class="text-h6 font-weight-bold text-success">{{ (contentsList?.length || 0) + (articleList?.length || 0) }}</div>
                      <div class="text-caption text-grey-darken-1">创建内容</div>
                    </div>
                  </v-col>
                </v-row>
              </div>
            </v-card-text>
          </v-card>

          <!-- 导航菜单卡片 -->
          <v-card class="navigation-card" rounded="xl" elevation="0" color="white">
            <v-card-text class="pa-3">
              <div class="nav-items">
                <div 
                  v-for="(item, i) in items" 
                  :key="i" 
                  class="nav-item-modern mb-2"
                  :class="{ 'nav-item-active-modern': selected === item.value }"
                  @click="router.push({ query: { tab: item.value } })">
                  
                  <div class="nav-item-content">
                    <div class="nav-icon-wrapper">
                      <v-icon :icon="item.icon" size="20" class="nav-icon-modern"></v-icon>
                    </div>
                    <span class="nav-title-modern">{{ item.text }}</span>
                    <v-icon 
                      v-if="selected === item.value" 
                      icon="mdi-chevron-right" 
                      size="16" 
                      color="primary" 
                      class="nav-arrow">
                    </v-icon>
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </v-col>

      <v-col cols="auto" class="flex-grow-1 d-flex justify-center">
        <div style="width: 720px; max-width: 800px;" class="py-6">
        <div v-if="mainArea == 'list'">
          <v-slide-y-reverse-transition hide-on-leave>
            <!-- info -->
            <div v-if="selected == 'info'">
              <div class="mb-5 px-3 text-grey d-flex align-center">
                <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
                <span class="text-body-2">点击图片修改头像，点击链接修改名称和介绍</span>
              </div>
              <v-row align="start" class="mt-12">
                <v-col cols="auto" class="text-end pe-6 border-e" style="padding-bottom: 130px;min-width: 135px;">
                  <div class="font-weight-bold">头像</div>
                </v-col>
                <v-col cols="9" class="ps-6">
                  <div class="">
                    <v-avatar image="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg" rounded="lg"
                      size="120" class="mb-6" style="border: 3px solid #333; padding: 0px"></v-avatar>
                  </div>
                </v-col>
              </v-row>

              <v-row align="center">
                <v-col cols="auto"style="min-width: 135px;" class="text-end pe-6 py-4 border-e">
                  <div class="font-weight-bold">名称</div>
                </v-col>
                <v-col cols="9" class="ps-6 py-0">
                  <div v-if="!displayModifyName" class="d-flex align-center">
                    {{ info.name }}
                    <v-btn @click="displayModifyName = true" prepend-icon="mdi-pencil" variant="plain" color="grey"
                      class="text-body-2 ps-8">修改</v-btn>
                  </div>
                  <div v-if="displayModifyName" class="d-flex align-baseline">
                    <v-text-field v-model="info.name" class="" hide-details density="compact" max-width="200"
                      variant="underlined"></v-text-field>
                    <v-btn @click="onModifyName" density="comfortable" prepend-icon="mdi-check" variant="plain"
                      color="grey" class="text-body-2 ps-8">确定</v-btn>
                  </div>
                </v-col>
              </v-row>

              <v-row align="center" class="">
                <v-col cols="auto" style="min-width: 135px;" class="text-end pe-6 py-4 border-e">
                  <div class="font-weight-bold">简单介绍自己</div>
                </v-col>
                <v-col cols="9" class="ps-6 py-0">
                  <div v-if="!displayModifyIntro" class="d-flex align-center">
                    {{ info.biography }}
                    <v-btn @click="displayModifyIntro = true" prepend-icon="mdi-pencil" variant="plain" color="grey"
                      class="text-body-2 ps-8">修改</v-btn>
                  </div>
                  <div v-if="displayModifyIntro" class="d-flex align-baseline">
                    <v-text-field v-model="info.biography" class="" hide-details density="compact" max-width="400"
                      variant="underlined"></v-text-field>
                    <v-btn @click="onModifyIntro" prepend-icon="mdi-check" variant="plain" color="grey"
                      class="text-body-2 ps-8">确定</v-btn>
                  </div>
                </v-col>
              </v-row>
            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>
            <!-- learning progress -->
            <div v-if="selected == 'learning'">
              <div class="mb-6">
                <h2 class="text-h5 font-weight-bold text-grey-darken-4 mb-2">我的学习</h2>
                <p class="text-body-2 text-grey-darken-2">管理您收藏的学习路线图和课程</p>
              </div>

              <!-- 统计卡片 -->
              <v-row class="mb-6">
                <v-col cols="3">
                  <v-card flat color="grey-lighten-5" rounded="lg">
                    <v-card-text class="text-center pa-4">
                      <v-icon icon="mdi-chart-line" color="primary" size="32" class="mb-2"></v-icon>
                      <h3 class="text-h4 font-weight-bold text-primary mb-1">{{ learningData.totalProgress }}%</h3>
                      <p class="text-body-2 text-grey-darken-2 mb-0">总体进度</p>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="3">
                  <v-card flat color="grey-lighten-5" rounded="lg">
                    <v-card-text class="text-center pa-4">
                      <v-icon icon="mdi-map" color="success" size="32" class="mb-2"></v-icon>
                      <h3 class="text-h4 font-weight-bold text-success mb-1">{{ learningData.roadmaps.length }}</h3>
                      <p class="text-body-2 text-grey-darken-2 mb-0">学习路线图</p>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="3">
                  <v-card flat color="grey-lighten-5" rounded="lg">
                    <v-card-text class="text-center pa-4">
                      <v-icon icon="mdi-book-multiple" color="warning" size="32" class="mb-2"></v-icon>
                      <h3 class="text-h4 font-weight-bold text-warning mb-1">{{ learningData.courses.length }}</h3>
                      <p class="text-body-2 text-grey-darken-2 mb-0">正在学习课程</p>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="3">
                  <v-card flat color="grey-lighten-5" rounded="lg">
                    <v-card-text class="text-center pa-4">
                      <v-icon icon="mdi-trophy" color="error" size="32" class="mb-2"></v-icon>
                      <h3 class="text-h4 font-weight-bold text-error mb-1">7</h3>
                      <p class="text-body-2 text-grey-darken-2 mb-0">连续学习天数</p>
                    </v-card-text>
                  </v-card>
                </v-col>
              </v-row>

              <!-- 标签页切换 -->
              <div class="mb-4">
                <v-btn-toggle v-model="selectedLearningTab" variant="outlined" color="primary" rounded="lg" density="comfortable">
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
            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>

            <!-- subscription -->
            <div v-if="selected == 'subscription'">
              <div class="mb-5 py-3 rounded text-grey d-flex align-center">
                <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
                <span class="text-body-2">将鼠标放置在课程上可以查看简介，操作删除，拖动图标可以切换顺序</span>
              </div>
              <v-item-group class="mt-3 mb-5" column>
                <v-item>
                  <draggable v-model="subscriptions" item-key="id" class="pt-5">
                    <template #item="{ element, index }">
                      <span>
                        <v-hover>
                          <template v-slot:default="{ isHovering, props }">
                            <v-chip variant="flat" v-bind="props" class="mr-4 mb-4 px-4 py-4 text-body-1"
                              :class="courseHoveringIndex == index ? 'bg-red-lighten-1' : 'bg-grey-lighten-4'"
                              @mouseenter="courseDescription = element.description; courseHoveringIndex = index">
                              <span class="font-weight-medium">{{ element.name }}</span>
                              <v-slide-x-transition hide-on-leave>
                                <v-icon @click="subscriptions.splice(index, 1);" v-if="isHovering"
                                  icon="mdi-close-circle-outline" class="ms-2"></v-icon>
                              </v-slide-x-transition>
                            </v-chip>
                          </template>
                        </v-hover>
                      </span>
                    </template>

                  </draggable>
                </v-item>
              </v-item-group>
              <div v-if="JSON.stringify(subscriptions) != JSON.stringify(subscriptionsCopy)">
                <v-btn variant="flat" color="teal" density="comfortable" class="mr-4 mt-2" @click="saveSubscription">
                  保存
                </v-btn>
                <v-btn variant="text" color="" density="comfortable" class="mr-4 mt-2" @click="recoverSubscription">
                  恢复
                </v-btn>
              </div>
              <v-divider class="mb-9 mt-9"></v-divider>
              <div class="">
                {{ courseDescription }}
              </div>
            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>

            <!-- follow -->
            <div v-if="selected == 'follow'">
              <div class="mb-5 py-3 rounded text-grey d-flex align-center">
                <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
                <span class="text-body-2">查看和管理您关注的用户</span>
              </div>
              <v-infinite-scroll :items="follows" :onLoad="loadFollowee" :key="selected" :no-more-text="'已经到底了'"
                style="position: relative;top:-12px">
                <v-list>
                  <v-list-item v-for="(item, i) in followeeList" :key="i" :value="item" class="mb-5 py-2">
                    <template v-slot:prepend>
                      <v-avatar icon="mdi-account" size="34" color="red">
                        <span class="text-body-1">CJ</span>
                      </v-avatar>
                    </template>

                    <v-list-item-title v-text="item.name"></v-list-item-title>
                    <v-list-item-subtitle v-text="item.biography"></v-list-item-subtitle>

                    <template v-slot:append>
                      <v-btn variant="text">取消关注</v-btn>
                    </template>
                  </v-list-item>
                </v-list>
                <template v-slot:empty>
                  <div class="text-body-2 text-grey py-5">已经到底了</div>
                </template>
              </v-infinite-scroll>
            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>

            <!-- contents -->
            <div v-if="selected == 'contents'">
              <v-infinite-scroll :items="contentsList" :onLoad="loadContents" :key="selected" :no-more-text="'已经到底了'"
                style="position: relative;top:-12px">
                <div v-for="(posting, index) in contentsList" :key="index">
                  <v-row class="ma-0 border-b px-0 pb-6" :class="{ 'pt-9': index != 0 }">
                    <div class="w-100 pb-8 d-flex justify-space-between align-end text-grey">
                      <div class="d-flex align-center text-body-1">
                        <a class="text-grey pe-2" :href="'/read?courseId=' + posting.node.course.id" target="_blank">
                          {{ posting.node.course.name }}
                        </a>
                        <v-icon icon="mdi-chevron-right" class="px-2 pe-2 text-body-1"></v-icon>
                        <a class="text-grey ps-2"
                          :href="'/read?courseId=' + posting.node.course.id + '&nodeId=' + posting.node.id"
                          target="_blank">
                          {{ posting.node.name }}
                        </a>
                      </div>
                    </div>

                    <UserPosting :posting="posting" :type="'list'" @switchMainArea="switchMainArea"
                      @deletePosting="deletePosting">
                    </UserPosting>

                  </v-row>
                </div>
                <template v-slot:empty>
                  <div class="text-body-2 text-grey py-5">已经到底了</div>
                </template>
              </v-infinite-scroll>

            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>

            <!-- article-->
            <div v-if="selected == 'article'">
              <v-infinite-scroll :items="articleList" :onLoad="loadArticle" :key="selected" :no-more-text="'已经到底了'"
                style="position: relative;top:-12px">
                <div v-for="(posting, index) in articleList" :key="index">

                  <v-row class="ma-0 border-b px-0 pb-7" :class="{ 'pt-10': index != 0 }">

                    <div class="w-100 pb-8 d-flex justify-space-between align-end text-grey">
                      <div class="d-flex align-center text-body-1">
                        <a class="text-grey pe-2" :href="'/read?courseId=' + posting.node.course.id" target="_blank">
                          {{ posting.node.course.name }}
                        </a>
                        <v-icon icon="mdi-chevron-right" class="px-2 pe-2 text-body-1"></v-icon>
                        <a class="text-grey ps-2"
                          :href="'/read?courseId=' + posting.node.course.id + '&nodeId=' + posting.node.id"
                          target="_blank">
                          {{ posting.node.name }}
                        </a>
                      </div>
                    </div>

                    <UserPosting :posting="posting" :type="'list'" @switchMainArea="switchMainArea"
                      @deletePosting="deletePosting">
                    </UserPosting>

                  </v-row>

                </div>

                <template v-slot:empty>
                  <div class="text-body-2 text-grey py-5">已经到底了</div>
                </template>
              </v-infinite-scroll>
            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>
            <div v-if="selected == 'message'" class="text-body-2">
              <div class="mb-10 text-grey-lighten-1 d-flex align-center">
                <v-icon icon="mdi-information-outline" start></v-icon>系统只保存30天内的消息，请及时查看
              </div>
              <v-row v-for="message in messages" key="" align="center">
                <v-col cols="2" class="text-end border-e pe-6 py-4">
                  <div class="pb-2">
                    <v-avatar image="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg" rounded="true"
                      size="20" class="me-3"></v-avatar>
                    {{ message.name }}
                  </div>
                  <div class="text-caption text-grey-lighten-1">{{ message.date }}</div>
                </v-col>
                <v-col cols="9" class="ps-6">
                  <v-badge content="NEW" size="small" color="red" dot offset-x="-2" offset-y="-2">
                    <div class="d-flex align-center border-thin border-dashed pa-3 rounded-lg d-inline-flex">
                      您的课程申请已被批准，课程在 [基础教育|数学] 的目录下，课程地址在<a href="">这里</a>，目录和文章的提交现已开放 {{ message.text }}
                    </div>
                  </v-badge>
                </v-col>
              </v-row>
            </div>
          </v-slide-y-reverse-transition>
        </div>

        <div v-if="mainArea == 'detail'">
          <UserPosting :posting="currPosting" :type="'detail'" @switchMainArea="switchMainArea"
            @deletePosting="deletePosting">
          </UserPosting>
          <v-row class="pa-0 ma-0 my-7">
            <Comment :posting="currPosting"></Comment>
          </v-row>
        </div>

        <div v-if="mainArea == 'edit'">
          <v-row class="mx-0 sticky-top mb-1" align="center"
            style="background-color: white;transform: translateX(-38px); width: 110%;">
            <v-btn variant="flat" class="me-0" color="" density="comfortable" icon="mdi-chevron-left"
              @click="switchToLastPage"></v-btn>
            <span class="ps-1 font-weight-bold text-body-1">修改文章</span>
          </v-row>
          <tiptap ref="editorRef" pathText="" :content="currPosting.content" />
          <div class="pt-1 pb-2 px-0" style="position: sticky; bottom: 0px; background-color: #fff;">
            <v-btn variant="tonal" color="teal" size="large" class="rounded-lg" block
              @click="modifyPosting">写好了，提交</v-btn>
          </div>
        </div>
        </div>
      </v-col>

      <v-col cols="3" class="ps-12 pt-6">
        <RightSidebar />
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
:deep(.sticky-top) {
  position: sticky;
  top: 49px;
  z-index: 10;
  height: 3.8vh;
  overflow-y: auto;
}

.v-infinite-scroll__side {
  display: none !important;
}

/* 新版导航样式 */
.user-profile-card {
  background: #fafafa;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.profile-avatar {
  border: 2px solid rgba(0, 0, 0, 0.1);
}

.stat-item {
  padding: 8px 0;
}

.navigation-card {
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.nav-item-modern {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.2s ease;
  padding: 0;
  border: 1px solid transparent;
}

.nav-item-modern:hover {
  background-color: rgba(25, 118, 210, 0.04);
  border-color: rgba(25, 118, 210, 0.1);
  transform: translateY(-1px);
}

.nav-item-active-modern {
  background-color: rgba(25, 118, 210, 0.08);
  border-color: rgba(25, 118, 210, 0.2);
}

.nav-item-content {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  width: 100%;
}

.nav-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background-color: rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  transition: all 0.2s ease;
}

.nav-item-modern:hover .nav-icon-wrapper {
  background-color: rgba(25, 118, 210, 0.1);
}

.nav-item-active-modern .nav-icon-wrapper {
  background-color: rgba(25, 118, 210, 0.15);
}

.nav-icon-modern {
  color: rgba(0, 0, 0, 0.7);
  transition: color 0.2s ease;
}

.nav-item-active-modern .nav-icon-modern {
  color: #1976d2;
}

.nav-title-modern {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.87);
  flex: 1;
  font-size: 14px;
}

.nav-item-active-modern .nav-title-modern {
  color: #1976d2;
  font-weight: 600;
}

.nav-arrow {
  margin-left: auto;
  opacity: 0.8;
}

/* 学习页面卡片样式 */
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

/* 状态标签样式 - 从 Learning.vue 复制 */
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

/* 关闭按钮样式 - 从 Learning.vue 复制 */
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

/* 时间信息样式 - 从 Learning.vue 复制 */
.time-info {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  padding: 8px 12px;
  margin-top: 8px;
}

/* 描述文字样式 - 最多三行 - 从 Learning.vue 复制 */
.description-text {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  max-height: calc(1.4em * 3);
}

/* Flat 卡片样式 - 从 Learning.vue 复制 */
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

/* Vue Flow 预览样式 - 从 Learning.vue 完全复制 */
.vue-flow-preview {
  border-radius: 0 !important;
  overflow: hidden !important;
  border: none !important;
  background: linear-gradient(135deg, #fafafa 0%, #f5f7ff 100%) !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* 只读模式下隐藏连接点 - 从 Learning.vue 复制 */
.vue-flow-readonly :deep(.vue-flow__handle) {
  width: 0 !important;
  height: 0 !important;
  border: none !important;
  background: transparent !important;
}

/* 只读模式下美化默认节点 - 从 Learning.vue 复制 */
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

/* 根节点特殊样式 - 从 Learning.vue 复制 */
.vue-flow-readonly :deep(.vue-flow__node[data-id="0"]) {
  background: #1976d2  !important;
  border: 3px solid #1976d2 !important;
  color: #ffffff !important;
  font-weight: 600 !important;
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

/* 边样式 - 从 Learning.vue 复制 */
:deep(.vue-flow__edge-path) {
  stroke: #9e9e9e;
  stroke-width: 2px;
}

:deep(.vue-flow__edge.selected .vue-flow__edge-path) {
  stroke: #1976d2;
  stroke-width: 3px;
}

/* 确保节点和边可以被选中 - 从 Learning.vue 复制 */
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

/* 投票动画效果 - 从 Learning.vue 复制 */
.vote-animation {
  animation: voteUp 0.3s ease;
}

@keyframes voteUp {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

/* 右侧卡片样式 */
.sticky-right {
  transition: all 0.3s ease;
}

/* 课程项悬停效果 */
.course-item {
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.course-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

/* 确保卡片无阴影 - 参考消息页面的flat设计 (不包括导航卡片和学习卡片) */
.v-card:not(.user-profile-card):not(.navigation-card):not(.flat-card) {
  box-shadow: none !important;
  border: 0px solid rgba(0, 0, 0, 0.08) !important;
  transition: all 0.2s ease;
}

.v-card:not(.user-profile-card):not(.navigation-card):not(.flat-card):hover {
  border-color: rgba(0, 0, 0, 0.12) !important;
}

/* 快速操作按钮间距 */
.gap-2 > * + * {
  margin-top: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sticky-left,
  .sticky-right {
    position: relative !important;
    top: unset !important;
    margin-bottom: 20px;
  }
  
  .pr-8,
  .ps-8 {
    padding-left: 16px !important;
    padding-right: 16px !important;
  }
}
</style>