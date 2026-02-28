<script setup lang="ts">
import { inject, ref } from 'vue'
import { adminApi } from '@/api'
import { ContentState, ObjectType } from '@/enums'
import type { Comment } from '@/types/comment.d'
import RejectBanDialog from './RejectBanDialog.vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

const currentTab = ref<string>('pending')

// 筛选条件
const filterObjectType = ref<number | undefined>(undefined)
const filterObjectId = ref<number | undefined>(undefined)
const filterCreatorId = ref<number | undefined>(undefined)
const isFilterMode = ref<boolean>(false)

// 对象类型选项
const objectTypeOptions = [
  { title: '帖子', value: ObjectType.POST },
  { title: '节点', value: ObjectType.NODE },
  { title: '课程', value: ObjectType.COURSE },
]

// 获取对象类型名称
const getObjectTypeName = (type?: number): string => {
  const option = objectTypeOptions.find((opt) => opt.value === type)
  return option ? option.title : ''
}

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const currentComment = ref<Comment | null>(null)
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
    label: '已通过',
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
    label: '已封禁',
    state: ContentState.BANNED,
    icon: 'mdi-cancel',
    color: 'grey',
  },
]

// 使用 useInfiniteScroll 加载评论列表
const {
  items: commentList,
  loading,
  hasMore,
  loadMore,
  reset: resetCommentList,
} = useInfiniteScroll({
  fetchFn: (params) => {
    const currentTabConfig = tabs.find((tab) => tab.key === currentTab.value)
    const state = currentTabConfig?.state

    if (isFilterMode.value) {
      return adminApi.getCommentsByFilter(
        filterObjectType.value,
        filterObjectId.value,
        filterCreatorId.value,
        params.lastId,
        state
      )
    } else {
      return adminApi.getContentsByState('comment', state, params.lastId)
    }
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: {},
  immediate: true,
})

// 应用筛选
const applyFilter = (): void => {
  if (!filterObjectType.value && !filterObjectId.value && !filterCreatorId.value) {
    showSnackbar?.('请至少输入一个筛选条件', 'warning')
    return
  }
  isFilterMode.value = true
  resetCommentList()
  loadMore()
}

// 清除筛选
const clearFilter = (): void => {
  filterObjectType.value = undefined
  filterObjectId.value = undefined
  filterCreatorId.value = undefined
  isFilterMode.value = false
  resetCommentList()
  loadMore()
}

// 使用 useMutation 批准评论
const { execute: executeApproveComment } = useMutation(
  (commentId: number) => adminApi.operateContent('comment', commentId, { action: 'approve' }),
  {
    successMessage: '操作成功',
    onSuccess: (_, commentId) => {
      const index = commentList.value.findIndex((c) => c.id === commentId)
      if (index !== -1) {
        commentList.value.splice(index, 1)
      }
    },
  }
)

const approveComment = async (comment: Comment): Promise<void> => {
  await executeApproveComment(comment.id)
}

// 显示拒绝对话框
const showRejectDialog = (comment: Comment) => {
  currentComment.value = comment
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 显示屏蔽对话框
const showBanDialog = (comment: Comment) => {
  currentComment.value = comment
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (data: { commentId: number; action: string; reason: string }) =>
    adminApi.operateContent('comment', data.commentId, { action: data.action.toLowerCase(), reason: data.reason }),
  {
    onSuccess: (_, data) => {
      const message = data.action === 'BAN' ? '已屏蔽' : '已拒绝'
      showSnackbar?.(message, 'success')

      const index = commentList.value.findIndex((c) => c.id === data.commentId)
      if (index !== -1) {
        commentList.value.splice(index, 1)
      }

      showReasonDialog.value = false
      currentComment.value = null
    },
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string) => {
  if (!currentComment.value) return

  const action = dialogType.value === 'reject' ? 'REJECT' : 'BAN'
  await executeRejectOrBan({
    commentId: currentComment.value.id,
    action,
    reason,
  })
}

// 拒绝评论
const rejectComment = async (comment: Comment): Promise<void> => {
  showRejectDialog(comment)
}

// 屏蔽评论
const banComment = async (comment: Comment): Promise<void> => {
  showBanDialog(comment)
}

// 使用 useMutation 取消屏蔽评论
const { execute: executeUnbanComment } = useMutation(
  (commentId: number) => adminApi.operateContent('comment', commentId, { action: 'approve' }),
  {
    successMessage: '已取消屏蔽',
    onSuccess: (_, commentId) => {
      const index = commentList.value.findIndex((c) => c.id === commentId)
      if (index !== -1) {
        commentList.value.splice(index, 1)
      }
    },
  }
)

const unbanComment = async (comment: Comment): Promise<void> => {
  await executeUnbanComment(comment.id)
}

const handleTabChange = () => {
  resetCommentList()
  loadMore()
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
    <h2 class="text-h5 font-weight-bold mb-4">评论审核</h2>

    <!-- 筛选与状态 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-filter-variant" size="18" class="mr-2"></v-icon>
        筛选与状态
      </v-card-title>
      <v-card-text>
        <!-- 筛选条件 -->
        <div v-if="!isFilterMode" class="d-flex align-center ga-3 mb-4 mt-2">
          <v-select
            v-model="filterObjectType"
            :items="objectTypeOptions"
            label="对象类型"
            variant="outlined"
            density="compact"
            hide-details
            clearable
            style="max-width: 140px"
          ></v-select>
          <v-text-field
            v-model.number="filterObjectId"
            type="number"
            label="对象 ID"
            variant="outlined"
            density="compact"
            hide-details
            clearable
            style="max-width: 140px"
          ></v-text-field>
          <v-text-field
            v-model.number="filterCreatorId"
            type="number"
            label="用户 ID"
            variant="outlined"
            density="compact"
            hide-details
            clearable
            style="max-width: 140px"
          ></v-text-field>
          <v-btn variant="tonal" size="default" @click="applyFilter">
            <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
            筛选
          </v-btn>
        </div>

        <!-- 筛选结果提示 -->
        <v-alert
          v-if="isFilterMode"
          type="info"
          color="teal"
          variant="outlined"
          class="mb-4"
          border="top"
          rounded="lg"
          closable
          @click:close="clearFilter"
        >
          <div class="d-flex align-center">
            <span class="font-weight-medium">筛选条件：</span>
            <v-chip v-if="filterObjectType" size="small" class="mx-1">对象类型: {{ getObjectTypeName(filterObjectType) }}</v-chip>
            <v-chip v-if="filterObjectId" size="small" class="mx-1">对象 ID: {{ filterObjectId }}</v-chip>
            <v-chip v-if="filterCreatorId" size="small" class="mx-1">用户 ID: {{ filterCreatorId }}</v-chip>
          </div>
        </v-alert>

        <!-- 状态标签 -->
        <v-tabs
          v-model="currentTab"
          color="primary"
          show-arrows
          @update:model-value="handleTabChange"
        >
          <v-tab v-for="tab in tabs" :key="tab.key" :value="tab.key" class="text-none">
            <v-icon :icon="tab.icon" size="16" class="mr-2"></v-icon>
            {{ tab.label }}
          </v-tab>
        </v-tabs>
      </v-card-text>
    </v-card>

    <!-- 评论列表 -->
    <v-card flat class="border">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-comment-multiple" size="18" class="mr-2"></v-icon>
        评论列表
      </v-card-title>
      <v-card-text>
        <!-- 空状态 -->
        <div v-if="!loading && commentList.length === 0" class="text-center py-12">
          <v-icon icon="mdi-comment-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">
            {{ currentTab === 'pending' ? '暂无待审核的评论' : `暂无${tabs.find((tab) => tab.key === currentTab)?.label}的评论` }}
          </p>
        </div>

        <!-- 列表 -->
        <div v-if="commentList.length > 0">
          <div
            v-for="comment in commentList"
            :key="comment.id"
            v-intersect="{
              handler: (isIntersecting: boolean) => {
                if (isIntersecting && comment === commentList[commentList.length - 1] && hasMore && !loading) {
                  loadMore()
                }
              },
            }"
            class="list-item mb-3"
          >
            <div class="d-flex align-start">
              <!-- 操作区 -->
              <div class="action-area mr-4">
                <v-chip variant="flat" :color="getStateColor(comment.state)" size="small" class="mb-4 d-flex justify-center">
                  {{ getStateText(comment.state) }}
                </v-chip>

                <!-- 待审核 -->
                <div v-if="comment.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block @click="approveComment(comment)">
                    批准
                  </v-btn>
                  <v-btn variant="tonal" color="error" size="small" block @click="rejectComment(comment)">
                    拒绝
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="banComment(comment)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已通过 -->
                <div v-if="comment.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="warning" size="small" block @click="rejectComment(comment)">
                    撤销通过
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="banComment(comment)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已拒绝 -->
                <div v-if="comment.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block @click="approveComment(comment)">
                    通过
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="banComment(comment)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已屏蔽 -->
                <div v-if="comment.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="info" size="small" block @click="unbanComment(comment)">
                    取消屏蔽
                  </v-btn>
                  <v-btn variant="tonal" color="warning" size="small" block @click="rejectComment(comment)">
                    降级为拒绝
                  </v-btn>
                </div>
              </div>

              <!-- 内容区 -->
              <div class="flex-grow-1">
                <!-- 标题行 -->
                <div class="d-flex align-center justify-space-between mb-2">
                  <div class="text-body-1 font-weight-medium text-grey-darken-3">
                    评论 ID: {{ comment.id }}
                  </div>
                  <v-btn
                    variant="outlined"
                    color="teal"
                    size="small"
                    rounded="lg"
                    :href="`/read?commentId=${comment.id}`"
                    target="_blank"
                  >
                    <v-icon icon="mdi-open-in-new" size="14" class="mr-1"></v-icon>
                    查看原文
                  </v-btn>
                </div>

                <!-- 元信息 -->
                <div class="d-flex align-center mb-2 text-caption text-grey-darken-1">
                  <v-icon icon="mdi-account-outline" size="14" class="mr-1"></v-icon>
                  <span>用户 #{{ comment.creatorId }}</span>
                  <span class="ml-2">{{ comment.createdAt }}</span>
                </div>

                <!-- 内容 -->
                <div class="content-wrapper">
                  <div class="text-body-2 text-grey-darken-2">
                    {{ comment.content }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载指示器 -->
        <div v-if="loading" class="text-center py-4">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 没有更多 -->
        <div v-if="!hasMore && commentList.length > 0" class="text-center py-4 text-caption text-grey">
          没有更多了
        </div>
      </v-card-text>
    </v-card>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="`评论ID: ${currentComment?.id || ''}`"
      :item-state="currentComment?.state"
      item-type="评论"
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

.content-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  background-color: white;
}
</style>
