<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'
import type {
  CourseWithDisplay,
  CourseCategory,
  CategoryMapping,
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

// 随机图标和颜色
const availableIcons = [
  'mdi-language-python', 'mdi-language-java', 'mdi-language-javascript',
  'mdi-chart-tree', 'mdi-database', 'mdi-brain', 'mdi-shield-lock',
  'mdi-function-variant', 'mdi-translate', 'mdi-briefcase', 'mdi-palette'
]

const availableColors = [
  'primary', 'success', 'warning', 'error', 'info', 'purple', 'indigo',
  'blue', 'cyan', 'teal', 'green', 'orange'
]

// Mock 数据 - 课程列表
const allCourses = ref<CourseWithDisplay[]>([
  // 计算机科学 - 编程基础
  {
    id: 1,
    name: 'Python编程入门',
    description: '从零开始学习Python编程，掌握基础语法和编程思维，完成实战项目。',
    mainCategory: 1,
    subCategory: 101,
    learnerCount: 15234,
    icon: availableIcons[0],
    iconColor: availableColors[0]
  },
  {
    id: 2,
    name: 'Java核心技术',
    description: '深入学习Java核心技术，面向对象编程和企业级开发实践。',
    mainCategory: 1,
    subCategory: 101,
    learnerCount: 12890,
    icon: availableIcons[1],
    iconColor: availableColors[1]
  },
  {
    id: 3,
    name: 'JavaScript从入门到精通',
    description: '全面掌握JavaScript，包括ES6+新特性和前端开发实战。',
    mainCategory: 1,
    subCategory: 101,
    learnerCount: 18765,
    icon: availableIcons[2],
    iconColor: availableColors[2]
  },
  // 计算机科学 - 数据结构与算法
  {
    id: 4,
    name: '数据结构与算法精讲',
    description: '系统学习常用数据结构和算法，提升编程能力和解题思维。',
    mainCategory: 1,
    subCategory: 102,
    learnerCount: 9876,
    icon: availableIcons[3],
    iconColor: availableColors[3]
  },
  {
    id: 5,
    name: '算法竞赛入门',
    description: '学习算法竞赛常用技巧，培养算法思维，提高解题能力。',
    mainCategory: 1,
    subCategory: 102,
    learnerCount: 6543,
    icon: availableIcons[4],
    iconColor: availableColors[4]
  },
  // 计算机科学 - 数据库
  {
    id: 6,
    name: 'MySQL数据库设计',
    description: '学习MySQL数据库设计原理、SQL语句和性能优化技巧。',
    mainCategory: 1,
    subCategory: 103,
    learnerCount: 11234,
    icon: availableIcons[5],
    iconColor: availableColors[5]
  },
  {
    id: 7,
    name: 'Redis缓存技术',
    description: '掌握Redis缓存技术，提升应用性能和数据处理能力。',
    mainCategory: 1,
    subCategory: 103,
    learnerCount: 8765,
    icon: availableIcons[6],
    iconColor: availableColors[6]
  },
  // 计算机科学 - 人工智能
  {
    id: 8,
    name: '机器学习入门',
    description: '学习机器学习基础理论和常用算法，掌握模型训练方法。',
    mainCategory: 1,
    subCategory: 104,
    learnerCount: 14567,
    icon: availableIcons[7],
    iconColor: availableColors[7]
  },
  {
    id: 9,
    name: '深度学习实战',
    description: '深入学习神经网络和深度学习技术，完成AI项目实战。',
    mainCategory: 1,
    subCategory: 104,
    learnerCount: 10234,
    icon: availableIcons[8],
    iconColor: availableColors[8]
  },
  // 计算机科学 - 网络安全
  {
    id: 10,
    name: '网络安全基础',
    description: '了解网络安全基本概念，学习常见攻击防御和安全实践。',
    mainCategory: 1,
    subCategory: 105,
    learnerCount: 7890,
    icon: availableIcons[9],
    iconColor: availableColors[9]
  },
  // 数学与统计 - 高等数学
  {
    id: 11,
    name: '高等数学基础',
    description: '系统学习微积分、极限、导数等高等数学核心内容。',
    mainCategory: 2,
    subCategory: 201,
    learnerCount: 13456,
    icon: availableIcons[10],
    iconColor: availableColors[10]
  },
  // 数学与统计 - 线性代数
  {
    id: 12,
    name: '线性代数及应用',
    description: '学习矩阵运算、线性方程组、特征值等线性代数知识。',
    mainCategory: 2,
    subCategory: 202,
    learnerCount: 9876,
    icon: availableIcons[0],
    iconColor: availableColors[11]
  },
  // 数学与统计 - 概率论
  {
    id: 13,
    name: '概率论与随机过程',
    description: '掌握概率论基础理论和随机过程的基本概念与应用。',
    mainCategory: 2,
    subCategory: 203,
    learnerCount: 8234,
    icon: availableIcons[1],
    iconColor: availableColors[0]
  },
  // 数学与统计 - 统计学
  {
    id: 14,
    name: '统计学原理与应用',
    description: '学习统计学基本原理，掌握数据分析和统计推断方法。',
    mainCategory: 2,
    subCategory: 204,
    learnerCount: 10567,
    icon: availableIcons[2],
    iconColor: availableColors[1]
  },
  // 语言学习 - 英语
  {
    id: 15,
    name: '商务英语实战',
    description: '提升商务英语沟通能力，掌握职场英语表达技巧。',
    mainCategory: 3,
    subCategory: 301,
    learnerCount: 16789,
    icon: availableIcons[3],
    iconColor: availableColors[2]
  },
  // 语言学习 - 日语
  {
    id: 16,
    name: '日语零基础入门',
    description: '从五十音开始，系统学习日语基础语法和常用表达。',
    mainCategory: 3,
    subCategory: 302,
    learnerCount: 12345,
    icon: availableIcons[4],
    iconColor: availableColors[3]
  },
  // 商业管理 - 市场营销
  {
    id: 17,
    name: '数字营销实战',
    description: '学习数字营销策略、社交媒体运营和数据分析技巧。',
    mainCategory: 4,
    subCategory: 401,
    learnerCount: 11234,
    icon: availableIcons[5],
    iconColor: availableColors[4]
  },
  // 商业管理 - 战略管理
  {
    id: 18,
    name: '企业战略管理',
    description: '掌握战略分析工具，学习企业战略规划和实施方法。',
    mainCategory: 4,
    subCategory: 402,
    learnerCount: 8765,
    icon: availableIcons[6],
    iconColor: availableColors[5]
  },
  // 设计艺术 - 平面设计
  {
    id: 19,
    name: 'Photoshop设计入门',
    description: '学习Photoshop基础操作和图像处理技巧，完成设计作品。',
    mainCategory: 5,
    subCategory: 501,
    learnerCount: 14567,
    icon: availableIcons[7],
    iconColor: availableColors[6]
  },
  // 设计艺术 - UI/UX设计
  {
    id: 20,
    name: 'UI设计实战课程',
    description: '学习UI设计原则和工具使用，掌握界面设计核心技能。',
    mainCategory: 5,
    subCategory: 502,
    learnerCount: 13456,
    icon: availableIcons[8],
    iconColor: availableColors[7]
  }
])

// 计算显示的课程
const displayedCourses = computed(() => {
  let filtered = allCourses.value

  // 搜索筛选
  if (searchText.value.trim()) {
    const searchLower = searchText.value.toLowerCase()
    filtered = filtered.filter(
      (course) =>
        course.name.toLowerCase().includes(searchLower) ||
        course.description?.toLowerCase().includes(searchLower)
    )
  }

  // 一级分类筛选
  if (activeFirstLvl.value !== -1) {
    filtered = filtered.filter((course) => course.mainCategory === activeFirstLvl.value)

    // 二级分类筛选
    if (activeSecondLvl.value !== -1) {
      filtered = filtered.filter((course) => course.subCategory === activeSecondLvl.value)
    }
  }

  return filtered
})

// 选择一级分类
const selectFirstLevel = (categoryId: number): void => {
  if (activeFirstLvl.value === categoryId) {
    activeFirstLvl.value = -1
    activeSecondLvl.value = -1
  } else {
    activeFirstLvl.value = categoryId
    activeSecondLvl.value = -1
  }
}

// 选择二级分类
const selectSecondLevel = (subCategoryId: number): void => {
  if (activeSecondLvl.value === subCategoryId) {
    activeSecondLvl.value = -1
  } else {
    activeSecondLvl.value = subCategoryId
  }
}

// 搜索
const performSearch = (): void => {
  console.log('搜索:', searchText.value)
}

// 打开课程详情
const goToCourse = (course: CourseWithDisplay): void => {
  console.log('打开课程:', course.name)
  router.push(`/read/${course.id}`)
}

// 打开申请对话框
const openCourseApplicationDialog = (): void => {
  showApplicationDialog.value = true
}

// 关闭申请对话框
const closeCourseApplicationDialog = (): void => {
  showApplicationDialog.value = false
}

// 获取分类名称
const getCategoryName = (categoryId: number): string => {
  return categories.value.find((c) => c.id === categoryId)?.title || ''
}

// 获取子分类列表
const getSubcategories = (mainCategoryId: number): Subcategory[] => {
  const mapping = categoryMapping.value.find((m) => m.mainCategoryId === mainCategoryId)
  return mapping?.subcategories || []
}

// 热门课程 - 按学习人数排序取前15个
const popularCourses = computed(() => {
  return [...allCourses.value]
    .sort((a, b) => b.learnerCount - a.learnerCount)
    .slice(0, 15)
})
</script>

<template>
  <div class="course-center-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
      <!-- 页面标题 -->
      <div class="mb-6">
        <div class="d-flex align-center">
          <v-avatar color="grey-lighten-3" size="64" rounded="lg" class="mr-3">
            <v-icon size="32" color="#666666">mdi-book-multiple</v-icon>
          </v-avatar>
          <div>
            <h1 class="text-h4 font-weight-bold text-grey-darken-4">课程中心</h1>
            <p class="text-body-2 text-grey-darken-2 mt-1">探索知识，成就未来</p>
          </div>
        </div>
      </div>

      <!-- 搜索和筛选 -->
      <v-card rounded="lg" class="mb-6 search-card no-border" flat>
        <v-card-text class="py-6 px-0">
          <div class="d-flex align-center" style="gap: 16px">
            <v-text-field
              v-model="searchText"
              placeholder="搜索课程..."
              variant="outlined"
              density="compact"
              hide-details
              clearable
              class="flex-grow-1"
              @keyup.enter="performSearch"
            >
              <template #prepend-inner>
                <v-icon icon="mdi-magnify" size="20"></v-icon>
              </template>
            </v-text-field>

            <v-btn color="primary" variant="flat" size="default" rounded="lg" @click="performSearch">
              <v-icon icon="mdi-magnify" size="18" class="mr-2"></v-icon>
              搜索
            </v-btn>

            <v-btn
              color="grey-darken-2"
              variant="outlined"
              size="default"
              rounded="lg"
              @click="openCourseApplicationDialog"
            >
              <v-icon icon="mdi-plus" size="18" class="mr-2"></v-icon>
              申请课程
            </v-btn>
          </div>
        </v-card-text>
      </v-card>

      <!-- 分类导航和课程网格 -->
      <v-row>
        <v-col cols="12" lg="auto" class="flex-grow-1 pr-lg-10">
          <!-- 分类导航 -->
          <v-card border rounded="lg" class="mb-6 category-nav-card">
            <v-card-text class="pa-6">
              <!-- 一级分类按钮 -->
              <div class="d-flex flex-wrap" style="gap: 12px">
                <!-- 全部分类 -->
                <v-btn
                  :color="activeFirstLvl === -1 ? 'primary' : 'grey-lighten-3'"
                  :variant="activeFirstLvl === -1 ? 'flat' : 'flat'"
                  rounded="lg"
                  class="category-btn"
                  @click="selectFirstLevel(-1)"
                >
                  <v-icon
                    icon="mdi-view-grid"
                    size="18"
                    class="mr-2"
                    :color="activeFirstLvl === -1 ? 'white' : 'grey-darken-2'"
                  ></v-icon>
                  <span :class="activeFirstLvl === -1 ? 'text-white' : 'text-grey-darken-3'">
                    全部
                  </span>
                </v-btn>

                <!-- 具体分类 -->
                <v-btn
                  v-for="category in categories"
                  :key="category.id"
                  :color="activeFirstLvl === category.id ? 'primary' : 'grey-lighten-3'"
                  :variant="activeFirstLvl === category.id ? 'flat' : 'flat'"
                  rounded="lg"
                  class="category-btn"
                  @click="selectFirstLevel(category.id)"
                >
                  <v-icon
                    :icon="category.icon"
                    size="18"
                    class="mr-2"
                    :color="activeFirstLvl === category.id ? 'white' : 'grey-darken-2'"
                  ></v-icon>
                  <span :class="activeFirstLvl === category.id ? 'text-white' : 'text-grey-darken-3'">
                    {{ category.title }}
                  </span>
                </v-btn>
              </div>

              <!-- 二级分类 -->
              <div v-if="activeFirstLvl !== -1" class="mt-6">
                <v-divider class="mb-5"></v-divider>

                <!-- 二级分类标题 -->
                <div class="d-flex align-center mb-4">
                  <v-icon icon="mdi-chevron-right" color="primary" size="20" class="mr-2"></v-icon>
                  <h4 class="text-body-1 font-weight-bold">
                    {{ activeFirstLvl === -1 ? '全部课程' : getCategoryName(activeFirstLvl) }} - 具体方向
                  </h4>
                </div>

                <!-- 二级分类按钮 -->
                <div class="d-flex flex-wrap" style="gap: 8px">
                  <!-- 二级全部 -->
                  <v-chip
                    :color="activeSecondLvl === -1 ? 'primary' : 'grey-lighten-2'"
                    :variant="activeSecondLvl === -1 ? 'flat' : 'flat'"
                    size="default"
                    class="subcategory-chip"
                    @click="selectSecondLevel(-1)"
                  >
                    <v-icon
                      :icon="activeSecondLvl === -1 ? 'mdi-check-circle' : 'mdi-circle-outline'"
                      size="14"
                      class="mr-1"
                    ></v-icon>
                    全部
                  </v-chip>

                  <!-- 具体二级分类 -->
                  <v-chip
                    v-for="sub in getSubcategories(activeFirstLvl)"
                    :key="sub.id"
                    :color="activeSecondLvl === sub.id ? 'primary' : 'grey-lighten-2'"
                    :variant="activeSecondLvl === sub.id ? 'flat' : 'flat'"
                    size="default"
                    class="subcategory-chip"
                    @click="selectSecondLevel(sub.id)"
                  >
                    <v-icon
                      :icon="activeSecondLvl === sub.id ? 'mdi-check-circle' : 'mdi-circle-outline'"
                      size="14"
                      class="mr-1"
                    ></v-icon>
                    {{ sub.name }}
                  </v-chip>
                </div>
              </div>
            </v-card-text>
          </v-card>

          <!-- 课程网格 -->
          <div v-if="loading" class="text-center py-12">
            <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
            <p class="text-body-1 text-grey-darken-2 mt-4">加载中...</p>
          </div>

          <div v-else-if="displayedCourses.length === 0" class="text-center py-12">
            <v-card border rounded="lg" class="pa-12 empty-state">
              <v-icon icon="mdi-book-open-variant" size="80" color="grey-lighten-1" class="mb-4"></v-icon>
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">未找到相关课程</h3>
              <p class="text-body-1 text-grey-darken-1 mb-4">
                {{ searchText ? '请尝试其他搜索关键词' : '该分类下暂时没有课程' }}
              </p>
              <v-btn
                v-if="activeFirstLvl !== -1 || searchText"
                color="primary"
                variant="outlined"
                rounded="lg"
                @click="
                  () => {
                    activeFirstLvl = -1
                    activeSecondLvl = -1
                    searchText = ''
                  }
                "
              >
                查看全部课程
              </v-btn>
            </v-card>
          </div>

          <div v-else>
            <!-- 分类标题 -->
            <div v-if="activeFirstLvl !== -1 || activeSecondLvl !== -1 || searchText" class="mb-4">
              <h2 class="text-h5 font-weight-bold text-grey-darken-4">
                <span v-if="searchText">搜索结果</span>
                <span v-else-if="activeSecondLvl !== -1">
                  {{ getCategoryName(activeFirstLvl) }} -
                  {{ getSubcategories(activeFirstLvl).find(s => s.id === activeSecondLvl)?.name }}
                </span>
                <span v-else>{{ getCategoryName(activeFirstLvl) }}</span>
              </h2>
              <p class="text-body-2 text-grey-darken-2 mt-1">共 {{ displayedCourses.length }} 门课程</p>
            </div>

            <!-- 课程卡片网格 -->
            <div class="course-grid">
              <v-card
                v-for="course in displayedCourses"
                :key="course.id"
                border
                rounded="lg"
                class="course-card"
                hover
                @click="goToCourse(course)"
              >
                <v-card-text class="pa-6">
                  <div class="d-flex align-center mb-4">
                    <div class="icon-container flex-shrink-0 mr-4">
                      <v-icon
                        :icon="course.icon"
                        :color="course.iconColor"
                        size="28"
                      ></v-icon>
                    </div>
                    <div class="flex-grow-1">
                      <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-1">
                        {{ course.name }}
                      </h3>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-account-group" size="14" color="grey" class="mr-1"></v-icon>
                        <span class="text-caption text-grey-darken-2">
                          {{ course.learnerCount.toLocaleString() }} 人学习
                        </span>
                      </div>
                    </div>
                  </div>

                  <p class="text-body-2 text-grey-darken-2 course-description">
                    {{ course.description }}
                  </p>
                </v-card-text>
              </v-card>
            </div>
          </div>
        </v-col>

        <!-- 右侧热门课程栏 -->
        <v-col cols="12" lg="auto" class="flex-shrink-0 pl-lg-0">
          <v-card rounded="lg" class="popular-card sticky-card narrow-sidebar no-border" flat>
            <v-card-title class="pa-4 pb-3">
              <div class="d-flex align-center justify-space-between w-100">
                <div class="d-flex align-center">
                  <v-icon icon="mdi-fire" color="error" class="mr-2"></v-icon>
                  <span class="text-h6 font-weight-bold">热门课程</span>
                </div>
                <v-btn
                  variant="text"
                  size="small"
                  color="primary"
                  class="text-caption"
                  @click="() => {
                    activeFirstLvl = -1
                    activeSecondLvl = -1
                    searchText = ''
                  }"
                >
                  全部
                  <v-icon icon="mdi-chevron-right" size="14" class="ml-1"></v-icon>
                </v-btn>
              </div>
            </v-card-title>
            <v-card-text class="pa-0 popular-list pb-4">
              <div
                v-for="(course, index) in popularCourses"
                :key="course.id"
                class="popular-item"
                @click="goToCourse(course)"
              >
                <div class="rank-badge" :class="index < 3 ? 'rank-top' : ''">
                  {{ index + 1 }}
                </div>
                <div class="flex-grow-1">
                  <div class="popular-name">{{ course.name }}</div>
                  <div class="popular-count">
                    <v-icon icon="mdi-account-group" size="12" color="grey"></v-icon>
                    {{ course.learnerCount.toLocaleString() }}
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>

    <!-- 课程申请对话框 -->
    <v-dialog v-model="showApplicationDialog" max-width="600px" persistent>
      <v-card rounded="lg" border>
        <v-card-title class="pa-6">
          <div class="d-flex align-center">
            <v-icon icon="mdi-plus-circle" color="primary" size="32" class="mr-3"></v-icon>
            <span class="text-h6 font-weight-bold">申请新课程</span>
          </div>
        </v-card-title>

        <v-card-text class="px-6 pb-0">
          <v-form v-model="applicationValid">
            <v-text-field
              label="课程名称 *"
              variant="outlined"
              clearable
              required
              class="mb-4"
            ></v-text-field>

            <v-textarea
              label="课程描述 *"
              variant="outlined"
              rows="3"
              clearable
              required
              class="mb-4"
            ></v-textarea>

            <v-select
              label="主分类 *"
              :items="categories"
              item-title="title"
              item-value="id"
              variant="outlined"
              clearable
              required
              class="mb-4"
            ></v-select>

            <v-textarea
              label="备注说明"
              variant="outlined"
              rows="2"
              clearable
              placeholder="请说明申请原因或其他补充信息..."
            ></v-textarea>
          </v-form>
        </v-card-text>

        <v-card-actions class="px-6 pb-6 pt-4">
          <v-spacer></v-spacer>
          <v-btn variant="outlined" rounded="lg" @click="closeCourseApplicationDialog">
            取消
          </v-btn>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            :disabled="!applicationValid"
          >
            提交申请
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.course-center-page {
  min-height: 100vh;
  background-color: #FFFFFF;
}

.main-content {
  margin-left: max(160px, calc((100vw - 1550px) / 2));
  padding: 80px 40px 40px 40px;
  max-width: 1550px;
  width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
}

@media (min-width: 2229px) {
  .main-content {
    margin-left: max(160px, calc((100vw - 1550px) / 2));
    padding: 80px 40px 40px 40px;
    width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
    max-width: 1550px;
  }
}

.empty-state {
  background-color: #FFFFFF;
  border: 1px solid #E5E5E5;
}

.category-nav-card {
  background-color: #FFFFFF;
  border: 1px solid #E5E5E5;
}

.category-btn {
  text-transform: none;
  letter-spacing: normal;
  font-weight: 500;
  transition: all 0.2s ease;
}

.category-btn:hover {
  transform: translateY(-2px);
}

.subcategory-chip {
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s ease;
}

.subcategory-chip:hover {
  transform: translateY(-1px);
}

.sticky-card {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.narrow-sidebar {
  width: 280px;
  max-width: 280px;
}

@media (max-width: 1264px) {
  .narrow-sidebar {
    width: 100%;
    max-width: 100%;
  }
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
  padding: 10px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.popular-item:hover {
  background-color: #F5F5F5;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background-color: #E5E5E5;
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
  background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);
  color: #FFFFFF;
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

.category-chip {
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.subcategory-chip {
  cursor: pointer;
  transition: all 0.2s ease;
}

.subcategory-chip:hover {
  transform: translateY(-1px);
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.course-card {
  background-color: #FFFFFF;
  border: 1px solid #E5E5E5;
  cursor: pointer;
  transition: all 0.3s ease;
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border-color: #000000 !important;
}

.icon-container {
  width: 56px;
  height: 56px;
  border: 1px solid #E5E5E5;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.course-description {
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 移动端 */
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    max-width: 100%;
    padding: 80px 20px 80px 20px;
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
