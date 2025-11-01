<script setup lang="ts">
import { inject, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { adminPostServiceV1 } from '@/services/api/v1/adminApiServiceV1'
import { ContentState, PostType, ApprovalAction } from '@/types/enums'
import type { Post } from '@/types/post'
import RejectBanDialog from './RejectBanDialog.vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

const currentTab = ref<string>('pending')

// 筛选条件
const filterNodeId = ref<number | undefined>(undefined)
const filterCreatorId = ref<number | undefined>(undefined)
const isFilterMode = ref<boolean>(false)

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const currentPost = ref<Post | null>(null)
const dialogType = ref<'reject' | 'ban'>('reject')

// 标签配置
interface TabConfig {
  key: string
  label: string
  state: ContentState
  icon: string
  color: string
}

const tabs: TabConfig[] = [
  {
    key: 'pending',
    label: '待审核',
    state: ContentState.SUBMITTED,
    icon: 'mdi-clock-outline',
    color: 'orange'
  },
  {
    key: 'approved',
    label: '已批准',
    state: ContentState.PUBLISHED,
    icon: 'mdi-check-circle',
    color: 'green'
  },
  {
    key: 'rejected',
    label: '已拒绝',
    state: ContentState.REJECTED,
    icon: 'mdi-close-circle',
    color: 'red'
  },
  {
    key: 'banned',
    label: '已屏蔽',
    state: ContentState.BANNED,
    icon: 'mdi-cancel',
    color: 'grey'
  }
]

// 使用 useInfiniteScroll 加载帖子列表
const {
  items: postList,
  loading,
  hasMore,
  loadMore,
  reset: resetPostList
} = useInfiniteScroll({
  fetchFn: (params) => {
    const currentTabConfig = tabs.find(tab => tab.key === currentTab.value)
    const state = currentTabConfig?.state

    if (isFilterMode.value) {
      return adminPostServiceV1.getPostsByFilter(
        filterNodeId.value,
        filterCreatorId.value,
        params.lastId,
        state
      )
    } else {
      return adminPostServiceV1.getPostsByState(currentTab.value, params.lastId, 20)
    }
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id
  }),
  initialParams: {
    lastId: undefined
  }
})

// 应用筛选
const applyFilter = (): void => {
  if (!filterNodeId.value && !filterCreatorId.value) {
    showSnackbar?.('请至少输入一个筛选条件', 'warning')
    return
  }
  isFilterMode.value = true
  resetPostList()
}

// 清除筛选
const clearFilter = (): void => {
  filterNodeId.value = undefined
  filterCreatorId.value = undefined
  isFilterMode.value = false
  resetPostList()
}

// 使用 useMutation 批准/拒绝帖子
const { execute: executeApprovePost } = useMutation(
  (data: { postId: number; approve: boolean }) =>
    adminPostServiceV1.approvePost(data.postId, data.approve ? ApprovalAction.APPROVE : ApprovalAction.REJECT),
  {
    successMessage: '操作成功',
    onSuccess: (response, data) => {
      const updatedPost = response as Post
      const index = postList.value.findIndex(p => p.id === data.postId)
      if (index !== -1) {
        const currentTabConfig = tabs.find(tab => tab.key === currentTab.value)
        if (updatedPost.state !== currentTabConfig?.state) {
          postList.value.splice(index, 1)
        } else {
          postList.value[index].state = updatedPost.state
        }
      }
    }
  }
)

const approvePost = async (post: Post, approve: boolean): Promise<void> => {
  await executeApprovePost({ postId: post.id, approve })
}

// 显示拒绝对话框
const showRejectDialog = (post: Post) => {
  currentPost.value = post
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 显示屏蔽对话框
const showBanDialog = (post: Post) => {
  currentPost.value = post
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (data: { postId: number; action: string; reason: string }) =>
    adminPostServiceV1.approvePost(data.postId, data.action, data.reason),
  {
    onSuccess: (_, data) => {
      const message = data.action === ApprovalAction.BAN ? '已屏蔽' : '已拒绝'
      const targetState = data.action === ApprovalAction.BAN ? ContentState.BANNED : ContentState.REJECTED
      showSnackbar?.(message, 'success')

      const index = postList.value.findIndex(p => p.id === data.postId)
      if (index !== -1) {
        const currentTabConfig = tabs.find(tab => tab.key === currentTab.value)
        if (currentTabConfig?.state !== targetState) {
          postList.value.splice(index, 1)
        } else {
          postList.value[index].state = targetState
        }
      }

      showReasonDialog.value = false
      currentPost.value = null
    }
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string) => {
  if (!currentPost.value) return

  const action = dialogType.value === 'ban' ? ApprovalAction.BAN : ApprovalAction.REJECT
  await executeRejectOrBan({
    postId: currentPost.value.id,
    action,
    reason
  })
}

// 拒绝文章（已弃用，保留兼容）
const rejectPost = async (post: Post): Promise<void> => {
  showRejectDialog(post)
}

// 屏蔽文章（已弃用，保留兼容）
const banPost = async (post: Post): Promise<void> => {
  showBanDialog(post)
}

// 使用 useMutation 取消屏蔽文章
const { execute: executeUnbanPost } = useMutation(
  (postId: number) => adminPostServiceV1.approvePost(postId, true),
  {
    successMessage: '已取消屏蔽',
    onSuccess: (_, postId) => {
      const index = postList.value.findIndex(p => p.id === postId)
      if (index !== -1) {
        const currentTabConfig = tabs.find(tab => tab.key === currentTab.value)
        if (currentTabConfig?.state !== ContentState.PUBLISHED) {
          postList.value.splice(index, 1)
        } else {
          postList.value[index].state = ContentState.PUBLISHED
        }
      }
    }
  }
)

const unbanPost = async (post: Post): Promise<void> => {
  await executeUnbanPost(post.id)
}

// 监听tab切换，重新加载数据
const handleTabChange = (newTab: string) => {
  resetPostList()
}

// 解析目录内容 JSON
const parseContents = (content: string) => {
  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed)) {
      return parsed
    }
    return []
  } catch (e) {
    // 向后兼容：如果解析失败，尝试按逗号分割
    return content.split(',').map((item, index) => ({
      id: index,
      name: item.trim(),
      description: ''
    }))
  }
}

</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-note-check-outline" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">
            {{ t('admin.articleReview') }}
          </h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">{{ t('admin.reviewUserArticles') }}</p>
        </div>
      </div>
      <v-chip variant="tonal" color="teal" rounded="lg">
        <v-icon
          icon="mdi-file-document-multiple"
          size="16"
          class="mr-1"
        ></v-icon>
        <span class="text-caption">{{ postList.length }}</span>
      </v-chip>
    </div>

    <!-- 筛选区域 -->
    <v-card v-if="!isFilterMode" flat class="pa-4 bg-grey-lighten-5 rounded-lg mb-6">
      <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
        <v-icon icon="mdi-filter-outline" size="16" class="mr-2"></v-icon>
        高级筛选
      </h4>
      <v-row dense>
        <v-col cols="12" sm="5">
          <v-text-field
            v-model.number="filterNodeId"
            type="number"
            label="节点 ID"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="5">
          <v-text-field
            v-model.number="filterCreatorId"
            type="number"
            label="用户 ID"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="2">
          <v-btn
            variant="flat"
            color="primary"
            rounded="lg"
            block
            @click="applyFilter"
          >
            <v-icon icon="mdi-magnify" class="mr-1"></v-icon>
            筛选
          </v-btn>
        </v-col>
      </v-row>
    </v-card>

    <!-- 筛选结果提示 -->
    <v-alert
      v-if="isFilterMode"
      type="info"
      color="teal"
      variant="outlined"
      class="mb-6"
      border="top"
      rounded="lg"
      closable
      @click:close="clearFilter"
    >
      <div class="d-flex align-center justify-space-between">
        <div>
          <span class="font-weight-medium">筛选条件：</span>
          <v-chip v-if="filterNodeId" size="small" class="mx-1">节点 ID: {{ filterNodeId }}</v-chip>
          <v-chip v-if="filterCreatorId" size="small" class="mx-1">用户 ID: {{ filterCreatorId }}</v-chip>
        </div>
      </div>
    </v-alert>

    <!-- 状态标签 -->
    <v-tabs
      v-model="currentTab"
      color="primary"
      class="mb-6"
      show-arrows
      @update:model-value="handleTabChange"
    >
      <v-tab
        v-for="tab in tabs"
        :key="tab.key"
        :value="tab.key"
        class="text-none"
      >
        <v-icon
          :icon="tab.icon"
          :color="`${tab.color}-darken-1`"
          size="18"
          class="mr-2"
        ></v-icon>
        {{ tab.label }}
      </v-tab>
    </v-tabs>

    <div v-if="postList.length === 0" class="text-center py-12">
      <v-icon
        icon="mdi-file-document-outline"
        size="48"
        color="grey-lighten-1"
        class="mb-4"
      ></v-icon>
      <p class="text-body-1 text-grey-darken-1">
        {{ currentTab === 'pending' ? t('admin.noArticlesToReview') : `暂无${tabs.find(tab => tab.key === currentTab)?.label}的文章` }}
      </p>
    </div>

    <div
      v-for="post in postList"
      :key="post.id"
      class="mb-4"
      v-intersect="{
        handler: (isIntersecting: boolean) => {
          if (isIntersecting && post === postList[postList.length - 1] && hasMore && !loading) {
            loadMore()
          }
        }
      }"
    >
      <v-card flat class="border rounded-lg pa-5" hover>
        <div class="d-flex align-start">
          <!-- 状态和操作区域 -->
          <div class="mr-4 action-area">
            <div class="mb-3">
              <v-chip
                v-if="post.state == ContentState.SUBMITTED"
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                {{ t('admin.pending') }}
              </v-chip>
              <v-chip
                v-if="post.state == ContentState.PUBLISHED"
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.approved') }}
              </v-chip>
              <v-chip
                v-if="post.state == ContentState.REJECTED"
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.rejected') }}
              </v-chip>
              <v-chip
                v-if="post.state == ContentState.BANNED"
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-cancel" size="14" class="mr-1"></v-icon>
                {{ t('admin.banned') }}
              </v-chip>
            </div>
            <!-- 待审核状态：批准、拒绝、屏蔽 -->
            <div v-if="post.state == ContentState.SUBMITTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="approvePost(post, true)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                批准
              </v-btn>
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="rejectPost(post)"
              >
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                拒绝
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                @click="banPost(post)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已批准状态：撤销通过、屏蔽 -->
            <div v-if="post.state == ContentState.PUBLISHED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                @click="rejectPost(post)"
              >
                <v-icon icon="mdi-undo" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                撤销通过
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                @click="banPost(post)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已拒绝状态：通过、屏蔽 -->
            <div v-if="post.state == ContentState.REJECTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="approvePost(post, true)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                通过
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                @click="banPost(post)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已屏蔽状态：取消屏蔽、降级为拒绝 -->
            <div v-if="post.state == ContentState.BANNED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="blue-lighten-4"
                rounded="lg"
                size="small"
                @click="unbanPost(post)"
              >
                <v-icon icon="mdi-lock-open" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                取消屏蔽
              </v-btn>
              <v-btn
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                @click="rejectPost(post)"
              >
                <v-icon icon="mdi-arrow-down" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                降级为拒绝
              </v-btn>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1 post-content-area">
            <div class="d-flex align-center mb-3">
              <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                <v-icon icon="mdi-account" color="grey-darken-1" size="18"></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-2">
                  {{ t('admin.articleId') }}: {{ post.id }}
                </div>
                <div class="text-caption text-grey-darken-1">{{ post.createdAt }}</div>
              </div>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <div
                v-if="post.type == PostType.ARTICLE"
                class="tiptap post-content"
                v-html="post.content"
              ></div>
              <div v-if="post.type == PostType.CONTENTS">
                <div class="text-caption text-grey-darken-1 mb-3">{{ t('admin.directory') }}</div>
                <div class="contents-list">
                  <div
                    v-for="(item, index) in parseContents(post.content)"
                    :key="index"
                    class="content-item mb-3"
                  >
                    <div class="d-flex align-start">
                      <div class="text-body-2 font-weight-medium text-grey-darken-3 mr-2">{{ index + 1 }}.</div>
                      <div class="flex-grow-1">
                        <div class="text-body-2 font-weight-medium text-grey-darken-3">{{ item.name }}</div>
                        <div v-if="item.description" class="text-caption text-grey-darken-1 mt-1">
                          {{ item.description }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </v-card>
    </div>

    <!-- 加载更多指示器 -->
    <div v-if="loading" class="text-center py-4">
      <v-progress-circular
        indeterminate
        color="primary"
        size="24"
      ></v-progress-circular>
      <span class="ml-2 text-grey-darken-1">加载中...</span>
    </div>

    <!-- 没有更多数据提示 -->
    <div v-if="!hasMore && postList.length > 0" class="text-center py-4">
      <span class="text-grey-darken-1">没有更多数据了</span>
    </div>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="currentPost?.title || ''"
      :item-state="currentPost?.state"
      item-type="帖子"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />
  </div>
</template>

<style scoped>
  /* 防止flex子元素溢出 */
  .post-content-area {
    min-width: 0;
    overflow: hidden;
  }

  .tiptap.post-content {
    max-height: 200px;
    overflow-y: auto;
    overflow-x: auto;
    word-wrap: break-word;
    word-break: break-word;
    overflow-wrap: break-word;
  }

  /* 防止内容中的元素溢出 */
  .tiptap.post-content :deep(*) {
    max-width: 100%;
  }

  /* 代码块样式 */
  .tiptap.post-content :deep(pre) {
    overflow-x: auto;
    white-space: pre;
    word-wrap: normal;
  }

  /* 表格样式 */
  .tiptap.post-content :deep(table) {
    display: block;
    overflow-x: auto;
    max-width: 100%;
  }

  /* 图片样式 */
  .tiptap.post-content :deep(img) {
    max-width: 100%;
    height: auto;
  }

  .action-area {
    min-width: 200px;
  }
</style>