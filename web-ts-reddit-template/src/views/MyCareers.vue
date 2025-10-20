<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'
import type { CareerWithDisplay } from '@/types/profession'

const router = useRouter()

// 状态管理
const loading = ref(false)
const searchText = ref('')
const selectedStatus = ref<string>('all')

// Mock 数据 - 我的职业
const myCareers = ref<CareerWithDisplay[]>([
  {
    id: 1,
    name: '前端工程师',
    description: '负责开发网站和应用的用户界面，使用现代前端框架构建响应式、高性能的Web应用。需要精通HTML、CSS、JavaScript等技术，熟悉Vue/React等主流框架。',
    mainCategory: 1,
    subCategory: 101,
    skills: 'Vue.js,React,TypeScript,HTML,CSS,JavaScript,Webpack',
    icon: 'mdi-laptop',
    iconColor: 'primary'
  },
  {
    id: 8,
    name: 'UX设计师',
    description: '研究用户行为和需求，设计用户体验流程，通过用户测试和数据分析优化产品的可用性和用户满意度。',
    mainCategory: 2,
    subCategory: 202,
    skills: '用户研究,原型设计,可用性测试,交互设计,信息架构',
    icon: 'mdi-account-heart',
    iconColor: 'success'
  },
  {
    id: 17,
    name: '数据分析师',
    description: '收集、处理和分析数据，提供业务洞察和决策支持，使用数据可视化工具呈现分析结果。',
    mainCategory: 5,
    subCategory: 501,
    skills: 'SQL,Python,Excel,Tableau,Power BI,统计分析',
    icon: 'mdi-chart-line',
    iconColor: 'info'
  }
])

// 为每个职业添加学习进度和状态
const careersWithProgress = ref(
  myCareers.value.map((career, index) => ({
    ...career,
    progress: [65, 30, 15][index], // 模拟不同进度
    state: [1, 1, 0][index], // 0: 未开始, 1: 进行中, 2: 已完成
    roadmapCount: [12, 8, 15][index], // 路线图数量
    completedRoadmaps: [8, 2, 0][index], // 已完成路线图
    lastActivity: ['昨天', '3天前', '从未学习'][index]
  }))
)

const filteredCareers = ref([...careersWithProgress.value])

// 筛选职业
const filterCareers = (): void => {
  let filtered = [...careersWithProgress.value]

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
        c.description?.toLowerCase().includes(searchLower) ||
        c.skills?.toLowerCase().includes(searchLower)
    )
  }

  filteredCareers.value = filtered
}

// 打开职业详情
const openCareer = (career: any): void => {
  console.log('打开职业:', career.name)
  // router.push(`/roadmap/${career.id}`)
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
  <div class="my-careers-page">
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
              <v-avatar color="orange-lighten-4" size="56" class="mr-3">
                <v-icon icon="mdi-briefcase-variant" color="orange" size="28"></v-icon>
              </v-avatar>
              <div>
                <h1 class="text-h4 font-weight-bold text-grey-darken-4">我的职业</h1>
                <p class="text-body-2 text-grey-darken-2 mt-1">
                  规划职业发展，实现职业目标
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
                @update:model-value="filterCareers"
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
                placeholder="搜索职业或技能..."
                variant="outlined"
                color="primary"
                density="comfortable"
                hide-details
                clearable
                class="search-field"
                @update:model-value="filterCareers"
              >
                <template #prepend-inner>
                  <v-icon icon="mdi-magnify" color="grey-darken-1" size="20"></v-icon>
                </template>
              </v-text-field>
            </div>
          </v-card>

          <!-- 职业列表 -->
          <div v-if="loading" class="text-center py-12">
            <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
            <p class="text-body-2 text-grey mt-4">加载中...</p>
          </div>

          <div v-else-if="filteredCareers.length === 0" class="text-center py-12">
            <v-card border rounded="xl" class="pa-12 empty-state">
              <v-icon
                icon="mdi-briefcase-search"
                size="80"
                color="grey-lighten-1"
                class="mb-4"
              ></v-icon>
              <h3 class="text-h6 text-grey-darken-2 mb-2">暂无职业规划</h3>
              <p class="text-body-2 text-grey">
                {{ selectedStatus === 'all' ? '还没有选择职业方向' : '该状态下暂无职业' }}
              </p>
              <v-btn
                v-if="selectedStatus === 'all'"
                color="primary"
                variant="flat"
                rounded="lg"
                class="mt-4"
                @click="router.push('/career')"
              >
                <v-icon icon="mdi-plus" size="20" class="mr-1"></v-icon>
                浏览职业
              </v-btn>
            </v-card>
          </div>

          <v-row v-else>
            <v-col
              v-for="career in filteredCareers"
              :key="career.id"
              cols="12"
            >
              <v-card
                border
                rounded="xl"
                class="career-card"
                hover
                @click="openCareer(career)"
              >
                <v-card-text class="pa-5">
                  <div class="d-flex">
                    <!-- 左侧：图标和基本信息 -->
                    <div class="flex-shrink-0 mr-5">
                      <v-avatar :color="career.iconColor" size="80">
                        <v-icon :icon="career.icon" color="white" size="40"></v-icon>
                      </v-avatar>
                    </div>

                    <!-- 中间：详细信息 -->
                    <div class="flex-grow-1">
                      <!-- 标题和状态 -->
                      <div class="d-flex align-center justify-space-between mb-2">
                        <div>
                          <h2 class="text-h5 font-weight-bold text-grey-darken-4 mb-1">
                            {{ career.name }}
                          </h2>
                          <div class="d-flex align-center gap-2">
                            <v-chip
                              :color="getStatusColor(career.state)"
                              size="small"
                              variant="flat"
                            >
                              {{ getStatusText(career.state) }}
                            </v-chip>
                            <span class="text-caption text-grey">{{ career.lastActivity }}</span>
                          </div>
                        </div>
                      </div>

                      <!-- 描述 -->
                      <p class="text-body-2 text-grey-darken-2 mb-3">
                        {{ career.description }}
                      </p>

                      <!-- 技能标签 -->
                      <div class="mb-4">
                        <span class="text-caption text-grey-darken-1 mr-2">核心技能：</span>
                        <v-chip
                          v-for="(skill, index) in career.skills.split(',')"
                          :key="index"
                          size="small"
                          variant="outlined"
                          class="ma-1"
                        >
                          {{ skill }}
                        </v-chip>
                      </div>

                      <!-- 学习进度 -->
                      <div class="mb-3">
                        <div class="d-flex justify-space-between align-center mb-2">
                          <div class="d-flex align-center gap-4">
                            <span class="text-body-2 text-grey-darken-1">
                              总体进度：
                              <span class="font-weight-bold text-grey-darken-4">{{ career.progress }}%</span>
                            </span>
                            <span class="text-body-2 text-grey-darken-1">
                              学习路线：
                              <span class="font-weight-bold text-grey-darken-4">
                                {{ career.completedRoadmaps }}/{{ career.roadmapCount }}
                              </span>
                            </span>
                          </div>
                        </div>
                        <v-progress-linear
                          :model-value="career.progress"
                          :color="getProgressColor(career.progress)"
                          height="8"
                          rounded
                        ></v-progress-linear>
                      </div>
                    </div>
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
.my-careers-page {
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

.gap-2 {
  gap: 8px;
}

.gap-4 {
  gap: 16px;
}

.career-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.career-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.empty-state {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
}

/* 移动端 */
@media (max-width: 1280px) {
  .main-content {
    padding-left: 20px;
  }
}
</style>
