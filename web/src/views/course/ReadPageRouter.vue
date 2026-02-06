<script lang="ts">
export default {
  name: 'ReadPageRouter',
}
</script>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ContentReadPage from './ContentReadPage.vue'
import PostDetailPage from './PostDetailPage.vue'

const route = useRoute()

// 根据查询参数判断加载哪个页面组件
const currentComponent = computed(() => {
  // 帖子/评论模式
  if (route.query.postId || route.query.commentId) {
    return PostDetailPage
  }

  // 课程/节点模式（统一使用 ContentReadPage）
  return ContentReadPage
})
</script>

<template>
  <component :is="currentComponent" />
</template>
