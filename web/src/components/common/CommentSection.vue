<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  postId?: number
  commentCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  postId: 0,
  commentCount: 0,
})

const newComment = ref('')

// Mock 评论数据
const mockComments = [
  {
    id: 1,
    author: '用户A',
    time: '2小时前',
    content: '讲解得很清楚，特别是 reactive 和 ref 的区别部分！',
    votes: 12,
  },
  {
    id: 2,
    author: '用户B',
    time: '5小时前',
    content: '能否再详细说明一下 computed 的缓存机制？',
    votes: 8,
  },
  {
    id: 3,
    author: '用户C',
    time: '1天前',
    content: '非常实用的教程，已经在项目中应用了这些概念。',
    votes: 15,
  },
]

// 发表评论
const handleSubmitComment = () => {
  console.log('Submit comment:', newComment.value)
  // TODO: 实现发表评论逻辑
}

// 点赞评论
const handleUpvoteComment = (commentId: number) => {
  console.log('Upvote comment:', commentId)
  // TODO: 实现点赞逻辑
}
</script>

<template>
  <div class="comments-section">
    <h3 class="text-h6 font-weight-bold mb-4">评论 {{ commentCount }}</h3>

    <!-- 评论输入 -->
    <div class="comment-input-section mb-9">
      <v-textarea
        v-model="newComment"
        placeholder="写下你的评论..."
        variant="outlined"
        rows="3"
        hide-details
        class="mb-3"
      ></v-textarea>
      <v-btn
        color="primary"
        variant="tonal"
        density="comfortable"
        :disabled="!newComment.trim()"
        @click="handleSubmitComment"
      >
        <v-icon icon="mdi-send" size="18" class="mr-1"></v-icon>
        发表评论
      </v-btn>
    </div>

    <!-- 评论列表 -->
    <div class="comment-list">
      <div v-for="comment in mockComments" :key="comment.id" class="comment-item mb-4">
        <div class="d-flex">
          <v-avatar size="36" color="grey-lighten-2" class="mr-3 mt-1">
            <v-icon icon="mdi-account" color="grey" size="20"></v-icon>
          </v-avatar>
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-1">
              <span class="text-body-2 font-weight-medium text-grey-darken-3">
                {{ comment.author }}
              </span>
              <span class="text-caption text-grey mx-2">·</span>
              <span class="text-caption text-grey">
                {{ comment.time }}
              </span>
            </div>
            <p class="text-body-2 text-grey-darken-2 mb-2">
              {{ comment.content }}
            </p>
            <v-btn
              size="x-small"
              variant="text"
              color="grey-darken-2"
              @click="handleUpvoteComment(comment.id)"
            >
              <v-icon icon="mdi-thumb-up-outline" size="14" class="mr-1"></v-icon>
              {{ comment.votes }}
            </v-btn>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.comments-section {
  padding-top: 40px;
}

.comment-input-section {
  margin-bottom: 48px;
}

.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
</style>
