<template>
  <div class="memory-card-section">
    <!-- 模块头部 -->
    <div class="sidebar-header">
      <v-icon icon="mdi-cards-outline" size="18" class="mr-2"></v-icon>
      <span class="sidebar-title">{{ t('memoryCardSidebar.title') }}</span>
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
        <span>{{ t('memoryCardSidebar.tooltip') }}</span>
      </v-tooltip>
      <v-spacer></v-spacer>

      <!-- 刷新按钮 -->
      <v-tooltip :text="t('common.refresh')" location="top">
        <template #activator="{ props: tooltipProps }">
          <v-btn
            v-bind="tooltipProps"
            icon
            size="x-small"
            variant="text"
            color="grey-darken-2"
            :loading="loading"
            class="me-1"
            @click="activeQuery.refetch()"
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
        {{ t('common.create') }}
      </v-btn>
    </div>

    <!-- 排序和筛选控件 -->
    <div class="filter-controls pb-2 pt-2">
      <div class="d-flex align-center justify-space-between">
        <!-- 左侧：筛选选项 -->
        <div class="d-flex gap-1">
          <v-tooltip :text="t('common.all')" location="top">
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
          <v-tooltip :text="t('memoryCardSidebar.authorOnly')" location="top">
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
          <v-tooltip :text="t('memoryCardSidebar.mySubmitted')" location="top">
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
          <v-tooltip :text="t('memoryCardSidebar.sortByHot')" location="top">
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
          <v-tooltip :text="t('memoryCardSidebar.sortByTime')" location="top">
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
        <h4 class="text-body-1 font-weight-medium text-grey-darken-2 mb-2">
          {{ t('memoryCardSidebar.noDecks') }}
        </h4>
        <p class="text-body-2 text-grey-darken-1 mb-4">{{ t('memoryCardSidebar.noDecksHint') }}</p>
        <v-btn
          color="primary"
          variant="tonal"
          rounded="lg"
          size="small"
          @click="emit('createDeck')"
        >
          {{ t('memoryCardSidebar.createDeck') }}
        </v-btn>
      </div>

      <!-- 卡片组列表 -->
      <div v-else>
        <div v-for="deck in decks" :key="deck.id" class="deck-item" @click="viewDeckDetail(deck)">
          <div class="d-flex align-stretch">
            <!-- 左侧：头像 + 作者 · 时间 / 点赞 -->
            <div class="d-flex flex-column justify-space-between flex-grow-1 min-width-0 mr-3">
              <!-- 用户 + 时间 -->
              <div class="d-flex align-center mb-2">
                <UserAvatar
                  :name="deck.creator?.name || t('common.anonymous')"
                  :avatar-url="deck.creator?.avatar"
                  size="16"
                  rounded="md"
                  class="mr-2 flex-shrink-0"
                />
                <span class="text-body-2 font-weight-medium text-grey-darken-3">
                  {{ deck.creator?.name || t('common.anonymous') }}
                </span>
                <span v-if="deck.updatedAt" class="text-caption text-grey mx-1">·</span>
                <span v-if="deck.updatedAt" class="text-caption text-grey">
                  {{ formatRelativeTime(deck.updatedAt) }}
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
              <!-- 点赞 + 收藏 -->
              <div class="d-flex align-center">
                <span
                  v-if="deck.creator?.id !== currentUserId"
                  class="d-flex align-center text-caption like-btn"
                  :class="{ 'text-error': deck.hasLiked, 'text-grey': !deck.hasLiked }"
                  @click.stop="handleUpvote(deck, $event)"
                >
                  <v-icon
                    :icon="deck.hasLiked ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
                    size="13"
                    class="mr-1"
                  ></v-icon>
                  {{ deck.likeCount || 0 }}
                </span>
                <span v-else class="d-flex align-center text-caption text-grey-lighten-1">
                  <v-icon icon="mdi-thumb-up-outline" size="13" class="mr-1"></v-icon>
                  {{ deck.likeCount || 0 }}
                </span>
                <span
                  class="d-flex align-center text-caption like-btn ml-3"
                  :class="{ 'text-primary': deck.bookmarked, 'text-grey': !deck.bookmarked }"
                  @click.stop="handleToggleBookmark(deck, $event)"
                >
                  <v-icon
                    :icon="deck.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
                    size="13"
                  ></v-icon>
                </span>
              </div>
            </div>

            <!-- 右侧：统计数字 -->
            <div class="d-flex flex-row align-center justify-center" style="gap: 12px">
              <div class="text-center">
                <div class="text-body-1 font-weight-bold text-grey-darken-2">
                  {{ deck.cardCount || 0 }}
                </div>
                <div class="text-caption text-no-wrap text-grey">{{ t('memoryCard.cards') }}</div>
              </div>
              <div v-if="deck.studyingCardCount && deck.studyingCardCount > 0" class="text-center">
                <div class="text-body-1 font-weight-bold text-success">
                  {{ deck.studyingCardCount }}
                </div>
                <div class="text-caption text-no-wrap text-grey">
                  {{ t('memoryCard.studying') }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore && !loading" class="text-center pa-4">
        <v-btn variant="text" color="primary" @click="loadMore">{{ t('common.loadMore') }}</v-btn>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="text-center pa-4">
        <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores'
import { formatRelativeTime } from '@/utils/format'
import UserAvatar from '@/components/common/UserAvatar.vue'
import type { MemoryCardDeck } from '@/types/memory'
import { useI18n } from '@/composables/useI18n'
import {
  usePostDecksQuery,
  usePostCreatorDeckQuery,
  useMyPostDeckQuery,
  useUpvoteDeckMutation,
} from '@/queries/memory'
import { useBookmarkToggleMutation } from '@/queries/interaction'

const props = defineProps<Props>()

const emit = defineEmits<Emits>()

const { t } = useI18n()

interface Props {
  postId: number
}

interface Emits {
  (e: 'createDeck'): void
  (e: 'addDeck', deck: MemoryCardDeck): void
  (e: 'viewDeck', deck: MemoryCardDeck): void
}

const userStore = useUserStore()

const sortBy = ref<'score' | 'createdAt' | 'upvoteCount'>('score')
const showAuthorOnly = ref(false)
const showMyOnly = ref(false)
const isHovering = ref(false)

const currentUserId = computed(() => userStore.currentUser?.id)

// 根据当前筛选模式选择对应的 query
const postId = computed(() => props.postId)
const sortByRef = computed(() => sortBy.value as string)

const publicQuery = usePostDecksQuery(postId, sortByRef)
const creatorQuery = usePostCreatorDeckQuery(postId, sortByRef)
const myQuery = useMyPostDeckQuery(postId, sortByRef)

const activeQuery = computed(() => {
  if (showAuthorOnly.value) return creatorQuery
  if (showMyOnly.value) return myQuery
  return publicQuery
})

const decks = computed(() => activeQuery.value.data.value?.pages.flatMap((p) => p.items) ?? [])
const loading = computed(() => activeQuery.value.isLoading.value)
const hasMore = computed(() => activeQuery.value.hasNextPage.value)

const loadMore = () => {
  void activeQuery.value.fetchNextPage()
}

// 点赞
const upvoteDeckMutation = useUpvoteDeckMutation()
const bookmarkToggleMutation = useBookmarkToggleMutation()

const handleUpvote = (deck: MemoryCardDeck, event: Event) => {
  event.stopPropagation()
  upvoteDeckMutation.mutate(deck.id, {
    onSuccess: (result) => {
      if (result) {
        deck.hasLiked = result.liked
        deck.likeCount = result.likeCount
      }
    },
  })
}

const handleToggleBookmark = (deck: MemoryCardDeck, event: Event) => {
  event.stopPropagation()
  bookmarkToggleMutation.mutate(
    { contentType: 'memory_card_deck', contentId: deck.id },
    {
      onSuccess: (result) => {
        if (result !== null) {
          deck.bookmarked = result
        }
      },
    }
  )
}

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

// 切换到"我提交的"标签
const switchToMyDecks = () => {
  handleMyFilterToggle()
}

// 获取状态显示文本
const getStateText = (state: number | undefined) => {
  switch (state) {
    case 1:
      return t('common.stateReviewing')
    case 2:
      return t('common.stateApproved')
    case 3:
      return t('common.stateRejected')
    case 4:
      return t('common.stateBanned')
    default:
      return t('common.stateUnknown')
  }
}

// 获取状态颜色
const getStateColor = (state: number | undefined) => {
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
  switchToMyDecks,
})

// 初始加载由 TanStack Query 自动触发
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

.card-preview {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
