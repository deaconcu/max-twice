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

// 类型定义
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
          <div class="course-header mb-4">
            <v-card>
              <v-card-title>课程列表</v-card-title>
              <v-card-text>
                <v-tabs v-model="selectedNavTab">
                  <v-tab value="courses">全部课程</v-tab>
                  <v-tab value="hot">热门课程</v-tab>
                </v-tabs>
                <v-btn 
                  color="primary" 
                  @click="handleOpenCreateDialog"
                  class="mt-2"
                >
                  创建课程
                </v-btn>
              </v-card-text>
            </v-card>
          </div>

          <!-- 分类卡片列表 -->
          <div v-for="(category, categoryIndex) in config.courses" :key="categoryIndex" class="mb-4">
            <v-card>
              <v-card-title 
                @click="handleToggleFirstLevel(categoryIndex)"
                class="cursor-pointer"
                :style="{ backgroundColor: category.color }"
              >
                <v-icon v-if="category.icon" class="mr-2">{{ category.icon }}</v-icon>
                {{ category.name }}
                <v-spacer />
                <v-icon>
                  {{ activeFirstLvl === categoryIndex ? 'mdi-chevron-up' : 'mdi-chevron-down' }}
                </v-icon>
              </v-card-title>
              
              <v-expand-transition>
                <div v-show="activeFirstLvl === categoryIndex">
                  <v-card-text>
                    <!-- 子分类选择 -->
                    <v-chip-group
                      v-model="selected[categoryIndex]"
                      @update:model-value="(value) => handleSelectSubCategory(categoryIndex, value)"
                      mandatory
                      class="mb-3"
                    >
                      <v-chip
                        v-for="(subCategory, subIndex) in category.list"
                        :key="subIndex"
                        :value="subIndex"
                        filter
                      >
                        {{ subCategory.name }}
                      </v-chip>
                    </v-chip-group>

                    <!-- 课程列表 -->
                    <div v-if="loading" class="text-center py-4">
                      <v-progress-circular indeterminate color="primary" />
                    </div>
                    
                    <v-row v-else-if="courses.length > 0">
                      <v-col
                        v-for="course in courses"
                        :key="course.id"
                        cols="12"
                        md="6"
                        lg="4"
                      >
                        <v-card 
                          @click="handleOpenCourse(course.id)"
                          class="cursor-pointer hover-card"
                          height="200"
                        >
                          <v-card-title class="text-h6">{{ course.name }}</v-card-title>
                          <v-card-text>
                            <p class="text-body-2">{{ course.description }}</p>
                            <v-chip size="small" color="primary" class="mt-2">
                              难度: 初级
                            </v-chip>
                          </v-card-text>
                        </v-card>
                      </v-col>
                    </v-row>
                    
                    <div v-else class="text-center py-4 text-grey">
                      暂无课程数据
                    </div>
                  </v-card-text>
                </div>
              </v-expand-transition>
            </v-card>
          </div>

          <!-- 课程创建对话框 -->
          <v-dialog v-model="applyCourseDialog" max-width="600px">
            <v-card>
              <v-card-title>创建新课程</v-card-title>
              <v-card-text>
                <p>课程创建功能待完善...</p>
              </v-card-text>
              <v-card-actions>
                <v-spacer />
                <v-btn @click="applyCourseDialog = false">取消</v-btn>
                <v-btn color="primary" @click="applyCourseDialog = false">确定</v-btn>
              </v-card-actions>
            </v-card>
          </v-dialog>
        </v-col>

        <!-- 右侧边栏 -->
        <v-col cols="3">
          <v-card>
            <v-card-title>热门课程</v-card-title>
            <v-card-text>
              <div v-for="course in hotCourses" :key="course.id" class="mb-2">
                <v-chip
                  @click="handleOpenCourse(course.id)"
                  class="cursor-pointer"
                  color="primary"
                  variant="outlined"
                >
                  {{ course.name }}
                </v-chip>
                <div class="text-caption text-grey">
                  学习人数: {{ course.learnerCount }}
                </div>
              </div>
            </v-card-text>
          </v-card>
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

.cursor-pointer {
  cursor: pointer;
}

.hover-card {
  transition: transform 0.2s ease-in-out;
}

.hover-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.12);
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