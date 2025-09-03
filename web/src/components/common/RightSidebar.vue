<script setup>
  import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { useI18n } from 'vue-i18n'
  import { usePlatformStats } from '@/composables/usePlatformStats'
  import {
    courseServiceV1,
    professionServiceV1,
    progressServiceV1,
  } from '@/services/api/v1/apiServiceV1'
  import { USER_COURSE_STATE, USER_ROADMAP_STATE } from '@/constants/statusConstants'

  const { t } = useI18n()
  const router = useRouter()

  // 侧边栏定位控制
  const sidebarRef = ref(null)
  const isFixed = ref(false)
  const sidebarWidth = ref('auto')
  const sidebarOriginalTop = 110

  const handleScroll = () => {
    if (!sidebarRef.value) return

    const scrollTop = window.pageYOffset || document.documentElement.scrollTop
    const windowHeight = window.innerHeight

    //console.log(windowHeight + " " + scrollTop + " " + sidebarRef.value.offsetHeight)

    // 当滚动超过(侧边栏高度 - 视窗高度)时，才固定在底部
    if (scrollTop > sidebarRef.value.offsetHeight - windowHeight + sidebarOriginalTop) {
      isFixed.value = true
    } else {
      isFixed.value = false
    }
  }

  const initSidebar = () => {
    if (!sidebarRef.value) return

    // 记录侧边栏的原始高度和位置
    //sidebarOriginalTop = sidebarRef.value.offsetTop;
    sidebarWidth.value = `${sidebarRef.value.offsetWidth}px`

    // 初始检查
    //handleScroll();
  }

  // 定义 props
  const props = defineProps({
    enabledModules: {
      type: Array,
      default: () => ['platform', 'learning', 'tips', 'careers', 'courses'],
    },
    excludeModules: {
      type: Array,
      default: () => [],
    },
  })

  // 计算实际启用的模块
  const actualEnabledModules = computed(() => {
    return props.enabledModules.filter((module) => !props.excludeModules.includes(module))
  })

  // 使用平台统计数据
  const {
    stats: platformStats,
    isLoading: statsLoading,
    error: statsError,
    refresh: refreshStats,
  } = usePlatformStats()

  // 热门数据
  const hotProfessions = ref([])
  const hotCourses = ref([])

  // 独立的学习数据
  const learningData = ref({
    totalProgress: 18,
    completedNodes: 2938,
    totalNodes: 29380,
    roadmaps: [],
    courses: [],
    recentActivities: [],
  })

  // 加载热门职业数据
  const loadHotProfessions = async () => {
    try {
      const response = await professionServiceV1.getHotProfessions()
      if (response && response.data) {
        hotProfessions.value = response.data.slice(0, 3) || []
      }
    } catch (error) {
      console.error('Error loading hot professions:', error)
      // 使用默认数据
      hotProfessions.value = [
        { id: 1, name: t('rightSidebar.professions.fullStack'), learnerCount: 2100 },
        { id: 2, name: t('rightSidebar.professions.frontend'), learnerCount: 1800 },
        { id: 3, name: t('rightSidebar.professions.dataScientist'), learnerCount: 1500 },
      ]
    }
  }

  // 加载热门课程数据
  const loadHotCourses = async () => {
    try {
      const response = await courseServiceV1.getHotCourses()
      if (response && response.data) {
        hotCourses.value = response.data.slice(0, 3) || []
      }
    } catch (error) {
      console.error('Error loading hot courses:', error)
      // 使用默认数据
      hotCourses.value = [
        {
          id: 1,
          name: t('rightSidebar.courses.vueComplete'),
          learnerCount: 2800,
          subscriptionCount: 400,
        },
        {
          id: 2,
          name: t('rightSidebar.courses.pythonData'),
          learnerCount: 2300,
          subscriptionCount: 500,
        },
        {
          id: 3,
          name: t('rightSidebar.courses.javaCore'),
          learnerCount: 2000,
          subscriptionCount: 500,
        },
      ]
    }
  }

  // 加载学习数据
  const loadLearningData = async () => {
    try {
      const [roadmapResponse, courseResponse] = await Promise.all([
        progressServiceV1.getUserRoadmaps(),
        progressServiceV1.getAllCourseProgress(),
      ])

      let roadmaps = []
      let courses = []

      if (roadmapResponse.code === 200 && Array.isArray(roadmapResponse.data)) {
        roadmaps = roadmapResponse.data.map((userRoadmap) => ({
          id: userRoadmap.roadmap.id,
          title: userRoadmap.roadmap.description || `学习路线图 ${userRoadmap.roadmap.id}`,
          progress: userRoadmap.progressPercent || 0,
          status: userRoadmap.status,
          lastActivity: getRelativeTime(userRoadmap.updatedAt),
          profession: userRoadmap.roadmap.profession,
        }))
      }

      if (courseResponse.code === 200 && Array.isArray(courseResponse.data)) {
        courses = courseResponse.data.map((userCourse) => ({
          id: userCourse.id,
          courseId: userCourse.course.id, // 添加实际课程ID
          title: userCourse.course.name,
          progress: userCourse.progressPercent || 0,
          status: userCourse.status,
          lastActivity: getRelativeTime(userCourse.updatedAt),
        }))
      }

      learningData.value = {
        roadmaps,
        courses,
      }
    } catch (error) {
      console.error('Error loading learning data:', error)
    }
  }

  // 获取相对时间
  const getRelativeTime = (dateString) => {
    if (!dateString) return t('rightSidebar.time.unknown')

    const date = new Date(dateString)
    const now = new Date()
    const diffMs = now - date
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 60) {
      return t('rightSidebar.time.minutesAgo', { minutes: diffMins })
    } else if (diffHours < 24) {
      return t('rightSidebar.time.hoursAgo', { hours: diffHours })
    } else {
      return t('rightSidebar.time.daysAgo', { days: diffDays })
    }
  }

  onMounted(() => {
    // 直接检查而不依赖函数内部的检查
    if (actualEnabledModules.value.includes('careers')) {
      loadHotProfessions()
    }
    if (actualEnabledModules.value.includes('courses')) {
      loadHotCourses()
    }
    if (actualEnabledModules.value.includes('learning')) {
      loadLearningData()
    }

    // 等DOM更新完成后初始化侧边栏
    nextTick(() => {
      initSidebar()
      window.addEventListener('scroll', handleScroll)
      window.addEventListener('resize', initSidebar)
    })
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll)
    window.removeEventListener('resize', initSidebar)
  })

  // 获取路线图状态颜色
  const getRoadmapStatusColor = (state) => {
    switch (state) {
      case USER_ROADMAP_STATE.NOT_STARTED:
        return 'grey'
      case USER_ROADMAP_STATE.IN_PROGRESS:
        return 'primary'
      case USER_ROADMAP_STATE.COMPLETED:
        return 'success'
      default:
        return 'grey'
    }
  }

  // 获取课程状态颜色
  const getCourseStatusColor = (state) => {
    switch (state) {
      case USER_COURSE_STATE.NOT_STARTED:
        return 'grey'
      case USER_COURSE_STATE.IN_PROGRESS:
        return 'primary'
      case USER_COURSE_STATE.COMPLETED:
        return 'success'
      default:
        return 'grey'
    }
  }

  // 获取路线图状态文本
  const getRoadmapStatusText = (state) => {
    const stateTexts = {
      [USER_ROADMAP_STATE.NOT_STARTED]: '未开始',
      [USER_ROADMAP_STATE.IN_PROGRESS]: '进行中',
      [USER_ROADMAP_STATE.COMPLETED]: '已完成',
    }
    return stateTexts[state] || t('rightSidebar.status.unknown')
  }

  // 获取课程状态文本
  const getCourseStatusText = (state) => {
    const stateTexts = {
      [USER_COURSE_STATE.NOT_STARTED]: '未开始',
      [USER_COURSE_STATE.IN_PROGRESS]: '进行中',
      [USER_COURSE_STATE.COMPLETED]: '已完成',
    }
    return stateTexts[state] || t('rightSidebar.status.unknown')
  }

  // 获取排名芯片颜色
  const getRankChipColor = (index) => {
    switch (index) {
      case 0:
        return 'gold'
      case 1:
        return 'grey-lighten-1'
      case 2:
        return 'orange-lighten-2'
      default:
        return 'grey-lighten-2'
    }
  }

  // 获取排名图标
  const getRankIcon = (index) => {
    switch (index) {
      case 0:
        return 'mdi-crown'
      case 1:
        return 'mdi-medal'
      case 2:
        return 'mdi-medal-outline'
      default:
        return 'mdi-numeric'
    }
  }

  // 跳转到职业详情
  const goToCareerDetail = (profession) => {
    router.push(`/roadmap/${profession.id}`)
  }

  // 跳转到路线图详情
  const goToRoadmapDetail = (roadmap) => {
    router.push(`/roadmap/${roadmap.id}`)
  }

  // 跳转到课程详情(用户课程)
  const openCourseInNewTab = (course) => {
    const courseId = course.courseId || course.id
    const url = router.resolve({ path: '/read', query: { courseId } }).href
    window.open(url, '_blank')
  }

  // 跳转到课程详情
  const openInNewTab = (courseId) => {
    const url = router.resolve({ path: '/read', query: { courseId } }).href
    window.open(url, '_blank')
  }
</script>

<template>
  <div ref="sidebarRef" :class="{ 'sidebar-fixed': isFixed, 'sidebar-width-fixed': isFixed }">
    <!-- 网站愿景 -->
    <v-alert
      icon="mdi-head-question-outline"
      color="success"
      lines="one"
      variant="tonal"
      rounded="xl"
      class="text-body-1 mb-6 d-flex align-center justify-start"
    >
      <span class="font-weight-medium">
        {{ t('rightSidebar.vision') }}
      </span>
    </v-alert>

    <!-- 网站数据统计 -->
    <v-card
      v-if="actualEnabledModules.includes('platform')"
      flat
      color="grey-lighten-5"
      rounded="xl"
      class="mb-6"
    >
      <v-card-text class="pa-4">
        <div class="d-flex align-center justify-space-between mb-3">
          <div class="d-flex align-center">
            <v-avatar color="grey-darken-1" size="24" class="mr-3">
              <v-icon icon="mdi-chart-box" color="white" size="13"></v-icon>
            </v-avatar>
            <div>
              <h3 class="text-h7 font-weight-bold text-grey-darken-4">
                {{ t('rightSidebar.platformData') }}
              </h3>
            </div>
          </div>
          <!-- 刷新按钮 -->
          <v-btn
            v-if="!platformStats || statsError"
            variant="text"
            size="small"
            color="primary"
            :loading="statsLoading"
            :disabled="statsLoading"
            @click="refreshStats"
          >
            <v-icon icon="mdi-refresh" size="16"></v-icon>
          </v-btn>
        </div>

        <!-- 加载状态骨架屏 -->
        <div v-if="statsLoading && !platformStats">
          <v-row dense>
            <v-col cols="6" class="pa-0-5">
              <div class="data-item pa-1 rounded bg-white text-center">
                <div class="d-flex flex-column align-center justify-center skeleton-container">
                  <div class="skeleton-line skeleton-shimmer mb-1 skeleton-line-primary"></div>
                  <div class="skeleton-line skeleton-shimmer skeleton-line-secondary"></div>
                </div>
              </div>
            </v-col>
            <v-col cols="6" class="pa-0-5">
              <div class="data-item pa-1 rounded bg-white text-center">
                <div class="d-flex flex-column align-center justify-center skeleton-container">
                  <div class="skeleton-line skeleton-shimmer mb-1 skeleton-line-secondary"></div>
                  <div class="skeleton-line skeleton-shimmer skeleton-line-secondary"></div>
                </div>
              </div>
            </v-col>
            <v-col cols="6" class="pa-0-5">
              <div class="data-item pa-1 rounded bg-white text-center">
                <div class="d-flex flex-column align-center justify-center skeleton-container">
                  <div class="skeleton-line skeleton-shimmer mb-1 skeleton-line-secondary"></div>
                  <div class="skeleton-line skeleton-shimmer skeleton-line-secondary"></div>
                </div>
              </div>
            </v-col>
            <v-col cols="6" class="pa-0-5">
              <div class="data-item pa-1 rounded bg-white text-center">
                <div class="d-flex flex-column align-center justify-center skeleton-container">
                  <div class="skeleton-line skeleton-shimmer mb-1 skeleton-line-secondary"></div>
                  <div class="skeleton-line skeleton-shimmer skeleton-line-secondary"></div>
                </div>
              </div>
            </v-col>
          </v-row>
        </div>

        <!-- 数据展示 -->
        <div v-else>
          <!-- 第一排：2个数据项 -->
          <v-row dense class="mb-1">
            <v-col cols="6" class="pa-0-5">
              <div
                class="data-item pa-1 rounded bg-white text-center data-item-clickable"
                @click="router.push('/course/list')"
              >
                <div class="position-relative">
                  <v-icon
                    icon="mdi-open-in-new"
                    size="12"
                    color="primary"
                    class="jump-icon"
                    @click.stop="router.push('/course/list')"
                  >
                  </v-icon>
                  <div class="text-subtitle-1 font-weight-bold text-primary mb-0">
                    {{ platformStats?.courseCount || '1,247' }}
                  </div>
                  <div class="text-caption text-grey-darken-3 caption-small">
                    {{ t('rightSidebar.courseTotal') }}
                  </div>
                </div>
              </div>
            </v-col>
            <v-col cols="6" class="pa-0-5">
              <div
                class="data-item pa-1 rounded bg-white text-center data-item-clickable"
                @click="router.push('/career')"
              >
                <div class="position-relative">
                  <v-icon
                    icon="mdi-open-in-new"
                    size="12"
                    color="teal"
                    class="jump-icon"
                    @click.stop="router.push('/career')"
                  >
                  </v-icon>
                  <div class="text-subtitle-1 font-weight-bold text-teal mb-0">
                    {{ platformStats?.careerPathCount || '156' }}
                  </div>
                  <div class="text-caption text-grey-darken-3 caption-small">
                    {{ t('rightSidebar.careerPaths') }}
                  </div>
                </div>
              </div>
            </v-col>
          </v-row>

          <!-- 第二排：3个数据项 -->
          <v-row dense>
            <v-col cols="4" class="pa-0-5">
              <div class="data-item pa-1 rounded bg-white text-center data-item-static">
                <div class="text-subtitle-1 font-weight-bold text-orange mb-0">
                  {{ platformStats?.roadmapCount || '324' }}
                </div>
                <div class="text-caption text-grey-darken-3 caption-small">
                  {{ t('rightSidebar.learningRoadmaps') }}
                </div>
              </div>
            </v-col>
            <v-col cols="4" class="pa-0-5">
              <div class="data-item pa-1 rounded bg-white text-center data-item-static">
                <div class="text-subtitle-1 font-weight-bold text-purple mb-0">
                  {{ platformStats?.knowledgeNodeCount || '12.5k' }}
                </div>
                <div class="text-caption text-grey-darken-3 caption-small">
                  {{ t('rightSidebar.knowledgeNodes') }}
                </div>
              </div>
            </v-col>
            <v-col cols="4" class="pa-0-5">
              <div class="data-item pa-1 rounded bg-white text-center data-item-static">
                <div class="text-subtitle-1 font-weight-bold text-indigo mb-0">
                  {{ platformStats?.articleCount || '2.8k' }}
                </div>
                <div class="text-caption text-grey-darken-3 caption-small">
                  {{ t('rightSidebar.articleCount') }}
                </div>
              </div>
            </v-col>
          </v-row>
        </div>
      </v-card-text>
    </v-card>

    <!-- 用户学习统计 -->
    <v-card
      v-if="actualEnabledModules.includes('learning')"
      flat
      color="grey-lighten-5"
      rounded="xl"
      class="mb-6"
    >
      <v-card-text class="pa-4">
        <div class="d-flex align-center mb-3">
          <v-avatar color="grey-darken-1" size="24" class="mr-3">
            <v-icon icon="mdi-account-school" color="white" size="13"></v-icon>
          </v-avatar>
          <div>
            <h3 class="text-h7 font-weight-bold text-grey-darken-4">
              {{ t('rightSidebar.myLearning') }}
            </h3>
          </div>
        </div>

        <!-- 我想成为 -->
        <div class="mb-3">
          <div class="d-flex align-center justify-space-between mb-2">
            <div class="d-flex align-center">
              <v-icon
                icon="mdi-briefcase-outline"
                color="teal-darken-1"
                size="14"
                class="mr-1"
              ></v-icon>
              <span class="text-body-2 font-weight-bold text-grey-darken-3">{{
                t('rightSidebar.wantToBe')
              }}</span>
            </div>
            <v-btn
              variant="text"
              size="small"
              color="teal-darken-1"
              @click="router.push('/learning?tab=roadmaps')"
            >
              {{ t('rightSidebar.viewAll') }}
              <v-icon icon="mdi-chevron-right" size="16" class="ml-1"></v-icon>
            </v-btn>
          </div>

          <div
            v-if="learningData.roadmaps && learningData.roadmaps.length > 0"
            class="scrollable-career-list"
          >
            <div
              v-for="roadmap in learningData.roadmaps.slice(0, 10)"
              :key="roadmap.id"
              class="career-item pa-2 rounded bg-white mb-1 d-flex align-center cursor-pointer"
              @click="goToRoadmapDetail(roadmap)"
            >
              <v-avatar color="teal-lighten-4" size="20" class="mr-2">
                <v-icon icon="mdi-account-tie" color="teal-darken-2" size="12"></v-icon>
              </v-avatar>
              <div class="flex-grow-1 min-width-0">
                <div class="text-body-2 font-weight-medium text-grey-darken-4 text-truncate">
                  {{ roadmap.profession?.name || roadmap.title }}
                </div>
                <div class="text-caption text-grey-darken-2">
                  {{ t('rightSidebar.progress') }} {{ roadmap.progress }}% ·
                  {{ roadmap.lastActivity }}
                </div>
              </div>
              <v-chip
                :color="getRoadmapStatusColor(roadmap.status)"
                variant="flat"
                size="x-small"
                class="ml-2"
              >
                {{ getRoadmapStatusText(roadmap.status) }}
              </v-chip>
            </div>
          </div>

          <div v-else class="text-center py-2">
            <v-icon
              icon="mdi-briefcase-search"
              color="grey-lighten-1"
              size="20"
              class="mb-1"
            ></v-icon>
            <div class="text-caption text-grey-darken-2">
              {{ t('rightSidebar.noLearningCareers') }}
            </div>
          </div>
        </div>

        <!-- 我在学习 -->
        <div class="mb-1">
          <div class="d-flex align-center justify-space-between mb-2">
            <div class="d-flex align-center">
              <v-icon icon="mdi-book-open-outline" color="primary" size="14" class="mr-1"></v-icon>
              <span class="text-body-2 font-weight-bold text-grey-darken-3">{{
                t('rightSidebar.studying')
              }}</span>
            </div>
            <v-btn
              variant="text"
              size="small"
              color="primary"
              @click="router.push('/learning?tab=courses')"
            >
              {{ t('rightSidebar.viewAll') }}
              <v-icon icon="mdi-chevron-right" size="16" class="ml-1"></v-icon>
            </v-btn>
          </div>

          <div
            v-if="learningData.courses && learningData.courses.length > 0"
            class="scrollable-course-list"
          >
            <div
              v-for="course in learningData.courses.slice(0, 10)"
              :key="course.id"
              class="course-item pa-2 rounded bg-white mb-1 d-flex align-center cursor-pointer"
              @click="openCourseInNewTab(course)"
            >
              <v-avatar color="blue-lighten-4" size="20" class="mr-2">
                <v-icon icon="mdi-book" color="primary" size="12"></v-icon>
              </v-avatar>
              <div class="flex-grow-1 min-width-0">
                <div class="text-body-2 font-weight-medium text-grey-darken-4 text-truncate">
                  {{ course.title }}
                </div>
                <div class="text-caption text-grey-darken-2">
                  {{ t('rightSidebar.progress') }} {{ course.progress }}% ·
                  {{ course.lastActivity }}
                </div>
              </div>
              <v-chip
                :color="getCourseStatusColor(course.status)"
                variant="flat"
                size="x-small"
                class="ml-2"
              >
                {{ getCourseStatusText(course.status) }}
              </v-chip>
            </div>
          </div>

          <div v-else class="text-center py-2">
            <v-icon icon="mdi-book-search" color="grey-lighten-1" size="20" class="mb-1"></v-icon>
            <div class="text-caption text-grey-darken-2">
              {{ t('rightSidebar.noLearningCourses') }}
            </div>
          </div>
        </div>
      </v-card-text>
    </v-card>

    <!-- 学习小贴士 -->
    <v-card
      v-if="actualEnabledModules.includes('tips')"
      flat
      color="amber-lighten-5"
      rounded="xl"
      class="mb-6"
    >
      <v-card-text class="pa-4">
        <div class="d-flex align-center mb-3">
          <v-avatar color="amber-darken-1" size="24" class="mr-3">
            <v-icon icon="mdi-lightbulb-outline" color="white" size="13"></v-icon>
          </v-avatar>
          <div>
            <h3 class="text-h7 font-weight-bold text-grey-darken-4">
              {{ t('rightSidebar.learningTips') }}
            </h3>
          </div>
        </div>

        <div class="tip-item pa-3 rounded-lg bg-amber-lighten-4 mb-2 d-flex align-center">
          <v-icon
            icon="mdi-lightbulb"
            color="amber-darken-2"
            size="16"
            class="mr-2 flex-shrink-0"
          ></v-icon>
          <div class="flex-grow-1">
            <div class="text-body-2 text-grey-darken-4">
              {{ t('rightSidebar.learningQuotes.quote1') }}
            </div>
          </div>
        </div>

        <div class="tip-item pa-3 rounded-lg bg-amber-lighten-4 mb-2 d-flex align-center">
          <v-icon
            icon="mdi-lightbulb"
            color="amber-darken-2"
            size="16"
            class="mr-2 flex-shrink-0"
          ></v-icon>
          <div class="flex-grow-1">
            <div class="text-body-2 text-grey-darken-4">
              {{ t('rightSidebar.learningQuotes.quote2') }}
            </div>
          </div>
        </div>

        <div class="tip-item pa-3 rounded-lg bg-amber-lighten-4 d-flex align-center">
          <v-icon
            icon="mdi-lightbulb"
            color="amber-darken-2"
            size="16"
            class="mr-2 flex-shrink-0"
          ></v-icon>
          <div class="flex-grow-1">
            <div class="text-body-2 text-grey-darken-4">
              {{ t('rightSidebar.learningQuotes.quote3') }}
            </div>
          </div>
        </div>
      </v-card-text>
    </v-card>

    <!-- 职业排名 -->
    <v-card
      v-if="actualEnabledModules.includes('careers')"
      flat
      color="grey-lighten-5"
      rounded="xl"
      class="mb-6"
    >
      <v-card-text class="pa-4">
        <div class="d-flex align-center justify-space-between mb-3">
          <div class="d-flex align-center">
            <v-avatar color="grey-darken-1" size="24" class="mr-3">
              <v-icon icon="mdi-trophy" color="white" size="13"></v-icon>
            </v-avatar>
            <div>
              <h3 class="text-h7 font-weight-bold text-grey-darken-4">
                {{ t('rightSidebar.hotCareers') }}
              </h3>
            </div>
          </div>
          <v-btn variant="text" size="small" color="teal-darken-1" @click="router.push('/career')">
            {{ t('rightSidebar.viewAll') }}
            <v-icon icon="mdi-chevron-right" size="16" class="ml-1"></v-icon>
          </v-btn>
        </div>

        <!-- 职业排名卡片 -->
        <div
          v-for="(profession, index) in hotProfessions"
          :key="profession.id"
          class="ranking-item pa-3 rounded-lg bg-white mb-2 d-flex align-center cursor-pointer"
          @click="goToCareerDetail(profession)"
        >
          <v-chip :color="getRankChipColor(index)" size="small" class="mr-3 rank-chip">
            <v-icon :icon="getRankIcon(index)" size="12" class="mr-1"></v-icon>
            {{ index + 1 }}
          </v-chip>
          <div class="flex-grow-1">
            <div class="text-body-2 font-weight-bold text-grey-darken-4">{{ profession.name }}</div>
            <div class="text-caption text-grey-darken-2">
              {{
                t('rightSidebar.peopleStudying', {
                  count: (profession.learnerCount || 0).toLocaleString(),
                })
              }}
            </div>
          </div>
        </div>
      </v-card-text>
    </v-card>

    <!-- 课程排名 -->
    <v-card
      v-if="actualEnabledModules.includes('courses')"
      flat
      color="grey-lighten-5"
      rounded="xl"
    >
      <v-card-text class="pa-4">
        <div class="d-flex align-center justify-space-between mb-3">
          <div class="d-flex align-center">
            <v-avatar color="grey-darken-1" size="24" class="mr-3">
              <v-icon icon="mdi-fire" color="white" size="13"></v-icon>
            </v-avatar>
            <div>
              <h3 class="text-h7 font-weight-bold text-grey-darken-4">
                {{ t('rightSidebar.hotCourses') }}
              </h3>
            </div>
          </div>
          <v-btn
            variant="text"
            size="small"
            color="blue-darken-1"
            @click="router.push('/course/list')"
          >
            {{ t('rightSidebar.viewAll') }}
            <v-icon icon="mdi-chevron-right" size="16" class="ml-1"></v-icon>
          </v-btn>
        </div>

        <!-- 课程排名卡片 -->
        <div
          v-for="(course, index) in hotCourses"
          :key="course.id"
          class="ranking-item pa-3 rounded-lg bg-white mb-2 d-flex align-center cursor-pointer"
          @click="openInNewTab(course.id)"
        >
          <v-chip :color="getRankChipColor(index)" size="small" class="mr-3 rank-chip">
            <v-icon :icon="getRankIcon(index)" size="12" class="mr-1"></v-icon>
            {{ index + 1 }}
          </v-chip>
          <div class="flex-grow-1">
            <div class="text-body-2 font-weight-bold text-grey-darken-4">{{ course.name }}</div>
            <div class="text-caption text-grey-darken-2">
              {{
                t('rightSidebar.peopleBookmarked', {
                  count: (
                    (course.learnerCount || 0) + (course.subscriptionCount || 0)
                  ).toLocaleString(),
                })
              }}
            </div>
          </div>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<style scoped>
  /* 固定在底部的样式 */
  .sidebar-fixed {
    position: sticky;
    top: -350px;
    z-index: 900;
  }

  .text-h7 {
    font-size: 1.15rem;
  }

  /* 鼠标指针样式 */
  .cursor-pointer {
    cursor: pointer;
  }

  /* 新增样式 */
  .data-item {
    transition: all 0.2s ease;
    border: 1px solid rgba(0, 0, 0, 0.06);
  }

  .data-item:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  /* 跳转图标样式 */
  .jump-icon {
    position: absolute;
    top: 2px;
    right: 2px;
    opacity: 0.7;
    transition: all 0.2s ease;
  }

  .data-item-clickable:hover .jump-icon {
    opacity: 1;
    transform: scale(1.2);
  }

  /* 可点击的数据项样式 */
  .data-item-clickable {
    cursor: pointer;
  }

  /* 静态数据项样式 - 覆盖默认悬停效果 */
  .data-item-static:hover {
    transform: none;
    box-shadow: none;
  }

  .vision-card {
    border: 2px solid #ffebee;
  }

  .vision-content {
    background: linear-gradient(135deg, #ffebee 0%, #fce4ec 100%);
    border: 1px solid #f8bbd9;
  }

  .stat-card {
    transition: all 0.2s ease;
    border: 1px solid rgba(0, 0, 0, 0.06);
  }

  .stat-card:hover {
    transform: translateX(4px);
    border-color: rgba(76, 175, 80, 0.3);
  }

  .progress-item {
    transition: all 0.2s ease;
  }

  .progress-item:hover {
    transform: scale(1.05);
  }

  .ranking-item {
    transition: all 0.2s ease;
    border: 1px solid rgba(0, 0, 0, 0.06);
  }

  .ranking-item:hover {
    transform: translateX(4px);
    border-color: rgba(25, 118, 210, 0.3);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .rank-chip {
    min-width: 50px !important;
  }

  /* 滚动列表样式 */
  .scrollable-career-list,
  .scrollable-course-list {
    max-height: 140px; /* 约2.5个项目的高度：每项50px * 2.5 + 间距 */
    overflow-y: auto;
    scrollbar-width: none; /* Firefox 隐藏滚动条 */
    -ms-overflow-style: none; /* IE 隐藏滚动条 */
  }

  /* Webkit 浏览器隐藏滚动条 */
  .scrollable-career-list::-webkit-scrollbar,
  .scrollable-course-list::-webkit-scrollbar {
    display: none;
  }

  /* 学习项目样式 */
  .career-item,
  .course-item {
    transition: all 0.2s ease;
    border: 1px solid rgba(0, 0, 0, 0.06);
  }

  .career-item:hover,
  .course-item:hover {
    transform: translateX(2px);
    border-color: rgba(76, 175, 80, 0.3);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  }

  .text-truncate {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  /* 紧凑间距样式 */
  .pa-0-5 {
    padding: 2px !important;
  }

  @keyframes shimmer {
    0% {
      background-position: -200px 0;
    }
    100% {
      background-position: calc(200px + 100%) 0;
    }
  }

  .skeleton-shimmer {
    background: linear-gradient(90deg, #fafafa 25%, #f0f0f0 50%, #fafafa 75%);
    background-size: 200px 100%;
    animation: shimmer 2.5s infinite;
  }

  .skeleton-container {
    min-height: 40px;
  }

  .skeleton-line-primary {
    width: 60%;
    height: 12px;
    border-radius: 6px;
  }

  .skeleton-line-secondary {
    width: 60%;
    height: 12px;
    border-radius: 6px;
  }

  .caption-small {
    font-size: 10px;
  }

  .sidebar-width-fixed {
    width: v-bind(sidebarWidth);
  }
</style>
