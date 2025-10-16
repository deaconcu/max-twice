<script setup lang="ts">
import { computed, inject, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { commentServiceV1, upvoteServiceV1 } from '@/services/api/v1/apiServiceV1'
import type { Comment } from '@/types/comment'

const { t } = useI18n()
const route = useRoute()
const showSnackbar = inject('showSnackbar') as (message: string, type?: string) => void

interface Props {
  commentId: number
  comments?: Comment[]
  count?: number
  activeReplyId?: string | number
  offsetId?: string | number
}

interface Emits {
  (e: 'update:activeReplyId', value: string | number): void
}

const props = withDefaults(defineProps<Props>(), {
  comments: () => [],
  count: 0,
  activeReplyId: 0,
  offsetId: 0,
})

const emit = defineEmits<Emits>()

const localOffsetId = ref<number>(0)
const displayLoadMore = ref<boolean>(true)
const highlightedSubCommentId = ref<number | null>(null)
const replyContent = ref<string>('')
const additionalComments = ref<Comment[]>([])

// 合并 props.comments 和额外加载的评论
const localComments = computed<Comment[]>(() => {
  return [...props.comments, ...additionalComments.value]
})

// 设置子评论高亮并在5秒后移除
const setSubHighlight = (subCommentId: number): void => {
  highlightedSubCommentId.value = subCommentId
  setTimeout(() => {
    highlightedSubCommentId.value = null
  }, 5000)
}

const updateActiveReplayId = (newValue: string | number): void => {
  if (props.activeReplyId === newValue) {
    emit('update:activeReplyId', 0)
  } else {
    emit('update:activeReplyId', newValue)
  }
}

onMounted(() => {
  // 初始化 offsetId
  if (props.comments.length > 0) {
    localOffsetId.value = props.comments[props.comments.length - 1].id
  }

  if (Number(props.offsetId) > localOffsetId.value) {
    localOffsetId.value = Number(props.offsetId) - 1
    loadMore()
  }

  // 如果有subCommentId，设置子评论高亮
  if ('subCommentId' in route.query) {
    nextTick(() => {
      setSubHighlight(parseInt(route.query.subCommentId as string))
    })
  }
})

const loadMore = async (): Promise<void> => {
  try {
    const response = await commentServiceV1.getCommentReplies(
      props.commentId,
      localOffsetId.value
    )

    if (response.code === 401) {
      showSnackbar('error.loadFailed', 'error')
    } else if (response.code === 200) {
      additionalComments.value.push(...response.data)

      if (response.data.length > 0) {
        localOffsetId.value = response.data[response.data.length - 1].id
      } else {
        displayLoadMore.value = false
      }
    } else {
      showSnackbar(response.message || 'error.loadFailed', 'error')
    }
  } catch (error) {
    console.error('Error loading comments:', error)
    showSnackbar('error.loadFailed', 'error')
  }
}

const upvote = async (comment: Comment): Promise<void> => {
  try {
    const response = await upvoteServiceV1.upvote(comment.id, 2, 2)

    if (response.code === 200) {
      comment.upvoteCount = response.data.upvotes
      comment.upvoted = response.data.upvoted
    } else {
      showSnackbar(response.message || 'error.operationFailed', 'error')
    }
  } catch (error) {
    console.error('Error submitting upvote:', error)
    showSnackbar('error.operationFailed', 'error')
  }
}

const sendComment = (): void => {
  // TODO: 实现发送评论功能
  replyContent.value = ''
}
</script>

<template>
  <div class="mt-4">
    <template v-for="(comment, key) in localComments" :key="key">
      <v-row
        class="mx-0 pt-2 pb-1 w-100"
        :class="{ highlighted: highlightedSubCommentId === comment.id }"
      >
        <div>
          <v-avatar icon="mdi-account" size="30" color="red" class="mr-3">
            <span class="text-body-1">CJ</span>
          </v-avatar>
        </div>
        <div class="comment-content">
          <div class="text-body-2 mb-2 text-grey-darken-1">
            {{ t('subcomment.username') }}
            <span class="ms-2 text-caption text-grey">{{ comment.createdAt }}</span>
          </div>
          <div class="">{{ comment.content }}</div>
          <div class="ma-0 py-2 pb-1 d-flex align-center justify-start text-grey-darken-1">
            <v-btn
              class="ms-0"
              variant="flat"
              :color="comment.upvoted ? 'grey-darken-1' : 'grey-lighten-4'"
              rounded="xl"
              density="compact"
              prepend-icon="mdi-arrow-up"
              @click="upvote(comment)"
              >{{ comment.upvoteCount }}</v-btn
            >

            <v-btn
              class="mx-3"
              variant="text"
              density="compact"
              prepend-icon="mdi-chat-outline"
              @click="updateActiveReplayId(comment.id)"
              >{{ t('subcomment.reply') }}</v-btn
            >
          </div>
          <div class="mt-2 mb-2">
            <v-text-field
              v-if="activeReplyId === comment.id"
              v-model="replyContent"
              variant="outlined"
              density="compact"
              append-inner-icon="mdi-email-fast-outline"
              :placeholder="t('subcomment.addComment')"
              class="w-100"
              hide-details
              @click:append-inner="sendComment"
            ></v-text-field>
          </div>
        </div>
      </v-row>
    </template>
    <v-btn
      v-if="props.count > 1 && displayLoadMore"
      variant="plain"
      class="pa-0 ma-0"
      @click="loadMore"
      >{{ t('subcomment.viewMoreComments') }}</v-btn
    >
  </div>
</template>

<style scoped>
.comment-content {
  width: 90%;
}

.highlighted {
  background: #fafafa !important;
  transition: background-color 1s ease;
}
</style>