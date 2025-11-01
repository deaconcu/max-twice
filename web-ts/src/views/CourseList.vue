<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import type { Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  courseServiceV1,
  subscriptionServiceV1,
  systemServiceV1,
} from '@/services/api/v1/apiServiceV1'
import { useUserStore } from '@/stores/user'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import type { Course } from '@/types/course'
import type { UserCourse } from '@/types/userCourse'
import RightSidebar from '@/components/common/RightSidebar.vue'
import CourseHeader from '@/components/course/CourseHeader.vue'
import CourseCreateDialog from '@/components/course/CourseCreateDialog.vue'
import CourseCategoryCard from '@/components/course/CourseCategoryCard.vue'


// 扩展类型，保持现有的嵌套结构
interface Category {
  id: number
  name: string
  icon?: string
  color?: string
}

interface SubCategory {
  id: number
  name: string
  description?: string
}

interface CategoryWithSubs extends Category {
  list: SubCategory[]
}

interface SystemConfig {
  courses: CategoryWithSubs[]
}

interface CourseCreateData {
  name: string
  description: string
  mainCategory: number
  subCategory: number
}

const { t } = useI18n()
const showSnackbar = inject('showSnackbar') as (message: string, type?: string) => void
const user = useUserStore()
const router = useRouter()

// 响应式数据
const selected: Ref<number[]> = ref([])
const selectedNavTab: Ref<string> = ref('courses')
const activeFirstLvl: Ref<number> = ref(-1)
const applyCourseDialog: Ref<boolean> = ref(false)
const resetFormFlag: Ref<boolean> = ref(false)

// 当前分类查询参数
const currentCategory = ref<{ mainCategory: number; subCategory: number } | null>(null)

// 使用 useFetch 加载课程分类
const {
  data: categoriesData,
  loading: loadingCategories,
  refresh: loadSystem
} = useFetch({
  fetchFn: systemServiceV1.getCourseCategories,
  immediate: true,
  defaultValue: { mainCategories: [], categoryMapping: [] },
  onSuccess: (data) => {
    console.log('课程分类API响应:', data)
    const { mainCategories, categoryMapping } = data

    if (!mainCategories || !categoryMapping) {
      console.error('缺少主分类或分类映射数据')
      return
    }

    const coursesConfig = mainCategories.map((mainCategory: Category) => {
      const mapping = categoryMapping.find((m: any) => m.mainCategoryId === mainCategory.id)
      return {
        id: mainCategory.id,
        name: mainCategory.name,
        icon: mainCategory.icon,
        color: mainCategory.color,
        list: mapping ? mapping.subCategories : [],
      }
    })

    config.value = { courses: coursesConfig }
    selected.value = new Array(coursesConfig.length).fill(-1)
  },
  onError: (error) => {
    console.error('Error loading course categories:', error)
  }
})

// 配置数据
const config: Ref<SystemConfig> = ref({ courses: [] })

// 使用 useFetch 加载热门课程
const { data: hotCourses, loading: loadingHotCourses } = useFetch<Course[]>({
  fetchFn: courseServiceV1.getHotCourses,
  immediate: true,
  defaultValue: []
})

// 使用 useFetch 加载用户订阅
const {
  data: subscriptions,
  loading: loadingSubscriptions,
  refresh: loadSubscription
} = useFetch<UserCourse[]>({
  fetchFn: async () => {
    const userId = user.currentUser?.id
    if (!userId) {
      return { code: 200, data: [], message: '' }
    }
    return subscriptionServiceV1.getUserSubscriptions(userId)
  },
  immediate: true,
  defaultValue: [],
  onError: (error) => {
    console.error('Error get subscription:', error)
  }
})

// 使用 useFetch 加载分类下的课程
const {
  data: courses,
  loading,
  refresh: refreshCourses
} = useFetch<Course[]>({
  fetchFn: async () => {
    if (!currentCategory.value) {
      return { code: 200, data: [], message: '' }
    }
    const { mainCategory, subCategory } = currentCategory.value
    return courseServiceV1.getCoursesByCategory(mainCategory, subCategory)
  },
  immediate: false,
  defaultValue: [],
  onError: (error) => {
    console.error('Error loading courses:', error)
  }
})

// 加载分类下的课程
const loadCoursesByCategory = async (mainCategory: number, subCategory: number): Promise<void> => {
  currentCategory.value = { mainCategory, subCategory }
  await refreshCourses()
}

// 监听分类选择变化，加载对应课程
watch(
  [() => activeFirstLvl.value, () => selected.value],
  async ([newFirstLvl, newSelected]) => {
    if (newFirstLvl >= 0 && newSelected[newFirstLvl] >= 0 && config.value.courses) {
      const mainCategory = config.value.courses[newFirstLvl]
      const subCategory = mainCategory.list[newSelected[newFirstLvl]]

      if (mainCategory && subCategory) {
        await loadCoursesByCategory(mainCategory.id, subCategory.id)
      }
    }
  },
  { deep: true }
)

// 使用 useMutation 处理课程创建
const { execute: createCourse, loading: creatingCourse } = useMutation(
  courseServiceV1.createCourse,
  {
    successMessage: t('message.courseCreateSuccess'),
    onSuccess: () => {
      applyCourseDialog.value = false
      resetFormFlag.value = true
      setTimeout(() => {
        resetFormFlag.value = false
      }, 100)
    },
    onError: (error) => {
      console.error('Error creating course:', error)
    }
  }
)

// 事件处理函数
const handleSearch = (): void => {
  console.log('search')
}

const handleOpenCreateDialog = (): void => {
  applyCourseDialog.value = true
}

const handleCreateCourse = async (courseData: CourseCreateData): Promise<void> => {
  // 验证必填字段
  if (!courseData.name.trim()) {
    showSnackbar(t('validation.required.courseName'), 'error')
    return
  }
  if (!courseData.description.trim()) {
    showSnackbar(t('validation.required.courseDescription'), 'error')
    return
  }
  if (!courseData.mainCategory) {
    showSnackbar(t('validation.required.mainCategory'), 'error')
    return
  }
  if (!courseData.subCategory) {
    showSnackbar(t('validation.required.subCategory'), 'error')
    return
  }

  console.log('Creating course:', courseData)
  await createCourse(courseData)
}

const handleToggleFirstLevel = (firstIndex: number): void => {
  activeFirstLvl.value = activeFirstLvl.value === firstIndex ? -1 : firstIndex
  selected.value.fill(-1)
}

const handleSelectSubCategory = (firstIndex: number, secondIndex: number): void => {
  selected.value[firstIndex] = secondIndex
}

const handleOpenCourse = (courseId: number): void => {
  const url = router.resolve({ path: '/read', query: { courseId: courseId.toString() } }).href
  window.open(url, '_blank')
}
</script>

<template>
  <Suspense>
    <v-container fluid>
      <v-row class="mt-2">
        <!-- 主内容区域 -->
        <v-col cols="9" class="pr-8">
          <!-- 页面头部 -->
          <CourseHeader
            v-model:selected-nav-tab="selectedNavTab"
            @search="handleSearch"
            @open-create-dialog="handleOpenCreateDialog"
          />

          <!-- 分类卡片列表 -->
          <CourseCategoryCard
            v-for="(category, categoryIndex) in config.courses"
            :key="categoryIndex"
            :category="category"
            :category-index="categoryIndex"
            :active-first-lvl="activeFirstLvl"
            :selected-sub-category="selected[categoryIndex]"
            :courses="courses"
            :loading="loading"
            @toggle-first-level="handleToggleFirstLevel"
            @select-sub-category="handleSelectSubCategory"
            @open-course="handleOpenCourse"
          />

          <!-- 课程创建对话框 -->
          <CourseCreateDialog
            v-model="applyCourseDialog"
            :categories="config.courses || []"
            :reset-form="resetFormFlag"
            @submit="handleCreateCourse"
          />
        </v-col>

        <!-- 右侧边栏 -->
        <v-col cols="3">
          <RightSidebar />
        </v-col>
      </v-row>
    </v-container>
  </Suspense>
</template>

<style scoped>
/* 改善字体渲染和清晰度 */
* {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* 增强文字对比度和清晰度 */
.text-grey-darken-1,
.text-grey-darken-2,
.text-grey-darken-3,
.text-grey-darken-4 {
  font-weight: 500 !important;
}

/* 确保主要文字有足够的对比度 */
h1,
h2,
h3,
h4,
h5,
h6 {
  font-weight: 700 !important;
  letter-spacing: -0.01em;
}

/* 自定义滚动条样式 */
:deep(.v-responsive) {
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.1) transparent;
}

:deep(.v-responsive)::-webkit-scrollbar {
  width: 4px;
}

:deep(.v-responsive)::-webkit-scrollbar-track {
  background: transparent;
}

:deep(.v-responsive)::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}
</style>