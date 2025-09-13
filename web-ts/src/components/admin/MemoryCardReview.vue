<script setup lang="ts">
import { inject, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MemoryService } from '@/services/memoryService'
import { DeckState } from '@/types/memoryCard'
import type { MemoryCardDeck } from '@/types/memoryCard'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

const activeTab = ref<'pending' | 'reviewed'>('pending')
const deckList = ref<MemoryCardDeck[]>([])
const loading = ref(false)
const lastId = ref<number | undefined>(undefined)
const hasMore = ref(true)

const loadDecks = async (reset = false): Promise<void> => {
  if (loading.value) return
  
  loading.value = true
  try {
    let states: number[]
    if (activeTab.value === 'pending') {
      states = [DeckState.PENDING]
    } else {
      states = [DeckState.NORMAL, DeckState.LOCKED, DeckState.DELETED]
    }
    
    // 由于后端接口只支持单个state参数，我们需要分别查询
    let allDecks: MemoryCardDeck[] = []
    let combinedHasMore = false
    
    for (const state of states) {
      const response = await MemoryService.getDecks({
        state: state,
        lastId: reset ? undefined : lastId.value,
        limit: 20,
        sortBy: 'createdAt',
        sortOrder: 'desc'
      })

      if (response.code === 200) {
        const responseData = response.data
        const newDecks = responseData.items || []
        allDecks = [...allDecks, ...newDecks]
        
        if (responseData.hasMore) {
          combinedHasMore = true
        }
        
        // 更新lastId - 使用 nextCursor 中的信息
        if (responseData.nextCursor?.lastId) {
          lastId.value = responseData.nextCursor.lastId
        }
      }
    }
    
    // 按创建时间排序
    allDecks.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    
    if (reset) {
      deckList.value = allDecks
      lastId.value = undefined
    } else {
      deckList.value = [...deckList.value, ...allDecks]
    }
    
    hasMore.value = combinedHasMore
    
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

const switchTab = (tab: 'pending' | 'reviewed') => {
  activeTab.value = tab
  deckList.value = []
  lastId.value = undefined
  hasMore.value = true
  loadDecks(true)
}

const approveDeck = async (deck: MemoryCardDeck, approve: boolean): Promise<void> => {
  try {
    // TODO: 需要后端提供审核API
    // const response = await MemoryService.approveDeck(deck.id, approve)
    
    // 临时模拟审核操作
    const newState = approve ? DeckState.NORMAL : DeckState.DELETED
    deck.state = newState
    
    // 从列表中移除已审核的项目
    const index = deckList.value.findIndex(d => d.id === deck.id)
    if (index > -1) {
      deckList.value.splice(index, 1)
    }
    
    showSnackbar?.(approve ? '卡片组审核通过' : '卡片组已拒绝', 'success')
    console.log(`Deck ${deck.id} ${approve ? 'approved' : 'rejected'}`)
  } catch (error) {
    console.error('Error approving deck:', error)
    showSnackbar?.('审核操作失败', 'error')
  }
}

const getStateText = (state: DeckState): string => {
  switch (state) {
    case DeckState.PENDING: return '待审核'
    case DeckState.NORMAL: return '正常'
    case DeckState.LOCKED: return '锁定'
    case DeckState.PRIVATE: return '私有'
    case DeckState.DELETED: return '已删除'
    default: return '未知'
  }
}

const getStateColor = (state: DeckState): string => {
  switch (state) {
    case DeckState.PENDING: return 'orange-lighten-4'
    case DeckState.NORMAL: return 'green-lighten-4'
    case DeckState.LOCKED: return 'red-lighten-4'
    case DeckState.PRIVATE: return 'blue-lighten-4'
    case DeckState.DELETED: return 'grey-lighten-3'
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
          <v-card flat class="border rounded-lg pa-5" hover>
            <div class="d-flex align-start">
              <!-- 状态和操作区域 -->
              <div class="mr-4 action-area">
                <div class="mb-3">
                  <v-chip
                    v-if="deck.state == DeckState.PENDING"
                    variant="flat"
                    :color="getStateColor(deck.state)"
                    rounded="lg"
                    size="small"
                  >
                    <span class="text-orange-darken-2 text-caption font-weight-medium">
                      {{ getStateText(deck.state) }}
                    </span>
                  </v-chip>
                </div>

                <div class="d-flex flex-column" style="gap: 8px;">
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
                    拒绝
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
  min-width: 120px;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}
</style>