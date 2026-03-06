<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useFetch } from '@/composables'
import { useUserStore } from '@/stores/modules/user'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { statsApi } from '@/api/modules/stats'
import { courseApi, subscriptionApi } from '@/api/modules/course'
import type { PlatformStats } from '@/types/stats'
import type { UserDailyStatsDTO } from '@/types/user'
import type { Course } from '@/types/course'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

// 用户信息
const userName = computed(() => userStore.currentUser?.name || t('common.guest'))

// 用户学习统计数据
const stats = ref({
  coursesInProgress: 0, // 从用户订阅课程获取
  completedCourses: 0, // 暂时保持 mock，需要新接口
  careersInProgress: 0, // 暂时保持 mock，需要新接口
  learningDays: 0, // 累计学习天数
  reviewCards: 0, // 待复习卡片数
})

// 1. 加载平台统计数据（GET /api/v1/stats/platform）
const { data: platformStatsData } = useFetch<PlatformStats>({
  fetchFn: () => statsApi.getPlatformStats(),
  immediate: true,
  defaultValue: {
    courseCount: 0,
    careerPathCount: 0,
    roadmapCount: 0,
    knowledgeNodeCount: 0,
    articleCount: 0,
    lastUpdated: '',
  },
})

// 2. 加载用户今日统计（GET /api/v1/stats/users/{userId}/today）
const { data: userTodayStatsData } = useFetch<UserDailyStatsDTO>({
  fetchFn: () => {
    if (!userStore.userId) {
      return Promise.reject(new Error('用户未登录'))
    }
    return statsApi.getUserTodayStats(userStore.userId)
  },
  immediate: userStore.isLoggedIn,
})

// 3. 加载用户订阅的课程（GET /api/v1/users/{userId}/subscriptions）
const { data: userSubscriptionsData } = useFetch<Course[]>({
  fetchFn: () => {
    if (!userStore.userId) {
      return Promise.reject(new Error('用户未登录'))
    }
    return subscriptionApi.getUserSubscriptions(userStore.userId)
  },
  immediate: userStore.isLoggedIn,
  defaultValue: [],
})

// 4. 加载热门课程（GET /api/v1/courses/hot）
const { data: hotCoursesData } = useFetch<Course[]>({
  fetchFn: () => courseApi.getHotCourses(),
  immediate: true,
  defaultValue: [],
})

// 计算属性：平台统计数据
const platformStats = computed(() => platformStatsData.value || {
  courseCount: 0,
  careerPathCount: 0,
  roadmapCount: 0,
  knowledgeNodeCount: 0,
  articleCount: 0,
  lastUpdated: '',
})

// 计算属性：正在学习的课程（取前3个订阅的课程，如果没有则使用mock数据）
const recentCourses = computed(() => {
  const subscriptions = userSubscriptionsData.value || []
  if (subscriptions.length > 0) {
    return subscriptions.slice(0, 3)
  }
  // mock 数据
  return [
    {
      id: 101,
      name: 'JavaScript 入门到精通',
      description: '从零基础到熟练掌握 JS 核心概念',
      progress: 45,
      icon: 'mdi-language-javascript',
      iconColor: 'warning',
    },
    {
      id: 102,
      name: 'Vue.js 3 实战开发',
      description: '组件化开发与状态管理',
      progress: 30,
      icon: 'mdi-vuejs',
      iconColor: 'success',
    },
    {
      id: 103,
      name: 'TypeScript 完全指南',
      description: '类型系统与工程化实践',
      progress: 15,
      icon: 'mdi-language-typescript',
      iconColor: 'info',
    },
  ]
})

// 课程图标和颜色池
const courseIcons = [
  'mdi-code-braces',
  'mdi-database',
  'mdi-cloud',
  'mdi-cellphone',
  'mdi-web',
  'mdi-chart-line',
  'mdi-cog',
  'mdi-security',
]
const courseColors = ['primary', 'success', 'warning', 'info', 'purple', 'teal', 'orange', 'indigo']

// 根据ID获取课程图标
const getCourseIcon = (id: number) => courseIcons[id % courseIcons.length]
const getCourseColor = (id: number) => courseColors[id % courseColors.length]

// 计算属性：推荐课程（取前4个热门课程，如果没有则使用mock数据）
const recommendedCourses = computed(() => {
  const hotCourses = hotCoursesData.value || []
  if (hotCourses.length > 0) {
    return hotCourses.slice(0, 4)
  }
  // mock 数据
  return [
    { id: 201, name: 'Python 数据分析', learnerCount: 3200 },
    { id: 202, name: 'React 18 新特性', learnerCount: 2800 },
    { id: 203, name: 'Docker 容器化部署', learnerCount: 2100 },
    { id: 204, name: 'Node.js 后端开发', learnerCount: 1900 },
  ]
})

// 监听数据变化，更新统计数据
watch([userSubscriptionsData, userTodayStatsData], () => {
  // 更新正在学习的课程数
  if (userSubscriptionsData.value) {
    stats.value.coursesInProgress = userSubscriptionsData.value.length
  }

  // 更新今日学习统计
  if (userTodayStatsData.value) {
    // 目前后端只返回基础统计数据（views, twices, likes, comments）
    // 暂时不更新 todayMinutes，等待后端添加学习时长统计
    // stats.value.todayMinutes = userTodayStatsData.value.xxx
  }
})

// 快速入口 - 3步学习路径
const quickLinks = computed(() => [
  {
    step: 1,
    title: '探索职业',
    description: '职业方向与学习路线规划',
    icon: 'mdi-briefcase-variant',
    color: 'warning',
    path: '/career',
  },
  {
    step: 2,
    title: '学习课程',
    description: '自定义知识目录，社区筛选易懂内容',
    icon: 'mdi-book-multiple',
    color: 'info',
    path: '/courses',
  },
  {
    step: 3,
    title: '复习巩固',
    description: '科学复习，牢固掌握知识',
    icon: 'mdi-brain',
    color: 'success',
    path: '/review',
  },
])

// 复习数据（mock）
const reviewData = ref({
  todayTotal: 12,        // 今日待复习总数
  todayCompleted: 5,     // 今日已复习
  streakDays: 7,         // 连续复习天数
  decks: [
    {
      id: 1,
      title: 'JavaScript 基础概念',
      cardCount: 24,
      dueCount: 5,
      courseName: 'JavaScript 入门',
      icon: 'mdi-language-javascript',
      iconColor: 'warning',
    },
    {
      id: 2,
      title: 'Vue 3 核心知识点',
      cardCount: 18,
      dueCount: 3,
      courseName: 'Vue.js 实战',
      icon: 'mdi-vuejs',
      iconColor: 'success',
    },
    {
      id: 3,
      title: '数据结构与算法',
      cardCount: 32,
      dueCount: 4,
      courseName: '计算机基础',
      icon: 'mdi-sitemap',
      iconColor: 'info',
    },
  ],
})

// 正在学习的职业（暂时保持 mock，需要新接口）
const recentCareers = ref([
  {
    id: 1,
    careerId: 201,
    name: '前端工程师',
    progress: 65,
    icon: 'mdi-laptop',
    iconColor: 'info',
    description: '掌握现代前端技术栈',
    courseCount: 8,
    completedCourses: 5,
  },
  {
    id: 2,
    careerId: 202,
    name: 'UX设计师',
    progress: 30,
    icon: 'mdi-palette',
    iconColor: 'warning',
    description: '用户体验与界面设计',
    courseCount: 6,
    completedCourses: 2,
  },
  {
    id: 3,
    careerId: 203,
    name: '后端工程师',
    progress: 15,
    icon: 'mdi-server',
    iconColor: 'success',
    description: '服务端开发与架构设计',
    courseCount: 10,
    completedCourses: 1,
  },
])

// 推荐职业（mock）
const recommendedCareers = ref([
  {
    id: 1,
    careerId: 301,
    name: '数据分析师',
    icon: 'mdi-chart-bar',
    iconColor: 'purple',
    description: '数据驱动决策，洞察商业价值',
    courseCount: 6,
    learnerCount: 2340,
  },
  {
    id: 2,
    careerId: 302,
    name: '产品经理',
    icon: 'mdi-lightbulb-outline',
    iconColor: 'orange',
    description: '产品规划与用户需求分析',
    courseCount: 5,
    learnerCount: 1890,
  },
  {
    id: 3,
    careerId: 303,
    name: '全栈工程师',
    icon: 'mdi-layers-triple',
    iconColor: 'teal',
    description: '前后端全面发展',
    courseCount: 12,
    learnerCount: 3210,
  },
  {
    id: 4,
    careerId: 304,
    name: 'AI工程师',
    icon: 'mdi-robot',
    iconColor: 'pink',
    description: '人工智能与机器学习',
    courseCount: 8,
    learnerCount: 1560,
  },
])

// 最近活动
const recentActivities = ref([
  {
    id: 1,
    type: 'course',
    title: `${t('home.completed')} Python编程入门 第3章`,
    time: '2小时前',
    icon: 'mdi-check-circle',
    iconColor: 'success',
  },
  {
    id: 2,
    type: 'career',
    title: `${t('home.started')} 前端工程师 - React基础`,
    time: '昨天',
    icon: 'mdi-play-circle',
    iconColor: 'info',
  },
  {
    id: 3,
    type: 'achievement',
    title: `${t('home.achieved')}：连续学习7天`,
    time: '2天前',
    icon: 'mdi-trophy',
    iconColor: 'warning',
  },
  {
    id: 4,
    type: 'course',
    title: `${t('home.completed')} 数据结构与算法 练习题`,
    time: '3天前',
    icon: 'mdi-check-circle',
    iconColor: 'success',
  },
])

// 导航函数
const navigateTo = (path: string): void => {
  router.push(path)
}

const openCourse = (courseId: number): void => {
  router.push(`/courses/${courseId}`)
}

const openCareer = (careerId: number): void => {
  router.push(`/careers/${careerId}`)
}
</script>

<template>
  <DefaultLayout>
    <!-- 欢迎区域 -->
    <div class="welcome-section mb-4 mb-md-6">
      <div class="d-flex flex-column flex-md-row align-start align-md-center justify-space-between ga-4 ga-md-6">
        <!-- 左侧：用户信息 -->
        <div class="d-flex align-center ga-3 ga-sm-4 flex-grow-1" style="min-width: 0">
          <UserAvatar
            :name="userName"
            :avatar-url="userStore.currentUser?.avatar"
            :size="$vuetify.display.mobile ? 48 : 64"
            rounded="lg"
            avatar-class="flex-shrink-0"
          />
          <div style="min-width: 0">
            <h1
              class="text-h5 text-sm-h4 font-weight-bold text-truncate"
              :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
            >
              {{ t('home.greeting', { name: userName }) }}
            </h1>
            <p
              class="text-caption text-sm-body-2 mt-1 text-truncate"
              :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
            >
              {{ t('home.keepLearning') }}
            </p>
          </div>
        </div>

        <!-- 右侧：学习统计指标 -->
        <div class="d-flex align-center ga-3 ga-md-4 flex-shrink-0">
          <!-- 累计学习天数 - 突出显示 -->
          <div class="d-flex align-center ga-2">
            <v-icon icon="mdi-calendar-check" color="warning" size="24"></v-icon>
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-warning">
                {{ stats.learningDays }} <span class="text-body-2">天</span>
              </div>
              <div class="text-caption text-no-wrap text-medium-emphasis">
                累计学习
              </div>
            </div>
          </div>

          <!-- 分隔线 -->
          <v-divider vertical class="d-none d-sm-block stats-divider"></v-divider>

          <!-- 其他三个指标 -->
          <div class="d-flex align-center ga-4 ga-md-5">
            <!-- 进行中课程 -->
            <div class="text-center">
              <div class="text-h6 font-weight-bold" :style="{ color: 'rgb(var(--v-theme-on-surface))' }">
                {{ stats.coursesInProgress }}
              </div>
              <div class="text-caption text-no-wrap" :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }">
                进行中课程
              </div>
            </div>

            <!-- 进行中职业 -->
            <div class="text-center">
              <div class="text-h6 font-weight-bold" :style="{ color: 'rgb(var(--v-theme-on-surface))' }">
                {{ stats.careersInProgress }}
              </div>
              <div class="text-caption text-no-wrap" :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }">
                进行中职业
              </div>
            </div>

            <!-- 复习卡片数 -->
            <div class="text-center">
              <div class="text-h6 font-weight-bold" :style="{ color: 'rgb(var(--v-theme-on-surface))' }">
                {{ stats.reviewCards || 0 }}
              </div>
              <div class="text-caption text-no-wrap" :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }">
                待复习
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 平台介绍和学习路径 -->
    <div class="guide-section mb-6 mb-md-10">
      <div class="d-flex flex-column flex-md-row ga-6 ga-md-8">
        <!-- 左侧：标题 + 3个步骤 -->
        <div class="flex-grow-1">
          <!-- 标题和平台数据 -->
          <div class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between ga-3 mb-4 mb-md-6">
            <div>
              <h2
                class="text-h6 text-sm-h5 font-weight-bold mb-1"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ t('home.guideTitle') }}
              </h2>
              <p
                class="text-body-2 text-sm-body-1 ma-0"
                :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
              >
                {{ t('home.guideSubtitle') }}
              </p>
            </div>

            <!-- 平台数据 -->
            <div class="d-flex align-center ga-6 ga-md-10 flex-shrink-0">
              <div class="text-center">
                <div class="text-h6 font-weight-bold text-primary">
                  {{ platformStats.careerPathCount }} <span class="text-body-2">个</span>
                </div>
                <div class="text-caption text-medium-emphasis">职业方向</div>
              </div>
              <div class="text-center">
                <div class="text-h6 font-weight-bold text-primary">
                  {{ platformStats.courseCount }} <span class="text-body-2">门</span>
                </div>
                <div class="text-caption text-medium-emphasis">课程</div>
              </div>
              <div class="text-center">
                <div class="text-h6 font-weight-bold text-primary">
                  {{ platformStats.knowledgeNodeCount.toLocaleString() }} <span class="text-body-2">个</span>
                </div>
                <div class="text-caption text-medium-emphasis">知识节点</div>
              </div>
              <div class="text-center">
                <div class="text-h6 font-weight-bold text-primary">
                  {{ platformStats.articleCount.toLocaleString() }} <span class="text-body-2">篇</span>
                </div>
                <div class="text-caption text-medium-emphasis">文章</div>
              </div>
            </div>
          </div>

          <!-- 学习路径步骤 -->
          <div class="path-steps">
            <template v-for="(link, index) in quickLinks" :key="link.step">
              <div class="step-wrapper">
                <v-card class="step-card" rounded="lg" border hover @click="navigateTo(link.path)">
                  <div class="d-flex align-center ga-3">
                    <!-- 数字 -->
                    <v-avatar
                      color="grey-lighten-4"
                      size="40"
                      rounded="lg"
                      class="flex-shrink-0"
                    >
                      <span class="text-h6 font-weight-bold text-grey-darken-1">{{ link.step }}</span>
                    </v-avatar>

                    <!-- 文字 -->
                    <div class="flex-grow-1">
                      <h3
                        class="text-subtitle-1 font-weight-bold mb-1"
                        :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                      >
                        {{ link.title }}
                      </h3>
                      <p
                        class="text-caption ma-0"
                        :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
                      >
                        {{ link.description }}
                      </p>
                    </div>

                    <!-- 点击箭头 -->
                    <v-icon
                      icon="mdi-chevron-right"
                      size="20"
                      color="on-surface-variant"
                      class="flex-shrink-0"
                    ></v-icon>
                  </div>
                </v-card>
              </div>

              <!-- 箭头（独立元素） -->
              <div v-if="index < quickLinks.length - 1" class="step-arrow d-none d-md-flex">
                <v-icon icon="mdi-arrow-right-thick" color="grey-lighten-4" size="36"></v-icon>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 复习区域 -->
    <div class="review-section mb-6 mb-md-10">
      <div class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between ga-3 mb-4">
        <h2
          class="text-h6 text-sm-h5 font-weight-bold"
          :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
        >
          今日复习
        </h2>
        <div class="d-flex align-center ga-4">
          <div class="d-flex align-center ga-1">
            <v-icon icon="mdi-fire" color="warning" size="18"></v-icon>
            <span class="text-body-2 font-weight-medium">连续 {{ reviewData.streakDays }} 天</span>
          </div>
          <v-btn
            variant="tonal"
            color="success"
            rounded="lg"
            size="small"
            @click="navigateTo('/review')"
          >
            开始复习
            <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
          </v-btn>
        </div>
      </div>

      <!-- 复习进度和卡片组 -->
      <div class="d-flex flex-wrap ga-3">
        <!-- 今日进度卡片 -->
        <v-card rounded="lg" border class="review-progress-card">
          <v-card-text class="d-flex align-center ga-4 pa-3">
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-success">
                {{ reviewData.todayCompleted }}/{{ reviewData.todayTotal }}
              </div>
              <div class="text-caption text-medium-emphasis">今日进度</div>
            </div>
            <v-progress-circular
              :model-value="(reviewData.todayCompleted / reviewData.todayTotal) * 100"
              color="success"
              :size="48"
              :width="5"
            >
              <span class="text-caption font-weight-bold">{{ Math.round((reviewData.todayCompleted / reviewData.todayTotal) * 100) }}%</span>
            </v-progress-circular>
          </v-card-text>
        </v-card>

        <!-- 待复习卡片组列表 -->
        <v-card
          v-for="deck in reviewData.decks"
          :key="deck.id"
          rounded="lg"
          border
          hover
          class="review-deck-card"
          @click="navigateTo(`/review/${deck.id}`)"
        >
          <v-card-text class="pa-3">
            <div class="d-flex align-center ga-3">
              <div class="icon-container-sm flex-shrink-0">
                <v-icon :icon="deck.icon || 'mdi-cards-outline'" :color="deck.iconColor || 'success'" size="20"></v-icon>
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div
                  class="text-body-2 font-weight-bold mb-1 text-truncate"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ deck.title }}
                </div>
                <div class="text-caption text-medium-emphasis text-truncate">
                  {{ deck.courseName }}
                </div>
              </div>
              <v-chip size="small" color="success" variant="tonal" class="flex-shrink-0">
                {{ deck.dueCount }}
              </v-chip>
            </div>
          </v-card-text>
        </v-card>
      </div>
    </div>

    <!-- 正在跟踪的职业 -->
    <div class="career-section mb-6 mb-md-10">
      <div class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between ga-3 mb-4">
        <h2
          class="text-h6 text-sm-h5 font-weight-bold"
          :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
        >
          正在跟踪的职业路线
        </h2>
        <v-btn
          variant="text"
          size="small"
          :color="'on-surface'"
          @click="navigateTo('/career')"
        >
          探索更多
          <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
        </v-btn>
      </div>

      <!-- 职业卡片列表 -->
      <div class="d-flex flex-wrap ga-3">
        <v-card
          v-for="career in recentCareers"
          :key="career.id"
          rounded="lg"
          border
          hover
          class="career-card"
          @click="openCareer(career.careerId)"
        >
          <v-card-text class="pa-4">
            <div class="d-flex align-center ga-3 mb-3">
              <div class="icon-container flex-shrink-0">
                <v-icon :icon="career.icon" :color="career.iconColor" size="24"></v-icon>
              </div>
              <div class="flex-grow-1">
                <div
                  class="text-body-1 font-weight-bold mb-1"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ career.name }}
                </div>
                <div class="text-caption text-medium-emphasis">
                  {{ career.description }}
                </div>
              </div>
            </div>
            <div class="d-flex align-center justify-space-between mb-2">
              <span class="text-caption text-medium-emphasis">
                {{ career.completedCourses }}/{{ career.courseCount }} 门课程
              </span>
              <span class="text-caption font-weight-bold text-grey">
                {{ career.progress }}%
              </span>
            </div>
            <v-progress-linear
              :model-value="career.progress"
              color="grey-lighten-3"
              height="6"
              rounded
            ></v-progress-linear>
          </v-card-text>
        </v-card>
      </div>
    </div>

    <!-- 正在学习的课程 -->
    <div class="course-section mb-6 mb-md-10">
      <div class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between ga-3 mb-4">
        <h2
          class="text-h6 text-sm-h5 font-weight-bold"
          :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
        >
          正在学习的课程
        </h2>
        <v-btn
          variant="text"
          size="small"
          :color="'on-surface'"
          @click="navigateTo('/courses')"
        >
          浏览更多
          <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
        </v-btn>
      </div>

      <!-- 课程卡片列表 -->
      <div class="d-flex flex-wrap ga-3">
        <v-card
          v-for="course in recentCourses"
          :key="course.id"
          rounded="lg"
          border
          hover
          class="course-card"
          @click="openCourse(course.id)"
        >
          <v-card-text class="pa-4">
            <div class="d-flex align-center ga-3 mb-3">
              <div class="icon-container flex-shrink-0">
                <v-icon :icon="course.icon || 'mdi-book-open-variant'" :color="course.iconColor || 'info'" size="24"></v-icon>
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div
                  class="text-body-1 font-weight-bold mb-1 text-truncate"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ course.name }}
                </div>
                <div class="text-caption text-medium-emphasis text-truncate">
                  {{ course.description || '点击查看详情' }}
                </div>
              </div>
            </div>
            <div class="d-flex align-center justify-space-between mb-2">
              <span class="text-caption text-medium-emphasis">
                学习进度
              </span>
              <span class="text-caption font-weight-bold text-grey">
                {{ course.progress || 0 }}%
              </span>
            </div>
            <v-progress-linear
              :model-value="course.progress || 0"
              color="grey-lighten-3"
              height="6"
              rounded
            ></v-progress-linear>
          </v-card-text>
        </v-card>
      </div>
    </div>

    <!-- 推荐区域 -->
    <v-row class="mb-6 mb-md-10">
      <!-- 左列：推荐职业 -->
      <v-col cols="12" md="6">
        <div class="d-flex align-center justify-space-between mb-4">
          <h2
            class="text-h6 text-sm-h5 font-weight-bold"
            :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
          >
            推荐职业
          </h2>
          <v-btn
            variant="text"
            size="small"
            :color="'on-surface'"
            @click="navigateTo('/career')"
          >
            更多
            <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
          </v-btn>
        </div>
        <div class="d-flex flex-column ga-3">
          <v-card
            v-for="career in recommendedCareers"
            :key="career.id"
            rounded="lg"
            border
            hover
            @click="openCareer(career.careerId)"
          >
            <v-card-text class="pa-3">
              <div class="d-flex align-center ga-3">
                <div class="icon-container-sm flex-shrink-0">
                  <v-icon :icon="career.icon" :color="career.iconColor" size="20"></v-icon>
                </div>
                <div class="flex-grow-1" style="min-width: 0">
                  <div
                    class="text-body-1 font-weight-bold mb-1 text-truncate"
                    :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                  >
                    {{ career.name }}
                  </div>
                  <div class="text-caption text-medium-emphasis text-truncate">
                    {{ career.courseCount }} 门课程 · {{ career.learnerCount.toLocaleString() }} 人学习
                  </div>
                </div>
                <v-icon
                  icon="mdi-chevron-right"
                  size="20"
                  color="on-surface-variant"
                  class="flex-shrink-0"
                ></v-icon>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </v-col>

      <!-- 右列：推荐课程 -->
      <v-col cols="12" md="6">
        <div class="d-flex align-center justify-space-between mb-4">
          <h2
            class="text-h6 text-sm-h5 font-weight-bold"
            :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
          >
            推荐课程
          </h2>
          <v-btn
            variant="text"
            size="small"
            :color="'on-surface'"
            @click="navigateTo('/courses')"
          >
            更多
            <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
          </v-btn>
        </div>
        <div class="d-flex flex-column ga-3">
          <v-card
            v-for="course in recommendedCourses"
            :key="course.id"
            rounded="lg"
            border
            hover
            @click="openCourse(course.id)"
          >
            <v-card-text class="pa-3">
              <div class="d-flex align-center ga-3">
                <div class="icon-container-sm flex-shrink-0">
                  <v-icon :icon="getCourseIcon(course.id)" :color="getCourseColor(course.id)" size="20"></v-icon>
                </div>
                <div class="flex-grow-1" style="min-width: 0">
                  <div
                    class="text-body-1 font-weight-bold mb-1 text-truncate"
                    :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                  >
                    {{ course.name }}
                  </div>
                  <div class="text-caption text-medium-emphasis text-truncate">
                    {{ course.learnerCount?.toLocaleString() || 0 }} 人学习
                  </div>
                </div>
                <v-icon
                  icon="mdi-chevron-right"
                  size="20"
                  color="on-surface-variant"
                  class="flex-shrink-0"
                ></v-icon>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </v-col>
    </v-row>
  </DefaultLayout>
</template>

<style scoped>
.home-page {
  padding: 16px;
}

@media (min-width: 600px) {
  .home-page {
    padding: 24px;
  }
}

/* 欢迎区域 */
.welcome-section {
  padding-top: 16px;
  padding-bottom: 24px;
}

@media (min-width: 600px) {
  .welcome-section {
    padding-top: 24px;
    padding-bottom: 32px;
  }
}

@media (min-width: 960px) {
  .welcome-section {
    padding-top: 32px;
    padding-bottom: 40px;
  }
}

/* 统计指标分隔线 */
.stats-divider {
  align-self: stretch;
  opacity: 0.2;
  margin: 0 8px;
}

/* 平台统计卡片 */
.platform-stats-card {
  width: 100%;
}

@media (min-width: 960px) {
  .platform-stats-card {
    width: 200px;
  }
}

/* 学习路径步骤 */
.path-steps {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 16px;
}

@media (min-width: 960px) {
  .path-steps {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
  }
}

.step-wrapper {
  flex: 1;
  display: flex;
}

.step-card {
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
}

@media (min-width: 600px) {
  .step-card {
    padding: 16px;
  }
}

@media (min-width: 960px) {
  .step-card {
    padding: 16px;
  }
}

.step-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.step-arrow {
  padding: 0 8px;
  flex-shrink: 0;
}

.step-arrow .v-icon {
  animation: arrow-slide 2s ease-in-out infinite;
}

@keyframes arrow-slide {
  0%,
  100% {
    transform: translateX(-10px);
    opacity: 0.6;
  }
  50% {
    transform: translateX(10px);
    opacity: 1;
  }
}

/* 复习进度卡片 */
.review-progress-card {
  flex: 0 0 200px;
}

/* 复习卡片组 */
.review-deck-card {
  flex: 1 1 200px;
  min-width: 200px;
  max-width: 280px;
}

/* 职业卡片 */
.career-card {
  flex: 1 1 280px;
  min-width: 280px;
  max-width: 350px;
}

/* 课程卡片 */
.course-card {
  flex: 1 1 280px;
  min-width: 280px;
  max-width: 350px;
}

/* 图标容器 - 大尺寸 */
.icon-container {
  width: 48px;
  height: 48px;
  border: 1px solid rgb(var(--v-theme-outline));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 图标容器 - 小尺寸 */
.icon-container-sm {
  width: 40px;
  height: 40px;
  border: 1px solid rgb(var(--v-theme-outline));
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 推荐项 */
.recommend-item {
  transition: background-color 0.2s;
  border: 1px solid transparent;
}

.recommend-item:hover {
  background-color: rgb(var(--v-theme-surface-variant));
  border-color: rgb(var(--v-theme-outline));
}

/* 活动时间线 */
.activity-timeline {
  position: relative;
}

.activity-item {
  position: relative;
}

.activity-item:not(.last-item)::before {
  content: '';
  position: absolute;
  left: 11px;
  top: 24px;
  width: 2px;
  height: calc(100% + 4px);
  background: rgb(var(--v-theme-outline));
}

.activity-dot {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
}
</style>
