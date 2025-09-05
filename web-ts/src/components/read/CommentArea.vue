<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { commentServiceV1, upvoteServiceV1 } from '@/services/api/v1/apiServiceV1'
import { ObjectType } from '@/types/enums'
import type { ObjectType as ObjectTypeType } from '@/types/enums'
import type { Comment } from '@/types/comment'
import SubcommentArea from './SubcommentArea.vue'

const route = useRoute()
const { t } = useI18n()

interface CommentObject {
  id: number
  commentCount: number
}

interface Props {
  object: CommentObject
  type: ObjectType
}

interface LoadCallback {
  (status: string): void
}

const props = defineProps<Props>()

const comments = ref<Comment[]>([])
const inputComment = ref<string>('')
const offsetId = ref<number>(0)
const scrollKey = ref<number>(Date.now())
const activeReplyId = ref<number | null>(null)
const replyContent = ref<string>('')
const targetItem = ref<HTMLElement | null>(null)
const highlightedCommentId = ref<number | null>(null)
const commentArea = ref<HTMLElement | null>(null)

// 用于获取目标元素的ref函数
const setTargetRef = (el: HTMLElement | null): void => {
  if (el) {
    targetItem.value = el
  }
}

// 设置高亮并在5秒后移除
const setHighlight = (commentId: number): void => {
  highlightedCommentId.value = commentId
  setTimeout(() => {
    highlightedCommentId.value = null
  }, 5000)
}

const load = async ({ done }: { done: LoadCallback }): Promise<void> => {
  try {
    if ('commentId' in route.query && offsetId.value === 0) {
      const commentId = Number(route.query.commentId)
      offsetId.value = commentId - 4 > 0 ? commentId - 4 : 0
    }
    const response = await commentServiceV1.getComments(
      props.object.id,
      props.type,
      offsetId.value
    )

    if (response.code === 401) {
      // not login
    } else if (response.code === 200) {
      comments.value.push(...response.data)

      if (response.data.length > 0) {
        offsetId.value = response.data[response.data.length - 1].id
        done('ok')

        await nextTick()
        targetItem.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })

        // 如果有commentId且没有subCommentId，设置主评论高亮
        if ('commentId' in route.query && !('subCommentId' in route.query)) {
          setHighlight(parseInt(route.query.commentId as string))
        }
      } else {
        done('empty')
      }
    }
  } catch (error) {
    console.error('Error loading comments:', error)
  }
}

const sendComment = async (): Promise<void> => {
  try {
    const response = await commentServiceV1.createComment(
      props.object.id,
      props.type,
      0,
      0,
      inputComment.value
    )
    inputComment.value = ''
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200) {
      console.log('Form submitted successfully')
      comments.value.unshift(response.data)
      // eslint-disable-next-line vue/no-mutating-props
      props.object.commentCount++
    }
  } catch (error) {
    console.error('Error submitting form:', error)
  }
}

const sendSubcomment = async (comment: Comment): Promise<void> => {
  try {
    console.log('begin post')

    const response = await commentServiceV1.createComment(
      props.object.id,
      props.type,
      activeReplyId.value,
      0,
      replyContent.value
    )
    replyContent.value = ''
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200) {
      console.log('Form submitted successfully')
      // eslint-disable-next-line vue/no-mutating-props
      props.object.commentCount++
      if (comment.children === null) {
        comment.children = [response.data]
      } else {
        comment.children.unshift(response.data)
      }
    }
  } catch (error) {
    console.error('Error submitting form:', error)
  }
}

const returnToAllComment = (): void => {
  let url = ''
  if (props.type === ObjectType.POST) {
    url = `/read?postId=${props.object.id}`
  } else {
    url = `/read?nodeId=${props.object.id}&tab=comment`
  }
  window.location.href = url.toString()
}

onMounted(async () => {
  await nextTick()
  commentArea.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
})

const upvote = async (comment: Comment): Promise<void> => {
  try {
    console.log('begin post')

    const response = await upvoteServiceV1.upvote(comment.id, 2, 2)
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200) {
      console.log('Form submitted successfully')
      comment.upvoteCount = response.data.upvotes
      comment.upvoted = response.data.upvoted
    }
  } catch (error) {
    console.error('Error submitting form:', error)
  }
}
</script>

<template>
  <v-text-field
    v-model="inputComment"
    variant="outlined"
    density="compact"
    append-inner-icon="mdi-email-fast-outline"
    :placeholder="t('comment.addComment')"
    class="w-100"
    @click:append-inner="sendComment"
  ></v-text-field>
  <a
    v-if="'commentId' in route.query"
    ref="commentArea"
    variant="text"
    class="cursor-pointer"
    @click="returnToAllComment"
    >... {{ t('comment.viewAllComments') }}</a
  >
  <v-infinite-scroll :key="scrollKey" :items="comments" @load="load" class="w-100">
    <div
      v-for="comment in comments"
      :key="comment.id"
      :ref="comment.id === parseInt(route.query.commentId as string) ? setTargetRef : null"
    >
      <v-row
        class="mx-0 my-0 py-2 w-100"
        :class="{ highlighted: highlightedCommentId === comment.id }"
      >
        <div>
          <v-avatar icon="mdi-account" size="30" color="grey" class="mr-3">
            <span class="text-body-1">CJ</span>
          </v-avatar>
        </div>
        <div class="comment-content">
          <div class="text-body-2 mb-2 text-grey-darken-1">
            {{ t('comment.username') }}
            <span class="ms-2 text-caption text-grey">{{ comment.createdAt }}</span>
          </div>
          <div>{{ comment.content }}</div>
          <div
            class="ma-0 mt-3 pb-1 d-flex align-center justify-start text-grey-darken-1 text-body-2"
          >
            <v-btn
              class="ms-0"
              variant="flat"
              :color="comment.upvoted ? 'teal' : 'grey-lighten-4'"
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
              :color="activeReplyId === comment.id ? 'grey-lighten-4' : ''"
              @click="activeReplyId = activeReplyId === comment.id ? 0 : comment.id"
              >{{ t('comment.reply') }}</v-btn
            >
          </div>
          <div class="mt-2 mb-2">
            <v-text-field
              v-if="activeReplyId === comment.id"
              v-model="replyContent"
              variant="outlined"
              density="compact"
              append-inner-icon="mdi-email-fast-outline"
              :placeholder="t('comment.addComment')"
              class="w-100"
              hide-details
              @click:append-inner="sendSubcomment(comment)"
            ></v-text-field>
          </div>
          <SubcommentArea
            v-if="comment.children !== null"
            v-model:active-reply-id="activeReplyId"
            :comment-id="comment.id"
            :comments="comment.children"
            :count="comment.replyCount"
            :offset-id="
              comment.id === parseInt(route.query.commentId as string) && 'subCommentId' in route.query
                ? route.query.subCommentId as string
                : 0
            "
          ></SubcommentArea>
        </div>
      </v-row>
    </div>
    <template #empty>
      <div v-if="comments.length > 0" class="text-grey py-4">
        - {{ t('comment.endOfComments') }} -
      </div>
      <div v-else class="py-2 text-center">
        <v-icon size="50" color="grey-lighten-2" class="mb-2">mdi-comment-text-outline</v-icon>
        <div class="text-grey-lighten-1 text-body-2 mb-2">{{ t('comment.noComments') }}</div>
      </div>
    </template>
  </v-infinite-scroll>
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