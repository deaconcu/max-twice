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
      <div v-else-if="course">
        <!-- 返回按钮和标题行 -->
        <div class="page-header-row d-flex align-center mb-4 mb-md-5">
          <v-btn
            icon="mdi-arrow-left"
            variant="flat"
            color="grey-lighten-5"
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            class="flex-shrink-0 mr-3"
            @click="handleBack"
          ></v-btn>
          <div class="d-flex align-center">
            <v-icon icon="mdi-text-box-outline" size="20" color="grey" class="mr-2" />
            <h2 class="text-subtitle-1 text-md-h6 font-weight-bold text-grey-darken-4 mb-0">
              课程详情
            </h2>
          </div>
        </div>

        <!-- 左右两栏内容 -->
        <div class="content-wrapper">
          <!-- 左侧内容 -->
          <div class="left-content">
            <!-- 课程头部 - 卡片样式 -->
            <v-card
              rounded="xl"
              class="course-header-card no-border hoverable mb-6 mb-md-8"
              elevation="0"
              hover
              @click="handleStartReading"
            >
              <v-card-text class="pa-5 pa-sm-6 pa-md-8 pb-2">
                <!-- 课程标题区 -->
                <div class="ma-0">
                  <!-- 图标和标题行 -->
                  <div class="d-flex align-center mb-3 mb-md-4">
                    <v-avatar
                      color="surface-variant"
                      :size="$vuetify.display.mobile ? 48 : 60"
                      rounded="lg"
                      class="mr-3 mr-md-4 flex-shrink-0"
                    >
                      <v-icon
                        icon="mdi-school"
                        color="grey"
                        :size="$vuetify.display.mobile ? 24 : 32"
                      />
                    </v-avatar>
                    <div class="d-flex align-center flex-wrap ga-4">
                      <h1 class="text-h5 text-md-h4 font-weight-bold text-grey-darken-4 mb-0">
                        {{ course.name }}
                      </h1>
                      <v-chip color="primary" variant="flat" size="small" rounded="lg">
                        主课程
                      </v-chip>
                    </div>
                  </div>

                  <!-- 描述 -->
                  <p
                    v-if="course.description"
                    class="text-body-2 text-md-body-1 font-weight-regular text-grey-darken-2 mb-3 mb-md-4"
                  >
                    {{ course.description }}
                  </p>

                  <!-- 统计信息 -->
                  <div class="d-flex align-center flex-wrap mb-4 mb-md-6 ga-6 ga-md-12">
                    <div class="d-flex align-center">
                      <v-icon
                        icon="mdi-book-multiple"
                        :size="$vuetify.display.mobile ? 20 : 24"
                        color="primary"
                        class="mr-2 mr-md-4"
                      />
                      <span class="text-body-2 text-md-body-1 text-grey-darken-2">
                        <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                          subCourses?.length ?? 0
                        }}</span>
                        <span class="text-caption text-md-body-2">{{
                          t('course.subCourses')
                        }}</span>
                      </span>
                    </div>
                    <div class="d-flex align-center">
                      <v-icon
                        icon="mdi-account-group"
                        :size="$vuetify.display.mobile ? 20 : 24"
                        color="success"
                        class="mr-2 mr-md-4"
                      />
                      <span class="text-body-2 text-md-body-1 text-grey-darken-2">
                        <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                          formatNumber(course.learnerCount)
                        }}</span>
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
                      @click.stop="handleStartReading"
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
                      @click.stop="handleToggleSubscribe"
                    >
                      <v-icon size="20" class="mr-2">
                        {{ course.subscribed ? 'mdi-heart' : 'mdi-heart-outline' }}
                      </v-icon>
                      {{ course.subscribed ? t('course.subscribed') : t('course.subscribe') }}
                    </v-btn>
                  </div>
                </div>
              </v-card-text>
            </v-card>

            <!-- 子课程列表 -->
            <div class="sub-courses-section">
              <!-- 标题区 -->
              <div class="d-flex align-center justify-space-between mb-5 mb-md-6">
                <div class="d-flex align-center">
                  <div class="section-icon-wrapper mr-3 mr-md-4">
                    <v-icon
                      icon="mdi-book-multiple"
                      color="grey"
                      :size="$vuetify.display.mobile ? 22 : 24"
                    />
                  </div>
                  <div>
                    <h2 class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-4 mb-0">
                      {{ t('course.subCoursesList') }}
                    </h2>
                    <p class="text-caption text-grey-darken-1 mb-0 mt-1 d-none d-sm-block">
                      探索课程的各个学习模块
                    </p>
                  </div>
                </div>
                <v-btn
                  color="primary"
                  variant="text"
                  :size="$vuetify.display.mobile ? 'default' : 'large'"
                  rounded="lg"
                  density="comfortable"
                  class="text-none px-4 px-md-6"
                  elevation="0"
                  @click="handleApplySubCourse"
                >
                  <v-icon icon="mdi-plus" size="20" class="mr-2" />
                  <span class="d-none d-sm-inline">{{ t('course.applySubCourse') }}</span>
                  <span class="d-sm-none">申请</span>
                </v-btn>
              </div>

              <!-- 子课程网格 -->
              <div class="sub-course-grid">
                <!-- 示例：已创建的子课程 -->
                <v-card
                  rounded="xl"
                  class="sub-course-card hoverable"
                  elevation="0"
                  hover
                  @click="handleGoToSubCourse(0)"
                >
                  <v-card-text class="pa-5 pa-sm-6">
                    <!-- 顶部：序号和操作 -->
                    <div class="d-flex align-center justify-space-between mb-4">
                      <div class="course-number">1</div>
                      <div class="d-flex align-center ga-1">
                        <v-btn
                          icon="mdi-book-open-page-variant"
                          size="small"
                          variant="text"
                          color="primary"
                          @click.stop="handleGoToSubCourse(0)"
                        ></v-btn>
                        <v-btn
                          icon="mdi-heart"
                          size="small"
                          variant="text"
                          color="error"
                          @click.stop
                        ></v-btn>
                      </div>
                    </div>

                    <!-- 子课程信息 -->
                    <h3
                      class="text-h6 text-md-h5 font-weight-bold mb-3 text-grey-darken-4 course-title"
                    >
                      Vue 3 核心概念
                    </h3>
                    <p class="text-body-2 text-grey-darken-2 mb-4 course-description">
                      深入学习 Vue 3 的响应式系统、组合式
                      API、生命周期钩子等核心概念，掌握现代前端框架的核心原理和最佳实践。
                    </p>

                    <!-- 底部统计 -->
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-account-group" size="18" color="grey" class="mr-2" />
                      <span class="text-body-2 text-grey-darken-1">
                        1,234
                        <span class="text-grey">人学习</span>
                      </span>
                    </div>
                  </v-card-text>
                </v-card>

                <!-- 真实子课程 -->
                <v-card
                  v-for="(subCourse, index) in subCourses"
                  :key="subCourse.id"
                  rounded="xl"
                  class="sub-course-card hoverable"
                  elevation="0"
                  hover
                  @click="handleGoToSubCourse(index)"
                >
                  <v-card-text class="pa-5 pa-sm-6">
                    <!-- 顶部：序号和操作 -->
                    <div class="d-flex align-center justify-space-between mb-4">
                      <div class="course-number">
                        {{ index + 2 }}
                      </div>
                      <div class="d-flex align-center ga-1">
                        <v-btn
                          icon="mdi-book-open-page-variant"
                          size="small"
                          variant="text"
                          color="primary"
                          @click.stop="handleGoToSubCourse(index)"
                        ></v-btn>
                        <v-btn
                          :icon="subCourse.subscribed ? 'mdi-heart' : 'mdi-heart-outline'"
                          size="small"
                          variant="text"
                          :color="subCourse.subscribed ? 'error' : 'grey'"
                          @click.stop="handleToggleSubCourseSubscribe(subCourse.id)"
                        ></v-btn>
                      </div>
                    </div>

                    <!-- 子课程信息 -->
                    <h3
                      class="text-h6 text-md-h5 font-weight-bold mb-3 text-grey-darken-4 course-title"
                    >
                      {{ subCourse.name }}
                    </h3>
                    <p
                      v-if="subCourse.description"
                      class="text-body-2 text-grey-darken-2 mb-4 course-description"
                    >
                      {{ subCourse.description }}
                    </p>

                    <!-- 底部统计 -->
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-account-group" size="18" color="grey" class="mr-2" />
                      <span class="text-body-2 text-grey-darken-1">
                        {{ formatNumber(subCourse.learnerCount) }}
                        <span class="text-grey">{{ t('course.learning') }}</span>
                      </span>
                    </div>
                  </v-card-text>
                </v-card>

                <!-- 默认待创建卡片：快速入门 -->
                <v-card
                  rounded="xl"
                  class="sub-course-card placeholder-card hoverable"
                  elevation="0"
                  hover
                  @click="handleCreateDefaultSubCourse('quickstart')"
                >
                  <v-card-text class="pa-5 pa-sm-6">
                    <div class="d-flex align-center justify-space-between mb-4">
                      <div class="course-number placeholder-number">
                        <v-icon icon="mdi-plus" size="20" />
                      </div>
                      <v-chip size="small" color="warning" variant="tonal">
                        {{ t('course.toBeCreated') }}
                      </v-chip>
                    </div>
                    <h3 class="text-h6 text-md-h5 font-weight-bold mb-3 text-grey-darken-4">
                      {{ t('course.quickstart') }}
                    </h3>
                    <p class="text-body-2 text-grey-darken-2 mb-0 course-description">
                      {{ t('course.quickstartDesc') }}
                    </p>
                  </v-card-text>
                </v-card>

                <!-- 默认待创建卡片：习题练习 -->
                <v-card
                  rounded="xl"
                  class="sub-course-card placeholder-card hoverable"
                  elevation="0"
                  hover
                  @click="handleCreateDefaultSubCourse('exercises')"
                >
                  <v-card-text class="pa-5 pa-sm-6">
                    <div class="d-flex align-center justify-space-between mb-4">
                      <div class="course-number placeholder-number">
                        <v-icon icon="mdi-plus" size="20" />
                      </div>
                      <v-chip size="small" color="warning" variant="tonal">
                        {{ t('course.toBeCreated') }}
                      </v-chip>
                    </div>
                    <h3 class="text-h6 text-md-h5 font-weight-bold mb-3 text-grey-darken-4">
                      {{ t('course.exercises') }}
                    </h3>
                    <p class="text-body-2 text-grey-darken-2 mb-0 course-description">
                      {{ t('course.exercisesDesc') }}
                    </p>
                  </v-card-text>
                </v-card>
              </div>
            </div>
            <!-- 子课程列表结束 -->
          </div>
          <!-- 左侧内容结束 -->

          <!-- 右侧帮助信息 -->
          <div class="right-sidebar d-none d-lg-block">
            <v-card rounded="lg" class="help-card sticky-card no-border" flat>
              <v-card-title class="px-4 py-0 pb-3">
                <div class="d-flex align-center">
                  <v-icon icon="mdi-help-circle" color="primary" class="mr-2" />
                  <span class="text-h6 font-weight-bold">{{ t('course.helpInfo') }}</span>
                </div>
              </v-card-title>
              <v-card-text class="px-4 py-0">
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
          <!-- 右侧帮助信息结束 -->
        </div>
        <!-- 左右两栏内容结束 -->
      </div>
      <!-- 内容区结束 -->

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
    </div>
    <!-- course-detail-page 结束 -->
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
  padding-top: 0;
  padding-bottom: 48px;
}

/* 页面标题行 */
.page-header-row {
  margin-bottom: 24px;
}

@media (min-width: 1800px) {
  .page-header-row {
    margin-left: -60px;
  }
}

/* 课程头部卡片 */
.course-header-card {
  background: rgb(var(--v-theme-surface));
  border: 1px double rgb(var(--v-theme-outline)) !important;
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

/* 标题图标包装器 */
.section-icon-wrapper {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(var(--v-theme-surface-variant));
  border-radius: 12px;
}

.sub-course-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

@media (min-width: 600px) {
  .sub-course-grid {
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 24px;
  }
}

@media (min-width: 960px) {
  .sub-course-grid {
    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  }
}

.sub-course-card {
  background-color: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline));
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  height: 100%;
}

/* 课程编号 */
.course-number {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    135deg,
    rgb(var(--v-theme-primary)) 0%,
    rgb(var(--v-theme-primary-darken-1)) 100%
  );
  color: white;
  border-radius: 12px;
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
}

.placeholder-number {
  background: rgb(var(--v-theme-surface-variant)) !important;
  color: rgb(var(--v-theme-grey));
}

.course-title {
  line-height: 1.4;
}

.course-description {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.6;
}

.sticky-card {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow-y: auto;
}

.help-card {
  background-color: rgb(var(--v-theme-surface));
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
  opacity: 0.85;
}

.placeholder-card:hover {
  opacity: 1;
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
