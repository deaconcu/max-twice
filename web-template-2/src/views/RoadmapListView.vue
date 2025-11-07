<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'

const router = useRouter()
const route = useRoute()

// 从路由获取职业ID
const professionId = ref(Number(route.params.professionId) || 1)

// 状态管理
const loading = ref(false)
const searchText = ref('')
const selectedStatus = ref<string>('all')
const selectedSort = ref<string>('latest')

// Mock 数据 - 职业信息
const profession = ref({
  id: 1,
  name: '前端工程师',
  description: '负责开发网站和应用的用户界面，使用现代前端框架构建响应式、高性能的Web应用',
  icon: 'mdi-laptop',
  iconColor: 'primary',
  roadmapCount: 12,
  learnerCount: 1580
})

// 筛选和排序
const allRoadmaps = ref([
  {
    id: 1,
    professionId: 1,
    description: 'Vue 3 + TypeScript 全栈开发路线',
    detail: '从基础到进阶，系统学习 Vue 3 组合式 API、TypeScript 类型系统、Pinia 状态管理，以及全栈项目开发实战。',
    creator: {
      id: 1,
      username: 'techmaster',
      avatar: null
    },
    vote: 156,
    upvoted: true,
    comment: 23,
    pinned: true,
    learning: true,
    learners: 450,
    state: 1,
    createdAt: '2024-01-15',
    updatedAt: '2024-01-20',
    nodeCount: 12
  },
  {
    id: 2,
    professionId: 1,
    description: 'React 18 现代前端开发完整路径',
    detail: '掌握 React 18 新特性、Hooks 使用技巧、状态管理方案，构建高性能的现代化 Web 应用。',
    creator: {
      id: 2,
      username: 'reactdev',
      avatar: null
    },
    vote: 142,
    upvoted: false,
    comment: 18,
    pinned: true,
    learning: false,
    learners: 380,
    state: 1,
    createdAt: '2024-01-12',
    updatedAt: '2024-01-18',
    nodeCount: 15
  },
  {
    id: 3,
    professionId: 1,
    description: '前端工程化与性能优化实战',
    detail: '深入学习 Webpack、Vite 等构建工具，掌握代码分割、懒加载、性能监控等优化技巧。',
    creator: {
      id: 3,
      username: 'frontend_guru',
      avatar: null
    },
    vote: 98,
    upvoted: false,
    comment: 12,
    pinned: false,
    learning: true,
    learners: 220,
    state: 1,
    createdAt: '2024-01-10',
    updatedAt: '2024-01-16',
    nodeCount: 10
  },
  {
    id: 4,
    professionId: 1,
    description: '从零开始的 JavaScript 进阶之路',
    detail: '从 ES6+ 语法到异步编程、原型链、闭包等核心概念，全面提升 JavaScript 编程能力。',
    creator: {
      id: 4,
      username: 'js_expert',
      avatar: null
    },
    vote: 234,
    upvoted: true,
    comment: 45,
    pinned: false,
    learning: false,
    learners: 680,
    state: 1,
    createdAt: '2024-01-08',
    updatedAt: '2024-01-14',
    nodeCount: 18
  },
  {
    id: 5,
    professionId: 1,
    description: 'CSS 高级技巧与响应式设计',
    detail: '学习 Flexbox、Grid 布局、CSS 动画、移动端适配等现代 CSS 开发技术。',
    creator: {
      id: 5,
      username: 'css_ninja',
      avatar: null
    },
    vote: 67,
    upvoted: false,
    comment: 8,
    pinned: false,
    learning: false,
    learners: 150,
    state: 1,
    createdAt: '2024-01-05',
    updatedAt: '2024-01-12',
    nodeCount: 8
  }
])

const filteredRoadmaps = ref([...allRoadmaps.value])

// 筛选和排序
const filterRoadmaps = (): void => {
  let filtered = [...allRoadmaps.value]

  // 状态筛选
  if (selectedStatus.value === 'pinned') {
    filtered = filtered.filter((r) => r.pinned)
  } else if (selectedStatus.value === 'learning') {
    filtered = filtered.filter((r) => r.learning)
  } else if (selectedStatus.value === 'upvoted') {
    filtered = filtered.filter((r) => r.upvoted)
  }

  // 搜索筛选
  if (searchText.value.trim()) {
    const searchLower = searchText.value.toLowerCase()
    filtered = filtered.filter((r) => r.description?.toLowerCase().includes(searchLower))
  }

  // 排序
  if (selectedSort.value === 'latest') {
    filtered.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  } else if (selectedSort.value === 'popular') {
    filtered.sort((a, b) => b.vote - a.vote)
  } else if (selectedSort.value === 'learners') {
    filtered.sort((a, b) => b.learners - a.learners)
  }

  // 置顶的始终在前面
  filtered.sort((a, b) => {
    if (a.pinned && !b.pinned) return -1
    if (!a.pinned && b.pinned) return 1
    return 0
  })

  filteredRoadmaps.value = filtered
}

// 打开路径详情
const openRoadmapDetail = (roadmap: any): void => {
  router.push(`/roadmap/${professionId.value}/detail/${roadmap.id}`)
}

// 创建新路径
const createRoadmap = (): void => {
  router.push(`/roadmap/${professionId.value}/create`)
}

// 返回职业中心
const backToCareer = (): void => {
  router.push('/career')
}

// 投票
const handleVote = (roadmap: any, event: Event): void => {
  event.stopPropagation()
  roadmap.upvoted = !roadmap.upvoted
  roadmap.vote += roadmap.upvoted ? 1 : -1
}

// 开始学习
const handleStartLearning = (roadmap: any, event: Event): void => {
  event.stopPropagation()
  roadmap.learning = !roadmap.learning
}

// 复制路径
const handleCopy = (roadmap: any, event: Event): void => {
  event.stopPropagation()
  console.log('复制路径:', roadmap.description)
  // 实际应该跳转到创建页面并携带数据
  router.push(`/roadmap/${professionId.value}/create?copy=${roadmap.id}`)
}

// 获取时间显示
const getTimeDisplay = (date: string): string => {
  const now = new Date()
  const created = new Date(date)
  const days = Math.floor((now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return date
}
</script>

<template>
  <div class="roadmap-list-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
          <!-- 返回按钮 -->
          <v-btn
            variant="text"
            color="grey-darken-2"
            class="mb-4"
            @click="backToCareer"
          >
            <v-icon icon="mdi-arrow-left" class="mr-1"></v-icon>
            返回职业中心
          </v-btn>

          <!-- 职业信息头部 -->
          <v-card rounded="lg" class="profession-header mb-6 no-border">
            <v-card-text class="pa-0">
              <div class="d-flex align-center">
                <!-- 职业图标 -->
                <v-avatar color="grey-lighten-3" size="80" rounded="lg" class="mr-5">
                  <v-icon :icon="profession.icon" color="#666666" size="40"></v-icon>
                </v-avatar>

                <!-- 职业信息 -->
                <div class="flex-grow-1">
                  <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-2">
                    {{ profession.name }}
                  </h1>
                  <p class="text-body-2 text-grey-darken-2 mb-3">
                    {{ profession.description }}
                  </p>
                  <div class="d-flex align-center gap-4">
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-map-marker-path" size="18" color="primary" class="mr-1"></v-icon>
                      <span class="text-body-2 text-grey-darken-1">
                        <span class="font-weight-bold text-grey-darken-4">{{ profession.roadmapCount }}</span> 条学习路径
                      </span>
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-account-group" size="18" color="success" class="mr-1"></v-icon>
                      <span class="text-body-2 text-grey-darken-1">
                        <span class="font-weight-bold text-grey-darken-4">{{ profession.learnerCount }}</span> 人学习
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 创建按钮 -->
                <v-btn
                  color="primary"
                  variant="flat"
                  size="large"
                  rounded="lg"
                  @click="createRoadmap"
                >
                  <v-icon icon="mdi-plus" size="20" class="mr-1"></v-icon>
                  创建路径
                </v-btn>
              </div>
            </v-card-text>
          </v-card>

          <v-row>
            <v-col cols="12" lg="8">
              <!-- 筛选和搜索 -->
              <div class="filter-section mb-6">
                <div class="d-flex align-center gap-3 flex-wrap">
                  <!-- 状态筛选 -->
                  <v-btn-toggle
                    v-model="selectedStatus"
                    variant="outlined"
                    color="primary"
                    rounded="lg"
                    density="comfortable"
                    mandatory
                    @update:model-value="filterRoadmaps"
                  >
                    <v-btn value="all" size="default">
                      <v-icon icon="mdi-format-list-bulleted" size="16" class="mr-1"></v-icon>
                      全部
                    </v-btn>
                    <v-btn value="pinned" size="default">
                      <v-icon icon="mdi-pin" size="16" class="mr-1"></v-icon>
                      置顶
                    </v-btn>
                    <v-btn value="learning" size="default">
                      <v-icon icon="mdi-school" size="16" class="mr-1"></v-icon>
                      学习中
                    </v-btn>
                    <v-btn value="upvoted" size="default">
                      <v-icon icon="mdi-heart" size="16" class="mr-1"></v-icon>
                      已点赞
                    </v-btn>
                  </v-btn-toggle>

                  <v-spacer></v-spacer>

                  <!-- 排序 -->
                  <v-select
                    v-model="selectedSort"
                    :items="[
                      { title: '最新发布', value: 'latest' },
                      { title: '最受欢迎', value: 'popular' },
                      { title: '学习人数', value: 'learners' }
                    ]"
                    variant="solo"
                    density="compact"
                    hide-details
                    flat
                    class="sort-select"
                    @update:model-value="filterRoadmaps"
                  >
                    <template #prepend-inner>
                      <v-icon icon="mdi-sort" size="18" class="mr-1"></v-icon>
                    </template>
                  </v-select>
                </div>
              </div>

              <!-- 路径列表 -->
              <div v-if="loading" class="text-center py-12">
                <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
                <p class="text-body-2 text-grey mt-4">加载中...</p>
              </div>

              <div v-else-if="filteredRoadmaps.length === 0" class="text-center py-12">
                <v-card border rounded="lg" class="pa-12 empty-state">
                  <v-icon
                    icon="mdi-map-marker-path"
                    size="80"
                    color="grey-lighten-1"
                    class="mb-4"
                  ></v-icon>
                  <h3 class="text-h6 text-grey-darken-2 mb-2">暂无学习路径</h3>
                  <p class="text-body-2 text-grey mb-4">
                    {{ selectedStatus === 'all' ? '还没有人创建学习路径' : '该筛选条件下暂无路径' }}
                  </p>
                  <v-btn
                    v-if="selectedStatus === 'all'"
                    color="primary"
                    variant="flat"
                    rounded="lg"
                    @click="createRoadmap"
                  >
                    <v-icon icon="mdi-plus" size="20" class="mr-1"></v-icon>
                    创建第一条路径
                  </v-btn>
                </v-card>
              </div>

              <div v-else>
              <v-card
                v-for="roadmap in filteredRoadmaps"
                :key="roadmap.id"
                border
                rounded="lg"
                class="roadmap-card mb-4"
                hover
                @click="openRoadmapDetail(roadmap)"
              >
                <v-card-text class="pa-5">
                  <div class="d-flex">
                    <!-- 左侧：投票区域 -->
                    <div class="vote-section mr-4">
                      <v-btn
                        :color="roadmap.upvoted ? 'primary' : 'grey-lighten-1'"
                        :variant="roadmap.upvoted ? 'flat' : 'outlined'"
                        icon
                        size="small"
                        @click="handleVote(roadmap, $event)"
                      >
                        <v-icon :icon="roadmap.upvoted ? 'mdi-heart' : 'mdi-heart-outline'"></v-icon>
                      </v-btn>
                      <div class="vote-count text-center mt-1">
                        <span class="text-caption font-weight-bold" :class="roadmap.upvoted ? 'text-primary' : 'text-grey-darken-2'">
                          {{ roadmap.vote }}
                        </span>
                      </div>
                    </div>

                    <!-- 中间：路径信息 -->
                    <div class="flex-grow-1">
                      <!-- 标题和标签 -->
                      <div class="d-flex align-center mb-2">
                        <v-chip
                          v-if="roadmap.pinned"
                          color="warning"
                          size="x-small"
                          variant="flat"
                          class="mr-2"
                        >
                          <v-icon icon="mdi-pin" size="12" class="mr-1"></v-icon>
                          置顶
                        </v-chip>
                        <v-chip
                          v-if="roadmap.learning"
                          color="success"
                          size="x-small"
                          variant="flat"
                          class="mr-2"
                        >
                          <v-icon icon="mdi-school" size="12" class="mr-1"></v-icon>
                          学习中
                        </v-chip>
                        <h3 class="text-h6 font-weight-bold text-grey-darken-4">
                          {{ roadmap.description }}
                        </h3>
                      </div>

                      <!-- 详细描述 -->
                      <p class="text-body-2 text-grey-darken-2 mb-3">
                        {{ roadmap.detail }}
                      </p>

                      <!-- 统计信息 -->
                      <div class="d-flex align-center gap-4 mb-3">
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-account" size="16" color="grey" class="mr-1"></v-icon>
                          <span class="text-caption text-grey-darken-2">
                            {{ roadmap.creator.username }}
                          </span>
                        </div>
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-account-group" size="16" color="grey" class="mr-1"></v-icon>
                          <span class="text-caption text-grey-darken-2">
                            {{ roadmap.learners }} 人学习
                          </span>
                        </div>
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-comment-outline" size="16" color="grey" class="mr-1"></v-icon>
                          <span class="text-caption text-grey-darken-2">
                            {{ roadmap.comment }} 评论
                          </span>
                        </div>
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-circle-medium" size="16" color="grey" class="mr-1"></v-icon>
                          <span class="text-caption text-grey-darken-2">
                            {{ roadmap.nodeCount }} 个节点
                          </span>
                        </div>
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-clock-outline" size="16" color="grey" class="mr-1"></v-icon>
                          <span class="text-caption text-grey-darken-2">
                            {{ getTimeDisplay(roadmap.createdAt) }}
                          </span>
                        </div>
                      </div>

                      <!-- 操作按钮 -->
                      <div class="d-flex align-center gap-2">
                        <v-btn
                          :color="roadmap.learning ? 'success' : 'primary'"
                          :variant="roadmap.learning ? 'outlined' : 'flat'"
                          size="small"
                          rounded="lg"
                          @click="handleStartLearning(roadmap, $event)"
                        >
                          <v-icon :icon="roadmap.learning ? 'mdi-check' : 'mdi-play'" size="16" class="mr-1"></v-icon>
                          {{ roadmap.learning ? '正在学习' : '开始学习' }}
                        </v-btn>
                        <v-btn
                          color="grey-darken-2"
                          variant="outlined"
                          size="small"
                          rounded="lg"
                          @click="handleCopy(roadmap, $event)"
                        >
                          <v-icon icon="mdi-content-copy" size="16" class="mr-1"></v-icon>
                          复制
                        </v-btn>
                      </div>
                    </div>
                  </div>
                </v-card-text>
              </v-card>
              </div>
            </v-col>

            <!-- 右侧：说明卡片 -->
            <v-col cols="12" lg="4" class="pt-0">
              <v-card rounded="lg" class="info-card sticky-card no-border px-4">
                <v-card-title class="pa-0 pb-4">
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-information" color="primary" class="mr-2"></v-icon>
                    <span class="text-h6 font-weight-bold">学习路径说明</span>
                  </div>
                </v-card-title>

                <v-card-text class="pa-0 pt-0">
                  <!-- 什么是学习路径 -->
                  <div class="info-section mb-5">
                    <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-2">
                      <v-icon icon="mdi-map-marker-path" size="18" color="primary" class="mr-1"></v-icon>
                      什么是学习路径？
                    </h3>
                    <p class="text-body-2 text-grey-darken-2">
                      学习路径是由社区成员创建的结构化学习计划，帮助你系统地掌握某个职业所需的技能和知识。
                    </p>
                  </div>

                  <v-divider class="mb-5"></v-divider>

                  <!-- 如何使用 -->
                  <div class="info-section mb-5">
                    <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-3">
                      <v-icon icon="mdi-lightbulb" size="18" color="warning" class="mr-1"></v-icon>
                      如何使用？
                    </h3>
                    <div class="d-flex align-start mb-2">
                      <v-icon icon="mdi-numeric-1-circle" size="20" color="primary" class="mr-2 mt-1"></v-icon>
                      <div>
                        <p class="text-body-2 font-weight-medium text-grey-darken-3">浏览和筛选</p>
                        <p class="text-caption text-grey-darken-1">使用筛选和搜索功能找到适合你的路径</p>
                      </div>
                    </div>
                    <div class="d-flex align-start mb-2">
                      <v-icon icon="mdi-numeric-2-circle" size="20" color="primary" class="mr-2 mt-1"></v-icon>
                      <div>
                        <p class="text-body-2 font-weight-medium text-grey-darken-3">开始学习</p>
                        <p class="text-caption text-grey-darken-1">点击"开始学习"追踪你的学习进度</p>
                      </div>
                    </div>
                    <div class="d-flex align-start mb-2">
                      <v-icon icon="mdi-numeric-3-circle" size="20" color="primary" class="mr-2 mt-1"></v-icon>
                      <div>
                        <p class="text-body-2 font-weight-medium text-grey-darken-3">查看详情</p>
                        <p class="text-caption text-grey-darken-1">点击路径卡片查看完整的学习路线图</p>
                      </div>
                    </div>
                    <div class="d-flex align-start">
                      <v-icon icon="mdi-numeric-4-circle" size="20" color="primary" class="mr-2 mt-1"></v-icon>
                      <div>
                        <p class="text-body-2 font-weight-medium text-grey-darken-3">参与互动</p>
                        <p class="text-caption text-grey-darken-1">点赞优质路径，评论分享学习经验</p>
                      </div>
                    </div>
                  </div>

                  <v-divider class="mb-5"></v-divider>

                  <!-- 创建自己的路径 -->
                  <div class="info-section">
                    <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-2">
                      <v-icon icon="mdi-plus-circle" size="18" color="success" class="mr-1"></v-icon>
                      创建你的路径
                    </h3>
                    <p class="text-body-2 text-grey-darken-2 mb-3">
                      你可以创建自己的学习路径，或者复制现有路径进行个性化修改。
                    </p>
                    <v-btn
                      color="primary"
                      variant="flat"
                      block
                      rounded="lg"
                      @click="createRoadmap"
                    >
                      <v-icon icon="mdi-plus" size="18" class="mr-1"></v-icon>
                      创建新路径
                    </v-btn>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

  </div>
</template>

<style scoped>
.roadmap-list-page {
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

.profession-header {
  background-color: #FFFFFF;
  border: 1px solid #E5E5E5;
  border-radius: 16px;
}

.filter-card,
.info-card {
  background-color: #FFFFFF;
  border-radius: 16px;
}

.filter-card {
  border: 1px solid #E5E5E5;
}

.info-card {
  border: 1px solid #E5E5E5;
}

.sticky-card {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow-y: auto;
}

.sticky-card::-webkit-scrollbar {
  width: 4px;
}

.sticky-card::-webkit-scrollbar-track {
  background: transparent;
}

.sticky-card::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.sticky-card::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

.search-field {
  min-width: 250px;
  flex: 1;
}

.sort-select {
  max-width: 160px;
  flex-shrink: 0;
}

.sort-select :deep(.v-field) {
  padding-left: 12px !important;
  padding-right: 0 !important;
}

.sort-select :deep(.v-field__input) {
  padding: 0 !important;
  padding-top: 8px !important;
  padding-bottom: 8px !important;
  min-width: 0 !important;
  flex: 0 1 auto !important;
}

.sort-select :deep(.v-field__append-inner) {
  padding-left: 0 !important;
  margin-left: 0 !important;
}

.sort-select :deep(.v-select__selection-text) {
  flex: 0 1 auto !important;
}

.gap-3 {
  gap: 12px;
}

.gap-4 {
  gap: 16px;
}

.gap-2 {
  gap: 8px;
}

.roadmap-card {
  background-color: #FFFFFF;
  border: 1px solid #E5E5E5;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.roadmap-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #000000;
}

.vote-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 48px;
}

.vote-count {
  line-height: 1;
}

.empty-state {
  background-color: #FFFFFF;
  border: 1px solid #E5E5E5;
  border-radius: 16px;
}

/* 移动端 */
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    max-width: 100%;
    padding: 80px 20px 80px 20px;
  }

  .gap-3 {
    gap: 8px;
  }

  .search-field {
    max-width: 100%;
    width: 100%;
  }

  .sort-select {
    max-width: 100%;
  }
}
</style>
