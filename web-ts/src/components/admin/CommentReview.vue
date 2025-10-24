<script setup lang="ts">
  import { inject, ref, computed, onMounted } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { commentServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { ContentState, ApprovalAction, ObjectType } from '@/types/enums'
  import type { Comment } from '@/types/comment'
  import RejectBanDialog from './RejectBanDialog.vue'

  const { t } = useI18n()
  const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

  const allCommentList = ref<Comment[]>([])
  const currentTab = ref<string>('pending')
  const offsetId = ref<number>(0)
  const loading = ref<boolean>(false)
  const hasMore = ref<boolean>(true)

  // 筛选条件
  const filterObjectType = ref<number | undefined>(undefined)
  const filterObjectId = ref<number | undefined>(undefined)
  const filterCreatorId = ref<number | undefined>(undefined)
  const isFilterMode = ref<boolean>(false)

  // 对象类型选项
  const objectTypeOptions = [
    { title: '帖子', value: ObjectType.POST },
    { title: '节点', value: ObjectType.NODE },
    { title: '课程', value: ObjectType.COURSE }
  ]

  // 获取对象类型名称
  const getObjectTypeName = (type?: number): string => {
    const option = objectTypeOptions.find(opt => opt.value === type)
    return option ? option.title : ''
  }

  // 拒绝/屏蔽对话框
  const showReasonDialog = ref<boolean>(false)
  const currentComment = ref<Comment | null>(null)
  const submitting = ref<boolean>(false)
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
      label: '已通过',
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
      label: '已封禁',
      state: ContentState.BANNED,
      icon: 'mdi-cancel',
      color: 'grey'
    }
  ]

  const commentList = computed<Comment[]>(() => {
    return allCommentList.value
  })

  // 按筛选条件获取评论
  const getCommentsByFilter = async (isLoadMore: boolean = false): Promise<void> => {
    if (loading.value) return

    loading.value = true

    try {
      const lastId = isLoadMore && allCommentList.value.length > 0
        ? allCommentList.value[allCommentList.value.length - 1].id
        : undefined

      // 获取当前tab的状态
      const currentTabConfig = tabs.find(tab => tab.key === currentTab.value)
      const state = currentTabConfig?.state

      const response = await commentServiceV1.getCommentsByFilter(
        filterObjectType.value,
        filterObjectId.value,
        filterCreatorId.value,
        lastId,
        state
      )

      if (response.code === 401) {
        // not login
      } else if (response.code === 200) {
        const newComments = response.data

        if (isLoadMore) {
          allCommentList.value.push(...newComments)
        } else {
          allCommentList.value = newComments
        }

        hasMore.value = newComments.length > 0
        if (newComments.length > 0) {
          offsetId.value = newComments[newComments.length - 1].id
        }
      } else {
        showSnackbar && showSnackbar(response.message || 'error.loadFailed', 'error')
      }
    } catch (error) {
      console.error('Error loading comments:', error)
      showSnackbar && showSnackbar('error.loadFailed', 'error')
    } finally {
      loading.value = false
    }
  }

  // 应用筛选
  const applyFilter = async (): Promise<void> => {
    if (!filterObjectType.value && !filterObjectId.value && !filterCreatorId.value) {
      showSnackbar && showSnackbar('请至少输入一个筛选条件', 'warning')
      return
    }
    isFilterMode.value = true
    hasMore.value = true
    await getCommentsByFilter()
  }

  // 清除筛选
  const clearFilter = async (): Promise<void> => {
    filterObjectType.value = undefined
    filterObjectId.value = undefined
    filterCreatorId.value = undefined
    isFilterMode.value = false
    hasMore.value = true
    await getCommentsByTab(currentTab.value)
  }

  const getCommentsByTab = async (tabKey: string, isLoadMore: boolean = false): Promise<void> => {
    if (loading.value) return

    loading.value = true

    try {
      const lastId = isLoadMore && allCommentList.value.length > 0
        ? allCommentList.value[allCommentList.value.length - 1].id
        : 0

      const response = await commentServiceV1.getCommentsByState(tabKey, lastId)

      if (response.code === 401) {
        // not login
      } else if (response.code === 200) {
        const newComments = response.data

        if (isLoadMore) {
          allCommentList.value.push(...newComments)
        } else {
          allCommentList.value = newComments
        }

        hasMore.value = newComments.length > 0
        if (newComments.length > 0) {
          offsetId.value = newComments[newComments.length - 1].id
        }
      } else {
        showSnackbar && showSnackbar(response.message || 'error.loadFailed', 'error')
      }
    } catch (error) {
      console.error('Error loading comments:', error)
      showSnackbar && showSnackbar('error.loadFailed', 'error')
    } finally {
      loading.value = false
    }
  }

  const loadMore = async (): Promise<void> => {
    if (hasMore.value && !loading.value) {
      if (isFilterMode.value) {
        await getCommentsByFilter(true)
      } else {
        await getCommentsByTab(currentTab.value, true)
      }
    }
  }

  const approveComment = async (comment: Comment): Promise<void> => {
    try {
      const response = await commentServiceV1.approveComment(comment.id, ApprovalAction.APPROVE)

      if (response.code === 401) {
        // not login
      } else if (response.code === 200) {
        // 审核操作后重新加载当前tab的数据
        await getCommentsByTab(currentTab.value)
        showSnackbar && showSnackbar(t('admin.operationSuccess'))
      } else {
        showSnackbar && showSnackbar(response.message || 'error.operationFailed', 'error')
      }
    } catch (error) {
      console.error('Error approving comment:', error)
      showSnackbar && showSnackbar('error.operationFailed', 'error')
    }
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

  // 处理对话框确认
  const handleConfirmAction = async (reason: string) => {
    if (!currentComment.value) return

    try {
      submitting.value = true
      const action = dialogType.value === 'reject' ? ApprovalAction.REJECT : ApprovalAction.BAN

      const response = await commentServiceV1.approveComment(currentComment.value.id, action, reason)

      if (response.code === 200) {
        await getCommentsByTab(currentTab.value)

        const message = dialogType.value === 'reject' ? '已拒绝' : '已屏蔽'
        showSnackbar && showSnackbar(message, 'success')

        showReasonDialog.value = false
        currentComment.value = null
      } else {
        showSnackbar && showSnackbar(response.message || '操作失败', 'error')
      }
    } catch (error) {
      console.error('Error updating comment:', error)
      showSnackbar && showSnackbar('操作失败', 'error')
    } finally {
      submitting.value = false
    }
  }

  // 拒绝评论（已弃用，保留兼容）
  const rejectComment = async (comment: Comment): Promise<void> => {
    showRejectDialog(comment)
  }

  // 屏蔽评论（已弃用，保留兼容）
  const banComment = async (comment: Comment): Promise<void> => {
    showBanDialog(comment)
  }

  const unbanComment = async (comment: Comment): Promise<void> => {
    try {
      const response = await commentServiceV1.approveComment(comment.id, ApprovalAction.APPROVE)

      if (response.code === 401) {
        // not login
      } else if (response.code === 200) {
        await getCommentsByTab(currentTab.value)
        showSnackbar && showSnackbar('已取消屏蔽')
      } else {
        showSnackbar && showSnackbar(response.message || '操作失败', 'error')
      }
    } catch (error) {
      console.error('Error unbanning comment:', error)
      showSnackbar && showSnackbar('操作失败', 'error')
    }
  }

  const handleTabChange = async (newTab: string) => {
    hasMore.value = true
    if (isFilterMode.value) {
      // 如果在筛选模式下，切换tab时重新应用筛选（保留筛选条件和状态）
      await getCommentsByFilter()
    } else {
      await getCommentsByTab(newTab)
    }
  }

  onMounted(() => {
    getCommentsByTab(currentTab.value)
  })
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-comment-check-outline" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">
            {{ t('admin.commentReview') }}
          </h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">{{ t('admin.reviewUserComments') }}</p>
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
          <v-chip v-if="filterObjectType" size="small" class="mx-1">对象类型: {{ getObjectTypeName(filterObjectType) }}</v-chip>
          <v-chip v-if="filterObjectId" size="small" class="mx-1">对象 ID: {{ filterObjectId }}</v-chip>
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

    <div v-if="commentList.length === 0 && !loading" class="text-center py-12">
      <v-icon icon="mdi-comment-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">
        {{ currentTab === 'pending' ? t('admin.noCommentsToReview') : `暂无${tabs.find(tab => tab.key === currentTab)?.label}的评论` }}
      </p>
    </div>

    <div
      v-for="comment in commentList"
      :key="comment.id"
      class="mb-4"
      v-intersect="{
        handler: (isIntersecting) => {
          if (isIntersecting && comment === commentList[commentList.length - 1] && hasMore && !loading) {
            loadMore()
          }
        }
      }"
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
                {{ t('admin.pending') }}
              </v-chip>
              <v-chip
                v-if="comment.state === ContentState.PUBLISHED"
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.approved') }}
              </v-chip>
              <v-chip
                v-if="comment.state === ContentState.REJECTED"
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.rejected') }}
              </v-chip>
              <v-chip
                v-if="comment.state === ContentState.BANNED"
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
                <v-icon icon="mdi-arrow-down" color="orange-darken-2" size="16" class="mr-1"></v-icon>
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
                    {{ t('admin.commentId') }}: {{ comment.id }}
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
                {{ t('admin.viewOriginal') }}
              </v-btn>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <div class="text-caption text-grey-darken-1 mb-2">
                {{ t('admin.commentContent') }}
              </div>
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
      <v-progress-circular
        indeterminate
        color="primary"
        size="24"
      ></v-progress-circular>
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