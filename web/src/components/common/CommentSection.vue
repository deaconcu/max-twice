<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { commentApi } from '@/api'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { useI18n } from '@/composables/useI18n'
import { ObjectType, VoteType } from '@/enums'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useUserStore } from '@/stores/modules/user'
import {
  useCommentListQuery,
  useCommentContextQuery,
  useCreateCommentMutation,
} from '@/queries/comment'
import { useUpvoteMutation } from '@/queries/interaction'
import { useMutation } from '@tanstack/vue-query'

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
  objectType?: ObjectType
  targetCommentId?: number | null
  targetSubCommentId?: number | null
}

// 验证规则
const commentRules = useValidationRules('comment-content')
const commentMaxLength = useMaxLength('comment-content')

// 用户信息
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userId)

const isOwnComment = (comment: any) => {
  return !!currentUserId.value && comment.creatorId === currentUserId.value
}

const newComment = ref('')
const isCommentFocused = ref(false)
const isReplyFocused = ref(false)
const activeReplyId = ref<number | null>(null)
const parentCommentId = ref<number | null>(null)
const replyContent = ref('')
const replyToUserId = ref<number | null>(null)
const replyToUserName = ref<string>('')
const loadMoreTrigger = ref<HTMLElement | null>(null)

// ========== 评论数据：本地工作副本 ==========
// query data 到达后同步到本地 ref；之后所有可变操作（unshift/push/liked）操作本地 ref
const comments = ref<any[]>([])
const hasMoreBefore = ref(false)
// 上下文模式向后翻页游标（仅 context 模式使用）
const contextLastCursor = ref<string | undefined>(undefined)
const contextHasMoreAfter = ref(false)
const loadingMoreContext = ref(false)

const highlightedCommentId = ref<number | null>(null)
const highlightedSubCommentId = ref<number | null>(null)

const isContextMode = computed(() => !!props.targetCommentId)

// ========== 主评论列表 query（仅在非上下文模式启用）==========
const {
  data: listData,
  isFetching: listFetching,
  isFetchingNextPage,
  hasNextPage,
  fetchNextPage,
} = useCommentListQuery(
  computed(() => props.postId),
  computed(() => props.objectType),
  computed(() => !isContextMode.value && props.postId > 0)
)

// ========== 主评论上下文 query ==========
const { data: contextData, isFetching: contextFetching } = useCommentContextQuery(
  computed(() => props.targetCommentId ?? null)
)

// ========== 子评论上下文 query ==========
const { data: subContextData, isFetching: subContextFetching } = useCommentContextQuery(
  computed(() => props.targetSubCommentId ?? null)
)

// 综合 loading 状态（首屏）
const loading = computed(() => {
  if (isContextMode.value) {
    if (props.targetSubCommentId) return contextFetching.value || subContextFetching.value
    return contextFetching.value
  }
  return listFetching.value && comments.value.length === 0
})

const loadingMore = computed(() => isFetchingNextPage.value || loadingMoreContext.value)

const hasMore = computed(() =>
  isContextMode.value ? contextHasMoreAfter.value : (hasNextPage.value ?? false)
)

// 给评论列表初始化 hasMoreReplies 标志
const initHasMoreReplies = (list: any[]) => {
  list.forEach((comment) => {
    const childrenCount = comment.children?.length || 0
    comment.hasMoreReplies = comment.replyCount > childrenCount
  })
}

// 滚动到目标元素并设置高亮（统一封装）
const scrollAndHighlight = (selector: string, clearFn: () => void) => {
  const observer = new MutationObserver(() => {
    const el = document.querySelector(selector)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      observer.disconnect()
    }
  })
  observer.observe(document.body, { childList: true, subtree: true })
  setTimeout(clearFn, 5000)
}

// ========== 列表模式：watch listData → 本地 ref ==========
watch(
  listData,
  async (val) => {
    if (isContextMode.value) return
    if (!val) return
    const flat = val.pages.flatMap((p) => p.items ?? [])
    initHasMoreReplies(flat)
    comments.value = flat
    await nextTick()
    setupInfiniteScroll()
  },
  { immediate: true }
)

// ========== 上下文模式：watch contextData → 本地 ref ==========
watch(
  contextData,
  async (val) => {
    if (!val || !val.items || val.items.length === 0) return
    comments.value = val.items
    initHasMoreReplies(comments.value)
    hasMoreBefore.value = !!val.hasMoreBefore
    contextHasMoreAfter.value = !!val.hasMoreAfter
    contextLastCursor.value = val.lastCursor

    // 子评论上下文延后加载；否则高亮主评论
    if (!props.targetSubCommentId) {
      highlightedCommentId.value = props.targetCommentId ?? null
      scrollAndHighlight(`[data-comment-id="${props.targetCommentId}"]`, () => {
        highlightedCommentId.value = null
      })
    }

    await nextTick()
    setupInfiniteScroll()
  },
  { immediate: true }
)

// ========== 子评论上下文：合并到对应父评论的 children ==========
watch(
  subContextData,
  (val) => {
    if (!val || !val.subItems || val.subItems.length === 0 || !val.parentCommentId) return
    const parent = comments.value.find((c) => c.id === val.parentCommentId)
    if (parent) {
      parent.children = val.subItems
      parent.hasMoreReplies = !!val.hasMoreAfter
      parent.hasMoreRepliesBefore = !!val.hasMoreBefore
    }
    highlightedSubCommentId.value = props.targetSubCommentId ?? null
    scrollAndHighlight(`[data-sub-comment-id="${props.targetSubCommentId}"]`, () => {
      highlightedCommentId.value = null
      highlightedSubCommentId.value = null
    })
  },
  { immediate: true }
)

// ========== 加载更多：列表模式走 fetchNextPage，上下文模式走 mutation ==========
const { mutateAsync: fetchMoreContextPage } = useMutation({
  mutationFn: (cursor: string) => commentApi.getComments(props.postId, props.objectType, cursor),
})

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return

  if (isContextMode.value) {
    if (!contextLastCursor.value) return
    loadingMoreContext.value = true
    try {
      const result = await fetchMoreContextPage(contextLastCursor.value)
      if (result?.items && result.items.length > 0) {
        initHasMoreReplies(result.items)
        comments.value = [...comments.value, ...result.items]
        contextLastCursor.value = result.nextCursor
        contextHasMoreAfter.value = !!result.hasMore
      } else {
        contextHasMoreAfter.value = false
      }
    } finally {
      loadingMoreContext.value = false
    }
  } else {
    await fetchNextPage()
  }
}

// ========== 子评论懒加载（按钮触发，保留直接 API 调用）==========
const { mutateAsync: fetchReplies } = useMutation({
  mutationFn: (params: { commentId: number; cursor?: string }) =>
    commentApi.getCommentReplies(params.commentId, params.cursor),
})

const loadSubComments = async (comment: any) => {
  if (!comment.children) {
    comment.children = []
  }
  // 用最后一个子评论的 nextCursor —— 但 children 自带的字段不一定是 cursor，
  // 取自上次返回的 nextCursor 缓存在 comment 上
  const cursor: string | undefined = comment._nextRepliesCursor
  const result = await fetchReplies({ commentId: comment.id, cursor })
  if (result?.items && result.items.length > 0) {
    comment.children.push(...result.items)
  }
  comment._nextRepliesCursor = result?.nextCursor
  comment.hasMoreReplies = result?.hasMore ?? false
}

// ========== 发表评论 / 回复 ==========
const { mutateAsync: createComment, isPending: submitting } = useCreateCommentMutation()
const { mutateAsync: createReply, isPending: replying } = useCreateCommentMutation()

const handleSubmitComment = async () => {
  if (!newComment.value.trim()) return
  const result = await createComment({
    objectId: props.postId,
    objectType: props.objectType,
    replyTo: null,
    toUser: null,
    content: newComment.value,
  })
  newComment.value = ''
  comments.value.unshift(result as any)
}

const handleSubmitReply = async (_commentId: number) => {
  if (!replyContent.value.trim()) return
  const targetParentId = parentCommentId.value
  const result = await createReply({
    objectId: props.postId,
    objectType: props.objectType,
    replyTo: targetParentId,
    toUser: replyToUserId.value,
    content: replyContent.value,
  })
  replyContent.value = ''
  const parent = comments.value.find((c) => c.id === targetParentId)
  if (parent) {
    if (!parent.children) parent.children = [result]
    else parent.children.unshift(result as any)
  }
  activeReplyId.value = null
  parentCommentId.value = null
  replyToUserId.value = null
  replyToUserName.value = ''
}

// ========== 切换回复框 ==========
const toggleReply = (commentId: number) => {
  if (activeReplyId.value === commentId) {
    activeReplyId.value = null
    parentCommentId.value = null
    replyContent.value = ''
    replyToUserId.value = null
    replyToUserName.value = ''
  } else {
    activeReplyId.value = commentId
    parentCommentId.value = commentId
    replyContent.value = ''
    replyToUserId.value = null
    replyToUserName.value = ''
  }
}

const replyToSubComment = (parentCommentIdParam: number, subComment: any) => {
  activeReplyId.value = subComment.id
  parentCommentId.value = parentCommentIdParam
  replyToUserId.value = subComment.creatorId || null
  replyToUserName.value = subComment.creator?.name || t('comment.anonymousUser')
  replyContent.value = ''
}

const cancelReply = () => {
  activeReplyId.value = null
  parentCommentId.value = null
  replyToUserId.value = null
  replyToUserName.value = ''
}

// ========== 点赞评论 ==========
const upvotingCommentId = ref<number | null>(null)
const { mutateAsync: doUpvote } = useUpvoteMutation()

const handleUpvoteComment = async (comment: any) => {
  if (upvotingCommentId.value === comment.id) return
  upvotingCommentId.value = comment.id
  try {
    const result = await doUpvote({
      objectId: comment.id,
      objectType: ObjectType.COMMENT,
      type: VoteType.LIKE,
    })
    comment.liked = (result as any).liked
    comment.likeCount = (result as any).likeCount
  } finally {
    upvotingCommentId.value = null
  }
}

// ========== Intersection Observer 无限滚动 ==========
let observer: IntersectionObserver | null = null

const setupInfiniteScroll = () => {
  if (!loadMoreTrigger.value) return
  if (observer) {
    observer.disconnect()
    observer = null
  }
  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry?.isIntersecting && hasMore.value && !loadingMore.value) {
        void loadMore()
      }
    },
    { root: null, rootMargin: '100px', threshold: 0.1 }
  )
  observer.observe(loadMoreTrigger.value)
}

const cleanupInfiniteScroll = () => {
  if (observer && loadMoreTrigger.value) {
    observer.unobserve(loadMoreTrigger.value)
  }
  if (observer) {
    observer.disconnect()
    observer = null
  }
}

onMounted(() => {
  // 数据由 watch immediate 触发，无需手动加载
})

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
        :counter="isCommentFocused ? commentMaxLength : undefined"
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
