<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useUserStore } from '@/stores/modules/user'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

// 用户信息
const userName = computed(() => userStore.currentUser?.username || t('common.guest'))

// Mock 学习统计数据
const stats = ref({
  coursesInProgress: 5,
  completedCourses: 12,
  careersInProgress: 3,
  learningDays: 45,
  todayMinutes: 30,
})

// Mock 平台统计数据
const platformStats = ref({
  totalCourses: 1280,
  totalNodes: 15600,
  totalCareers: 156,
  totalLearners: 89420,
  coursesGrowth: '2.3k',
  completionRate: 85,
})

// 快速入口 - 4步学习路径
const quickLinks = computed(() => [
  {
    step: 1,
    title: t('home.exploreCareer.title'),
    description: t('home.exploreCareer.description'),
    icon: 'mdi-briefcase-variant',
    color: 'warning',
    path: '/career',
    stat: t('home.exploreCareer.stat', { count: platformStats.value.totalCareers }),
    statDetail: t('home.exploreCareer.statDetail'),
  },
  {
    step: 2,
    title: t('home.browseCourses.title'),
    description: t('home.browseCourses.description'),
    icon: 'mdi-book-multiple',
    color: 'info',
    path: '/learning',
    stat: t('home.browseCourses.stat', { count: platformStats.value.totalCourses }),
    statDetail: t('home.browseCourses.statDetail', {
      nodes: platformStats.value.totalNodes.toLocaleString(),
    }),
  },
  {
    step: 3,
    title: t('home.trackCourses.title'),
    description: t('home.trackCourses.description'),
    icon: 'mdi-book-open-variant',
    color: 'secondary',
    path: '/my-courses',
    stat: t('home.trackCourses.stat', {
      count: (platformStats.value.totalLearners / 1000).toFixed(1),
    }),
    statDetail: t('home.trackCourses.statDetail', { growth: platformStats.value.coursesGrowth }),
  },
  {
    step: 4,
    title: t('home.trackCareers.title'),
    description: t('home.trackCareers.description'),
    icon: 'mdi-flag-checkered',
    color: 'success',
    path: '/my-careers',
    stat: t('home.trackCareers.stat', { rate: platformStats.value.completionRate }),
    statDetail: t('home.trackCareers.statDetail'),
  },
])

// 正在学习的课程
const recentCourses = ref([
  {
    id: 1,
    courseId: 101,
    name: 'Python编程入门',
    progress: 45,
    icon: 'mdi-language-python',
    iconColor: 'info',
  },
  {
    id: 2,
    courseId: 102,
    name: '数据结构与算法',
    progress: 78,
    icon: 'mdi-chart-tree',
    iconColor: 'success',
  },
  {
    id: 3,
    courseId: 103,
    name: '机器学习入门',
    progress: 23,
    icon: 'mdi-brain',
    iconColor: 'secondary',
  },
])

// 正在学习的职业
const recentCareers = ref([
  {
    id: 1,
    careerId: 201,
    name: '前端工程师',
    progress: 65,
    icon: 'mdi-laptop',
    iconColor: 'info',
  },
  {
    id: 2,
    careerId: 202,
    name: 'UX设计师',
    progress: 30,
    icon: 'mdi-account-heart',
    iconColor: 'success',
  },
])

// 推荐课程
const recommendedCourses = ref([
  {
    id: 10,
    courseId: 110,
    name: '微积分基础',
    learnerCount: 8765,
    icon: 'mdi-function',
    iconColor: 'info',
  },
  {
    id: 13,
    courseId: 113,
    name: '英语语法精讲',
    learnerCount: 20123,
    icon: 'mdi-book-alphabet',
    iconColor: 'error',
  },
  {
    id: 18,
    courseId: 118,
    name: 'UI设计基础',
    learnerCount: 13890,
    icon: 'mdi-cellphone',
    iconColor: 'info',
  },
  {
    id: 6,
    courseId: 106,
    name: 'MySQL数据库设计',
    learnerCount: 11234,
    icon: 'mdi-database',
    iconColor: 'teal',
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

const addRecommendedCourse = (courseId: number): void => {
  console.log('添加推荐课程:', courseId)
  // TODO: 调用 API 添加课程到学习列表
}
</script>

<template>
  <DefaultLayout>
    <div class="home-page">
      <!-- 欢迎区域 -->
      <div class="welcome-section mb-10">
        <div class="d-flex align-center justify-space-between">
          <div class="d-flex align-center ga-4">
            <v-avatar :color="'rgb(var(--v-theme-surface-variant))'" size="64" rounded="lg">
              <v-icon size="32" :color="'rgb(var(--v-theme-on-surface-variant))'"
                >mdi-account</v-icon
              >
            </v-avatar>
            <div>
              <h1
                class="text-h4 font-weight-bold"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ t('home.greeting', { name: userName }) }}
              </h1>
              <p
                class="text-body-2 mt-1"
                :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
              >
                {{ t('home.keepLearning') }}
              </p>
            </div>
          </div>
          <div class="text-center">
            <div
              class="text-h3 font-weight-bold"
              :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
            >
              {{ stats.todayMinutes }}
            </div>
            <div class="text-caption" :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }">
              {{ t('home.todayLearning') }}
            </div>
          </div>
        </div>
      </div>

      <!-- 平台介绍和学习路径 -->
      <div class="guide-section mb-10">
        <div class="text-center mb-8">
          <h2
            class="text-h5 font-weight-bold mb-2"
            :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
          >
            {{ t('home.guideTitle') }}
          </h2>
          <p class="text-body-1" :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }">
            {{ t('home.guideSubtitle') }}
          </p>
        </div>

        <!-- 学习路径步骤 -->
        <div class="path-steps">
          <div v-for="(link, index) in quickLinks" :key="link.step" class="step-wrapper">
            <v-card class="step-card" rounded="lg" border hover @click="navigateTo(link.path)">
              <!-- STEP标签 -->
              <v-chip class="step-badge" size="small" variant="flat" :color="'surface-variant'">
                {{ t('home.step') }} {{ link.step }}
              </v-chip>

              <!-- 图标 -->
              <v-avatar
                :color="'rgb(var(--v-theme-surface-variant))'"
                size="56"
                rounded="lg"
                class="mx-auto mb-4"
              >
                <v-icon :icon="link.icon" :color="link.color" size="28"></v-icon>
              </v-avatar>

              <!-- 标题和描述 -->
              <h3
                class="text-h6 font-weight-bold text-center mb-2"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ link.title }}
              </h3>
              <p
                class="text-body-2 text-center mb-4"
                :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
              >
                {{ link.description }}
              </p>

              <!-- 平台数据 -->
              <v-card class="step-data" rounded="lg" variant="outlined">
                <div
                  class="text-body-2 font-weight-bold text-center mb-1"
                  :style="{ color: `rgb(var(--v-theme-${link.color}))` }"
                >
                  {{ link.stat }}
                </div>
                <p
                  class="text-caption text-center ma-0"
                  :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
                >
                  {{ link.statDetail }}
                </p>
              </v-card>
            </v-card>

            <!-- 箭头 -->
            <div v-if="index < quickLinks.length - 1" class="step-arrow d-none d-md-flex">
              <v-icon icon="mdi-arrow-right-thick" :color="'on-surface-variant'" size="36"></v-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 统计卡片 -->
      <v-row class="mb-10">
        <v-col cols="6" md="3">
          <v-card rounded="lg" border>
            <v-card-text class="d-flex align-center ga-4">
              <v-avatar :color="'rgb(var(--v-theme-surface-variant))'" size="48" rounded="lg">
                <v-icon icon="mdi-book-open-variant" color="info" size="24"></v-icon>
              </v-avatar>
              <div>
                <div
                  class="text-h5 font-weight-bold"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ stats.coursesInProgress }}
                </div>
                <div
                  class="text-caption"
                  :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
                >
                  {{ t('home.stats.coursesInProgress') }}
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="6" md="3">
          <v-card rounded="lg" border>
            <v-card-text class="d-flex align-center ga-4">
              <v-avatar :color="'rgb(var(--v-theme-surface-variant))'" size="48" rounded="lg">
                <v-icon icon="mdi-check-circle" color="success" size="24"></v-icon>
              </v-avatar>
              <div>
                <div
                  class="text-h5 font-weight-bold"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ stats.completedCourses }}
                </div>
                <div
                  class="text-caption"
                  :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
                >
                  {{ t('home.stats.completedCourses') }}
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="6" md="3">
          <v-card rounded="lg" border>
            <v-card-text class="d-flex align-center ga-4">
              <v-avatar :color="'rgb(var(--v-theme-surface-variant))'" size="48" rounded="lg">
                <v-icon icon="mdi-briefcase-variant" color="warning" size="24"></v-icon>
              </v-avatar>
              <div>
                <div
                  class="text-h5 font-weight-bold"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ stats.careersInProgress }}
                </div>
                <div
                  class="text-caption"
                  :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
                >
                  {{ t('home.stats.careersInProgress') }}
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="6" md="3">
          <v-card rounded="lg" border>
            <v-card-text class="d-flex align-center ga-4">
              <v-avatar :color="'rgb(var(--v-theme-surface-variant))'" size="48" rounded="lg">
                <v-icon icon="mdi-calendar-check" color="secondary" size="24"></v-icon>
              </v-avatar>
              <div>
                <div
                  class="text-h5 font-weight-bold"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ stats.learningDays }}
                </div>
                <div
                  class="text-caption"
                  :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
                >
                  {{ t('home.stats.learningDays') }}
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- 主要内容区 -->
      <v-row>
        <!-- 左列 -->
        <v-col cols="12" md="6">
          <!-- 正在学习的课程 -->
          <v-card class="mb-5" rounded="lg" border>
            <v-card-text>
              <div class="d-flex align-center justify-space-between mb-4">
                <h2
                  class="text-h6 font-weight-bold"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ t('home.learningCourses') }}
                </h2>
                <v-btn
                  variant="text"
                  size="small"
                  :color="'on-surface'"
                  @click="navigateTo('/my-courses')"
                >
                  {{ t('home.viewAll') }}
                  <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
                </v-btn>
              </div>

              <v-card
                v-for="course in recentCourses"
                :key="course.id"
                class="mb-3"
                rounded="lg"
                border
                hover
                @click="openCourse(course.courseId)"
              >
                <v-card-text>
                  <div class="d-flex align-center ga-3 mb-3">
                    <v-avatar :color="'rgb(var(--v-theme-surface-variant))'" size="40" rounded="lg">
                      <v-icon :icon="course.icon" :color="course.iconColor" size="20"></v-icon>
                    </v-avatar>
                    <div class="flex-grow-1">
                      <div
                        class="text-body-1 font-weight-bold mb-1"
                        :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                      >
                        {{ course.name }}
                      </div>
                      <div
                        class="text-caption"
                        :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
                      >
                        {{ t('home.progress') }}: {{ course.progress }}%
                      </div>
                    </div>
                  </div>
                  <v-progress-linear
                    :model-value="course.progress"
                    :color="'outline'"
                    height="5"
                    rounded
                  ></v-progress-linear>
                </v-card-text>
              </v-card>
            </v-card-text>
          </v-card>

          <!-- 正在学习的职业 -->
          <v-card rounded="lg" border>
            <v-card-text>
              <div class="d-flex align-center justify-space-between mb-4">
                <h2
                  class="text-h6 font-weight-bold"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ t('home.learningCareers') }}
                </h2>
                <v-btn
                  variant="text"
                  size="small"
                  :color="'on-surface'"
                  @click="navigateTo('/my-careers')"
                >
                  {{ t('home.viewAll') }}
                  <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
                </v-btn>
              </div>

              <v-card
                v-for="career in recentCareers"
                :key="career.id"
                class="mb-3"
                rounded="lg"
                border
                hover
                @click="openCareer(career.careerId)"
              >
                <v-card-text>
                  <div class="d-flex align-center ga-3 mb-3">
                    <v-avatar :color="'rgb(var(--v-theme-surface-variant))'" size="40" rounded="lg">
                      <v-icon :icon="career.icon" :color="career.iconColor" size="20"></v-icon>
                    </v-avatar>
                    <div class="flex-grow-1">
                      <div
                        class="text-body-1 font-weight-bold mb-1"
                        :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                      >
                        {{ career.name }}
                      </div>
                      <div
                        class="text-caption"
                        :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
                      >
                        {{ t('home.progress') }}: {{ career.progress }}%
                      </div>
                    </div>
                  </div>
                  <v-progress-linear
                    :model-value="career.progress"
                    :color="'outline'"
                    height="5"
                    rounded
                  ></v-progress-linear>
                </v-card-text>
              </v-card>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 右列 -->
        <v-col cols="12" md="6">
          <!-- 推荐课程 -->
          <v-card class="mb-5" rounded="lg" border>
            <v-card-text>
              <div class="d-flex align-center justify-space-between mb-4">
                <h2
                  class="text-h6 font-weight-bold"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ t('home.recommendedCourses') }}
                </h2>
                <v-btn
                  variant="text"
                  size="small"
                  :color="'on-surface'"
                  @click="navigateTo('/learning')"
                >
                  {{ t('home.more') }}
                  <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
                </v-btn>
              </div>

              <div
                v-for="course in recommendedCourses"
                :key="course.id"
                class="recommend-item d-flex align-center ga-3 pa-3 mb-2 rounded-lg"
              >
                <v-avatar :color="'rgb(var(--v-theme-surface-variant))'" size="40" rounded="lg">
                  <v-icon :icon="course.icon" :color="course.iconColor" size="18"></v-icon>
                </v-avatar>
                <div class="flex-grow-1">
                  <div
                    class="text-body-2 font-weight-medium mb-1"
                    :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                  >
                    {{ course.name }}
                  </div>
                  <div
                    class="text-caption d-flex align-center"
                    :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
                  >
                    <v-icon icon="mdi-account-multiple" size="12" class="mr-1"></v-icon>
                    {{ course.learnerCount.toLocaleString() }} {{ t('home.learners') }}
                  </div>
                </div>
                <v-btn
                  icon="mdi-plus"
                  size="small"
                  variant="outlined"
                  @click="addRecommendedCourse(course.courseId)"
                ></v-btn>
              </div>
            </v-card-text>
          </v-card>

          <!-- 最近活动 -->
          <v-card rounded="lg" border>
            <v-card-text>
              <h2
                class="text-h6 font-weight-bold mb-4"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ t('home.recentActivities') }}
              </h2>

              <div class="activity-timeline">
                <div
                  v-for="(activity, index) in recentActivities"
                  :key="activity.id"
                  class="activity-item d-flex ga-3 mb-4"
                  :class="{ 'last-item': index === recentActivities.length - 1 }"
                >
                  <v-avatar :color="activity.iconColor" size="24" class="activity-dot">
                    <v-icon :icon="activity.icon" size="12" color="white"></v-icon>
                  </v-avatar>
                  <div class="flex-grow-1">
                    <div
                      class="text-body-2 mb-1"
                      :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                    >
                      {{ activity.title }}
                    </div>
                    <div
                      class="text-caption"
                      :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
                    >
                      {{ activity.time }}
                    </div>
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.home-page {
  padding: 24px;
}

/* 欢迎区域 */
.welcome-section {
  padding-bottom: 32px;
}

/* 学习路径步骤 */
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
  transition: all 0.2s;
  width: 100%;
  min-height: 260px;
  display: flex;
  flex-direction: column;
}

.step-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.step-badge {
  margin: 0 auto 16px auto;
}

.step-data {
  padding: 12px;
  margin-top: auto;
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
    transform: translateX(0);
    opacity: 1;
  }
  50% {
    transform: translateX(20px);
    opacity: 0.6;
  }
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

/* 移动端 */
@media (max-width: 960px) {
  .home-page {
    padding: 16px;
  }

  .path-steps {
    flex-direction: column;
  }

  .step-wrapper {
    width: 100%;
  }

  .step-card {
    min-height: auto;
  }
}
</style>
