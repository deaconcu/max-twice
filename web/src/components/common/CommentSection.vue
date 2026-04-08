<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { commentApi } from '@/api'
import { upvoteApi } from '@/api'
import { useFetch, useMutation } from '@/composables'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { useI18n } from '@/composables/useI18n'
import { ObjectType, VoteType } from '@/enums'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useUserStore } from '@/stores/modules/user'

const props = withDefaults(defineProps<Props>(), {
  postId: 0,
  commentCount: 0,
  objectType: ObjectType.POST,
  targetCommentId: null,
  targetSubCommentId: null,
})

const emit = defineEmits<{
  viewAllComments: []
}>()

const { t } = useI18n()

interface Props {
  postId?: number
  commentCount?: number
  objectType?: number
  targetCommentId?: number | null
  targetSubCommentId?: number | null
}

// 验证规则
const commentRules = useValidationRules('comment-content')
const commentMaxLength = useMaxLength('comment-content')

// 用户信息
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userId)

// 判断是否是自己的评论
const isOwnComment = (comment: any) => {
  return currentUserId.value && comment.creatorId === currentUserId.value
}

const newComment = ref('')
const isCommentFocused = ref(false)
const isReplyFocused = ref(false)
const activeReplyId = ref<number | null>(null) // 控制输入框显示位置（主评论ID或子评论ID）
const parentCommentId = ref<number | null>(null) // API提交时使用的父评论ID
const replyContent = ref('')
const replyToUserId = ref<number | null>(null)
const replyToUserName = ref<string>('')
const loadMoreTrigger = ref<HTMLElement | null>(null)

// 评论列表状态
const comments = ref<any[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const hasMoreBefore = ref(false)
const lastScore = ref<number | undefined>(undefined)
const lastId = ref<number | undefined>(undefined)
const firstScore = ref<number | undefined>(undefined)
const firstId = ref<number | undefined>(undefined)
const highlightedCommentId = ref<number | null>(null)
const highlightedSubCommentId = ref<number | null>(null)

// 使用 useFetch 加载主评论上下文
const { execute: fetchCommentContext } = useFetch({
  fetchFn: () => commentApi.getCommentContext(props.targetCommentId!),
  immediate: false,
  onSuccess: async (result) => {
    if (result?.items && result.items.length > 0) {
      comments.value = result.items
      // 初始化每个评论的 hasMoreReplies 标志
      comments.value.forEach((comment) => {
        const childrenCount = comment.children?.length || 0
        comment.hasMoreReplies = comment.replyCount > childrenCount
      })
      hasMore.value = result.hasMoreAfter
      hasMoreBefore.value = result.hasMoreBefore
      if (result.lastScore !== undefined && result.lastId !== undefined) {
        lastScore.value = result.lastScore
        lastId.value = result.lastId
      }
      if (result.firstScore !== undefined && result.firstId !== undefined) {
        firstScore.value = result.firstScore
        firstId.value = result.firstId
      }

      // 如果有子评论ID，加载子评论上下文（不高亮父评论）
      if (props.targetSubCommentId) {
        await loadSubCommentContext()
      } else {
        // 设置高亮（仅主评论定位时）
        highlightedCommentId.value = props.targetCommentId ?? null

        // 使用 MutationObserver 监听目标主评论元素出现
        const observer = new MutationObserver(() => {
          const commentEl = document.querySelector(`[data-comment-id="${props.targetCommentId}"]`)
          if (commentEl) {
            commentEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
            observer.disconnect()
          }
        })
        observer.observe(document.body, { childList: true, subtree: true })

        // 5秒后移除高亮
        setTimeout(() => {
          highlightedCommentId.value = null
        }, 5000)
      }
    } else {
      hasMore.value = false
    }
    loading.value = false

    // 设置无限滚动
    await nextTick()
    setupInfiniteScroll()
  },
  onError: () => {
    loading.value = false
  },
})

// 使用 useFetch 加载子评论上下文
const { execute: fetchSubCommentContext } = useFetch({
  fetchFn: () => commentApi.getCommentContext(props.targetSubCommentId!),
  immediate: false,
  onSuccess: async (result) => {
    if (result?.subItems && result.subItems.length > 0 && result.parentCommentId) {
      // 找到父评论，替换其 children
      const parentComment = comments.value.find((c) => c.id === result.parentCommentId)
      if (parentComment) {
        parentComment.children = result.subItems
        // 更新 hasMoreReplies 标志
        parentComment.hasMoreReplies = result.hasMoreAfter
        parentComment.hasMoreRepliesBefore = result.hasMoreBefore
      }

      // 设置子评论高亮
      highlightedSubCommentId.value = props.targetSubCommentId ?? null

      // 使用 MutationObserver 监听目标子评论元素出现
      const observer = new MutationObserver(() => {
        const subCommentEl = document.querySelector(
          `[data-sub-comment-id="${props.targetSubCommentId}"]`
        )
        if (subCommentEl) {
          subCommentEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
          observer.disconnect()
        }
      })
      observer.observe(document.body, { childList: true, subtree: true })

      // 5秒后移除高亮
      setTimeout(() => {
        highlightedCommentId.value = null
        highlightedSubCommentId.value = null
      }, 5000)
    }
  },
})

// 加载子评论上下文
const loadSubCommentContext = async () => {
  await fetchSubCommentContext()
}

// 使用 useFetch 加载初始评论列表
const { execute: fetchInitialComments } = useFetch({
  fetchFn: () => commentApi.getComments(props.postId, props.objectType),
  immediate: false,
  onSuccess: async (result) => {
    if (result?.items && result.items.length > 0) {
      comments.value = result.items
      // 初始化每个评论的 hasMoreReplies 标志
      comments.value.forEach((comment) => {
        // 如果回复总数大于已加载的子评论数量，说明还有更多
        const childrenCount = comment.children?.length || 0
        comment.hasMoreReplies = comment.replyCount > childrenCount
      })
      if (result.nextCursor) {
        lastScore.value = result.nextCursor.lastScore
        lastId.value = result.nextCursor.lastId
      }
      hasMore.value = result.hasMore
    } else {
      hasMore.value = false
    }
    loading.value = false

    // 设置无限滚动
    await nextTick()
    setupInfiniteScroll()
  },
  onError: () => {
    loading.value = false
  },
})

// 使用 useMutation 加载更多评论
const { execute: fetchMoreComments } = useMutation(
  (params: { lastScore: number; lastId: number }) =>
    commentApi.getComments(props.postId, props.objectType, params.lastScore, params.lastId),
  {
    showToast: false,
  }
)

// 加载评论列表（初始加载或重新加载）
const loadComments = async (reset = false) => {
  if (reset) {
    comments.value = []
    lastScore.value = undefined
    lastId.value = undefined
    firstScore.value = undefined
    firstId.value = undefined
    hasMore.value = true
    hasMoreBefore.value = false
  }

  loading.value = true

  // 如果有目标评论，使用上下文加载
  if (props.targetCommentId) {
    await fetchCommentContext()
  } else {
    await fetchInitialComments()
  }
}

// 加载更多评论
const loadMore = async () => {
  if (
    loadingMore.value ||
    !hasMore.value ||
    lastScore.value === undefined ||
    lastId.value === undefined
  )
    return

  loadingMore.value = true
  const result = await fetchMoreComments({ lastScore: lastScore.value, lastId: lastId.value })

  if (result?.items && result.items.length > 0) {
    // 初始化每个新加载评论的 hasMoreReplies 标志
    result.items.forEach((comment: any) => {
      const childrenCount = comment.children?.length || 0
      comment.hasMoreReplies = comment.replyCount > childrenCount
    })
    comments.value = [...comments.value, ...result.items]
    if (result.nextCursor) {
      lastScore.value = result.nextCursor.lastScore
      lastId.value = result.nextCursor.lastId
    }
    hasMore.value = result.hasMore
  } else {
    hasMore.value = false
  }
  loadingMore.value = false
}

// 加载子评论（点击按钮）
const { execute: executeLoadSubComments } = useMutation(
  (params: { commentId: number; lastScore: number; lastId: number }) =>
    commentApi.getCommentReplies(params.commentId, params.lastScore, params.lastId),
  {
    showToast: false,
  }
)

const loadSubComments = async (comment: any) => {
  if (!comment.children) {
    comment.children = []
  }

  // 如果有子评论，使用最后一个子评论的 score 和 id
  let lastScore: number | undefined
  let lastId: number | undefined
  if (comment.children.length > 0) {
    const lastChild = comment.children[comment.children.length - 1]
    lastScore = lastChild.score
    lastId = lastChild.id
  }

  // 首次加载不传参数，后续加载传 lastScore 和 lastId
  const result =
    lastScore !== undefined && lastId !== undefined
      ? await executeLoadSubComments({ commentId: comment.id, lastScore, lastId })
      : await commentApi.getCommentReplies(comment.id)

  if (result?.items && result.items.length > 0) {
    comment.children.push(...result.items)
  }

  // 更新是否还有更多子评论的标志
  comment.hasMoreReplies = result?.hasMore ?? false
}

// 使用 useMutation 发表评论
const { execute: submitComment, loading: submitting } = useMutation(
  () => commentApi.createComment(props.postId, props.objectType, null, null, newComment.value),
  {
    successMessage: t('comment.submitSuccess'),
    onSuccess: (result) => {
      newComment.value = ''
      // 直接插入新评论到列表顶部，不重新加载
      if (comments.value) {
        comments.value.unshift(result)
      } else {
        comments.value = [result]
      }
    },
  }
)

// 使用 useMutation 发表子评论
const { execute: submitReply, loading: replying } = useMutation(
  () => {
    // 使用parentCommentId作为API参数
    return commentApi.createComment(
      props.postId,
      props.objectType,
      parentCommentId.value,
      replyToUserId.value,
      replyContent.value
    )
  },
  {
    successMessage: t('comment.replySuccess'),
    onSuccess: (result) => {
      replyContent.value = ''
      // 使用parentCommentId找到父评论
      const parentComment = comments.value?.find((c) => c.id === parentCommentId.value)

      if (parentComment) {
        if (!parentComment.children) {
          parentComment.children = [result]
        } else {
          parentComment.children.unshift(result)
        }
      }
      activeReplyId.value = null
      parentCommentId.value = null
      replyToUserId.value = null
      replyToUserName.value = ''
    },
  }
)

// 发表评论
const handleSubmitComment = async () => {
  if (!newComment.value.trim()) return
  await submitComment()
}

// 发表回复
const handleSubmitReply = async (commentId: number) => {
  if (!replyContent.value.trim()) return
  await submitReply()
}

// 切换回复框 - 回复主评论
const toggleReply = (commentId: number) => {
  if (activeReplyId.value === commentId) {
    activeReplyId.value = null
    parentCommentId.value = null
    replyContent.value = ''
    replyToUserId.value = null
    replyToUserName.value = ''
  } else {
    activeReplyId.value = commentId
    parentCommentId.value = commentId // 回复主评论，父评论ID就是主评论ID
    replyContent.value = ''
    replyToUserId.value = null
    replyToUserName.value = ''
  }
}

// 回复子评论
const replyToSubComment = (parentCommentIdParam: number, subComment: any) => {
  activeReplyId.value = subComment.id // 用于控制输入框显示在子评论下方
  parentCommentId.value = parentCommentIdParam // 用于API提交，父评论ID
  replyToUserId.value = subComment.creatorId || null
  replyToUserName.value = subComment.creator?.name || t('comment.anonymousUser')
  replyContent.value = ''
}

// 取消回复
const cancelReply = () => {
  activeReplyId.value = null
  parentCommentId.value = null
  replyToUserId.value = null
  replyToUserName.value = ''
}

// 点赞评论
const upvotingCommentId = ref<number | null>(null)
let currentUpvoteComment: any = null

const { execute: doUpvote } = useMutation(
  () => upvoteApi.upvote(currentUpvoteComment.id, ObjectType.COMMENT, VoteType.LIKE),
  {
    onSuccess: (result) => {
      currentUpvoteComment.liked = result.liked
      currentUpvoteComment.likeCount = result.likeCount
      upvotingCommentId.value = null
    },
  }
)

const handleUpvoteComment = async (comment: any) => {
  if (upvotingCommentId.value === comment.id) return
  upvotingCommentId.value = comment.id
  currentUpvoteComment = comment
  await doUpvote()
}

// Intersection Observer 实例
let observer: IntersectionObserver | null = null

// 设置无限滚动
const setupInfiniteScroll = () => {
  if (!loadMoreTrigger.value) return

  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry?.isIntersecting && hasMore.value && !loadingMore.value) {
        void loadMore()
      }
    },
    {
      root: null,
      rootMargin: '100px',
      threshold: 0.1,
    }
  )

  observer.observe(loadMoreTrigger.value)
}

// 清理 Intersection Observer
const cleanupInfiniteScroll = () => {
  if (observer && loadMoreTrigger.value) {
    observer.unobserve(loadMoreTrigger.value)
    observer.disconnect()
    observer = null
  }
}

// 组件挂载时加载初始数据
onMounted(() => {
  void loadComments(true)
})

// 组件卸载时清理
onBeforeUnmount(() => {
  cleanupInfiniteScroll()
})
</script>

<template>
  <div class="comments-section">
    <!-- 评论输入 -->
    <div class="comment-input-section">
      <v-textarea
        v-model="newComment"
        :placeholder="t('comment.inputPlaceholder')"
        variant="outlined"
        density="comfortable"
        rounded="lg"
        :rows="isCommentFocused ? 3 : 1"
        :rules="isCommentFocused ? commentRules : []"
        :counter="isCommentFocused ? commentMaxLength : false"
        auto-grow
        hide-details="auto"
        class="mb-3"
        @focus="isCommentFocused = true"
        @blur="isCommentFocused = false"
      ></v-textarea>
      <v-btn
        color="primary"
        variant="tonal"
        density="comfortable"
        :disabled="!newComment.trim()"
        :loading="submitting"
        @mousedown.prevent="handleSubmitComment"
      >
        <v-icon icon="mdi-send" size="18" class="mr-1"></v-icon>
        {{ t('comment.postComment') }}
      </v-btn>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
    </div>

    <!-- 空状态 -->
    <div v-else-if="comments.length === 0" class="text-center py-8">
      <v-icon icon="mdi-comment-outline" size="48" color="grey-lighten-2" class="mb-2"></v-icon>
      <p class="text-body-2 text-grey">{{ t('comment.noCommentsHint') }}</p>
    </div>

    <!-- 评论列表 -->
    <div v-else class="comment-list">
      <!-- 查看全部评论提示 -->
      <div v-if="hasMoreBefore" class="view-all-hint mb-4">
        <div class="action-row">
          <v-chip
            color="primary"
            variant="tonal"
            size="small"
            prepend-icon="mdi-arrow-up"
            @click="$emit('viewAllComments')"
          >
            {{ t('comment.viewAllComments') }}
          </v-chip>
        </div>
        <div class="ellipsis-row">
          <v-icon icon="mdi-dots-horizontal" color="grey" size="24"></v-icon>
        </div>
      </div>

      <div
        v-for="comment in comments"
        :key="comment.id"
        :data-comment-id="comment.id"
        class="comment-item mb-2"
      >
        <div class="comment-content" :class="{ highlighted: highlightedCommentId === comment.id }">
          <div class="d-flex">
            <UserAvatar
              :name="comment.creator?.name || t('comment.anonymousUser')"
              :avatar-url="comment.creator?.avatar"
              size="30"
              rounded="lg"
              class="mr-3"
            />
            <div class="flex-grow-1">
              <div class="d-flex align-center mb-2">
                <span class="text-body-2 font-weight-medium text-grey-darken-3">
                  {{ comment.creator?.name || t('comment.anonymousUser') }}
                </span>
                <span class="text-caption text-grey mx-2">·</span>
                <span class="text-caption text-grey">
                  {{ comment.createdAt }}
                </span>
              </div>
              <p class="text-body-2 mb-2">
                {{ comment.content }}
              </p>

              <!-- 操作按钮 -->
              <div class="d-flex align-center mb-2">
                <v-btn
                  size="small"
                  variant="text"
                  :color="comment.liked ? 'primary' : 'grey-darken-2'"
                  :disabled="isOwnComment(comment)"
                  @click="handleUpvoteComment(comment)"
                >
                  <v-icon
                    :icon="comment.liked ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
                    size="16"
                    class="mr-1"
                  ></v-icon>
                  {{ comment.likeCount || 0 }}
                </v-btn>
                <v-btn
                  size="small"
                  variant="text"
                  color="grey-darken-2"
                  class="ml-2"
                  @click="toggleReply(comment.id)"
                >
                  <v-icon icon="mdi-comment-outline" size="16" class="mr-1"></v-icon>
                  {{ t('comment.reply') }}
                </v-btn>
              </div>

              <!-- 回复输入框 - 只在回复主评论时显示 -->
              <div v-if="activeReplyId === comment.id && replyToUserId === null" class="mt-2 mb-3">
                <v-textarea
                  v-model="replyContent"
                  :placeholder="t('comment.replyPlaceholder')"
                  variant="outlined"
                  density="comfortable"
                  rounded="lg"
                  rows="2"
                  :rules="isReplyFocused ? commentRules : []"
                  :counter="commentMaxLength"
                  auto-grow
                  hide-details="auto"
                  class="mb-2"
                  @focus="isReplyFocused = true"
                  @blur="isReplyFocused = false"
                ></v-textarea>
                <div class="d-flex justify-end">
                  <v-btn
                    size="small"
                    variant="text"
                    color="grey"
                    class="mr-2"
                    @click="toggleReply(comment.id)"
                  >
                    {{ t('common.cancel') }}
                  </v-btn>
                  <v-btn
                    size="small"
                    color="primary"
                    variant="tonal"
                    :disabled="!replyContent.trim()"
                    :loading="replying"
                    @mousedown.prevent="handleSubmitReply(comment.id)"
                  >
                    {{ t('comment.postReply') }}
                  </v-btn>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- comment-content 结束 -->

        <!-- 子评论列表 -->
        <div v-if="comment.children && comment.children.length > 0" class="sub-comments mt-3 ms-10">
          <!-- 子评论上方省略号 -->
          <div v-if="comment.hasMoreRepliesBefore" class="sub-comments-ellipsis mb-4">
            <v-icon icon="mdi-dots-horizontal" color="grey" size="20"></v-icon>
          </div>

          <div
            v-for="subComment in comment.children"
            :key="subComment.id"
            :data-sub-comment-id="subComment.id"
            class="sub-comment-item mb-2"
            :class="{ highlighted: highlightedSubCommentId === subComment.id }"
          >
            <div class="d-flex">
              <UserAvatar
                :name="subComment.creator?.name || t('comment.anonymousUser')"
                :avatar-url="subComment.creator?.avatar"
                size="24"
                rounded="lg"
                class="mr-2"
              />
              <div class="flex-grow-1">
                <div class="d-flex align-center mb-2">
                  <span class="text-body-2 font-weight-medium text-grey-darken-3">
                    {{ subComment.creator?.name || t('comment.anonymousUser') }}
                  </span>
                  <span class="text-caption text-grey mx-2">·</span>
                  <span class="text-caption text-grey">
                    {{ subComment.createdAt }}
                  </span>
                </div>
                <p class="text-body-2 mb-2">
                  <span v-if="subComment.toUser?.name" class="text-primary font-weight-medium">
                    @{{ subComment.toUser.name }}
                  </span>
                  {{ subComment.content }}
                </p>
                <div class="d-flex align-center mb-2">
                  <v-btn
                    size="small"
                    variant="text"
                    :color="subComment.liked ? 'primary' : 'grey-darken-2'"
                    :disabled="isOwnComment(subComment)"
                    @click="handleUpvoteComment(subComment)"
                  >
                    <v-icon
                      :icon="subComment.liked ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
                      size="16"
                      class="mr-1"
                    ></v-icon>
                    {{ subComment.likeCount || 0 }}
                  </v-btn>
                  <v-btn
                    size="small"
                    variant="text"
                    color="grey-darken-2"
                    class="ml-2"
                    @click="replyToSubComment(comment.id, subComment)"
                  >
                    <v-icon icon="mdi-comment-outline" size="16" class="mr-1"></v-icon>
                    {{ t('comment.reply') }}
                  </v-btn>
                </div>

                <!-- 子评论的回复输入框 -->
                <div v-if="activeReplyId === subComment.id" class="mt-2">
                  <v-textarea
                    v-model="replyContent"
                    :placeholder="
                      t('comment.replyToPlaceholder', {
                        name: subComment.creator?.name || t('comment.anonymousUser'),
                      })
                    "
                    variant="outlined"
                    density="comfortable"
                    rounded="lg"
                    rows="2"
                    :rules="isReplyFocused ? commentRules : []"
                    :counter="commentMaxLength"
                    auto-grow
                    hide-details="auto"
                    class="mb-2"
                    @focus="isReplyFocused = true"
                    @blur="isReplyFocused = false"
                  ></v-textarea>
                  <div class="d-flex justify-end">
                    <v-btn
                      size="small"
                      variant="text"
                      color="grey"
                      class="mr-2"
                      @click="cancelReply"
                    >
                      {{ t('common.cancel') }}
                    </v-btn>
                    <v-btn
                      size="small"
                      color="primary"
                      variant="tonal"
                      :disabled="!replyContent.trim()"
                      :loading="replying"
                      @mousedown.prevent="handleSubmitReply(comment.id)"
                    >
                      {{ t('comment.postReply') }}
                    </v-btn>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 查看更多子评论按钮 -->
          <v-btn
            v-if="comment.hasMoreReplies"
            variant="text"
            size="small"
            color="grey-darken-2"
            class="mt-0"
            @click="loadSubComments(comment)"
          >
            {{ t('comment.viewMoreReplies') }} ({{
              comment.replyCount - (comment.children?.length || 0)
            }})
          </v-btn>
        </div>
      </div>
      <!-- comment-item 结束 -->

      <!-- 加载更多指示器 -->
      <div v-if="hasMore" ref="loadMoreTrigger" class="text-center mt-6 py-4">
        <v-progress-circular v-if="loadingMore" indeterminate color="primary" size="32" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.comments-section {
  padding-top: 0px;
}

.comment-input-section {
  margin-bottom: 24px;
}

.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  padding-bottom: 2px;
  border-bottom: 0px solid #f0f0f0;
  transition: background-color 0.3s ease;
}

.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.comment-content.highlighted {
  border-left: 4px solid rgb(var(--v-theme-primary));
  padding-left: 12px;
  margin-left: -16px;
  animation: highlight-pulse 1.5s ease-in-out 3;
}

.sub-comments {
  padding-left: 0;
}

.sub-comments-ellipsis {
  text-align: left;
}

.sub-comment-item {
  padding-bottom: 0px;
  transition: background-color 0.3s ease;
}

.sub-comment-item:last-child {
  padding-bottom: 0;
}

.sub-comment-item.highlighted {
  border-left: 4px solid rgb(var(--v-theme-primary));
  padding-left: 12px;
  margin-left: -16px;
  animation: highlight-pulse 1.5s ease-in-out 3;
}

@keyframes highlight-pulse {
  0%,
  100% {
    border-left-color: rgb(var(--v-theme-primary));
  }
  50% {
    border-left-color: transparent;
  }
}

.view-all-hint {
  display: flex;
  flex-direction: column;
}

.view-all-hint .ellipsis-row {
  text-align: left;
}

.view-all-hint .action-row {
  text-align: center;
}
</style>
