<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useFetch } from '@/composables'
import { useUserStore } from '@/stores/modules/user'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import DynamicIcon from '@/components/common/DynamicIcon.vue'
import ActivityHeatmap from '@/components/profile/ActivityHeatmap.vue'
import { homeApi } from '@/api/modules/home'
import { getColorByString } from '@/utils/color'
import type { HomePage } from '@/types/home'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

// 用户信息
const userName = computed(() => userStore.currentUser?.name ?? t('common.guest'))

// 加载首页聚合数据
const { data: homeData, loading: homeLoading } = useFetch<HomePage>({
  fetchFn: () => homeApi.getHomePageData(),
  immediate: userStore.isLoggedIn,
  defaultValue: {
    platformStats: {
      courseCount: 0,
      rolePathCount: 0,
      roadmapCount: 0,
      knowledgeNodeCount: 0,
      articleCount: 0,
      lastUpdated: '',
    },
    userStats: {
      learningDays: 0,
      coursesInProgress: 0,
      professionsInProgress: 0,
    },
    learningProfessions: [],
    learningCourses: [],
    reviewSummary: {
      todayTotal: 0,
      todayCompleted: 0,
      streakDays: 0,
      courses: [],
    },
    recommendedProfessions: [],
    recommendedCourses: [],
  },
})

// 计算属性：用户学习统计
const stats = computed(() => ({
  learningDays: homeData.value.userStats.learningDays,
  coursesInProgress: homeData.value.userStats.coursesInProgress,
  professionsInProgress: homeData.value.userStats.professionsInProgress,
  reviewCards: homeData.value.reviewSummary.todayTotal,
}))

// 计算属性：平台统计数据
const platformStats = computed(() => homeData.value.platformStats)

// 计算属性：复习数据
const reviewData = computed(() => ({
  todayTotal: homeData.value.reviewSummary.todayTotal,
  todayCompleted: homeData.value.reviewSummary.todayCompleted,
  streakDays: homeData.value.reviewSummary.streakDays,
  courses: homeData.value.reviewSummary.courses.slice(0, 3),
}))

// 计算属性：正在学习的职业路线
const recentRoles = computed(() => {
  return homeData.value.learningProfessions.map((item) => {
    const roadmap = item.object as
      | { id: number; professionName: string; professionIcon?: string; nodeCount?: number }
      | undefined
    const name = roadmap?.professionName ?? '未知职业'
    return {
      id: item.id,
      roadmapId: item.objectId,
      name,
      progress: Math.round(item.progressPercent / 100),
      icon: roadmap?.professionIcon ?? 'mdi-briefcase-variant',
      iconColor: getColorByString(name),
      nodeCount: roadmap?.nodeCount ?? 0,
    }
  })
})

// 计算属性：正在学习的课程
const recentCourses = computed(() => {
  return homeData.value.learningCourses.map((item) => {
    const course = item.object as
      | { id: number; name: string; description?: string; icon?: string }
      | undefined
    const courseId = course?.id ?? item.objectId
    const name = course?.name ?? '未知课程'
    return {
      id: courseId,
      name,
      description: course?.description ?? '',
      progress: Math.round(item.progressPercent / 100),
      icon: course?.icon,
      iconColor: getColorByString(name),
    }
  })
})

// 计算属性：推荐职业
const recommendedProfessions = computed(() => {
  return homeData.value.recommendedProfessions.map((item, index) => ({
    id: index + 1,
    roleId: item.id,
    name: item.name,
    icon: item.icon ?? 'mdi-briefcase-variant',
    iconColor: getColorByString(item.name),
    description: item.description ?? '',
    learnerCount: item.learnerCount ?? 0,
  }))
})

// 计算属性：推荐课程
const recommendedCourses = computed(() => homeData.value.recommendedCourses)

// 是否有更多职业（总数超过8个时，第8个显示为"更多"）
const hasMoreRoles = computed(() => stats.value.professionsInProgress > 8)

// 是否有更多课程（总数超过8个时，第8个显示为"更多"）
const hasMoreCourses = computed(() => stats.value.coursesInProgress > 8)

// 显示的职业列表（如果有更多，只显示前7个）
const displayRoles = computed(() => {
  if (hasMoreRoles.value) {
    return recentRoles.value.slice(0, 7)
  }
  return recentRoles.value
})

// 显示的课程列表（如果有更多，只显示前7个）
const displayCourses = computed(() => {
  if (hasMoreCourses.value) {
    return recentCourses.value.slice(0, 7)
  }
  return recentCourses.value
})

// 快速入口 - 3步学习路径
const quickLinks = computed(() => [
  {
    step: 1,
    title: '探索职业',
    description: '职业方向与学习路线规划',
    icon: 'mdi-briefcase-variant',
    color: 'warning',
    path: '/role',
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

// 导航函数
const navigateTo = (path: string): void => {
  router.push(path)
}

const openCourse = (courseId: number): void => {
  router.push(`/read?courseId=${String(courseId)}`)
}

const openRole = (roleId: number): void => {
  router.push(`/role/${String(roleId)}`)
}

const openRoadmap = (roadmapId: number): void => {
  router.push(`/roadmap/${String(roadmapId)}`)
}

// 暴露 homeLoading 供模板使用（可选，用于显示加载状态）
void homeLoading
</script>

<template>
  <DefaultLayout>
    <!-- 加载状态 -->
    <LoadingSpinner v-if="homeLoading" />

    <template v-else>
    <!-- 欢迎区域 -->
    <div class="welcome-section mb-4 mb-md-6">
      <div
        class="d-flex flex-column flex-md-row align-start align-md-center justify-space-between ga-4 ga-md-6"
      >
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
          <!-- 连续学习天数 - 突出显示 -->
          <div class="d-flex align-center ga-2">
            <v-icon icon="mdi-fire" color="warning" size="24"></v-icon>
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-warning">
                {{ stats.learningDays }} <span class="text-body-2">天</span>
              </div>
              <div class="text-caption text-no-wrap text-medium-emphasis">连续学习</div>
            </div>
          </div>

          <!-- 分隔线 -->
          <v-divider vertical class="d-none d-sm-block stats-divider"></v-divider>

          <!-- 其他三个指标 -->
          <div class="d-flex align-center ga-4 ga-md-5">
            <!-- 进行中课程 -->
            <div class="text-center">
              <div
                class="text-h6 font-weight-bold"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ stats.coursesInProgress }}
              </div>
              <div
                class="text-caption text-no-wrap"
                :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
              >
                进行中课程
              </div>
            </div>

            <!-- 进行中职业 -->
            <div class="text-center">
              <div
                class="text-h6 font-weight-bold"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ stats.professionsInProgress }}
              </div>
              <div
                class="text-caption text-no-wrap"
                :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
              >
                进行中职业
              </div>
            </div>

            <!-- 复习卡片数 -->
            <div class="text-center">
              <div
                class="text-h6 font-weight-bold"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ stats.reviewCards || 0 }}
              </div>
              <div
                class="text-caption text-no-wrap"
                :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
              >
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
          <div
            class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between ga-3 mb-4 mb-md-6"
          >
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
                  {{ platformStats.rolePathCount }} <span class="text-body-2">个</span>
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
                  {{ platformStats.knowledgeNodeCount.toLocaleString() }}
                  <span class="text-body-2">个</span>
                </div>
                <div class="text-caption text-medium-emphasis">知识节点</div>
              </div>
              <div class="text-center">
                <div class="text-h6 font-weight-bold text-primary">
                  {{ platformStats.articleCount.toLocaleString() }}
                  <span class="text-body-2">篇</span>
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
                    <v-avatar color="grey-lighten-4" size="40" rounded="lg" class="flex-shrink-0">
                      <span class="text-h6 font-weight-bold text-grey-darken-1">{{
                        link.step
                      }}</span>
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
      <div
        class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between ga-3 mb-4"
      >
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
              :model-value="
                reviewData.todayTotal > 0
                  ? (reviewData.todayCompleted / reviewData.todayTotal) * 100
                  : 0
              "
              color="success"
              :size="48"
              :width="5"
            >
              <span class="text-caption font-weight-bold"
                >{{
                  reviewData.todayTotal > 0
                    ? Math.round((reviewData.todayCompleted / reviewData.todayTotal) * 100)
                    : 0
                }}%</span
              >
            </v-progress-circular>
          </v-card-text>
        </v-card>

        <!-- 待复习课程列表 -->
        <v-card
          v-for="course in reviewData.courses"
          :key="course.course.id"
          rounded="lg"
          border
          hover
          class="review-deck-card"
          @click="navigateTo('/review')"
        >
          <v-card-text class="pa-3">
            <div class="d-flex align-center ga-3">
              <div class="icon-container-sm flex-shrink-0">
                <DynamicIcon
                  :icon="course.course.icon"
                  default-icon="mdi-book-open-variant"
                  :size="20"
                  :color="getColorByString(course.course.name)"
                />
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div
                  class="text-body-2 font-weight-bold mb-1 text-truncate"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ course.course.name }}
                </div>
                <div class="text-caption text-medium-emphasis text-truncate">
                  共 {{ course.cardCount }} 张卡片
                </div>
              </div>
              <v-chip size="small" color="success" variant="tonal" class="flex-shrink-0">
                {{ course.dueCardCount }}
              </v-chip>
            </div>
          </v-card-text>
        </v-card>

        <!-- 没有数据时显示占位卡片 -->
        <v-card
          v-if="reviewData.courses.length === 0"
          rounded="lg"
          border
          class="review-deck-card empty-placeholder-card"
          @click="navigateTo('/courses')"
        >
          <v-card-text class="pa-3">
            <div class="d-flex align-center ga-3">
              <div class="icon-container-sm flex-shrink-0 empty-icon-container">
                <v-icon icon="mdi-cards-outline" color="grey-lighten-1" size="20"></v-icon>
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div class="text-body-2 font-weight-bold mb-1 text-truncate text-grey-lighten-1">
                  暂无复习卡片
                </div>
                <div class="text-caption text-grey-lighten-1 text-truncate">
                  学习课程后会自动添加
                </div>
              </div>
              <v-icon icon="mdi-arrow-right" color="grey-lighten-1" size="16"></v-icon>
            </div>
          </v-card-text>
        </v-card>
      </div>
    </div>

    <!-- 正在跟踪的职业 -->
    <div class="role-section mb-6 mb-md-10">
      <div
        class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between ga-3 mb-4"
      >
        <h2
          class="text-h6 text-sm-h5 font-weight-bold"
          :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
        >
          正在跟踪的职业路线
        </h2>
        <v-btn variant="text" size="small" :color="'on-surface'" @click="navigateTo('/role')">
          探索更多
          <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
        </v-btn>
      </div>

      <!-- 职业卡片列表 -->
      <v-row>
        <!-- 有数据时显示真实卡片 -->
        <v-col v-for="role in displayRoles" :key="role.id" cols="12" sm="6" md="4" lg="3">
          <v-card rounded="lg" border hover class="h-100" @click="openRoadmap(role.roadmapId)">
            <v-card-text class="pa-4">
              <div class="d-flex align-center ga-3 mb-3">
                <div class="icon-container flex-shrink-0">
                  <DynamicIcon
                    :icon="role.icon"
                    default-icon="mdi-briefcase-variant"
                    :size="24"
                    :color="role.iconColor"
                  />
                </div>
                <div class="flex-grow-1">
                  <div
                    class="text-body-1 font-weight-bold"
                    :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                  >
                    {{ role.name }}
                  </div>
                  <div class="text-caption text-medium-emphasis">
                    {{ role.nodeCount }} 个知识节点
                  </div>
                </div>
              </div>
              <div class="d-flex align-center justify-space-between mb-2">
                <span class="text-caption text-medium-emphasis"> 学习进度 </span>
                <span class="text-caption font-weight-bold text-grey">
                  {{ role.progress }}%
                </span>
              </div>
              <v-progress-linear
                :model-value="role.progress"
                color="grey-lighten-3"
                height="6"
                rounded
              ></v-progress-linear>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 没有数据时显示占位卡片 -->
        <v-col v-if="recentRoles.length === 0" cols="12" sm="6" md="4" lg="3">
          <v-card rounded="lg" border class="empty-placeholder-card" @click="navigateTo('/role')">
            <v-card-text class="pa-4">
              <div class="d-flex align-center ga-3">
                <div class="icon-container flex-shrink-0 empty-icon-container">
                  <v-icon icon="mdi-briefcase-variant" color="grey-lighten-1" size="24"></v-icon>
                </div>
                <div class="flex-grow-1">
                  <div class="text-body-1 font-weight-bold mb-1 text-grey-lighten-1">
                    暂无职业路线
                  </div>
                  <div class="text-caption text-grey-lighten-1">去职业中心探索适合你的方向</div>
                </div>
                <v-icon icon="mdi-arrow-right" color="grey-lighten-1" size="20"></v-icon>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 查看更多卡片 -->
        <v-col v-if="hasMoreRoles" cols="12" sm="6" md="4" lg="3">
          <v-card
            rounded="lg"
            border
            hover
            class="h-100 view-more-card"
            @click="navigateTo('/users/me?tab=roles')"
          >
            <v-card-text class="pa-4 d-flex align-center justify-center h-100">
              <div class="text-center">
                <v-icon icon="mdi-dots-horizontal" color="grey" size="32" class="mb-2"></v-icon>
                <div class="text-body-2 text-grey">查看全部 {{ stats.professionsInProgress }} 个</div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>

    <!-- 正在学习的课程 -->
    <div class="course-section mb-6 mb-md-10">
      <div
        class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between ga-3 mb-4"
      >
        <h2
          class="text-h6 text-sm-h5 font-weight-bold"
          :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
        >
          正在学习的课程
        </h2>
        <v-btn variant="text" size="small" :color="'on-surface'" @click="navigateTo('/courses')">
          浏览更多
          <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
        </v-btn>
      </div>

      <!-- 课程卡片列表 -->
      <v-row>
        <!-- 有数据时显示真实卡片 -->
        <v-col v-for="course in displayCourses" :key="course.id" cols="12" sm="6" md="4" lg="3">
          <v-card rounded="lg" border hover class="h-100" @click="openCourse(course.id)">
            <v-card-text class="pa-4">
              <div class="d-flex align-center ga-3 mb-3">
                <div class="icon-container flex-shrink-0">
                  <DynamicIcon
                    :icon="course.icon"
                    default-icon="mdi-book-open-variant"
                    :size="24"
                    :color="course.iconColor"
                  />
                </div>
                <div class="flex-grow-1" style="min-width: 0">
                  <div
                    class="text-body-1 font-weight-bold mb-1 text-truncate"
                    :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                  >
                    {{ course.name }}
                  </div>
                  <div class="text-caption text-medium-emphasis text-truncate">
                    {{ course.description ?? '点击查看详情' }}
                  </div>
                </div>
              </div>
              <div class="d-flex align-center justify-space-between mb-2">
                <span class="text-caption text-medium-emphasis"> 学习进度 </span>
                <span class="text-caption font-weight-bold text-grey">
                  {{ course.progress ?? 0 }}%
                </span>
              </div>
              <v-progress-linear
                :model-value="course.progress ?? 0"
                color="grey-lighten-3"
                height="6"
                rounded
              ></v-progress-linear>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 没有数据时显示占位卡片 -->
        <v-col v-if="recentCourses.length === 0" cols="12" sm="6" md="4" lg="3">
          <v-card
            rounded="lg"
            border
            class="empty-placeholder-card"
            @click="navigateTo('/courses')"
          >
            <v-card-text class="pa-4">
              <div class="d-flex align-center ga-3">
                <div class="icon-container flex-shrink-0 empty-icon-container">
                  <v-icon icon="mdi-book-open-variant" color="grey-lighten-1" size="24"></v-icon>
                </div>
                <div class="flex-grow-1" style="min-width: 0">
                  <div class="text-body-1 font-weight-bold mb-1 text-truncate text-grey-lighten-1">
                    暂无学习中的课程
                  </div>
                  <div class="text-caption text-grey-lighten-1 text-truncate">
                    去课程中心发现感兴趣的内容
                  </div>
                </div>
                <v-icon icon="mdi-arrow-right" color="grey-lighten-1" size="20"></v-icon>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 查看更多卡片 -->
        <v-col v-if="hasMoreCourses" cols="12" sm="6" md="4" lg="3">
          <v-card
            rounded="lg"
            border
            hover
            class="h-100 view-more-card"
            @click="navigateTo('/users/me?tab=courses-learning')"
          >
            <v-card-text class="pa-4 d-flex align-center justify-center h-100">
              <div class="text-center">
                <v-icon icon="mdi-dots-horizontal" color="grey" size="32" class="mb-2"></v-icon>
                <div class="text-body-2 text-grey">查看全部 {{ stats.coursesInProgress }} 门</div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>

    <!-- 热门区域 -->
    <v-row class="mb-6 mb-md-10">
      <!-- 左列：热门职业 -->
      <v-col cols="12" md="6">
        <div class="d-flex align-center justify-space-between mb-4">
          <h2
            class="text-h6 text-sm-h5 font-weight-bold"
            :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
          >
            热门职业
          </h2>
          <v-btn variant="text" size="small" :color="'on-surface'" @click="navigateTo('/role')">
            更多
            <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
          </v-btn>
        </div>
        <div class="d-flex flex-wrap ga-2">
          <v-btn
            v-for="role in recommendedProfessions"
            :key="role.id"
            variant="tonal"
            color="default"
            rounded="lg"
            @click="openRole(role.roleId)"
          >
            <DynamicIcon
              :icon="role.icon"
              default-icon="mdi-briefcase-variant"
              :size="18"
              :color="role.iconColor"
              start
            />
            {{ role.name }}
            <span class="text-caption ml-1 text-medium-emphasis">{{
              role.learnerCount.toLocaleString()
            }}</span>
          </v-btn>
        </div>
      </v-col>

      <!-- 右列：热门课程 -->
      <v-col cols="12" md="6">
        <div class="d-flex align-center justify-space-between mb-4">
          <h2
            class="text-h6 text-sm-h5 font-weight-bold"
            :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
          >
            热门课程
          </h2>
          <v-btn variant="text" size="small" :color="'on-surface'" @click="navigateTo('/courses')">
            更多
            <v-icon icon="mdi-arrow-right" size="16" class="ml-1"></v-icon>
          </v-btn>
        </div>
        <div class="d-flex flex-wrap ga-2">
          <v-btn
            v-for="course in recommendedCourses"
            :key="course.id"
            variant="flat"
            color="grey-lighten-5"
            rounded="lg"
            @click="openCourse(course.id)"
          >
            <DynamicIcon
              :icon="course.icon"
              default-icon="mdi-book-open-variant"
              :size="18"
              :color="getColorByString(course.name)"
              start
            />
            {{ course.name }}
            <span class="text-caption ml-1 text-medium-emphasis">{{
              course.learnerCount?.toLocaleString() || 0
            }}</span>
          </v-btn>
        </div>
      </v-col>
    </v-row>
    </template>
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
  padding-top: 24px;
  padding-bottom: 24px;
}

@media (max-width: 960px) {
  .welcome-section {
    padding-top: 16px;
  }
}

@media (min-width: 600px) {
  .welcome-section {
    padding-bottom: 32px;
  }
}

@media (min-width: 960px) {
  .welcome-section {
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

/* 空数据占位卡片 */
.empty-placeholder-card {
  cursor: pointer;
  border-style: dashed !important;
  opacity: 0.7;
  transition: all 0.2s;
}

.empty-placeholder-card:hover {
  opacity: 1;
  border-color: rgb(var(--v-theme-primary)) !important;
}

.empty-icon-container {
  border-style: dashed;
  border-color: rgb(var(--v-theme-outline));
}

/* 查看更多卡片 */
.view-more-card {
  min-height: 120px;
  cursor: pointer;
  border-style: dashed !important;
  transition: all 0.2s;
}

.view-more-card:hover {
  border-color: rgb(var(--v-theme-primary)) !important;
  background-color: rgba(var(--v-theme-primary), 0.04);
}
</style>
