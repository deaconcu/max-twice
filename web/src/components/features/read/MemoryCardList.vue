<template>
  <div class="memory-card-list">
    <!-- 空状态 -->
    <div v-if="decks.length === 0 && !loading" class="text-center pa-12">
      <v-icon icon="mdi-cards-outline" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
      <h4 class="text-h6 font-weight-medium text-grey-darken-2 mb-2">
        {{ t('memoryCard.noDecks') }}
      </h4>
      <p class="text-body-2 text-grey-darken-1 mb-4">{{ t('memoryCard.noDecksHint') }}</p>
      <v-btn
        color="primary"
        variant="tonal"
        rounded="lg"
        prepend-icon="mdi-plus"
        @click="emit('createDeck')"
      >
        {{ t('memoryCard.createFirstDeck') }}
      </v-btn>
    </div>

    <!-- 卡片组列表 -->
    <div v-else class="deck-list">
      <div v-for="deck in decks" :key="deck.id" class="deck-item" @click="viewDeckDetail(deck)">
        <!-- 左右两栏布局 -->
        <div class="d-flex align-stretch">
          <!-- 左侧：用户 / 简介 / 点赞收藏 -->
          <div class="d-flex flex-column justify-space-between flex-grow-1 min-width-0 mr-4">
            <!-- 用户 + 时间 -->
            <div class="d-flex align-center mb-3">
              <UserAvatar
                :name="deck.creator?.name || t('common.anonymous')"
                :avatar-url="deck.creator?.avatar"
                size="18"
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
            </div>
            <!-- 简介 -->
            <div class="text-body-1 text-grey-darken-1 deck-desc mb-3">
              {{ deck.description || t('common.noDescription') }}
            </div>
            <!-- 点赞 + 收藏 -->
            <div class="d-flex align-center">
              <span
                class="d-flex align-center text-caption like-btn"
                :class="{ 'text-error': deck.hasLiked, 'text-grey': !deck.hasLiked }"
                @click.stop="handleUpvote(deck)"
              >
                <v-icon
                  :icon="deck.hasLiked ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
                  size="14"
                  class="mr-1"
                ></v-icon>
                {{ deck.likeCount || 0 }}
              </span>
              <span
                class="d-flex align-center text-caption bookmark-btn ml-3"
                :class="{ 'text-primary': deck.bookmarked, 'text-grey': !deck.bookmarked }"
                @click.stop="handleToggleBookmark(deck)"
              >
                <v-icon
                  :icon="deck.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
                  size="14"
                ></v-icon>
              </span>
            </div>
          </div>

          <!-- 右侧：统计数字 -->
          <div class="d-flex flex-row align-center justify-center" style="gap: 16px">
            <div class="text-center">
              <div class="text-h6 font-weight-bold text-grey-darken-2">
                {{ deck.cardCount || 0 }}
              </div>
              <div class="text-caption text-no-wrap text-grey">{{ t('memoryCard.cards') }}</div>
            </div>
            <div v-if="deck.studyingCardCount && deck.studyingCardCount > 0" class="text-center">
              <div class="text-h6 font-weight-bold text-success">{{ deck.studyingCardCount }}</div>
              <div class="text-caption text-no-wrap text-grey">{{ t('memoryCard.studying') }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore && !loading" ref="loadMoreTrigger" class="text-center mt-6 py-4">
      <v-progress-circular indeterminate color="primary" size="32" />
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && decks.length === 0" class="text-center pa-12">
      <v-progress-circular indeterminate color="primary" size="40"></v-progress-circular>
      <p class="text-body-1 text-grey-darken-1 mt-4">{{ t('common.loading') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { memoryApi, bookmarkApi } from '@/api'
import { useMutation } from '@/composables'
import { useUserStore } from '@/stores'
import { formatRelativeTime } from '@/utils/format'
import UserAvatar from '@/components/common/UserAvatar.vue'
import type { MemoryCardDeck } from '@/types/memory'
import { useI18n } from '@/composables/useI18n'

const props = defineProps<Props>()

const emit = defineEmits<Emits>()

const { t } = useI18n()

interface Props {
  nodeId: number
}

interface Emits {
  (e: 'viewDeck', deck: MemoryCardDeck): void
  (e: 'createDeck'): void
}

const userStore = useUserStore()

const decks = ref<MemoryCardDeck[]>([])
const loading = ref(false)
const hasMore = ref(true)
const lastId = ref<number>(0)
const lastScore = ref<number>(0)
const loadMoreTrigger = ref<HTMLElement | null>(null)

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
      limit: 20,
    }

    // 只有在非第一页时才添加 lastScore 和 lastId
    if (lastId.value > 0) {
      queryParams.lastScore = lastScore.value
      queryParams.lastId = lastId.value
    }

    const response = await memoryApi.getDecksByNode(props.nodeId, queryParams)

    if (response.data?.items) {
      const items = response.data.items
      decks.value = reset ? items : [...decks.value, ...items]

      if (items.length > 0) {
        const lastItem = items[items.length - 1]
        lastId.value = lastItem.id
        lastScore.value = lastItem.likeCount || 0
      }

      hasMore.value = response.data.hasMore
    } else {
      hasMore.value = false
    }
  } catch (error) {
    console.error('Failed to load memory decks:', error)
  } finally {
    loading.value = false
  }
}

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

const handleUpvote = async (deck: MemoryCardDeck) => {
  await upvoteDeck(deck.id)
}

// 使用 useMutation 处理收藏
const { execute: toggleBookmark } = useMutation(
  (deckId: number) => bookmarkApi.toggle('memory_card_deck', deckId),
  {
    showToast: false,
    onSuccess: (result, deckId) => {
      const deck = decks.value.find((d) => d.id === deckId)
      if (deck && result !== null) {
        deck.bookmarked = result
      }
    },
  }
)

const handleToggleBookmark = async (deck: MemoryCardDeck) => {
  await toggleBookmark(deck.id)
}

const viewDeckDetail = (deck: MemoryCardDeck) => {
  emit('viewDeck', deck)
}

// 获取状态显示文本
const getStateText = (state: number) => {
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

/**
 * Intersection Observer 实例
 */
let observer: IntersectionObserver | null = null

/**
 * 设置无限滚动
 */
const setupInfiniteScroll = () => {
  if (!loadMoreTrigger.value) return

  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry?.isIntersecting && hasMore.value && !loading.value) {
        void loadDecks(false)
      }
    },
    {
      root: null,
      rootMargin: '100px',
      threshold: 0.1,
    }
  )

  observer.observe(loadMoreTrigger.value)
}

/**
 * 清理 Intersection Observer
 */
const cleanupInfiniteScroll = () => {
  if (observer && loadMoreTrigger.value) {
    observer.unobserve(loadMoreTrigger.value)
    observer.disconnect()
    observer = null
  }
}

onMounted(() => {
  void loadDecks(true)
  setTimeout(setupInfiniteScroll, 100)
})

onBeforeUnmount(() => {
  cleanupInfiniteScroll()
})
</script>

<style scoped>
.memory-card-list {
  padding: 8px 0;
}

.deck-list {
  display: flex;
  flex-direction: column;
}

.deck-item {
  padding: 18px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.deck-item:last-child {
  border-bottom: none;
}

.deck-item:hover {
  background-color: rgba(var(--v-theme-surface-variant), 0.5);
}

.min-width-0 {
  min-width: 0;
}

.deck-desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.stat-item {
  min-width: 36px;
}

.like-btn,
.bookmark-btn {
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  transition: background-color 0.15s ease;
}

.like-btn:hover,
.bookmark-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}
</style>
