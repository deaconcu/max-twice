<template>
  <DefaultLayout>
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
              探索知识，成就未来
            </p>
          </div>
        </div>

        <!-- 右侧：搜索栏和操作按钮 -->
        <div class="d-flex align-center ga-3 actions-wrapper pb-1">
          <v-text-field
            v-model="searchText"
            placeholder="搜索课程..."
            variant="outlined"
            density="compact"
            hide-details
            clearable
            class="search-input"
            color="primary"
            @keyup.enter="handleSearch"
          >
            <template #prepend-inner>
              <v-icon icon="mdi-magnify" color="grey-darken-1" size="20" />
            </template>
            <template #append-inner>
              <v-btn
                icon="mdi-arrow-right"
                color="grey-darken-1"
                variant="text"
                size="small"
                density="comfortable"
                @click="handleSearch"
              ></v-btn>
            </template>
          </v-text-field>
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
            @change="handleFilterChange"
          />

          <!-- 加载状态 -->
          <LoadingSpinner v-if="loading" />

          <!-- 空状态 -->
          <div v-else-if="filteredCourses.length === 0" class="text-center py-12">
          <v-card rounded="lg" class="pa-12 empty-state no-border">
            <v-icon icon="mdi-book-open-variant" size="80" color="grey-lighten-1" class="mb-4" />
            <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">未找到相关课程</h3>
            <p class="text-body-1 text-grey-darken-1 mb-4">
              {{ searchText ? '请尝试其他搜索关键词' : '该分类下暂时没有课程' }}
            </p>
            <v-btn
              v-if="selectedMainCategory || searchText"
              color="primary"
              variant="outlined"
              rounded="lg"
              @click="clearAll"
            >
              查看全部课程
            </v-btn>
          </v-card>
        </div>

        <!-- 课程列表 -->
        <div v-else>
          <!-- 分类标题 -->
          <div class="mb-4 mt-6 mt-md-10">
            <div class="d-flex align-center justify-space-between">
              <div class="d-flex align-center">
                <v-icon
                  icon="mdi-format-list-bulleted"
                  size="20"
                  class="mr-2 text-grey-darken-2"
                ></v-icon>
                <h2 class="text-h6 font-weight-regular text-grey-darken-4">
                  <span v-if="searchText">搜索结果</span>
                  <span v-else-if="selectedSubCategory">
                    {{ getCategoryName(selectedMainCategory) }} -
                    {{ getSubCategoryName(selectedSubCategory) }}
                  </span>
                  <span v-else-if="selectedMainCategory">{{ getCategoryName(selectedMainCategory) }}</span>
                  <span v-else>全部课程</span>
                </h2>
              </div>
              <p class="text-body-2 text-grey-darken-2">共 {{ filteredCourses.length }} 门课程</p>
            </div>
          </div>

          <!-- 课程网格 -->
          <div class="course-grid mb-16">
            <CourseCard
              v-for="course in displayedCourses"
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
                  <div class="text-caption text-grey">分享你的知识与经验</div>
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
                  <span class="text-h6 font-weight-bold">热门课程</span>
                </div>
                <v-btn
                  variant="text"
                  size="small"
                  color="primary"
                  class="text-caption"
                  @click="clearAll"
                >
                  全部
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
      @submit="handleCreateCourse"
    />
  </DefaultLayout>
</template>

<script lang="ts">
export default {
  name: 'CourseListPage',
}
</script>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useFetch, useMutation } from '@/composables'
import { courseApi, subscriptionApi } from '@/api'
import type { Course, CreateCourseRequest } from '@/types/course'
import { useCategoryStore } from '@/stores'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import CourseCard from '@/components/features/course/CourseCard.vue'
import CourseFilter from '@/components/features/course/CourseFilter.vue'
import CourseCreateDialog from '@/components/features/course/CourseCreateDialog.vue'

const router = useRouter()
const { t } = useI18n()
const categoryStore = useCategoryStore()

// 状态管理
const searchText = ref('')
const selectedMainCategory = ref<number | undefined>()
const selectedSubCategory = ref<number | undefined>()
const createDialog = ref(false)
const resetCreateForm = ref(false)
const loadMoreTrigger = ref<HTMLElement | null>(null)

// 当前分类查询参数（subCategory 可选）
const currentCategory = ref<{ mainCategory: number; subCategory?: number } | null>(null)

// 使用 useFetch 加载热门课程
const { data: hotCoursesData, loading: _loadingHotCourses } = useFetch<Course[]>({
  fetchFn: () => courseApi.getHotCourses(),
  immediate: true,
  defaultValue: [],
})

// 使用 ref 直接管理课程列表（不用 useFetch，因为需要追加数据）
const coursesData = ref<Course[]>([])
const loading = ref(false)
const loadingMore = ref(false)

// 分页状态
const lastId = ref<number | undefined>(undefined)
const hasMoreCourses = ref(true)

/**
 * 加载课程列表（初始加载或刷新）
 */
const loadCourses = async (reset = false) => {
  if (reset) {
    loading.value = true
    lastId.value = undefined
    hasMoreCourses.value = true
  } else {
    loadingMore.value = true
  }

  try {
    let response
    if (!currentCategory.value) {
      // 不选分类时，返回所有已发布课程
      response = await courseApi.getCoursesByCategory(undefined, undefined, lastId.value)
    } else {
      const { mainCategory, subCategory } = currentCategory.value
      response = await courseApi.getCoursesByCategory(mainCategory, subCategory, lastId.value)
    }

    if (response.data) {
      const pageResponse = response.data
      const newCourses = pageResponse.items

      if (reset) {
        coursesData.value = newCourses
      } else {
        coursesData.value = [...coursesData.value, ...newCourses]
      }

      // 更新分页状态
      hasMoreCourses.value = pageResponse.hasMore
      if (pageResponse.hasMore && pageResponse.nextCursor?.lastId) {
        lastId.value = pageResponse.nextCursor.lastId
      }
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 计算属性 - 从 Store 中获取分类数据
const categories = computed(() => {
  return categoryStore.getCourseMainCategories()
})

const subCategories = computed(() => {
  const allSubCategories: { id: number; name: string; mainCategoryId: number }[] = []

  categories.value.forEach((mainCategory) => {
    const subs = categoryStore.getCourseSubCategories(mainCategory.id)
    subs.forEach((sub) => {
      allSubCategories.push({
        id: sub.id,
        name: sub.name,
        mainCategoryId: mainCategory.id,
      })
    })
  })

  return allSubCategories
})

const courses = computed(() => coursesData.value ?? [])
const hotCourses = computed(() => hotCoursesData.value ?? [])

// 筛选后的课程（只在前端筛选搜索关键词）
const filteredCourses = computed(() => {
  let result = courses.value

  // 按搜索关键词筛选
  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    result = result.filter(
      (course) =>
        course.name.toLowerCase().includes(keyword) ||
        course.description?.toLowerCase().includes(keyword)
    )
  }

  return result
})

// 显示的课程就是筛选后的课程（服务端分页）
const displayedCourses = computed(() => {
  return filteredCourses.value
})

// 热门课程 - 前15个
const popularCourses = computed(() => {
  return hotCourses.value.slice(0, 15)
})

/**
 * 格式化数字（千位分隔）
 */
const formatNumber = (num?: number) => {
  if (!num) return '0'
  return num.toLocaleString()
}

/**
 * 获取分类名称
 */
const getCategoryName = (categoryId?: number) => {
  if (!categoryId) return ''
  return categories.value.find((c) => c.id === categoryId)?.name ?? ''
}

/**
 * 获取子分类名称
 */
const getSubCategoryName = (subCategoryId?: number) => {
  if (!subCategoryId) return ''
  return subCategories.value.find((s) => s.id === subCategoryId)?.name ?? ''
}

/**
 * 清空所有筛选
 */
const clearAll = () => {
  selectedMainCategory.value = undefined
  selectedSubCategory.value = undefined
  searchText.value = ''
  currentCategory.value = null
  void loadCourses(true) // 重新加载
}

/**
 * 根据分类加载课程（subCategory 可选）
 */
const loadCoursesByCategory = async (
  mainCategory: number,
  subCategory?: number
): Promise<void> => {
  currentCategory.value = { mainCategory, subCategory }
  await loadCourses(true) // 重置并加载
}

/**
 * 处理搜索
 */
const handleSearch = async () => {
  if (!searchText.value) {
    currentCategory.value = null
    return
  }

  try {
    loading.value = true
    const response = await courseApi.searchCourses(searchText.value)
    if (response.data) {
      coursesData.value = response.data
    }
  } catch (error) {
    console.error('搜索课程失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 处理筛选变化
 */
const handleFilterChange = () => {
  searchText.value = '' // 清空搜索
  // 支持只传主分类或主分类+子分类
  if (selectedMainCategory.value) {
    void loadCoursesByCategory(selectedMainCategory.value, selectedSubCategory.value)
  }
}

/**
 * 监听分类变化，自动加载课程
 */
watch([selectedMainCategory, selectedSubCategory], async () => {
  // 先清理旧的 observer
  cleanupInfiniteScroll()

  // 只要选了主分类就加载（子分类可选）
  if (selectedMainCategory.value) {
    await loadCoursesByCategory(selectedMainCategory.value, selectedSubCategory.value)
  } else {
    currentCategory.value = null
    await loadCourses(true)
  }

  // 数据加载完成后，重新设置无限滚动
  setTimeout(setupInfiniteScroll, 100)
})

/**
 * 加载更多
 */
const loadMore = () => {
  if (loadingMore.value || !hasMoreCourses.value) return
  void loadCourses(false) // 追加加载
}

/**
 * Intersection Observer 实例
 */
let observer: IntersectionObserver | null = null

/**
 * 设置无限滚动
 */
const setupInfiniteScroll = () => {
  if (!loadMoreTrigger.value) return

  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry?.isIntersecting && hasMoreCourses.value && !loadingMore.value) {
        loadMore()
      }
    },
    {
      root: null,
      rootMargin: '100px',
      threshold: 0.1,
    }
  )

  observer.observe(loadMoreTrigger.value)
}

/**
 * 清理 Intersection Observer
 */
const cleanupInfiniteScroll = () => {
  if (observer && loadMoreTrigger.value) {
    observer.unobserve(loadMoreTrigger.value)
    observer.disconnect()
    observer = null
  }
}

/**
 * 组件挂载时设置无限滚动
 */
onMounted(async () => {
  // 加载分类数据
  await categoryStore.checkAndLoad()
  // 初始加载课程
  await loadCourses(true)
  // 延迟设置无限滚动，确保 DOM 已渲染
  setTimeout(setupInfiniteScroll, 100)
})

/**
 * 组件卸载时清理
 */
onBeforeUnmount(() => {
  cleanupInfiniteScroll()
})

/**
 * 跳转到课程详情
 */
const goToCourseDetail = (course: Course) => {
  void router.push(`/courses/${String(course.id)}`)
}

/**
 * 订阅课程
 */
const handleSubscribe = async (courseId: number) => {
  try {
    await subscriptionApi.subscribe(courseId)
    // 更新本地状态
    const course = courses.value.find((c) => c.id === courseId)
    if (course) {
      course.subscribed = true
      if (course.subscriptionCount) {
        course.subscriptionCount += 1
      }
    }
    // 同时更新热门课程列表中的状态
    const hotCourse = hotCourses.value.find((c) => c.id === courseId)
    if (hotCourse) {
      hotCourse.subscribed = true
      if (hotCourse.subscriptionCount) {
        hotCourse.subscriptionCount += 1
      }
    }
  } catch (error) {
    console.error('订阅失败:', error)
  }
}

/**
 * 取消订阅
 */
const handleUnsubscribe = async (courseId: number) => {
  try {
    await subscriptionApi.unsubscribe(courseId)
    // 更新本地状态
    const course = courses.value.find((c) => c.id === courseId)
    if (course) {
      course.subscribed = false
      if (course.subscriptionCount && course.subscriptionCount > 0) {
        course.subscriptionCount -= 1
      }
    }
    // 同时更新热门课程列表中的状态
    const hotCourse = hotCourses.value.find((c) => c.id === courseId)
    if (hotCourse) {
      hotCourse.subscribed = false
      if (hotCourse.subscriptionCount && hotCourse.subscriptionCount > 0) {
        hotCourse.subscriptionCount -= 1
      }
    }
  } catch (error) {
    console.error('取消订阅失败:', error)
  }
}

/**
 * 打开创建对话框
 */
const openCreateDialog = () => {
  createDialog.value = true
}

/**
 * 使用 useMutation 创建课程
 */
const { execute: executeCreateCourse, loading: _creatingCourse } = useMutation(
  (payload: CreateCourseRequest) => courseApi.createCourse(payload),
  {
    successMessage: '课程创建成功',
    onSuccess: () => {
      createDialog.value = false
      resetCreateForm.value = true
      setTimeout(() => {
        resetCreateForm.value = false
      }, 100)
      // 刷新课程列表
      if (currentCategory.value) {
        void refreshCourses()
      }
    },
  }
)

/**
 * 处理创建课程提交
 */
const handleCreateCourse = async (courseData: CreateCourseRequest) => {
  await executeCreateCourse(courseData)
}
</script>

<style scoped>
.course-list-page {
  /* 使用 DefaultLayout 的默认 padding */
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

/* 搜索输入框 */
.search-input {
  border-radius: 12px;
  width: 100%;
}

@media (min-width: 600px) {
  .search-input {
    width: clamp(280px, 40vw, 600px);
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
