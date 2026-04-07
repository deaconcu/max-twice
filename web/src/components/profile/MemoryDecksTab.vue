<template>
  <div class="pa-0 pa-sm-1">
    <!-- 加载状态 -->
    <LoadingSpinner v-if="loading" />

    <!-- 卡片组列表 -->
    <div v-else-if="decks.length > 0">
      <div class="deck-grid">
        <v-card
          v-for="deck in decks"
          :key="deck.id"
          rounded="lg"
          border
          hover
          class="deck-card"
          @click="openDeckDetail(deck)"
        >
          <v-card-text class="pa-4 position-relative">
            <!-- 删除按钮 -->
            <v-btn
              v-if="isOwnProfile"
              color="grey"
              variant="text"
              size="x-small"
              icon="mdi-close"
              class="close-btn"
              @click.stop="deleteDeck(deck.id)"
            />

            <!-- 图标和标题区域 -->
            <div class="d-flex align-center ga-3 mb-3">
              <div class="icon-container flex-shrink-0">
                <v-icon icon="mdi-cards" :size="24" :color="deck.color" />
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div
                  class="text-body-1 font-weight-bold text-truncate cursor-pointer"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                  @click.stop="deck.courseId && goToCourse(deck.courseId)"
                >
                  {{ deck.courseName }}
                </div>
                <div
                  class="text-body-2 text-medium-emphasis text-truncate cursor-pointer"
                  @click.stop="deck.nodeId && goToNode(deck.nodeId)"
                >
                  {{ deck.nodeName }}
                </div>
              </div>
            </div>

            <!-- 卡片数和原文 -->
            <div class="d-flex align-center justify-space-between mb-3">
              <span class="text-body-2 text-medium-emphasis">
                {{ deck.cardCount }} {{ t('review.cards') }}
              </span>
              <v-btn
                v-if="deck.postId"
                variant="text"
                color="primary"
                size="small"
                class="px-1"
                @click.stop="goToPost(deck.postId)"
              >
                {{ t('admin.viewOriginal') }}
              </v-btn>
            </div>

            <!-- 底部：时间和状态 -->
            <div class="d-flex align-center justify-space-between">
              <div class="text-body-2 text-grey d-flex align-center">
                <v-icon icon="mdi-clock-outline" size="16" class="mr-1" />
                {{ formatDate(deck.createdAt) }}
              </div>
              <v-chip size="small" color="success" variant="tonal">
                {{ t('review.readyToReview') }}
              </v-chip>
            </div>
          </v-card-text>
        </v-card>
      </div>
    </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-8 py-md-12">
          <v-icon
            icon="mdi-cards"
            :size="$vuetify.display.mobile ? 48 : 64"
            color="grey-lighten-2"
            class="mb-3 mb-md-4"
          />
          <p class="text-body-2 text-md-body-1 text-grey-darken-2">{{ t('review.noCards') }}</p>
          <p class="text-caption text-md-body-2 text-grey">{{ t('review.description') }}</p>
          <v-btn
            color="primary"
            variant="outlined"
            rounded="md"
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            class="mt-3 mt-md-4"
            to="/memory-review"
          >
            <v-icon icon="mdi-brain" :size="$vuetify.display.mobile ? 16 : 18" class="mr-2" />
            {{ t('review.title') }}
          </v-btn>
        </div>

        <!-- 删除确认对话框 -->
        <ConfirmDialog
          v-model="showDeleteDialog"
          :title="t('common.confirm')"
          :message="t('common.delete') + '?'"
          :confirm-text="t('common.confirm')"
          @confirm="confirmDelete"
        />

    <DeckDetailDialog v-model="showDeckDetail" :deck="selectedDeck" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { useI18n } from '@/composables/useI18n'
import { useUserStore } from '@/stores/modules/user'
import { memoryApi } from '@/api'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import DeckDetailDialog from '@/components/features/read/DeckDetailDialog.vue'

const { t, locale } = useI18n()

interface Props {
  userId?: number | null
  isOwnProfile?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  userId: null,
  isOwnProfile: false,
})

const router = useRouter()
const userStore = useUserStore()

// 获取用户创建的卡片组列表
const {
  data: userDecksData,
  loading,
  execute: fetchUserDecks,
} = useFetch({
  fetchFn: () => {
    if (props.isOwnProfile) {
      // 获取当前用户自己的卡片组
      return memoryApi.getCurrentUserDecks({ limit: 20 })
    } else if (props.userId) {
      // 获取指定用户的卡片组
      return memoryApi.getUserDecks(props.userId, { limit: 20 })
    } else {
      return Promise.resolve({ data: { items: [], hasMore: false } })
    }
  },
  immediate: true,
  defaultValue: { items: [], hasMore: false },
})

// 删除卡片组
const { execute: removeDeck } = useMutation(
  (deckId: number) => memoryApi.deleteDeck(deckId),
  {
    successMessage: t('user.profile.deckDeleted'),
    onSuccess: () => {
      fetchUserDecks()
    },
  }
)

// 转换为卡片组格式
const decks = computed(() => {
  if (!userDecksData.value?.items) return []

  return userDecksData.value.items.map((deck) => ({
    id: deck.id, // 使用真实的deck ID
    // 主要显示信息：课程和节点为主角
    courseName: deck.course?.name || t('user.profile.unknownCourse'),
    nodeName: deck.node?.name || t('user.profile.unknownNode'),
    cardCount: deck.cardCount || 0,
    createdAt: deck.createdAt || null,
    courseId: deck.courseId,
    nodeId: deck.nodeId,
    postId: deck.postId || deck.sourcePostId, // 关联的帖子ID
    course: deck.course || null,
    node: deck.node || null,
    // 保留原始字段以备需要
    firstCardQuestion: deck.firstCardQuestion || '',
    description: deck.description || '',
    icon: 'mdi-cards',
    color: '#42b883',
  }))
})

// 删除对话框
const showDeleteDialog = ref(false)
const deckToDelete = ref<number | null>(null)

// 卡片组详情对话框
const showDeckDetail = ref(false)
const selectedDeck = ref<any>(null)

// 格式化日期
const formatDate = (date: string | null) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString(locale.value === 'zh' ? 'zh-CN' : 'en-US')
}

// 跳转到复习页面
const goToReview = (deckId: number) => {
  router.push(`/memory-review?deckId=${deckId}`)
}

// 打开卡片组详情
const openDeckDetail = (deck: any) => {
  // 转换数据格式以匹配 DeckDetailDialog 组件的需求
  selectedDeck.value = {
    id: deck.id,
    title: `${deck.courseName} - ${deck.nodeName}`, // 组合课程和节点名称作为标题
    description: deck.description || '', // 只显示原始描述，没有则为空
    cardCount: deck.cardCount,
    creator: {
      id: userStore.currentUser?.id || 0,
      name: userStore.currentUser?.name || t('user.profile.currentUser'),
      avatar: userStore.currentUser?.avatar || undefined,
    },
    upvoteCount: 0,
    hasUpvoted: false,
    nodeId: deck.nodeId, // 设置关联的节点ID
  }
  showDeckDetail.value = true
}

// 跳转到帖子详情
const goToPost = (postId: number | undefined) => {
  if (postId) {
    router.push({ path: '/read', query: { postId: postId } })
  }
}

// 跳转到课程详情
const goToCourse = (courseId: number | undefined) => {
  if (courseId) {
    router.push(`/courses/${courseId}`)
  }
}

// 跳转到节点详情
const goToNode = (nodeId: number | undefined) => {
  if (nodeId) {
    router.push({ path: '/read', query: { nodeId: nodeId } })
  }
}

// 删除卡片组
const deleteDeck = (deckId: number) => {
  deckToDelete.value = deckId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (deckToDelete.value !== null) {
    await removeDeck(deckToDelete.value)
  }
  deckToDelete.value = null
}
</script>

<style scoped>
.deck-card {
  cursor: pointer;
  transition: all 0.2s;
  background-color: rgb(var(--v-theme-surface));
}

.deck-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.close-btn {
  position: absolute;
  top: 8px;
  right: 8px;
}

.icon-container {
  width: 48px;
  height: 48px;
  border: 1px solid rgb(var(--v-theme-outline));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cursor-pointer {
  cursor: pointer;
  transition: opacity 0.15s;
}

.cursor-pointer:hover {
  opacity: 0.7;
}

/* 基于容器宽度的响应式网格 */
.deck-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@container (max-width: 1200px) {
  .deck-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@container (max-width: 750px) {
  .deck-grid {
    grid-template-columns: 1fr;
  }
}

/* 启用 container query */
.pa-0 {
  container-type: inline-size;
}
</style>
