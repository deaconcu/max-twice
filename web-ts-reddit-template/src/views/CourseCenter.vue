<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import CourseFilter from '@/components/course/CourseFilter.vue'
import CourseCategoryCard from '@/components/course/CourseCategoryCard.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'
import type {
  CourseWithDisplay,
  CourseCategory,
  CategoryMapping,
  CourseApplication,
  Subcategory
} from '@/types/profession'

const router = useRouter()

// 状态管理
const loading = ref(false)
const searchText = ref('')
const activeFirstLvl = ref(-1)
const activeSecondLvl = ref(-1)
const showApplicationDialog = ref(false)
const applicationValid = ref(false)
const submitting = ref(false)

// Mock 数据 - 分类
const categories = ref<CourseCategory[]>([
  { id: 1, title: '计算机科学', icon: 'mdi-laptop', order: 1 },
  { id: 2, title: '数学与统计', icon: 'mdi-function-variant', order: 2 },
  { id: 3, title: '语言学习', icon: 'mdi-translate', order: 3 },
  { id: 4, title: '商业管理', icon: 'mdi-briefcase', order: 4 },
  { id: 5, title: '设计艺术', icon: 'mdi-palette', order: 5 }
])

// Mock 数据 - 分类映射
const categoryMapping = ref<CategoryMapping[]>([
  {
    mainCategoryId: 1,
    subcategories: [
      { id: 101, name: '编程基础', order: 1 },
      { id: 102, name: '数据结构与算法', order: 2 },
      { id: 103, name: '数据库', order: 3 },
      { id: 104, name: '人工智能', order: 4 },
      { id: 105, name: '网络安全', order: 5 }
    ]
  },
  {
    mainCategoryId: 2,
    subcategories: [
      { id: 201, name: '高等数学', order: 1 },
      { id: 202, name: '线性代数', order: 2 },
      { id: 203, name: '概率论', order: 3 },
      { id: 204, name: '统计学', order: 4 }
    ]
  },
  {
    mainCategoryId: 3,
    subcategories: [
      { id: 301, name: '英语', order: 1 },
      { id: 302, name: '日语', order: 2 },
      { id: 303, name: '韩语', order: 3 },
      { id: 304, name: '法语', order: 4 }
    ]
  },
  {
    mainCategoryId: 4,
    subcategories: [
      { id: 401, name: '市场营销', order: 1 },
      { id: 402, name: '战略管理', order: 2 },
      { id: 403, name: '财务管理', order: 3 },
      { id: 404, name: '人力资源', order: 4 }
    ]
  },
  {
    mainCategoryId: 5,
    subcategories: [
      { id: 501, name: '平面设计', order: 1 },
      { id: 502, name: 'UI/UX设计', order: 2 },
      { id: 503, name: '插画', order: 3 },
      { id: 504, name: '摄影', order: 4 }
    ]
  }
])

// Mock 数据 - 课程列表
const allCourses = ref<CourseWithDisplay[]>([
  // 计算机科学 - 编程基础
  {
    id: 1,
    name: 'Python编程入门',
    description: '从零开始学习Python编程，掌握基础语法和编程思维',
    mainCategory: 1,
    subCategory: 101,
    learnerCount: 15234,
    icon: 'mdi-language-python',
    iconColor: 'blue'
  },
  {
    id: 2,
    name: 'Java核心技术',
    description: '深入学习Java核心技术，面向对象编程和企业级开发',
    mainCategory: 1,
    subCategory: 101,
    learnerCount: 12890,
    icon: 'mdi-language-java',
    iconColor: 'orange'
  },
  {
    id: 3,
    name: 'JavaScript从入门到精通',
    description: '全面掌握JavaScript，包括ES6+新特性和实战项目',
    mainCategory: 1,
    subCategory: 101,
    learnerCount: 18765,
    icon: 'mdi-language-javascript',
    iconColor: 'yellow'
  },
  // 计算机科学 - 数据结构与算法
  {
    id: 4,
    name: '数据结构与算法精讲',
    description: '系统学习常用数据结构和算法，提升编程能力',
    mainCategory: 1,
    subCategory: 102,
    learnerCount: 9876,
    icon: 'mdi-chart-tree',
    iconColor: 'green'
  },
  {
    id: 5,
    name: '算法竞赛入门',
    description: '学习算法竞赛常用技巧，提高解题能力',
    mainCategory: 1,
    subCategory: 102,
    learnerCount: 6543,
    icon: 'mdi-trophy',
    iconColor: 'purple'
  },
  // 计算机科学 - 数据库
  {
    id: 6,
    name: 'MySQL数据库设计',
    description: '学习MySQL数据库设计原理和性能优化',
    mainCategory: 1,
    subCategory: 103,
    learnerCount: 11234,
    icon: 'mdi-database',
    iconColor: 'teal'
  },
  {
    id: 7,
    name: 'MongoDB实战',
    description: 'NoSQL数据库MongoDB的实战应用',
    mainCategory: 1,
    subCategory: 103,
    learnerCount: 7890,
    icon: 'mdi-leaf',
    iconColor: 'green'
  },
  // 计算机科学 - 人工智能
  {
    id: 8,
    name: '机器学习入门',
    description: '学习机器学习基础理论和常用算法',
    mainCategory: 1,
    subCategory: 104,
    learnerCount: 13456,
    icon: 'mdi-brain',
    iconColor: 'purple'
  },
  {
    id: 9,
    name: '深度学习与神经网络',
    description: '深入学习神经网络和深度学习框架',
    mainCategory: 1,
    subCategory: 104,
    learnerCount: 10234,
    icon: 'mdi-chip',
    iconColor: 'indigo'
  },
  // 数学与统计 - 高等数学
  {
    id: 10,
    name: '微积分基础',
    description: '系统学习微积分的基本概念和应用',
    mainCategory: 2,
    subCategory: 201,
    learnerCount: 8765,
    icon: 'mdi-function',
    iconColor: 'blue'
  },
  {
    id: 11,
    name: '数学分析',
    description: '深入学习数学分析的理论和方法',
    mainCategory: 2,
    subCategory: 201,
    learnerCount: 5432,
    icon: 'mdi-math-integral',
    iconColor: 'cyan'
  },
  // 数学与统计 - 线性代数
  {
    id: 12,
    name: '线性代数及其应用',
    description: '学习线性代数基础和在各领域的应用',
    mainCategory: 2,
    subCategory: 202,
    learnerCount: 9234,
    icon: 'mdi-matrix',
    iconColor: 'teal'
  },
  // 语言学习 - 英语
  {
    id: 13,
    name: '英语语法精讲',
    description: '系统学习英语语法规则和用法',
    mainCategory: 3,
    subCategory: 301,
    learnerCount: 20123,
    icon: 'mdi-book-alphabet',
    iconColor: 'red'
  },
  {
    id: 14,
    name: '商务英语实战',
    description: '学习商务场景下的英语表达和写作',
    mainCategory: 3,
    subCategory: 301,
    learnerCount: 12345,
    icon: 'mdi-briefcase-account',
    iconColor: 'orange'
  },
  {
    id: 15,
    name: 'IELTS备考冲刺',
    description: '雅思考试全面备考和技巧训练',
    mainCategory: 3,
    subCategory: 301,
    learnerCount: 15678,
    icon: 'mdi-certificate',
    iconColor: 'green'
  },
  // 商业管理 - 市场营销
  {
    id: 16,
    name: '市场营销原理',
    description: '学习市场营销的基本原理和策略',
    mainCategory: 4,
    subCategory: 401,
    learnerCount: 11567,
    icon: 'mdi-bullhorn',
    iconColor: 'pink'
  },
  {
    id: 17,
    name: '数字营销实战',
    description: '掌握数字时代的营销方法和工具',
    mainCategory: 4,
    subCategory: 401,
    learnerCount: 9876,
    icon: 'mdi-web',
    iconColor: 'purple'
  },
  // 设计艺术 - UI/UX设计
  {
    id: 18,
    name: 'UI设计基础',
    description: '学习用户界面设计的基本原则和技巧',
    mainCategory: 5,
    subCategory: 502,
    learnerCount: 13890,
    icon: 'mdi-cellphone',
    iconColor: 'blue'
  },
  {
    id: 19,
    name: 'UX用户体验设计',
    description: '深入学习用户体验设计方法和流程',
    mainCategory: 5,
    subCategory: 502,
    learnerCount: 10234,
    icon: 'mdi-account-heart',
    iconColor: 'pink'
  },
  {
    id: 20,
    name: 'Figma设计实战',
    description: '使用Figma进行界面设计的完整实战',
    mainCategory: 5,
    subCategory: 502,
    learnerCount: 14567,
    icon: 'mdi-vector-square',
    iconColor: 'purple'
  }
])

const displayedCourses = ref<CourseWithDisplay[]>([])

// 课程申请表单
const newCourseApplication = ref<CourseApplication>({
  name: '',
  description: '',
  mainCategory: null,
  subCategory: null
})

// 搜索和筛选
const performSearch = (): void => {
  if (!searchText.value.trim()) {
    displayedCourses.value = []
    return
  }

  const searchLower = searchText.value.toLowerCase()
  displayedCourses.value = allCourses.value.filter(
    (course) =>
      course.name?.toLowerCase().includes(searchLower) ||
      course.description?.toLowerCase().includes(searchLower)
  )
}

// 选择一级分类
const selectFirstLevel = (categoryIndex: number): void => {
  if (activeFirstLvl.value === categoryIndex) {
    activeFirstLvl.value = -1
    activeSecondLvl.value = -1
    displayedCourses.value = []
    return
  }

  activeFirstLvl.value = categoryIndex
  activeSecondLvl.value = -1
  displayedCourses.value = []
}

// 选择二级分类
const selectSecondLevel = (subcategoryIndex: number): void => {
  if (activeSecondLvl.value === subcategoryIndex) {
    activeSecondLvl.value = -1
    displayedCourses.value = []
    return
  }

  activeSecondLvl.value = subcategoryIndex

  const category = categories.value[activeFirstLvl.value]
  const mapping = categoryMapping.value.find((m) => m.mainCategoryId === category.id)
  const subCategory = mapping?.subcategories[subcategoryIndex]

  if (subCategory) {
    loading.value = true
    setTimeout(() => {
      displayedCourses.value = allCourses.value.filter(
        (c) => c.subCategory === subCategory.id
      )
      loading.value = false
    }, 300)
  }
}

// 打开课程
const goToCourse = (course: CourseWithDisplay): void => {
  console.log('打开课程:', course.name)
  // router.push(`/course/${course.id}`)
}

// 打开申请对话框
const openCourseApplicationDialog = (): void => {
  showApplicationDialog.value = true
}

// 关闭申请对话框
const closeApplicationDialog = (): void => {
  showApplicationDialog.value = false
  newCourseApplication.value = {
    name: '',
    description: '',
    mainCategory: null,
    subCategory: null
  }
}

// 提交课程申请
const submitCourseApplication = (): void => {
  console.log('提交课程申请:', newCourseApplication.value)
  closeApplicationDialog()
}

// 获取子分类
const getSubcategoriesByMainCategory = (mainCategoryId: number | null): Subcategory[] => {
  if (!mainCategoryId) return []
  const mapping = categoryMapping.value.find((m) => m.mainCategoryId === mainCategoryId)
  return mapping?.subcategories || []
}
</script>

<template>
  <div class="course-center-page">
    <AppHeader />

    <v-container fluid class="page-content">
      <v-row class="mt-2 fill-height">
        <!-- 左侧导航栏 -->
        <v-col cols="auto" class="d-none d-lg-block pa-0">
          <LeftSidebar />
        </v-col>

        <!-- 主内容区 -->
        <v-col class="main-content pl-6">
          <!-- 页面标题 -->
          <div class="mb-6">
            <div class="d-flex align-center mb-2">
              <v-avatar color="teal-lighten-4" size="56" class="mr-3">
                <v-icon icon="mdi-book-multiple" color="teal" size="28"></v-icon>
              </v-avatar>
              <div>
                <h1 class="text-h4 font-weight-bold text-grey-darken-4">课程中心</h1>
                <p class="text-body-2 text-grey-darken-2 mt-1">探索知识，成就未来</p>
              </div>
            </div>
          </div>

          <!-- 搜索和筛选 -->
          <CourseFilter
            v-model:search-text="searchText"
            @perform-search="performSearch"
            @open-course-application="openCourseApplicationDialog"
          />

          <!-- 分类卡片列表 -->
          <CourseCategoryCard
            v-for="(category, index) in categories"
            :key="category.id"
            :category="category"
            :category-index="index"
            :category-mapping="categoryMapping"
            :active-first-lvl="activeFirstLvl"
            :active-second-lvl="activeSecondLvl"
            :courses="displayedCourses"
            :loading="loading"
            @toggle-first-level="selectFirstLevel"
            @select-second-level="selectSecondLevel"
            @open-course="goToCourse"
          />
        </v-col>
      </v-row>
    </v-container>

    <!-- 课程申请对话框 -->
    <v-dialog v-model="showApplicationDialog" max-width="600px" persistent>
      <v-card rounded="xl" border>
        <v-card-title class="pa-6">
          <div class="d-flex align-center">
            <v-icon icon="mdi-plus-circle" color="primary" size="32" class="mr-3"></v-icon>
            <span class="text-h6 font-weight-bold">申请新课程</span>
          </div>
        </v-card-title>

        <v-card-text class="px-6 pb-0">
          <v-form v-model="applicationValid">
            <v-text-field
              v-model="newCourseApplication.name"
              label="课程名称"
              variant="outlined"
              clearable
              required
              class="mb-4"
            ></v-text-field>

            <v-textarea
              v-model="newCourseApplication.description"
              label="课程描述"
              variant="outlined"
              clearable
              required
              rows="3"
              class="mb-4"
            ></v-textarea>

            <v-select
              v-model="newCourseApplication.mainCategory"
              :items="categories"
              item-title="title"
              item-value="id"
              label="主分类"
              variant="outlined"
              class="mb-4"
              clearable
              required
            ></v-select>

            <v-select
              v-model="newCourseApplication.subCategory"
              :items="getSubcategoriesByMainCategory(newCourseApplication.mainCategory)"
              item-title="name"
              item-value="id"
              label="子分类"
              variant="outlined"
              class="mb-4"
              :disabled="!newCourseApplication.mainCategory"
              clearable
              required
            ></v-select>
          </v-form>
        </v-card-text>

        <v-card-actions class="px-6 pb-6">
          <v-spacer></v-spacer>
          <v-btn
            variant="outlined"
            rounded="lg"
            :disabled="submitting"
            @click="closeApplicationDialog"
          >
            取消
          </v-btn>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            :disabled="!applicationValid || submitting"
            :loading="submitting"
            @click="submitCourseApplication"
          >
            提交申请
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <AppFooter />
  </div>
</template>

<style scoped>
.course-center-page {
  min-height: 100vh;
  background-color: #FAFBFC;
}

.page-content {
  position: relative;
  z-index: 1;
  max-width: 100%;
}

.main-content {
  padding-left: 24px;
  padding-right: 20px;
  flex: 1;
}

/* 移动端 */
@media (max-width: 1280px) {
  .main-content {
    padding-left: 20px;
  }
}
</style>
