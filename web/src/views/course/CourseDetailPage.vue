<template>
  <DefaultLayout>
    <div class="course-detail-page">
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
          <!-- 课程头部区域 -->
          <div class="course-header-wrapper">
            <!-- 返回按钮 -->
            <div class="back-button-container">
              <v-btn
                icon="mdi-arrow-left"
                variant="flat"
                color="grey-lighten-5"
                :size="$vuetify.display.mobile ? 'small' : 'default'"
                class="flex-shrink-0"
                @click="handleBack"
              ></v-btn>
            </div>

            <!-- 课程头部 - 卡片样式 -->
            <v-card rounded="xl" class="course-header-card no-border" elevation="0">
              <v-card-text class="pa-5 pa-sm-6 pa-md-8 pb-2">
                <!-- 课程标题区 -->
                <div class="d-flex align-start ma-0">
                  <v-avatar color="grey-lighten-4" :size="$vuetify.display.mobile ? 48 : 60" rounded="lg" class="mr-3 mr-md-5 flex-shrink-0">
                    <v-icon icon="mdi-book-open-variant" color="grey" :size="$vuetify.display.mobile ? 24 : 32" />
                  </v-avatar>
                  <div class="flex-grow-1" style="min-width: 0;">
                    <h1 class="text-h5 text-md-h4 font-weight-bold text-grey-darken-4 mb-2 mb-md-3">
                      {{ course.name }}
                    </h1>
                    <p v-if="course.description" class="text-body-2 text-md-body-1 font-weight-regular text-grey-darken-2 mb-3 mb-md-4">
                      {{ course.description }}
                    </p>

                    <!-- 统计信息 -->
                    <div class="d-flex align-center flex-wrap mb-4 mb-md-6 ga-6 ga-md-12">
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-book-multiple" :size="$vuetify.display.mobile ? 20 : 24" color="primary" class="mr-2 mr-md-4" />
                        <span class="text-body-2 text-md-body-1 text-grey-darken-2">
                          <span class="font-weight-bold text-grey-darken-4 mr-1">{{ subCourses?.length ?? 0 }}</span>
                          <span class="text-caption text-md-body-2">{{ t('course.subCourses') }}</span>
                        </span>
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-account-group" :size="$vuetify.display.mobile ? 20 : 24" color="success" class="mr-2 mr-md-4" />
                        <span class="text-body-2 text-md-body-1 text-grey-darken-2">
                          <span class="font-weight-bold text-grey-darken-4 mr-1">{{ formatNumber(course.learnerCount) }}</span>
                          <span class="text-caption text-md-body-2">{{ t('course.learning') }}</span>
                        </span>
                      </div>
                    </div>

                    <!-- 操作按钮 -->
                    <div class="d-flex flex-wrap align-center ga-3">
                      <v-btn
                        color="primary"
                        variant="flat"
                        :size="$vuetify.display.mobile ? 'default' : 'large'"
                        rounded="lg"
                        class="text-none px-4 px-md-6"
                        @click="handleStartReading"
                      >
                        <v-icon size="20" class="mr-2">mdi-book-open-page-variant</v-icon>
                        {{ t('course.startReading') }}
                      </v-btn>
                      <v-btn
                        :color="course.subscribed ? 'error' : 'grey-darken-2'"
                        :variant="course.subscribed ? 'flat' : 'tonal'"
                        :size="$vuetify.display.mobile ? 'default' : 'large'"
                        rounded="lg"
                        class="text-none px-4 px-md-6"
                        :loading="subscribing"
                        @click="handleToggleSubscribe"
                      >
                        <v-icon size="20" class="mr-2">
                          {{ course.subscribed ? 'mdi-heart' : 'mdi-heart-outline' }}
                        </v-icon>
                        {{ course.subscribed ? t('course.subscribed') : t('course.subscribe') }}
                      </v-btn>
                    </div>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </div>

          <!-- 子课程列表 -->
          <div class="sub-courses-section">
            <v-card flat rounded="xl" class="section-header-card mb-4 mb-md-6 no-border">
              <v-card-text class="px-0 py-4 py-sm-5 d-flex align-center justify-space-between">
                <div class="d-flex align-center">
                  <v-avatar color="primary" size="36" class="mr-3">
                    <v-icon icon="mdi-book-multiple" color="white" size="20" />
                  </v-avatar>
                  <div>
                    <h2 class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-4 mb-0">
                      {{ t('course.subCoursesList') }}
                    </h2>
                    <p class="text-caption text-grey-darken-2 mb-0 d-none d-sm-block">
                      探索课程的各个学习模块
                    </p>
                  </div>
                </div>
                <v-btn
                  color="primary"
                  variant="flat"
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  rounded="lg"
                  class="text-none"
                  @click="handleApplySubCourse"
                >
                  <v-icon icon="mdi-plus" :size="$vuetify.display.mobile ? 16 : 18" class="mr-1 mr-sm-2" />
                  <span class="d-none d-sm-inline">{{ t('course.applySubCourse') }}</span>
                  <span class="d-sm-none">申请</span>
                </v-btn>
              </v-card-text>
            </v-card>

            <!-- 子课程网格 -->
            <div class="sub-course-grid">
              <!-- 真实子课程 -->
              <v-card
                v-for="(subCourse, index) in subCourses"
                :key="subCourse.id"
                rounded="xl"
                class="sub-course-card"
                elevation="0"
                border
                hover
                @click="handleGoToSubCourse(index)"
              >
                <v-card-text class="pa-4 pa-sm-5 pa-md-6">
                  <!-- 顶部：序号和操作 -->
                  <div class="d-flex align-center justify-space-between mb-3 mb-md-4">
                    <v-chip
                      color="primary"
                      variant="flat"
                      :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                      class="font-weight-bold"
                    >
                      {{ index + 1 }}
                    </v-chip>
                    <div class="d-flex align-center" style="gap: 4px">
                      <v-btn
                        icon="mdi-book-open-page-variant"
                        :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                        variant="text"
                        color="primary"
                        @click.stop="handleGoToSubCourse(index)"
                      ></v-btn>
                      <v-btn
                        :icon="subCourse.subscribed ? 'mdi-heart' : 'mdi-heart-outline'"
                        :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                        variant="text"
                        :color="subCourse.subscribed ? 'error' : 'grey'"
                        @click.stop="handleToggleSubCourseSubscribe(subCourse.id)"
                      ></v-btn>
                    </div>
                  </div>

                  <!-- 子课程信息 -->
                  <h3 class="text-subtitle-1 text-md-h6 font-weight-bold mb-2 text-grey-darken-4">
                    {{ subCourse.name }}
                  </h3>
                  <p
                    v-if="subCourse.description"
                    class="text-caption text-sm-body-2 text-grey-darken-2 mb-3 mb-md-4 sub-course-description"
                  >
                    {{ subCourse.description }}
                  </p>

                  <!-- 底部统计 -->
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-account-group" :size="$vuetify.display.mobile ? 14 : 16" color="grey-darken-1" class="mr-1" />
                    <span class="text-caption text-grey-darken-1">
                      {{ formatNumber(subCourse.learnerCount) }} {{ t('course.learning') }}
                    </span>
                  </div>
                </v-card-text>
              </v-card>

              <!-- 默认待创建卡片：快速入门 -->
              <v-card
                rounded="xl"
                class="sub-course-card placeholder-card"
                border
                hover
                @click="handleCreateDefaultSubCourse('quickstart')"
              >
                <v-card-text class="pa-4 pa-sm-5">
                  <div class="d-flex align-center justify-space-between mb-3">
                    <div class="sub-course-number placeholder-number">
                      <v-icon icon="mdi-plus" :size="$vuetify.display.mobile ? 16 : 18" />
                    </div>
                  </div>
                  <h3 class="text-subtitle-1 text-md-h6 font-weight-bold mb-2 sub-course-title">
                    {{ t('course.quickstart') }}
                  </h3>
                  <p class="text-caption text-sm-body-2 text-medium-emphasis mb-3 sub-course-description">
                    {{ t('course.quickstartDesc') }}
                  </p>
                  <div class="d-flex align-center">
                    <v-chip :size="$vuetify.display.mobile ? 'x-small' : 'small'" color="grey-lighten-2" variant="flat">
                      {{ t('course.toBeCreated') }}
                    </v-chip>
                  </div>
                </v-card-text>
              </v-card>

              <!-- 默认待创建卡片：习题练习 -->
              <v-card
                rounded="xl"
                class="sub-course-card placeholder-card"
                border
                hover
                @click="handleCreateDefaultSubCourse('exercises')"
              >
                <v-card-text class="pa-4 pa-sm-5">
                  <div class="d-flex align-center justify-space-between mb-3">
                    <div class="sub-course-number placeholder-number">
                      <v-icon icon="mdi-plus" :size="$vuetify.display.mobile ? 16 : 18" />
                    </div>
                  </div>
                  <h3 class="text-subtitle-1 text-md-h6 font-weight-bold mb-2 sub-course-title">
                    {{ t('course.exercises') }}
                  </h3>
                  <p class="text-caption text-sm-body-2 text-medium-emphasis mb-3 sub-course-description">
                    {{ t('course.exercisesDesc') }}
                  </p>
                  <div class="d-flex align-center">
                    <v-chip :size="$vuetify.display.mobile ? 'x-small' : 'small'" color="grey-lighten-2" variant="flat">
                      {{ t('course.toBeCreated') }}
                    </v-chip>
                  </div>
                </v-card-text>
              </v-card>
            </div>
          </div>
        </div>

        <!-- 右侧帮助信息 -->
        <div class="right-sidebar d-none d-lg-block">
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
    path: '/read',
    query: {
      courseId: String(courseId.value),
    },
  })
}

/**
 * 跳转到子课程阅读页
 */
const handleGoToSubCourse = (_index: number) => {
  void router.push({
    path: '/read',
    query: {
      courseId: String(courseId.value),
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

/* 课程头部包装器 */
.course-header-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 32px;
}

/* 返回按钮容器 */
.back-button-container {
  display: flex;
  align-items: center;
}

/* 课程头部卡片 */
.course-header-card {
  background: rgb(var(--v-theme-surface));
  border: 1px double rgb(var(--v-theme-outline)) !important;
  flex: 1;
}

/* 宽屏时：返回按钮和卡片同一行 */
@media (min-width: 1800px) {
  .course-header-wrapper {
    flex-direction: row;
    align-items: flex-start;
    gap: 16px;
  }

  .back-button-container {
    /* 让返回按钮向左延伸到页面外 */
    margin-left: -64px;
    padding-top: 0; /* 对齐卡片顶部 */
  }
}

/* 使用flex布局实现左右结构 */
.content-wrapper {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

@media (min-width: 1280px) {
  .content-wrapper {
    gap: 40px;
  }
}

.left-content {
  flex: 1;
  min-width: 0;
}

.right-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.sub-courses-section {
  margin-top: 0;
}

.section-header-card {
  background-color: rgb(var(--v-theme-surface));
}

.sub-course-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

@media (min-width: 600px) {
  .sub-course-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (min-width: 960px) {
  .sub-course-grid {
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 20px;
  }
}

.sub-course-card {
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-outline)) !important;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  height: 100%;
}

.sub-course-card:hover {
  border-color: rgb(var(--v-theme-primary)) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px);
}

.sub-course-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 40px;
}

@media (min-width: 960px) {
  .sub-course-description {
    min-height: 60px;
  }
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
