<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import { MemoryService } from '@/services/memoryService'
import type { MemoryCardDeck } from '@/types/memoryCard'
import { DeckState } from '@/types/memoryCard'
import { useRouter } from 'vue-router'
import DeckDetailDialog from '@/components/memory/DeckDetailDialog.vue'
import { useUserStore } from '@/stores/user'
import { memoryCardDeckServiceV1 } from '@/services/api/v1/apiServiceV1'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

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

// 卡片组详情对话框
const showDeckDetail = ref(false)
const selectedDeck: Ref<MemoryCardDeck | null> = ref(null)

// 删除相关
const deleteDialog = ref(false)
const deckToDelete: Ref<MemoryCardDeck | null> = ref(null)
const deleting = ref(false)

// 加载卡片组数据
const loadDecks = async ({ done }: LoadEventData): Promise<void> => {
  try {
    let response

    // 如果是查看自己的卡片组，使用 getMyAllDecks（显示所有状态）
    // 如果是查看其他用户的，使用 getUserDecks（显示所有状态）
    if (isCurrentUser.value) {
      response = await MemoryService.getMyAllDecks({
        lastId: lastId.value,
        limit: 10,
      })
    } else {
      response = await MemoryService.getUserDecks(props.userId!, {
        lastId: lastId.value,
        limit: 10,
      })
    }

    if (response.code === 200) {
      const { items, hasMore, nextCursor } = response.data

      deckList.value.push(...items)

      if (hasMore && nextCursor) {
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
      return '已发布'
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

const deleteMessage = computed(() =>
  deckToDelete.value
    ? `确定要删除卡片组「${deckToDelete.value.title}」吗？此操作无法撤销。`
    : ''
)

// 打开删除确认对话框
const confirmDelete = (deck: MemoryCardDeck, event: Event): void => {
  event.stopPropagation() // 阻止事件冒泡
  deckToDelete.value = deck
  deleteDialog.value = true
}

// 执行删除
const handleDelete = async (): Promise<void> => {
  if (!deckToDelete.value) return

  try {
    deleting.value = true
    const response = await memoryCardDeckServiceV1.deleteDeck(deckToDelete.value.id)

    if (response.code === 200) {
      // 从列表中移除已删除的卡片组
      const index = deckList.value.findIndex(d => d.id === deckToDelete.value!.id)
      if (index !== -1) {
        deckList.value.splice(index, 1)
      }
      deleteDialog.value = false
      deckToDelete.value = null
    } else {
      console.error('删除失败:', response.message)
      alert('删除失败: ' + response.message)
    }
  } catch (error) {
    console.error('删除卡片组时发生错误:', error)
    alert('删除失败，请稍后重试')
  } finally {
    deleting.value = false
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
            <div class="d-flex align-center justify-space-between">
              <v-btn
                v-if="deck.sourcePostId"
                variant="text"
                size="small"
                color="primary"
                class="px-2"
                @click.stop="viewSourcePost(deck.sourcePostId)"
              >
                <v-icon icon="mdi-file-document-outline" size="16" class="mr-1"></v-icon>
                查看来源帖子
              </v-btn>
              <v-spacer v-else></v-spacer>
              <!-- 只为当前用户显示删除按钮 -->
              <v-btn
                v-if="isCurrentUser"
                variant="text"
                size="small"
                color="error"
                icon="mdi-delete"
                @click.stop="confirmDelete(deck, $event)"
              ></v-btn>
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

    <!-- 删除确认对话框 -->
    <ConfirmDialog
      v-model="deleteDialog"
      title="确认删除"
      :message="deleteMessage"
      confirm-text="删除"
      cancel-text="取消"
      :loading="deleting"
      @confirm="handleDelete"
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
