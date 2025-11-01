<script setup lang="ts">
import { ref, computed } from 'vue'
import { MemoryService } from '@/services/memoryService'
import type { MemoryCardDeck } from '@/types/memoryCard'
import DeckDetailDialog from './DeckDetailDialog.vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

interface Props {
  nodeId: number
}

const props = defineProps<Props>()

// 弹窗相关
const showDeckDetail = ref(false)
const selectedDeck = ref<MemoryCardDeck | null>(null)

// 使用 useInfiniteScroll 处理卡片组列表
const { items: decks, loading, hasMore, loadMore: loadMoreDecks, refresh } = useInfiniteScroll({
  fetchFn: (params) => MemoryService.getDecksByNode(props.nodeId, {
    lastScore: params.lastScore,
    lastId: params.lastId,
    limit: params.limit
  }),
  getNextParams: (lastItem, currentParams) => ({
    lastId: lastItem.id,
    lastScore: lastItem.upvoteCount || 0,
    limit: currentParams.limit
  }),
  initialParams: {
    lastId: 0,
    lastScore: 0,
    limit: 20
  }
})

// 使用 useMutation 处理点赞
const { execute: upvoteDeck } = useMutation(
  (deckId: number) => MemoryService.upvoteDeck(deckId),
  {
    showToast: false, // 点赞不显示提示
    onSuccess: (result, deckId) => {
      // 更新本地状态
      const deck = decks.value.find(d => d.id === deckId)
      if (deck) {
        deck.hasUpvoted = result.upvoted
        deck.upvoteCount = result.upvotes
      }
    }
  }
)

// 处理卡片组点击事件
const handleDeckClick = (deck: MemoryCardDeck) => {
  selectedDeck.value = deck
  showDeckDetail.value = true
}

// 处理来源文章点击事件
const handleSourcePostClick = (postId: number, event: Event) => {
  event.stopPropagation() // 阻止事件冒泡，避免触发卡片组点击
  // 跳转到来源文章
  window.location.href = `/read?postId=${postId}`
}

// 处理点赞
const handleUpvote = async (deck: MemoryCardDeck, event: Event) => {
  event.stopPropagation() // 阻止事件冒泡
  await upvoteDeck(deck.id)
}

// 加载更多
const loadMore = async ({ done }: { done: (status: 'ok' | 'empty') => void }) => {
  await loadMoreDecks(done)
}

// 计算属性：是否有数据
const hasData = computed(() => decks.value.length > 0)

// 格式化时间
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

// 获取状态文本
const getStateText = (state: number) => {
  const stateMap: Record<number, string> = {
    0: '审核中',
    1: '正常',
    2: '已屏蔽',
    3: '私有'
  }
  return stateMap[state] || '未知'
}

// 获取状态颜色
const getStateColor = (state: number) => {
  const colorMap: Record<number, string> = {
    0: 'orange',
    1: 'success',
    2: 'error',
    3: 'grey'
  }
  return colorMap[state] || 'grey'
}
</script>

<template>
  <div>
    <!-- 空状态 -->
    <div v-if="!loading && !hasData" class="text-center py-12">
      <v-icon icon="mdi-cards-outline" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
      <p class="text-h6 text-grey-darken-1 mb-2">暂无记忆卡片</p>
      <p class="text-body-2 text-grey">该节点下还没有记忆卡片</p>
    </div>

    <!-- 卡片组列表 -->
    <div v-if="hasData" class="deck-list">
      <v-infinite-scroll
        :items="decks"
        @load="loadMore"
        :no-more-text="'没有更多卡片组了'"
      >
        <div
          v-for="(deck, index) in decks"
          :key="`deck-${deck.id}-${index}`"
          class="mb-1"
        >
          <v-card
            elevation="0"
            rounded="lg"
            class="deck-card pt-2"
          >
            <v-card-title class="d-flex align-center justify-space-between pb-5">
              <div class="d-flex align-center">
                <v-icon icon="mdi-cards" color="primary" class="mr-3"></v-icon>
                <span
                  class="text-h6 font-weight-bold clickable-title"
                  @click="handleDeckClick(deck)"
                >
                  {{ deck.title }}
                </span>
              </div>
              <!-- 来源文章 -->
              <div v-if="deck.sourcePostId">
                <v-chip
                  size="small"
                  color="primary"
                  variant="text"
                  prepend-icon="mdi-file-document-outline"
                  @click="handleSourcePostClick(deck.sourcePostId, $event)"
                >
                  来源文章
                </v-chip>
              </div>
            </v-card-title>

            <v-card-text>
              <!-- 描述 -->
              <p v-if="deck.description" class="text-body-2 text-grey-darken-1 mb-3">
                {{ deck.description }}
              </p>

              <!-- 其它信息 -->
              <div class="d-flex align-center justify-space-between">
                <div class="d-flex align-center">
                  <div class="d-flex align-center mr-6">
                    <v-icon icon="mdi-card-multiple" size="16" color="grey-darken-1" class="mr-2"></v-icon>
                    <span class="text-body-2 text-grey-darken-1">{{ deck.cardCount }} 张卡片</span>
                  </div>
                  <div class="d-flex align-center mr-6">
                    <v-btn
                      :color="deck.hasUpvoted ? 'grey-darken-1' : 'grey-darken-1'"
                      variant="text"
                      density="comfortable"
                      rounded="lg"
                      @click="handleUpvote(deck, $event)"
                    >
                      <v-icon
                        :icon="deck.hasUpvoted ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
                        :color="deck.hasUpvoted ? 'red' : 'grey-darken-1'"
                        size="16"
                        class="mr-2"
                      ></v-icon>
                      {{ deck.upvoteCount }} 点赞
                    </v-btn>
                  </div>
                </div>
                <div class="d-flex align-center">
                  <v-icon icon="mdi-clock-outline" size="16" color="grey-darken-1" class="mr-2"></v-icon>
                  <span class="text-body-2 text-grey-darken-1">{{ formatDate(deck.updatedAt) }}</span>
                </div>
              </div>
            </v-card-text>
          </v-card>

          <!-- 分隔线 (除了最后一个item) -->
          <v-divider v-if="index < decks.length - 1" class="my-4"></v-divider>
        </div>

        <template #empty>
          <div class="text-center py-8">
            <p class="text-body-2 text-grey">- 没有更多卡片组了 -</p>
          </div>
        </template>
      </v-infinite-scroll>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-12">
      <v-progress-circular
        indeterminate
        color="primary"
        class="mb-4"
      ></v-progress-circular>
      <p class="text-body-2 text-grey">加载中...</p>
    </div>

    <!-- 卡片组详情弹窗 -->
    <DeckDetailDialog
      v-model="showDeckDetail"
      :deck="selectedDeck"
    />
  </div>
</template>

<style scoped>
.deck-card {
  transition: all 0.2s ease;
}

.clickable-title {
  cursor: pointer;
  transition: color 0.2s ease;
}

.clickable-title:hover {
  color: #1976d2;
}
</style>