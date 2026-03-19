<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import SinglePost from './SinglePost.vue'
import CommentSection from '@/components/common/CommentSection.vue'
import CreateDeckDialog from './CreateDeckDialog.vue'
import MemoryCardSidebar from './MemoryCardSidebar.vue'
import DeckDetailDialog from './DeckDetailDialog.vue'
import AiAssistant from './AiAssistant.vue'
import { pageApi, memoryApi } from '@/api'
import type { ReadResponse } from '@/api/modules/page'
import type { MemoryCardDeck } from '@/types/memory'
import { useFetch } from '@/composables/useFetch'
import { ObjectType } from '@/enums'
import { convertVoteType } from '@/utils/postUtils'

interface Props {
  // 是否显示节点标题（在 ContentReadPage 中可能不需要）
  showNodeHeader?: boolean
  // 是否显示右侧工具栏（在 ContentReadPage 中由父组件控制）
  showRightSidebar?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showNodeHeader: true,
  showRightSidebar: true,
})

const router = useRouter()
const route = useRoute()

// 基本状态
const showCreateDeckDialog = ref(false)
const selectedDeck = ref<MemoryCardDeck | null>(null)
const showDeckDetailDialog = ref(false)
const assistantSheetOpen = ref(false)
const memorySheetOpen = ref(false)

// 答疑助手 - 文章选中文本
const articleSelectedText = ref('')

// 处理文章区域文本选择
const handleTextSelected = (text: string) => {
  articleSelectedText.value = text
}

// 目标评论ID（从 URL 获取）
const targetCommentId = computed(() => {
  if (route.query.commentId) {
    return Number(route.query.commentId)
  }
  return null
})

// 目标子评论ID（从 URL 获取）
const targetSubCommentId = computed(() => {
  if (route.query.subCommentId) {
    return Number(route.query.subCommentId)
  }
  return null
})

// 使用 useFetch 加载页面数据
const {
  data,
  loading: dataLoading,
  execute: loadData,
} = useFetch<ReadResponse>({
  fetchFn: () => {
    const params: { postId?: number; commentId?: number } = {}
    if (route.query.postId) {
      params.postId = Number(route.query.postId)
    } else if (route.query.commentId) {
      params.commentId = Number(route.query.commentId)
    } else {
      return Promise.reject(new Error('缺少 postId 或 commentId 参数'))
    }
    return pageApi.readPostDetail(params)
  },
  immediate: true,
  onDataReady: () => {
    // 特殊处理：如果返回了 commentId 和 subCommentId，说明需要重定向
    if (data.value.commentId && data.value.subCommentId) {
      router.replace({
        path: '/read',
        query: {
          ...(route.query.courseId ? { courseId: route.query.courseId } : {}),
          ...(route.query.nodeId ? { nodeId: route.query.nodeId } : {}),
          commentId: String(data.value.commentId),
          subCommentId: String(data.value.subCommentId),
        },
      })
      return
    }

    // 处理投票类型
    const posting = data.value.currPosting || data.value.post
    if (posting) {
      posting.voteType = convertVoteType(posting.voteType)
    }

    // 处理 otherPostings
    data.value.otherPostings?.forEach((posting: any) => {
      posting.voteType = convertVoteType(posting.voteType)
    })
  },
  onError: () => {
    router.replace({
      path: '/error/404',
      state: { message: '您访问的内容不存在或已被删除' },
    })
  },
})

// 处理创建卡片组成功
const handleDeckCreated = (deck: MemoryCardDeck) => {
  console.log('Deck created:', deck)
  showCreateDeckDialog.value = false
}

// 处理创建卡片组
const handleCreateDeck = () => {
  const posting = data.value?.currPosting || data.value?.post
  if (posting) {
    showCreateDeckDialog.value = true
  }
}

// 处理查看卡片组详情
const handleViewDeck = (deck: MemoryCardDeck) => {
  selectedDeck.value = deck
  showDeckDetailDialog.value = true
}

// 处理查看全部评论
const handleViewAllComments = () => {
  const postId = data.value?.currPosting?.id || data.value?.post?.id
  if (postId) {
    router.replace({
      path: '/read',
      query: {
        ...(route.query.courseId ? { courseId: route.query.courseId } : {}),
        ...(route.query.nodeId ? { nodeId: route.query.nodeId } : {}),
        postId: String(postId),
      },
    })
  }
}

// 监听路由变化，重新加载数据
watch(
  () => [route.query.postId, route.query.commentId],
  () => {
    if (route.query.postId || route.query.commentId) {
      loadData()
    }
  },
  { deep: true }
)

// 是否在课程模式下（有 courseId 或 nodeId）
const isInCourseMode = computed(() => {
  return !!(route.query.courseId || route.query.nodeId)
})

// 返回列表页
const goBackToList = () => {
  router.back()
}

// 暴露数据给父组件
defineExpose({
  data,
  dataLoading,
})
</script>

<template>
  <div class="post-detail">
    <!-- 加载状态 -->
    <LoadingSpinner v-if="dataLoading" />

    <!-- 内容区 -->
    <template v-else-if="data">
      <div class="post-detail-content">
        <!-- 中间内容区 -->
        <div class="center-content">
          <!-- 节点标题行 -->
          <div v-if="showNodeHeader && data.node" :class="isInCourseMode ? 'node-header-sticky node-header-compact' : 'node-header'">
            <div class="d-flex align-center">
              <!-- 返回按钮（仅在课程模式下显示） -->
              <v-btn
                v-if="isInCourseMode"
                variant="text"
                color="grey-darken-2"
                size="default"
                icon="mdi-arrow-left"
                class="me-2"
                density="comfortable"
                @click="goBackToList"
              ></v-btn>
              <v-icon
                icon="mdi-list-box-outline"
                color="primary-darken-1"
                :size="isInCourseMode ? 18 : ($vuetify.display.mobile ? 20 : 24)"
              ></v-icon>
              <h2
                class="font-weight-bold text-grey-darken-4 ms-2 ms-md-3"
                :class="isInCourseMode ? 'text-body-1' : 'text-h6 text-md-h5'"
              >
                {{ data.node.name }}
              </h2>
            </div>
          </div>

          <!-- 节点描述（仅在非课程模式下显示） -->
          <div v-if="showNodeHeader && !isInCourseMode && data.node?.description" class="node-description mb-4 mb-md-6">
            <p class="text-body-2 text-md-body-1 text-grey-darken-1 mb-0">
              {{ data.node.description }}
            </p>
          </div>

          <!-- 文章详情 -->
          <SinglePost
            v-if="data.currPosting || data.post"
            :data="data"
            :posting="data.currPosting || data.post"
            :detail="true"
            @text-selected="handleTextSelected"
          />

          <!-- 评论区 -->
          <CommentSection
            v-if="data.currPosting || data.post"
            :post-id="(data.currPosting || data.post).id"
            :comment-count="(data.currPosting || data.post).commentCount || 0"
            :object-type="ObjectType.POST"
            :target-comment-id="targetCommentId"
            :target-sub-comment-id="targetSubCommentId"
            class="mt-6"
            @view-all-comments="handleViewAllComments"
          />
        </div>

        <!-- 右侧工具栏 -->
        <div v-if="showRightSidebar && (data.currPosting || data.post)" class="right-sidebar">
          <div class="sidebar-sticky">
            <!-- AI答疑助手 -->
            <AiAssistant
              v-model:selected-text="articleSelectedText"
              :node-title="data.node?.name"
              :node-description="data.node?.description"
              class="mb-4"
            />

            <!-- 记忆卡片组侧边栏 -->
            <MemoryCardSidebar
              :post-id="(data.currPosting || data.post).id"
              class="mb-4 mb-md-4"
              @create-deck="handleCreateDeck"
              @view-deck="handleViewDeck"
            />
          </div>
        </div>
      </div>
    </template>

    <!-- 创建卡片组对话框 -->
    <CreateDeckDialog
      v-if="data && (data.currPosting || data.post)"
      v-model="showCreateDeckDialog"
      :post-id="(data.currPosting || data.post).id"
      @created="handleDeckCreated"
    />

    <!-- 卡片组详情对话框 -->
    <DeckDetailDialog
      v-model="showDeckDetailDialog"
      :deck="selectedDeck"
      :course-id="data?.course?.id"
    />

    <!-- 移动端AI答疑助手底部面板 -->
    <v-bottom-sheet v-if="$vuetify.display.mobile" v-model="assistantSheetOpen" max-height="70vh">
      <v-card rounded="t-xl">
        <v-card-title class="pa-4 d-flex align-center justify-space-between">
          <div class="d-flex align-center">
            <v-icon
              icon="mdi-robot-excited"
              color="primary"
              class="mr-2"
              :size="$vuetify.display.mobile ? 20 : 24"
            />
            <span class="text-h6 font-weight-bold">答疑助手</span>
          </div>
          <v-btn
            icon="mdi-close"
            variant="text"
            size="small"
            @click="assistantSheetOpen = false"
          />
        </v-card-title>

        <v-divider />

        <v-card-text class="pa-0" style="max-height: calc(70vh - 73px); overflow-y: auto">
          <AiAssistant
            v-model:selected-text="articleSelectedText"
            :node-title="data.node?.name"
            :node-description="data.node?.description"
            class="mobile-assistant"
          />
        </v-card-text>
      </v-card>
    </v-bottom-sheet>

    <!-- 移动端记忆卡片底部面板 -->
    <v-bottom-sheet v-if="$vuetify.display.mobile" v-model="memorySheetOpen" max-height="70vh">
      <v-card rounded="t-xl">
        <v-card-title class="pa-4 pa-md-4 d-flex align-center justify-space-between">
          <div class="d-flex align-center">
            <v-icon
              icon="mdi-cards-outline"
              color="primary"
              class="mr-2"
              :size="$vuetify.display.mobile ? 18 : 20"
            />
            <span class="text-h6 text-md-h5 font-weight-bold">记忆卡片组</span>
          </div>
          <div class="d-flex align-center" style="gap: 8px">
            <v-btn
              color="success"
              variant="flat"
              rounded="lg"
              :size="$vuetify.display.mobile ? 'small' : 'default'"
              prepend-icon="mdi-plus"
              class="text-caption text-md-body-2"
              @click="handleCreateDeck"
            >
              创建
            </v-btn>
            <v-btn
              icon="mdi-close"
              variant="text"
              :size="$vuetify.display.mobile ? 'small' : 'default'"
              @click="memorySheetOpen = false"
            />
          </div>
        </v-card-title>

        <v-divider />

        <v-card-text class="pa-0" style="max-height: calc(70vh - 73px); overflow-y: auto">
          <MemoryCardSidebar
            v-if="data && (data.currPosting || data.post)"
            :post-id="(data.currPosting || data.post).id"
            class="mobile-memory-sidebar"
            @create-deck="handleCreateDeck"
            @view-deck="handleViewDeck"
          />
        </v-card-text>
      </v-card>
    </v-bottom-sheet>

    <!-- 移动端浮动按钮组 -->
    <div v-if="$vuetify.display.mobile && !showRightSidebar" class="mobile-fab-group">
      <!-- AI答疑助手按钮 -->
      <v-btn
        icon
        color="primary"
        :size="$vuetify.display.mobile ? 'large' : 'x-large'"
        elevation="6"
        class="mb-3"
        @click="assistantSheetOpen = true"
      >
        <v-icon :size="$vuetify.display.mobile ? 20 : 24">mdi-robot-excited</v-icon>
        <v-tooltip activator="parent" location="left">答疑助手</v-tooltip>
      </v-btn>

      <!-- 记忆卡片按钮 -->
      <v-btn
        icon
        color="success"
        :size="$vuetify.display.mobile ? 'large' : 'x-large'"
        elevation="6"
        @click="memorySheetOpen = true"
      >
        <v-icon :size="$vuetify.display.mobile ? 20 : 24">mdi-cards</v-icon>
        <v-tooltip activator="parent" location="left">记忆卡片</v-tooltip>
      </v-btn>
    </div>
  </div>
</template>

<style scoped>
.post-detail {
  width: 100%;
}

.post-detail-content {
  display: flex;
  justify-content: center;
  max-width: 100%;
}

/* 中间内容区 - 固定宽度 */
.center-content {
  flex: 1 1 750px;
  max-width: 750px;
  padding: 4px 26px 40px 26px;
}

/* 节点标题行 - sticky（课程模式） */
.node-header-sticky {
  position: sticky;
  top: 56px;
  background-color: white;
  z-index: 10;
  padding: 12px 0;
  margin-bottom: 8px;
}

/* 节点标题行 - 非 sticky（独立模式） */
.node-header {
  padding: 4px 0;
  margin-bottom: 8px;
}

/* 课程模式下的紧凑样式 */
.node-header-sticky.node-header-compact {
  padding: 0 0 4px 0;
  margin-bottom: 4px;
  margin-left: -9px;
}

/* 节点描述 */
.node-description {
  margin-top: 0;
}

/* 右侧边栏 */
.right-sidebar {
  flex: 0 1 360px;
  max-width: 360px;
  padding: 24px 0 24px 24px;
}

.sidebar-sticky {
  position: sticky;
  top: 46px;
  max-height: calc(100vh - 61px);
}

.sidebar-card {
  background-color: white;
  border: 1px solid rgb(var(--v-theme-border));
}

.sidebar-card .v-card-title {
  font-size: 0.9375rem;
  font-weight: 600;
}

/* 移动端浮动按钮组 */
.mobile-fab-group {
  position: fixed;
  bottom: 80px;
  right: 24px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

/* 移动端记忆卡片侧边栏 - 隐藏标题 */
.mobile-memory-sidebar :deep(.v-card-title) {
  display: none;
}

/* 移动端记忆卡片侧边栏 - 筛选控件区域增加顶部间距 */
.mobile-memory-sidebar :deep(.filter-controls) {
  padding-top: 16px !important;
}

/* 移动端答疑助手 - 隐藏标题和边框 */
.mobile-assistant :deep(.v-card-title) {
  display: none;
}

.mobile-assistant {
  border: none !important;
  box-shadow: none !important;
}

/* 中等屏幕：隐藏右侧栏 */
@media (max-width: 1700px) {
  .right-sidebar {
    display: none;
  }
}

/* 小屏幕 */
@media (max-width: 1280px) and (min-width: 751px) {
  .center-content {
    padding: 0 !important;
  }

  .node-header {
    padding-top: 24px;
  }
}

/* 超小屏幕 */
@media (max-width: 750px) {
  .center-content {
    flex: 1 !important;
    max-width: none !important;
    min-width: 0 !important;
    padding: 16px 4px 32px 4px !important;
    width: 100% !important;
  }

  .node-header {
    padding-top: 16px;
  }
}

@media (max-width: 600px) {
  .center-content {
    padding: 12px 4px 24px 4px !important;
  }

  .node-header {
    padding-top: 12px;
  }
}
</style>
