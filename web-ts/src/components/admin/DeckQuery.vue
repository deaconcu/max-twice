<script setup lang="ts">
import { inject, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MemoryService } from '@/services/memoryService'
import { DeckState } from '@/types/memoryCard'
import type { DeckDetail } from '@/types/memoryCard'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 查询条件
const searchForm = ref({
  nodeId: null as number | null,
  creatorId: null as number | null,
  state: null as number | null,
  keyword: ''
})

// 使用 useInfiniteScroll 管理分页数据
const {
  items: deckList,
  loading,
  hasMore,
  params,
  loadMore,
  reset: resetList
} = useInfiniteScroll<DeckDetail>({
  fetchFn: async (currentParams) => {
    const query: any = {
      limit: 20,
      sortBy: 'createdAt',
      sortOrder: 'desc',
      ...currentParams
    }

    if (searchForm.value.nodeId) {
      query.postId = searchForm.value.nodeId
    }
    if (searchForm.value.creatorId) {
      query.creatorId = searchForm.value.creatorId
    }
    if (searchForm.value.state !== null) {
      query.state = searchForm.value.state
    }

    const response = await MemoryService.getDecksForReview(query)

    if (response.code === 200) {
      const responseData = response.data
      return {
        code: 200,
        data: responseData.items || [],
        message: '',
        hasMore: responseData.hasMore || false,
        nextCursor: responseData.nextCursor
      }
    }

    throw new Error(response.message || '查询失败')
  },
  getNextParams: (lastItem, currentParams) => ({
    ...currentParams,
    lastId: lastItem.id
  }),
  initialParams: {
    lastId: undefined
  },
  onError: (error) => {
    console.error('Error searching decks:', error)
    showSnackbar?.('查询失败', 'error')
  }
})

// 重置查询
const resetSearch = () => {
  searchForm.value = {
    nodeId: null,
    creatorId: null,
    state: null,
    keyword: ''
  }
  resetList()
}

// 搜索
const handleSearch = () => {
  resetList()
  loadMore({ done: () => {} } as any)
}

// 状态文本
const getStateText = (state: DeckState): string => {
  switch (state) {
    case DeckState.PENDING: return '待审核'
    case DeckState.NORMAL: return '正常'
    case DeckState.BLOCKED: return '已屏蔽'
    case DeckState.PRIVATE: return '私有'
    default: return '未知'
  }
}

// 状态颜色
const getStateColor = (state: DeckState): string => {
  switch (state) {
    case DeckState.PENDING: return 'orange-lighten-4'
    case DeckState.NORMAL: return 'green-lighten-4'
    case DeckState.BLOCKED: return 'red-lighten-4'
    case DeckState.PRIVATE: return 'blue-lighten-4'
    default: return 'grey-lighten-3'
  }
}

// 计算是否可以搜索
const canSearch = computed(() => {
  return searchForm.value.nodeId || searchForm.value.creatorId || searchForm.value.state !== null
})
</script>

<template>
  <div>
    <!-- 页面标题 -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-blue-lighten-5 mr-3">
          <v-icon icon="mdi-magnify" color="blue-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">
            卡片组查询
          </h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">查询特定节点或用户的记忆卡片组</p>
        </div>
      </div>
    </div>

    <!-- 查询表单 -->
    <v-card flat class="border rounded-lg pa-6 mb-6">
      <h4 class="text-h6 font-weight-bold text-grey-darken-3 mb-4">
        <v-icon icon="mdi-filter-variant" size="20" class="mr-2"></v-icon>
        查询条件
      </h4>
      
      <v-row>
        <v-col cols="12" md="3">
          <v-text-field
            v-model.number="searchForm.nodeId"
            label="节点ID"
            type="number"
            variant="outlined"
            density="compact"
            clearable
            prepend-inner-icon="mdi-sitemap"
          ></v-text-field>
        </v-col>
        
        <v-col cols="12" md="3">
          <v-text-field
            v-model.number="searchForm.creatorId"
            label="用户ID"
            type="number"
            variant="outlined"
            density="compact"
            clearable
            prepend-inner-icon="mdi-account"
          ></v-text-field>
        </v-col>
        
        <v-col cols="12" md="3">
          <v-select
            v-model="searchForm.state"
            label="状态"
            :items="[
              { title: '全部', value: null },
              { title: '待审核', value: DeckState.PENDING },
              { title: '正常', value: DeckState.NORMAL },
              { title: '已屏蔽', value: DeckState.BLOCKED },
              { title: '私有', value: DeckState.PRIVATE }
            ]"
            variant="outlined"
            density="compact"
            clearable
            prepend-inner-icon="mdi-state-machine"
          ></v-select>
        </v-col>
        
        <v-col cols="12" md="3">
          <div class="d-flex gap-2">
            <v-btn
              color="primary"
              variant="flat"
              rounded="lg"
              prepend-icon="mdi-magnify"
              :disabled="!canSearch"
              @click="handleSearch"
            >
              查询
            </v-btn>
            <v-btn
              color="grey"
              variant="outlined"
              rounded="lg"
              prepend-icon="mdi-refresh"
              @click="resetSearch"
            >
              重置
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </v-card>

    <!-- 查询结果 -->
    <div v-if="deckList.length > 0 || loading">
      <div class="d-flex align-center justify-space-between mb-4">
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">
          查询结果
        </h4>
        <v-chip variant="flat" color="blue-lighten-4" rounded="lg">
          <v-icon icon="mdi-cards-outline" color="blue-darken-2" size="16" class="mr-1"></v-icon>
          <span class="text-blue-darken-2 text-caption">{{ deckList.length }} 个卡片组</span>
        </v-chip>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading && deckList.length === 0" class="text-center py-12">
        <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
        <p class="text-body-1 text-grey-darken-1 mt-4">正在查询...</p>
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
                <!-- 状态区域 -->
                <div class="mr-6">
                  <v-chip
                    variant="flat"
                    :color="getStateColor(deck.state)"
                    rounded="lg"
                    size="small"
                  >
                    <span class="text-caption font-weight-medium" :class="{
                      'text-orange-darken-2': deck.state === DeckState.PENDING,
                      'text-green-darken-2': deck.state === DeckState.NORMAL,
                      'text-red-darken-2': deck.state === DeckState.BLOCKED,
                      'text-blue-darken-2': deck.state === DeckState.PRIVATE
                    }">
                      {{ getStateText(deck.state) }}
                    </span>
                  </v-chip>
                </div>

                <!-- 卡片组内容区域 -->
                <div class="flex-grow-1">
                  <div class="d-flex align-center justify-space-between mb-2">
                    <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                      {{ deck.title }}
                    </h4>
                    <div class="d-flex align-center gap-4">
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-cards-outline" size="16" color="grey-darken-2" class="mr-1"></v-icon>
                        <span class="text-body-2 text-grey-darken-2">{{ deck.cardCount }} 张卡片</span>
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-thumb-up-outline" size="16" color="grey-darken-2" class="mr-1"></v-icon>
                        <span class="text-body-2 text-grey-darken-2">{{ deck.upvoteCount }} 点赞</span>
                      </div>
                    </div>
                  </div>

                  <div v-if="deck.description" class="mb-3">
                    <p class="text-body-2 text-grey-darken-1">{{ deck.description }}</p>
                  </div>

                  <!-- 卡片预览区域 -->
                  <div class="cards-preview-area mb-3">
                    <div class="preview-header">
                      <v-icon icon="mdi-cards-outline" size="18" color="primary" class="mr-2"></v-icon>
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
                      <span v-if="deck.creator?.id" class="text-caption text-grey-darken-1 ml-2">
                        (ID: {{ deck.creator.id }})
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

    <!-- 空状态 -->
    <div v-else-if="!loading" class="text-center py-12">
      <v-icon icon="mdi-magnify" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">请设置查询条件并点击查询按钮</p>
    </div>
  </div>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

/* 卡片预览区域样式复用 */
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