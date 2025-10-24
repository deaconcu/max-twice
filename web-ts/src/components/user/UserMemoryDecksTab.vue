<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import { MemoryService } from '@/services/memoryService'
import type { MemoryCardDeck } from '@/types/memoryCard'
import { DeckState } from '@/types/memoryCard'
import { useRouter } from 'vue-router'
import DeckDetailDialog from '@/components/memory/DeckDetailDialog.vue'
import { useUserStore } from '@/stores/user'

interface LoadEventData {
  done: (status: 'ok' | 'empty') => void
}

// Props - 支持传入 userId 查看其他用户的卡片组
const props = defineProps<{
  userId?: number
}>()

const router = useRouter()
const userStore = useUserStore()

// 判断是否为当前用户自己
const isCurrentUser = computed(() => !props.userId || props.userId === userStore.userId)

// 卡片组列表数据
const deckList: Ref<MemoryCardDeck[]> = ref([])
const lastId: Ref<number | undefined> = ref(undefined)
const lastScore: Ref<number | undefined> = ref(undefined)

// 卡片组详情对话框
const showDeckDetail = ref(false)
const selectedDeck: Ref<MemoryCardDeck | null> = ref(null)

// 加载卡片组数据
const loadDecks = async ({ done }: LoadEventData): Promise<void> => {
  try {
    let response

    // 如果是查看自己的卡片组，使用 getMyAllDecks（显示所有状态）
    // 如果是查看其他用户的，使用 getUserDecks（只显示正常状态）
    if (isCurrentUser.value) {
      response = await MemoryService.getMyAllDecks({
        sortBy: 'createdAt',
        sortOrder: 'desc',
        lastScore: lastScore.value,
        lastId: lastId.value,
        limit: 10,
      })
    } else {
      response = await MemoryService.getUserDecks(props.userId!, {
        sortBy: 'createdAt',
        sortOrder: 'desc',
        lastId: lastId.value,
        limit: 10,
      })
    }

    if (response.code === 200) {
      const { items, hasMore, nextCursor } = response.data

      deckList.value.push(...items)

      if (hasMore && nextCursor) {
        lastScore.value = nextCursor.lastScore
        lastId.value = nextCursor.lastId
        done('ok')
      } else {
        done('empty')
      }
    } else {
      done('empty')
    }
  } catch (error) {
    console.error('Error loading memory decks:', error)
    done('empty')
  }
}

// 获取状态文本
const getStateText = (state: number): string => {
  switch (state) {
    case DeckState.SUBMITTED:
      return '审核中'
    case DeckState.PUBLISHED:
      return '正常'
    case DeckState.REJECTED:
      return '已拒绝'
    case DeckState.BANNED:
      return '已屏蔽'
    default:
      return '未知'
  }
}

// 获取状态颜色
const getStateColor = (state: number): string => {
  switch (state) {
    case DeckState.SUBMITTED:
      return 'orange'
    case DeckState.PUBLISHED:
      return 'success'
    case DeckState.REJECTED:
      return 'warning'
    case DeckState.BANNED:
      return 'error'
    default:
      return 'grey'
  }
}

// 查看卡片组详情
const viewDeckDetail = (deck: MemoryCardDeck): void => {
  selectedDeck.value = deck
  showDeckDetail.value = true
}

// 跳转到来源帖子
const viewSourcePost = (postId: number): void => {
  router.push({ path: '/read', query: { postId: postId.toString() } })
}

// 格式化日期
const formatDate = (dateString: string): string => {
  const date = new Date(dateString)
  const now = new Date()
  const diffTime = Math.abs(now.getTime() - date.getTime())
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays === 0) {
    return '今天'
  } else if (diffDays === 1) {
    return '昨天'
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else if (diffDays < 30) {
    return `${Math.floor(diffDays / 7)}周前`
  } else if (diffDays < 365) {
    return `${Math.floor(diffDays / 30)}个月前`
  } else {
    return `${Math.floor(diffDays / 365)}年前`
  }
}
</script>

<template>
  <div>
    <v-infinite-scroll :items="deckList" @load="loadDecks" no-more-text="已经到底了">
      <div v-for="(deck, index) in deckList" :key="deck.id">
        <v-card
          class="mb-4 deck-card"
          :class="{ 'mt-0': index === 0 }"
          elevation="0"
          @click="viewDeckDetail(deck)"
        >
          <v-card-text class="pa-4">
            <!-- 标题和状态 -->
            <div class="d-flex justify-space-between align-start mb-2">
              <h3 class="text-h6 font-weight-bold text-grey-darken-4 flex-grow-1">
                {{ deck.title }}
              </h3>
              <v-chip
                :color="getStateColor(deck.state)"
                size="small"
                variant="flat"
                class="ml-2"
              >
                {{ getStateText(deck.state) }}
              </v-chip>
            </div>

            <!-- 描述 -->
            <p
              v-if="deck.description"
              class="text-body-2 text-grey-darken-2 mb-3 deck-description"
            >
              {{ deck.description }}
            </p>

            <!-- 统计信息 -->
            <div class="d-flex align-center text-body-2 text-grey mb-3">
              <v-icon icon="mdi-cards-outline" size="18" class="mr-1"></v-icon>
              <span class="mr-4">{{ deck.cardCount }} 张卡片</span>

              <v-icon icon="mdi-thumb-up-outline" size="18" class="mr-1"></v-icon>
              <span class="mr-4">{{ deck.upvoteCount }} 点赞</span>

              <v-icon icon="mdi-clock-outline" size="18" class="mr-1"></v-icon>
              <span>{{ formatDate(deck.createdAt) }}</span>
            </div>

            <!-- 来源帖子链接 -->
            <div v-if="deck.sourcePostId" class="d-flex align-center">
              <v-btn
                variant="text"
                size="small"
                color="primary"
                class="px-2"
                @click.stop="viewSourcePost(deck.sourcePostId)"
              >
                <v-icon icon="mdi-file-document-outline" size="16" class="mr-1"></v-icon>
                查看来源帖子
              </v-btn>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <template #empty>
        <div v-if="deckList.length === 0" class="text-center py-8">
          <v-icon icon="mdi-cards-outline" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey">还没有创建任何卡片组</p>
          <p class="text-body-2 text-grey-lighten-1">开始创建你的第一个记忆卡片组吧！</p>
        </div>
        <div v-else class="text-body-2 text-grey py-5">已经到底了</div>
      </template>
    </v-infinite-scroll>

    <!-- 卡片组详情对话框 -->
    <DeckDetailDialog
      v-model="showDeckDetail"
      :deck="selectedDeck"
    />
  </div>
</template>

<style scoped>
.deck-card {
  border: 1px solid rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
  cursor: pointer;
}

.deck-card:hover {
  border-color: rgba(0, 0, 0, 0.12);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.deck-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
  max-height: 3em;
}

.v-infinite-scroll__side {
  display: none !important;
}
</style>
