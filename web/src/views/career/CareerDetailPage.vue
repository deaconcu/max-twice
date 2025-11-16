<template>
  <DefaultLayout>
    <div class="career-detail-page">
      <!-- 返回按钮 -->
      <v-btn variant="text" color="grey-darken-2" class="mb-4" @click="handleBack">
        <v-icon icon="mdi-arrow-left" class="mr-1" />
        返回职业中心
      </v-btn>

      <!-- 加载状态 -->
      <div v-if="loading" class="text-center py-12">
        <v-progress-circular indeterminate color="primary" size="64" />
        <p class="text-body-2 text-grey mt-4">加载中...</p>
      </div>

      <!-- 错误状态 -->
      <v-alert v-else-if="error" type="error" variant="tonal" class="mb-6">
        {{ error }}
      </v-alert>

      <!-- 内容区 -->
      <div v-else-if="career" class="content-wrapper">
        <!-- 职业信息头部 -->
        <div class="profession-header mt-4 mb-8 pa-0">
          <div class="d-flex align-center">
            <!-- 职业图标 -->
            <v-avatar color="primary" size="80" class="mr-5">
              <v-icon :icon="getCareerIcon()" color="white" size="40" />
            </v-avatar>

            <!-- 职业信息 -->
            <div class="flex-grow-1">
              <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-2">
                {{ career.name }}
              </h1>
              <p class="text-body-2 text-grey-darken-2 mb-3">
                {{ career.description }}
              </p>
              <div class="d-flex align-center gap-4">
                <div class="d-flex align-center">
                  <v-icon icon="mdi-map-marker-path" size="18" color="primary" class="mr-1" />
                  <span class="text-body-2 text-grey-darken-1">
                    <span class="font-weight-bold text-grey-darken-4">{{ roadmapsCount }}</span>
                    条学习路径
                  </span>
                </div>
                <div class="d-flex align-center">
                  <v-icon icon="mdi-account-group" size="18" color="success" class="mr-1" />
                  <span class="text-body-2 text-grey-darken-1">
                    <span class="font-weight-bold text-grey-darken-4">{{
                      formatNumber(career.learnerCount)
                    }}</span>
                    人学习
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
              @click="handleCreateRoadmap"
            >
              <v-icon icon="mdi-plus" size="20" class="mr-1" />
              创建路径
            </v-btn>
          </div>
        </div>

        <!-- 路径列表 -->
        <v-row class="ma-0 mt-2">
          <v-col cols="12" lg="8" class="pa-0">
            <!-- 筛选和搜索 -->
            <div class="filter-card mb-6">
              <div class="d-flex align-center justify-space-between">
                <!-- 状态筛选 -->
                <v-btn-toggle
                  v-model="filterStatus"
                  variant="text"
                  color="primary"
                  rounded="lg"
                  density="comfortable"
                  mandatory
                  @update:model-value="filterRoadmaps"
                >
                  <v-btn value="all" size="default">
                    <v-icon icon="mdi-format-list-bulleted" size="16" class="mr-1" />
                    全部
                  </v-btn>
                  <v-btn value="pinned" size="default">
                    <v-icon icon="mdi-pin" size="16" class="mr-1" />
                    置顶
                  </v-btn>
                  <v-btn value="learning" size="default">
                    <v-icon icon="mdi-school" size="16" class="mr-1" />
                    学习中
                  </v-btn>
                  <v-btn value="upvoted" size="default">
                    <v-icon icon="mdi-heart" size="16" class="mr-1" />
                    已点赞
                  </v-btn>
                </v-btn-toggle>

                <!-- 排序 -->
                <v-select
                  v-model="sortBy"
                  :items="sortOptions"
                  variant="solo"
                  density="comfortable"
                  hide-details
                  flat
                  class="sort-select"
                  @update:model-value="filterRoadmaps"
                >
                  <template #prepend-inner>
                    <v-icon icon="mdi-sort" size="18" class="mr-1" />
                  </template>
                </v-select>
              </div>
            </div>

            <!-- 加载状态 -->
            <div v-if="loading" class="text-center py-12">
              <v-progress-circular indeterminate color="primary" size="64" />
              <p class="text-body-2 text-grey mt-4">加载中...</p>
            </div>

            <!-- 空状态 -->
            <div v-else-if="filteredRoadmaps.length === 0" class="text-center py-12">
              <v-card border rounded="xl" class="pa-12 empty-state">
                <v-icon icon="mdi-map-marker-path" size="80" color="grey-lighten-1" class="mb-4" />
                <h3 class="text-h6 text-grey-darken-2 mb-2">暂无学习路径</h3>
                <p class="text-body-2 text-grey mb-4">
                  {{ filterStatus === 'all' ? '还没有人创建学习路径' : '该筛选条件下暂无路径' }}
                </p>
                <v-btn
                  v-if="filterStatus === 'all'"
                  color="primary"
                  variant="flat"
                  rounded="lg"
                  @click="handleCreateRoadmap"
                >
                  <v-icon icon="mdi-plus" size="20" class="mr-1" />
                  创建第一条路径
                </v-btn>
              </v-card>
            </div>

            <!-- 路线图列表 -->
            <v-card
              v-for="roadmap in filteredRoadmaps"
              v-else
              :key="roadmap.id"
              border
              rounded="xl"
              class="roadmap-card mb-4"
              hover
              @click="handleGoToRoadmap(roadmap)"
            >
              <v-card-text class="pa-5">
                <div class="d-flex align-start">
                  <!-- 左侧：投票区域 -->
                  <div class="vote-section mr-4">
                    <v-btn
                      :color="roadmap.upvoted ? 'primary' : 'grey-lighten-1'"
                      :variant="roadmap.upvoted ? 'flat' : 'outlined'"
                      icon
                      size="small"
                      @click="handleVote(roadmap, $event)"
                    >
                      <v-icon :icon="roadmap.upvoted ? 'mdi-heart' : 'mdi-heart-outline'" />
                    </v-btn>
                    <div class="vote-count text-center mt-1">
                      <span
                        class="text-caption font-weight-bold"
                        :class="roadmap.upvoted ? 'text-primary' : 'text-grey-darken-2'"
                      >
                        {{ roadmap.vote }}
                      </span>
                    </div>
                  </div>

                  <!-- 中间：路径信息 -->
                  <div class="flex-grow-1">
                    <!-- 标签 -->
                    <div class="d-flex align-center mb-2">
                      <v-chip
                        v-if="roadmap.pinned"
                        color="warning"
                        size="x-small"
                        variant="flat"
                        class="mr-2"
                      >
                        <v-icon icon="mdi-pin" size="12" class="mr-1" />
                        置顶
                      </v-chip>
                      <v-chip v-if="roadmap.learning" color="success" size="x-small" variant="flat">
                        <v-icon icon="mdi-school" size="12" class="mr-1" />
                        学习中
                      </v-chip>
                    </div>

                    <!-- 描述 -->
                    <div class="text-body-1 text-grey-darken-4 mb-2">
                      {{ roadmap.description }}
                    </div>

                    <!-- 详细描述 -->
                    <p class="text-body-2 text-grey-darken-2 mb-3">
                      {{ roadmap.detail }}
                    </p>

                    <!-- 统计信息 -->
                    <div class="d-flex align-center gap-4 mb-3">
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-account" size="16" color="grey" class="mr-1" />
                        <span class="text-caption text-grey-darken-2">
                          {{ roadmap.creator.username }}
                        </span>
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-account-group" size="16" color="grey" class="mr-1" />
                        <span class="text-caption text-grey-darken-2">
                          {{ roadmap.learnerCount ?? 0 }} 人学习
                        </span>
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-comment-outline" size="16" color="grey" class="mr-1" />
                        <span class="text-caption text-grey-darken-2">
                          {{ roadmap.commentCount ?? 0 }} 评论
                        </span>
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-circle-medium" size="16" color="grey" class="mr-1" />
                        <span class="text-caption text-grey-darken-2">
                          {{ roadmap.nodeCount }} 个节点
                        </span>
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-clock-outline" size="16" color="grey" class="mr-1" />
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
                        <v-icon
                          :icon="roadmap.learning ? 'mdi-check' : 'mdi-play'"
                          size="16"
                          class="mr-1"
                        />
                        {{ roadmap.learning ? '正在学习' : '开始学习' }}
                      </v-btn>
                      <v-btn
                        color="grey-darken-2"
                        variant="outlined"
                        size="small"
                        rounded="lg"
                        @click="handleCopy(roadmap, $event)"
                      >
                        <v-icon icon="mdi-content-copy" size="16" class="mr-1" />
                        复制
                      </v-btn>
                    </div>
                  </div>

                  <!-- 右侧：路线图图标 -->
                  <div class="ml-4">
                    <v-icon icon="mdi-graph-outline" size="80" color="grey-lighten-2" />
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 右侧：说明卡片 -->
          <v-col cols="12" lg="4" class="pa-0 pl-16">
            <v-card rounded="xl" class="info-card sticky-card no-border">
              <v-card-title class="py-6 px-0 pb-4">
                <div class="d-flex align-center">
                  <v-icon icon="mdi-information" color="primary" class="mr-2" />
                  <span class="text-h6 font-weight-bold">学习路径说明</span>
                </div>
              </v-card-title>

              <v-card-text class="py-6 px-0 pt-0">
                <!-- 什么是学习路径 -->
                <div class="info-section mb-5">
                  <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-2">
                    <v-icon icon="mdi-map-marker-path" size="18" color="primary" class="mr-1" />
                    什么是学习路径？
                  </h3>
                  <p class="text-body-2 text-grey-darken-2">
                    学习路径是由社区成员创建的结构化学习计划，帮助你系统地掌握某个职业所需的技能和知识。
                  </p>
                </div>

                <v-divider class="mb-5" />

                <!-- 如何使用 -->
                <div class="info-section mb-5">
                  <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-3">
                    <v-icon icon="mdi-lightbulb" size="18" color="warning" class="mr-1" />
                    如何使用？
                  </h3>
                  <div class="d-flex align-start mb-2">
                    <v-icon
                      icon="mdi-numeric-1-circle"
                      size="20"
                      color="primary"
                      class="mr-2 mt-1"
                    />
                    <div>
                      <p class="text-body-2 font-weight-medium text-grey-darken-3">浏览和筛选</p>
                      <p class="text-caption text-grey-darken-1">
                        使用筛选和搜索功能找到适合你的路径
                      </p>
                    </div>
                  </div>
                  <div class="d-flex align-start mb-2">
                    <v-icon
                      icon="mdi-numeric-2-circle"
                      size="20"
                      color="primary"
                      class="mr-2 mt-1"
                    />
                    <div>
                      <p class="text-body-2 font-weight-medium text-grey-darken-3">开始学习</p>
                      <p class="text-caption text-grey-darken-1">点击"开始学习"追踪你的学习进度</p>
                    </div>
                  </div>
                  <div class="d-flex align-start mb-2">
                    <v-icon
                      icon="mdi-numeric-3-circle"
                      size="20"
                      color="primary"
                      class="mr-2 mt-1"
                    />
                    <div>
                      <p class="text-body-2 font-weight-medium text-grey-darken-3">查看详情</p>
                      <p class="text-caption text-grey-darken-1">
                        点击路径卡片查看完整的学习路线图
                      </p>
                    </div>
                  </div>
                  <div class="d-flex align-start">
                    <v-icon
                      icon="mdi-numeric-4-circle"
                      size="20"
                      color="primary"
                      class="mr-2 mt-1"
                    />
                    <div>
                      <p class="text-body-2 font-weight-medium text-grey-darken-3">参与互动</p>
                      <p class="text-caption text-grey-darken-1">点赞优质路径，评论分享学习经验</p>
                    </div>
                  </div>
                </div>

                <v-divider class="mb-5" />

                <!-- 创建自己的路径 -->
                <div class="info-section">
                  <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-2">
                    <v-icon icon="mdi-plus-circle" size="18" color="success" class="mr-1" />
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
                    @click="handleCreateRoadmap"
                  >
                    <v-icon icon="mdi-plus" size="18" class="mr-1" />
                    创建新路径
                  </v-btn>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useFetch } from '@/composables'
import { professionApi, roadmapApi } from '@/api'
import type { Profession } from '@/types/profession'
import type { Roadmap } from '@/types/roadmap'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'

const router = useRouter()
const route = useRoute()

// 从路由获取职业ID
const careerId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : 0
})

// 使用 useFetch 加载职业详情
const {
  data: career,
  loading,
  error: fetchError,
} = useFetch<Profession>({
  fetchFn: () => professionApi.getProfession(careerId.value),
  immediate: true,
  defaultValue: null,
})

// 使用 useFetch 加载路线图列表
const {
  data: roadmapsData,
  loading: loadingRoadmaps,
  error: roadmapsError,
} = useFetch<Roadmap[]>({
  fetchFn: () => roadmapApi.getProfessionRoadmaps(careerId.value),
  immediate: true,
  defaultValue: [],
})

const allRoadmaps = computed(() => roadmapsData.value ?? [])

// 状态管理
const searchText = ref('')
const filterStatus = ref<string>('all')
const sortBy = ref<string>('latest')

// 排序选项
const sortOptions = [
  { title: '最新发布', value: 'latest' },
  { title: '最受欢迎', value: 'popular' },
  { title: '学习人数', value: 'learners' },
]

const error = computed(() => (fetchError.value ? '加载职业信息失败' : null))

// 筛选后的路线图
const filteredRoadmaps = computed(() => {
  let filtered = [...allRoadmaps.value]

  // 状态筛选
  if (filterStatus.value === 'pinned') {
    filtered = filtered.filter((r) => r.pinned)
  } else if (filterStatus.value === 'learning') {
    filtered = filtered.filter((r) => r.learning)
  } else if (filterStatus.value === 'upvoted') {
    filtered = filtered.filter((r) => r.upvoted)
  }

  // 搜索筛选
  if (searchText.value.trim()) {
    const searchLower = searchText.value.toLowerCase()
    filtered = filtered.filter((r) => r.description?.toLowerCase().includes(searchLower))
  }

  // 排序
  if (sortBy.value === 'latest') {
    filtered.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  } else if (sortBy.value === 'popular') {
    filtered.sort((a, b) => b.vote - a.vote)
  } else if (sortBy.value === 'learners') {
    filtered.sort((a, b) => (b.learnerCount ?? 0) - (a.learnerCount ?? 0))
  }

  // 置顶的始终在前面
  filtered.sort((a, b) => {
    if (a.pinned && !b.pinned) return -1
    if (!a.pinned && b.pinned) return 1
    return 0
  })

  return filtered
})

const roadmapsCount = computed(() => allRoadmaps.value.length)

// 筛选和排序
const filterRoadmaps = (): void => {
  // 触发 filteredRoadmaps 重新计算
}

// 获取职业图标
const getCareerIcon = () => {
  return 'mdi-briefcase-outline'
}

// 格式化数字
const formatNumber = (num?: number) => {
  if (!num) return '0'
  return num.toLocaleString()
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

// 返回职业中心
const handleBack = (): void => {
  void router.push('/career')
}

// 创建新路径
const handleCreateRoadmap = (): void => {
  void router.push(`/career/${careerId.value}/roadmap/create`)
}

// 打开路径详情
const handleGoToRoadmap = (roadmap: { id: number }): void => {
  void router.push(`/roadmap/${careerId.value}/detail/${roadmap.id}`)
}

// 投票
const handleVote = (roadmap: { upvoted: boolean; vote: number }, event: Event): void => {
  event.stopPropagation()
  roadmap.upvoted = !roadmap.upvoted
  roadmap.vote += roadmap.upvoted ? 1 : -1
}

// 开始学习
const handleStartLearning = (roadmap: { learning: boolean }, event: Event): void => {
  event.stopPropagation()
  roadmap.learning = !roadmap.learning
}

// 复制路径
const handleCopy = (roadmap: { id: number }, event: Event): void => {
  event.stopPropagation()
  console.log('复制路径:', roadmap.id)
  void router.push(`/roadmap/${careerId.value}/create?copy=${roadmap.id}`)
}
</script>

<style scoped>
.career-detail-page {
  /* 使用 DefaultLayout 的默认 padding */
}

.profession-header,
.filter-card {
  background-color: transparent;
}

.info-card {
  background-color: #ffffff;
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
  max-width: 300px;
}

.sort-select {
  max-width: 180px;
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
  background-color: #ffffff;
  border: 1px solid #edeff1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.roadmap-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: rgb(var(--v-theme-primary));
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
  background-color: #ffffff;
  border: 1px solid #edeff1;
}

/* 移动端 */
@media (max-width: 1280px) {
  .career-detail-page {
    /* 使用 DefaultLayout 的默认 padding */
  }
}

@media (max-width: 960px) {
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
