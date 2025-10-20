<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'
import type { CourseWithDisplay } from '@/types/profession'

const router = useRouter()

// 状态管理
const loading = ref(false)
const searchText = ref('')
const selectedStatus = ref<string>('all')

// Mock 数据 - 我的课程
const myCourses = ref<CourseWithDisplay[]>([
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
    id: 18,
    name: 'UI设计基础',
    description: '学习用户界面设计的基本原则和技巧',
    mainCategory: 5,
    subCategory: 502,
    learnerCount: 13890,
    icon: 'mdi-cellphone',
    iconColor: 'blue'
  }
])

// 为每个课程添加学习进度和状态
const coursesWithProgress = ref(
  myCourses.value.map((course, index) => ({
    ...course,
    progress: [0, 45, 78, 100, 23][index], // 模拟不同进度
    state: [0, 1, 1, 2, 1][index], // 0: 未开始, 1: 进行中, 2: 已完成
    lastActivity: ['从未学习', '2小时前', '1天前', '3天前', '刚刚'][index]
  }))
)

const filteredCourses = ref([...coursesWithProgress.value])

// 筛选课程
const filterCourses = (): void => {
  let filtered = [...coursesWithProgress.value]

  // 状态筛选
  if (selectedStatus.value !== 'all') {
    const statusNum = parseInt(selectedStatus.value)
    filtered = filtered.filter((c) => c.state === statusNum)
  }

  // 搜索筛选
  if (searchText.value.trim()) {
    const searchLower = searchText.value.toLowerCase()
    filtered = filtered.filter(
      (c) =>
        c.name?.toLowerCase().includes(searchLower) ||
        c.description?.toLowerCase().includes(searchLower)
    )
  }

  filteredCourses.value = filtered
}

// 打开课程
const openCourse = (course: any): void => {
  console.log('打开课程:', course.name)
  // router.push(`/course/${course.id}`)
}

// 获取状态文本
const getStatusText = (state: number): string => {
  const stateTexts: Record<number, string> = {
    0: '未开始',
    1: '进行中',
    2: '已完成'
  }
  return stateTexts[state] || '未知'
}

// 获取状态颜色
const getStatusColor = (state: number): string => {
  const colors: Record<number, string> = {
    0: 'grey',
    1: 'primary',
    2: 'success'
  }
  return colors[state] || 'grey'
}

// 获取进度颜色
const getProgressColor = (progress: number): string => {
  return 'grey-lighten-1'
}
</script>

<template>
  <div class="my-courses-page">
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
            <div class="d-flex align-center mb-4">
              <v-avatar color="purple-lighten-4" size="56" class="mr-3">
                <v-icon icon="mdi-book-multiple" color="purple" size="28"></v-icon>
              </v-avatar>
              <div>
                <h1 class="text-h4 font-weight-bold text-grey-darken-4">我的课程</h1>
                <p class="text-body-2 text-grey-darken-2 mt-1">
                  继续学习，持续进步
                </p>
              </div>
            </div>
          </div>

          <!-- 搜索和筛选 -->
          <v-card border rounded="xl" class="filter-card pa-4 mb-6">
            <div class="d-flex align-center gap-3">
              <!-- 状态筛选 -->
              <v-btn-toggle
                v-model="selectedStatus"
                variant="outlined"
                color="primary"
                rounded="lg"
                density="comfortable"
                mandatory
                @update:model-value="filterCourses"
              >
                <v-btn value="all" size="default">
                  <v-icon icon="mdi-format-list-bulleted" size="16" class="mr-1"></v-icon>
                  全部
                </v-btn>
                <v-btn value="0" size="default">
                  <v-icon icon="mdi-circle-outline" size="16" class="mr-1"></v-icon>
                  未开始
                </v-btn>
                <v-btn value="1" size="default">
                  <v-icon icon="mdi-play-circle" size="16" class="mr-1"></v-icon>
                  进行中
                </v-btn>
                <v-btn value="2" size="default">
                  <v-icon icon="mdi-check-circle" size="16" class="mr-1"></v-icon>
                  已完成
                </v-btn>
              </v-btn-toggle>

              <v-spacer></v-spacer>

              <!-- 搜索框 -->
              <v-text-field
                v-model="searchText"
                placeholder="搜索课程..."
                variant="outlined"
                color="primary"
                density="comfortable"
                hide-details
                clearable
                class="search-field"
                @update:model-value="filterCourses"
              >
                <template #prepend-inner>
                  <v-icon icon="mdi-magnify" color="grey-darken-1" size="20"></v-icon>
                </template>
              </v-text-field>
            </div>
          </v-card>

          <!-- 课程列表 -->
          <div v-if="loading" class="text-center py-12">
            <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
            <p class="text-body-2 text-grey mt-4">加载中...</p>
          </div>

          <div v-else-if="filteredCourses.length === 0" class="text-center py-12">
            <v-card border rounded="xl" class="pa-12 empty-state">
              <v-icon
                icon="mdi-book-search"
                size="80"
                color="grey-lighten-1"
                class="mb-4"
              ></v-icon>
              <h3 class="text-h6 text-grey-darken-2 mb-2">暂无课程</h3>
              <p class="text-body-2 text-grey">
                {{ selectedStatus === 'all' ? '还没有学习任何课程' : '该状态下暂无课程' }}
              </p>
              <v-btn
                v-if="selectedStatus === 'all'"
                color="primary"
                variant="flat"
                rounded="lg"
                class="mt-4"
                @click="router.push('/learning')"
              >
                <v-icon icon="mdi-plus" size="20" class="mr-1"></v-icon>
                浏览课程
              </v-btn>
            </v-card>
          </div>

          <v-row v-else>
            <v-col
              v-for="course in filteredCourses"
              :key="course.id"
              cols="12"
              md="6"
              lg="4"
            >
              <v-card
                border
                rounded="xl"
                class="course-card"
                hover
                @click="openCourse(course)"
              >
                <v-card-text class="pa-5">
                  <!-- 课程图标和标题 -->
                  <div class="d-flex align-start mb-3">
                    <v-avatar :color="course.iconColor" size="48" class="mr-3">
                      <v-icon :icon="course.icon" color="white" size="24"></v-icon>
                    </v-avatar>
                    <div class="flex-grow-1">
                      <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-1">
                        {{ course.name }}
                      </h3>
                      <div class="d-flex align-center gap-2">
                        <v-chip
                          :color="getStatusColor(course.state)"
                          size="x-small"
                          variant="flat"
                        >
                          {{ getStatusText(course.state) }}
                        </v-chip>
                        <span class="text-caption text-grey">{{ course.lastActivity }}</span>
                      </div>
                    </div>
                  </div>

                  <!-- 课程描述 -->
                  <p class="text-body-2 text-grey-darken-2 mb-4 course-description">
                    {{ course.description }}
                  </p>

                  <!-- 进度条 -->
                  <div class="mb-2">
                    <div class="d-flex justify-space-between align-center mb-1">
                      <span class="text-caption text-grey-darken-1">学习进度</span>
                      <span class="text-caption font-weight-bold">{{ course.progress }}%</span>
                    </div>
                    <v-progress-linear
                      :model-value="course.progress"
                      :color="getProgressColor(course.progress)"
                      height="6"
                      rounded
                    ></v-progress-linear>
                  </div>

                  <!-- 学习人数 -->
                  <div class="d-flex align-center text-caption text-grey">
                    <v-icon icon="mdi-account-multiple" size="14" class="mr-1"></v-icon>
                    {{ course.learnerCount }} 人学习
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </v-col>
      </v-row>
    </v-container>

    <AppFooter />
  </div>
</template>

<style scoped>
.my-courses-page {
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

.filter-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
}

.search-field {
  max-width: 300px;
}

.gap-3 {
  gap: 12px;
}

.course-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.course-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.empty-state {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
}

.gap-2 {
  gap: 8px;
}

/* 移动端 */
@media (max-width: 1280px) {
  .main-content {
    padding-left: 20px;
  }
}
</style>
