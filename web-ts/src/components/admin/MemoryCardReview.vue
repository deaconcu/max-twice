<script setup lang="ts">
import { inject, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MemoryService } from '@/services/memoryService'
import { DeckState } from '@/types/memoryCard'
import type { DeckDetail } from '@/types/memoryCard'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

const activeTab = ref<'pending' | 'reviewed' | 'blocked'>('pending')
const deckList = ref<DeckDetail[]>([])
const loading = ref(false)
const lastId = ref<number | undefined>(undefined)
const hasMore = ref(true)

// 筛选条件
const filterForm = ref({
  postId: null as number | null,
  creatorId: null as number | null
})

const loadDecks = async (reset = false): Promise<void> => {
  if (loading.value) return
  
  loading.value = true
  try {
    let state: number
    if (activeTab.value === 'pending') {
      state = DeckState.PENDING
    } else if (activeTab.value === 'reviewed') {
      state = DeckState.NORMAL
    } else {
      state = DeckState.BLOCKED
    }
    
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
        deckList.value = [...deckList.value, ...newDecks]
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

const loadMore = async ({ done }: { done: (status: string) => void }): Promise<void> => {
  if (loading.value || !hasMore.value) {
    done('empty')
    return
  }
  
  await loadDecks(false)
  done(hasMore.value ? 'ok' : 'empty')
}

const switchTab = (tab: 'pending' | 'reviewed' | 'blocked') => {
  activeTab.value = tab
  deckList.value = []
  lastId.value = undefined
  hasMore.value = true
  loadDecks(true)
}

const applyFilter = () => {
  deckList.value = []
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

const getStateText = (state: DeckState): string => {
  switch (state) {
    case DeckState.PENDING: return '待审核'
    case DeckState.NORMAL: return '正常'
    case DeckState.BLOCKED: return '已屏蔽'
    case DeckState.PRIVATE: return '私有'
    default: return '未知'
  }
}

const getStateColor = (state: DeckState): string => {
  switch (state) {
    case DeckState.PENDING: return 'orange-lighten-4'
    case DeckState.NORMAL: return 'green-lighten-4'
    case DeckState.BLOCKED: return 'red-lighten-4'
    case DeckState.PRIVATE: return 'blue-lighten-4'
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

    <!-- 筛选条件 -->
    <v-card flat class="border rounded-lg pa-4 mb-6">
      <div class="d-flex align-center mb-3">
        <v-icon icon="mdi-filter-variant" size="18" color="grey-darken-2" class="mr-2"></v-icon>
        <h5 class="text-subtitle-2 font-weight-medium text-grey-darken-2 mb-0">筛选条件</h5>
      </div>
      <v-row>
        <v-col cols="12" md="4">
          <v-text-field
            v-model.number="filterForm.postId"
            label="帖子ID"
            type="number"
            variant="outlined"
            density="compact"
            clearable
            prepend-inner-icon="mdi-post"
            hide-spin-buttons
            @keyup.enter="applyFilter"
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="4">
          <v-text-field
            v-model.number="filterForm.creatorId"
            label="用户ID"
            type="number"
            variant="outlined"
            density="compact"
            clearable
            prepend-inner-icon="mdi-account"
            hide-spin-buttons
            @keyup.enter="applyFilter"
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="4">
          <div class="d-flex gap-2">
            <v-btn
              color="primary"
              variant="flat"
              rounded="lg"
              class="mr-2"
              prepend-icon="mdi-magnify"
              @click="applyFilter"
            >
              筛选
            </v-btn>
            <v-btn
              color="grey"
              variant="outlined"
              rounded="lg"
              prepend-icon="mdi-refresh"
              @click="resetFilter"
            >
              重置
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </v-card>

    <!-- 标签页 -->
    <div class="mb-6">
      <v-tabs v-model="activeTab" color="purple" density="compact" class="mb-4">
        <v-tab value="pending" @click="switchTab('pending')">
          <v-icon icon="mdi-clock-outline" size="16" class="mr-2"></v-icon>
          待审核
          <v-chip 
            v-if="activeTab === 'pending'" 
            size="x-small" 
            color="orange" 
            variant="flat" 
            class="ml-2"
          >
            {{ activeTab === 'pending' ? deckList.length : 0 }}
          </v-chip>
        </v-tab>
        <v-tab value="reviewed" @click="switchTab('reviewed')">
          <v-icon icon="mdi-check-circle-outline" size="16" class="mr-2"></v-icon>
          已审核
          <v-chip 
            v-if="activeTab === 'reviewed'" 
            size="x-small" 
            color="success" 
            variant="flat" 
            class="ml-2"
          >
            {{ activeTab === 'reviewed' ? deckList.length : 0 }}
          </v-chip>
        </v-tab>
        <v-tab value="blocked" @click="switchTab('blocked')">
          <v-icon icon="mdi-block-helper" size="16" class="mr-2"></v-icon>
          已屏蔽
          <v-chip 
            v-if="activeTab === 'blocked'" 
            size="x-small" 
            color="error" 
            variant="flat" 
            class="ml-2"
          >
            {{ activeTab === 'blocked' ? deckList.length : 0 }}
          </v-chip>
        </v-tab>
      </v-tabs>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-12">
      <v-progress-circular indeterminate color="purple" size="48"></v-progress-circular>
      <p class="text-body-1 text-grey-darken-1 mt-4">正在加载待审核卡片组...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="deckList.length === 0" class="text-center py-12">
      <v-icon
        icon="mdi-cards-outline"
        size="48"
        color="grey-lighten-1"
        class="mb-4"
      ></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无需要审核的记忆卡片组</p>
    </div>

    <!-- 卡片组列表 -->
    <div v-else>
      <v-infinite-scroll
        @load="loadMore"
        :loading="loading"
        mode="intersect"
        side="end"
      >
        <div v-for="deck in deckList" :key="deck.id" class="mb-4">
          <v-card flat class="border rounded-lg pa-4" hover>
            <div class="d-flex align-start">
              <!-- 状态和操作区域 -->
              <div class="mr-8 action-area">
                <div class="mb-3">
                  <v-chip
                    variant="flat"
                    :color="getStateColor(deck.state)"
                    rounded="lg"
                    size="small"
                  >
                    <span class="text-caption font-weight-medium" :class="{
                      'text-orange-darken-2': deck.state === DeckState.PENDING,
                      'text-green-darken-2': deck.state === DeckState.NORMAL,
                      'text-red-darken-2': deck.state === DeckState.BLOCKED
                    }">
                      {{ getStateText(deck.state) }}
                    </span>
                  </v-chip>
                </div>

                <!-- 待审核状态的操作 -->
                <div v-if="activeTab === 'pending'" class="d-flex flex-column" style="gap: 8px;">
                  <v-btn
                    color="green"
                    variant="flat"
                    rounded="lg"
                    size="small"
                    prepend-icon="mdi-check"
                    @click="approveDeck(deck, true)"
                  >
                    通过
                  </v-btn>
                  <v-btn
                    color="red"
                    variant="outlined"
                    rounded="lg"
                    size="small"
                    prepend-icon="mdi-close"
                    @click="approveDeck(deck, false)"
                  >
                    废弃
                  </v-btn>
                </div>

                <!-- 已审核状态的操作 -->
                <div v-else-if="activeTab === 'reviewed'" class="d-flex flex-column" style="gap: 8px;">
                  <v-btn
                    color="red"
                    variant="outlined"
                    rounded="lg"
                    size="small"
                    prepend-icon="mdi-delete"
                    @click="discardDeck(deck)"
                  >
                    废弃
                  </v-btn>
                </div>

                <!-- 已屏蔽状态的操作 -->
                <div v-else-if="activeTab === 'blocked'" class="d-flex flex-column" style="gap: 8px;">
                  <v-btn
                    color="green"
                    variant="flat"
                    rounded="lg"
                    size="small"
                    prepend-icon="mdi-restore"
                    @click="restoreDeck(deck)"
                  >
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
      </v-infinite-scroll>
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