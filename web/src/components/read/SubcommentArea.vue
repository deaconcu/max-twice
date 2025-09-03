<script setup>
  import { nextTick, onMounted, ref } from 'vue'
  import { useRoute } from 'vue-router'
  import { useI18n } from 'vue-i18n'
  import { commentServiceV1, upvoteServiceV1 } from '@/services/api/v1/apiServiceV1'

  const { t } = useI18n()
  const route = useRoute()
  const props = defineProps({
    commentId: {
      type: [String, Number],
      required: true,
    },
    comments: {
      type: Array,
      default: () => [],
    },
    count: {
      type: Number,
      default: 0,
    },
    activeReplyId: {
      type: [String, Number],
      default: 0,
    },
    offsetId: {
      type: [String, Number],
      default: 0,
    },
  })
  const localOffsetId = ref(0)
  const displayLoadMore = ref(true)
  const highlightedSubCommentId = ref(null)
  const replyContent = ref('')
  const localComments = ref([])

  // 设置子评论高亮并在5秒后移除
  const setSubHighlight = (subCommentId) => {
    highlightedSubCommentId.value = subCommentId
    setTimeout(() => {
      highlightedSubCommentId.value = null
    }, 5000)
  }

  const emit = defineEmits(['update:activeReplyId'])

  const updateActiveReplayId = (newValue) => {
    if (props.activeReplyId === newValue) {
      emit('update:activeReplyId', 0)
    } else {
      emit('update:activeReplyId', newValue)
    }
  }

  onMounted(() => {
    // console.log(`offsetId: ${props.offsetId}`)

    // Initialize localComments with props data
    localComments.value = [...props.comments]

    if (localComments.value.length > 0) {
      localOffsetId.value = localComments.value[localComments.value.length - 1].id
    }

    if (props.offsetId > localOffsetId.value) {
      localComments.value = []
      localOffsetId.value = props.offsetId - 1
      loadMore()
    }

    // 如果有subCommentId，设置子评论高亮
    if ('subCommentId' in route.query) {
      nextTick(() => {
        setSubHighlight(parseInt(route.query.subCommentId))
      })
    }
  })

  const loadMore = async () => {
    try {
      const response = await commentServiceV1.getCommentReplies(
        props.commentId,
        localOffsetId.value
      )

      if (response.code === 401) {
        // console.log('not login')
        //router.push('/login');
      } else if (response.code === 200) {
        // console.log(`get data:${JSON.stringify(response.data)}`)
        localComments.value.push(...response.data)

        if (response.data.length > 0) {
          localOffsetId.value = response.data[response.data.length - 1].id
        } else {
          displayLoadMore.value = false
        }
      }
    } catch {
      // Handle error without using undefined variable
    }
  }

  const upvote = async (comment) => {
    try {
      // console.log('begin post')

      const response = await upvoteServiceV1.upvote(comment.id, 2, 2)
      // console.log(`response: ${JSON.stringify(response)}`)

      if (response.code === 200) {
        // console.log('Form submitted successfully')
        comment.upvoteCount = response.data.upvoteCount
        comment.upvoted = response.data.upvoted
      }
    } catch {
      // console.error('Error submitting form:', error)
    }
  }

  const sendComment = () => {
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
              :color="comment.upvoted > 0 ? 'teal' : 'grey-lighten-4'"
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
