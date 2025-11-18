<template>
  <DefaultLayout>
    <!-- 页面标题和搜索栏 -->
    <div class="page-header mb-10">
      <div class="d-flex align-end justify-space-between">
        <!-- 左侧：标题 -->
        <div class="d-flex align-center">
          <v-avatar color="grey-lighten-3" size="64" rounded="lg" class="mr-3">
            <v-icon icon="mdi-book-multiple" size="32" color="grey-darken-1" />
          </v-avatar>
          <div>
            <h1 class="text-h4 font-weight-bold text-grey-darken-4">{{ t('course.center') }}</h1>
            <p class="text-body-2 text-grey-darken-2 mt-1">探索知识，成就未来</p>
          </div>
        </div>

        <!-- 右侧：搜索栏 -->
        <div class="d-flex align-center search-container">
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
        </div>
      </div>
    </div>

    <!-- 分类导航和课程网格 -->
    <v-row>
      <v-col class="pr-12">
        <!-- 分类导航 -->
        <CourseFilter
          v-model:main-category="selectedMainCategory"
          v-model:sub-category="selectedSubCategory"
          :categories="categories"
          :sub-categories="subCategories"
          @change="handleFilterChange"
        />

        <!-- 加载状态 -->
        <div v-if="loading" class="text-center py-12">
          <v-progress-circular indeterminate color="primary" size="64" />
          <p class="text-body-1 text-grey-darken-2 mt-4">{{ t('common.loading') }}</p>
        </div>

        <!-- 提示：请选择分类 -->
        <div
          v-else-if="!selectedMainCategory && !selectedSubCategory && !searchText"
          class="text-center py-12"
        >
          <v-card rounded="lg" class="pa-12 empty-state no-border">
            <v-icon icon="mdi-filter" size="80" color="grey-lighten-1" class="mb-4" />
            <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">请选择分类</h3>
            <p class="text-body-1 text-grey-darken-1">请从上方选择主分类和子分类来查看课程</p>
          </v-card>
        </div>

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
          <div v-if="selectedMainCategory || selectedSubCategory || searchText" class="mb-4 mt-10">
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
                  <span v-else>{{ getCategoryName(selectedMainCategory) }}</span>
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
          <div v-if="hasMore" ref="loadMoreTrigger" class="text-center mt-6 py-4 mb-16">
            <v-progress-circular v-if="loadingMore" indeterminate color="primary" size="32" />
          </div>
        </div>
      </v-col>

      <!-- 右侧热门课程栏 -->
      <v-col class="right-sidebar">
        <div class="sticky-wrapper">
          <!-- 创建课程按钮 -->
          <v-btn
            color="grey-lighten-4"
            variant="flat"
            block
            rounded="lg"
            size="large"
            class="mb-4 create-course-btn"
            prepend-icon="mdi-plus-circle"
            @click="openCreateDialog"
          >
            {{ t('course.createNew') }}
          </v-btn>

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
      </v-col>
    </v-row>

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
import { courseApi, subscriptionApi, systemApi } from '@/api'
import type { Course, CreateCourseRequest } from '@/types/course'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import CourseCard from '@/components/features/course/CourseCard.vue'
import CourseFilter from '@/components/features/course/CourseFilter.vue'
import CourseCreateDialog from '@/components/features/course/CourseCreateDialog.vue'

const router = useRouter()
const { t } = useI18n()

// 状态管理
const loadingMore = ref(false)
const searchText = ref('')
const selectedMainCategory = ref<number | undefined>()
const selectedSubCategory = ref<number | undefined>()
const createDialog = ref(false)
const resetCreateForm = ref(false)
const loadMoreTrigger = ref<HTMLElement | null>(null)

// 当前分类查询参数
const currentCategory = ref<{ mainCategory: number; subCategory: number } | null>(null)

// 使用 useFetch 加载课程分类
const { data: categoriesData, loading: _loadingCategories } = useFetch({
  fetchFn: () => systemApi.getCourseCategories(),
  immediate: true,
  defaultValue: { mainCategories: [], categoryMapping: [] },
})

// 使用 useFetch 加载热门课程
const { data: hotCoursesData, loading: _loadingHotCourses } = useFetch<Course[]>({
  fetchFn: () => courseApi.getHotCourses(),
  immediate: true,
  defaultValue: [],
})

// 使用 useFetch 加载分类下的课程
const {
  data: coursesData,
  loading,
  refresh: refreshCourses,
} = useFetch<Course[]>({
  fetchFn: async () => {
    if (!currentCategory.value) {
      return { code: 200, data: [], message: '' }
    }
    const { mainCategory, subCategory } = currentCategory.value
    return courseApi.getCoursesByCategory(mainCategory, subCategory)
  },
  immediate: false,
  defaultValue: [],
})

// 计算属性 - 从 useFetch 数据中提取
const categories = computed(() => {
  const data = categoriesData.value
  if (!data?.mainCategories) return []
  return data.mainCategories as { id: number; name: string; icon?: string }[]
})

const subCategories = computed(() => {
  const data = categoriesData.value
  if (!data?.categoryMapping) return []

  const allSubCategories: { id: number; name: string; mainCategoryId: number }[] = []
  const categoryMapping = data.categoryMapping as {
    mainCategoryId: number
    subCategories: { id: number; name: string }[]
  }[]

  categoryMapping.forEach((mapping) => {
    mapping.subCategories.forEach((sub) => {
      allSubCategories.push({
        id: sub.id,
        name: sub.name,
        mainCategoryId: mapping.mainCategoryId,
      })
    })
  })

  return allSubCategories
})

const courses = computed(() => coursesData.value ?? [])
const hotCourses = computed(() => hotCoursesData.value ?? [])

// 分页
const pageSize = 12
const currentPage = ref(1)

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

// 显示的课程（分页）
const displayedCourses = computed(() => {
  return filteredCourses.value.slice(0, currentPage.value * pageSize)
})

// 是否还有更多
const hasMore = computed(() => {
  return displayedCourses.value.length < filteredCourses.value.length
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
  currentPage.value = 1
  currentCategory.value = null
}

/**
 * 根据分类加载课程
 */
const loadCoursesByCategory = async (mainCategory: number, subCategory: number): Promise<void> => {
  currentCategory.value = { mainCategory, subCategory }
  await refreshCourses()
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
  currentPage.value = 1
  searchText.value = '' // 清空搜索
  if (selectedMainCategory.value && selectedSubCategory.value) {
    void loadCoursesByCategory(selectedMainCategory.value, selectedSubCategory.value)
  }
}

/**
 * 监听分类变化，自动加载课程
 */
watch([selectedMainCategory, selectedSubCategory], () => {
  if (selectedMainCategory.value && selectedSubCategory.value) {
    void loadCoursesByCategory(selectedMainCategory.value, selectedSubCategory.value)
  } else {
    currentCategory.value = null
  }
  // 重置分页并重新设置无限滚动
  currentPage.value = 1
  cleanupInfiniteScroll()
  setTimeout(setupInfiniteScroll, 100)
})

/**
 * 加载更多
 */
const loadMore = () => {
  if (loadingMore.value) return
  loadingMore.value = true
  currentPage.value += 1
  // 模拟加载延迟，实际情况下这里会立即显示数据
  setTimeout(() => {
    loadingMore.value = false
  }, 300)
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
      if (entry?.isIntersecting && hasMore.value && !loadingMore.value) {
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
onMounted(() => {
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
  margin-bottom: 24px;
}

/* 搜索容器样式 */
.search-container {
  gap: 0;
}

.search-input {
  border-radius: 12px;
  width: 600px;
}

.action-btn {
  min-width: 120px;
  height: 40px;
  font-weight: 500;
  text-transform: none;
  letter-spacing: normal;
}

.empty-state {
  background-color: #ffffff;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

/* 右侧热门课程栏 */
.right-sidebar {
  max-width: 280px;
}

.sticky-wrapper {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  display: flex;
  flex-direction: column;
}

.create-course-btn {
  font-weight: 600;
  text-transform: none;
  letter-spacing: 0.3px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.12);
  transition: all 0.25s ease;
}

.create-course-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  background-color: rgb(var(--v-theme-grey-lighten-3));
}

.popular-card {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
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
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.popular-list::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

.popular-item {
  display: flex;
  align-items: center;
  padding: 10px 5px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background-color: #e5e5e5;
  color: #666666;
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
  color: #ffffff;
}

.popular-name {
  font-size: 14px;
  font-weight: 500;
  color: #000000;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.popular-count {
  font-size: 12px;
  color: #666666;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 移动端响应式 */
@media (max-width: 1264px) {
  .right-sidebar {
    max-width: 100%;
  }
}

@media (max-width: 960px) {
  .course-list-page {
    /* 使用 DefaultLayout 的默认 padding */
  }

  .course-grid {
    grid-template-columns: 1fr;
  }

  .sticky-card {
    position: static;
    max-height: none;
  }

  .popular-list {
    max-height: 400px;
  }
}
</style>
