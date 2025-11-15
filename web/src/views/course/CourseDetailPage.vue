<template>
  <DefaultLayout>
    <div class="course-detail-page">
      <!-- 返回按钮 -->
      <v-btn variant="text" color="grey-darken-2" class="mb-4" @click="handleBack">
        <v-icon icon="mdi-arrow-left" class="mr-1" />
        {{ t('common.back') }}
      </v-btn>

      <!-- 加载状态 -->
      <div v-if="loading" class="text-center py-12">
        <v-progress-circular indeterminate color="primary" size="64" />
        <p class="text-body-1 text-medium-emphasis mt-4">{{ t('common.loading') }}</p>
      </div>

      <!-- 错误状态 -->
      <v-alert v-else-if="error" type="error" variant="tonal" class="mb-6">
        {{ error }}
      </v-alert>

      <!-- 内容区 -->
      <div v-else-if="course" class="content-wrapper">
        <!-- 左侧内容 -->
        <div class="left-content">
          <!-- 课程信息卡片 -->
          <v-card
            rounded="lg"
            class="course-info-card mb-6"
            border
            hover
            @click="handleStartReading"
          >
            <v-card-text class="pa-6">
              <!-- 课程头部信息 -->
              <div class="d-flex align-start mb-4">
                <v-avatar color="grey-lighten-3" size="80" rounded="lg" class="mr-4">
                  <v-icon icon="mdi-book-open-variant" color="grey-darken-1" size="40" />
                </v-avatar>
                <div class="flex-grow-1">
                  <!-- 标题和按钮在同一行 -->
                  <div class="d-flex align-center justify-space-between mb-2">
                    <h1 class="text-h4 font-weight-bold">
                      {{ course.name }}
                    </h1>
                    <!-- 操作按钮 -->
                    <div class="d-flex align-center flex-shrink-0 ml-4" style="gap: 8px">
                      <v-btn
                        color="primary"
                        variant="flat"
                        size="default"
                        rounded="lg"
                        class="text-none"
                        @click.stop="handleStartReading"
                      >
                        <v-icon size="18" class="mr-2">mdi-book-open-page-variant</v-icon>
                        {{ t('course.startReading') }}
                      </v-btn>
                      <v-btn
                        :color="course.subscribed ? 'error' : 'grey-darken-2'"
                        :variant="course.subscribed ? 'flat' : 'outlined'"
                        size="default"
                        rounded="lg"
                        class="text-none"
                        :loading="subscribing"
                        @click.stop="handleToggleSubscribe"
                      >
                        <v-icon size="18" class="mr-2">
                          {{ course.subscribed ? 'mdi-heart' : 'mdi-heart-outline' }}
                        </v-icon>
                        {{ course.subscribed ? t('course.subscribed') : t('course.subscribe') }}
                      </v-btn>
                    </div>
                  </div>
                  <p v-if="course.description" class="text-body-1 text-medium-emphasis mb-3">
                    {{ course.description }}
                  </p>
                  <!-- 统计信息 -->
                  <div class="d-flex align-center flex-wrap" style="gap: 24px">
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-book-multiple" size="20" color="primary" class="mr-2" />
                      <span class="text-body-2 text-medium-emphasis">
                        <span class="font-weight-bold">{{ subCourses?.length ?? 0 }}</span>
                        {{ t('course.subCourses') }}
                      </span>
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-account-group" size="20" color="primary" class="mr-2" />
                      <span class="text-body-2 text-medium-emphasis">
                        <span class="font-weight-bold">{{
                          formatNumber(course.learnerCount)
                        }}</span>
                        {{ t('course.learning') }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>

          <!-- 子课程列表 -->
          <div class="sub-courses-section">
            <div class="d-flex align-center justify-space-between mb-4">
              <div class="d-flex align-center">
                <v-icon icon="mdi-book-multiple" size="20" color="grey-darken-2" class="mr-2" />
                <h2 class="text-h6 font-weight-bold">{{ t('course.subCoursesList') }}</h2>
              </div>
              <v-btn
                color="grey-lighten-4"
                variant="flat"
                size="small"
                rounded="lg"
                class="text-none"
                @click="handleApplySubCourse"
              >
                <v-icon icon="mdi-plus" size="16" class="mr-2" />
                <span class="text-grey-darken-3">{{ t('course.applySubCourse') }}</span>
              </v-btn>
            </div>

            <!-- 子课程网格 -->
            <div class="sub-course-grid">
              <!-- 真实子课程 -->
              <v-card
                v-for="(subCourse, index) in subCourses"
                :key="subCourse.id"
                rounded="lg"
                class="sub-course-card"
                border
                hover
                @click="handleGoToSubCourse(index)"
              >
                <v-card-text class="pa-5">
                  <!-- 序号和按钮 -->
                  <div class="d-flex align-center justify-space-between mb-3">
                    <div class="sub-course-number">
                      {{ index + 1 }}
                    </div>
                    <div class="d-flex align-center" style="gap: 4px">
                      <v-btn
                        icon
                        size="x-small"
                        variant="text"
                        color="grey"
                        @click.stop="handleGoToSubCourse(index)"
                      >
                        <v-icon size="18">mdi-book-open-page-variant</v-icon>
                      </v-btn>
                      <v-btn
                        icon
                        size="x-small"
                        variant="text"
                        :color="subCourse.subscribed ? 'error' : 'grey'"
                        @click.stop="handleToggleSubCourseSubscribe(subCourse.id)"
                      >
                        <v-icon size="18">
                          {{ subCourse.subscribed ? 'mdi-heart' : 'mdi-heart-outline' }}
                        </v-icon>
                      </v-btn>
                    </div>
                  </div>

                  <!-- 子课程信息 -->
                  <h3 class="text-h6 font-weight-bold mb-2 sub-course-title">
                    {{ subCourse.name }}
                  </h3>
                  <p
                    v-if="subCourse.description"
                    class="text-body-2 text-medium-emphasis mb-3 sub-course-description"
                  >
                    {{ subCourse.description }}
                  </p>

                  <!-- 统计信息 -->
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-account-group" size="14" color="grey" class="mr-1" />
                    <span class="text-caption text-medium-emphasis">
                      {{ formatNumber(subCourse.learnerCount) }} {{ t('course.learning') }}
                    </span>
                  </div>
                </v-card-text>
              </v-card>

              <!-- 默认待创建卡片：快速入门 -->
              <v-card
                rounded="lg"
                class="sub-course-card placeholder-card"
                border
                hover
                @click="handleCreateDefaultSubCourse('quickstart')"
              >
                <v-card-text class="pa-5">
                  <div class="d-flex align-center justify-space-between mb-3">
                    <div class="sub-course-number placeholder-number">
                      <v-icon icon="mdi-plus" size="18" />
                    </div>
                  </div>
                  <h3 class="text-h6 font-weight-bold mb-2 sub-course-title">
                    {{ t('course.quickstart') }}
                  </h3>
                  <p class="text-body-2 text-medium-emphasis mb-3 sub-course-description">
                    {{ t('course.quickstartDesc') }}
                  </p>
                  <div class="d-flex align-center">
                    <v-chip size="small" color="grey-lighten-2" variant="flat">
                      {{ t('course.toBeCreated') }}
                    </v-chip>
                  </div>
                </v-card-text>
              </v-card>

              <!-- 默认待创建卡片：习题练习 -->
              <v-card
                rounded="lg"
                class="sub-course-card placeholder-card"
                border
                hover
                @click="handleCreateDefaultSubCourse('exercises')"
              >
                <v-card-text class="pa-5">
                  <div class="d-flex align-center justify-space-between mb-3">
                    <div class="sub-course-number placeholder-number">
                      <v-icon icon="mdi-plus" size="18" />
                    </div>
                  </div>
                  <h3 class="text-h6 font-weight-bold mb-2 sub-course-title">
                    {{ t('course.exercises') }}
                  </h3>
                  <p class="text-body-2 text-medium-emphasis mb-3 sub-course-description">
                    {{ t('course.exercisesDesc') }}
                  </p>
                  <div class="d-flex align-center">
                    <v-chip size="small" color="grey-lighten-2" variant="flat">
                      {{ t('course.toBeCreated') }}
                    </v-chip>
                  </div>
                </v-card-text>
              </v-card>
            </div>
          </div>
        </div>

        <!-- 右侧帮助信息 -->
        <div class="right-sidebar">
          <v-card rounded="lg" class="help-card sticky-card" flat border>
            <v-card-title class="pa-4 pb-3">
              <div class="d-flex align-center">
                <v-icon icon="mdi-help-circle" color="primary" class="mr-2" />
                <span class="text-h6 font-weight-bold">{{ t('course.helpInfo') }}</span>
              </div>
            </v-card-title>
            <v-card-text class="pa-4 pt-0">
              <div class="help-section mb-4">
                <h4 class="text-subtitle-2 font-weight-bold mb-2">
                  {{ t('course.howToLearn') }}
                </h4>
                <p class="text-body-2 text-medium-emphasis mb-0">
                  {{ t('course.howToLearnDesc') }}
                </p>
              </div>

              <div class="help-section mb-4">
                <h4 class="text-subtitle-2 font-weight-bold mb-2">
                  {{ t('course.learningProgress') }}
                </h4>
                <p class="text-body-2 text-medium-emphasis mb-0">
                  {{ t('course.learningProgressDesc') }}
                </p>
              </div>

              <div class="help-section mb-4">
                <h4 class="text-subtitle-2 font-weight-bold mb-2">
                  {{ t('course.subscribeCourse') }}
                </h4>
                <p class="text-body-2 text-medium-emphasis mb-0">
                  {{ t('course.subscribeCourseDesc') }}
                </p>
              </div>

              <div class="help-section">
                <h4 class="text-subtitle-2 font-weight-bold mb-2">
                  {{ t('course.memoryCards') }}
                </h4>
                <p class="text-body-2 text-medium-emphasis mb-0">
                  {{ t('course.memoryCardsDesc') }}
                </p>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </div>
    </div>

    <!-- 申请子课程对话框 -->
    <SubCourseApplicationDialog
      v-model="applicationDialog"
      :parent-course-id="courseId"
      :submitting="creatingSubCourse"
      @submit="handleSubmitSubCourse"
    />

    <!-- 确认创建默认子课程对话框 -->
    <v-dialog v-model="confirmDialog" max-width="480px">
      <v-card rounded="xl">
        <v-card-title class="pa-6 pb-4">
          <div class="d-flex align-center">
            <v-icon icon="mdi-information" color="primary" size="24" class="mr-3" />
            <span class="text-h6 font-weight-bold">{{ t('course.createSubCourse') }}</span>
          </div>
        </v-card-title>

        <v-card-text class="px-6 pb-2">
          <p class="text-body-1 mb-2">
            {{
              t('course.confirmCreateSubCourse', {
                name:
                  pendingSubCourseType === 'quickstart'
                    ? t('course.quickstart')
                    : t('course.exercises'),
              })
            }}
          </p>
          <p class="text-body-2 text-medium-emphasis">
            {{
              pendingSubCourseType === 'quickstart'
                ? t('course.quickstartDesc')
                : t('course.exercisesDesc')
            }}
          </p>
        </v-card-text>

        <v-card-actions class="px-6 pb-6 pt-4">
          <v-spacer />
          <v-btn variant="outlined" size="default" rounded="lg" @click="confirmDialog = false">
            {{ t('common.cancel') }}
          </v-btn>
          <v-btn
            color="primary"
            variant="flat"
            size="default"
            rounded="lg"
            :loading="creatingSubCourse"
            @click="handleConfirmCreateDefaultSubCourse"
          >
            {{ t('common.confirm') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useFetch, useMutation } from '@/composables'
import { courseApi, subscriptionApi } from '@/api'
import type { Course } from '@/types/course'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import SubCourseApplicationDialog from '@/components/features/course/SubCourseApplicationDialog.vue'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

// 课程 ID
const courseId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : 0
})

// 使用 useFetch 加载课程详情
const {
  data: course,
  loading,
  error: fetchError,
} = useFetch<Course>({
  fetchFn: () => courseApi.getCourse(courseId.value),
  immediate: true,
  defaultValue: null,
})

// 使用 useFetch 加载子课程列表
const { data: subCourses, loading: _loadingSubCourses } = useFetch<Course[]>({
  fetchFn: () => courseApi.getSubCourses(courseId.value),
  immediate: true,
  defaultValue: [],
})

// 使用 useMutation 处理订阅/取消订阅
const { execute: executeSubscribe, loading: subscribing } = useMutation(
  (payload: { id: number; action: 'subscribe' | 'unsubscribe' }) =>
    payload.action === 'subscribe'
      ? subscriptionApi.subscribe(payload.id)
      : subscriptionApi.unsubscribe(payload.id),
  {
    successMessage: '',
    showToast: false,
  }
)

// 使用 useMutation 处理创建子课程
const {
  execute: executeCreateSubCourse,
  loading: creatingSubCourse,
  refresh: refreshSubCourses,
} = useMutation(
  (payload: { parentId: number; name: string; description: string }) =>
    courseApi.createSubcourse(payload.parentId, payload.name, payload.description),
  {
    successMessage: '申请提交成功，等待审核',
    onSuccess: () => {
      applicationDialog.value = false
      // 刷新子课程列表
      void refreshSubCourses()
    },
  }
)

// 申请对话框状态
const applicationDialog = ref(false)

// 确认创建对话框状态
const confirmDialog = ref(false)
const pendingSubCourseType = ref<'quickstart' | 'exercises'>('quickstart')

// 默认子课程的名称和描述
const defaultSubCourses = {
  quickstart: {
    name: '快速入门',
    nameEn: 'Quick Start',
    description: '快速了解本课程的核心内容，帮助您快速上手',
    descriptionEn: 'Quickly understand the core content of this course to help you get started',
  },
  exercises: {
    name: '习题练习',
    nameEn: 'Exercises',
    description: '通过习题巩固所学知识，提升实践能力',
    descriptionEn: 'Consolidate knowledge through exercises and improve practical skills',
  },
}

const error = computed(() => (fetchError.value ? t('course.loadError') : null))

/**
 * 格式化数字（千位分隔）
 */
const formatNumber = (num?: number) => {
  if (!num) return '0'
  return num.toLocaleString()
}

/**
 * 返回上一页
 */
const handleBack = () => {
  router.back()
}

/**
 * 开始阅读
 */
const handleStartReading = () => {
  void router.push({
    name: 'content-read',
    params: {
      id: String(courseId.value),
    },
  })
}

/**
 * 跳转到子课程阅读页
 */
const handleGoToSubCourse = (_index: number) => {
  void router.push({
    name: 'content-read',
    params: {
      id: String(courseId.value),
    },
  })
}

/**
 * 切换订阅状态
 */
const handleToggleSubscribe = async () => {
  if (!course.value) return

  const action = course.value.subscribed ? 'unsubscribe' : 'subscribe'
  const result = await executeSubscribe({ id: course.value.id, action })

  if (result && course.value) {
    course.value.subscribed = !course.value.subscribed
    if (course.value.subscribed) {
      course.value.subscriptionCount = (course.value.subscriptionCount ?? 0) + 1
    } else {
      course.value.subscriptionCount = Math.max((course.value.subscriptionCount ?? 0) - 1, 0)
    }
  }
}

/**
 * 切换子课程订阅状态
 */
const handleToggleSubCourseSubscribe = async (subCourseId: number) => {
  if (!subCourses.value) return
  const subCourse = subCourses.value.find((c) => c.id === subCourseId)
  if (!subCourse) return

  const action = subCourse.subscribed ? 'unsubscribe' : 'subscribe'
  const result = await executeSubscribe({ id: subCourseId, action })

  if (result && subCourse) {
    subCourse.subscribed = !subCourse.subscribed
    if (subCourse.subscribed) {
      subCourse.subscriptionCount = (subCourse.subscriptionCount ?? 0) + 1
    } else {
      subCourse.subscriptionCount = Math.max((subCourse.subscriptionCount ?? 0) - 1, 0)
    }
  }
}

/**
 * 申请子课程
 */
const handleApplySubCourse = () => {
  applicationDialog.value = true
}

/**
 * 提交子课程申请
 */
const handleSubmitSubCourse = async (data: { name: string; description: string }) => {
  await executeCreateSubCourse({
    parentId: courseId.value,
    name: data.name,
    description: data.description,
  })
}

/**
 * 点击默认子课程卡片
 */
const handleCreateDefaultSubCourse = (type: 'quickstart' | 'exercises') => {
  pendingSubCourseType.value = type
  confirmDialog.value = true
}

/**
 * 确认创建默认子课程
 */
const handleConfirmCreateDefaultSubCourse = async () => {
  const type = pendingSubCourseType.value
  const subCourseData = defaultSubCourses[type]

  await executeCreateSubCourse({
    parentId: courseId.value,
    name: subCourseData.name,
    description: subCourseData.description,
  })

  confirmDialog.value = false
}
</script>

<style scoped>
.course-detail-page {
  padding-top: 24px;
  padding-bottom: 48px;
}

/* 使用flex布局实现左右结构 */
.content-wrapper {
  display: flex;
  gap: 40px;
  align-items: flex-start;
}

.left-content {
  flex: 1;
  min-width: 0;
}

.right-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.course-info-card {
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-border));
  cursor: pointer;
  transition: all 0.3s ease;
}

.course-info-card:hover {
  border-color: rgb(var(--v-theme-primary));
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.sub-courses-section {
  margin-top: 0;
}

.sub-course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.sub-course-card {
  border: 1px solid rgb(var(--v-theme-border));
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
  background-color: rgb(var(--v-theme-surface));
}

.sub-course-card:hover {
  border-color: rgb(var(--v-theme-primary));
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.sub-course-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background-color: rgb(var(--v-theme-primary));
  color: rgb(var(--v-theme-on-primary));
  border-radius: 8px;
  font-weight: bold;
  font-size: 0.875rem;
}

.sub-course-title {
  color: rgb(var(--v-theme-on-surface));
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sub-course-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 60px;
}

.sticky-card {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow-y: auto;
}

.help-card {
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-border));
}

.help-section {
  padding-bottom: 12px;
  border-bottom: 1px solid rgb(var(--v-theme-surface-variant));
}

.help-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

/* 占位卡片样式 */
.placeholder-card {
  opacity: 0.6;
}

.placeholder-card:hover {
  opacity: 0.8;
}

.placeholder-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background-color: rgb(var(--v-theme-grey-lighten-3));
  color: rgb(var(--v-theme-grey));
  border-radius: 8px;
  font-weight: bold;
  font-size: 0.875rem;
}

/* 移动端响应式 */
@media (max-width: 1264px) {
  .content-wrapper {
    flex-direction: column;
  }

  .right-sidebar {
    width: 100%;
  }

  .sticky-card {
    position: static;
    max-height: none;
  }
}

@media (max-width: 960px) {
  .course-detail-page {
    /* 使用 DefaultLayout 的默认 padding */
  }

  .sub-course-grid {
    grid-template-columns: 1fr;
  }
}
</style>
