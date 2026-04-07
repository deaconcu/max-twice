<template>
  <div class="pa-0 pa-sm-1">
    <!-- 顶部操作栏 -->
    <div
      class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between mb-4 mb-md-6 ga-3"
    >
      <!-- Tab 切换 -->
      <div class="d-flex align-center">
        <v-btn
          variant="text"
          size="small"
          rounded="lg"
          :color="statusTab === 'learning' ? 'primary' : 'default'"
          @click="statusTab = 'learning'"
        >
          {{ t('user.profile.learningTab') }}
        </v-btn>
        <v-btn
          variant="text"
          size="small"
          rounded="lg"
          :color="statusTab === 'completed' ? 'primary' : 'default'"
          @click="statusTab = 'completed'"
        >
          {{ t('user.profile.completedTab') }}
        </v-btn>
      </div>
    </div>

    <!-- 加载状态 -->
    <LoadingSpinner v-if="loading" />

    <!-- 课程列表 -->
    <div v-else-if="courses.length > 0">
      <div class="course-grid">
        <v-card
          v-for="course in courses"
          :key="course.id"
          rounded="lg"
          border
          hover
          class="course-card"
          @click="goToCourse(course.courseId)"
        >
          <v-card-text class="pa-4 position-relative">
            <v-btn
              color="grey"
              variant="text"
              size="x-small"
              icon="mdi-close"
              class="close-btn"
              @click.stop="cancelLearning(course.id)"
            />
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
                  {{ course.title }}
                </div>
                <div class="text-caption text-medium-emphasis">
                  {{ course.totalLessons }} {{ t('rightSidebar.knowledgeNodes') }}
                </div>
              </div>
            </div>
            <div class="d-flex align-center justify-space-between mb-2">
              <span class="text-caption text-medium-emphasis">{{ t('user.profile.progress') }}</span>
              <span class="text-caption font-weight-bold text-grey">
                {{ course.progress }}%
              </span>
            </div>
            <v-progress-linear
              :model-value="course.progress"
              color="grey-lighten-3"
              height="6"
              rounded
            />
          </v-card-text>
        </v-card>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-8 py-md-12">
      <v-icon
        icon="mdi-school"
        :size="$vuetify.display.mobile ? 48 : 64"
        color="grey-lighten-2"
        class="mb-3 mb-md-4"
      />
      <p class="text-body-2 text-md-body-1 text-grey-darken-2">
        {{ statusTab === 'learning' ? t('user.profile.noLearningCourses') : t('user.profile.noCompletedCourses') }}
      </p>
      <p class="text-caption text-md-body-2 text-grey">{{ t('learning.browseCoursesDesc') }}</p>
    </div>

    <!-- 删除确认对话框 -->
    <ConfirmDialog
      v-model="showDeleteDialog"
      :title="t('common.confirm')"
      :message="t('learning.exitCourseConfirm')"
      :confirm-text="t('common.confirm')"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { useI18n } from '@/composables/useI18n'
import { progressApi } from '@/api'
import { getColorByString } from '@/utils/color'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import DynamicIcon from '@/components/common/DynamicIcon.vue'

const { t } = useI18n()
const router = useRouter()

// Tab 状态
const statusTab = ref<'learning' | 'completed'>('learning')

// 删除确认对话框
const showDeleteDialog = ref(false)
const courseToDelete = ref<number | null>(null)

// 获取用户课程数据
const {
  data: userCourses,
  loading,
  execute: fetchCourses,
} = useFetch({
  fetchFn: () => progressApi.getAllCourseProgress(statusTab.value),
  immediate: true,
  defaultValue: [],
})

// 监听 Tab 切换，重新加载数据
watch(statusTab, () => {
  fetchCourses()
})

// 删除课程进度
const { execute: deleteProgress } = useMutation(
  (courseId: number) => progressApi.deleteCourseProgress(courseId),
  {
    successMessage: t('user.profile.courseUnlearned'),
    onSuccess: () => {
      fetchCourses()
    },
  }
)

// 转换课程数据为组件所需格式
const courses = computed(() => {
  if (!userCourses.value) return []

  return userCourses.value.map((userLearning: any) => {
    // 后端返回的是 UserLearningDTO，关联对象在 object 字段
    const course = userLearning.object
    // 后端返回的是万分位（0-10000），转换为百分比（0-100）
    const progress = userLearning.progressPercent ? userLearning.progressPercent / 100 : 0

    const title = course?.name || t('user.profile.unknownCourse')
    const totalLessons = course?.nodeCount || 0
    const completedLessons = Math.round((totalLessons * progress) / 100)

    return {
      id: userLearning.id,
      courseId: course?.id || 0,
      title,
      progress,
      totalLessons,
      completedLessons,
      icon: course?.icon || 'mdi-book-open-variant',
      iconColor: getColorByString(title),
    }
  })
})

// 跳转到课程详情
const goToCourse = (courseId: number) => {
  router.push(`/read?courseId=${courseId}`)
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
.course-card {
  cursor: pointer;
  transition: all 0.2s;
  background-color: rgb(var(--v-theme-surface));
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.close-btn {
  position: absolute;
  top: 8px;
  right: 8px;
}

.icon-container {
  width: 48px;
  height: 48px;
  border: 1px solid rgb(var(--v-theme-outline));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 基于容器宽度的响应式网格 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@container (max-width: 1200px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@container (max-width: 750px) {
  .course-grid {
    grid-template-columns: 1fr;
  }
}

/* 启用 container query */
.pa-0 {
  container-type: inline-size;
}
</style>
