<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { commentApi } from '@/api'
import { useFetch, useMutation } from '@/composables'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { ObjectType } from '@/enums'

interface Props {
  postId?: number
  commentCount?: number
  objectType?: number
}

const props = withDefaults(defineProps<Props>(), {
  postId: 0,
  commentCount: 0,
  objectType: ObjectType.POST,
})

// 验证规则
const commentRules = useValidationRules('comment-content')
const commentMaxLength = useMaxLength('comment-content')

const newComment = ref('')
const isCommentFocused = ref(false)
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
const lastScore = ref<number | undefined>(undefined)
const lastId = ref<number | undefined>(undefined)

// 使用 useFetch 加载初始评论列表
const { execute: fetchInitialComments } = useFetch({
  fetchFn: () => commentApi.getComments(props.postId, props.objectType),
  immediate: false,
  onSuccess: (result) => {
    if (result?.items && result.items.length > 0) {
      comments.value = result.items
      // 初始化每个评论的 hasMoreReplies 标志
      comments.value.forEach((comment) => {
        // 如果有子评论且子评论数量小于总回复数，说明还有更多
        if (
          comment.children &&
          comment.children.length > 0 &&
          comment.replyCount > comment.children.length
        ) {
          comment.hasMoreReplies = true
        } else {
          comment.hasMoreReplies = false
        }
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
    hasMore.value = true
  }

  loading.value = true
  await fetchInitialComments()
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
    successMessage: '评论发表成功',
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
    successMessage: '回复发表成功',
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
  replyToUserName.value = subComment.creatorName || '匿名用户'
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
const handleUpvoteComment = (commentId: number) => {
  console.log('Upvote comment:', commentId)
  // TODO: 实现点赞逻辑
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

// 组件挂载时设置无限滚动和加载初始数据
onMounted(() => {
  void loadComments(true)
  setTimeout(setupInfiniteScroll, 100)
})

// 组件卸载时清理
onBeforeUnmount(() => {
  cleanupInfiniteScroll()
})
</script>

<template>
  <div class="comments-section">
    <!-- 评论输入 -->
    <div class="comment-input-section mb-9">
      <v-textarea
        v-model="newComment"
        placeholder="写下你的评论..."
        variant="outlined"
        density="comfortable"
        rounded="lg"
        :rows="isCommentFocused ? 3 : 1"
        :rules="commentRules"
        :counter="commentMaxLength"
        auto-grow
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
        发表评论
      </v-btn>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
    </div>

    <!-- 空状态 -->
    <div v-else-if="comments.length === 0" class="text-center py-8">
      <v-icon icon="mdi-comment-outline" size="48" color="grey-lighten-2" class="mb-2"></v-icon>
      <p class="text-body-2 text-grey">暂无评论，快来发表第一条评论吧！</p>
    </div>

    <!-- 评论列表 -->
    <div v-else class="comment-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item mb-2">
        <div class="d-flex">
          <v-avatar size="36" color="grey-lighten-2" class="mr-3 mt-1">
            <v-icon icon="mdi-account" color="grey" size="20"></v-icon>
          </v-avatar>
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-2">
              <span class="text-body-2 font-weight-medium text-grey-darken-3">
                {{ comment.creatorName || '匿名用户' }}
              </span>
              <span class="text-caption text-grey mx-2">·</span>
              <span class="text-caption text-grey">
                {{ comment.createdAt }}
              </span>
            </div>
            <p class="text-body-1 mb-2">
              {{ comment.content }}
            </p>

            <!-- 操作按钮 -->
            <div class="d-flex align-center mb-2">
              <v-btn
                size="small"
                variant="text"
                color="grey-darken-2"
                @click="handleUpvoteComment(comment.id)"
              >
                <v-icon icon="mdi-thumb-up-outline" size="16" class="mr-1"></v-icon>
                {{ comment.vote || 0 }}
              </v-btn>
              <v-btn
                size="small"
                variant="text"
                color="grey-darken-2"
                class="ml-2"
                @click="toggleReply(comment.id)"
              >
                <v-icon icon="mdi-comment-outline" size="16" class="mr-1"></v-icon>
                回复
              </v-btn>
            </div>

            <!-- 回复输入框 - 只在回复主评论时显示 -->
            <div v-if="activeReplyId === comment.id && replyToUserId === null" class="mt-2 mb-3">
              <v-textarea
                v-model="replyContent"
                placeholder="写下你的回复..."
                variant="outlined"
                density="comfortable"
                rounded="lg"
                rows="2"
                :rules="commentRules"
                :counter="commentMaxLength"
                auto-grow
                class="mb-2"
              ></v-textarea>
              <div class="d-flex justify-end">
                <v-btn
                  size="small"
                  variant="text"
                  color="grey"
                  class="mr-2"
                  @click="toggleReply(comment.id)"
                >
                  取消
                </v-btn>
                <v-btn
                  size="small"
                  color="primary"
                  variant="tonal"
                  :disabled="!replyContent.trim()"
                  :loading="replying"
                  @mousedown.prevent="handleSubmitReply(comment.id)"
                >
                  发表回复
                </v-btn>
              </div>
            </div>

            <!-- 子评论列表 -->
            <div v-if="comment.children && comment.children.length > 0" class="sub-comments mt-3">
              <div
                v-for="subComment in comment.children"
                :key="subComment.id"
                class="sub-comment-item mb-2"
              >
                <div class="d-flex">
                  <v-avatar size="32" color="grey-lighten-2" class="mr-2">
                    <v-icon icon="mdi-account" color="grey" size="16"></v-icon>
                  </v-avatar>
                  <div class="flex-grow-1">
                    <div class="d-flex align-center mb-2">
                      <span class="text-body-2 font-weight-medium text-grey-darken-3">
                        {{ subComment.creatorName || '匿名用户' }}
                      </span>
                      <span class="text-caption text-grey mx-2">·</span>
                      <span class="text-caption text-grey">
                        {{ subComment.createdAt }}
                      </span>
                    </div>
                    <p class="text-body-1 mb-2">
                      <span v-if="subComment.toUserName" class="text-primary font-weight-medium">
                        @{{ subComment.toUserName }}
                      </span>
                      {{ subComment.content }}
                    </p>
                    <div class="d-flex align-center mb-2">
                      <v-btn
                        size="small"
                        variant="text"
                        color="grey-darken-2"
                        @click="handleUpvoteComment(subComment.id)"
                      >
                        <v-icon icon="mdi-thumb-up-outline" size="16" class="mr-1"></v-icon>
                        {{ subComment.vote || 0 }}
                      </v-btn>
                      <v-btn
                        size="small"
                        variant="text"
                        color="grey-darken-2"
                        class="ml-2"
                        @click="replyToSubComment(comment.id, subComment)"
                      >
                        <v-icon icon="mdi-comment-outline" size="16" class="mr-1"></v-icon>
                        回复
                      </v-btn>
                    </div>

                    <!-- 子评论的回复输入框 -->
                    <div v-if="activeReplyId === subComment.id" class="mt-2">
                      <v-textarea
                        v-model="replyContent"
                        :placeholder="`回复 @${subComment.creatorName || '匿名用户'}...`"
                        variant="outlined"
                        density="comfortable"
                        rounded="lg"
                        rows="2"
                        :rules="commentRules"
                        :counter="commentMaxLength"
                        auto-grow
                        class="mb-2"
                      ></v-textarea>
                      <div class="d-flex justify-end">
                        <v-btn
                          size="small"
                          variant="text"
                          color="grey"
                          class="mr-2"
                          @click="cancelReply"
                        >
                          取消
                        </v-btn>
                        <v-btn
                          size="small"
                          color="primary"
                          variant="tonal"
                          :disabled="!replyContent.trim()"
                          :loading="replying"
                          @mousedown.prevent="handleSubmitReply(comment.id)"
                        >
                          发表回复
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
                查看更多回复
              </v-btn>
            </div>
          </div>
        </div>
      </div>

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
  margin-bottom: 48px;
}

.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  padding-bottom: 2px;
  border-bottom: 0px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.sub-comments {
  padding-left: 0;
}

.sub-comment-item {
  padding-bottom: 0px;
}

.sub-comment-item:last-child {
  padding-bottom: 0;
}
</style>
