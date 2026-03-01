<script setup lang="ts">
import { inject, ref } from 'vue'
import { adminApi } from '@/api'
import { ContentState, PostType } from '@/enums'
import type { Post } from '@/types/post.d'
import RejectBanDialog from './RejectBanDialog.vue'
import { useFetchForScroll } from '@/composables/useFetchForScroll'
import { useMutation } from '@/composables/useMutation'

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
    color: 'orange',
  },
  {
    key: 'approved',
    label: '已批准',
    state: ContentState.PUBLISHED,
    icon: 'mdi-check-circle',
    color: 'green',
  },
  {
    key: 'rejected',
    label: '已拒绝',
    state: ContentState.REJECTED,
    icon: 'mdi-close-circle',
    color: 'red',
  },
  {
    key: 'banned',
    label: '已屏蔽',
    state: ContentState.BANNED,
    icon: 'mdi-cancel',
    color: 'grey',
  },
]

// 获取当前状态
const getCurrentState = () => tabs.find((tab) => tab.key === currentTab.value)?.state

// 使用 useFetchForScroll 加载帖子列表
const {
  items: postList,
  loading,
  hasMore,
  loadMore,
  reset: resetPostList,
} = useFetchForScroll<Post>({
  fetchFn: (params) => {
    const state = getCurrentState()
    if (isFilterMode.value) {
      return adminApi.getPostsByFilter(
        filterNodeId.value,
        filterCreatorId.value,
        params.lastId ?? undefined,
        state
      )
    } else {
      return adminApi.getContentsByState('post', state, params.lastId ?? undefined)
    }
  },
  initialParams: {
    lastId: null,
  },
  immediate: true,
})

// 应用筛选
const applyFilter = (): void => {
  if (!filterNodeId.value && !filterCreatorId.value) {
    showSnackbar?.('请至少输入一个筛选条件', 'warning')
    return
  }
  isFilterMode.value = true
  resetPostList()
  loadMore()
}

// 清除筛选
const clearFilter = (): void => {
  filterNodeId.value = undefined
  filterCreatorId.value = undefined
  isFilterMode.value = false
  resetPostList()
  loadMore()
}

// 使用 useMutation 批准/拒绝帖子
const { execute: executeApprovePost } = useMutation(
  (data: { postId: number; approve: boolean }) =>
    adminApi.operateContent('post', data.postId, { action: data.approve ? 'approve' : 'reject' }),
  {
    successMessage: '操作成功',
    onSuccess: (response, data) => {
      const updatedPost = response as Post
      const index = postList.value.findIndex((p) => p.id === data.postId)
      if (index !== -1) {
        const currentTabConfig = tabs.find((tab) => tab.key === currentTab.value)
        if (updatedPost.state !== currentTabConfig?.state) {
          postList.value.splice(index, 1)
        } else {
          postList.value[index].state = updatedPost.state
        }
      }
    },
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
    adminApi.operateContent('post', data.postId, { action: data.action.toLowerCase(), reason: data.reason }),
  {
    onSuccess: (_, data) => {
      const message = data.action === 'BAN' ? '已屏蔽' : '已拒绝'
      const targetState = data.action === 'BAN' ? ContentState.BANNED : ContentState.REJECTED
      showSnackbar?.(message, 'success')

      const index = postList.value.findIndex((p) => p.id === data.postId)
      if (index !== -1) {
        const currentTabConfig = tabs.find((tab) => tab.key === currentTab.value)
        if (currentTabConfig?.state !== targetState) {
          postList.value.splice(index, 1)
        } else {
          postList.value[index].state = targetState
        }
      }

      showReasonDialog.value = false
      currentPost.value = null
    },
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string) => {
  if (!currentPost.value) return

  const action = dialogType.value === 'ban' ? 'BAN' : 'REJECT'
  await executeRejectOrBan({
    postId: currentPost.value.id,
    action,
    reason,
  })
}

// 拒绝文章
const rejectPost = async (post: Post): Promise<void> => {
  showRejectDialog(post)
}

// 屏蔽文章
const banPost = async (post: Post): Promise<void> => {
  showBanDialog(post)
}

// 使用 useMutation 取消屏蔽文章
const { execute: executeUnbanPost } = useMutation(
  (postId: number) => adminApi.operateContent('post', postId, { action: 'approve' }),
  {
    successMessage: '已取消屏蔽',
    onSuccess: (_, postId) => {
      const index = postList.value.findIndex((p) => p.id === postId)
      if (index !== -1) {
        const currentTabConfig = tabs.find((tab) => tab.key === currentTab.value)
        if (currentTabConfig?.state !== ContentState.PUBLISHED) {
          postList.value.splice(index, 1)
        } else {
          postList.value[index].state = ContentState.PUBLISHED
        }
      }
    },
  }
)

const unbanPost = async (post: Post): Promise<void> => {
  await executeUnbanPost(post.id)
}

// 监听tab切换，重新加载数据
const handleTabChange = () => {
  resetPostList()
  loadMore()
}

// 解析目录内容 JSON
const parseContents = (content: string) => {
  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed)) {
      return parsed
    }
    return []
  } catch {
    return content.split(',').map((item, index) => ({
      id: index,
      name: item.trim(),
      description: '',
    }))
  }
}

const getStateText = (state: number): string => {
  switch (state) {
    case ContentState.SUBMITTED:
      return '待审核'
    case ContentState.PUBLISHED:
      return '已通过'
    case ContentState.REJECTED:
      return '已拒绝'
    case ContentState.BANNED:
      return '已封禁'
    default:
      return '未知'
  }
}

const getStateColor = (state: number): string => {
  switch (state) {
    case ContentState.SUBMITTED:
      return 'orange-lighten-4'
    case ContentState.PUBLISHED:
      return 'green-lighten-4'
    case ContentState.REJECTED:
      return 'red-lighten-4'
    case ContentState.BANNED:
      return 'grey-lighten-2'
    default:
      return 'grey-lighten-3'
  }
}
</script>

<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">文章审核</h2>

    <!-- ID查询 -->
    <v-card flat class="border mb-4">
      <v-card-text>
        <v-row align="center">
          <v-col cols="3">
            <v-text-field
              v-model.number="filterNodeId"
              type="number"
              label="节点 ID"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="applyFilter"
            ></v-text-field>
          </v-col>
          <v-col cols="3">
            <v-text-field
              v-model.number="filterCreatorId"
              type="number"
              label="用户 ID"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="applyFilter"
            ></v-text-field>
          </v-col>
          <v-col cols="auto">
            <v-btn variant="tonal" size="default" @click="applyFilter">
              <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
              筛选
            </v-btn>
            <v-btn
              v-if="isFilterMode"
              variant="text"
              size="default"
              @click="clearFilter"
            >
              清除
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 文章列表 -->
    <v-card flat class="border">
      <v-card-text>
        <!-- 状态标签 -->
        <v-tabs
          v-model="currentTab"
          color="primary"
          density="compact"
          @update:model-value="handleTabChange"
          class="mb-4"
        >
          <v-tab v-for="tab in tabs" :key="tab.key" :value="tab.key" class="text-none" size="small">
            <v-icon :icon="tab.icon" size="14" class="mr-1"></v-icon>
            {{ tab.label }}
          </v-tab>
        </v-tabs>

        <!-- 首次加载状态 -->
        <div v-if="loading && postList.length === 0" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!loading && postList.length === 0" class="text-center py-12">
          <v-icon icon="mdi-file-document-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">
            暂无{{ tabs.find((tab) => tab.key === currentTab)?.label }}的文章
          </p>
        </div>

        <!-- 列表 -->
        <div v-else>
          <div
            v-for="post in postList"
            :key="post.id"
            v-intersect="{
              handler: (isIntersecting: boolean) => {
                if (isIntersecting && post === postList[postList.length - 1] && hasMore && !loading) {
                  loadMore()
                }
              },
            }"
            class="list-item mb-3"
          >
            <div class="d-flex align-start">
              <!-- 操作区 -->
              <div class="action-area mr-4">
                <!-- 待审核 -->
                <div v-if="post.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block @click="approvePost(post, true)">
                    批准
                  </v-btn>
                  <v-btn variant="tonal" color="error" size="small" block @click="rejectPost(post)">
                    拒绝
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="banPost(post)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已通过 -->
                <div v-if="post.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="warning" size="small" block @click="rejectPost(post)">
                    撤回
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="banPost(post)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已拒绝 -->
                <div v-if="post.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block @click="approvePost(post, true)">
                    通过
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="banPost(post)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已屏蔽 -->
                <div v-if="post.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="info" size="small" block @click="unbanPost(post)">
                    解封
                  </v-btn>
                  <v-btn variant="tonal" color="warning" size="small" block @click="rejectPost(post)">
                    降级
                  </v-btn>
                </div>
              </div>

              <!-- 内容区 -->
              <div class="flex-grow-1 content-area">
                <!-- 标题行 -->
                <div class="d-flex align-center justify-space-between mb-2">
                  <div class="d-flex align-center">
                    <div class="text-body-1 font-weight-medium text-grey-darken-3">
                      文章 #{{ post.id }}
                    </div>
                    <v-chip variant="flat" :color="getStateColor(post.state)" size="x-small" class="ml-2">
                      {{ getStateText(post.state) }}
                    </v-chip>
                  </div>
                  <div class="d-flex align-center text-caption text-grey-darken-1">
                    <span>用户 #{{ post.creatorId }}</span>
                    <span class="mx-1">·</span>
                    <span>{{ post.createdAt }}</span>
                    <span class="mx-1">·</span>
                    <span>ID: {{ post.id }}</span>
                  </div>
                </div>

                <!-- 内容 -->
                <div class="content-wrapper">
                  <div
                    v-if="post.type === PostType.ARTICLE"
                    class="tiptap post-content"
                    v-html="post.content"
                  ></div>
                  <div v-if="post.type === PostType.INDEX">
                    <div class="text-caption text-grey-darken-1 mb-2">目录</div>
                    <div class="contents-list">
                      <div
                        v-for="(item, index) in parseContents(post.content)"
                        :key="index"
                        class="content-item mb-2"
                      >
                        <div class="d-flex align-start">
                          <div class="text-body-2 font-weight-medium text-grey-darken-3 mr-2">
                            {{ index + 1 }}.
                          </div>
                          <div class="flex-grow-1">
                            <div class="text-body-2 font-weight-medium text-grey-darken-3">
                              {{ item.name }}
                            </div>
                            <div v-if="item.description" class="text-caption text-grey-darken-1 mt-1">
                              {{ item.description }}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 拒绝/封禁原因 -->
                  <div v-if="(post.state === ContentState.REJECTED || post.state === ContentState.BANNED) && post.reason" class="mt-2">
                    <span class="text-caption text-red-darken-2">{{ post.state === ContentState.BANNED ? '封禁' : '拒绝' }}原因：{{ post.reason }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多指示器 -->
        <div v-if="loading && postList.length > 0" class="text-center py-4">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 没有更多 -->
        <div v-if="!hasMore && postList.length > 0" class="text-center py-4 text-caption text-grey">
          没有更多了
        </div>
      </v-card-text>
    </v-card>

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
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.list-item {
  padding: 16px;
  border-radius: 8px;
  background-color: #fafafa;
}

.action-area {
  width: 70px;
  flex-shrink: 0;
}

.content-area {
  min-width: 0;
  overflow: hidden;
}

.content-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  background-color: white;
}

.tiptap.post-content {
  max-height: 200px;
  overflow-y: auto;
  overflow-x: auto;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.tiptap.post-content :deep(*) {
  max-width: 100%;
}

.tiptap.post-content :deep(pre) {
  overflow-x: auto;
  white-space: pre;
  word-wrap: normal;
}

.tiptap.post-content :deep(table) {
  display: block;
  overflow-x: auto;
  max-width: 100%;
}

.tiptap.post-content :deep(img) {
  max-width: 100%;
  height: auto;
}
</style>
