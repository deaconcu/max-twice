<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
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

const decks = ref<MemoryCardDeck[]>([])
const loading = ref(false)
const sortBy = ref<'score' | 'createdAt' | 'upvoteCount'>('score')
const showAuthorOnly = ref(false)
const page = ref(1)
const hasMore = ref(true)

const sortedDecks = computed(() => {
  // 确保 decks.value 是一个数组
  if (!decks.value || !Array.isArray(decks.value)) {
    return []
  }
  
  let filtered = decks.value

  if (showAuthorOnly.value && props.post.creator) {
    filtered = filtered.filter(deck => deck.creator?.id === props.post.creator!.id)
  }

  return filtered.sort((a, b) => {
    switch (sortBy.value) {
      case 'score':
        // 使用 upvoteCount 作为 score 的替代
        return (b.upvoteCount || 0) - (a.upvoteCount || 0)
      case 'upvoteCount':
        return (b.upvoteCount || 0) - (a.upvoteCount || 0)
      case 'createdAt':
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      default:
        return 0
    }
  })
})

const loadDecks = async (reset = false) => {
  if (loading.value) return
  
  loading.value = true
  
  try {
    const response = await MemoryService.getDecks({
      postId: props.post.id,
      sortBy: sortBy.value,
      sortOrder: 'desc',
      limit: 20,
      lastScore: reset || !decks.value?.length ? undefined : decks.value[decks.value.length - 1]?.upvoteCount,
      lastId: reset || !decks.value?.length ? undefined : decks.value[decks.value.length - 1]?.id
    })
    
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
}

const addDeckToStudy = (deck: MemoryCardDeck) => {
  console.log('Adding deck to study:', deck)
  emit('addDeck', deck)
}

const viewDeckDetail = (deck: MemoryCardDeck) => {
  console.log('Viewing deck detail:', deck)
  emit('viewDeck', deck)
}

onMounted(() => {
  loadDecks(true)
})

// 暴露方法给父组件
defineExpose({
  loadDecks
})
</script>

<template>
  <div class="memory-card-sidebar h-100 d-flex flex-column">
    <!-- 模块头部 -->
    <div class="sidebar-header pa-4 bg-grey-lighten-5" style="position: sticky; top: 0; z-index: 2;">
      <div class="d-flex align-center">
        <v-icon icon="mdi-cards-outline" color="primary" size="20" class="mr-2"></v-icon>
        <h3 class="text-body-1 font-weight-bold text-grey-darken-2">记忆卡片组</h3>
      </div>
    </div>

    <!-- 排序和筛选控件 -->
    <div class="px-4 py-3 bg-grey-lighten-5">
      <div class="d-flex align-center justify-space-between mb-2">
        <v-btn-toggle
          v-model="sortBy"
          density="compact"
          variant="outlined"
          divided
          @update:model-value="handleSort"
        >
          <v-btn value="score" size="small">
            <v-icon icon="mdi-trending-up" size="14" class="mr-1"></v-icon>
            热度
          </v-btn>
          <v-btn value="createdAt" size="small">
            <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
            时间
          </v-btn>
        </v-btn-toggle>

        <div>
        <v-btn
          :color="showAuthorOnly ? 'primary' : 'grey-darken-1'"
          :variant="showAuthorOnly ? 'tonal' : 'text'"
          size="small"
          rounded="lg"
          class="mr-3"
          @click="handleFilterToggle"
        ><v-icon v-if="showAuthorOnly" icon="mdi-check" size="14" class="mr-1"></v-icon>
        只看作者
        </v-btn>
        <v-btn
          color="primary"
          icon="mdi-plus"
          variant="flat"
          density="compact"
          rounded="xl"
          @click="emit('createDeck')"
        ></v-btn>
        </div>
      </div>
    </div>

    <!-- 卡片组列表 -->
    <div style="height: 500px; overflow-y: auto;">
      <!-- 空状态 -->
      <div v-if="sortedDecks.length === 0 && !loading" class="text-center pa-6">
        <v-icon icon="mdi-cards-outline" size="48" color="grey-lighten-2" class="mb-3"></v-icon>
        <h4 class="text-h6 font-weight-medium text-grey-darken-2 mb-2">
          暂无卡片组
        </h4>
        <p class="text-body-2 text-grey-darken-1 mb-4">
          成为第一个贡献者！
        </p>
        <v-btn
          color="primary"
          variant="outlined"
          rounded="lg"
          @click="emit('createDeck')"
        >
          立即为本文创建卡片组
        </v-btn>
      </div>

      <!-- 卡片组列表 -->
      <div v-else>
        <v-card
          v-for="deck in sortedDecks"
          :key="deck.id"
          class="ma-3 mb-2"
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
            </div>

            <p v-if="deck.description" class="text-body-2 text-grey-darken-1 mb-3">
              {{ deck.description }}
            </p>

            <div class="d-flex align-center justify-space-between mb-3">
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
                <v-icon icon="mdi-thumb-up-outline" size="14" color="grey-darken-2" class="mr-1"></v-icon>
                <span class="text-body-2 text-grey-darken-2 mr-3">{{ deck.upvoteCount }}</span>
                <v-icon icon="mdi-cards-outline" size="14" color="grey-darken-2" class="mr-1"></v-icon>
                <span class="text-body-2 text-grey-darken-2">{{ deck.cardCount }}</span>
              </div>
            </div>

            <div class="d-flex" style="gap: 8px;">
              <v-btn
                color="primary"
                variant="outlined"
                rounded="lg"
                size="small"
                class="flex-grow-1"
                prepend-icon="mdi-eye"
                @click.stop="viewDeckDetail(deck)"
              >
                查看详情
              </v-btn>
              
              <v-btn
                color="success"
                variant="flat"
                rounded="lg"
                size="small"
                class="flex-grow-1"
                prepend-icon="mdi-plus"
                @click.stop="addDeckToStudy(deck)"
              >
                添加学习
              </v-btn>
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
</style>