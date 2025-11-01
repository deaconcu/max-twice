<script setup lang="ts">
import { inject, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  courseServiceV1,
  progressServiceV1,
  subscriptionServiceV1,
} from '@/services/api/v1/apiServiceV1'
import { useUserStore } from '@/stores/user'
import type { Course } from '@/types/course'
import { useMutation } from '@/composables/useMutation'

// 扩展课程信息，添加UI所需字段
interface CourseInfo extends Course {
  subscribed?: boolean
}

interface SubCourse {
  id?: number
  name: string
  description: string
}

interface ApplyCourseData {
  name: string
  description: string
}

interface Props {
  parentCourseInfo?: CourseInfo | null
  currentCourse?: Course
  subCourseList?: SubCourse[]
  isMainCourse?: boolean
  isLearning?: boolean
  displayCourseAll?: boolean
}

interface Emits {
  (e: 'start-learning', data: any): void
  (e: 'subscribe-course', data: { courseId: number; action: boolean; data: any }): void
  (e: 'show-details'): void
  (e: 'apply-course'): void
  (e: 'toggle-display'): void
  (e: 'subcourse-created'): void
}

const props = withDefaults(defineProps<Props>(), {
  parentCourseInfo: null,
  currentCourse: () => ({} as Course),
  subCourseList: () => [],
  isMainCourse: true,
  isLearning: false,
  displayCourseAll: true,
})

const emit = defineEmits<Emits>()

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const user = useUserStore()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 创建子课程对话框状态
const applyCourseDialog = ref<boolean>(false)
const subcourseForm = ref<any>(null)
const applyCourseData = ref<ApplyCourseData>({
  name: '',
  description: '',
})

// 使用 useMutation 处理订阅/取消订阅
const { execute: handleSubscription } = useMutation(
  (data: { courseId: number; action: boolean }) => {
    return data.action
      ? subscriptionServiceV1.subscribe(data.courseId)
      : subscriptionServiceV1.unsubscribe(data.courseId)
  },
  {
    onSuccess: (response, data) => {
      // 更新用户store中的数据
      if (user.currentUser) {
        user.setUser({
          ...user.currentUser,
          subscriptions: response
        })
      }
      showSnackbar?.(
        data.action ? t('read.messages.favoriteSuccess') : t('read.messages.favoriteRemoved'),
        'success'
      )
      emit('subscribe-course', { courseId: data.courseId, action: data.action, data: response })
    },
    onError: () => {
      showSnackbar?.(t('read.messages.operationFailed'), 'error')
    },
  },
)

// 订阅/取消订阅课程
const subscript = async (courseId: number, action: boolean): Promise<void> => {
  await handleSubscription({ courseId, action })
}

// 使用 useMutation 开始学习课程
const { execute: startLearning } = useMutation(
  () => progressServiceV1.startCourse(parseInt(route.query.courseId as string)),
  {
    onSuccess: (response) => {
      emit('start-learning', response)
      if (response) {
        showSnackbar?.(t('read.messages.startLearningSuccess'))
      } else {
        showSnackbar?.(t('read.messages.stopLearningSuccess'))
      }
    },
    onError: () => {
      showSnackbar?.(t('read.messages.operationFailed'))
    },
  },
)

// 开始学习课程
const startCourse = async (): Promise<void> => {
  await startLearning()
}

// 跳转到子课程
const goToSubcourse = (subcourse: SubCourse): void => {
  if (subcourse.id) {
    router.push(`/read?courseId=${subcourse.id}`)
  }
}

// 跳转到主课程
const goToParentCourse = (): void => {
  if (props.parentCourseInfo && props.parentCourseInfo.id) {
    router.push(`/read?courseId=${props.parentCourseInfo.id}`)
  }
}

// 使用 useMutation 创建子课程
const { execute: createSubcourse, loading: isCreatingSubcourse } = useMutation(
  () => {
    const courseId = parseInt(route.query.courseId as string)
    return courseServiceV1.createSubcourse(
      courseId,
      applyCourseData.value.name,
      applyCourseData.value.description
    )
  },
  {
    successMessage: t('read.subcourse.applySuccess'),
    onSuccess: () => {
      applyCourseDialog.value = false
      applyCourseData.value = { name: '', description: '' }
      emit('subcourse-created')
    },
    onError: (error: any) => {
      showSnackbar?.(
        t('read.subcourse.createFailedWithMsg', {
          msg: error?.message || t('read.subcourse.unknownError'),
        }),
        'error'
      )
    },
  },
)

// 创建子课程
const postApplyCourse = async (): Promise<void> => {
  if (!user.currentUser?.id) {
    showSnackbar?.(t('read.messages.pleaseLogin'), 'error')
    return
  }
  await createSubcourse()
}

const closeCreateDialog = (): void => {
  applyCourseDialog.value = false
  applyCourseData.value = { name: '', description: '' }
}
</script>

<template>
  <div class="course-header">
    <div class="d-flex align-center">
      <div class="course-info">
        <!-- 课程标题区域 -->
        <div class="d-flex align-center mb-4">
          <v-avatar size="32" border="md" class="me-3">
            <v-icon icon="mdi-book-open-outline" size="20" color="grey"></v-icon>
          </v-avatar>
          <h1 class="course-title">{{ parentCourseInfo?.name || currentCourse.name }}</h1>
          <v-chip
            v-if="!isMainCourse"
            color="red-darken-2"
            variant="flat"
            rounded="xl"
            class="ms-5 px-5"
            density="comfortable"
            prepend-icon="mdi-bookmark-outline"
          >
            <span class="font-weight-black text-body-1">{{
              t('read.course.subCourse', { name: currentCourse.name })
            }}</span>
          </v-chip>
        </div>

        <!-- 标签和操作按钮一行显示 -->
        <div class="d-flex align-center">
          <!-- 返回主课程按钮 -->
          <v-btn
            v-if="!isMainCourse && parentCourseInfo"
            variant="text"
            rounded="lg"
            prepend-icon="mdi-arrow-left"
            size="small"
            class="action-btn-inline"
            @click="goToParentCourse"
          >
            <span class="text-body-2">{{ t('read.course.backToMainCourse') }}</span>
          </v-btn>

          <!-- 订阅按钮 -->
          <v-btn
            v-if="parentCourseInfo && parentCourseInfo.subscribed"
            prepend-icon="mdi-heart"
            variant="text"
            color="error"
            rounded="lg"
            size="small"
            class="action-btn-inline"
            @click="subscript(parentCourseInfo ? parentCourseInfo.id : currentCourse.id, false)"
          >
            <span class="text-body-2">{{ t('read.course.subscribed') }}</span>
          </v-btn>

          <v-btn
            v-else
            prepend-icon="mdi-heart-outline"
            variant="text"
            rounded="lg"
            size="small"
            class="action-btn-inline"
            @click="subscript(parentCourseInfo ? parentCourseInfo.id : currentCourse.id, true)"
          >
            <span class="text-body-2">订阅课程</span>
          </v-btn>

          <!-- 展开/收起按钮，右对齐 -->
          <v-btn
            variant="text"
            rounded="lg"
            :prepend-icon="displayCourseAll ? 'mdi-chevron-up' : 'mdi-chevron-down'"
            size="small"
            class="expand-btn-inline"
            @click="emit('toggle-display')"
          >
            <span class="text-body-2">{{
              displayCourseAll ? t('read.course.collapse') : t('read.course.details')
            }}</span>
          </v-btn>
        </div>
      </div>
      <div class="course-actions">
        <div class="progress-info">
          <v-icon icon="mdi-chart-donut" size="20" color="grey-darken-3"></v-icon>
          <div class="progress-text">
            <span class="progress-stats">23,434 节点</span>
            <div class="progress-bar-container">
              <v-progress-linear
                :model-value="60"
                color="primary"
                height="4"
                rounded
                class="progress-bar"
              ></v-progress-linear>
              <span class="progress-percent">60%</span>
            </div>
          </div>
        </div>

        <!-- 学习状态区域 -->
        <div
          class="learning-status-container clickable"
          :class="{ 'learning-active': isLearning }"
          @click="startCourse"
        >
          <!-- 第一行：状态信息 -->
          <div class="d-flex justify-space-between align-center" :class="{ 'mb-1': isLearning }">
            <div class="d-flex align-center">
              <v-icon
                :icon="
                  !isLearning
                    ? 'mdi-play-circle-outline'
                    : (currentCourse?.progress || 0) >= 10000
                      ? 'mdi-check-circle'
                      : 'mdi-play-circle'
                "
                size="16"
                class="mr-1"
                :color="
                  !isLearning
                    ? 'grey-darken-2'
                    : (currentCourse?.progress || 0) >= 10000
                      ? 'success'
                      : 'primary'
                "
              ></v-icon>
              <span
                class="text-body-2 font-weight-bold"
                :class="
                  !isLearning
                    ? 'text-grey-darken-3'
                    : (currentCourse?.progress || 0) >= 10000
                      ? 'text-success'
                      : 'text-primary'
                "
              >
                {{
                  !isLearning
                    ? t('read.course.startLearning')
                    : (currentCourse?.progress || 0) >= 10000
                      ? t('read.course.learningCompleted')
                      : t('read.course.learningInProgress')
                }}
              </span>
            </div>
            <!-- 只有在学习状态下才显示进度百分比 -->
            <span v-if="isLearning" class="text-caption font-weight-bold"
              >{{ parseFloat(((currentCourse?.progress || 0) / 100).toFixed(2)) }}%</span
            >
          </div>

          <!-- 第二行：进度条（只有在学习状态下才显示） -->
          <v-progress-linear
            v-if="isLearning"
            :model-value="(currentCourse?.progress || 0) / 100"
            :color="(currentCourse?.progress || 0) >= 10000 ? 'success' : 'primary'"
            height="3"
            rounded
          ></v-progress-linear>
        </div>
      </div>
    </div>

    <v-expand-transition>
      <div v-if="displayCourseAll" class="expanded-content">
        <div class="course-description">
          <!-- 如果有父课程信息，优先显示父课程描述；否则显示当前课程描述 -->
          {{ parentCourseInfo ? parentCourseInfo.description : currentCourse.description }}
        </div>

        <!-- 子课程列表标题 -->
        <div class="subcourses-header">
          <v-chip
            color=""
            variant="text"
            prepend-icon="mdi-format-list-group"
            rounded="lg"
            class="me-0"
          >
            <span class="text-grey-darken-4"> 子课程列表 </span>
          </v-chip>
          <v-icon
            icon="mdi-information-outline"
            variant="text"
            size="x-small"
            color="grey-darken-1"
            class="mx-0"
          ></v-icon>
          <v-spacer></v-spacer>
          <!-- 只有主课程才能申请子课程 -->
          <v-btn
            variant="text"
            prepend-icon="mdi-plus-circle-outline"
            color="grey-darken-4"
            size="default"
            density="default"
            rounded="xl"
            @click="applyCourseDialog = true"
          >
            申请子课程
          </v-btn>
        </div>

        <div class="">
          <div
            v-for="subcourse in subCourseList"
            :key="subcourse.id || subcourse.name"
            class="subcourse-item"
          >
            <div class="subcourse-info">
              <h3 class="subcourse-title">{{ subcourse.name }}</h3>
              <p class="subcourse-desc">{{ subcourse.description }}</p>
            </div>
            <!-- 如果当前子课程就是正在学习的课程，显示"正在学习"标签 -->
            <v-chip
              v-if="!isMainCourse && subcourse.id === currentCourse.id"
              variant="flat"
              color="success"
              rounded="lg"
              prepend-icon="mdi-play-circle"
            >
              <span class="font-weight-bold">正在学习</span>
            </v-chip>
            <v-chip
              v-else
              variant="tonal"
              color="success"
              rounded="lg"
              :disabled="!subcourse.id"
              prepend-icon="mdi-play"
              @click="goToSubcourse(subcourse)"
            >
              <span class="font-weight-medium">开始学习</span>
            </v-chip>
          </div>

          <!-- 空状态提示 -->
          <div v-if="subCourseList.length === 0" class="empty-subcourses">
            <p class="text-grey d-flex align-center justify-center mb-1">
              <v-icon
                icon="mdi-book-outline"
                size="18"
                color="grey-lighten-1"
                class="me-2"
              ></v-icon>
              <span class="font-weight-medium text-body-2">
                {{
                  isMainCourse ? t('read.course.noSubCourses') : t('read.course.noSameLevelCourses')
                }}
              </span>
            </p>
            <p v-if="isMainCourse" class="text-body-2 text-grey-lighten-1">
              点击上方 + 按钮创建第一个子课程
            </p>
          </div>
        </div>
      </div>
    </v-expand-transition>

    <!-- 创建子课程对话框 -->
    <v-dialog v-model="applyCourseDialog" width="500" persistent>
      <v-card rounded="xl" elevation="8">
        <!-- 头部 -->
        <div class="d-flex align-center justify-space-between pa-6 pb-4">
          <div class="d-flex align-center">
            <div class="pa-3 rounded-lg bg-blue-lighten-5 mr-3">
              <v-icon icon="mdi-book-plus-multiple" color="blue-darken-1" size="20"></v-icon>
            </div>
            <div>
              <h3 class="text-h6 font-weight-bold text-grey-darken-3">创建子课程</h3>
              <p class="text-body-2 text-grey-darken-1 mb-0">为当前课程添加新的子课程</p>
            </div>
          </div>
          <v-btn icon="mdi-close" variant="text" size="small" @click="closeCreateDialog"></v-btn>
        </div>

        <v-divider></v-divider>

        <!-- 表单内容 -->
        <v-card-text class="pa-6">
          <v-form ref="subcourseForm" @submit.prevent="postApplyCourse">
            <div class="mb-4">
              <label class="text-body-2 font-weight-medium text-grey-darken-2 mb-2 d-block">
                课程名称 <span class="text-red">*</span>
              </label>
              <v-text-field
                v-model="applyCourseData.name"
                :placeholder="t('read.subcourse.namePlaceholder')"
                variant="outlined"
                rounded="lg"
                density="comfortable"
                :rules="[(v: string) => !!v || t('read.subcourse.nameRequired')]"
                hide-details="auto"
              ></v-text-field>
            </div>

            <div class="mb-4">
              <label class="text-body-2 font-weight-medium text-grey-darken-2 mb-2 d-block">
                课程描述 <span class="text-red">*</span>
              </label>
              <v-textarea
                v-model="applyCourseData.description"
                :placeholder="t('read.subcourse.descriptionPlaceholder')"
                variant="outlined"
                rounded="lg"
                rows="4"
                density="comfortable"
                :rules="[(v: string) => !!v || t('read.subcourse.descriptionRequired')]"
                hide-details="auto"
              ></v-textarea>
            </div>
          </v-form>
        </v-card-text>

        <!-- 底部操作按钮 -->
        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn
            variant="outlined"
            rounded="lg"
            class="mr-3"
            :disabled="isCreatingSubcourse"
            @click="closeCreateDialog"
          >
            取消
          </v-btn>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            :loading="isCreatingSubcourse"
            :disabled="!applyCourseData.name || !applyCourseData.description"
            @click="postApplyCourse"
          >
            <v-icon icon="mdi-check" size="16" class="mr-2"></v-icon>
            创建课程
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
/* 课程头部 - 简洁版 */
.course-header {
  border-radius: 16px;
  margin-bottom: 2px;
  border: 2px #4c83cc solid;
  border-top: 8px #4c83cc solid;
  padding: 18px 16px;
  transition: background-color 0.2s ease;
}

.course-info {
  flex: 1;
}

.course-icon {
  color: #424242;
}

.course-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.2;
}

.course-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 240px;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8f9fa;
  padding: 12px 16px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.progress-text {
  flex: 1;
}

.progress-stats {
  font-size: 14px;
  font-weight: 600;
  color: #212121;
  display: block;
  line-height: 1.2;
  margin-bottom: 6px;
}

.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-bar {
  flex: 1;
  min-width: 120px;
}

.progress-percent {
  font-size: 12px;
  font-weight: 600;
  color: #424242;
  min-width: 32px;
}

/* 学习状态容器样式 */
.learning-status-container {
  background: #f8f9fa;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  min-width: 180px;
  height: 65px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: all 0.2s ease;
}

/* 未开始学习时居中显示 */
.learning-status-container:not(.learning-active) {
  justify-content: center;
  align-items: center;
}

.learning-status-container.clickable {
  cursor: pointer;
  user-select: none;
}

.learning-status-container.clickable:hover {
  background: #e3f2fd;
  border-color: #1976d2;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.2);
}

/* 展开内容 */
.expanded-content {
  padding-top: 20px;
  margin-top: 0px;
}

.course-description {
  font-size: 15px;
  line-height: 1.6;
  color: #212121;
  margin-bottom: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
  border-left: 4px solid #b2d3dc;
}

.subcourses-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  padding-bottom: 2px;
}

.subcourse-info {
  flex: 1;
}

.subcourse-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 4px 0;
  line-height: 1.3;
}

.subcourse-desc {
  font-size: 14px;
  color: #424242;
  margin: 0;
  line-height: 1.4;
}

.subcourse-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  margin-bottom: 8px;
  border-radius: 12px;
  background: #fafafa;
  transition: all 0.2s ease;
}

.subcourse-item:hover {
  background: #f5f5f5;
  border-color: #e0e0e0;
}

.subcourse-info {
  flex: 1;
  margin-right: 12px;
}

.subcourse-title {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 2px;
  line-height: 1.3;
}

.subcourse-desc {
  color: #666;
  font-size: 0.85rem;
  line-height: 1.3;
  margin: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.empty-subcourses {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 24px;
  text-align: center;
  border: 0px dashed #e0e0e0;
  border-radius: 12px;
  margin: 0px 0;
  background-color: #f9f9f9;
}

.empty-subcourses p {
  margin: 4px 0;
}

.action-btn-inline:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
}

.expand-btn-inline:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
}
</style>