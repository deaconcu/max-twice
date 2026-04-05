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
    bookmarkedProfessions: [],
    learningProfessions: [],
    learningCourses: [],
    reviewSummary: {
      todayTotal: 0,
      todayCompleted: 0,
      streakDays: 0,
      courses: [],
    },
    hotProfessions: [],
    hotCourses: [],
    beginnerProfessions: [],
    beginnerRoadmaps: [],
    beginnerCourses: [],
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

// 计算属性：收藏的职业
const bookmarkedProfessions = computed(() => homeData.value.bookmarkedProfessions)

// 是否显示"查看更多"（等于10个时显示）
const showMoreBookmarkedProfessions = computed(() => bookmarkedProfessions.value.length >= 10)

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

// 计算属性：热门职业榜单
const hotProfessions = computed(() => {
  return homeData.value.hotProfessions.map((item, index) => ({
    id: index + 1,
    roleId: item.id,
    name: item.name,
    icon: item.icon ?? 'mdi-briefcase-variant',
    iconColor: getColorByString(item.name),
    description: item.description ?? '',
    learnerCount: item.learnerCount ?? 0,
  }))
})

// 计算属性：热门课程榜单
const hotCourses = computed(() => homeData.value.hotCourses)

// 计算属性：新手推荐职业
const beginnerProfessions = computed(() => {
  return homeData.value.beginnerProfessions.map((item) => ({
    id: item.id,
    name: item.name,
    icon: item.icon ?? 'mdi-briefcase-variant',
    iconColor: getColorByString(item.name),
  }))
})

// 计算属性：新手推荐路线图
const beginnerRoadmaps = computed(() => {
  return homeData.value.beginnerRoadmaps.map((item) => ({
    id: item.id,
    name: item.profession?.name ?? '未知职业',
    icon: item.profession?.icon ?? 'mdi-briefcase-variant',
    iconColor: getColorByString(item.profession?.name ?? ''),
    nodeCount: item.nodeCount ?? 0,
  }))
})

// 计算属性：新手推荐课程
const beginnerCourses = computed(() => {
  return homeData.value.beginnerCourses.map((item) => ({
    id: item.id,
    name: item.name,
    icon: item.icon ?? 'mdi-book-open-variant',
    iconColor: getColorByString(item.name),
  }))
})

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
    description: '每个人都在扮演多种角色：程序员、父亲、投资者、跑者...收藏你想扮演的角色，开启系统化学习',
    icon: 'mdi-account-question',
    color: 'warning',
    path: '/role',
  },
  {
    step: 2,
    title: '我要学什么',
    description: '学习路线是由社区用户创建的课程组合，帮助你系统地掌握某个职业所需的技能',
    icon: 'mdi-map-marker-path',
    color: 'primary',
    path: '/role',
  },
  {
    step: 3,
    title: '我在学什么',
    description: '每门课程包含多个知识节点和文章，帮助你深入理解某个领域',
    icon: 'mdi-book-open-page-variant',
    color: 'info',
    path: '/courses',
  },
  {
    step: 4,
    title: '我学会了吗',
    description: '复习系统基于艾宾浩斯遗忘曲线，在最佳时机提醒你复习，学习课程时会自动生成记忆卡片',
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

        <!-- 右侧：热力图 -->
        <div class="d-none d-lg-flex align-center flex-shrink-0">
          <ActivityHeatmap :months="12" />
        </div>
      </div>
    </div>

    <!-- 模块1：我在扮演什么角色 - 收藏的职业 -->
    <div class="role-entry-section mb-8 mb-md-12">
      <!-- 模块标题 -->
      <div class="section-header mb-4">
        <div class="d-flex align-center ga-4 mb-3">
          <v-badge content="1" :color="quickLinks[0].color" offset-x="4" offset-y="4">
            <v-avatar :color="quickLinks[0].color" size="48" rounded="lg">
              <v-icon :icon="quickLinks[0].icon" color="white" size="26" />
            </v-avatar>
          </v-badge>
          <div class="flex-grow-1">
            <div class="d-flex align-center justify-space-between">
              <h2
                class="text-h6 text-md-h5 font-weight-bold"
                :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
              >
                {{ quickLinks[0].title }}
              </h2>
              <div class="d-flex align-center ga-4">
                <span class="text-body-2 text-medium-emphasis d-none d-sm-inline">
                  平台已有 <span class="text-primary font-weight-bold">{{ platformStats.rolePathCount }}</span> 个职业方向
                </span>
                <v-btn variant="text" size="small" color="primary" rounded="lg" @click="navigateTo('/role')">
                  探索职业
                  <v-icon icon="mdi-arrow-right" size="16" class="ml-1" />
                </v-btn>
              </div>
            </div>
            <p class="text-body-2 text-medium-emphasis ma-0 mt-1">
              {{ quickLinks[0].description }}
            </p>
          </div>
        </div>
      </div>

      <!-- 收藏的职业列表 -->
      <template v-if="bookmarkedProfessions.length > 0">
        <div class="d-flex flex-wrap ga-3">
          <v-chip
            v-for="profession in bookmarkedProfessions"
            :key="profession.id"
            color="primary"
            variant="tonal"
            size="large"
            rounded="lg"
            class="profession-chip"
            @click="openRole(profession.id)"
          >
            <DynamicIcon
              :icon="profession.icon"
              default-icon="mdi-briefcase-variant"
              :size="18"
              :color="getColorByString(profession.name)"
              class="mr-2"
            />
            {{ profession.name }}
          </v-chip>
          <!-- 查看更多 -->
          <v-chip
            v-if="showMoreBookmarkedProfessions"
            color="grey"
            variant="outlined"
            size="large"
            rounded="lg"
            class="profession-chip"
            @click="navigateTo('/profile/bookmarks?type=profession')"
          >
            查看更多
            <v-icon icon="mdi-arrow-right" size="16" class="ml-1" />
          </v-chip>
        </div>
      </template>

      <!-- 空状态：新手引导 -->
      <template v-else>
        <div class="empty-guide-text">
          <span class="text-body-2 text-medium-emphasis">试试添加新角色：</span>
          <template v-for="(profession, index) in beginnerProfessions" :key="profession.id">
            <a class="text-body-2 link" @click="openRole(profession.id)">{{ profession.name }}</a>
            <span v-if="index < beginnerProfessions.length - 1" class="text-body-2 text-medium-emphasis">，</span>
          </template>
          <span class="text-body-2 text-medium-emphasis"> 或者</span>
          <a class="text-body-2 link font-weight-medium" @click="navigateTo('/role')">探索更多</a>
        </div>
      </template>
    </div>

    <!-- 模块2：我要学什么 - 正在跟踪的路线图 -->
    <div class="roadmap-section mb-8 mb-md-12">
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
              <div class="d-flex align-center ga-4">
                <span class="text-body-2 text-medium-emphasis d-none d-sm-inline">
                  共 <span class="text-primary font-weight-bold">{{ platformStats.roadmapCount }}</span> 条学习路线
                </span>
                <v-btn variant="text" size="small" color="primary" rounded="lg" @click="navigateTo('/role')">
                  查看全部
                  <v-icon icon="mdi-arrow-right" size="16" class="ml-1" />
                </v-btn>
              </div>
            </div>
            <p class="text-body-2 text-medium-emphasis ma-0 mt-1">
              {{ quickLinks[1].description }}
            </p>
          </div>
        </div>
      </div>

      <!-- 路线图卡片列表 -->
      <template v-if="recentRoles.length > 0">
        <v-row>
          <v-col v-for="role in displayRoles" :key="role.id" cols="12" sm="6" md="4" lg="3">
            <v-card rounded="xl" hover class="h-100 module-card" @click="openRoadmap(role.roadmapId)">
              <v-card-text class="pa-4 pa-sm-5">
                <div class="d-flex align-center ga-3 mb-4">
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
                      class="text-subtitle-1 font-weight-bold text-truncate"
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
                  <span class="text-caption font-weight-bold text-primary">{{ role.progress }}%</span>
                </div>
                <v-progress-linear
                  :model-value="role.progress"
                  color="primary"
                  bg-color="primary"
                  bg-opacity="0.15"
                  height="8"
                  rounded
                />
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 添加更多卡片 -->
          <v-col v-if="hasMoreRoles" cols="12" sm="6" md="4" lg="3">
            <v-card
              rounded="xl"
              class="h-100 empty-placeholder-card"
              @click="navigateTo('/role')"
            >
              <v-card-text class="pa-4 pa-sm-5 d-flex align-center justify-center h-100" style="min-height: 140px">
                <div class="text-center">
                  <v-avatar color="primary" size="48" class="mb-3" variant="tonal">
                    <v-icon icon="mdi-plus" size="24" />
                  </v-avatar>
                  <div class="text-body-2 text-medium-emphasis">添加更多路线</div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </template>

      <!-- 空状态：新手引导 -->
      <template v-else>
        <div class="empty-guide-text">
          <span class="text-body-2 text-medium-emphasis">试试添加路线图：</span>
          <template v-for="(roadmap, index) in beginnerRoadmaps" :key="roadmap.id">
            <a class="text-body-2 link" @click="openRoadmap(roadmap.id)">{{ roadmap.name }}</a>
            <span v-if="index < beginnerRoadmaps.length - 1" class="text-body-2 text-medium-emphasis">，</span>
          </template>
          <span class="text-body-2 text-medium-emphasis">，或者在</span>
          <a class="text-body-2 link font-weight-medium" @click="navigateTo('/role')">角色中心</a>
          <span class="text-body-2 text-medium-emphasis">选择学习你感兴趣的路线</span>
        </div>
      </template>
    </div>

    <!-- 模块3：我在学什么 - 正在学习的课程 -->
    <div class="course-section mb-8 mb-md-12">
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
              <div class="d-flex align-center ga-4">
                <span class="text-body-2 text-medium-emphasis d-none d-sm-inline">
                  共 <span class="text-primary font-weight-bold">{{ platformStats.courseCount }}</span> 门课程，<span class="text-primary font-weight-bold">{{ platformStats.knowledgeNodeCount.toLocaleString() }}</span> 个知识节点，<span class="text-primary font-weight-bold">{{ platformStats.articleCount.toLocaleString() }}</span> 篇文章
                </span>
                <v-btn variant="text" size="small" color="primary" rounded="lg" @click="navigateTo('/courses')">
                  浏览全部
                  <v-icon icon="mdi-arrow-right" size="16" class="ml-1" />
                </v-btn>
              </div>
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
      <template v-if="recentCourses.length > 0">
        <v-row>
          <v-col v-for="course in displayCourses" :key="course.id" cols="12" sm="6" md="4" lg="3">
            <v-card rounded="xl" hover class="h-100 module-card" @click="openCourse(course.id)">
              <v-card-text class="pa-4 pa-sm-5">
                <div class="d-flex align-center ga-3 mb-4">
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
                      class="text-subtitle-1 font-weight-bold text-truncate"
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
                  <span class="text-caption font-weight-bold text-primary">{{ course.progress ?? 0 }}%</span>
                </div>
                <v-progress-linear
                  :model-value="course.progress ?? 0"
                  color="primary"
                  bg-color="primary"
                  bg-opacity="0.15"
                  height="8"
                  rounded
                />
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 添加更多卡片 -->
          <v-col v-if="hasMoreCourses" cols="12" sm="6" md="4" lg="3">
            <v-card
              rounded="xl"
              class="h-100 empty-placeholder-card"
              @click="navigateTo('/courses')"
            >
              <v-card-text class="pa-4 pa-sm-5 d-flex align-center justify-center h-100" style="min-height: 140px">
                <div class="text-center">
                  <v-avatar color="primary" size="48" class="mb-3" variant="tonal">
                    <v-icon icon="mdi-plus" size="24" />
                  </v-avatar>
                  <div class="text-body-2 text-medium-emphasis">添加更多课程</div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </template>

      <!-- 空状态：新手引导 -->
      <template v-else>
        <div class="empty-guide-text">
          <span class="text-body-2 text-medium-emphasis">尝试开始学习课程：</span>
          <template v-for="(course, index) in beginnerCourses" :key="course.id">
            <a class="text-body-2 link" @click="openCourse(course.id)">{{ course.name }}</a>
            <span v-if="index < beginnerCourses.length - 1" class="text-body-2 text-medium-emphasis">，</span>
          </template>
          <span class="text-body-2 text-medium-emphasis">，或者</span>
          <a class="text-body-2 link font-weight-medium" @click="navigateTo('/courses')">浏览更多</a>
        </div>
      </template>
    </div>

    <!-- 模块4：我学会了吗 - 今日复习 -->
    <div class="review-section mb-8 mb-md-12">
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
      <template v-if="reviewData.todayTotal > 0 || reviewData.courses.length > 0">
        <div class="d-flex flex-wrap ga-4">
          <!-- 今日进度卡片 -->
          <v-card rounded="xl" class="review-progress-card module-card">
            <v-card-text class="d-flex align-center ga-5 pa-4 pa-sm-5">
              <v-progress-circular
                :model-value="
                  reviewData.todayTotal > 0
                    ? (reviewData.todayCompleted / reviewData.todayTotal) * 100
                    : 0
                "
                color="primary"
                :size="72"
                :width="6"
              >
                <div class="text-center">
                  <div class="text-h6 font-weight-bold text-primary">
                    {{ reviewData.todayCompleted }}
                  </div>
                  <div class="text-caption text-medium-emphasis">/{{ reviewData.todayTotal }}</div>
                </div>
              </v-progress-circular>
              <div>
                <div class="text-subtitle-1 font-weight-bold mb-1">今日进度</div>
                <div class="text-body-2 text-medium-emphasis">
                  已完成 {{
                    reviewData.todayTotal > 0
                      ? Math.round((reviewData.todayCompleted / reviewData.todayTotal) * 100)
                      : 0
                  }}%
                </div>
                <v-chip
                  v-if="reviewData.streakDays > 0"
                  color="warning"
                  variant="tonal"
                  size="small"
                  class="mt-2"
                >
                  <v-icon icon="mdi-fire" size="14" class="mr-1" />
                  连续 {{ reviewData.streakDays }} 天
                </v-chip>
              </div>
            </v-card-text>
          </v-card>

          <!-- 待复习课程列表 -->
          <v-card
            v-for="course in reviewData.courses"
            :key="course.course.id"
            rounded="xl"
            hover
            class="review-deck-card module-card"
            @click="navigateTo('/review')"
          >
            <v-card-text class="pa-4 pa-sm-5">
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
                    class="text-subtitle-2 font-weight-bold text-truncate"
                    :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                  >
                    {{ course.course.name }}
                  </div>
                  <div class="text-caption text-medium-emphasis text-truncate">
                    共 {{ course.cardCount }} 张卡片
                  </div>
                </div>
                <v-chip size="small" color="primary" variant="flat" class="flex-shrink-0">
                  {{ course.dueCardCount }} 待复习
                </v-chip>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </template>

      <!-- 空状态：新手引导 -->
      <template v-else>
        <div class="empty-guide-text">
          <span class="text-body-2 text-medium-emphasis">学习课程时会自动生成记忆卡片，系统会在最佳时机提醒你复习。</span>
          <a class="text-body-2 link font-weight-medium" @click="navigateTo('/courses')">先去学习课程</a>
        </div>
      </template>
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
            v-for="role in hotProfessions"
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
            v-for="course in hotCourses"
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

/* 模块卡片样式 */
.module-card {
  border: 1px solid rgb(var(--v-theme-outline));
  transition: all 0.2s ease;
}

.module-card:hover {
  border-color: rgb(var(--v-theme-primary));
}

/* 图标容器 */
.icon-container {
  width: 48px;
  height: 48px;
  background: rgba(var(--v-theme-primary), 0.08);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 图标容器 - 小尺寸 */
.icon-container-sm {
  width: 40px;
  height: 40px;
  background: rgba(var(--v-theme-primary), 0.08);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 复习进度卡片 */
.review-progress-card {
  flex: 0 0 auto;
  min-width: 240px;
}

/* 复习卡片组 */
.review-deck-card {
  flex: 1 1 200px;
  min-width: 200px;
  max-width: 320px;
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

/* 职业标签样式 */
.profession-chip {
  cursor: pointer;
  transition: all 0.2s ease;
}

.profession-chip:hover {
  transform: translateY(-1px);
}

/* 空状态引导文字 */
.empty-guide-text {
  line-height: 2;
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
