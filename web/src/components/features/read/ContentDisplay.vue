<template>
  <div class="content-display">
    <v-progress-circular v-if="loading" indeterminate />
    <div v-else>
      <div v-for="article in articles" :key="article.id" class="article-card mb-4">
        <v-card @click="$emit('article-click', article.id)">
          <v-card-title>{{ article.title }}</v-card-title>
          <v-card-text>
            <div v-html="article.content"></div>
          </v-card-text>
        </v-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Article {
  id: number
  title: string
  content: string
  author: string
  createTime: string
}

defineProps<{
  articles: Article[]
  loading: boolean
}>()

defineEmits<{
  'article-click': [articleId: number]
}>()
</script>

<style scoped>
.article-card {
  cursor: pointer;
}
</style>
