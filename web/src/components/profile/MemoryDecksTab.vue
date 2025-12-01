<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 - 宽度不够时隐藏 -->
    <v-col cols="12" md="2" class="d-none d-lg-block">
      <div class="sticky-sidebar">
        <div class="pa-3 pa-md-4">
          <div class="mb-4">
            <h4 class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-4 mb-2">
              {{ isOwnProfile ? '我的卡片组' : 'TA的卡片组' }}
            </h4>
            <p class="text-caption text-md-body-2 text-grey mb-0">
              {{ isOwnProfile ? '使用间隔重复算法高效记忆知识点。' : '查看TA创建的记忆卡片组。' }}
            </p>
          </div>
          <template v-if="isOwnProfile">
            <v-divider class="my-3 my-md-4" />
            <div class="text-caption text-md-body-2 text-grey">
              <div class="d-flex align-start mb-2 mb-md-3">
                <v-icon icon="mdi-brain" size="16" color="grey" class="mr-2 mt-1" />
                <span>科学记忆方法</span>
              </div>
              <div class="d-flex align-start mb-2 mb-md-3">
                <v-icon icon="mdi-calendar-clock" size="16" color="grey" class="mr-2 mt-1" />
                <span>定期复习提醒</span>
              </div>
              <div class="d-flex align-start">
                <v-icon icon="mdi-chart-line" size="16" color="grey" class="mr-2 mt-1" />
                <span>进度统计分析</span>
              </div>
            </div>
          </template>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" lg="10">
      <div class="pa-0 pa-sm-2">
        <div class="d-flex align-center justify-space-between mb-4 mb-md-6">
          <div></div>
        </div>

        <!-- 加载状态 -->
        <LoadingSpinner v-if="loading" />

        <!-- 卡片组列表 -->
        <div v-else-if="decks.length > 0">
          <v-row>
            <v-col v-for="deck in decks" :key="deck.id" cols="12" md="6">
              <v-card
                rounded="xl"
                hover
                elevation="0"
                class="deck-card hoverable"
                @click="openDeckDetail(deck)"
              >
                <v-card-text class="pa-5">
                  <!-- 顶部：课程标题和删除按钮 -->
                  <div class="d-flex align-center justify-space-between mb-3">
                    <v-btn
                      v-if="deck.courseId"
                      variant="text"
                      class="text-h6 font-weight-bold text-primary pa-0 justify-start"
                      style="text-transform: none; min-height: auto; height: auto;"
                      @click.stop="goToCourse(deck.courseId)"
                    >
                      {{ deck.courseName }}
                    </v-btn>
                    <h3 v-else class="text-h6 font-weight-bold text-primary mb-0">
                      {{ deck.courseName }}
                    </h3>

                    <v-btn
                      v-if="isOwnProfile"
                      color="grey-lighten-1"
                      variant="text"
                      size="small"
                      icon="mdi-delete"
                      @click.stop="deleteDeck(deck.id)"
                    >
                      <v-icon size="20">mdi-delete</v-icon>
                      <v-tooltip activator="parent" location="top">删除卡片组</v-tooltip>
                    </v-btn>
                  </div>

                  <!-- 中间：节点 + 卡片数 + 原文按钮（同一行） -->
                  <div class="d-flex align-center justify-space-between mb-4">
                    <!-- 左侧：节点信息 -->
                    <v-btn
                      v-if="deck.nodeId"
                      variant="text"
                      class="text-body-1 text-grey-darken-2 pa-0 justify-start"
                      style="text-transform: none; min-height: auto; height: auto;"
                      @click.stop="goToNode(deck.nodeId)"
                    >
                      {{ deck.nodeName }}
                    </v-btn>
                    <div v-else class="text-body-1 text-grey-darken-2">
                      {{ deck.nodeName }}
                    </div>

                    <!-- 右侧：卡片数 + 原文按钮 -->
                    <div class="d-flex align-center ga-3">
                      <v-btn
                        variant="text"
                        color="primary"
                        size="small"
                        prepend-icon="mdi-cards"
                      >
                        {{ deck.cardCount }} 张
                      </v-btn>

                      <v-btn
                        v-if="deck.postId"
                        variant="text"
                        color="primary"
                        size="small"
                        @click.stop="goToPost(deck.postId)"
                      >
                        原文
                      </v-btn>
                    </div>
                  </div>

                  <!-- 底部：分隔线和时间状态 -->
                  <v-divider class="my-3" />
                  <div class="d-flex align-center justify-space-between">
                    <div class="text-caption text-grey d-flex align-center">
                      <v-icon icon="mdi-clock-outline" size="14" class="mr-1" />
                      创建于 {{ formatDate(deck.createdAt) }}
                    </div>
                    <v-chip
                      size="x-small"
                      color="success"
                      variant="flat"
                    >
                      就绪
                    </v-chip>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-8 py-md-12">
          <v-icon
            icon="mdi-cards"
            :size="$vuetify.display.mobile ? 48 : 64"
            color="grey-lighten-2"
            class="mb-3 mb-md-4"
          />
          <p class="text-body-2 text-md-body-1 text-grey-darken-2">暂无卡片组</p>
          <p class="text-caption text-md-body-2 text-grey">创建记忆卡片,高效复习知识点</p>
          <v-btn
            color="primary"
            variant="outlined"
            rounded="md"
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            class="mt-3 mt-md-4"
            to="/memory-review"
          >
            <v-icon icon="mdi-brain" :size="$vuetify.display.mobile ? 16 : 18" class="mr-2" />
            前往复习中心
          </v-btn>
        </div>

        <!-- 删除确认对话框 -->
        <ConfirmDialog
          v-model="showDeleteDialog"
          title="确认删除"
          message="确定要删除该卡片组吗？此操作不可恢复。"
          confirm-text="确认删除"
          @confirm="confirmDelete"
        />

        <!-- 卡片组详情对话框 -->
        <DeckDetailDialog v-model="showDeckDetail" :deck="selectedDeck" @add-to-study="handleAddToStudy" />
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { useUserStore } from '@/stores/modules/user'
import { memoryApi } from '@/api'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import DeckDetailDialog from '@/components/features/read/DeckDetailDialog.vue'

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
    successMessage: '已删除该卡片组',
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
    courseName: deck.course?.name || '未知课程',
    nodeName: deck.node?.name || '未知节点',
    cardCount: deck.cardCount || 0,
    createdAt: deck.createdAt || null,
    courseId: deck.courseId,
    nodeId: deck.nodeId,
    postId: deck.postId || deck.sourcePostId, // 关联的帖子ID
    course: deck.course || null,
    node: deck.node || null,
    // 保留原始字段以备需要
    title: deck.title || '',
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
  return new Date(date).toLocaleDateString('zh-CN')
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
    creatorName: userStore.currentUser?.name || '当前用户',
    creatorId: userStore.currentUser?.id || null,
    creatorAvatar: userStore.currentUser?.avatar || null,
    upvoteCount: 0,
    hasUpvoted: false,
    nodeId: deck.nodeId, // 设置关联的节点ID
  }
  showDeckDetail.value = true
}

// 处理添加到学习
const handleAddToStudy = (deck: any) => {
  console.log('添加到学习:', deck)
  // 这里可以处理添加到学习的逻辑
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
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 140px;
  align-self: flex-start;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.deck-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-outline-variant));
}

.deck-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 32px;
}

@media (min-width: 600px) {
  .deck-description {
    min-height: 40px;
  }
}

.min-w-0 {
  min-width: 0;
}

/* 课程链接按钮样式 */
.course-link-btn {
  color: rgb(var(--v-theme-primary)) !important;
  text-decoration: none;
  font-weight: 500;
  min-height: auto !important;
  padding: 0 !important;
}

.course-link-btn:hover {
  text-decoration: underline;
}

/* 移动端取消 sticky */
@media (max-width: 960px) {
  .sticky-sidebar {
    position: relative;
    top: 0;
    max-height: none;
    margin-bottom: 16px;
  }
}
</style>
