<script setup>
import { ref, onMounted, inject, watch, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { learnService } from '@/services/learnService'
import { userService } from '@/services/learnService'
import draggable from 'vuedraggable';
import { useUserStore } from "@/stores/user";
import UserPosting from '../components/UserPosting.vue';
import Comment from '../components/Comment.vue';
import Tiptap from '../components/Tiptap.vue'


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

if (!route.query || !route.query.tab) router.replace({ query: { tab: 'info', id: route.query.id } })

const selected = computed(() => route.query.tab || 'info');

const subscriptions = ref([]);
const subscriptionsCopy = ref([]);
const currPosting = ref(null);
const mainArea = ref('list');
const editorRef = ref(null);
const lastPage = ref('');

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
});

watch(() => route.query.tab, (newValue, oldValue) => {
  //selected.value = newValue;
  onTabChange(newValue);
})

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
    response = await userService.getUser(route.query.id);

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

async function loadSubscription() {
  console.log("load subscription");
  console.log("user: " + JSON.stringify(user));
  try {
    let response = '';
    response = await userService.getSubscription(route.query.id);

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

const loadLearningProgress = async () => {
  try {
    // 模拟加载学习数据 - 这里应该调用后端API获取用户收藏的roadmap posts和课程
    learningData.value = {
      totalProgress: 18,
      completedNodes: 2938,
      totalNodes: 29380,
      roadmaps: [
        {
          id: 1,
          title: '前端开发完整学习路线',
          author: '张老师',
          content: `
            <h3>前端开发学习路径</h3>
            <div class="learning-path">
              <div class="path-step completed">
                <div class="step-number">1</div>
                <div class="step-content">
                  <h4>HTML 基础</h4>
                  <p>学习HTML标签、语义化、表单等基础知识</p>
                  <div class="step-duration">预计时间: 2周</div>
                </div>
              </div>
              <div class="path-step completed">
                <div class="step-number">2</div>
                <div class="step-content">
                  <h4>CSS 样式设计</h4>
                  <p>掌握CSS选择器、布局、动画等技能</p>
                  <div class="step-duration">预计时间: 3周</div>
                </div>
              </div>
              <div class="path-step current">
                <div class="step-number">3</div>
                <div class="step-content">
                  <h4>JavaScript 编程</h4>
                  <p>学习JS基础语法、DOM操作、事件处理</p>
                  <div class="step-duration">预计时间: 4周</div>
                </div>
              </div>
              <div class="path-step">
                <div class="step-number">4</div>
                <div class="step-content">
                  <h4>Vue.js 框架</h4>
                  <p>掌握现代前端框架开发</p>
                  <div class="step-duration">预计时间: 6周</div>
                </div>
              </div>
              <div class="path-step">
                <div class="step-number">5</div>
                <div class="step-content">
                  <h4>项目实战</h4>
                  <p>完成完整的前端项目开发</p>
                  <div class="step-duration">预计时间: 8周</div>
                </div>
              </div>
            </div>
          `,
          progress: 40, // 当前完成的步骤百分比
          currentStep: 3,
          totalSteps: 5,
          addedDate: '2024-01-15',
          lastActivity: '2小时前',
          tags: ['前端', 'JavaScript', 'Vue.js']
        },
        {
          id: 2,
          title: '数据科学入门路线图',
          author: '李教授',
          content: `
            <h3>数据科学学习路径</h3>
            <div class="learning-path">
              <div class="path-step completed">
                <div class="step-number">1</div>
                <div class="step-content">
                  <h4>Python 基础</h4>
                  <p>学习Python语法和基础编程</p>
                  <div class="step-duration">预计时间: 3周</div>
                </div>
              </div>
              <div class="path-step current">
                <div class="step-number">2</div>
                <div class="step-content">
                  <h4>数据处理与分析</h4>
                  <p>掌握Pandas、NumPy等数据处理库</p>
                  <div class="step-duration">预计时间: 4周</div>
                </div>
              </div>
              <div class="path-step">
                <div class="step-number">3</div>
                <div class="step-content">
                  <h4>数据可视化</h4>
                  <p>学习Matplotlib、Seaborn等可视化工具</p>
                  <div class="step-duration">预计时间: 3周</div>
                </div>
              </div>
              <div class="path-step">
                <div class="step-number">4</div>
                <div class="step-content">
                  <h4>机器学习基础</h4>
                  <p>了解机器学习算法和应用</p>
                  <div class="step-duration">预计时间: 6周</div>
                </div>
              </div>
            </div>
          `,
          progress: 25,
          currentStep: 2,
          totalSteps: 4,
          addedDate: '2024-02-01',
          lastActivity: '1天前',
          tags: ['数据科学', 'Python', '机器学习']
        }
      ],
      courses: [
        {
          id: 1,
          title: 'Vue.js 3 组合式API',
          description: '现代Vue.js开发实践',
          progress: 80,
          totalLessons: 24,
          completedLessons: 19,
          category: 'frontend',
          difficulty: 'intermediate',
          estimatedTime: '4周',
          lastActivity: '30分钟前',
          instructor: '张老师'
        },
        {
          id: 2,
          title: 'Python数据分析基础',
          description: '使用Pandas和NumPy进行数据处理',
          progress: 45,
          totalLessons: 32,
          completedLessons: 14,
          category: 'datascience',
          difficulty: 'beginner',
          estimatedTime: '6周',
          lastActivity: '2小时前',
          instructor: '李老师'
        }
      ],
      recentActivities: [
        { type: 'course', title: 'Vue.js 3 组合式API', action: '完成了第19课', time: '30分钟前' },
        { type: 'roadmap', title: '前端开发完整学习路线', action: '进入第3步：JavaScript编程', time: '2小时前' },
        { type: 'achievement', title: '连续学习7天', action: '获得成就徽章', time: '1天前' }
      ]
    };
  } catch (error) {
    console.error('Error loading learning data:', error);
    showSnackbar('加载学习数据失败');
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
    response = await learnService.getUserContents(route.query.id, lastContentsId.value);

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
    response = await learnService.getUserArticle(route.query.id, lastArticleId.value);

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

const openCourse = (courseId) => {
  const url = router.resolve({ path: '/read', query: { courseId: courseId } }).href;
  window.open(url, '_blank');
};

const openRoadmap = (roadmapId) => {
  router.push({ name: 'roadmap', params: { professionId: roadmapId } });
};

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
    response = await userService.getFolloweeList(route.query.id, lastFolloweeId.value);

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
                      <div class="text-h6 font-weight-bold text-primary">{{ subscriptions?.length || 0 }}</div>
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
                  @click="router.push({ query: { tab: item.value, id: route.query.id } })">
                  
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
              <div class="mb-5 pa-3 bg-blue-lighten-5 rounded text-info d-flex align-center">
                <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
                <span class="text-body-2">查看用户的个人信息和活动</span>
              </div>
              <v-row align="start">
                <v-col cols="2" class="text-end pe-6 border-e" style="padding-bottom: 130px;">
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
                <v-col cols="2" class="text-end pe-6 py-4 border-e">
                  <div class="font-weight-bold">名称</div>
                </v-col>
                <v-col cols="9" class="ps-6 py-0">
                  <div v-if="!displayModifyName" class="d-flex align-center">
                    {{ info.name }}
                  </div>
                  <div v-if="displayModifyName" class="d-flex align-baseline">
                    <v-text-field v-model="info.name" class="" hide-details density="compact" max-width="200"
                      variant="underlined"></v-text-field>
                  </div>
                </v-col>
              </v-row>

              <v-row align="center" class="">
                <v-col cols="2" class="text-end pe-6 py-4 border-e">
                  <div class="font-weight-bold">简单介绍自己</div>
                </v-col>
                <v-col cols="9" class="ps-6 py-0">
                  <div v-if="!displayModifyIntro" class="d-flex align-center">
                    {{ info.biography }}
                  </div>
                  <div v-if="displayModifyIntro" class="d-flex align-baseline">
                    <v-text-field v-model="info.biography" class="" hide-details density="compact" max-width="400"
                      variant="underlined"></v-text-field>
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
                  <v-card v-for="roadmap in learningData.roadmaps" :key="roadmap.id" flat color="grey-lighten-5" rounded="lg" class="mb-4">
                    <v-card-text class="pa-0">
                      <!-- 路线图头部信息 -->
                      <div class="pa-4 border-b">
                        <div class="d-flex align-start justify-space-between mb-3">
                          <div class="flex-grow-1">
                            <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ roadmap.title }}</h3>
                            <p class="text-body-2 text-grey-darken-2 mb-2">作者：{{ roadmap.author }}</p>
                            <div class="d-flex align-center mb-2">
                              <v-chip v-for="tag in roadmap.tags" :key="tag" variant="flat" color="grey-lighten-3" size="small" class="mr-1">
                                {{ tag }}
                              </v-chip>
                            </div>
                          </div>
                          <div class="text-end">
                            <div class="text-primary text-h6 font-weight-bold mb-1">{{ roadmap.progress }}%</div>
                            <p class="text-body-2 text-grey-darken-2 mb-1">第{{ roadmap.currentStep }}/{{ roadmap.totalSteps }}步</p>
                            <p class="text-caption text-grey-darken-1">{{ roadmap.lastActivity }}</p>
                          </div>
                        </div>
                        
                        <!-- 进度条 -->
                        <v-progress-linear 
                          :model-value="roadmap.progress" 
                          color="primary" 
                          background-color="grey-lighten-3" 
                          height="8" 
                          rounded="lg">
                        </v-progress-linear>
                      </div>

                      <!-- 路线图内容 -->
                      <div class="pa-4">
                        <div v-html="roadmap.content" class="roadmap-content"></div>
                      </div>
                    </v-card-text>
                  </v-card>
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
              <div class="mb-5 text-grey-lighten-1 d-flex align-center text-body-2">
                <v-icon icon="mdi-information-outline" start></v-icon>将鼠标放置在课程上可以查看简介
              </div>

              <v-chip-group selected-class="text-primary" column>
                <v-hover>
                  <template v-slot:default="{ props }">
                    <v-chip variant="flat" v-bind="props" v-for="element, index in subscriptions" :key="index" class="mr-4 mb-4 px-4 py-4 text-body-1"
                      @mouseenter="courseDescription = element.description; courseHoveringIndex = index"
                      :class="courseHoveringIndex == index ? 'bg-red-lighten-1' : 'bg-grey-lighten-4'"
                      >
                      <span class="font-weight-medium">{{ element.name }}</span>
                    </v-chip>
                  </template>
                </v-hover>
              </v-chip-group>
             
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
                      <div class="d-flex align-center text-body-2">
                        <a class="text-grey pe-2" :href="'/read?courseId=' + posting.node.course.id" target="_blank">
                          {{ posting.node.course.name }}
                        </a>
                        <v-icon icon="mdi-chevron-right" class="px-2 pe-2 text-body-2"></v-icon>
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
                      <div class="d-flex align-center text-body-2">
                        <a class="text-grey pe-2" :href="'/read?courseId=' + posting.node.course.id" target="_blank">
                          {{ posting.node.course.name }}
                        </a>
                        <v-icon icon="mdi-chevron-right" class="px-2 pe-2 text-body-2"></v-icon>
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
            <Comment :object="currPosting" :type="0"></Comment>
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
        <!-- 参考消息页面设计的右侧边栏 -->
        <div class="sticky-right" style="position: sticky; top: 90px;">
          
          <!-- 用户统计卡片 -->
          <v-card flat color="grey-lighten-5" rounded="lg" class="mb-4">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-account-circle" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">用户信息</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">该用户的基本数据</p>
                </div>
              </div>

              <div class="d-flex justify-space-between align-center mb-2">
                <span class="text-body-2 text-grey-darken-3">关注课程</span>
                <span class="text-h6 font-weight-bold text-primary">{{ subscriptions?.length || 0 }}</span>
              </div>
              <div class="d-flex justify-space-between align-center mb-2">
                <span class="text-body-2 text-grey-darken-3">创建内容</span>
                <span class="text-h6 font-weight-bold text-primary">{{ postings?.length || 0 }}</span>
              </div>
              <div class="d-flex justify-space-between align-center mb-2">
                <span class="text-body-2 text-grey-darken-3">学习进度</span>
                <span class="text-h6 font-weight-bold text-success">{{ learningData.totalProgress }}%</span>
              </div>
              <div class="d-flex justify-space-between align-center">
                <span class="text-body-2 text-grey-darken-3">活跃状态</span>
                <span class="text-h6 font-weight-bold text-warning">在线</span>
              </div>
            </v-card-text>

            <v-card-actions class="px-4 pb-4">
              <v-btn variant="flat" color="grey-darken-2" rounded="lg" density="comfortable" class="w-100">
                <v-icon icon="mdi-account-plus" class="mr-2" size="16"></v-icon>
                关注用户
              </v-btn>
            </v-card-actions>
          </v-card>

          <!-- 互动操作卡片 -->
          <v-card flat color="grey-lighten-5" rounded="lg" class="mb-4">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-message-text" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">互动操作</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">与用户交流</p>
                </div>
              </div>

              <div class="d-flex flex-column gap-2">
                <v-btn variant="tonal" color="primary" rounded="lg" density="default" class="justify-start mb-2">
                  <v-icon icon="mdi-message" class="mr-2" size="16"></v-icon>
                  发送私信
                </v-btn>
                <v-btn variant="tonal" color="info" rounded="lg" density="default" class="justify-start mb-2">
                  <v-icon icon="mdi-share" class="mr-2" size="16"></v-icon>
                  分享用户
                </v-btn>
                <v-btn variant="tonal" color="success" rounded="lg" density="default" class="justify-start">
                  <v-icon icon="mdi-flag" class="mr-2" size="16"></v-icon>
                  举报用户
                </v-btn>
              </div>
            </v-card-text>
          </v-card>

          <!-- 相关用户 -->
          <v-card flat color="grey-lighten-5" rounded="lg">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-account-group" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">相似用户</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">您可能感兴趣的用户</p>
                </div>
              </div>

              <v-list class="bg-transparent pa-0" density="compact">
                <v-list-item 
                  v-for="user in relatedUsers" 
                  :key="user.id"
                  class="px-2 py-1 ma-0 rounded course-item" 
                  density="compact"
                  @click="router.push({ query: { tab: 'info', id: user.id } })">
                  <template v-slot:prepend>
                    <v-avatar size="24" class="mr-2">
                      <v-img :src="`https://picsum.photos/48/48?random=${user.id}`"></v-img>
                    </v-avatar>
                  </template>
                  <v-list-item-title class="text-body-2 text-grey-darken-3">{{ user.name }}</v-list-item-title>
                  <v-list-item-subtitle class="text-caption text-grey-darken-1">{{ user.description }}</v-list-item-subtitle>
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>
        </div>
      </v-col>
    </v-row>
  </v-container>
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

/* 学习页面卡片样式 */
.roadmap-card,
.course-card {
  cursor: pointer;
  transition: all 0.2s ease;
}

.roadmap-card:hover,
.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 路线图内容样式 */
.roadmap-content {
  font-family: inherit;
}

.roadmap-content .learning-path {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.roadmap-content .path-step {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  border: 2px solid #e0e0e0;
  background: white;
  transition: all 0.2s ease;
}

.roadmap-content .path-step.completed {
  border-color: #4caf50;
  background: #f1f8e9;
}

.roadmap-content .path-step.current {
  border-color: #2196f3;
  background: #e3f2fd;
}

.roadmap-content .step-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e0e0e0;
  color: #666;
  font-weight: bold;
  font-size: 14px;
  flex-shrink: 0;
}

.roadmap-content .path-step.completed .step-number {
  background: #4caf50;
  color: white;
}

.roadmap-content .path-step.current .step-number {
  background: #2196f3;
  color: white;
}

.roadmap-content .step-content {
  flex: 1;
}

.roadmap-content .step-content h4 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.roadmap-content .step-content p {
  margin: 0 0 8px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.4;
}

.roadmap-content .step-duration {
  font-size: 12px;
  color: #999;
  font-weight: 500;
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

.course-item {
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.course-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
}
</style>