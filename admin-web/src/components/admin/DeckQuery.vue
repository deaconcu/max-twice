<script setup lang="ts">
import { inject, ref, computed } from 'vue'
import { useI18n } from '@/composables/useI18n'
import { adminApi } from '@/api/modules/admin'
import { DeckState } from '@/types/memory'
import type { DeckDetail } from '@/types/memory'
import { useFetchForScroll } from '@/composables/useFetchForScroll'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 查询条件
const searchForm = ref({
  nodeId: null as number | null,
  creatorId: null as number | null,
  state: null as number | null,
  keyword: '',
})

// 使用 useFetchForScroll 管理分页数据
const {
  items: deckList,
  loading,
  hasMore,
  params,
  loadMore,
  reset: resetList,
} = useFetchForScroll<DeckDetail>({
  fetchFn: (currentParams) => {
    return adminApi.getDecksByFilter(
      searchForm.value.nodeId || undefined,
      searchForm.value.creatorId || undefined,
      searchForm.value.state !== null ? searchForm.value.state : undefined,
      currentParams.lastId ?? undefined
    )
  },
  initialParams: {
    lastId: null,
  },
  onError: (error) => {
    console.error('Error searching decks:', error)
    showSnackbar?.('查询失败', 'error')
  },
})

// 重置查询
const resetSearch = () => {
  searchForm.value = {
    nodeId: null,
    creatorId: null,
    state: null,
    keyword: '',
  }
  resetList()
}

// 搜索
const handleSearch = () => {
  resetList()
  loadMore()
}

// 状态文本
const getStateText = (state: number): string => {
  switch (state) {
    case DeckState.SUBMITTED:
      return '待审核'
    case DeckState.PUBLISHED:
      return '正常'
    case DeckState.REJECTED:
      return '已拒绝'
    case DeckState.BANNED:
      return '已封禁'
    default:
      return '未知'
  }
}

// 状态颜色
const getStateColor = (state: number): string => {
  switch (state) {
    case DeckState.SUBMITTED:
      return 'orange-lighten-4'
    case DeckState.PUBLISHED:
      return 'green-lighten-4'
    case DeckState.REJECTED:
      return 'yellow-lighten-4'
    case DeckState.BANNED:
      return 'red-lighten-4'
    default:
      return 'grey-lighten-3'
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
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">卡片组查询</h3>
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
              { title: '待审核', value: DeckState.SUBMITTED },
              { title: '正常', value: DeckState.PUBLISHED },
              { title: '已拒绝', value: DeckState.REJECTED },
              { title: '已封禁', value: DeckState.BANNED },
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
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">查询结果</h4>
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
        <v-infinite-scroll :loading="loading" mode="intersect" side="end" @load="loadMore">
          <div v-for="deck in deckList" :key="deck.id" class="mb-4">
            <v-card flat class="border rounded-lg pa-4" hover>
              <!-- 来源信息 -->
              <div class="d-flex align-center justify-space-between mb-2">
                <div class="d-flex align-center">
                  <v-chip variant="flat" :color="getStateColor(deck.state)" rounded="lg" size="small" class="mr-3">
                    <span class="text-caption font-weight-medium">{{ getStateText(deck.state) }}</span>
                  </v-chip>
                  <span class="text-body-1 font-weight-medium text-grey-darken-3">
                    <template v-if="deck.course || deck.node">
                      <span v-if="deck.course">{{ deck.course.name }}</span>
                      <v-icon v-if="deck.course && deck.node" icon="mdi-chevron-right" size="18" class="mx-1 text-grey"></v-icon>
                      <span v-if="deck.node">{{ deck.node.name }}</span>
                    </template>
                    <span v-else class="text-grey-darken-1">帖子 #{{ deck.postId }}</span>
                  </span>
                </div>
                <div class="d-flex align-center text-body-2 text-grey-darken-1">
                  <span>{{ deck.cardCount }} 张卡片</span>
                  <span class="mx-2">{{ deck.likeCount }} 点赞</span>
                </div>
              </div>

              <!-- 创建者 + 时间 -->
              <div class="d-flex align-center mb-3 text-body-2 text-grey-darken-1">
                <v-avatar size="20" class="mr-1">
                  <v-img v-if="deck.creator?.avatar" :src="deck.creator.avatar" />
                  <v-icon v-else icon="mdi-account-circle" size="20" color="grey"></v-icon>
                </v-avatar>
                <span>{{ deck.creator?.name || '匿名用户' }}</span>
                <span v-if="deck.creator?.id" class="ml-1">(ID: {{ deck.creator.id }})</span>
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
