<script setup lang="ts">
import { inject, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MemoryService } from '@/services/memoryService'
import { ContentState } from '@/types/enums'
import type { DeckDetail } from '@/types/memoryCard'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

const activeTab = ref<string>('pending')
const deckList = ref<DeckDetail[]>([])
const loading = ref(false)
const lastId = ref<number | undefined>(undefined)
const hasMore = ref(true)

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

const loadDecks = async (reset = false): Promise<void> => {
  if (loading.value) return

  loading.value = true
  try {
    const currentTab = getCurrentTab()
    const state = currentTab.state

    const response = await MemoryService.getDecksForReview({
      state: state,
      postId: filterForm.value.postId || undefined,
      creatorId: filterForm.value.creatorId || undefined,
      lastId: reset ? undefined : lastId.value,
      limit: 20,
      sortBy: 'createdAt',
      sortOrder: 'desc'
    })

    if (response.code === 200) {
      const responseData = response.data
      const newDecks = responseData.items || []

      if (reset) {
        deckList.value = newDecks
      } else {
        deckList.value.push(...newDecks)
      }

      hasMore.value = responseData.hasMore || false

      // 更新lastId - 使用 nextCursor 中的信息
      if (responseData.nextCursor?.lastId) {
        lastId.value = responseData.nextCursor.lastId
      } else if (reset) {
        lastId.value = undefined
      }
    }

  } catch (error) {
    console.error('Error loading decks:', error)
    showSnackbar?.('加载卡片组失败', 'error')
  } finally {
    loading.value = false
  }
}

const loadMore = (): void => {
  if (!loading.value && hasMore.value) {
    loadDecks(false)
  }
}

const switchTab = (tabKey: string) => {
  activeTab.value = tabKey
  lastId.value = undefined
  hasMore.value = true
  loadDecks(true)
}

const applyFilter = () => {
  lastId.value = undefined
  hasMore.value = true
  loadDecks(true)
}

const resetFilter = () => {
  filterForm.value = {
    postId: null,
    creatorId: null
  }
  applyFilter()
}

const approveDeck = async (deck: DeckDetail, approve: boolean): Promise<void> => {
  try {
    if (approve) {
      await MemoryService.approveDeck(deck.id)
    } else {
      await MemoryService.discardDeck(deck.id)
    }

    // 从列表中移除已审核的项目
    const index = deckList.value.findIndex(d => d.id === deck.id)
    if (index > -1) {
      deckList.value.splice(index, 1)
    }

    showSnackbar?.(approve ? '卡片组审核通过' : '卡片组已废弃', 'success')
  } catch (error) {
    console.error('Error approving deck:', error)
    showSnackbar?.('审核操作失败', 'error')
  }
}

const discardDeck = async (deck: DeckDetail): Promise<void> => {
  try {
    await MemoryService.discardDeck(deck.id)

    // 从列表中移除已废弃的项目
    const index = deckList.value.findIndex(d => d.id === deck.id)
    if (index > -1) {
      deckList.value.splice(index, 1)
    }

    showSnackbar?.('卡片组已废弃', 'success')
  } catch (error) {
    console.error('Error discarding deck:', error)
    showSnackbar?.('废弃操作失败', 'error')
  }
}

const restoreDeck = async (deck: DeckDetail): Promise<void> => {
  try {
    await MemoryService.restoreDeck(deck.id)
    
    // 从列表中移除已恢复的项目
    const index = deckList.value.findIndex(d => d.id === deck.id)
    if (index > -1) {
      deckList.value.splice(index, 1)
    }
    
    showSnackbar?.('卡片组已恢复', 'success')
  } catch (error) {
    console.error('Error restoring deck:', error)
    showSnackbar?.('恢复操作失败', 'error')
  }
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

onMounted(() => {
  loadDecks(true)
})
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
          handler: (isIntersecting) => {
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

                <!-- 待审核状态的操作 -->
                <div v-if="deck.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                  <v-btn
                    variant="flat"
                    color="green-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="approveDeck(deck, true)"
                  >
                    <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                    {{ t('admin.approve') }}
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="red-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="approveDeck(deck, false)"
                  >
                    <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                    {{ t('admin.reject') }}
                  </v-btn>
                </div>

                <!-- 已通过状态下显示屏蔽按钮 -->
                <div v-if="deck.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                  <v-btn
                    variant="flat"
                    color="red-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="approveDeck(deck, false)"
                  >
                    <v-icon icon="mdi-block-helper" color="red-darken-2" size="16" class="mr-1"></v-icon>
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已拒绝状态下显示通过按钮 -->
                <div v-if="deck.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                  <v-btn
                    variant="flat"
                    color="green-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="approveDeck(deck, true)"
                  >
                    <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                    {{ t('admin.approve') }}
                  </v-btn>
                </div>

                <!-- 已封禁状态下显示恢复按钮 -->
                <div v-if="deck.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                  <v-btn
                    variant="flat"
                    color="orange-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="restoreDeck(deck)"
                  >
                    <v-icon icon="mdi-restore" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                    恢复
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
                    <span class="text-body-2 text-grey-darken-2">{{ deck.cardCount }} 张卡片</span>
                    <v-icon icon="mdi-thumb-up-outline" size="16" color="grey-darken-2" class="ml-3 mr-1"></v-icon>
                    <span class="text-body-2 text-grey-darken-2">{{ deck.upvoteCount }} 点赞</span>
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