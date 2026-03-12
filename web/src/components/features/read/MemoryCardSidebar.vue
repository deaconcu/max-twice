<template>
  <div class="memory-card-section">
    <!-- 模块头部 -->
    <div class="sidebar-header">
      <v-icon icon="mdi-cards-outline" size="18" class="mr-2"></v-icon>
      <span class="sidebar-title">本文的记忆卡片</span>
      <v-tooltip location="bottom" max-width="280">
        <template #activator="{ props: tooltipProps }">
          <v-icon
            v-bind="tooltipProps"
            icon="mdi-help-circle-outline"
            size="16"
            color="grey-lighten-1"
            class="ml-1"
            style="cursor: help"
          ></v-icon>
        </template>
        <span>基于本文内容创建的记忆卡片组，帮助你提取和巩固文章中的知识点。你也可以创建自己的卡片组分享给其他学习者。</span>
      </v-tooltip>
      <v-spacer></v-spacer>

      <!-- 刷新按钮 -->
      <v-tooltip text="刷新" location="top">
        <template #activator="{ props: tooltipProps }">
          <v-btn
            v-bind="tooltipProps"
            icon
            size="x-small"
            variant="text"
            color="grey-darken-2"
            :loading="loading"
            class="me-1"
            @click="loadDecks(true)"
          >
            <v-icon icon="mdi-refresh" size="16"></v-icon>
          </v-btn>
        </template>
      </v-tooltip>

      <v-btn
        color="success"
        variant="flat"
        rounded="lg"
        size="small"
        prepend-icon="mdi-plus"
        @click="emit('createDeck')"
      >
        创建
      </v-btn>
    </div>

    <!-- 排序和筛选控件 -->
    <div class="filter-controls pb-2 pt-2">
      <div class="d-flex align-center justify-space-between">
        <!-- 左侧：筛选选项 -->
        <div class="d-flex gap-1">
          <v-tooltip text="全部" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                :color="!showAuthorOnly && !showMyOnly ? 'primary' : 'grey-darken-1'"
                :variant="!showAuthorOnly && !showMyOnly ? 'tonal' : 'text'"
                size="x-small"
                rounded="lg"
                icon="mdi-all-inclusive"
                @click="handleShowAll"
              />
            </template>
          </v-tooltip>
          <v-tooltip text="只看本文作者" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                :color="showAuthorOnly ? 'primary' : 'grey-darken-1'"
                :variant="showAuthorOnly ? 'tonal' : 'text'"
                size="x-small"
                rounded="lg"
                icon="mdi-account-edit"
                @click="handleFilterToggle"
              />
            </template>
          </v-tooltip>
          <v-tooltip text="我提交的" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                :color="showMyOnly ? 'primary' : 'grey-darken-1'"
                :variant="showMyOnly ? 'tonal' : 'text'"
                size="x-small"
                rounded="lg"
                icon="mdi-account"
                @click="handleMyFilterToggle"
              />
            </template>
          </v-tooltip>
        </div>

        <!-- 右侧：排序选项 -->
        <div class="d-flex gap-1">
          <v-tooltip text="热度排序" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                :color="sortBy === 'score' ? 'primary' : 'grey-darken-1'"
                :variant="sortBy === 'score' ? 'tonal' : 'text'"
                size="x-small"
                rounded="lg"
                icon="mdi-trending-up"
                @click="handleSort('score')"
              />
            </template>
          </v-tooltip>
          <v-tooltip text="时间排序" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                :color="sortBy === 'createdAt' ? 'primary' : 'grey-darken-1'"
                :variant="sortBy === 'createdAt' ? 'tonal' : 'text'"
                size="x-small"
                rounded="lg"
                icon="mdi-clock-outline"
                @click="handleSort('createdAt')"
              />
            </template>
          </v-tooltip>
        </div>
      </div>
    </div>

    <!-- 卡片组列表 -->
    <div
      class="pb-4 pt-2 deck-list"
      :class="{ 'deck-list-hover': isHovering }"
      style="max-height: 500px; overflow-y: auto"
      @mouseenter="isHovering = true"
      @mouseleave="isHovering = false"
    >
      <!-- 空状态 -->
      <div v-if="decks.length === 0 && !loading" class="text-center pa-6">
        <v-icon icon="mdi-cards-outline" size="48" color="grey-lighten-2" class="mb-3"></v-icon>
        <h4 class="text-body-1 font-weight-medium text-grey-darken-2 mb-2">暂无卡片组</h4>
        <p class="text-body-2 text-grey-darken-1 mb-4">基于本文创建记忆卡片，帮助巩固知识点</p>
        <v-btn color="primary" variant="tonal" rounded="lg" size="small" @click="emit('createDeck')">
          创建卡片组
        </v-btn>
      </div>

      <!-- 卡片组列表 -->
      <div v-else>
        <div
          v-for="deck in decks"
          :key="deck.id"
          class="deck-item"
          @click="viewDeckDetail(deck)"
        >
          <!-- 第一行：头像 + 用户名 + 状态标签 + 点赞 + 卡片数 -->
          <div class="d-flex align-center justify-space-between">
            <div class="d-flex align-center">
              <UserAvatar
                :name="deck.creator?.name || '匿名用户'"
                :avatar-url="deck.creator?.avatar"
                size="22"
                rounded="circle"
                class="mr-2"
              />
              <span class="text-body-2 font-weight-medium text-grey-darken-3">
                {{ deck.creator?.name || '匿名用户' }}
              </span>
              <v-chip
                v-if="showMyOnly && deck.creatorId === currentUserId"
                size="x-small"
                :color="getStateColor(deck.state)"
                variant="flat"
                class="ml-2"
              >
                {{ getStateText(deck.state) }}
              </v-chip>
            </div>
            <div class="d-flex align-center ga-3">
              <span
                v-if="deck.creator?.id !== currentUserId"
                class="d-flex align-center text-body-2 like-btn"
                :class="{ 'text-error': deck.hasLiked, 'text-grey-darken-1': !deck.hasLiked }"
                @click.stop="handleUpvote(deck, $event)"
              >
                <v-icon
                  :icon="deck.hasLiked ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
                  size="16"
                  class="mr-1"
                ></v-icon>
                {{ deck.likeCount || 0 }}
              </span>
              <span
                v-else
                class="d-flex align-center text-body-2 text-grey-lighten-1"
              >
                <v-icon icon="mdi-thumb-up-outline" size="16" class="mr-1"></v-icon>
                {{ deck.likeCount || 0 }}
              </span>
              <span class="d-flex align-center text-body-2 text-grey-darken-1">
                <v-icon icon="mdi-cards-outline" size="16" class="mr-1"></v-icon>
                {{ deck.cardCount }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore && !loading" class="text-center pa-4">
        <v-btn variant="text" color="primary" @click="loadMore">加载更多</v-btn>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="text-center pa-4">
        <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { memoryApi } from '@/api'
import { useMutation } from '@/composables'
import { useUserStore } from '@/stores'
import UserAvatar from '@/components/common/UserAvatar.vue'
import type { MemoryCardDeck } from '@/types/memory'

interface Props {
  postId: number
}

interface Emits {
  (e: 'createDeck'): void
  (e: 'addDeck', deck: MemoryCardDeck): void
  (e: 'viewDeck', deck: MemoryCardDeck): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const userStore = useUserStore()

const sortBy = ref<'score' | 'createdAt' | 'upvoteCount'>('score')
const showAuthorOnly = ref(false)
const showMyOnly = ref(false)
const decks = ref<MemoryCardDeck[]>([])
const loading = ref(false)
const hasMore = ref(true)
const lastId = ref<number>(0)
const lastScore = ref<number>(0)
const isHovering = ref(false)

const currentUserId = computed(() => userStore.currentUser?.id)

// 加载卡片组列表
const loadDecks = async (reset = false) => {
  if (reset) {
    decks.value = []
    lastId.value = 0
    lastScore.value = 0
    hasMore.value = true
  }

  if (loading.value || !hasMore.value) return

  try {
    loading.value = true

    const queryParams: any = {
      sortBy: sortBy.value,
      sortOrder: 'desc' as const,
      limit: 20,
    }

    // 只有在非第一页时才添加 lastScore 和 lastId
    if (lastId.value > 0) {
      queryParams.lastScore = lastScore.value
      queryParams.lastId = lastId.value
    }

    let response
    if (showAuthorOnly.value) {
      response = await memoryApi.getPostCreatorDeck(props.postId, queryParams)
    } else if (showMyOnly.value) {
      response = await memoryApi.getMyPostDeck(props.postId, queryParams)
    } else {
      response = await memoryApi.getPostPublicDecks(props.postId, queryParams)
    }

    if (response.data?.items) {
      const items = response.data.items
      decks.value = reset ? items : [...decks.value, ...items]

      if (items.length > 0) {
        const lastItem = items[items.length - 1]
        lastId.value = lastItem.id
        lastScore.value = lastItem.likeCount || 0
      }

      hasMore.value = items.length >= 20
    } else {
      hasMore.value = false
    }
  } catch (error) {
    console.error('Failed to load memory decks:', error)
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  loadDecks(false)
}

// 监听筛选条件变化，重新加载数据
watch([sortBy, showAuthorOnly, showMyOnly], () => {
  loadDecks(true)
})

// 使用 useMutation 处理点赞
const { execute: upvoteDeck } = useMutation((deckId: number) => memoryApi.upvoteDeck(deckId), {
  showToast: false,
  onSuccess: (result, deckId) => {
    const deck = decks.value.find((d) => d.id === deckId)
    if (deck && result) {
      deck.hasLiked = result.liked
      deck.likeCount = result.likeCount
    }
  },
})

const handleSort = (newSortBy: typeof sortBy.value) => {
  sortBy.value = newSortBy
}

const handleFilterToggle = () => {
  showAuthorOnly.value = !showAuthorOnly.value
  if (showAuthorOnly.value) {
    showMyOnly.value = false
  }
}

const handleMyFilterToggle = () => {
  showMyOnly.value = !showMyOnly.value
  if (showMyOnly.value) {
    showAuthorOnly.value = false
  }
}

const handleShowAll = () => {
  showAuthorOnly.value = false
  showMyOnly.value = false
}

const viewDeckDetail = (deck: MemoryCardDeck) => {
  emit('viewDeck', deck)
}

// 处理点赞
const handleUpvote = async (deck: MemoryCardDeck, event: Event) => {
  event.stopPropagation()
  await upvoteDeck(deck.id)
}

// 切换到"我提交的"标签
const switchToMyDecks = () => {
  handleMyFilterToggle()
}

// 获取状态显示文本
const getStateText = (state: number) => {
  switch (state) {
    case 1:
      return '审核中'
    case 2:
      return '已通过'
    case 3:
      return '已拒绝'
    case 4:
      return '已屏蔽'
    default:
      return '未知'
  }
}

// 获取状态颜色
const getStateColor = (state: number) => {
  switch (state) {
    case 1:
      return 'warning'
    case 2:
      return 'success'
    case 3:
      return 'grey'
    case 4:
      return 'error'
    default:
      return 'grey'
  }
}

// 暴露方法给父组件
defineExpose({
  loadDecks,
  switchToMyDecks,
})

// 初始加载
loadDecks(true)
</script>

<style scoped>
.memory-card-section {
  /* 无边框和背景 */
}

.sidebar-header {
  display: flex;
  align-items: center;
  padding-bottom: 12px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.deck-item {
  padding: 12px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.deck-item:hover {
  background-color: rgba(var(--v-theme-surface-variant), 0.5);
}

.deck-desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.like-btn {
  cursor: pointer;
  padding: 4px 6px;
  margin: -4px -6px;
  border-radius: 4px;
  transition: background-color 0.15s ease;
}

.like-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

/* 滚动条样式 - hover 时显示 */
.deck-list::-webkit-scrollbar {
  width: 2px;
}

.deck-list::-webkit-scrollbar-track {
  background: transparent;
}

.deck-list::-webkit-scrollbar-thumb {
  background-color: transparent;
  border-radius: 2px;
}

.deck-list-hover::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
}

.deck-list-hover::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}
</style>
