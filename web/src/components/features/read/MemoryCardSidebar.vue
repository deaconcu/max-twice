<template>
  <v-card class="memory-card-sidebar no-border" flat rounded="lg">
    <!-- 模块头部 -->
    <v-card-title class="pa-4">
      <div class="d-flex align-center justify-space-between w-100">
        <div class="d-flex align-center">
          <v-icon icon="mdi-cards-outline" color="primary" size="20" class="mr-2"></v-icon>
          <span class="text-h6 font-weight-bold">记忆卡片组</span>
        </div>
        <div class="d-flex align-center" style="gap: 4px">
          <v-btn
            color="success"
            variant="flat"
            rounded="lg"
            size="small"
            class="me-1"
            prepend-icon="mdi-plus"
            @click="emit('createDeck')"
          >
            创建
          </v-btn>

          <!-- 刷新按钮 -->
          <v-tooltip text="刷新" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                icon
                size="x-small"
                variant="text"
                color="grey-darken-2"
                :loading="loading"
                @click="loadDecks(true)"
              >
                <v-icon icon="mdi-refresh" size="16"></v-icon>
              </v-btn>
            </template>
          </v-tooltip>
        </div>
      </div>
    </v-card-title>

    <!-- 排序和筛选控件 -->
    <v-card-text class="px-4 pb-2 pt-0">
      <div class="d-flex align-center justify-space-between">
        <!-- 左侧：筛选选项 -->
        <div class="d-flex gap-2">
          <v-btn
            :color="!showAuthorOnly && !showMyOnly ? 'primary' : 'grey-darken-1'"
            :variant="!showAuthorOnly && !showMyOnly ? 'tonal' : 'text'"
            size="small"
            rounded="lg"
            class="px-2"
            @click="handleShowAll"
          >
            <v-icon
              v-if="!showAuthorOnly && !showMyOnly"
              icon="mdi-check"
              size="14"
              class="mr-1"
            ></v-icon>
            全部
          </v-btn>
          <v-btn
            :color="showAuthorOnly ? 'primary' : 'grey-darken-1'"
            :variant="showAuthorOnly ? 'tonal' : 'text'"
            size="small"
            rounded="lg"
            class="px-2"
            @click="handleFilterToggle"
          >
            <v-icon v-if="showAuthorOnly" icon="mdi-check" size="14" class="mr-1"></v-icon>
            只看作者
          </v-btn>
          <v-btn
            :color="showMyOnly ? 'primary' : 'grey-darken-1'"
            :variant="showMyOnly ? 'tonal' : 'text'"
            size="small"
            class="px-2"
            rounded="lg"
            @click="handleMyFilterToggle"
          >
            <v-icon v-if="showMyOnly" icon="mdi-check" size="14" class="mr-1"></v-icon>
            我提交的
          </v-btn>
        </div>

        <!-- 右侧：排序选项 -->
        <div class="d-flex gap-1">
          <v-tooltip text="热度排序" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                :color="sortBy === 'score' ? 'primary' : 'grey-darken-1'"
                :variant="sortBy === 'score' ? 'tonal' : 'text'"
                size="x-small"
                rounded="lg"
                icon="mdi-trending-up"
                @click="handleSort('score')"
              />
            </template>
          </v-tooltip>
          <v-tooltip text="时间排序" location="top">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                v-bind="tooltipProps"
                :color="sortBy === 'createdAt' ? 'primary' : 'grey-darken-1'"
                :variant="sortBy === 'createdAt' ? 'tonal' : 'text'"
                size="x-small"
                rounded="lg"
                icon="mdi-clock-outline"
                @click="handleSort('createdAt')"
              />
            </template>
          </v-tooltip>
        </div>
      </div>
    </v-card-text>

    <!-- 卡片组列表 -->
    <v-card-text class="px-4 pb-4 pt-2" style="max-height: 500px; overflow-y: auto">
      <!-- 空状态 -->
      <div v-if="decks.length === 0 && !loading" class="text-center pa-6">
        <v-icon icon="mdi-cards-outline" size="48" color="grey-lighten-2" class="mb-3"></v-icon>
        <h4 class="text-h6 font-weight-medium text-grey-darken-2 mb-2">暂无卡片组</h4>
        <p class="text-body-2 text-grey-darken-1 mb-4">成为第一个贡献者！</p>
        <v-btn color="primary" variant="tonal" rounded="lg" @click="emit('createDeck')">
          立即为本文创建卡片组
        </v-btn>
      </div>

      <!-- 卡片组列表 -->
      <div v-else>
        <v-card v-for="deck in decks" :key="deck.id" class="mb-3" elevation="0" rounded="lg" border>
          <v-card-text class="pa-4">
            <div class="d-flex align-start justify-space-between mb-2">
              <h4
                class="text-subtitle-1 font-weight-bold text-grey-darken-3 flex-grow-1 clickable-title"
                @click="viewDeckDetail(deck)"
              >
                {{ deck.title }}
              </h4>
              <!-- 状态标签 (仅在"我提交的"筛选下显示) -->
              <v-chip
                v-if="showMyOnly && deck.creatorId === currentUserId"
                size="x-small"
                :color="getStateColor(deck.state)"
                variant="flat"
                class="ml-2"
              >
                {{ getStateText(deck.state) }}
              </v-chip>
            </div>

            <p v-if="deck.description" class="text-body-2 text-grey-darken-1 mb-3">
              {{ deck.description }}
            </p>

            <div class="d-flex align-center justify-space-between">
              <div class="d-flex align-center">
                <v-avatar size="24" class="mr-2">
                  <v-img v-if="deck.creatorAvatar" :src="deck.creatorAvatar" />
                  <v-icon v-else icon="mdi-account-circle" size="16" color="grey"></v-icon>
                </v-avatar>
                <span class="text-body-2 text-grey-darken-2">
                  {{ deck.creatorName || '匿名用户' }}
                </span>
              </div>
              <div class="d-flex align-center">
                <v-btn
                  :color="deck.hasUpvoted ? 'grey-darken-2' : 'grey-darken-2'"
                  variant="text"
                  size="small"
                  rounded="lg"
                  @click="handleUpvote(deck, $event)"
                >
                  <v-icon
                    :icon="deck.hasUpvoted ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
                    :color="deck.hasUpvoted ? 'red' : 'grey-darken-2'"
                    size="14"
                    class="mr-2"
                  ></v-icon>
                  {{ deck.upvoteCount }}
                </v-btn>
                <v-icon
                  icon="mdi-cards-outline"
                  size="14"
                  color="grey-darken-2"
                  class="ml-3 mr-2"
                ></v-icon>
                <span class="text-body-2 text-grey-darken-2">{{ deck.cardCount }}</span>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore && !loading" class="text-center pa-4">
        <v-btn variant="text" color="primary" @click="loadMore">加载更多</v-btn>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="text-center pa-4">
        <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { memoryApi } from '@/api'
import { useMutation } from '@/composables'
import { useUserStore } from '@/stores'
import type { MemoryCardDeck } from '@/types/memory'

interface Props {
  postId: number
}

interface Emits {
  (e: 'createDeck'): void
  (e: 'addDeck', deck: MemoryCardDeck): void
  (e: 'viewDeck', deck: MemoryCardDeck): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const userStore = useUserStore()

const sortBy = ref<'score' | 'createdAt' | 'upvoteCount'>('score')
const showAuthorOnly = ref(false)
const showMyOnly = ref(false)
const decks = ref<MemoryCardDeck[]>([])
const loading = ref(false)
const hasMore = ref(true)
const lastId = ref<number>(0)
const lastScore = ref<number>(0)

const currentUserId = computed(() => userStore.user?.id)

// 加载卡片组列表
const loadDecks = async (reset = false) => {
  if (reset) {
    decks.value = []
    lastId.value = 0
    lastScore.value = 0
    hasMore.value = true
  }

  if (loading.value || !hasMore.value) return

  try {
    loading.value = true

    const queryParams: any = {
      sortBy: sortBy.value,
      sortOrder: 'desc' as const,
      limit: 20,
    }

    // 只有在非第一页时才添加 lastScore 和 lastId
    if (lastId.value > 0) {
      queryParams.lastScore = lastScore.value
      queryParams.lastId = lastId.value
    }

    let response
    if (showAuthorOnly.value) {
      response = await memoryApi.getPostCreatorDeck(props.postId, queryParams)
    } else if (showMyOnly.value) {
      response = await memoryApi.getMyPostDeck(props.postId, queryParams)
    } else {
      response = await memoryApi.getPostPublicDecks(props.postId, queryParams)
    }

    if (response.data?.items) {
      const items = response.data.items
      decks.value = reset ? items : [...decks.value, ...items]

      if (items.length > 0) {
        const lastItem = items[items.length - 1]
        lastId.value = lastItem.id
        lastScore.value = lastItem.upvoteCount || 0
      }

      hasMore.value = items.length >= 20
    } else {
      hasMore.value = false
    }
  } catch (error) {
    console.error('Failed to load memory decks:', error)
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  loadDecks(false)
}

// 监听筛选条件变化，重新加载数据
watch([sortBy, showAuthorOnly, showMyOnly], () => {
  loadDecks(true)
})

// 使用 useMutation 处理点赞
const { execute: upvoteDeck } = useMutation((deckId: number) => memoryApi.upvoteDeck(deckId), {
  showToast: false,
  onSuccess: (result, deckId) => {
    const deck = decks.value.find((d) => d.id === deckId)
    if (deck && result) {
      deck.hasUpvoted = result.upvoted
      deck.upvoteCount = result.upvotes
    }
  },
})

const handleSort = (newSortBy: typeof sortBy.value) => {
  sortBy.value = newSortBy
}

const handleFilterToggle = () => {
  showAuthorOnly.value = !showAuthorOnly.value
  if (showAuthorOnly.value) {
    showMyOnly.value = false
  }
}

const handleMyFilterToggle = () => {
  showMyOnly.value = !showMyOnly.value
  if (showMyOnly.value) {
    showAuthorOnly.value = false
  }
}

const handleShowAll = () => {
  showAuthorOnly.value = false
  showMyOnly.value = false
}

const viewDeckDetail = (deck: MemoryCardDeck) => {
  emit('viewDeck', deck)
}

// 处理点赞
const handleUpvote = async (deck: MemoryCardDeck, event: Event) => {
  event.stopPropagation()
  await upvoteDeck(deck.id)
}

// 切换到"我提交的"标签
const switchToMyDecks = () => {
  handleMyFilterToggle()
}

// 获取状态显示文本
const getStateText = (state: number) => {
  switch (state) {
    case 1:
      return '审核中'
    case 2:
      return '已通过'
    case 3:
      return '已拒绝'
    case 4:
      return '已屏蔽'
    default:
      return '未知'
  }
}

// 获取状态颜色
const getStateColor = (state: number) => {
  switch (state) {
    case 1:
      return 'warning'
    case 2:
      return 'success'
    case 3:
      return 'grey'
    case 4:
      return 'error'
    default:
      return 'grey'
  }
}

// 暴露方法给父组件
defineExpose({
  loadDecks,
  switchToMyDecks,
})

// 初始加载
loadDecks(true)
</script>

<style scoped>
.memory-card-sidebar {
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-border));
}

.clickable-title {
  cursor: pointer;
  transition: color 0.2s ease;
}

.clickable-title:hover {
  color: rgb(var(--v-theme-primary));
  text-decoration: underline;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(var(--v-theme-on-surface), 0.2);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(var(--v-theme-on-surface), 0.3);
}
</style>
