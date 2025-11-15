<script setup lang="ts">
import { inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { adminApi } from '@/api'
import { ContentState, ApprovalAction } from '@/enums'
import type { DeckDetail } from '@/types/memoryCard'
import RejectBanDialog from './RejectBanDialog.vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

const { t } = useI18n()
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
    color: 'orange'
  },
  {
    key: 'approved',
    label: '已通过',
    state: ContentState.PUBLISHED,
    icon: 'mdi-check-circle',
    color: 'green'
  },
  {
    key: 'rejected',
    label: '已拒绝',
    state: ContentState.REJECTED,
    icon: 'mdi-close-circle',
    color: 'red'
  },
  {
    key: 'banned',
    label: '已封禁',
    state: ContentState.BANNED,
    icon: 'mdi-cancel',
    color: 'grey'
  }
]

// 筛选条件
const filterForm = ref({
  postId: null as number | null,
  creatorId: null as number | null
})

const getCurrentTab = () => tabs.find(tab => tab.key === activeTab.value) || tabs[0]

// 使用 useInfiniteScroll 加载卡片组列表
const {
  items: deckList,
  loading,
  hasMore,
  loadMore,
  reset: resetDeckList
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

    return response.data
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id
  }),
  initialParams: {
    lastId: undefined
  }
})

const applyFilter = () => {
  resetDeckList()
}

const resetFilter = () => {
  filterForm.value = {
    postId: null,
    creatorId: null
  }
  applyFilter()
}

// 使用 useMutation 批准卡片组
const { execute: executeApproveDeck } = useMutation(
  (deckId: number) => adminApi.approveDeck(deckId, { action: ApprovalAction.APPROVE }),
  {
    successMessage: '卡片组审核通过',
    onSuccess: (_, deckId) => {
      const index = deckList.value.findIndex(d => d.id === deckId)
      if (index > -1) {
        deckList.value.splice(index, 1)
      }
    }
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
    adminApi.approveDeck(data.deckId, { action: data.action, reason: data.reason }),
  {
    onSuccess: (_, data) => {
      const message = data.action === ApprovalAction.BAN ? '卡片组已屏蔽' : '卡片组已拒绝'
      showSnackbar?.(message, 'success')

      const index = deckList.value.findIndex(d => d.id === data.deckId)
      if (index > -1) {
        deckList.value.splice(index, 1)
      }

      showReasonDialog.value = false
      currentDeck.value = null
    }
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string) => {
  if (!currentDeck.value) return

  const action = dialogType.value === 'ban' ? ApprovalAction.BAN : ApprovalAction.REJECT
  await executeRejectOrBan({
    deckId: currentDeck.value.id,
    action,
    reason
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
  (deckId: number) => adminApi.approveDeck(deckId, { action: ApprovalAction.APPROVE }),
  {
    successMessage: '卡片组已取消屏蔽',
    onSuccess: (_, deckId) => {
      const index = deckList.value.findIndex(d => d.id === deckId)
      if (index > -1) {
        deckList.value.splice(index, 1)
      }
    }
  }
)

const unbanDeck = async (deck: DeckDetail): Promise<void> => {
  await executeUnbanDeck(deck.id)
}

const switchTab = (tabKey: string) => {
  resetDeckList()
}

const getStateText = (state: number): string => {
  switch (state) {
    case ContentState.SUBMITTED: return '待审核'
    case ContentState.PUBLISHED: return '已通过'
    case ContentState.REJECTED: return '已拒绝'
    case ContentState.BANNED: return '已封禁'
    default: return '未知'
  }
}

const getStateColor = (state: number): string => {
  switch (state) {
    case ContentState.SUBMITTED: return 'orange-lighten-4'
    case ContentState.PUBLISHED: return 'green-lighten-4'
    case ContentState.REJECTED: return 'red-lighten-4'
    case ContentState.BANNED: return 'grey-lighten-2'
    default: return 'grey-lighten-3'
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
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">
            记忆卡片审核
          </h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">审核用户提交的记忆卡片组</p>
        </div>
      </div>
      <v-chip variant="flat" color="purple-lighten-4" rounded="lg">
        <v-icon
          icon="mdi-cards-outline"
          color="purple-darken-2"
          size="16"
          class="mr-1"
        ></v-icon>
        <span class="text-purple-darken-2 text-caption">
          {{ deckList.length }} 个卡片组
        </span>
      </v-chip>
    </div>

    <!-- 筛选区域 -->
    <v-card flat class="pa-4 bg-grey-lighten-5 rounded-lg mb-6">
      <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
        <v-icon icon="mdi-filter-outline" size="16" class="mr-2"></v-icon>
        高级筛选
      </h4>
      <v-row dense>
        <v-col cols="12" md="4">
          <v-text-field
            v-model.number="filterForm.postId"
            label="帖子ID"
            type="number"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="4">
          <v-text-field
            v-model.number="filterForm.creatorId"
            label="用户ID"
            type="number"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="4">
          <div class="d-flex gap-2">
            <v-btn
              variant="flat"
              color="primary"
              rounded="lg"
              @click="applyFilter"
            >
              <v-icon icon="mdi-magnify" class="mr-1"></v-icon>
              筛选
            </v-btn>
            <v-btn
              variant="outlined"
              color="grey"
              rounded="lg"
              @click="resetFilter"
            >
              <v-icon icon="mdi-refresh" class="mr-1"></v-icon>
              重置
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </v-card>

    <!-- 状态标签 -->
    <v-tabs
      v-model="activeTab"
      color="primary"
      class="mb-6"
      show-arrows
      @update:model-value="switchTab"
    >
      <v-tab
        v-for="tab in tabs"
        :key="tab.key"
        :value="tab.key"
        class="text-none"
      >
        <v-icon
          :icon="tab.icon"
          :color="`${tab.color}-darken-1`"
          size="18"
          class="mr-2"
        ></v-icon>
        {{ tab.label }}
      </v-tab>
    </v-tabs>

    <!-- 加载状态 -->
    <div v-if="loading && deckList.length === 0" class="text-center py-12">
      <v-progress-circular indeterminate color="purple" size="48"></v-progress-circular>
      <p class="text-body-1 text-grey-darken-1 mt-4">正在加载待审核卡片组...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && deckList.length === 0" class="text-center py-12">
      <v-icon
        icon="mdi-cards-outline"
        size="48"
        color="grey-lighten-1"
        class="mb-4"
      ></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无需要审核的记忆卡片组</p>
    </div>

    <!-- 卡片组列表 -->
    <div v-if="deckList.length > 0">
      <div
        v-for="deck in deckList"
        :key="deck.id"
        class="mb-4"
        v-intersect="{
          handler: (isIntersecting: boolean) => {
            if (isIntersecting && deck === deckList[deckList.length - 1] && hasMore && !loading) {
              loadMore()
            }
          }
        }"
      >
          <v-card flat class="border rounded-lg pa-4" hover>
            <div class="d-flex align-start">
              <!-- 状态和操作区域 -->
              <div class="mr-8 action-area">
                <div class="mb-3">
                  <v-chip
                    v-if="deck.state === ContentState.SUBMITTED"
                    variant="flat"
                    color="orange-lighten-4"
                    rounded="lg"
                    size="small"
                  >
                    <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                    {{ t('admin.pending') }}
                  </v-chip>
                  <v-chip
                    v-if="deck.state === ContentState.PUBLISHED"
                    variant="flat"
                    color="green-lighten-4"
                    rounded="lg"
                    size="small"
                  >
                    <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                    {{ t('admin.approved') }}
                  </v-chip>
                  <v-chip
                    v-if="deck.state === ContentState.REJECTED"
                    variant="flat"
                    color="red-lighten-4"
                    rounded="lg"
                    size="small"
                  >
                    <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                    {{ t('admin.rejected') }}
                  </v-chip>
                  <v-chip
                    v-if="deck.state === ContentState.BANNED"
                    variant="flat"
                    color="grey-lighten-2"
                    rounded="lg"
                    size="small"
                  >
                    <v-icon icon="mdi-cancel" size="14" class="mr-1"></v-icon>
                    {{ t('admin.banned') }}
                  </v-chip>
                </div>

                <!-- 待审核状态：批准、拒绝、屏蔽 -->
                <div v-if="deck.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                  <v-btn
                    variant="flat"
                    color="green-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="approveDeck(deck)"
                  >
                    <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                    批准
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="red-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="rejectDeck(deck)"
                  >
                    <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                    拒绝
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="grey-lighten-2"
                    rounded="lg"
                    size="small"
                    @click="banDeck(deck)"
                  >
                    <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已通过状态：撤销通过、屏蔽 -->
                <div v-if="deck.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                  <v-btn
                    variant="flat"
                    color="orange-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="rejectDeck(deck)"
                  >
                    <v-icon icon="mdi-undo" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                    撤销通过
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="grey-lighten-2"
                    rounded="lg"
                    size="small"
                    @click="banDeck(deck)"
                  >
                    <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已拒绝状态：通过、屏蔽 -->
                <div v-if="deck.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                  <v-btn
                    variant="flat"
                    color="green-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="approveDeck(deck)"
                  >
                    <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                    通过
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="grey-lighten-2"
                    rounded="lg"
                    size="small"
                    @click="banDeck(deck)"
                  >
                    <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已封禁状态：取消屏蔽、降级为拒绝 -->
                <div v-if="deck.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                  <v-btn
                    variant="flat"
                    color="blue-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="unbanDeck(deck)"
                  >
                    <v-icon icon="mdi-lock-open" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                    取消屏蔽
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="orange-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="rejectDeck(deck)"
                  >
                    <v-icon icon="mdi-arrow-down" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                    降级为拒绝
                  </v-btn>
                </div>
              </div>

              <!-- 卡片组内容区域 -->
              <div class="flex-grow-1">
                <div class="d-flex align-center justify-space-between mb-2">
                  <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                    {{ deck.title }}
                  </h4>
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-cards-outline" size="16" color="grey-darken-2" class="mr-1"></v-icon>
                    <span class="text-body-2 text-grey-darken-2">{{ deck.cardCount || 0 }} 张卡片</span>
                    <v-icon icon="mdi-thumb-up-outline" size="16" color="grey-darken-2" class="ml-3 mr-1"></v-icon>
                    <span class="text-body-2 text-grey-darken-2">{{ deck.upvoteCount || 0 }} 点赞</span>
                  </div>
                </div>

                <div v-if="deck.description" class="mb-3">
                  <p class="text-body-2 text-grey-darken-1">{{ deck.description }}</p>
                </div>

                <!-- 卡片预览区域 -->
                <div class="cards-preview-area mb-3">
                  <div class="preview-header">
                    <v-icon icon="mdi-cards-outline" size="18" color="purple-darken-1" class="mr-2"></v-icon>
                    <h5 class="text-subtitle-2 font-weight-medium text-grey-darken-2 mb-0">
                      卡片内容预览 ({{ deck.cards?.length || 0 }})
                    </h5>
                  </div>
                  <div class="cards-container">
                    <div v-if="!deck.cards || deck.cards.length === 0" class="empty-state">
                      <v-icon icon="mdi-cards-outline" size="24" color="grey-lighten-2" class="mb-2"></v-icon>
                      <p class="text-body-2 text-grey-darken-1 mb-0">暂无卡片内容</p>
                    </div>
                    <div v-else class="cards-list">
                      <div
                        v-for="(card, index) in deck.cards"
                        :key="card.id"
                        class="card-item"
                        :class="{ 'border-bottom': index < deck.cards.length - 1 }"
                      >
                        <div class="card-index">
                          <span class="index-number">{{ index + 1 }}</span>
                        </div>
                        <div class="card-content-wrapper">
                          <div class="card-qa-pair">
                            <div class="qa-item question">
                              <div class="qa-row">
                                <div class="qa-label">
                                  <v-icon icon="mdi-help-circle-outline" size="14" color="blue-darken-2"></v-icon>
                                  <span class="label-text">问题</span>
                                </div>
                                <div class="qa-content">{{ card.front }}</div>
                              </div>
                            </div>
                            <div class="qa-item answer">
                              <div class="qa-row">
                                <div class="qa-label">
                                  <v-icon icon="mdi-lightbulb-outline" size="14" color="green-darken-2"></v-icon>
                                  <span class="label-text">答案</span>
                                </div>
                                <div class="qa-content">{{ card.back }}</div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="d-flex align-center justify-space-between">
                  <div class="d-flex align-center">
                    <v-avatar size="24" class="mr-2">
                      <v-img v-if="deck.creator?.avatar" :src="deck.creator.avatar" />
                      <v-icon v-else icon="mdi-account-circle" size="16" color="grey"></v-icon>
                    </v-avatar>
                    <span class="text-body-2 text-grey-darken-2">
                      {{ deck.creator?.name || '匿名用户' }}
                    </span>
                  </div>
                  <div class="text-body-2 text-grey-darken-1">
                    创建时间：{{ new Date(deck.createdAt).toLocaleDateString() }}
                  </div>
                </div>
              </div>
            </div>
        </v-card>
      </div>
    </div>

    <!-- 加载更多指示器 -->
    <div v-if="loading" class="text-center py-4">
      <v-progress-circular
        indeterminate
        color="primary"
        size="24"
      ></v-progress-circular>
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
      :item-name="currentDeck?.title || ''"
      :item-state="currentDeck?.state"
      item-type="卡片组"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />
  </div>
</template>

<style scoped>
.action-area {
  min-width: 180px;
  background-color: #fafafa;
  border-radius: 8px;
  padding: 18px;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

/* 卡片预览区域 */
.cards-preview-area {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  background: linear-gradient(145deg, #fafafa 0%, #f5f5f5 100%);
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
}

.cards-container {
  max-height: 280px;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  color: #9e9e9e;
}

.cards-list {
  padding: 0;
}

.card-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  background: white;
  transition: background-color 0.2s ease;
}

.card-item:hover {
  background-color: #f8f9fa;
}

.card-item.border-bottom {
  border-bottom: 1px solid #f0f0f0;
}

.card-index {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  margin-right: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #999ccc;
  border-radius: 50%;
  margin-top: 2px;
}

.index-number {
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.card-content-wrapper {
  flex: 1;
  min-width: 0;
}

.card-qa-pair {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.qa-item {
  margin-bottom: 0px;
}

.qa-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.qa-label {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  min-width: 60px;
}

.label-text {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.question .label-text {
  color: #1565c0;
}

.answer .label-text {
  color: #2e7d32;
}

.qa-content {
  flex: 1;
  font-size: 14px;
  line-height: 1.5;
  color: #424242;
  word-break: break-word;
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid transparent;
}

.question .qa-content {
  border-left-color: #2196f3;
  background-color: #f3f8ff;
}

.answer .qa-content {
  border-left-color: #4caf50;
  background-color: #f1f8e9;
}

/* 滚动条样式 */
.cards-container::-webkit-scrollbar {
  width: 6px;
}

.cards-container::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

.cards-container::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.cards-container::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}
</style>