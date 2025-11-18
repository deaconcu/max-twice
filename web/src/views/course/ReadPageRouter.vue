<script lang="ts">
export default {
  name: 'ReadPageRouter',
}
</script>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ContentReadPage from './ContentReadPage.vue'
import NodePostsPage from './NodePostsPage.vue'
import PostDetailPage from './PostDetailPage.vue'

const route = useRoute()

// 根据查询参数判断加载哪个页面组件
const currentComponent = computed(() => {
  // 节点模式
  if (route.query.nodeId) {
    return NodePostsPage
  }

  // 帖子/评论模式
  if (route.query.postId || route.query.commentId) {
    return PostDetailPage
  }

  // 完整课程模式（默认）
  return ContentReadPage
})
</script>

<template>
  <keep-alive :include="['NodePostsPage', 'ContentReadPage']">
    <component :is="currentComponent" />
  </keep-alive>
</template>
