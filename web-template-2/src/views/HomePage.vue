<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
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
    iconColor: '#2563EB'
  },
  {
    id: 2,
    name: '数据结构与算法',
    progress: 78,
    icon: 'mdi-chart-tree',
    iconColor: '#10B981'
  },
  {
    id: 3,
    name: '机器学习入门',
    progress: 23,
    icon: 'mdi-brain',
    iconColor: '#8B5CF6'
  }
])

// 正在学习的职业
const recentCareers = ref([
  {
    id: 1,
    name: '前端工程师',
    progress: 65,
    icon: 'mdi-laptop',
    iconColor: '#2563EB'
  },
  {
    id: 2,
    name: 'UX设计师',
    progress: 30,
    icon: 'mdi-account-heart',
    iconColor: '#10B981'
  }
])

// 推荐课程
const recommendedCourses = ref([
  {
    id: 10,
    name: '微积分基础',
    learnerCount: 8765,
    icon: 'mdi-function',
    iconColor: '#2563EB'
  },
  {
    id: 13,
    name: '英语语法精讲',
    learnerCount: 20123,
    icon: 'mdi-book-alphabet',
    iconColor: '#EF4444'
  },
  {
    id: 18,
    name: 'UI设计基础',
    learnerCount: 13890,
    icon: 'mdi-cellphone',
    iconColor: '#2563EB'
  },
  {
    id: 6,
    name: 'MySQL数据库设计',
    learnerCount: 11234,
    icon: 'mdi-database',
    iconColor: '#14B8A6'
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
    iconColor: '#10B981'
  },
  {
    id: 2,
    type: 'career',
    title: '开始学习 前端工程师 - React基础',
    time: '昨天',
    icon: 'mdi-play-circle',
    iconColor: '#2563EB'
  },
  {
    id: 3,
    type: 'achievement',
    title: '获得成就：连续学习7天',
    time: '2天前',
    icon: 'mdi-trophy',
    iconColor: '#F59E0B'
  },
  {
    id: 4,
    type: 'course',
    title: '完成了 数据结构与算法 练习题',
    time: '3天前',
    icon: 'mdi-check-circle',
    iconColor: '#10B981'
  }
])

// 快速入口 - 带流程引导和平台数据
const quickLinks = ref([
  {
    step: 1,
    title: '探索职业',
    description: '探索职业方向，制定职业发展路线',
    icon: 'mdi-briefcase-variant',
    color: '#F59E0B',
    path: '/career',
    stat: '156个职业方向',
    statDetail: '覆盖技术、设计、管理等热门领域'
  },
  {
    step: 2,
    title: '浏览课程',
    description: '从课程中心选择感兴趣的课程开始学习',
    icon: 'mdi-book-multiple',
    color: '#14B8A6',
    path: '/learning',
    stat: '1,280门课程',
    statDetail: '15,600个精心编排的知识节点'
  },
  {
    step: 3,
    title: '跟踪课程学习',
    description: '在我的课程中查看进度，持续学习',
    icon: 'mdi-book-open-variant',
    color: '#8B5CF6',
    path: '/my-courses',
    stat: '89,420+位学习者',
    statDetail: '每天2.3k+新增活跃用户'
  },
  {
    step: 4,
    title: '跟踪职业进度',
    description: '跟踪职业进度，实现职业目标',
    icon: 'mdi-flag-checkered',
    color: '#10B981',
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
  router.push(`/read/${courseId}`)
}

const openCareer = (careerId: number): void => {
  console.log('打开职业:', careerId)
  router.push('/roadmap/1/detail/2')
}
</script>

<template>
  <div class="home-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
      <!-- 欢迎区域 -->
      <div class="welcome-card">
        <div class="welcome-content">
          <div class="welcome-left">
            <v-avatar color="grey-lighten-3" size="64" rounded="lg">
              <v-icon size="32" color="#666666">mdi-account</v-icon>
            </v-avatar>
            <div>
              <h1 class="text-h4 font-weight-bold text-grey-darken-4">你好，{{ userName }}！</h1>
              <p class="text-body-2 text-grey-darken-2 mt-1">今天也要继续加油学习哦 🎯</p>
            </div>
          </div>
          <div class="today-stats">
            <div class="today-number">{{ stats.todayMinutes }}</div>
            <div class="today-label">今日学习(分钟)</div>
          </div>
        </div>
      </div>

      <!-- 平台介绍和学习路径 -->
      <div class="platform-guide-card">
        <div class="guide-header">
          <h2 class="guide-title">开始你的学习之旅</h2>
          <p class="guide-subtitle">4步开启系统化学习，海量资源助你实现职业目标</p>
        </div>

        <!-- 学习路径步骤 -->
        <div class="path-steps">
          <div
            v-for="(link, index) in quickLinks"
            :key="link.title"
            class="step-wrapper"
          >
            <v-card class="step-card" rounded="lg" border @click="navigateTo(link.path)">
              <!-- STEP标签 -->
              <v-chip class="step-badge" size="small" variant="flat" color="grey-lighten-3">
                STEP {{ link.step }}
              </v-chip>

              <!-- 图标 -->
              <v-avatar color="grey-lighten-4" size="56" rounded="lg" class="mx-auto mb-4">
                <v-icon :icon="link.icon" :color="link.color" size="28"></v-icon>
              </v-avatar>

              <!-- 标题和描述 -->
              <h3 class="step-title">{{ link.title }}</h3>
              <p class="step-desc">{{ link.description }}</p>

              <!-- 平台数据 -->
              <v-card class="step-data" rounded="lg" border variant="outlined">
                <div class="step-stat" :style="{ color: link.color }">{{ link.stat }}</div>
                <p class="step-detail">{{ link.statDetail }}</p>
              </v-card>
            </v-card>

            <!-- 箭头 -->
            <div v-if="index < quickLinks.length - 1" class="step-arrow">
              <v-icon icon="mdi-arrow-right-thick" color="#666666" size="36"></v-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-grid">
        <v-card class="stat-card" rounded="lg" border>
          <v-avatar color="grey-lighten-4" size="48" rounded="lg">
            <v-icon icon="mdi-book-open-variant" color="#2563EB" size="24"></v-icon>
          </v-avatar>
          <div class="stat-content">
            <div class="stat-value">{{ stats.coursesInProgress }}</div>
            <div class="stat-label">进行中课程</div>
          </div>
        </v-card>

        <v-card class="stat-card" rounded="lg" border>
          <v-avatar color="grey-lighten-4" size="48" rounded="lg">
            <v-icon icon="mdi-check-circle" color="#10B981" size="24"></v-icon>
          </v-avatar>
          <div class="stat-content">
            <div class="stat-value">{{ stats.completedCourses }}</div>
            <div class="stat-label">已完成课程</div>
          </div>
        </v-card>

        <v-card class="stat-card" rounded="lg" border>
          <v-avatar color="grey-lighten-4" size="48" rounded="lg">
            <v-icon icon="mdi-briefcase-variant" color="#F59E0B" size="24"></v-icon>
          </v-avatar>
          <div class="stat-content">
            <div class="stat-value">{{ stats.careersInProgress }}</div>
            <div class="stat-label">进行中职业</div>
          </div>
        </v-card>

        <v-card class="stat-card" rounded="lg" border>
          <v-avatar color="grey-lighten-4" size="48" rounded="lg">
            <v-icon icon="mdi-calendar-check" color="#8B5CF6" size="24"></v-icon>
          </v-avatar>
          <div class="stat-content">
            <div class="stat-value">{{ stats.learningDays }}</div>
            <div class="stat-label">累计学习天数</div>
          </div>
        </v-card>
      </div>

      <!-- 主要内容区 -->
      <div class="content-grid">
        <!-- 左列 -->
        <div class="content-left">
          <!-- 正在学习的课程 -->
          <v-card class="section-card" rounded="lg" border>
            <div class="section-header">
              <h2 class="section-title">正在学习的课程</h2>
              <button class="section-link" @click="navigateTo('/my-courses')">
                查看全部
                <v-icon icon="mdi-arrow-right" size="16"></v-icon>
              </button>
            </div>
            <v-card v-for="course in recentCourses" :key="course.id" class="course-item" rounded="lg" border @click="openCourse(course.id)">
              <div class="course-info">
                <v-avatar color="grey-lighten-4" size="40" rounded="lg">
                  <v-icon :icon="course.icon" :color="course.iconColor" size="20"></v-icon>
                </v-avatar>
                <div class="course-details">
                  <div class="course-name">{{ course.name }}</div>
                  <div class="course-progress-text">进度: {{ course.progress }}%</div>
                </div>
              </div>
              <v-progress-linear
                :model-value="course.progress"
                color="grey-lighten-1"
                height="5"
                rounded
              ></v-progress-linear>
            </v-card>
          </v-card>

          <!-- 正在学习的职业 -->
          <v-card class="section-card" rounded="lg" border>
            <div class="section-header">
              <h2 class="section-title">正在学习的职业</h2>
              <button class="section-link" @click="navigateTo('/my-careers')">
                查看全部
                <v-icon icon="mdi-arrow-right" size="16"></v-icon>
              </button>
            </div>
            <v-card v-for="career in recentCareers" :key="career.id" class="course-item" rounded="lg" border @click="openCareer(career.id)">
              <div class="course-info">
                <v-avatar color="grey-lighten-4" size="40" rounded="lg">
                  <v-icon :icon="career.icon" :color="career.iconColor" size="20"></v-icon>
                </v-avatar>
                <div class="course-details">
                  <div class="course-name">{{ career.name }}</div>
                  <div class="course-progress-text">进度: {{ career.progress }}%</div>
                </div>
              </div>
              <v-progress-linear
                :model-value="career.progress"
                color="grey-lighten-1"
                height="5"
                rounded
              ></v-progress-linear>
            </v-card>
          </v-card>
        </div>

        <!-- 右列 -->
        <div class="content-right">
          <!-- 推荐课程 -->
          <v-card class="section-card" rounded="lg" border>
            <div class="section-header">
              <h2 class="section-title">推荐课程</h2>
              <button class="section-link" @click="navigateTo('/learning')">
                更多
                <v-icon icon="mdi-arrow-right" size="16"></v-icon>
              </button>
            </div>
            <div v-for="course in recommendedCourses" :key="course.id" class="recommend-item">
              <v-avatar color="grey-lighten-4" size="40" rounded="lg">
                <v-icon :icon="course.icon" :color="course.iconColor" size="18"></v-icon>
              </v-avatar>
              <div class="recommend-details">
                <div class="recommend-name">{{ course.name }}</div>
                <div class="recommend-count">
                  <v-icon icon="mdi-account-multiple" size="12" color="#666666"></v-icon>
                  {{ course.learnerCount }} 人学习
                </div>
              </div>
              <button class="recommend-btn">
                <v-icon icon="mdi-plus" size="18" color="#666666"></v-icon>
              </button>
            </div>
          </v-card>

          <!-- 最近活动 -->
          <v-card class="section-card" rounded="lg" border>
            <h2 class="section-title">最近活动</h2>
            <div class="activity-timeline">
              <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
                <div class="activity-dot" :style="{ backgroundColor: activity.iconColor }">
                  <v-icon :icon="activity.icon" size="12" color="white"></v-icon>
                </div>
                <div class="activity-content">
                  <div class="activity-title">{{ activity.title }}</div>
                  <div class="activity-time">{{ activity.time }}</div>
                </div>
              </div>
            </div>
          </v-card>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.home-page {
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

/* 欢迎卡片 */
.welcome-card {
  background: #FFFFFF;
  border-bottom: 1px solid #E5E5E5;
  padding: 0 0 32px 0;
  margin-bottom: 40px;
}

.welcome-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.today-stats {
  text-align: center;
}

.today-number {
  font-size: 36px;
  font-weight: 700;
  color: #000000;
  line-height: 1;
}

.today-label {
  font-size: 13px;
  color: #666666;
  margin-top: 4px;
}

/* 平台引导卡片 */
.platform-guide-card {
  background: #FFFFFF;
  margin-bottom: 40px;
}

.guide-header {
  text-align: center;
  margin-bottom: 32px;
}

.guide-title {
  font-size: 24px;
  font-weight: 700;
  color: #000000;
  margin-bottom: 8px;
}

.guide-subtitle {
  font-size: 16px;
  color: #666666;
}

.path-steps {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.step-wrapper {
  display: flex;
  align-items: center;
  flex: 1;
}

.step-card {
  padding: 24px;
  cursor: pointer;
  transition: border-color 0.2s;
  width: 100%;
  min-height: 240px;
  display: flex;
  flex-direction: column;
}

.step-card:hover {
  border-color: #000000 !important;
}

.step-badge {
  display: flex;
  margin: 0 auto 16px auto;
  align-items: center;
  justify-content: center;
}

.step-title {
  font-size: 18px;
  font-weight: 600;
  color: #000000;
  text-align: center;
  margin-bottom: 8px;
}

.step-desc {
  font-size: 14px;
  color: #666666;
  text-align: center;
  line-height: 1.5;
  margin-bottom: 16px;
  min-height: 42px;
}

.step-data {
  padding: 12px;
  text-align: center;
  margin-top: auto;
}

.step-stat {
  font-size: 13px;
  font-weight: 600;
  color: #000000;
  margin-bottom: 4px;
}

.step-detail {
  font-size: 12px;
  color: #666666;
  margin: 0;
}

.step-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  flex-shrink: 0;
}

/* 统计网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 40px;
}

.stat-card {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #000000;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #666666;
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.content-left,
.content-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 区域卡片 */
.section-card {
  padding: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #000000;
}

.section-link {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #000000;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
}

.section-link:hover {
  text-decoration: underline;
}

/* 课程项 */
.course-item {
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.course-item:hover {
  border-color: #000000 !important;
}

.course-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.course-details {
  flex: 1;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #000000;
  margin-bottom: 4px;
}

.course-progress-text {
  font-size: 13px;
  color: #666666;
}

/* 推荐项 */
.recommend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.recommend-item:hover {
  border-color: #E5E5E5;
}

.recommend-details {
  flex: 1;
}

.recommend-name {
  font-size: 14px;
  font-weight: 500;
  color: #000000;
  margin-bottom: 4px;
}

.recommend-count {
  font-size: 13px;
  color: #666666;
  display: flex;
  align-items: center;
  gap: 4px;
}

.recommend-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #E5E5E5;
  background: #FFFFFF;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.recommend-btn:hover {
  background: #000000;
  border-color: #000000;
}

.recommend-btn:hover .v-icon {
  color: white !important;
}

/* 活动时间线 */
.activity-timeline {
  position: relative;
  margin-top: 16px;
}

.activity-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  position: relative;
}

.activity-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 11px;
  top: 24px;
  width: 2px;
  height: calc(100% + 4px);
  background: #E5E5E5;
}

.activity-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.activity-content {
  flex: 1;
  padding-top: 2px;
}

.activity-title {
  font-size: 14px;
  color: #000000;
  margin-bottom: 4px;
  font-weight: 400;
}

.activity-time {
  font-size: 13px;
  color: #666666;
}

/* 移动端 */
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    padding: 80px 20px 80px 20px;
    max-width: 100%;
  }

  .welcome-content {
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }

  .today-stats {
    align-self: center;
  }

  .path-steps {
    flex-direction: column;
    gap: 20px;
  }

  .step-wrapper {
    width: 100%;
  }

  .step-arrow {
    display: none;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .content-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>
