<template>
  <DefaultLayout>
    <div class="course-list-page">
      <!-- 页面标题和搜索栏 -->
      <div class="page-header mb-6 mb-md-10">
        <div class="d-flex flex-column flex-sm-row align-start align-sm-end ga-4 header-wrapper">
          <!-- 左侧：标题 -->
          <div class="d-flex align-center title-container">
            <v-avatar
              :color="'rgb(var(--v-theme-surface-variant))'"
              :size="$vuetify.display.mobile ? 48 : 64"
              rounded="lg"
              class="mr-3 flex-shrink-0"
            >
              <v-icon
                icon="mdi-book-multiple"
                :size="$vuetify.display.mobile ? 24 : 32"
                color="grey-darken-1"
              />
            </v-avatar>
            <div style="min-width: 0; overflow: hidden">
              <h1 class="text-h5 text-md-h4 font-weight-bold text-grey-darken-4 text-truncate">
                {{ t('course.center') }}
              </h1>
              <p class="text-caption text-md-body-2 text-grey-darken-2 mt-1 text-truncate">
                {{ t('course.subtitle') }}
              </p>
            </div>
          </div>

          <!-- 右侧：操作按钮 -->
          <div class="d-flex align-center ga-3 actions-wrapper pb-1">
            <!-- 创建课程按钮（仅在右侧栏隐藏时显示） -->
            <v-btn
              color="primary"
              variant="flat"
              rounded="lg"
              class="d-lg-none flex-shrink-0"
              @click="openCreateDialog"
            >
              <v-icon icon="mdi-plus" size="20" class="mr-1" />
              创建
            </v-btn>
          </div>
        </div>
      </div>

      <!-- 分类导航和课程网格 -->
      <div class="content-layout">
        <div class="main-content">
          <!-- 页面初始加载状态 -->
          <LoadingSpinner v-if="categoryStore.loading && categories.length === 0" />

          <template v-else>
            <!-- 分类导航 -->
            <CourseFilter
              v-model:main-category="selectedMainCategory"
              v-model:sub-category="selectedSubCategory"
              :categories="categories"
              :sub-categories="subCategories"
            />

            <!-- 加载状态 -->
            <LoadingSpinner v-if="loading" />

            <!-- 空状态 -->
            <div v-else-if="courses.length === 0" class="text-center py-12">
              <v-card rounded="lg" class="pa-12 empty-state no-border">
                <v-icon
                  icon="mdi-book-open-variant"
                  size="80"
                  color="grey-lighten-1"
                  class="mb-4"
                />
                <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">
                  {{ t('course.noCoursesFound') }}
                </h3>
                <p class="text-body-1 text-grey-darken-1 mb-4">
                  {{ t('course.noCourses') }}
                </p>
                <v-btn
                  v-if="selectedMainCategory"
                  color="primary"
                  variant="outlined"
                  rounded="lg"
                  @click="clearAll"
                >
                  {{ t('course.viewAllCourses') }}
                </v-btn>
              </v-card>
            </div>

            <!-- 课程列表 -->
            <div v-else class="mt-6 mt-md-8">
              <!-- 课程网格 -->
              <div class="course-grid mb-16">
                <CourseCard
                  v-for="course in courses"
                  :key="course.id"
                  :course="course"
                  @subscribe="handleSubscribe"
                  @unsubscribe="handleUnsubscribe"
                />
              </div>

              <!-- 加载更多指示器 -->
              <div v-if="hasMoreCourses" ref="loadMoreTrigger" class="text-center mt-6 py-4 mb-16">
                <v-progress-circular v-if="loadingMore" indeterminate color="primary" size="32" />
              </div>
            </div>
          </template>
        </div>

        <!-- 右侧热门课程栏 -->
        <div class="right-sidebar d-none d-lg-block">
          <div class="sticky-wrapper">
            <!-- 创建课程按钮 -->
            <v-card
              rounded="xl"
              class="mb-4 create-course-card"
              elevation="0"
              @click="openCreateDialog"
            >
              <v-card-text class="pa-6">
                <div class="d-flex align-center">
                  <v-avatar color="primary" size="40" class="mr-3">
                    <v-icon icon="mdi-plus" size="22" color="white" />
                  </v-avatar>
                  <div class="flex-grow-1">
                    <div class="text-subtitle-1 font-weight-bold text-grey-darken-4">
                      {{ t('course.createNew') }}
                    </div>
                    <div class="text-caption text-grey">{{ t('course.shareKnowledge') }}</div>
                  </div>
                  <v-icon icon="mdi-chevron-right" size="20" color="grey-lighten-1" />
                </div>
              </v-card-text>
            </v-card>

            <v-card rounded="lg" class="popular-card no-border" flat>
              <v-card-title class="py-4 px-0 pb-3">
                <div class="d-flex align-center justify-space-between w-100">
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-fire" color="error" class="mr-2" />
                    <span class="text-h6 font-weight-bold">{{ t('course.hot') }}</span>
                  </div>
                  <v-btn
                    variant="text"
                    size="small"
                    color="primary"
                    class="text-caption"
                    @click="clearAll"
                  >
                    {{ t('course.all') }}
                    <v-icon icon="mdi-chevron-right" size="14" class="ml-1" />
                  </v-btn>
                </div>
              </v-card-title>
              <v-card-text class="px-0 popular-list pb-4">
                <div
                  v-for="(course, index) in popularCourses"
                  :key="course.id"
                  class="popular-item rounded-lg"
                  @click="goToCourseDetail(course)"
                >
                  <div class="rank-badge" :class="index < 3 ? 'rank-top' : ''">
                    {{ index + 1 }}
                  </div>
                  <div class="flex-grow-1">
                    <div class="popular-name">{{ course.name }}</div>
                    <div class="popular-count">
                      <v-icon icon="mdi-account-group" size="12" color="grey" />
                      {{ formatNumber(course.learnerCount) }}
                    </div>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </div>
        </div>
      </div>

      <!-- 创建课程对话框 -->
      <CourseCreateDialog
        v-model="createDialog"
        :categories="categories"
        :sub-categories="subCategories"
        :reset-form="resetCreateForm"
        :initial="dialogInitial"
        @submit="handleCreateCourse"
      />
    </div>
  </DefaultLayout>
</template>

<script lang="ts">
export default {
  name: 'CourseListPage',
}
</script>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { getGlobalSnackbar } from '@/composables/config'
import type { Course, CreateCourseRequest } from '@/types/course'
import { useCategoryStore } from '@/stores'
import { courseApi } from '@/api/modules/course'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import CourseCard from '@/components/features/course/CourseCard.vue'
import CourseFilter from '@/components/features/course/CourseFilter.vue'
import CourseCreateDialog from '@/components/features/course/CourseCreateDialog.vue'
import type { CourseCreateInitial } from '@/components/features/course/CourseCreateDialog.vue'
import {
  useHotCoursesQuery,
  useCourseListQuery,
  useCreateCourseMutation,
  useResubmitCourseMutation,
  useSubscribeMutation,
  useUnsubscribeMutation,
} from '@/queries/course'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const categoryStore = useCategoryStore()

// 状态管理
const selectedMainCategory = ref<number | undefined>()
const selectedSubCategory = ref<number | undefined>()
const createDialog = ref(false)
const resetCreateForm = ref(false)
const loadMoreTrigger = ref<HTMLElement | null>(null)

// resubmit 模式
const resubmitId = ref<number | null>(null)
const dialogInitial = ref<CourseCreateInitial>({})

// 监听 query：?resubmitId=xxx → 拉课程信息回填表单并打开对话框
watch(
  () => route.query.resubmitId,
  async (val) => {
    const idStr = Array.isArray(val) ? val[0] : val
    if (idStr == null) return
    const id = Number(idStr)
    if (!Number.isFinite(id) || id <= 0) return
    try {
      const course = await courseApi.getCourse(id)
      dialogInitial.value = {
        name: course.name ?? '',
        description: course.description ?? '',
        mainCategory: course.mainCategory ?? null,
        subCategory: course.subCategory ?? null,
        parentCourseId: course.parentCourse?.id ?? null,
        resubmit: true,
      }
      resubmitId.value = id
      createDialog.value = true
    } catch {
      getGlobalSnackbar()?.(t('common.fetchFailed'), 'error')
    }
  },
  { immediate: true }
)

// 热门课程
const { data: hotCoursesData } = useHotCoursesQuery()

// 分页课程列表（分类筛选）
const {
  data: courseListData,
  isLoading: loading,
  isFetchingNextPage,
  hasNextPage,
  fetchNextPage,
} = useCourseListQuery(selectedMainCategory, selectedSubCategory)

const loadingMore = computed(() => isFetchingNextPage.value)
const hasMoreCourses = computed(() => hasNextPage.value)

// 计算属性 - 从 Store 中获取分类数据
const categories = computed(() => {
  return categoryStore.getCourseMainCategories()
})

const subCategories = computed(() => {
  const allSubCategories: { id: number; name: string; mainCategoryId: number }[] = []
  categories.value.forEach((mainCategory) => {
    const subs = categoryStore.getCourseSubCategories(mainCategory.id)
    subs.forEach((sub) => {
      allSubCategories.push({ id: sub.id, name: sub.name, mainCategoryId: mainCategory.id })
    })
  })
  return allSubCategories
})

const courses = computed(() => courseListData.value?.pages.flatMap((p) => p.items) ?? [])
const hotCourses = computed(() => hotCoursesData.value ?? [])

// 热门课程 - 前15个
const popularCourses = computed(() => hotCourses.value.slice(0, 15))

const formatNumber = (num?: number) => {
  if (!num) return '0'
  return num.toLocaleString()
}

const clearAll = () => {
  selectedMainCategory.value = undefined
  selectedSubCategory.value = undefined
}

let observer: IntersectionObserver | null = null

const setupInfiniteScroll = () => {
  if (!loadMoreTrigger.value) return
  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry?.isIntersecting && hasNextPage.value && !isFetchingNextPage.value) {
        void fetchNextPage()
      }
    },
    { root: null, rootMargin: '100px', threshold: 0.1 }
  )
  observer.observe(loadMoreTrigger.value)
}

const cleanupInfiniteScroll = () => {
  if (observer && loadMoreTrigger.value) {
    observer.unobserve(loadMoreTrigger.value)
    observer.disconnect()
    observer = null
  }
}

onMounted(async () => {
  await categoryStore.checkAndLoad()
  setTimeout(setupInfiniteScroll, 100)
})

onBeforeUnmount(() => {
  cleanupInfiniteScroll()
})

const goToCourseDetail = (course: Course) => {
  void router.push(`/courses/${String(course.id)}`)
}

// 订阅/取消订阅（乐观更新本地状态，mutation 自动 invalidate cache）
const subscribeMutation = useSubscribeMutation()
const unsubscribeMutation = useUnsubscribeMutation()

const handleSubscribe = (courseId: number) => {
  subscribeMutation.mutate(courseId)
}

const handleUnsubscribe = (courseId: number) => {
  unsubscribeMutation.mutate(courseId)
}

const openCreateDialog = () => {
  // 走全新创建：清空 resubmit 上下文
  resubmitId.value = null
  dialogInitial.value = {}
  createDialog.value = true
}

const onDialogClose = (open: boolean) => {
  if (!open && resubmitId.value !== null) {
    resubmitId.value = null
    dialogInitial.value = {}
    if (route.query.resubmitId !== undefined) {
      const { resubmitId: _omit, ...rest } = route.query
      void router.replace({ query: rest })
    }
  }
}

watch(createDialog, (val) => onDialogClose(val))

const createCourseMutation = useCreateCourseMutation()
const resubmitCourseMutation = useResubmitCourseMutation()

const handleCreateCourse = (courseData: CreateCourseRequest) => {
  if (resubmitId.value !== null) {
    if (
      courseData.mainCategory == null ||
      courseData.subCategory == null ||
      !courseData.name ||
      !courseData.description
    ) {
      // 子课程在表单上没分类输入；resubmit 表单一律保留原 mainCategory/subCategory
      return
    }
    resubmitCourseMutation.mutate(
      {
        id: resubmitId.value,
        data: {
          name: courseData.name,
          description: courseData.description,
          mainCategory: courseData.mainCategory,
          subCategory: courseData.subCategory,
        },
      },
      {
        onSuccess: () => {
          createDialog.value = false
          resetCreateForm.value = true
          setTimeout(() => {
            resetCreateForm.value = false
          }, 100)
          getGlobalSnackbar()?.(t('common.success'), 'success')
        },
      }
    )
  } else {
    createCourseMutation.mutate(courseData, {
      onSuccess: () => {
        createDialog.value = false
        resetCreateForm.value = true
        setTimeout(() => {
          resetCreateForm.value = false
        }, 100)
      },
    })
  }
}
</script>

<style scoped>
.course-list-page {
  padding-top: 24px;
}

@media (max-width: 960px) {
  .course-list-page {
    padding-top: 16px;
  }
}

.page-header {
  margin-bottom: 16px;
}

@media (min-width: 960px) {
  .page-header {
    margin-bottom: 24px;
  }
}

/* 头部包装器 */
.header-wrapper {
  width: 100%;
}

/* 标题容器 */
.title-container {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

/* 操作按钮包装器（搜索框+按钮） */
.actions-wrapper {
  width: 100%;
  flex-shrink: 0;
}

@media (min-width: 600px) {
  .actions-wrapper {
    width: auto;
    flex-shrink: 0;
  }
}

.empty-state {
  background-color: rgb(var(--v-theme-surface));
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

/* 内容布局 */
.content-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

@media (min-width: 1280px) {
  .content-layout {
    flex-direction: row;
    gap: 48px;
  }
}

.main-content {
  flex: 1;
  min-width: 0;
}

/* 右侧热门课程栏 */
.right-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.sticky-wrapper {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  display: flex;
  flex-direction: column;
}

.create-course-card {
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #e9ecef;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.create-course-card:hover {
  border-color: rgb(var(--v-theme-primary));
  transform: translateY(-2px);
}

.popular-card {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: rgb(var(--v-theme-surface));
}

.popular-list {
  overflow-y: auto;
  flex: 1;
}

.popular-list::-webkit-scrollbar {
  width: 4px;
}

.popular-list::-webkit-scrollbar-track {
  background: transparent;
}

.popular-list::-webkit-scrollbar-thumb {
  background-color: rgba(var(--v-theme-on-surface), 0.1);
  border-radius: 2px;
}

.popular-list::-webkit-scrollbar-thumb:hover {
  background-color: rgba(var(--v-theme-on-surface), 0.2);
}

.popular-item {
  display: flex;
  align-items: center;
  padding: 10px 5px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 8px;
}

.popular-item:hover {
  background-color: rgb(var(--v-theme-surface-variant));
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background-color: rgb(var(--v-theme-surface-variant));
  color: rgb(var(--v-theme-on-surface-variant));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  margin-right: 12px;
  flex-shrink: 0;
}

.rank-badge.rank-top {
  background: linear-gradient(135deg, #ffd700 0%, #ffa500 100%);
  color: rgb(var(--v-theme-surface));
}

.popular-name {
  font-size: 14px;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface));
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.popular-count {
  font-size: 12px;
  color: rgb(var(--v-theme-on-surface-variant));
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 移动端响应式 */
@media (max-width: 960px) {
  .course-grid {
    grid-template-columns: 1fr;
  }

  .popular-list {
    max-height: 400px;
  }
}
</style>
