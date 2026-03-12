<template>
  <div class="memory-card-list">
    <!-- 空状态 -->
    <div v-if="decks.length === 0 && !loading" class="text-center pa-12">
      <v-icon icon="mdi-cards-outline" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
      <h4 class="text-h6 font-weight-medium text-grey-darken-2 mb-2">暂无记忆卡片组</h4>
      <p class="text-body-2 text-grey-darken-1 mb-4">还没有人为这个节点创建记忆卡片组</p>
      <v-btn color="primary" variant="tonal" rounded="lg" prepend-icon="mdi-plus" @click="emit('createDeck')">
        创建第一个卡片组
      </v-btn>
    </div>

    <!-- 卡片组列表 -->
    <div v-else class="deck-list">
      <v-card
        v-for="deck in decks"
        :key="deck.id"
        class="deck-card"
        elevation="0"
        rounded="xl"
        flat
        @click="viewDeckDetail(deck)"
      >
        <v-card-text class="pa-4 pa-sm-5">
          <div class="d-flex align-start justify-space-between mb-3">
            <h4 class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-3">
              {{ deck.title }}
            </h4>
          </div>

          <p v-if="deck.description" class="text-body-2 text-grey-darken-1 mb-4 line-clamp-2">
            {{ deck.description }}
          </p>

          <div class="d-flex align-center justify-space-between">
            <div class="d-flex align-center">
              <UserAvatar
                :name="deck.creator?.name || '匿名用户'"
                :avatar-url="deck.creator?.avatar"
                size="28"
                rounded="circle"
                class="mr-2"
              />
              <span class="text-body-2 text-grey-darken-2">
                {{ deck.creator?.name || '匿名用户' }}
              </span>
            </div>
            <div class="d-flex align-center">
              <v-btn
                :color="deck.hasLiked ? 'grey-darken-2' : 'grey-darken-2'"
                variant="text"
                size="small"
                rounded="lg"
                @click.stop="handleUpvote(deck)"
              >
                <v-icon
                  :icon="deck.hasLiked ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
                  :color="deck.hasLiked ? 'red' : 'grey-darken-2'"
                  size="14"
                  class="mr-2"
                ></v-icon>
                {{ deck.likeCount }}
              </v-btn>
              <v-btn
                :icon="deck.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
                :color="deck.bookmarked ? 'primary' : 'grey-darken-2'"
                variant="text"
                size="small"
                rounded="lg"
                class="ml-1"
                @click.stop="handleToggleBookmark(deck)"
              />
              <v-icon
                icon="mdi-cards-outline"
                size="14"
                color="grey-darken-2"
                class="ml-3 mr-2"
              ></v-icon>
              <span class="text-body-2 text-grey-darken-2">{{ deck.cardCount }}</span>
            </div>
          </div>
        </v-card-text>
      </v-card>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore && !loading" ref="loadMoreTrigger" class="text-center mt-6 py-4">
      <v-progress-circular indeterminate color="primary" size="32" />
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && decks.length === 0" class="text-center pa-12">
      <v-progress-circular indeterminate color="primary" size="40"></v-progress-circular>
      <p class="text-body-1 text-grey-darken-1 mt-4">加载中...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { memoryApi, bookmarkApi } from '@/api'
import { useMutation } from '@/composables'
import { useUserStore } from '@/stores'
import UserAvatar from '@/components/common/UserAvatar.vue'
import type { MemoryCardDeck } from '@/types/memory'

interface Props {
  nodeId: number
}

type Emits = {
  (e: 'viewDeck', deck: MemoryCardDeck): void
  (e: 'createDeck'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const userStore = useUserStore()

const decks = ref<MemoryCardDeck[]>([])
const loading = ref(false)
const hasMore = ref(true)
const lastId = ref<number>(0)
const lastScore = ref<number>(0)
const loadMoreTrigger = ref<HTMLElement | null>(null)

const currentUserId = computed(() => userStore.user?.id)

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
  (deckId: number) => bookmarkApi.toggle('memory_card', deckId),
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
  padding: 16px 0;
}

.deck-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.deck-card {
  transition: all 0.2s ease;
  cursor: pointer;
  background-color: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline));
}

.deck-card:hover {
  border-color: rgb(var(--v-theme-primary));
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
