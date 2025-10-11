<script setup lang="ts">
import { inject, onMounted, ref, watch } from 'vue'
import type { Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  courseServiceV1,
  subscriptionServiceV1,
  systemServiceV1,
} from '@/services/api/v1/apiServiceV1'
import { useUserStore } from '@/stores/user'
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
const config: Ref<SystemConfig> = ref({ courses: [] })
const selected: Ref<number[]> = ref([])
const selectedNavTab: Ref<string> = ref('courses')
const courses: Ref<Course[]> = ref([])
const loading: Ref<boolean> = ref(false)
const activeFirstLvl: Ref<number> = ref(-1)
const applyCourseDialog: Ref<boolean> = ref(false)
const resetFormFlag: Ref<boolean> = ref(false)

// 其他数据
const subscriptions: Ref<UserCourse[]> = ref([])
const hotCourses: Ref<Course[]> = ref([])

onMounted(() => {
  loadSystem()
  loadSubscription()
  loadHotCourses()
})

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

// 数据加载函数
const loadSystem = async (): Promise<void> => {
  try {
    console.log('开始加载课程分类...')
    const response = await systemServiceV1.getCourseCategories()
    console.log('课程分类API响应:', response)

    if (response.code === 401) {
      console.log('not login')
    } else if (response.code === 200) {
      const { mainCategories, categoryMapping } = response.data

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
    } else {
      console.error('API返回错误:', response)
    }
  } catch (error) {
    console.error('Error loading course categories:', error)
  }
}

const loadCoursesByCategory = async (mainCategory: number, subCategory: number): Promise<void> => {
  try {
    loading.value = true
    const response = await courseServiceV1.getCoursesByCategory(mainCategory, subCategory)

    if (response.code === 200) {
      courses.value = response.data || []
    } else {
      console.error('Failed to load courses:', response)
      courses.value = []
    }
  } catch (error) {
    console.error('Error loading courses:', error)
    courses.value = []
  } finally {
    loading.value = false
  }
}

const loadSubscription = async (): Promise<void> => {
  try {
    const { userId } = user
    if (userId) {
      const response = await subscriptionServiceV1.getUserSubscriptions(userId)

      if (response.code === 200) {
        subscriptions.value = response.data || []
      } else {
        subscriptions.value = []
      }
    } else {
      subscriptions.value = []
    }
  } catch (error) {
    console.error('Error get subscription:', error)
    subscriptions.value = []
  }
}

const loadHotCourses = async (): Promise<void> => {
  try {
    const response = await courseServiceV1.getHotCourses()

    if (response.code === 200) {
      hotCourses.value = response.data
    }
  } catch (error) {
    console.error('Error get hot courses:', error)
    hotCourses.value = [
      { id: 1, name: '数据结构与算法', learnerCount: 15534, subscriptionCount: 8900 },
      { id: 2, name: '英语写作', learnerCount: 20001, subscriptionCount: 7200 },
      { id: 3, name: '计算机网络', learnerCount: 6888, subscriptionCount: 9100 },
      { id: 4, name: '人工智能导论', learnerCount: 12230, subscriptionCount: 5800 },
      { id: 5, name: '法律基础', learnerCount: 18910, subscriptionCount: 4500 },
    ]
  }
}

// 事件处理函数
const handleSearch = (): void => {
  console.log('search')
}

const handleOpenCreateDialog = (): void => {
  applyCourseDialog.value = true
}

const handleCreateCourse = async (courseData: CourseCreateData): Promise<void> => {
  try {
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
    const response = await courseServiceV1.createCourse(courseData)

    if (response.code === 401) {
      showSnackbar('请先登录！', 'error')
    } else if (response.code === 200) {
      console.log('Course created successfully')
      applyCourseDialog.value = false
      resetFormFlag.value = true
      setTimeout(() => {
        resetFormFlag.value = false
      }, 100)
      showSnackbar(t('message.courseCreateSuccess'))
    } else {
      showSnackbar(response.message || '创建失败，请重试！', 'error')
    }
  } catch (error) {
    console.error('Error creating course:', error)
    showSnackbar('创建失败，请重试！', 'error')
  }
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