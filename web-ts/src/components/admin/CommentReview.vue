<script setup lang="ts">
  import { inject, ref, computed, onMounted } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { commentServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { ContentState } from '@/types/enums'
  import type { Comment } from '@/types/comment'

  const { t } = useI18n()
  const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

  const allCommentList = ref<Comment[]>([])
  const currentTab = ref<string>('pending')
  const offsetId = ref<number>(0)
  const loading = ref<boolean>(false)
  const hasMore = ref<boolean>(true)

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
      label: t('admin.pending'),
      state: ContentState.SUBMITTED,
      icon: 'mdi-clock-outline',
      color: 'orange'
    },
    {
      key: 'approved',
      label: t('admin.approved'),
      state: ContentState.APPROVED,
      icon: 'mdi-check-circle',
      color: 'green'
    },
    {
      key: 'rejected',
      label: t('admin.rejected'),
      state: ContentState.REJECTED,
      icon: 'mdi-close-circle',
      color: 'red'
    }
  ]

  const commentList = computed<Comment[]>(() => {
    return allCommentList.value
  })

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
      await getCommentsByTab(currentTab.value, true)
    }
  }

  const approveComment = async (comment: Comment, approve: boolean): Promise<void> => {
    try {
      const response = await commentServiceV1.approveComment(comment.id, approve)

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

  const handleTabChange = async (newTab: string) => {
    hasMore.value = true
    await getCommentsByTab(newTab)
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
                v-if="comment.state === ContentState.APPROVED"
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
            </div>
            <div v-if="comment.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="approveComment(comment, true)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.approve') }}
              </v-btn>
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="approveComment(comment, false)"
              >
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.reject') }}
              </v-btn>
            </div>

            <!-- 已通过状态下显示屏蔽按钮 -->
            <div v-if="comment.state === ContentState.APPROVED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="approveComment(comment, false)"
              >
                <v-icon icon="mdi-block-helper" color="red-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已拒绝状态下显示通过按钮 -->
            <div v-if="comment.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="approveComment(comment, true)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.approve') }}
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