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

// 快速入口 - 4步学习路径
const quickLinks = computed(() => [
  {
    step: 1,
    title: '我在扮演什么角色',
    description: '探索职业方向，找到你想扮演的角色',
    icon: 'mdi-account-question',
    color: 'warning',
    path: '/role',
  },
  {
    step: 2,
    title: '我要学什么',
    description: '每个角色都有对应的学习路线，跟着路线走，成长更高效',
    icon: 'mdi-map-marker-path',
    color: 'primary',
    path: '/role',
  },
  {
    step: 3,
    title: '我在学什么',
    description: '路线中的每门课程都值得深入学习，一步一个脚印',
    icon: 'mdi-book-open-page-variant',
    color: 'info',
    path: '/courses',
  },
  {
    step: 4,
    title: '我学会了吗',
    description: '通过复习检验，真正掌握',
    icon: 'mdi-check-decagram',
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
    <div class="welcome-section mb-6 mb-md-8">
      <div
        class="d-flex flex-column flex-md-row align-start align-md-center justify-space-between ga-4 ga-md-6"
      >
        <!-- 左侧：用户信息 + 连续学习天数 -->
        <div class="d-flex align-center ga-3 ga-sm-4 flex-grow-1" style="min-width: 0">
          <UserAvatar
            :name="userName"
            :avatar-url="userStore.currentUser?.avatar"
            :size="$vuetify.display.mobile ? 48 : 64"
            rounded="lg"
            avatar-class="flex-shrink-0"
          />
          <div style="min-width: 0">
            <div class="d-flex align-center ga-2 ga-sm-3 flex-wrap">
              <h1
                class="text-h5 text-sm-h4 font-weight-bold text-truncate"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ t('home.greeting', { name: userName }) }}
              </h1>
              <!-- 连续学习天数 -->
              <v-chip
                v-if="stats.learningDays > 0"
                color="warning"
                variant="tonal"
                size="small"
                class="flex-shrink-0"
              >
                <v-icon icon="mdi-fire" size="14" class="mr-1" />
                连续 {{ stats.learningDays }} 天
              </v-chip>
            </div>
            <p
              class="text-caption text-sm-body-2 mt-1 text-truncate"
              :style="{ color: 'rgb(var(--v-theme-on-surface-variant))' }"
            >
              {{ t('home.keepLearning') }}
            </p>
          </div>
        </div>

        <!-- 右侧：平台数据 + 热力图 -->
        <div class="d-flex align-center ga-6 flex-shrink-0">
          <!-- 平台数据 -->
          <div class="d-none d-md-flex align-center ga-6">
            <div class="text-center">
              <div class="text-subtitle-1 font-weight-bold text-primary">
                {{ platformStats.rolePathCount }}
              </div>
              <div class="text-caption text-medium-emphasis">职业方向</div>
            </div>
            <div class="text-center">
              <div class="text-subtitle-1 font-weight-bold text-primary">
                {{ platformStats.roadmapCount }}
              </div>
              <div class="text-caption text-medium-emphasis">学习路线</div>
            </div>
            <div class="text-center">
              <div class="text-subtitle-1 font-weight-bold text-primary">
                {{ platformStats.courseCount }}
              </div>
              <div class="text-caption text-medium-emphasis">门课程</div>
            </div>
            <div class="text-center">
              <div class="text-subtitle-1 font-weight-bold text-primary">
                {{ platformStats.knowledgeNodeCount.toLocaleString() }}
              </div>
              <div class="text-caption text-medium-emphasis">知识节点</div>
            </div>
            <div class="text-center">
              <div class="text-subtitle-1 font-weight-bold text-primary">
                {{ platformStats.articleCount.toLocaleString() }}
              </div>
              <div class="text-caption text-medium-emphasis">篇文章</div>
            </div>
          </div>
          <!-- 热力图 -->
          <div class="d-none d-lg-block">
            <ActivityHeatmap :months="12" />
          </div>
        </div>
      </div>
    </div>

    <!-- 模块1：我在扮演什么角色 - 入口 -->
    <div class="role-entry-section mb-6 mb-md-8">
      <!-- 模块标题 -->
      <div class="section-header mb-4">
        <div class="d-flex align-center ga-4 mb-3">
          <v-badge content="1" :color="quickLinks[0].color" offset-x="4" offset-y="4">
            <v-avatar :color="quickLinks[0].color" size="48" rounded="lg">
              <v-icon :icon="quickLinks[0].icon" color="white" size="26" />
            </v-avatar>
          </v-badge>
          <div class="flex-grow-1">
            <h2
              class="text-h6 text-md-h5 font-weight-bold"
              :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
            >
              {{ quickLinks[0].title }}
            </h2>
            <p class="text-body-2 text-medium-emphasis ma-0 mt-1">
              {{ t('home.guideSubtitle') }}
              <a
                class="text-primary text-decoration-none cursor-pointer font-weight-bold"
                @click="navigateTo('/role')"
              >探索职业</a>
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 模块2：我要学什么 - 正在跟踪的路线图 -->
    <div class="roadmap-section mb-6 mb-md-8">
      <!-- 模块标题 -->
      <div class="section-header mb-4">
        <div class="d-flex align-center ga-4 mb-3">
          <v-badge content="2" :color="quickLinks[1].color" offset-x="4" offset-y="4">
            <v-avatar :color="quickLinks[1].color" size="48" rounded="lg">
              <v-icon :icon="quickLinks[1].icon" color="white" size="26" />
            </v-avatar>
          </v-badge>
          <div class="flex-grow-1">
            <div class="d-flex align-center justify-space-between">
              <h2
                class="text-h6 text-md-h5 font-weight-bold"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ quickLinks[1].title }}
              </h2>
              <v-btn variant="text" size="small" color="primary" rounded="lg" @click="navigateTo('/role')">
                查看全部
                <v-icon icon="mdi-arrow-right" size="16" class="ml-1" />
              </v-btn>
            </div>
            <p class="text-body-2 text-medium-emphasis ma-0 mt-1">
              {{ quickLinks[1].description }}
            </p>
          </div>
        </div>
      </div>

      <!-- 路线图卡片列表 -->
      <v-row>
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
                <div class="flex-grow-1" style="min-width: 0">
                  <div
                    class="text-body-1 font-weight-bold text-truncate"
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
                <span class="text-caption text-medium-emphasis">学习进度</span>
                <span class="text-caption font-weight-bold text-medium-emphasis">{{ role.progress }}%</span>
              </div>
              <v-progress-linear
                :model-value="role.progress"
                color="primary"
                height="6"
                rounded
              />
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 空状态或添加卡片 -->
        <v-col cols="12" sm="6" md="4" lg="3">
          <v-card
            rounded="lg"
            class="h-100 empty-placeholder-card"
            @click="navigateTo('/role')"
          >
            <v-card-text class="pa-4 d-flex align-center justify-center h-100" style="min-height: 120px">
              <div class="text-center">
                <v-icon :icon="recentRoles.length === 0 ? 'mdi-map-marker-plus' : 'mdi-plus'" color="grey" size="32" class="mb-2" />
                <div class="text-body-2 text-medium-emphasis">
                  {{ recentRoles.length === 0 ? '添加第一个学习路线' : '添加更多路线' }}
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>

    <!-- 模块3：我在学什么 - 正在学习的课程 -->
    <div class="course-section mb-6 mb-md-8">
      <!-- 模块标题 -->
      <div class="section-header mb-4">
        <div class="d-flex align-center ga-4 mb-3">
          <v-badge content="3" :color="quickLinks[2].color" offset-x="4" offset-y="4">
            <v-avatar :color="quickLinks[2].color" size="48" rounded="lg">
              <v-icon :icon="quickLinks[2].icon" color="white" size="26" />
            </v-avatar>
          </v-badge>
          <div class="flex-grow-1">
            <div class="d-flex align-center justify-space-between">
              <h2
                class="text-h6 text-md-h5 font-weight-bold"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ quickLinks[2].title }}
              </h2>
              <v-btn variant="text" size="small" color="primary" rounded="lg" @click="navigateTo('/courses')">
                浏览全部
                <v-icon icon="mdi-arrow-right" size="16" class="ml-1" />
              </v-btn>
            </div>
            <p
              class="text-body-2 text-medium-emphasis ma-0 mt-1"
            >
              {{ quickLinks[2].description }}
            </p>
          </div>
        </div>
      </div>

      <!-- 课程卡片列表 -->
      <v-row>
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
                    class="text-body-1 font-weight-bold text-truncate"
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
                <span class="text-caption text-medium-emphasis">学习进度</span>
                <span class="text-caption font-weight-bold text-medium-emphasis">{{ course.progress ?? 0 }}%</span>
              </div>
              <v-progress-linear
                :model-value="course.progress ?? 0"
                color="primary"
                height="6"
                rounded
              />
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 空状态或添加卡片 -->
        <v-col cols="12" sm="6" md="4" lg="3">
          <v-card
            rounded="lg"
            class="h-100 empty-placeholder-card"
            @click="navigateTo('/courses')"
          >
            <v-card-text class="pa-4 d-flex align-center justify-center h-100" style="min-height: 120px">
              <div class="text-center">
                <v-icon :icon="recentCourses.length === 0 ? 'mdi-book-plus' : 'mdi-plus'" color="grey" size="32" class="mb-2" />
                <div class="text-body-2 text-medium-emphasis">
                  {{ recentCourses.length === 0 ? '开始你的第一门课程' : '添加更多课程' }}
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>

    <!-- 模块4：我学会了吗 - 今日复习 -->
    <div class="review-section mb-6 mb-md-8">
      <!-- 模块标题 -->
      <div class="section-header mb-4">
        <div class="d-flex align-center ga-4 mb-3">
          <v-badge content="4" :color="quickLinks[3].color" offset-x="4" offset-y="4">
            <v-avatar :color="quickLinks[3].color" size="48" rounded="lg">
              <v-icon :icon="quickLinks[3].icon" color="white" size="26" />
            </v-avatar>
          </v-badge>
          <div class="flex-grow-1">
            <div class="d-flex align-center justify-space-between">
              <div class="d-flex align-center ga-3">
                <h2
                  class="text-h6 text-md-h5 font-weight-bold"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ quickLinks[3].title }}
                </h2>
                <v-chip
                  v-if="stats.reviewCards > 0"
                  color="success"
                  variant="tonal"
                  size="small"
                >
                  {{ stats.reviewCards }} 张待复习
                </v-chip>
              </div>
              <v-btn variant="text" size="small" color="primary" rounded="lg" @click="navigateTo('/review')">
                开始复习
                <v-icon icon="mdi-arrow-right" size="16" class="ml-1" />
              </v-btn>
            </div>
            <p
              class="text-body-2 text-medium-emphasis ma-0 mt-1"
            >
              {{ quickLinks[3].description }}
            </p>
          </div>
        </div>
      </div>

      <!-- 复习进度和卡片组 -->
      <div class="d-flex flex-wrap ga-3">
        <!-- 今日进度卡片 -->
        <v-card rounded="lg" border class="review-progress-card">
          <v-card-text class="d-flex align-center ga-4 pa-3">
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-primary">
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
              color="primary"
              :size="48"
              :width="5"
            >
              <span class="text-caption font-weight-bold">{{
                reviewData.todayTotal > 0
                  ? Math.round((reviewData.todayCompleted / reviewData.todayTotal) * 100)
                  : 0
              }}%</span>
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
                  class="text-body-2 font-weight-bold text-truncate"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ course.course.name }}
                </div>
                <div class="text-caption text-medium-emphasis text-truncate">
                  共 {{ course.cardCount }} 张卡片
                </div>
              </div>
              <v-chip size="small" color="primary" variant="tonal" class="flex-shrink-0">
                {{ course.dueCardCount }}
              </v-chip>
            </div>
          </v-card-text>
        </v-card>

        <!-- 没有数据时显示占位卡片 -->
        <v-card
          v-if="reviewData.courses.length === 0"
          rounded="lg"
          class="review-deck-card empty-placeholder-card"
          @click="navigateTo('/courses')"
        >
          <v-card-text class="pa-3">
            <div class="d-flex align-center ga-3">
              <div class="icon-container-sm flex-shrink-0">
                <v-icon icon="mdi-cards-outline" color="grey" size="20" />
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div class="text-body-2 font-weight-bold text-truncate text-medium-emphasis">
                  暂无复习卡片
                </div>
                <div class="text-caption text-medium-emphasis text-truncate">
                  学习课程后会自动添加
                </div>
              </div>
              <v-icon icon="mdi-arrow-right" color="grey" size="16" />
            </div>
          </v-card-text>
        </v-card>
      </div>
    </div>

    <!-- 热门区域 -->
    <v-row class="mb-6 mb-md-10">
      <!-- 左列：热门职业 -->
      <v-col cols="12" md="6">
        <div class="d-flex align-center justify-space-between mb-4">
          <h2 class="text-h6 text-sm-h5 font-weight-bold">热门职业</h2>
          <v-btn variant="text" size="small" color="primary" @click="navigateTo('/role')">
            更多
            <v-icon icon="mdi-arrow-right" size="16" class="ml-1" />
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
          <h2 class="text-h6 text-sm-h5 font-weight-bold">热门课程</h2>
          <v-btn variant="text" size="small" color="primary" @click="navigateTo('/courses')">
            更多
            <v-icon icon="mdi-arrow-right" size="16" class="ml-1" />
          </v-btn>
        </div>
        <div class="d-flex flex-wrap ga-2">
          <v-btn
            v-for="course in recommendedCourses"
            :key="course.id"
            variant="tonal"
            color="default"
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
  padding-bottom: 32px;
}

@media (max-width: 960px) {
  .welcome-section {
    padding-top: 16px;
    padding-bottom: 24px;
  }
}

@media (min-width: 960px) {
  .welcome-section {
    padding-bottom: 48px;
  }
}

/* 图标容器 */
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

/* 空数据占位卡片 */
.empty-placeholder-card {
  cursor: pointer;
  border: 1px dashed rgb(var(--v-theme-outline)) !important;
  transition: all 0.2s ease;
}

.empty-placeholder-card:hover {
  border-color: rgb(var(--v-theme-primary)) !important;
}

/* 模块标题区域的角标样式 */
:deep(.section-header .v-badge__badge) {
  border: 2px solid rgb(var(--v-theme-surface));
  font-weight: 700;
  font-size: 12px;
}

/* 响应式调整 */
@media (max-width: 600px) {
  .review-progress-card {
    flex: 1 1 100%;
    min-width: 100%;
  }

  .review-deck-card {
    flex: 1 1 100%;
    max-width: 100%;
  }
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
</style>
