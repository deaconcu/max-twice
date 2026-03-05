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
  // 有 courseId 或 nodeId 时，使用带目录的 ContentReadPage
  if (route.query.courseId || route.query.nodeId) {
    return ContentReadPage
  }

  // 只有 postId 或 commentId 时，使用纯文章详情页
  if (route.query.postId || route.query.commentId) {
    return PostDetailPage
  }

  // 默认使用 ContentReadPage
  return ContentReadPage
})
</script>

<template>
  <keep-alive :include="['ContentReadPage']">
    <component :is="currentComponent" />
  </keep-alive>
</template>
