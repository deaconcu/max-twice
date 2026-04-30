<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { commentApi } from '@/api'
import { ObjectType } from '@/enums'
import { useQuery } from '@tanstack/vue-query'
import { useI18n } from '@/composables/useI18n'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const commentId = Number(route.params.id)

const {
  isPending: loading,
  data,
  error,
} = useQuery({
  queryKey: ['comment', 'basic', commentId],
  queryFn: () => commentApi.getCommentBasic(commentId),
})

watch(data, (result) => {
  if (!result) return
  const { objectType, objectId, replyToCommentId } = result

  // 根据对象类型跳转到对应页面
  switch (objectType) {
    case ObjectType.POST:
      // 帖子评论
      if (replyToCommentId && replyToCommentId > 0) {
        // 子评论：带上父评论ID和子评论ID
        router.replace({
          path: '/read',
          query: {
            postId: String(objectId),
            commentId: String(replyToCommentId),
            subCommentId: String(commentId),
          },
        })
      } else {
        // 主评论
        router.replace({
          path: '/read',
          query: {
            postId: String(objectId),
            commentId: String(commentId),
          },
        })
      }
      break

    case ObjectType.NODE:
      // 节点评论
      if (replyToCommentId && replyToCommentId > 0) {
        router.replace({
          path: '/read',
          query: {
            nodeId: String(objectId),
            commentId: String(replyToCommentId),
            subCommentId: String(commentId),
          },
        })
      } else {
        router.replace({
          path: '/read',
          query: {
            nodeId: String(objectId),
            commentId: String(commentId),
          },
        })
      }
      break

    case ObjectType.ROADMAP:
      // 路线图评论
      if (replyToCommentId && replyToCommentId > 0) {
        router.replace({
          path: `/roadmap/${objectId}`,
          query: {
            commentId: String(replyToCommentId),
            subCommentId: String(commentId),
          },
        })
      } else {
        router.replace({
          path: `/roadmap/${objectId}`,
          query: {
            commentId: String(commentId),
          },
        })
      }
      break

    default:
      router.replace('/error/404')
  }
})

watch(error, (err) => {
  if (err) router.replace('/error/404')
})
</script>

<template>
  <div class="redirect-page">
    <v-progress-circular
      v-if="loading"
      indeterminate
      color="primary"
      size="48"
    ></v-progress-circular>
    <p class="mt-4 text-grey">{{ t('common.redirecting') }}</p>
  </div>
</template>

<style scoped>
.redirect-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
</style>
