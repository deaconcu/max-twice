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
  learningDays: 0, // 暂时保持 mock，需要新接口
  todayMinutes: 0, // 从今日统计获取
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

// 计算属性：正在学习的课程（取前3个订阅的课程）
const recentCourses = computed(() => {
  return (userSubscriptionsData.value || []).slice(0, 3)
})

// 计算属性：推荐课程（取前4个热门课程）
const recommendedCourses = computed(() => {
  return (hotCoursesData.value || []).slice(0, 4)
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

// 快速入口 - 4步学习路径
const quickLinks = computed(() => [
  {
    step: 1,
    title: t('home.exploreCareer.title'),
    description: t('home.exploreCareer.description'),
    icon: 'mdi-briefcase-variant',
    color: 'warning',
    path: '/career',
    stat: t('home.exploreCareer.stat', { count: platformStats.value.careerPathCount }),
    statDetail: t('home.exploreCareer.statDetail'),
  },
  {
    step: 2,
    title: t('home.trackCareers.title'),
    description: t('home.trackCareers.description'),
    icon: 'mdi-flag-checkered',
    color: 'success',
    path: '/roadmap',
    stat: t('home.trackCareers.stat', {
      count: platformStats.value.roadmapCount.toLocaleString()
    }),
    statDetail: t('home.trackCareers.statDetail'),
  },
  {
    step: 3,
    title: t('home.browseCourses.title'),
    description: t('home.browseCourses.description'),
    icon: 'mdi-book-multiple',
    color: 'info',
    path: '/learning',
    stat: t('home.browseCourses.stat', { count: platformStats.value.courseCount }),
    statDetail: t('home.browseCourses.statDetail', {
      nodes: platformStats.value.knowledgeNodeCount.toLocaleString(),
    }),
  },
  {
    step: 4,
    title: t('home.trackCourses.title'),
    description: t('home.trackCourses.description'),
    icon: 'mdi-book-open-variant',
    color: 'purple',
    path: '/my-courses',
    stat: t('home.trackCourses.stat', {
      count: platformStats.value.articleCount.toLocaleString(),
    }),
    statDetail: t('home.trackCourses.statDetail'),
  },
])

// 正在学习的职业（暂时保持 mock，需要新接口）
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
    <div class="welcome-section mb-6 mb-md-10">
      <div class="d-flex flex-row align-center justify-space-between ga-3 ga-sm-4">
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
        <div class="text-center flex-shrink-0">
          <div
            class="text-h4 text-sm-h3 font-weight-bold"
            :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
          >
            {{ stats.todayMinutes }}
          </div>
          <div
            class="text-caption text-no-wrap"
            :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
          >
            {{ t('home.todayLearning') }}
          </div>
        </div>
      </div>
    </div>

    <!-- 平台介绍和学习路径 -->
    <div class="guide-section mb-6 mb-md-10">
      <div class="text-center mb-6 mb-md-8">
        <h2
          class="text-h6 text-sm-h5 font-weight-bold mb-2"
          :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
        >
          {{ t('home.guideTitle') }}
        </h2>
        <p
          class="text-body-2 text-sm-body-1"
          :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
        >
          {{ t('home.guideSubtitle') }}
        </p>
      </div>

      <!-- 学习路径步骤 -->
      <div class="path-steps">
        <template v-for="(link, index) in quickLinks" :key="link.step">
          <div class="step-wrapper">
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
                class="text-subtitle-1 text-sm-h6 font-weight-bold text-center mb-2"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ link.title }}
              </h3>
              <p
                class="text-caption text-sm-body-2 text-center mb-3 mb-sm-4"
                :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
              >
                {{ link.description }}
              </p>

              <!-- 平台数据 -->
              <v-card class="step-data" rounded="lg" variant="outlined">
                <div
                  class="text-body-2 font-weight-bold text-center mb-1"
                  :class="`text-${link.color}`"
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
          </div>

          <!-- 箭头（独立元素） -->
          <div v-if="index < quickLinks.length - 1" class="step-arrow d-none d-md-flex">
            <v-icon icon="mdi-arrow-right-thick" :color="'on-surface-variant'" size="36"></v-icon>
          </div>
        </template>
      </div>
    </div>

    <!-- 统计卡片 -->
    <v-row class="mb-6 mb-md-10">
      <v-col cols="12" sm="6" md="3">
        <v-card rounded="lg" border>
          <v-card-text class="d-flex align-center ga-3 ga-sm-4 pa-4 pa-sm-5">
            <v-avatar
              :color="'rgb(var(--v-theme-surface-variant))'"
              :size="$vuetify.display.mobile ? 40 : 48"
              rounded="lg"
            >
              <v-icon
                icon="mdi-book-open-variant"
                color="info"
                :size="$vuetify.display.mobile ? 20 : 24"
              ></v-icon>
            </v-avatar>
            <div>
              <div
                class="text-h6 text-sm-h5 font-weight-bold"
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

      <v-col cols="12" sm="6" md="3">
        <v-card rounded="lg" border>
          <v-card-text class="d-flex align-center ga-3 ga-sm-4 pa-4 pa-sm-5">
            <v-avatar
              :color="'rgb(var(--v-theme-surface-variant))'"
              :size="$vuetify.display.mobile ? 40 : 48"
              rounded="lg"
            >
              <v-icon
                icon="mdi-check-circle"
                color="success"
                :size="$vuetify.display.mobile ? 20 : 24"
              ></v-icon>
            </v-avatar>
            <div>
              <div
                class="text-h6 text-sm-h5 font-weight-bold"
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

      <v-col cols="12" sm="6" md="3">
        <v-card rounded="lg" border>
          <v-card-text class="d-flex align-center ga-3 ga-sm-4 pa-4 pa-sm-5">
            <v-avatar
              :color="'rgb(var(--v-theme-surface-variant))'"
              :size="$vuetify.display.mobile ? 40 : 48"
              rounded="lg"
            >
              <v-icon
                icon="mdi-briefcase-variant"
                color="warning"
                :size="$vuetify.display.mobile ? 20 : 24"
              ></v-icon>
            </v-avatar>
            <div>
              <div
                class="text-h6 text-sm-h5 font-weight-bold"
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

      <v-col cols="12" sm="6" md="3">
        <v-card rounded="lg" border>
          <v-card-text class="d-flex align-center ga-3 ga-sm-4 pa-4 pa-sm-5">
            <v-avatar
              :color="'rgb(var(--v-theme-surface-variant))'"
              :size="$vuetify.display.mobile ? 40 : 48"
              rounded="lg"
            >
              <v-icon
                icon="mdi-calendar-check"
                color="secondary"
                :size="$vuetify.display.mobile ? 20 : 24"
              ></v-icon>
            </v-avatar>
            <div>
              <div
                class="text-h6 text-sm-h5 font-weight-bold"
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
                class="text-subtitle-1 text-sm-h6 font-weight-bold"
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
              @click="openCourse(course.id)"
            >
              <v-card-text>
                <div class="d-flex align-center ga-3 mb-3">
                  <v-avatar :color="'rgb(var(--v-theme-surface-variant))'" size="40" rounded="lg">
                    <v-icon icon="mdi-book-open-variant" color="info" size="20"></v-icon>
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
                      {{ course.description || '点击查看详情' }}
                    </div>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </v-card-text>
        </v-card>

        <!-- 正在学习的职业 -->
        <v-card rounded="lg" border>
          <v-card-text>
            <div class="d-flex align-center justify-space-between mb-4">
              <h2
                class="text-subtitle-1 text-sm-h6 font-weight-bold"
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
                class="text-subtitle-1 text-sm-h6 font-weight-bold"
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
              style="cursor: pointer"
              @click="openCourse(course.id)"
            >
              <v-avatar :color="'rgb(var(--v-theme-surface-variant))'" size="40" rounded="lg">
                <v-icon icon="mdi-book-multiple" color="info" size="18"></v-icon>
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
                  {{ course.learnerCount?.toLocaleString() || 0 }} {{ t('home.learners') }}
                </div>
              </div>
              <v-icon icon="mdi-chevron-right" size="20" :color="'on-surface-variant'"></v-icon>
            </div>
          </v-card-text>
        </v-card>

        <!-- 最近活动 -->
        <v-card rounded="lg" border>
          <v-card-text>
            <h2
              class="text-subtitle-1 text-sm-h6 font-weight-bold mb-4"
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
  padding-bottom: 24px;
}

@media (min-width: 600px) {
  .welcome-section {
    padding-bottom: 32px;
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
  min-height: auto;
  display: flex;
  flex-direction: column;
}

@media (min-width: 600px) {
  .step-card {
    padding: 20px;
  }
}

@media (min-width: 960px) {
  .step-card {
    padding: 24px;
    min-height: 260px;
  }
}

.step-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.step-badge {
  margin: 0 auto 12px auto;
}

@media (min-width: 960px) {
  .step-badge {
    margin: 0 auto 16px auto;
  }
}

.step-data {
  padding: 10px;
  margin-top: auto;
}

@media (min-width: 600px) {
  .step-data {
    padding: 12px;
  }
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
