<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { MemoryService } from '@/services/memoryService'
import type { MemoryCardDeck } from '@/types/memoryCard'
import DeckDetailDialog from './DeckDetailDialog.vue'

interface Props {
  nodeId: number
}

const props = defineProps<Props>()

// 状态管理
const decks = ref<MemoryCardDeck[]>([])
const loading = ref(false)
const hasMore = ref(true)
const lastScore = ref<number | undefined>(undefined)
const lastId = ref<number | undefined>(undefined)
const error = ref<string | null>(null)

// 弹窗相关
const showDeckDetail = ref(false)
const selectedDeck = ref<MemoryCardDeck | null>(null)

// 分页参数
const LIMIT = 20

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

// 加载卡片组列表
const loadDecks = async (reset = false) => {
  if (loading.value || (!reset && !hasMore.value)) return

  try {
    loading.value = true
    error.value = null

    // 如果是重置，清空分页参数
    if (reset) {
      decks.value = []
      lastScore.value = undefined
      lastId.value = undefined
      hasMore.value = true
    }

    const response = await MemoryService.getDecksByNode(props.nodeId, {
      lastScore: lastScore.value,
      lastId: lastId.value,
      limit: LIMIT
    })

    if (response.code === 200) {
      const newDecks = response.data.items || []

      if (reset) {
        decks.value = newDecks
      } else {
        decks.value.push(...newDecks)
      }

      // 更新分页参数
      hasMore.value = response.data.hasMore
      if (response.data.nextCursor) {
        lastScore.value = response.data.nextCursor.lastScore
        lastId.value = response.data.nextCursor.lastId
      }
    } else {
      error.value = response.message || '加载失败'
    }
  } catch (err) {
    console.error('Failed to load memory card decks:', err)
    error.value = '加载卡片组失败，请重试'
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = async ({ done }: { done: (status: 'ok' | 'empty') => void }) => {
  await loadDecks(false)
  done(hasMore.value ? 'ok' : 'empty')
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

// 初始化
onMounted(() => {
  loadDecks(true)
})
</script>

<template>
  <div>
    <!-- 空状态 -->
    <div v-if="!loading && !hasData && !error" class="text-center py-12">
      <v-icon icon="mdi-cards-outline" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
      <p class="text-h6 text-grey-darken-1 mb-2">暂无记忆卡片</p>
      <p class="text-body-2 text-grey">该节点下还没有记忆卡片</p>
    </div>

    <!-- 错误状态 -->
    <div v-if="error && !loading" class="text-center py-8">
      <v-alert
        type="error"
        variant="tonal"
        class="mb-4"
      >
        {{ error }}
      </v-alert>
      <v-btn
        color="primary"
        variant="outlined"
        @click="loadDecks(true)"
      >
        重试
      </v-btn>
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
            hover
            @click="handleDeckClick(deck)"
          >
            <v-card-title class="d-flex align-center justify-space-between pb-2">
              <div class="d-flex align-center">
                <v-icon icon="mdi-cards" color="primary" class="mr-3"></v-icon>
                <span class="text-h6 font-weight-bold">{{ deck.title }}</span>
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
                    <v-icon icon="mdi-card-multiple" size="16" color="grey-darken-1" class="mr-1"></v-icon>
                    <span class="text-body-2 text-grey-darken-1">{{ deck.cardCount }} 张卡片</span>
                  </div>
                  <div class="d-flex align-center mr-6">
                    <v-icon icon="mdi-thumb-up" size="16" color="grey-darken-1" class="mr-1"></v-icon>
                    <span class="text-body-2 text-grey-darken-1">{{ deck.upvoteCount }} 点赞</span>
                  </div>
                </div>
                <div class="d-flex align-center">
                  <v-icon icon="mdi-clock-outline" size="16" color="grey-darken-1" class="mr-1"></v-icon>
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
  cursor: pointer;
}

.deck-card:hover {
  transform: translateY(-2px);
}

</style>