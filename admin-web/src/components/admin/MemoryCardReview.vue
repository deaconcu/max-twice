<script setup lang="ts">
import { inject, ref } from 'vue'
import { adminApi } from '@/api'
import { ContentState, ApprovalAction } from '@/enums'
import type { DeckDetail } from '@/types/memoryCard'
import RejectBanDialog from './RejectBanDialog.vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

const activeTab = ref<string>('pending')

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const currentDeck = ref<DeckDetail | null>(null)
const dialogType = ref<'reject' | 'ban'>('reject')

// 标签配置
interface TabConfig {
  key: string
  label: string
  state: number
  icon: string
  color: string
}

const tabs: TabConfig[] = [
  {
    key: 'pending',
    label: '待审核',
    state: ContentState.SUBMITTED,
    icon: 'mdi-clock-outline',
    color: 'orange',
  },
  {
    key: 'approved',
    label: '已通过',
    state: ContentState.PUBLISHED,
    icon: 'mdi-check-circle',
    color: 'green',
  },
  {
    key: 'rejected',
    label: '已拒绝',
    state: ContentState.REJECTED,
    icon: 'mdi-close-circle',
    color: 'red',
  },
  {
    key: 'banned',
    label: '已封禁',
    state: ContentState.BANNED,
    icon: 'mdi-cancel',
    color: 'grey',
  },
]

// 筛选条件
const filterForm = ref({
  postId: null as number | null,
  creatorId: null as number | null,
})

const getCurrentTab = () => tabs.find((tab) => tab.key === activeTab.value) || tabs[0]

// 使用 useInfiniteScroll 加载卡片组列表
const {
  items: deckList,
  loading,
  hasMore,
  loadMore,
  reset: resetDeckList,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const currentTab = getCurrentTab()
    const state = currentTab.state

    const response = await adminApi.getDecksByFilter(
      filterForm.value.postId || undefined,
      filterForm.value.creatorId || undefined,
      state,
      params.lastId
    )

    const pageData = response.data
    return {
      code: response.code,
      data: pageData?.items || [],
      message: response.message || '',
      hasMore: pageData?.hasMore ?? false,
    }
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: {
    lastId: undefined,
  },
  immediate: true, // 自动初始加载
})

const applyFilter = () => {
  resetDeckList()
  loadMore() // 重新加载数据
}

const resetFilter = () => {
  filterForm.value = {
    postId: null,
    creatorId: null,
  }
  applyFilter()
}

// 使用 useMutation 批准卡片组
const { execute: executeApproveDeck } = useMutation(
  (deckId: number) => adminApi.operateContent('memory_card_deck', deckId, { action: 'approve' }),
  {
    successMessage: '卡片组审核通过',
    onSuccess: (_, deckId) => {
      const index = deckList.value.findIndex((d) => d.id === deckId)
      if (index > -1) {
        deckList.value.splice(index, 1)
      }
    },
  }
)

const approveDeck = async (deck: DeckDetail): Promise<void> => {
  await executeApproveDeck(deck.id)
}

// 显示拒绝对话框
const showRejectDialog = (deck: DeckDetail) => {
  currentDeck.value = deck
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 显示屏蔽对话框
const showBanDialog = (deck: DeckDetail) => {
  currentDeck.value = deck
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (data: { deckId: number; action: ApprovalAction; reason: string }) =>
    adminApi.operateContent('memory_card_deck', data.deckId, { action: data.action.toLowerCase(), reason: data.reason }),
  {
    onSuccess: (_, data) => {
      const message = data.action === ApprovalAction.BAN ? '卡片组已屏蔽' : '卡片组已拒绝'
      showSnackbar?.(message, 'success')

      const index = deckList.value.findIndex((d) => d.id === data.deckId)
      if (index > -1) {
        deckList.value.splice(index, 1)
      }

      showReasonDialog.value = false
      currentDeck.value = null
    },
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string) => {
  if (!currentDeck.value) return

  const action = dialogType.value === 'ban' ? ApprovalAction.BAN : ApprovalAction.REJECT
  await executeRejectOrBan({
    deckId: currentDeck.value.id,
    action,
    reason,
  })
}

// 拒绝卡片组（兼容旧调用）
const rejectDeck = async (deck: DeckDetail): Promise<void> => {
  showRejectDialog(deck)
}

// 屏蔽卡片组（兼容旧调用）
const banDeck = async (deck: DeckDetail): Promise<void> => {
  showBanDialog(deck)
}

// 使用 useMutation 取消屏蔽卡片组
const { execute: executeUnbanDeck } = useMutation(
  (deckId: number) => adminApi.operateContent('memory_card_deck', deckId, { action: 'approve' }),
  {
    successMessage: '卡片组已取消屏蔽',
    onSuccess: (_, deckId) => {
      const index = deckList.value.findIndex((d) => d.id === deckId)
      if (index > -1) {
        deckList.value.splice(index, 1)
      }
    },
  }
)

const unbanDeck = async (deck: DeckDetail): Promise<void> => {
  await executeUnbanDeck(deck.id)
}

const switchTab = (tabKey: string) => {
  resetDeckList()
  loadMore() // 重新加载数据
}

const getStateText = (state: number): string => {
  switch (state) {
    case ContentState.SUBMITTED:
      return '待审核'
    case ContentState.PUBLISHED:
      return '已通过'
    case ContentState.REJECTED:
      return '已拒绝'
    case ContentState.BANNED:
      return '已封禁'
    default:
      return '未知'
  }
}

const getStateColor = (state: number): string => {
  switch (state) {
    case ContentState.SUBMITTED:
      return 'orange-lighten-4'
    case ContentState.PUBLISHED:
      return 'green-lighten-4'
    case ContentState.REJECTED:
      return 'red-lighten-4'
    case ContentState.BANNED:
      return 'grey-lighten-2'
    default:
      return 'grey-lighten-3'
  }
}
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-purple-lighten-5 mr-3">
          <v-icon icon="mdi-cards-variant" color="purple-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">记忆卡片管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">管理用户提交的记忆卡片组</p>
        </div>
      </div>
      <v-chip variant="flat" color="purple-lighten-4" rounded="lg">
        <v-icon icon="mdi-cards-outline" color="purple-darken-2" size="16" class="mr-1"></v-icon>
        <span class="text-purple-darken-2 text-caption"> {{ deckList.length }} 个卡片组 </span>
      </v-chip>
    </div>

    <!-- 筛选区域 -->
    <div class="d-flex align-center ga-3 mb-6">
      <v-text-field
        v-model.number="filterForm.postId"
        label="帖子ID"
        type="number"
        variant="outlined"
        density="compact"
        rounded="lg"
        hide-details
        clearable
        style="max-width: 180px"
      ></v-text-field>
      <v-text-field
        v-model.number="filterForm.creatorId"
        label="用户ID"
        type="number"
        variant="outlined"
        density="compact"
        rounded="lg"
        hide-details
        clearable
        style="max-width: 180px"
      ></v-text-field>
      <v-btn variant="flat" color="primary" rounded="lg" size="small" @click="applyFilter">
        <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
        筛选
      </v-btn>
      <v-btn variant="text" color="grey" rounded="lg" size="small" @click="resetFilter">
        重置
      </v-btn>
    </div>

    <!-- 状态标签 -->
    <v-tabs
      v-model="activeTab"
      color="primary"
      class="mb-6"
      show-arrows
      @update:model-value="switchTab"
    >
      <v-tab v-for="tab in tabs" :key="tab.key" :value="tab.key" class="text-none">
        <v-icon :icon="tab.icon" :color="`${tab.color}-darken-1`" size="18" class="mr-2"></v-icon>
        {{ tab.label }}
      </v-tab>
    </v-tabs>

    <!-- 空状态 -->
    <div v-if="!loading && deckList.length === 0" class="text-center py-12">
      <v-icon icon="mdi-cards-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无需要审核的记忆卡片组</p>
    </div>

    <!-- 卡片组列表 -->
    <div v-if="deckList.length > 0">
      <div
        v-for="deck in deckList"
        :key="deck.id"
        v-intersect="{
          handler: (isIntersecting: boolean) => {
            if (isIntersecting && deck === deckList[deckList.length - 1] && hasMore && !loading) {
              loadMore()
            }
          },
        }"
        class="mb-4"
      >
        <v-card flat class="border rounded-lg pa-4" hover>
          <div class="d-flex align-start">
            <!-- 状态和操作区域 -->
            <div class="mr-6 action-area">
              <div class="mb-3">
                <v-chip variant="flat" :color="getStateColor(deck.state)" rounded="lg" size="small">
                  {{ getStateText(deck.state) }}
                </v-chip>
              </div>

              <!-- 待审核状态 -->
              <div v-if="deck.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                <v-btn variant="flat" color="green-lighten-4" rounded="lg" size="small" @click="approveDeck(deck)">
                  <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                  批准
                </v-btn>
                <v-btn variant="flat" color="red-lighten-4" rounded="lg" size="small" @click="rejectDeck(deck)">
                  <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                  拒绝
                </v-btn>
                <v-btn variant="flat" color="grey-lighten-2" rounded="lg" size="small" @click="banDeck(deck)">
                  <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                  屏蔽
                </v-btn>
              </div>

              <!-- 已通过状态 -->
              <div v-if="deck.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                <v-btn variant="flat" color="orange-lighten-4" rounded="lg" size="small" @click="rejectDeck(deck)">
                  <v-icon icon="mdi-undo" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                  撤销通过
                </v-btn>
                <v-btn variant="flat" color="grey-lighten-2" rounded="lg" size="small" @click="banDeck(deck)">
                  <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                  屏蔽
                </v-btn>
              </div>

              <!-- 已拒绝状态 -->
              <div v-if="deck.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                <v-btn variant="flat" color="green-lighten-4" rounded="lg" size="small" @click="approveDeck(deck)">
                  <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                  通过
                </v-btn>
                <v-btn variant="flat" color="grey-lighten-2" rounded="lg" size="small" @click="banDeck(deck)">
                  <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                  屏蔽
                </v-btn>
              </div>

              <!-- 已封禁状态 -->
              <div v-if="deck.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                <v-btn variant="flat" color="blue-lighten-4" rounded="lg" size="small" @click="unbanDeck(deck)">
                  <v-icon icon="mdi-lock-open" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                  取消屏蔽
                </v-btn>
                <v-btn variant="flat" color="orange-lighten-4" rounded="lg" size="small" @click="rejectDeck(deck)">
                  <v-icon icon="mdi-arrow-down" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                  降级为拒绝
                </v-btn>
              </div>
            </div>

            <!-- 卡片组内容区域 -->
            <div class="flex-grow-1">
              <!-- 来源信息 -->
              <div class="d-flex align-center justify-space-between mb-2">
                <div class="d-flex align-center text-body-1 font-weight-medium text-grey-darken-3">
                  <template v-if="deck.course || deck.node">
                    <span v-if="deck.course">{{ deck.course.name }}</span>
                    <v-icon v-if="deck.course && deck.node" icon="mdi-chevron-right" size="18" class="mx-1 text-grey"></v-icon>
                    <span v-if="deck.node">{{ deck.node.name }}</span>
                  </template>
                  <span v-else class="text-grey-darken-1">帖子 #{{ deck.postId }}</span>
                </div>
                <div class="d-flex align-center text-body-2 text-grey-darken-1">
                  <span>{{ deck.cardCount || 0 }} 张卡片</span>
                  <span class="mx-2">{{ deck.likeCount || 0 }} 点赞</span>
                </div>
              </div>

              <!-- 创建者 + 时间 -->
              <div class="d-flex align-center mb-3 text-body-2 text-grey-darken-1">
                <v-avatar size="20" class="mr-1">
                  <v-img v-if="deck.creator?.avatar" :src="deck.creator.avatar" />
                  <v-icon v-else icon="mdi-account-circle" size="20" color="grey"></v-icon>
                </v-avatar>
                <span>{{ deck.creator?.name || '匿名用户' }}</span>
                <span v-if="deck.postId" class="ml-3">帖子 #{{ deck.postId }}</span>
                <span class="ml-3">{{ new Date(deck.createdAt).toLocaleDateString() }}</span>
              </div>

              <!-- 描述 -->
              <div v-if="deck.description" class="mb-3">
                <p class="text-body-2 text-grey-darken-1 mb-0">{{ deck.description }}</p>
              </div>

              <!-- 卡片列表 -->
              <div v-if="deck.cards && deck.cards.length > 0" class="cards-area">
                <div
                  v-for="(card, index) in deck.cards"
                  :key="card.id"
                  class="card-row"
                  :class="{ 'border-bottom': index < deck.cards.length - 1 }"
                >
                  <div class="card-index">{{ index + 1 }}</div>
                  <div class="card-qa">
                    <div class="qa-line">
                      <span class="qa-tag q">Q</span>
                      <span>{{ card.front }}</span>
                    </div>
                    <div class="qa-line">
                      <span class="qa-tag a">A</span>
                      <span>{{ card.back }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </v-card>
      </div>
    </div>

    <!-- 加载指示器 -->
    <div v-if="loading" class="text-center py-4">
      <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
      <span class="ml-2 text-grey-darken-1">加载中...</span>
    </div>

    <!-- 没有更多数据提示 -->
    <div v-if="!hasMore && deckList.length > 0" class="text-center py-4">
      <span class="text-grey-darken-1">没有更多数据了</span>
    </div>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="currentDeck?.node?.name || `卡片组 #${currentDeck?.id}`"
      :item-state="currentDeck?.state"
      item-type="卡片组"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />
  </div>
</template>

<style scoped>
.action-area {
  min-width: 140px;
  background-color: #fafafa;
  border-radius: 8px;
  padding: 16px;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.cards-area {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.card-row {
  display: flex;
  align-items: flex-start;
  padding: 10px 14px;
  gap: 10px;
}

.card-row:nth-child(odd) {
  background-color: #fafafa;
}

.card-row.border-bottom {
  border-bottom: 1px solid #f0f0f0;
}

.card-index {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #bdbdbd;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  margin-top: 2px;
}

.card-qa {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.qa-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: #424242;
  word-break: break-word;
}

.qa-tag {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  margin-top: 1px;
}

.qa-tag.q {
  background-color: #e3f2fd;
  color: #1565c0;
}

.qa-tag.a {
  background-color: #e8f5e9;
  color: #2e7d32;
}
</style>
