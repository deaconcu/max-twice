<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Post } from '@/types/post'
import type { MemoryCardDeck, DeckDetail, GetDecksQuery } from '@/types/memoryCard'
import { DeckState } from '@/types/memoryCard'
import { MemoryCardMockService } from '@/services/memoryCardMockService'

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
  let filtered = decks.value

  if (showAuthorOnly.value && props.post.creator) {
    filtered = filtered.filter(deck => deck.creatorId === props.post.creator!.id)
  }

  return filtered.sort((a, b) => {
    switch (sortBy.value) {
      case 'score':
        return (b.score || 0) - (a.score || 0)
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
    // TODO: 调用真实API
    // const response = await memoryCardService.getDecks({
    //   postId: props.post.id,
    //   sortBy: sortBy.value,
    //   page: reset ? 1 : page.value,
    //   size: 20
    // })
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 模拟数据
    const mockDecks: MemoryCardDeck[] = [
      {
        id: 1,
        sourcePostId: props.post.id,
        creatorId: 1,
        creator: { id: 1, name: 'AI助手', email: 'ai@example.com' },
        title: '核心概念记忆卡',
        description: 'AI生成的核心概念记忆卡片组，包含本文的重要知识点',
        version: 1,
        state: DeckState.NORMAL,
        upvoteCount: 15,
        cardCount: 8,
        score: 85.5,
        createdAt: '2024-01-01T10:00:00Z',
        updatedAt: '2024-01-01T10:00:00Z'
      },
      {
        id: 2,
        sourcePostId: props.post.id,
        creatorId: 2,
        creator: { id: 2, name: '学习者小王', email: 'wang@example.com' },
        title: '实践练习卡片',
        description: '根据文章内容制作的实践练习题，适合巩固理解',
        version: 1,
        state: DeckState.NORMAL,
        upvoteCount: 8,
        cardCount: 12,
        score: 78.2,
        createdAt: '2024-01-02T14:30:00Z',
        updatedAt: '2024-01-02T14:30:00Z'
      }
    ]
    
    if (reset) {
      decks.value = mockDecks
      page.value = 1
    } else {
      decks.value.push(...mockDecks)
    }
    
    hasMore.value = mockDecks.length === 20
    if (!reset) page.value++
    
  } catch (error) {
    console.error('Failed to load decks:', error)
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
</script>

<template>
  <div class="memory-card-sidebar h-100 d-flex flex-column">
    <!-- 固定头部 -->
    <div class="sidebar-header pa-4 bg-white" style="position: sticky; top: 0; z-index: 2;">
      <div class="d-flex align-center mb-3">
        <v-avatar size="32" class="mr-3">
          <v-img v-if="post.creator?.avatar" :src="post.creator.avatar" />
          <v-icon v-else icon="mdi-account-circle" color="grey"></v-icon>
        </v-avatar>
        <div class="flex-grow-1">
          <h3 class="text-h6 font-weight-bold text-grey-darken-3 mb-1">
            {{ post.creator?.name || '匿名用户' }}
          </h3>
          <!--
          <p class="text-body-2 text-grey-darken-1 mb-0 text-truncate">
            {{ post.content?.substring(0, 30) || '文章标题' }}...
          </p>-->
        </div>
      </div>
      
      <v-btn
        color="primary"
        variant="flat"
        rounded="lg"
        block
        class="text-white font-weight-medium"
        prepend-icon="mdi-plus"
        @click="emit('createDeck')"
      >
        创建新卡片组
      </v-btn>
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
    <div class="flex-grow-1" style="overflow-y: auto;">
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