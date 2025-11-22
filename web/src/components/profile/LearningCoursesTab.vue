<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-4">
          <div class="mb-4">
            <h4 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">学习的课程</h4>
            <p class="text-body-2 text-grey mb-0">
              跟踪您当前学习的所有课程进度和完成情况。
            </p>
          </div>
          <v-divider class="my-4" />
          <div class="text-body-2 text-grey">
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-chart-line" size="18" color="grey" class="mr-2 mt-1" />
              <span>实时同步学习进度</span>
            </div>
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-clock-outline" size="18" color="grey" class="mr-2 mt-1" />
              <span>记录最近学习时间</span>
            </div>
            <div class="d-flex align-start">
              <v-icon icon="mdi-target" size="18" color="grey" class="mr-2 mt-1" />
              <span>设定学习目标</span>
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-6">
          <!-- Tab 切换 -->
          <v-btn-toggle
            v-model="statusTab"
            mandatory
            color="grey-darken-3"
            variant="text"
            rounded="lg"
            density="compact"
          >
            <v-btn value="learning" rounded="lg">
              <v-icon icon="mdi-school" size="18" class="mr-2" />
              正在学习
            </v-btn>
            <v-btn value="completed" rounded="lg">
              <v-icon icon="mdi-check-circle" size="18" class="mr-2" />
              已经完成
            </v-btn>
          </v-btn-toggle>

          <v-btn color="primary" variant="text" rounded="lg" to="/learning">
            查看全部课程
            <v-icon icon="mdi-chevron-right" size="18" class="ml-1" />
          </v-btn>
        </div>

        <!-- 课程列表 -->
        <div v-if="filteredCourses.length > 0">
          <v-row>
            <v-col v-for="course in filteredCourses" :key="course.id" cols="12" md="6">
              <v-card rounded="xl" hover border elevation="0" class="course-card hoverable">
                <v-card-text class="pa-6" @click="goToCourse(course.courseId)">
                  <div class="d-flex align-start justify-space-between mb-4">
                    <div class="d-flex align-center flex-grow-1">
                      <v-avatar :color="course.iconColor" size="56" rounded="lg" class="mr-4">
                        <v-icon :icon="course.icon" color="white" size="28" />
                      </v-avatar>
                      <div>
                        <h4 class="text-h6 font-weight-bold mb-1">{{ course.title }}</h4>
                        <p class="text-caption text-grey mb-0">{{ course.lastActivity }}</p>
                      </div>
                    </div>
                    <v-btn
                      color="grey"
                      variant="text"
                      size="small"
                      icon="mdi-close"
                      @click.stop="cancelLearning(course.id)"
                    />
                  </div>

                  <v-progress-linear
                    :model-value="course.progress"
                    color="primary"
                    bg-color="grey-lighten-3"
                    height="8"
                    rounded
                    class="mb-3"
                  />

                  <div class="d-flex align-center justify-space-between">
                    <span class="text-body-2 text-grey-darken-2">
                      <v-icon icon="mdi-check-circle" size="16" color="success" class="mr-1" />
                      {{ course.completedLessons }} / {{ course.totalLessons }} 节已完成
                    </span>
                    <span class="text-body-1 font-weight-bold text-primary">{{ course.progress }}%</span>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-school" size="64" color="grey-lighten-2" class="mb-4" />
          <p class="text-body-1 text-grey-darken-2">
            {{ statusTab === 'learning' ? '暂无正在学习的课程' : '暂无已完成的课程' }}
          </p>
          <p class="text-body-2 text-grey">开始学习新课程，掌握新技能</p>
        </div>

        <!-- 删除确认对话框 -->
        <ConfirmDialog
          v-model="showDeleteDialog"
          title="确认取消学习"
          message="确定要取消学习该课程吗？此操作无法撤销。"
          confirm-text="确认取消"
          @confirm="confirmDelete"
        />
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { progressApi } from '@/api'
import { UserProgressState } from '@/enums'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()

// Tab 状态
const statusTab = ref('learning')

// 删除确认对话框
const showDeleteDialog = ref(false)
const courseToDelete = ref<number | null>(null)

// 获取用户课程数据
const {
  data: userCourses,
  loading,
  execute: fetchCourses,
} = useFetch({
  fetchFn: progressApi.getAllCourseProgress,
  immediate: true,
  defaultValue: [],
})

// 删除课程进度
const { execute: deleteProgress } = useMutation(
  (courseId: number) => progressApi.deleteCourseProgress(courseId),
  {
    successMessage: '已取消学习该课程',
    onSuccess: () => {
      fetchCourses()
    },
  }
)

// 转换课程数据为组件所需格式
const courses = computed(() => {
  if (!userCourses.value) return []

  return userCourses.value.map((userCourse) => {
    const course = userCourse.course
    const progress = userCourse.progressPercent || 0
    const state = userCourse.state || UserProgressState.NOT_STARTED
    const isCompleted = state === UserProgressState.COMPLETED

    // 计算最后活动时间
    const lastActivity = userCourse.updatedAt
      ? formatLastActivity(new Date(userCourse.updatedAt))
      : '暂无活动'

    // 由于后端返回的课程数据中没有节点总数，这里使用固定值作为估算
    // 实际使用时应该根据后端实际返回的数据结构调整
    const totalLessons = 50
    const completedLessons = Math.round((totalLessons * progress) / 100)

    return {
      id: userCourse.id,
      courseId: course?.id || 0,
      title: course?.name || '未知课程',
      progress,
      totalLessons,
      completedLessons,
      lastActivity,
      icon: 'mdi-school',
      iconColor: '#42b883',
      status: isCompleted ? 'completed' : 'learning',
    }
  })
})

// 根据状态过滤课程列表
const filteredCourses = computed(() => {
  return courses.value.filter((course) => course.status === statusTab.value)
})

// 格式化最后活动时间
const formatLastActivity = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  const weeks = Math.floor(days / 7)

  if (weeks > 0) return `${weeks}周前学习`
  if (days > 0) return `${days}天前学习`
  if (hours > 0) return `${hours}小时前学习`
  if (minutes > 0) return `${minutes}分钟前学习`
  return '刚刚学习'
}

// 跳转到课程详情
const goToCourse = (courseId: number) => {
  router.push(`/course/${courseId}`)
}

// 取消学习课程
const cancelLearning = (courseId: number) => {
  courseToDelete.value = courseId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (courseToDelete.value !== null) {
    await deleteProgress(courseToDelete.value)
  }
  courseToDelete.value = null
}
</script>

<style scoped>
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 140px;
  align-self: flex-start;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.course-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: #ffffff;
  border: 1px solid #e9ecef !important;
}

/* 移动端取消 sticky */
@media (max-width: 960px) {
  .sticky-sidebar {
    position: relative;
    top: 0;
    max-height: none;
    margin-bottom: 16px;
  }
}
</style>
