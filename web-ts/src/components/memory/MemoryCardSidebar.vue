<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import type { Post } from '@/types/post'
import type { MemoryCardDeck, DeckDetail, GetDecksQuery } from '@/types/memoryCard'
import { DeckState } from '@/types/memoryCard'
import { MemoryService } from '@/services/memoryService'

interface Props {
  post: Post
}

interface Emits {
  (e: 'createDeck'): void
  (e: 'addDeck', deck: MemoryCardDeck): void
  (e: 'viewDeck', deck: MemoryCardDeck): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()
const userStore = useUserStore()

const decks = ref<MemoryCardDeck[]>([])
const loading = ref(false)
const sortBy = ref<'score' | 'createdAt' | 'upvoteCount'>('score')
const showAuthorOnly = ref(false)
const showMyOnly = ref(false)
const page = ref(1)
const hasMore = ref(true)

const loadDecks = async (reset = false) => {
  if (loading.value) return

  loading.value = true

  try {
    // 构建查询参数
    const queryParams = {
      sortBy: sortBy.value,
      sortOrder: 'desc',
      limit: 20,
      lastScore: reset || !decks.value?.length ? undefined : decks.value[decks.value.length - 1]?.upvoteCount,
      lastId: reset || !decks.value?.length ? undefined : decks.value[decks.value.length - 1]?.id
    }

    // 根据选中的标签调用不同的接口
    let response
    if (showAuthorOnly.value) {
      // 需求2: 获取帖子创建者提交的卡片组
      response = await MemoryService.getPostCreatorDeck(props.post.id, queryParams)
    } else if (showMyOnly.value) {
      // 需求3: 获取用户自己在指定帖子下提交的卡片组
      response = await MemoryService.getMyPostDeck(props.post.id, queryParams)
    } else {
      // 需求1: 获取帖子下的公共卡片组列表
      response = await MemoryService.getPostPublicDecks(props.post.id, queryParams)
    }
    
    if (response.code === 200) {
      const responseData = response.data
      
      if (reset || !decks.value) {
        decks.value = responseData.items || []
        page.value = 1
      } else {
        decks.value.push(...(responseData.items || []))
      }
      
      hasMore.value = responseData.hasMore || false
      if (!reset) page.value++
    } else {
      // API 返回错误状态码时，确保 decks.value 是数组
      if (reset || !decks.value) {
        decks.value = []
      }
    }
    
  } catch (error) {
    console.error('Failed to load decks:', error)
    // 发生错误时，确保 decks.value 是数组
    if (reset || !decks.value) {
      decks.value = []
    }
  } finally {
    loading.value = false
  }
}

const handleSort = (newSortBy: typeof sortBy.value) => {
  sortBy.value = newSortBy
  loadDecks(true)
}

const handleFilterToggle = () => {
  showAuthorOnly.value = !showAuthorOnly.value
  if (showAuthorOnly.value) {
    showMyOnly.value = false
  }
  loadDecks(true)
}

const handleMyFilterToggle = () => {
  showMyOnly.value = !showMyOnly.value
  if (showMyOnly.value) {
    showAuthorOnly.value = false
  }
  loadDecks(true)
}

const handleShowAll = () => {
  showAuthorOnly.value = false
  showMyOnly.value = false
  loadDecks(true)
}

const addDeckToStudy = (deck: MemoryCardDeck) => {
  console.log('Adding deck to study:', deck)
  emit('addDeck', deck)
}

const viewDeckDetail = (deck: MemoryCardDeck) => {
  console.log('Viewing deck detail:', deck)
  emit('viewDeck', deck)
}

// 处理点赞
const handleUpvote = async (deck: MemoryCardDeck, event: Event) => {
  event.stopPropagation() // 阻止事件冒泡
  try {
    const response = await MemoryService.upvoteDeck(deck.id)
    if (response.code === 200) {
      // 更新本地状态 - 适配后端返回的字段名
      deck.hasUpvoted = response.data.upvoted
      deck.upvoteCount = response.data.upvotes
    }
  } catch (error) {
    console.error('Failed to upvote deck:', error)
  }
}

// 切换到"我提交的"标签
const switchToMyDecks = () => {
  handleMyFilterToggle()
}

// 获取状态显示文本
const getStateText = (state: number) => {
  switch (state) {
    case DeckState.PENDING: return '审核中'
    case DeckState.NORMAL: return '已通过'
    case DeckState.BLOCKED: return '已屏蔽'
    case DeckState.PRIVATE: return '私有'
    default: return '未知'
  }
}

// 获取状态颜色
const getStateColor = (state: number) => {
  switch (state) {
    case DeckState.PENDING: return 'warning'
    case DeckState.NORMAL: return 'success'
    case DeckState.BLOCKED: return 'error'
    case DeckState.PRIVATE: return 'grey'
    default: return 'grey'
  }
}

onMounted(() => {
  loadDecks(true)
})

// 暴露方法和变量给父组件
defineExpose({
  loadDecks,
  switchToMyDecks
})
</script>

<template>
  <div class="memory-card-sidebar h-100 d-flex flex-column">
    <!-- 模块头部 -->
    <div class="sidebar-header pa-3 bg-grey-lighten-5" style="position: sticky; top: 0; z-index: 2;">
      <div class="d-flex align-center justify-space-between">
        <div class="d-flex align-center">
          <v-icon icon="mdi-cards-outline" color="primary" size="20" class="mr-2"></v-icon>
          <h3 class="text-body-1 font-weight-bold text-grey-darken-2">记忆卡片组</h3>
        </div>
        <div class="d-flex align-center" style="gap: 4px;">
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
          <v-btn
            icon
            size="x-small"
            variant="text"
            color="grey-darken-2"
            @click="loadDecks(true)"
            :loading="loading"
          >
            <v-tooltip activator="parent" location="top">
              刷新
            </v-tooltip>
            <v-icon icon="mdi-refresh" size="16"></v-icon>
          </v-btn>
        </div>
      </div>
    </div>

    <!-- 排序和筛选控件 -->
    <div class="px-3 pb-2 bg-grey-lighten-5">
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
            <v-icon v-if="!showAuthorOnly && !showMyOnly" icon="mdi-check" size="14" class="mr-1"></v-icon>
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
          <v-btn
            :color="sortBy === 'score' ? 'primary' : 'grey-darken-1'"
            :variant="sortBy === 'score' ? 'tonal' : 'text'"
            size="x-small"
            class="mr-2"
            rounded="lg"
            icon="mdi-trending-up"
            @click="handleSort('score')"
          ></v-btn>
          <v-btn
            :color="sortBy === 'createdAt' ? 'primary' : 'grey-darken-1'"
            :variant="sortBy === 'createdAt' ? 'tonal' : 'text'"
            size="x-small"
            rounded="lg"
            icon="mdi-clock-outline"
            @click="handleSort('createdAt')"
          ></v-btn>
        </div>
      </div>
    </div>

    <!-- 卡片组列表 -->
    <div style="max-height: 500px; overflow-y: auto;">
      <!-- 空状态 -->
      <div v-if="decks.length === 0 && !loading" class="text-center pa-6">
        <v-icon icon="mdi-cards-outline" size="48" color="grey-lighten-2" class="mb-3"></v-icon>
        <h4 class="text-h6 font-weight-medium text-grey-darken-2 mb-2">
          暂无卡片组
        </h4>
        <p class="text-body-2 text-grey-darken-1 mb-4">
          成为第一个贡献者！
        </p>
        <v-btn
          color="primary"
          variant="tonal"
          rounded="lg"
          @click="emit('createDeck')"
        >
          立即为本文创建卡片组
        </v-btn>
      </div>

      <!-- 卡片组列表 -->
      <div v-else>
        <v-card
          v-for="deck in decks"
          :key="deck.id"
          class="ma-3"
          elevation="0"
          rounded="lg"
        >
          <v-card-text class="pa-4">
            <div class="d-flex align-start justify-space-between mb-2">
              <h4
                class="text-subtitle-1 font-weight-bold text-grey-darken-3 flex-grow-1 clickable-title"
                @click="viewDeckDetail(deck)"
              >
                {{ deck.title }}
              </h4>
              <!-- 状态标签 (仅当创建者是当前用户时显示) -->
              <v-chip
                v-if="deck.creator?.id === userStore.userId"
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
                  <v-img v-if="deck.creator?.avatar" :src="deck.creator.avatar" />
                  <v-icon v-else icon="mdi-account-circle" size="16" color="grey"></v-icon>
                </v-avatar>
                <span class="text-body-2 text-grey-darken-2">
                  {{ deck.creator?.name || '匿名用户' }}
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
                <v-icon icon="mdi-cards-outline" size="14" color="grey-darken-2" class="ml-3 mr-2"></v-icon>
                <span class="text-body-2 text-grey-darken-2">{{ deck.cardCount }}</span>
              </div>
            </div>

          </v-card-text>
        </v-card>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore && !loading" class="text-center pa-4">
        <v-btn
          variant="text"
          color="primary"
          @click="loadDecks(false)"
        >
          加载更多
        </v-btn>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="text-center pa-4">
        <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
      </div>
    </div>
  </div>
</template>

<style scoped>
.memory-card-sidebar {
  border: 0px solid #e0e0e0;
  background-color: #fafafa;
}

.sidebar-header {
  border-bottom: 0px solid #e0e0e0;
}

.clickable-title {
  cursor: pointer;
  transition: color 0.2s ease;
}

.clickable-title:hover {
  color: #1976d2;
  text-decoration: underline;
}

/* 滚动条样式 */
.memory-card-sidebar ::-webkit-scrollbar {
  width: 6px;
}

.memory-card-sidebar ::-webkit-scrollbar-track {
  background: #fafafa;
}

.memory-card-sidebar ::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.memory-card-sidebar ::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}
</style>