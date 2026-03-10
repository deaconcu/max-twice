<template>
  <DefaultLayout>
    <div class="role-detail-page">
      <!-- 加载状态 -->
      <LoadingSpinner v-if="loading" />

      <!-- 错误状态 -->
      <v-alert v-else-if="error" type="error" variant="tonal" class="mb-6">
        {{ error }}
      </v-alert>

      <!-- 内容区 -->
      <div v-else-if="role" class="content-wrapper">
        <!-- 职业信息头部 -->
        <div class="profession-header mb-6 mb-md-8 pa-0">
          <div
            class="d-flex flex-column flex-md-row align-start align-md-center justify-space-between ga-4"
          >
            <!-- 左侧：职业信息 -->
            <div class="flex-grow-1" style="min-width: 0">
              <div class="d-flex align-center mb-4 mb-md-5 role-title-row">
                <!-- 职业图标和标题 -->
                <div class="d-flex align-center" style="min-width: 0">
                  <v-avatar
                    color="primary"
                    :size="$vuetify.display.mobile ? 40 : 48"
                    class="mr-3 flex-shrink-0"
                  >
                    <v-icon
                      :icon="getRoleIcon()"
                      color="white"
                      :size="$vuetify.display.mobile ? 20 : 24"
                    />
                  </v-avatar>
                  <h1 class="text-h5 text-md-h4 font-weight-bold text-grey-darken-4 text-truncate">
                    {{ role.name }}
                  </h1>
                </div>
              </div>

              <!-- 简介 -->
              <div>
                <p class="text-body-2 text-grey-darken-2 mb-0">
                  {{ role.description }}
                </p>
              </div>
            </div>

            <!-- 右侧：操作按钮 -->
            <div class="d-flex align-center ga-4 flex-shrink-0">
              <v-tooltip location="bottom">
                <template #activator="{ props }">
                  <v-btn
                    v-bind="props"
                    :icon="role.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
                    :color="role.bookmarked ? 'primary' : 'grey-darken-1'"
                    variant="tonal"
                    density="comfortable"
                    :size="$vuetify.display.mobile ? 'default' : 'large'"
                    rounded="lg"
                    @click="handleToggleBookmark"
                  />
                </template>
                {{ role.bookmarked ? '取消收藏' : '收藏职业' }}
              </v-tooltip>
              <v-btn
                color="primary"
                variant="flat"
                :size="$vuetify.display.mobile ? 'default' : 'large'"
                rounded="lg"
                @click="handleCreateRoadmap"
              >
                <v-icon icon="mdi-plus" size="20" class="mr-1" />
                创建路径
              </v-btn>
            </div>
          </div>
        </div>

        <!-- 路径列表 -->
        <div class="content-layout">
          <div class="main-content">
            <!-- 筛选和搜索 -->
            <div class="filter-card mb-2">
              <div
                class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between ga-3 ga-sm-4"
              >
                <!-- 状态筛选 -->
                <v-btn-toggle
                  v-model="filterStatus"
                  variant="text"
                  color="primary"
                  rounded="lg"
                  density="comfortable"
                  mandatory
                  class="filter-toggle"
                  @update:model-value="filterRoadmaps"
                >
                  <v-btn value="all" :size="$vuetify.display.mobile ? 'small' : 'default'">
                    <v-icon icon="mdi-format-list-bulleted" size="16" class="mr-1" />
                    <span class="d-none d-sm-inline">全部</span>
                  </v-btn>
                  <v-btn value="learning" :size="$vuetify.display.mobile ? 'small' : 'default'">
                    <v-icon icon="mdi-school" size="16" class="mr-1" />
                    <span class="d-none d-sm-inline">学习中</span>
                  </v-btn>
                </v-btn-toggle>

                <!-- 排序（仅在"全部"状态下显示） -->
                <v-select
                  v-if="filterStatus === 'all'"
                  v-model="sortBy"
                  :items="sortOptions"
                  variant="solo"
                  density="comfortable"
                  hide-details
                  flat
                  class="sort-select"
                  @update:model-value="filterRoadmaps"
                >
                </v-select>
              </div>
            </div>

            <!-- 加载状态 -->
            <LoadingSpinner v-if="loadingRoadmaps" />

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
            <v-infinite-scroll v-else-if="filteredRoadmaps.length > 0" :items="filteredRoadmaps" @load="onLoadMore">
              <template v-for="roadmap in filteredRoadmaps" :key="roadmap.id">
                <v-card
                  border
                  rounded="xl"
                  class="roadmap-card mb-4"
                  hover
                >
              <v-card-text class="pa-4 pa-sm-5">
                <div class="d-flex align-start">
                  <!-- 左侧：投票区域 -->
                  <div class="vote-section mr-3 mr-sm-4">
                    <v-btn
                      :color="roadmap.liked ? 'primary' : 'grey-lighten-1'"
                      :variant="roadmap.liked ? 'flat' : 'outlined'"
                      :disabled="roadmap.creator?.id === currentUserId"
                      icon
                      size="small"
                      @click="handleVote(roadmap)"
                    >
                      <v-icon :icon="roadmap.liked ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'" />
                    </v-btn>
                    <div class="vote-count text-center mt-1">
                      <span
                        class="text-caption font-weight-bold"
                        :class="roadmap.liked ? 'text-primary' : 'text-grey-darken-2'"
                      >
                        {{ roadmap.likeCount }}
                      </span>
                    </div>
                  </div>

                  <!-- 右侧：内容和操作区域 -->
                  <div class="flex-grow-1">
                    <!-- 内容区域（可点击跳转） -->
                    <div class="d-flex align-start">
                      <div class="content-area flex-grow-1" @click="handleGoToRoadmap(roadmap)">
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
                    <div class="d-flex flex-wrap align-center gap-4 gap-sm-6 gap-md-8 mb-3">
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-account" size="16" color="grey" class="mr-2" />
                        <span class="text-caption text-grey-darken-2">
                          {{ roadmap.creator?.name || '未知用户' }}
                        </span>
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-account-group" size="16" color="grey" class="mr-2" />
                        <span class="text-caption text-grey-darken-2">
                          {{ roadmap.learnerCount ?? 0 }}
                          <span class="d-none d-sm-inline">人学习</span>
                        </span>
                      </div>
                      <div class="d-flex align-center d-none d-sm-flex">
                        <v-icon icon="mdi-comment-outline" size="16" color="grey" class="mr-2" />
                        <span class="text-caption text-grey-darken-2">
                          {{ roadmap.commentCount ?? 0 }} 评论
                        </span>
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-circle-medium" size="16" color="grey" class="mr-2" />
                        <span class="text-caption text-grey-darken-2">
                          {{ roadmap.nodeCount }}
                          <span class="d-none d-sm-inline">个</span>节点
                        </span>
                      </div>
                      <div class="d-flex align-center d-none d-md-flex">
                        <v-icon icon="mdi-clock-outline" size="16" color="grey" class="mr-2" />
                        <span class="text-caption text-grey-darken-2">
                          {{ getTimeDisplay(roadmap.createdAt) }}
                        </span>
                      </div>
                    </div>
                      </div>

                      <!-- 右侧：路线图图标 -->
                      <div class="ml-3 ml-sm-4 d-none d-sm-block roadmap-icon">
                        <v-icon
                          icon="mdi-graph-outline"
                          :size="$vuetify.display.mdAndUp ? 80 : 60"
                          color="grey-lighten-2"
                        />
                      </div>
                    </div>

                    <!-- 操作按钮区域（独立，不触发跳转） -->
                    <div class="d-flex align-center gap-3 mt-3">
                    <v-btn
                      :color="roadmap.learning ? 'success' : 'primary'"
                      :variant="roadmap.learning ? 'outlined' : 'flat'"
                      size="small"
                      rounded="lg"
                      @click="handleStartLearning(roadmap)"
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
                      variant="tonal"
                      size="small"
                      rounded="lg"
                      @click="handleCopy(roadmap)"
                    >
                      <v-icon icon="mdi-content-copy" size="16" class="mr-1" />
                      复制
                    </v-btn>
                  </div>
                </div>
              </div>
              </v-card-text>
            </v-card>
              </template>

              <!-- 自定义底部提示 -->
              <template #empty>
                <div class="text-center py-10">
                  <p class="text-body-2 text-grey">已经到底了</p>
                </div>
              </template>
            </v-infinite-scroll>
          </div>

          <!-- 右侧：说明卡片 -->
          <div class="right-sidebar d-none d-lg-block">
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
          </div>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useFetch, useMutation } from '@/composables'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useUserStore } from '@/stores'
import { professionApi, roadmapApi, progressApi, upvoteApi, bookmarkApi } from '@/api'
import { ObjectType, VoteType } from '@/enums'
import type { Profession } from '@/types/profession'
import type { Roadmap } from '@/types/roadmap'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 当前用户ID
const currentUserId = computed(() => userStore.currentUser?.id)

// 从路由获取职业ID
const roleId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : 0
})

// 使用 useFetch 加载职业详情
const {
  data: role,
  loading,
  error: fetchError,
} = useFetch<Profession>({
  fetchFn: () => professionApi.getProfession(roleId.value),
  immediate: true,
  defaultValue: null,
})

// 状态管理
const searchText = ref('')
const filterStatus = ref<string>('all')
const sortBy = ref<string>('score')

// 排序选项
const sortOptions = [
  { title: '最新发布', value: 'latest' },
  { title: '综合排序', value: 'score' },
]

const error = computed(() => (fetchError.value ? '加载职业信息失败' : null))

// 使用无限滚动加载路线图
const {
  items: allRoadmaps,
  loading: loadingRoadmaps,
  hasMore,
  loadMore: loadMoreRoadmaps,
  reset: resetRoadmaps,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const response = await roadmapApi.getProfessionRoadmaps(roleId.value, params.lastId, sortBy.value)
    return {
      code: response.code,
      data: response.data || [],
      message: response.message || '',
      hasMore: response.data?.length === 20, // 假设每页20条
    }
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: { lastId: undefined },
})

// 监听排序变化，重置列表
watch(sortBy, () => {
  resetRoadmaps()
  loadMoreRoadmaps({ done: () => {} })
})

// 学习中的路线图（单独接口，不分页）
const learningRoadmaps = ref<any[]>([])

// 监听筛选状态变化，加载学习中数据
watch(filterStatus, async (newStatus) => {
  if (newStatus === 'learning' && learningRoadmaps.value.length === 0) {
    loadingRoadmaps.value = true
    try {
      const response = await progressApi.getLearningRoadmapsByProfession(roleId.value)
      if (response.code === 200) {
        learningRoadmaps.value = response.data || []
      }
    } finally {
      loadingRoadmaps.value = false
    }
  } else if (newStatus === 'all' && allRoadmaps.value.length === 0) {
    loadMoreRoadmaps({ done: () => {} })
  }
})

// 首次加载数据
onMounted(() => {
  if (allRoadmaps.value.length === 0) {
    loadMoreRoadmaps({ done: () => {} })
  }
})

// 加载更多
const onLoadMore = async ({ done }: { done: (status: string) => void }) => {
  await loadMoreRoadmaps({
    done: () => {
      done(hasMore.value ? 'ok' : 'empty')
    },
  })
}

// 筛选后的路线图
const filteredRoadmaps = computed(() => {
  // 学习中状态：使用单独接口的数据
  if (filterStatus.value === 'learning') {
    return learningRoadmaps.value || []
  }

  // 全部状态：使用滚动加载的数据
  let filtered = [...allRoadmaps.value]

  // 搜索筛选
  if (searchText.value.trim()) {
    const searchLower = searchText.value.toLowerCase()
    filtered = filtered.filter((r) => r.description?.toLowerCase().includes(searchLower))
  }

  return filtered
})

const roadmapsCount = computed(() => allRoadmaps.value.length)

// 筛选和排序
const filterRoadmaps = (): void => {
  // 触发 filteredRoadmaps 重新计算
}

// 获取职业图标
const getRoleIcon = () => {
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

// 创建新路径
const handleCreateRoadmap = (): void => {
  void router.push(`/role/${roleId.value}/roadmap/create`)
}

// 切换职业收藏状态
const { execute: executeToggleBookmark, loading: bookmarking } = useMutation(
  () => bookmarkApi.toggle('profession', roleId.value),
  {
    successMessage: '',
    showToast: false,
  }
)

const handleToggleBookmark = async () => {
  if (!role.value) return

  const result = await executeToggleBookmark()
  if (result !== null && role.value) {
    role.value.bookmarked = result
  }
}

// 打开路径详情
const handleGoToRoadmap = (roadmap: { id: number }): void => {
  void router.push(`/roadmap/${roadmap.id}`)
}

// 投票
const { execute: toggleUpvote } = useMutation(
  ({ roadmapId }: { roadmapId: number }) =>
    upvoteApi.upvote(roadmapId, ObjectType.ROADMAP, VoteType.NORMAL),
  { showToast: false }
)

const handleVote = async (
  roadmap: { id: number; liked: boolean; likeCount: number }
): Promise<void> => {
  console.log('[点赞前] roadmap:', roadmap.id, 'likeCount:', roadmap.likeCount)
  const result = await toggleUpvote({ roadmapId: roadmap.id })
  console.log('[点赞后] result:', result)

  // 调用成功，更新状态（使用后端返回的点赞数，而不是手动计算）
  if (result) {
    roadmap.liked = result.liked || false
    roadmap.likeCount = result.likeCount || 0
    console.log('[更新后] liked:', roadmap.liked, 'likeCount:', roadmap.likeCount)
  }
  // 调用失败，不做任何更新
}

// 开始学习路线图的 mutation
const { execute: startRoadmapMutation } = useMutation(progressApi.startRoadmap, {
  successMessage: '已开始学习',
  showToast: true,
})

// 取消学习路线图的 mutation
const { execute: cancelRoadmapMutation } = useMutation(progressApi.cancelRoadmap, {
  successMessage: '已取消学习',
  showToast: true,
})

// 开始学习
const handleStartLearning = async (roadmap: { id: number; learning: boolean }): Promise<void> => {
  try {
    if (roadmap.learning) {
      // 取消学习
      const result = await cancelRoadmapMutation(roadmap.id)
      if (result) {
        roadmap.learning = result.learning
      }
    } else {
      // 开始学习
      const result = await startRoadmapMutation(roadmap.id)
      if (result) {
        roadmap.learning = result.learning
      }
    }
  } catch (error) {
    console.error('操作失败:', error)
  }
}

// 复制路径
const handleCopy = (roadmap: { id: number }): void => {
  console.log('复制路径:', roadmap.id)
  void router.push(`/role/${roleId.value}/roadmap/create?copy=${roadmap.id}`)
}
</script>

<style scoped>
.role-detail-page {
  /* 使用 DefaultLayout 的默认 padding */
}

/* 宽屏时向左延伸，让后退按钮露出到页面外 */
@media (min-width: 1800px) {
  .role-title-row {
    margin-left: -56px;
  }
}

.profession-header,
.filter-card {
  background-color: transparent;
}

/* 内容布局 */
.content-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

@media (min-width: 1280px) {
  .content-layout {
    flex-direction: row;
    gap: 48px;
  }
}

.main-content {
  flex: 1;
  min-width: 0;
}

/* 右侧信息栏 */
.right-sidebar {
  width: 320px;
  flex-shrink: 0;
}

.info-card {
  background-color: rgb(var(--v-theme-surface));
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
  background-color: rgba(var(--v-theme-on-surface), 0.1);
  border-radius: 2px;
}

.sticky-card::-webkit-scrollbar-thumb:hover {
  background-color: rgba(var(--v-theme-on-surface), 0.2);
}

/* 筛选切换按钮 */
.filter-toggle {
  width: 100%;
}

@media (min-width: 600px) {
  .filter-toggle {
    width: auto;
  }
}

.sort-select {
  width: 100%;
  max-width: 100%;
}

@media (min-width: 600px) {
  .sort-select {
    width: auto;
    max-width: 127px;
  }
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
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-outline));
  transition: all 0.2s ease;
}

.roadmap-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: rgb(var(--v-theme-primary));
}

.content-area {
  cursor: pointer;
  border-radius: 8px;
  padding: 4px;
  margin: -4px;
  transition: background-color 0.2s;
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
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-outline));
}

/* 无限滚动底部文本样式 */
:deep(.v-infinite-scroll__side) {
  font-size: 14px;
  color: rgb(var(--v-theme-grey));
  opacity: 0.6;
}

/* 移动端 */
@media (max-width: 960px) {
  .gap-3 {
    gap: 8px;
  }

  .gap-4 {
    gap: 12px;
  }
}
</style>
