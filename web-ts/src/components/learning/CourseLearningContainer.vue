<script setup lang="ts">
import { ref, computed, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { progressServiceV1 } from '@/services/api/v1/apiServiceV1'
import CourseLearningCard from './CourseLearningCard.vue'
import { UserCourseState } from '@/types/enums'
import type { UserCourse } from '@/types/userCourse'

interface ProcessedCourse {
  id: string | number
  courseId: string | number
  title: string
  name: string
  description?: string
  progress: number
  state: string
  startedAt?: string
  completedAt?: string
  createdAt?: string
  updatedAt?: string
  lastActivity: string
  category: string
  difficulty: string
  [key: string]: any
}

interface Props {
  searchQuery?: string
  selectedStatus?: string
}

const props = withDefaults(defineProps<Props>(), {
  searchQuery: '',
  selectedStatus: 'all'
})

const { t } = useI18n()
const showSnackbar = inject<(message: string) => void>('showSnackbar')
const router = useRouter()

// 响应式数据
const courses = ref<ProcessedCourse[]>([])
const loading = ref<boolean>(true)
const error = ref<string | null>(null)

// 筛选后的课程数据
const filteredCourses = computed((): ProcessedCourse[] => {
  return courses.value.filter((course) => {
    // 状态筛选
    if (props.selectedStatus !== 'all' && course.state !== props.selectedStatus) {
      return false
    }
    // 搜索筛选
    if (props.searchQuery) {
      const query = props.searchQuery.toLowerCase()
      return (
        course.title?.toLowerCase().includes(query) ||
        course.description?.toLowerCase().includes(query)
      )
    }
    return true
  })
})

// 加载课程数据
const loadCourseData = async (): Promise<void> => {
  loading.value = true
  error.value = null
  
  try {
    const response = await progressServiceV1.getAllCourseProgress()
    
    if (response.code === 200 && Array.isArray(response.data)) {
      courses.value = (response.data as UserCourse[]).map((userCourse: UserCourse): ProcessedCourse => {
        return {
          id: userCourse.id,
          courseId: userCourse.course.id,
          title: userCourse.course.name,
          name: userCourse.course.name,
          description: userCourse.course.description,
          progress: userCourse.progressPercent ? userCourse.progressPercent / 100 : 0,
          state: String(userCourse.state),
          startedAt: userCourse.startedAt,
          completedAt: userCourse.completedAt,
          createdAt: userCourse.createdAt,
          updatedAt: userCourse.updatedAt,
          lastActivity: getRelativeTime(userCourse.updatedAt),
          category: getCategoryFromDescription(userCourse.course.description),
          difficulty: getDifficultyFromStatus(userCourse.state as UserCourseState),
        }
      })
    }
  } catch (err) {
    console.error('Error loading course data:', err)
    error.value = t('learning.loadFailed')
    showSnackbar?.(t('learning.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 获取相对时间
const getRelativeTime = (dateString?: string): string => {
  if (!dateString) return t('learning.timeAgo.unknownTime')

  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 60) {
    return t('learning.timeAgo.minutesAgo', { minutes: diffMins })
  } else if (diffHours < 24) {
    return t('learning.timeAgo.hoursAgo', { hours: diffHours })
  } else {
    return t('learning.timeAgo.daysAgo', { days: diffDays })
  }
}

// 根据课程描述推断分类
const getCategoryFromDescription = (description?: string): string => {
  if (!description) return 'other'
  const desc = description.toLowerCase()

  if (
    desc.includes('前端') ||
    desc.includes('vue') ||
    desc.includes('react') ||
    desc.includes('javascript')
  )
    return 'frontend'
  if (desc.includes('数据') || desc.includes('python') || desc.includes('分析'))
    return 'datascience'
  if (desc.includes('ai') || desc.includes('机器学习') || desc.includes('深度学习')) return 'ai'
  if (desc.includes('java') || desc.includes('spring') || desc.includes('后端')) return 'backend'

  return 'other'
}

// 根据状态推断难度
const getDifficultyFromStatus = (state: UserCourseState): string => {
  switch (state) {
    case UserCourseState.NOT_STARTED:
      return 'beginner'
    case UserCourseState.IN_PROGRESS:
      return 'intermediate'
    case UserCourseState.COMPLETED:
      return 'advanced'
    default:
      return 'beginner'
  }
}

// 获取状态文本
const getStatusText = (state: string): string => {
  const courseStateTexts: Record<string, string> = {
    [UserCourseState.NOT_STARTED]: '未开始',
    [UserCourseState.IN_PROGRESS]: '进行中',
    [UserCourseState.COMPLETED]: '已完成',
  }
  return courseStateTexts[state] || t('learning.unknownStatus', '未知状态')
}

// 打开课程
const openCourse = (course: any): void => {
  const courseId = course.courseId || course.id
  const url = router.resolve({ path: '/read', query: { courseId } }).href
  window.open(url, '_blank')
}

// 关闭/退出学习课程
const closeCourse = (course: any, event?: Event): void => {
  if (event) {
    event.stopPropagation()
  }
  // TODO: 实现关闭逻辑
}

onMounted(() => {
  loadCourseData()
})

// 暴露给父组件的数据和方法
defineExpose({
  courses: filteredCourses,
  loading,
  error,
  refresh: loadCourseData
})
</script>

<template>
  <div>
    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="text-grey-darken-2 mt-4">{{ t('learning.loadingCourses') }}</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="text-center py-8">
      <v-alert type="error" variant="tonal">{{ error }}</v-alert>
      <v-btn color="primary" variant="flat" class="mt-4" @click="loadCourseData">
        {{ t('common.retry') }}
      </v-btn>
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredCourses.length === 0" class="text-center py-8">
      <v-card flat color="grey-lighten-5" rounded="lg">
        <v-card-text class="py-8">
          <v-icon
            icon="mdi-book-search"
            size="64"
            color="grey-lighten-1"
            class="mb-4"
          ></v-icon>
          <h3 class="text-h6 text-grey-darken-2 mb-2">
            {{
              selectedStatus === 'all'
                ? t('learning.noCourses')
                : t('learning.noStatusCourses', { status: getStatusText(selectedStatus) })
            }}
          </h3>
          <p class="text-body-2 text-grey-darken-1">
            {{
              selectedStatus === 'all'
                ? t('learning.browseCoursesDesc')
                : t('learning.tryOtherStatusCourse')
            }}
          </p>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            class="mt-4"
            @click="router.push('/course/list')"
          >
            <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
            {{ t('learning.browseCourses') }}
          </v-btn>
        </v-card-text>
      </v-card>
    </div>

    <!-- 课程列表 -->
    <div v-else>
      <v-row>
        <CourseLearningCard
          v-for="course in filteredCourses"
          :key="course.id"
          :course="course as any"
          @open-course="openCourse"
          @close-course="closeCourse"
        />
      </v-row>
    </div>
  </div>
</template>

<style scoped>
/* 课程描述文字样式 - 最多五行 */
.course-description-text {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  max-height: calc(1.4em * 5);
  word-break: break-word;
  overflow-wrap: break-word;
}

/* 课程难度标签样式 - 设置最小宽度 */
.course-difficulty-chip {
  min-width: 50px !important;
  justify-content: center !important;
  text-align: center !important;
}

/* 课程关闭按钮样式 */
.course-close-btn {
  min-width: auto !important;
  width: 24px !important;
  height: 24px !important;
  border-radius: 50% !important;
  padding: 0 !important;
  transition: all 0.2s ease !important;
}

.course-close-btn:hover {
  background: rgba(0, 0, 0, 0.08) !important;
  transform: scale(1.1) !important;
}

/* 课程项目样式 */
.course-item {
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.course-item:hover {
  transform: translateX(2px);
  border-color: rgba(76, 175, 80, 0.3);
}

/* 课程卡片样式 */
.course-card {
  cursor: pointer;
  transition: all 0.2s ease;
}

.course-card:hover {
  transform: translateY(-2px);
}

/* 滚动列表样式 */
.scrollable-course-list {
  max-height: 140px; /* 约2.5个项目的高度：每项50px * 2.5 + 间距 */
  overflow-y: auto;
  scrollbar-width: none; /* Firefox 隐藏滚动条 */
  -ms-overflow-style: none; /* IE 隐藏滚动条 */
}

/* Webkit 浏览器隐藏滚动条 */
.scrollable-course-list::-webkit-scrollbar {
  display: none;
}

/* 紧凑间距样式 */
.pa-0-5 {
  padding: 2px !important;
}

/* 文本截断样式 */
.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>