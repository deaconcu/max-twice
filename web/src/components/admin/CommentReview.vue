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
      // 使用统一接口
      return adminApi.getContentsByState('comment', state, params.lastId)
    }
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: {},
  immediate: true, // 自动初始加载
})

// 应用筛选
const applyFilter = (): void => {
  if (!filterObjectType.value && !filterObjectId.value && !filterCreatorId.value) {
    showSnackbar?.('请至少输入一个筛选条件', 'warning')
    return
  }
  isFilterMode.value = true
  resetCommentList()
  loadMore() // 重新加载数据
}

// 清除筛选
const clearFilter = (): void => {
  filterObjectType.value = undefined
  filterObjectId.value = undefined
  filterCreatorId.value = undefined
  isFilterMode.value = false
  resetCommentList()
  loadMore() // 重新加载数据
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
    adminApi.operateContent('comment', data.commentId, {
      action: data.action.toLowerCase(),
      reason: data.reason,
    }),
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

// 拒绝评论（已弃用，保留兼容）
const rejectComment = async (comment: Comment): Promise<void> => {
  showRejectDialog(comment)
}

// 屏蔽评论（已弃用，保留兼容）
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

const handleTabChange = (newTab: string) => {
  resetCommentList()
  loadMore() // 重新加载数据
}
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-comment-check-outline" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">评论审核</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">审核用户评论</p>
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <v-card v-if="!isFilterMode" flat class="pa-4 bg-grey-lighten-5 rounded-lg mb-6">
      <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
        <v-icon icon="mdi-filter-outline" size="16" class="mr-2"></v-icon>
        高级筛选
      </h4>
      <v-row dense>
        <v-col cols="12" sm="4">
          <v-select
            v-model="filterObjectType"
            :items="objectTypeOptions"
            label="对象类型"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          >
            <template #prepend-inner>
              <v-icon icon="mdi-shape-outline" size="16" color="grey-darken-1"></v-icon>
            </template>
          </v-select>
        </v-col>
        <v-col cols="12" sm="3">
          <v-text-field
            v-model.number="filterObjectId"
            type="number"
            label="对象 ID"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="3">
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
          <v-btn variant="flat" color="primary" rounded="lg" block @click="applyFilter">
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
          <v-chip v-if="filterObjectType" size="small" class="mx-1"
            >对象类型: {{ getObjectTypeName(filterObjectType) }}</v-chip
          >
          <v-chip v-if="filterObjectId" size="small" class="mx-1"
            >对象 ID: {{ filterObjectId }}</v-chip
          >
          <v-chip v-if="filterCreatorId" size="small" class="mx-1"
            >用户 ID: {{ filterCreatorId }}</v-chip
          >
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
      <v-tab v-for="tab in tabs" :key="tab.key" :value="tab.key" class="text-none">
        <v-icon :icon="tab.icon" :color="`${tab.color}-darken-1`" size="18" class="mr-2"></v-icon>
        {{ tab.label }}
      </v-tab>
    </v-tabs>

    <div v-if="commentList.length === 0 && !loading" class="text-center py-12">
      <v-icon icon="mdi-comment-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">
        {{
          currentTab === 'pending'
            ? '暂无待审核的评论'
            : `暂无${tabs.find((tab) => tab.key === currentTab)?.label}的评论`
        }}
      </p>
    </div>

    <div
      v-for="comment in commentList"
      :key="comment.id"
      v-intersect="{
        handler: (isIntersecting: boolean) => {
          if (
            isIntersecting &&
            comment === commentList[commentList.length - 1] &&
            hasMore &&
            !loading
          ) {
            loadMore()
          }
        },
      }"
      class="mb-4"
    >
      <v-card flat class="border rounded-lg pa-5" hover>
        <div class="d-flex align-start">
          <!-- 状态和操作区域 -->
          <div class="mr-4 status-actions-area">
            <div class="mb-3">
              <v-chip
                v-if="comment.state === ContentState.SUBMITTED"
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                待审核
              </v-chip>
              <v-chip
                v-if="comment.state === ContentState.PUBLISHED"
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                已通过
              </v-chip>
              <v-chip
                v-if="comment.state === ContentState.REJECTED"
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                已拒绝
              </v-chip>
              <v-chip
                v-if="comment.state === ContentState.BANNED"
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-cancel" size="14" class="mr-1"></v-icon>
                已封禁
              </v-chip>
            </div>
            <!-- 待审核状态：批准、拒绝、屏蔽 -->
            <div v-if="comment.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="approveComment(comment)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                批准
              </v-btn>
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="rejectComment(comment)"
              >
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                拒绝
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                @click="banComment(comment)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已通过状态：撤销通过、屏蔽 -->
            <div v-if="comment.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                @click="rejectComment(comment)"
              >
                <v-icon icon="mdi-undo" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                撤销通过
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                @click="banComment(comment)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已拒绝状态：通过、屏蔽 -->
            <div v-if="comment.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="approveComment(comment)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                通过
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                @click="banComment(comment)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已屏蔽状态：取消屏蔽、降级为拒绝 -->
            <div v-if="comment.state === ContentState.BANNED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="blue-lighten-4"
                rounded="lg"
                size="small"
                @click="unbanComment(comment)"
              >
                <v-icon icon="mdi-lock-open" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                取消屏蔽
              </v-btn>
              <v-btn
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                @click="rejectComment(comment)"
              >
                <v-icon
                  icon="mdi-arrow-down"
                  color="orange-darken-2"
                  size="16"
                  class="mr-1"
                ></v-icon>
                降级为拒绝
              </v-btn>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center justify-space-between mb-3">
              <div class="d-flex align-center">
                <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                  <v-icon icon="mdi-account" color="grey-darken-1" size="18"></v-icon>
                </v-avatar>
                <div>
                  <div class="text-body-2 font-weight-medium text-grey-darken-2">
                    评论 ID: {{ comment.id }}
                  </div>
                  <div class="text-caption text-grey-darken-1">{{ comment.createdAt }}</div>
                </div>
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

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <div class="text-caption text-grey-darken-1 mb-2">评论内容</div>
              <div class="text-body-1 text-grey-darken-2 line-height-relaxed">
                {{ comment.content }}
              </div>
            </div>
          </div>
        </div>
      </v-card>
    </div>

    <!-- 加载更多指示器 -->
    <div v-if="loading" class="text-center py-4">
      <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
      <span class="ml-2 text-grey-darken-1">加载中...</span>
    </div>

    <!-- 没有更多数据提示 -->
    <div v-if="!hasMore && commentList.length > 0" class="text-center py-4">
      <span class="text-grey-darken-1">没有更多数据了</span>
    </div>

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
.comment-content {
  max-height: 150px;
  overflow-y: auto;
}

.status-actions-area {
  min-width: 200px;
}
</style>
