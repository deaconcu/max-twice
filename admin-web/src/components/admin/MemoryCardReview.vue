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
    <h2 class="text-h5 font-weight-bold mb-4">记忆卡片管理</h2>

    <!-- ID查询 -->
    <v-card flat class="border mb-4">
      <v-card-text>
        <v-row align="center">
          <v-col cols="3">
            <v-text-field
              v-model.number="filterForm.postId"
              label="帖子 ID"
              type="number"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="applyFilter"
            ></v-text-field>
          </v-col>
          <v-col cols="3">
            <v-text-field
              v-model.number="filterForm.creatorId"
              label="用户 ID"
              type="number"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="applyFilter"
            ></v-text-field>
          </v-col>
          <v-col cols="auto">
            <v-btn variant="tonal" size="default" @click="applyFilter">
              <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
              筛选
            </v-btn>
            <v-btn
              v-if="filterForm.postId || filterForm.creatorId"
              variant="text"
              size="default"
              @click="resetFilter"
            >
              清除
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 卡片组列表 -->
    <v-card flat class="border">
      <v-card-text>
        <!-- 状态标签 -->
        <v-tabs
          v-model="activeTab"
          color="primary"
          density="compact"
          @update:model-value="switchTab"
          class="mb-4"
        >
          <v-tab v-for="tab in tabs" :key="tab.key" :value="tab.key" class="text-none" size="small">
            <v-icon :icon="tab.icon" size="14" class="mr-1"></v-icon>
            {{ tab.label }}
          </v-tab>
        </v-tabs>

        <!-- 首次加载状态 -->
        <div v-if="loading && deckList.length === 0" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!loading && deckList.length === 0" class="text-center py-12">
          <v-icon icon="mdi-cards-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">
            暂无{{ tabs.find((tab) => tab.key === activeTab)?.label }}的卡片组
          </p>
        </div>

        <!-- 列表 -->
        <div v-else>
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
            class="list-item mb-3"
          >
            <div class="d-flex align-start">
              <!-- 操作区 -->
              <div class="action-area mr-4">
                <!-- 待审核 -->
                <div v-if="deck.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block @click="approveDeck(deck)">
                    批准
                  </v-btn>
                  <v-btn variant="tonal" color="error" size="small" block @click="rejectDeck(deck)">
                    拒绝
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="banDeck(deck)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已通过 -->
                <div v-if="deck.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="warning" size="small" block @click="rejectDeck(deck)">
                    撤回
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="banDeck(deck)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已拒绝 -->
                <div v-if="deck.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block @click="approveDeck(deck)">
                    通过
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="banDeck(deck)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已封禁 -->
                <div v-if="deck.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="info" size="small" block @click="unbanDeck(deck)">
                    解封
                  </v-btn>
                  <v-btn variant="tonal" color="warning" size="small" block @click="rejectDeck(deck)">
                    降级
                  </v-btn>
                </div>
              </div>

              <!-- 内容区 -->
              <div class="flex-grow-1">
                <!-- 标题行 -->
                <div class="d-flex align-center justify-space-between mb-2">
                  <div class="d-flex align-center">
                    <div class="text-body-1 font-weight-medium text-grey-darken-3">
                      <template v-if="deck.course || deck.node">
                        <span v-if="deck.course">{{ deck.course.name }}</span>
                        <span v-if="deck.course && deck.node" class="mx-1">/</span>
                        <span v-if="deck.node">{{ deck.node.name }}</span>
                      </template>
                      <span v-else>卡片组 #{{ deck.id }}</span>
                    </div>
                    <v-chip variant="flat" :color="getStateColor(deck.state)" size="x-small" class="ml-2">
                      {{ getStateText(deck.state) }}
                    </v-chip>
                  </div>
                  <div class="d-flex align-center text-caption text-grey-darken-1">
                    <span>{{ deck.creator?.name || '匿名' }}</span>
                    <span class="mx-1">·</span>
                    <span>{{ new Date(deck.createdAt).toLocaleDateString() }}</span>
                    <span class="mx-1">·</span>
                    <span>ID: {{ deck.id }}</span>
                    <span v-if="deck.postId" class="mx-1">·</span>
                    <span v-if="deck.postId">帖子 #{{ deck.postId }}</span>
                  </div>
                </div>

                <!-- 内容 -->
                <div class="content-wrapper">
                  <!-- 描述 -->
                  <div v-if="deck.description" class="text-body-2 text-grey-darken-1 mb-2">
                    {{ deck.description }}
                  </div>

                  <!-- 统计信息 -->
                  <div class="text-caption text-grey-darken-1 mb-2">
                    {{ deck.cardCount || 0 }} 张卡片 · {{ deck.likeCount || 0 }} 赞
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

                  <!-- 拒绝/封禁原因 -->
                  <div v-if="(deck.state === ContentState.REJECTED || deck.state === ContentState.BANNED) && deck.reason" class="mt-2">
                    <span class="text-caption text-red-darken-2">{{ deck.state === ContentState.BANNED ? '封禁' : '拒绝' }}原因：{{ deck.reason }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多指示器 -->
        <div v-if="loading && deckList.length > 0" class="text-center py-4">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 没有更多 -->
        <div v-if="!hasMore && deckList.length > 0" class="text-center py-4 text-caption text-grey">
          没有更多了
        </div>
      </v-card-text>
    </v-card>

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
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.list-item {
  padding: 16px;
  border-radius: 8px;
  background-color: #fafafa;
}

.action-area {
  width: 70px;
  flex-shrink: 0;
}

.content-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  background-color: white;
}

.cards-area {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  margin-top: 8px;
}

.card-row {
  display: flex;
  align-items: flex-start;
  padding: 10px 14px;
  gap: 10px;
}

.card-row:nth-child(odd) {
  background-color: white;
}

.card-row:nth-child(even) {
  background-color: #fafafa;
}

.card-row.border-bottom {
  border-bottom: 1px solid #f0f0f0;
}

.card-index {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #bdbdbd;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.card-qa {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.qa-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.4;
  color: #424242;
}

.qa-tag {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
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
