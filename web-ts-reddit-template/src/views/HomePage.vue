<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'

const router = useRouter()

// Mock 系统统计数据
const platformStats = ref({
  totalCourses: 1280,
  totalNodes: 15600,
  totalCareers: 156,
  totalLearners: 89420,
  coursesGrowth: '+128',
  learnersGrowth: '+2.3k'
})

// Mock 用户数据
const userName = ref('张三')
const userAvatar = ref('')

// Mock 学习统计
const stats = ref({
  coursesInProgress: 5,
  careersInProgress: 3,
  completedCourses: 12,
  learningDays: 45,
  todayMinutes: 30
})

// 正在学习的课程
const recentCourses = ref([
  {
    id: 1,
    name: 'Python编程入门',
    progress: 45,
    icon: 'mdi-language-python',
    iconColor: 'blue'
  },
  {
    id: 2,
    name: '数据结构与算法',
    progress: 78,
    icon: 'mdi-chart-tree',
    iconColor: 'green'
  },
  {
    id: 3,
    name: '机器学习入门',
    progress: 23,
    icon: 'mdi-brain',
    iconColor: 'purple'
  }
])

// 正在学习的职业
const recentCareers = ref([
  {
    id: 1,
    name: '前端工程师',
    progress: 65,
    icon: 'mdi-laptop',
    iconColor: 'primary'
  },
  {
    id: 2,
    name: 'UX设计师',
    progress: 30,
    icon: 'mdi-account-heart',
    iconColor: 'success'
  }
])

// 推荐课程
const recommendedCourses = ref([
  {
    id: 10,
    name: '微积分基础',
    learnerCount: 8765,
    icon: 'mdi-function',
    iconColor: 'blue'
  },
  {
    id: 13,
    name: '英语语法精讲',
    learnerCount: 20123,
    icon: 'mdi-book-alphabet',
    iconColor: 'red'
  },
  {
    id: 18,
    name: 'UI设计基础',
    learnerCount: 13890,
    icon: 'mdi-cellphone',
    iconColor: 'blue'
  },
  {
    id: 6,
    name: 'MySQL数据库设计',
    learnerCount: 11234,
    icon: 'mdi-database',
    iconColor: 'teal'
  }
])

// 最近活动
const recentActivities = ref([
  {
    id: 1,
    type: 'course',
    title: '完成了 Python编程入门 第3章',
    time: '2小时前',
    icon: 'mdi-check-circle',
    iconColor: 'success'
  },
  {
    id: 2,
    type: 'career',
    title: '开始学习 前端工程师 - React基础',
    time: '昨天',
    icon: 'mdi-play-circle',
    iconColor: 'primary'
  },
  {
    id: 3,
    type: 'achievement',
    title: '获得成就：连续学习7天',
    time: '2天前',
    icon: 'mdi-trophy',
    iconColor: 'warning'
  },
  {
    id: 4,
    type: 'course',
    title: '完成了 数据结构与算法 练习题',
    time: '3天前',
    icon: 'mdi-check-circle',
    iconColor: 'success'
  }
])

// 快速入口 - 带流程引导和平台数据
const quickLinks = ref([
  {
    step: 1,
    title: '探索职业',
    description: '探索职业方向，制定职业发展路线',
    icon: 'mdi-briefcase-variant',
    color: 'orange',
    path: '/career',
    stat: '156个职业方向',
    statDetail: '覆盖技术、设计、管理等热门领域'
  },
  {
    step: 2,
    title: '浏览课程',
    description: '从课程中心选择感兴趣的课程开始学习',
    icon: 'mdi-book-multiple',
    color: 'teal',
    path: '/learning',
    stat: '1,280门课程',
    statDetail: '15,600个精心编排的知识节点'
  },
  {
    step: 3,
    title: '跟踪课程学习',
    description: '在我的课程中查看进度，持续学习',
    icon: 'mdi-book-open-variant',
    color: 'purple',
    path: '/my-courses',
    stat: '89,420+位学习者',
    statDetail: '每天2.3k+新增活跃用户'
  },
  {
    step: 4,
    title: '跟踪职业进度',
    description: '跟踪职业进度，实现职业目标',
    icon: 'mdi-flag-checkered',
    color: 'green',
    path: '/my-careers',
    stat: '85%完成率',
    statDetail: '系统化路径助力目标实现'
  }
])

// 导航函数
const navigateTo = (path: string): void => {
  router.push(path)
}

const openCourse = (courseId: number): void => {
  console.log('打开课程:', courseId)
  router.push('/my-courses')
}

const openCareer = (careerId: number): void => {
  console.log('打开职业:', careerId)
  router.push('/my-careers')
}
</script>

<template>
  <div class="home-page">
    <AppHeader />

    <v-container fluid class="page-content">
      <v-row class="mt-2 fill-height">
        <!-- 左侧导航栏 -->
        <v-col cols="auto" class="d-none d-lg-block pa-0">
          <LeftSidebar />
        </v-col>

        <!-- 主内容区 -->
        <v-col class="main-content pl-6">
          <!-- 欢迎区域 -->
          <v-card border rounded="xl" class="welcome-card pa-6 mb-6">
            <div class="d-flex align-center justify-space-between">
              <div class="d-flex align-center">
                <v-avatar color="primary" size="64" class="mr-4">
                  <v-icon icon="mdi-account" size="32" color="white"></v-icon>
                </v-avatar>
                <div>
                  <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">
                    你好，{{ userName }}！
                  </h1>
                  <p class="text-body-1 text-grey-darken-2">
                    今天也要继续加油学习哦 🎯
                  </p>
                </div>
              </div>

              <!-- 今日学习时间 -->
              <div class="text-center">
                <div class="text-h3 font-weight-bold text-primary">{{ stats.todayMinutes }}</div>
                <div class="text-caption text-grey">今日学习(分钟)</div>
              </div>
            </div>
          </v-card>

          <!-- 平台介绍和学习路径 -->
          <v-card border rounded="xl" class="platform-guide-card pa-6 mb-6">
            <!-- 涂鸦装饰元素 -->
            <div class="doodle-decoration doodle-dots"></div>

            <!-- 标题 -->
            <div class="text-center mb-6">
              <h2 class="text-h5 font-weight-bold text-grey-darken-4 mb-2">
                开始你的学习之旅
              </h2>
              <p class="text-body-1 text-grey-darken-2">
                4步开启系统化学习，海量资源助你实现职业目标
              </p>
            </div>

            <!-- 学习路径步骤 - 横向 -->
            <div class="path-steps-horizontal">
              <div
                v-for="(link, index) in quickLinks"
                :key="link.title"
                class="step-wrapper-horizontal"
              >
                <!-- 步骤卡片 -->
                <v-card
                  border
                  rounded="xl"
                  class="step-card-horizontal-new pa-4"
                  @click="navigateTo(link.path)"
                >
                  <!-- STEP标签 -->
                  <div class="text-center mb-2">
                    <v-chip :color="`${link.color}`" size="x-small" variant="flat">
                      <span class="text-caption font-weight-bold text-white">STEP {{ link.step }}</span>
                    </v-chip>
                  </div>

                  <!-- 图标 -->
                  <div class="text-center mb-3">
                    <v-avatar :color="`${link.color}-lighten-4`" size="56">
                      <v-icon :icon="link.icon" :color="link.color" size="28"></v-icon>
                    </v-avatar>
                  </div>

                  <!-- 标题 -->
                  <h3 class="text-body-1 font-weight-bold text-grey-darken-4 mb-2 text-center">
                    {{ link.title }}
                  </h3>

                  <!-- 描述 -->
                  <p class="text-caption text-grey-darken-2 mb-3 text-center step-desc-h">
                    {{ link.description }}
                  </p>

                  <!-- 平台数据 -->
                  <div class="platform-data-box">
                    <div class="text-body-2 font-weight-bold mb-1" :style="`color: var(--v-theme-${link.color})`">
                      {{ link.stat }}
                    </div>
                    <p class="text-caption text-grey-darken-1 mb-0">
                      {{ link.statDetail }}
                    </p>
                  </div>
                </v-card>

                <!-- 向右箭头 -->
                <div v-if="index < quickLinks.length - 1" class="arrow-right-h">
                  <v-icon icon="mdi-arrow-right-thick" color="primary" size="36"></v-icon>
                </div>
              </div>
            </div>
          </v-card>
          <v-row>
            <v-col cols="12" sm="6" md="3">
              <v-card border rounded="xl" class="stat-card pa-4">
                <div class="d-flex align-center">
                  <v-avatar color="primary-lighten-4" size="48" class="mr-3">
                    <v-icon icon="mdi-book-open-variant" color="primary" size="24"></v-icon>
                  </v-avatar>
                  <div>
                    <div class="text-h5 font-weight-bold">{{ stats.coursesInProgress }}</div>
                    <div class="text-caption text-grey">进行中课程</div>
                  </div>
                </div>
              </v-card>
            </v-col>
            <v-col cols="12" sm="6" md="3">
              <v-card border rounded="xl" class="stat-card pa-4">
                <div class="d-flex align-center">
                  <v-avatar color="success-lighten-4" size="48" class="mr-3">
                    <v-icon icon="mdi-check-circle" color="success" size="24"></v-icon>
                  </v-avatar>
                  <div>
                    <div class="text-h5 font-weight-bold">{{ stats.completedCourses }}</div>
                    <div class="text-caption text-grey">已完成课程</div>
                  </div>
                </div>
              </v-card>
            </v-col>
            <v-col cols="12" sm="6" md="3">
              <v-card border rounded="xl" class="stat-card pa-4">
                <div class="d-flex align-center">
                  <v-avatar color="orange-lighten-4" size="48" class="mr-3">
                    <v-icon icon="mdi-briefcase-variant" color="orange" size="24"></v-icon>
                  </v-avatar>
                  <div>
                    <div class="text-h5 font-weight-bold">{{ stats.careersInProgress }}</div>
                    <div class="text-caption text-grey">进行中职业</div>
                  </div>
                </div>
              </v-card>
            </v-col>
            <v-col cols="12" sm="6" md="3">
              <v-card border rounded="xl" class="stat-card pa-4">
                <div class="d-flex align-center">
                  <v-avatar color="purple-lighten-4" size="48" class="mr-3">
                    <v-icon icon="mdi-calendar-check" color="purple" size="24"></v-icon>
                  </v-avatar>
                  <div>
                    <div class="text-h5 font-weight-bold">{{ stats.learningDays }}</div>
                    <div class="text-caption text-grey">累计学习天数</div>
                  </div>
                </div>
              </v-card>
            </v-col>
          </v-row>

          <v-row>
            <!-- 左列：正在学习 -->
            <v-col cols="12" lg="6">
              <!-- 正在学习的课程 -->
              <v-card border rounded="xl" class="pa-5 mb-6">
                <div class="d-flex align-center justify-space-between mb-4">
                  <h2 class="text-h6 font-weight-bold text-grey-darken-4">正在学习的课程</h2>
                  <v-btn
                    variant="text"
                    color="primary"
                    size="small"
                    @click="navigateTo('/my-courses')"
                  >
                    查看全部
                    <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
                  </v-btn>
                </div>
                <div v-for="course in recentCourses" :key="course.id" class="mb-3">
                  <div
                    class="course-item pa-3 rounded-lg"
                    @click="openCourse(course.id)"
                  >
                    <div class="d-flex align-center mb-2">
                      <v-avatar :color="course.iconColor" size="40" class="mr-3">
                        <v-icon :icon="course.icon" color="white" size="20"></v-icon>
                      </v-avatar>
                      <div class="flex-grow-1">
                        <div class="text-body-1 font-weight-bold text-grey-darken-4">
                          {{ course.name }}
                        </div>
                        <div class="text-caption text-grey">进度: {{ course.progress }}%</div>
                      </div>
                    </div>
                    <v-progress-linear
                      :model-value="course.progress"
                      color="grey-lighten-1"
                      height="6"
                      rounded
                    ></v-progress-linear>
                  </div>
                </div>
              </v-card>

              <!-- 正在学习的职业 -->
              <v-card border rounded="xl" class="pa-5 mb-6">
                <div class="d-flex align-center justify-space-between mb-4">
                  <h2 class="text-h6 font-weight-bold text-grey-darken-4">正在学习的职业</h2>
                  <v-btn
                    variant="text"
                    color="primary"
                    size="small"
                    @click="navigateTo('/my-careers')"
                  >
                    查看全部
                    <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
                  </v-btn>
                </div>
                <div v-for="career in recentCareers" :key="career.id" class="mb-3">
                  <div
                    class="course-item pa-3 rounded-lg"
                    @click="openCareer(career.id)"
                  >
                    <div class="d-flex align-center mb-2">
                      <v-avatar :color="career.iconColor" size="40" class="mr-3">
                        <v-icon :icon="career.icon" color="white" size="20"></v-icon>
                      </v-avatar>
                      <div class="flex-grow-1">
                        <div class="text-body-1 font-weight-bold text-grey-darken-4">
                          {{ career.name }}
                        </div>
                        <div class="text-caption text-grey">进度: {{ career.progress }}%</div>
                      </div>
                    </div>
                    <v-progress-linear
                      :model-value="career.progress"
                      color="grey-lighten-1"
                      height="6"
                      rounded
                    ></v-progress-linear>
                  </div>
                </div>
              </v-card>
            </v-col>

            <!-- 右列：推荐和活动 -->
            <v-col cols="12" lg="6">
              <!-- 推荐课程 -->
              <v-card border rounded="xl" class="pa-5 mb-6">
                <div class="d-flex align-center justify-space-between mb-4">
                  <h2 class="text-h6 font-weight-bold text-grey-darken-4">推荐课程</h2>
                  <v-btn
                    variant="text"
                    color="primary"
                    size="small"
                    @click="navigateTo('/learning')"
                  >
                    更多
                    <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
                  </v-btn>
                </div>
                <div v-for="course in recommendedCourses" :key="course.id" class="mb-3">
                  <div class="recommend-item pa-3 rounded-lg">
                    <div class="d-flex align-center">
                      <v-avatar :color="course.iconColor" size="36" class="mr-3">
                        <v-icon :icon="course.icon" color="white" size="18"></v-icon>
                      </v-avatar>
                      <div class="flex-grow-1">
                        <div class="text-body-2 font-weight-bold text-grey-darken-4">
                          {{ course.name }}
                        </div>
                        <div class="text-caption text-grey">
                          <v-icon icon="mdi-account-multiple" size="12" class="mr-1"></v-icon>
                          {{ course.learnerCount }} 人学习
                        </div>
                      </div>
                      <v-btn
                        icon
                        variant="text"
                        size="small"
                        color="primary"
                      >
                        <v-icon icon="mdi-plus"></v-icon>
                      </v-btn>
                    </div>
                  </div>
                </div>
              </v-card>

              <!-- 最近活动 -->
              <v-card border rounded="xl" class="pa-5 mb-6">
                <h2 class="text-h6 font-weight-bold text-grey-darken-4 mb-4">最近活动</h2>
                <v-timeline side="end" density="compact" class="activity-timeline">
                  <v-timeline-item
                    v-for="activity in recentActivities"
                    :key="activity.id"
                    :dot-color="activity.iconColor"
                    size="small"
                  >
                    <template #icon>
                      <v-icon :icon="activity.icon" size="16"></v-icon>
                    </template>
                    <div class="pb-3">
                      <div class="text-body-2 text-grey-darken-4">{{ activity.title }}</div>
                      <div class="text-caption text-grey">{{ activity.time }}</div>
                    </div>
                  </v-timeline-item>
                </v-timeline>
              </v-card>
            </v-col>
          </v-row>
        </v-col>
      </v-row>
    </v-container>

    <AppFooter />
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background-color: #FAFBFC;
}

.page-content {
  position: relative;
  z-index: 1;
  max-width: 100%;
}

.main-content {
  padding-left: 24px;
  padding-right: 20px;
  flex: 1;
}

.welcome-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
}

.quick-links-section {
  padding: 20px;
  background-color: #FAFBFC;
  border-radius: 16px;
}

.learning-path-section {
  /* 路径引导区域 */
}

.path-container-horizontal {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0;
}

.path-step-horizontal {
  display: flex;
  align-items: center;
  flex: 1;
}

.step-card-horizontal {
  background-color: #FFFFFF;
  border: 2px solid #EDEFF1;
  transition: all 0.3s ease;
  cursor: pointer;
  width: 100%;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.step-card-horizontal:hover {
  border-color: rgb(var(--v-theme-primary));
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

.active-step {
  border-color: rgb(var(--v-theme-primary)) !important;
  background-color: #FFFFFF;
}

.path-arrow-horizontal {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  animation: slideRight 2s infinite;
  flex-shrink: 0;
}

@keyframes slideRight {
  0%, 100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(5px);
  }
}

.step-description {
  line-height: 1.4;
  min-height: 40px;
}

.path-container {
  max-width: 900px;
  margin: 0 auto;
}

.path-step {
  position: relative;
  cursor: pointer;
}

.step-card {
  background-color: #FFFFFF;
  border: 2px solid #EDEFF1;
  transition: all 0.3s ease;
}

.step-card:hover {
  border-color: rgb(var(--v-theme-primary));
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateX(4px);
}

.active-step {
  border-color: rgb(var(--v-theme-primary)) !important;
  background-color: #FFFFFF;
}

.step-number {
  flex-shrink: 0;
}

.path-arrow {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(5px);
  }
}

.quick-link-item {
  text-align: center;
  padding: 16px 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.quick-link-item:hover {
  transform: translateY(-6px);
}

.quick-link-item:hover .link-description {
  color: rgb(var(--v-theme-grey-darken-4));
}

.link-description {
  line-height: 1.3;
  font-size: 0.7rem;
}

.path-steps-horizontal {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.step-wrapper-horizontal {
  display: flex;
  align-items: center;
  flex: 1;
}

.step-card-horizontal-new {
  background-color: #FFFFFF;
  border: 2px solid #EDEFF1;
  transition: all 0.3s ease;
  cursor: pointer;
  width: 100%;
  min-height: 280px;
  display: flex;
  flex-direction: column;
}

.step-card-horizontal-new:hover {
  border-color: rgb(var(--v-theme-primary));
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-6px);
}

.active-step-h {
  border-color: rgb(var(--v-theme-primary)) !important;
  background-color: rgba(var(--v-theme-primary), 0.02);
}

.step-desc-h {
  line-height: 1.4;
  min-height: 48px;
}

.platform-data-box {
  background-color: #FAFBFC;
  padding: 12px;
  border-radius: 8px;
  text-align: center;
  margin-top: auto;
}

.arrow-right-h {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  animation: slideRight 2s infinite;
  flex-shrink: 0;
}

@keyframes slideRight {
  0%, 100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(8px);
  }
}

.path-steps-container {
  max-width: 800px;
  margin: 0 auto;
}

.path-step-wrapper {
  margin-bottom: 0;
}

.step-card-integrated {
  background-color: #FFFFFF;
  border: 2px solid #EDEFF1;
  transition: all 0.3s ease;
  cursor: pointer;
}

.step-card-integrated:hover {
  border-color: rgb(var(--v-theme-primary));
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateX(8px);
}

.active-step-integrated {
  border-color: rgb(var(--v-theme-primary)) !important;
  background-color: rgba(var(--v-theme-primary), 0.02);
}

.step-left {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.platform-data-inline {
  background-color: #FAFBFC;
  padding: 12px;
  border-radius: 8px;
  border-left: 3px solid currentColor;
}

.step-arrow-down {
  display: flex;
  justify-content: center;
  padding: 16px 0;
  animation: bounceDown 2s infinite;
}

@keyframes bounceDown {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(6px);
  }
}

.platform-guide-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
  position: relative;
  overflow: visible;
}

/* 左上角光晕效果 */
.platform-guide-card::before {
  content: '';
  position: absolute;
  top: -80px;
  left: -80px;
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgba(244, 67, 54, 0.15) 0%, rgba(244, 67, 54, 0.08) 40%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}

/* 波浪线已删除 */

.platform-guide-card > * {
  position: relative;
  z-index: 1;
}

/* 涂鸦装饰基础样式 */
.doodle-decoration {
  position: absolute;
  pointer-events: none;
  z-index: 0;
  opacity: 0.6;
}

/* 圆圈已删除 */
/* 之字形已删除 */

/* 涂鸦点点 - 右上角 */
.doodle-dots {
  top: 40px;
  right: 60px;
  width: 160px;
  height: 160px;
  background-image:
    /* 第一层 - 蓝色 */
    radial-gradient(circle, rgba(33, 150, 243, 0.45) 4px, transparent 4px),
    radial-gradient(circle, rgba(33, 150, 243, 0.4) 3px, transparent 3px),
    radial-gradient(circle, rgba(33, 150, 243, 0.35) 3px, transparent 3px),
    radial-gradient(circle, rgba(33, 150, 243, 0.4) 4px, transparent 4px),
    /* 第二层 - 绿色 */
    radial-gradient(circle, rgba(76, 175, 80, 0.45) 3px, transparent 3px),
    radial-gradient(circle, rgba(76, 175, 80, 0.4) 4px, transparent 4px),
    radial-gradient(circle, rgba(76, 175, 80, 0.35) 3px, transparent 3px),
    radial-gradient(circle, rgba(76, 175, 80, 0.4) 3px, transparent 3px),
    /* 第三层 - 橙色 */
    radial-gradient(circle, rgba(255, 152, 0, 0.45) 4px, transparent 4px),
    radial-gradient(circle, rgba(255, 152, 0, 0.4) 3px, transparent 3px),
    radial-gradient(circle, rgba(255, 152, 0, 0.35) 4px, transparent 4px),
    radial-gradient(circle, rgba(255, 152, 0, 0.4) 3px, transparent 3px),
    /* 第四层 - 紫色 */
    radial-gradient(circle, rgba(156, 39, 176, 0.45) 3px, transparent 3px),
    radial-gradient(circle, rgba(156, 39, 176, 0.4) 4px, transparent 4px),
    radial-gradient(circle, rgba(156, 39, 176, 0.35) 3px, transparent 3px),
    /* 第五层 - 红色 */
    radial-gradient(circle, rgba(244, 67, 54, 0.45) 4px, transparent 4px),
    radial-gradient(circle, rgba(244, 67, 54, 0.4) 3px, transparent 3px),
    radial-gradient(circle, rgba(244, 67, 54, 0.35) 3px, transparent 3px);
  background-size:
    12px 12px, 10px 10px, 11px 11px, 13px 13px,
    11px 11px, 12px 12px, 10px 10px, 13px 13px,
    13px 13px, 11px 11px, 12px 12px, 10px 10px,
    10px 10px, 12px 12px, 11px 11px,
    13px 13px, 11px 11px, 10px 10px;
  background-position:
    0 0, 6px 6px, 3px 9px, 9px 3px,
    12px 8px, 2px 5px, 15px 11px, 5px 14px,
    8px 1px, 14px 5px, 4px 12px, 11px 15px,
    16px 2px, 7px 10px, 13px 13px,
    1px 7px, 10px 4px, 6px 16px;
  transform: rotate(25deg);
}

.platform-stat {
  padding: 12px;
}

.step-card-inline {
  text-align: center;
  padding: 20px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: #FAFBFC;
  border-radius: 12px;
}

.step-card-inline:hover {
  background-color: #F0F1F2;
  transform: translateY(-4px);
}

.active-step-inline {
  background-color: rgba(var(--v-theme-primary), 0.08);
  border: 2px solid rgb(var(--v-theme-primary));
}

.step-desc {
  line-height: 1.4;
  min-height: 36px;
}

.path-arrow-inline {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  animation: slideRight 2s infinite;
  flex-shrink: 0;
}

.platform-intro-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
}

.stat-item {
  transition: all 0.2s ease;
  border-radius: 8px;
}

.stat-item:hover {
  background-color: #FAFBFC;
}

.stat-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.quick-link-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.course-item {
  background-color: #FAFBFC;
  cursor: pointer;
  transition: all 0.2s ease;
}

.course-item:hover {
  background-color: #F5F6F7;
  transform: translateX(4px);
}

.recommend-item {
  background-color: #FAFBFC;
  transition: all 0.2s ease;
}

.recommend-item:hover {
  background-color: #F5F6F7;
}

.activity-timeline {
  padding-left: 0;
}

/* 移动端 */
@media (max-width: 1280px) {
  .main-content {
    padding-left: 20px;
  }
}
</style>
