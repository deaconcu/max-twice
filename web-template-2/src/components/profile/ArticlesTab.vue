<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">创建的文章</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">
            管理您创作的文章，分享知识和经验。
          </p>
          <v-divider class="my-3"></v-divider>
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-pencil" size="14" class="mr-1"></v-icon>
              编辑发布文章
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-eye" size="14" class="mr-1"></v-icon>
              查看阅读统计
            </div>
            <div>
              <v-icon icon="mdi-tag" size="14" class="mr-1"></v-icon>
              添加标签分类
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-4">
          <v-icon icon="mdi-menu" size="18" color="grey-lighten-1"></v-icon>
        </div>

        <!-- 文章列表 -->
        <div v-if="articles.length > 0">
          <v-card
            v-for="article in articles"
            :key="article.id"
            border
            rounded="lg"
            hover
            class="hoverable mb-4"
          >
            <v-card-text class="pa-4">
              <div class="d-flex align-start justify-space-between mb-3">
                <div class="flex-grow-1" style="cursor: pointer;">
                  <!-- 所属节点 -->
                  <div v-if="article.node" class="mb-3">
                    <v-chip
                      size="small"
                      variant="tonal"
                      color="grey-darken-2"
                      class="cursor-pointer"
                    >
                      <v-icon icon="mdi-file-document-outline" size="14" class="mr-1"></v-icon>
                      {{ article.node.name }}
                    </v-chip>
                  </div>

                  <p class="text-body-2 text-grey-darken-2 mb-3 article-preview">{{ article.preview }}</p>

                  <!-- 统计信息 -->
                  <div class="d-flex align-center text-caption text-grey" style="gap: 16px;">
                    <div>
                      <v-icon icon="mdi-eye-outline" size="14" class="mr-1"></v-icon>
                      {{ article.views }} 阅读
                    </div>
                    <div>
                      <v-icon icon="mdi-heart-outline" size="14" class="mr-1"></v-icon>
                      {{ article.likes }} 点赞
                    </div>
                    <div>
                      <v-icon icon="mdi-comment-outline" size="14" class="mr-1"></v-icon>
                      {{ article.comments }} 评论
                    </div>
                    <div>{{ formatDate(article.publishedAt) }}</div>
                  </div>
                </div>

                <!-- 删除按钮 -->
                <v-btn
                  color="grey"
                  variant="tonal"
                  size="x-small"
                  icon="mdi-delete"
                  @click.stop="deleteArticle(article.id)"
                >
                  <v-icon>mdi-delete</v-icon>
                  <v-tooltip activator="parent" location="top">删除文章</v-tooltip>
                </v-btn>
              </div>
            </v-card-text>
          </v-card>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-file-document-multiple" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-2">暂无创建的文章</p>
          <p class="text-body-2 text-grey">分享您的学习心得和经验</p>
        </div>

        <!-- 删除确认对话框 -->
        <ConfirmDialog
          v-model="showDeleteDialog"
          title="确认删除"
          message="确定要删除该文章吗？此操作不可恢复。"
          confirm-text="确认删除"
          @confirm="confirmDelete"
        />
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

// 删除确认对话框
const showDeleteDialog = ref(false)
const articleToDelete = ref<number | null>(null)

// Mock 文章数据
const articles = ref([
  {
    id: 1,
    preview: '详细介绍 Vue 3 Composition API 的设计理念、使用方法和最佳实践。Composition API 是 Vue 3 中最重要的新特性之一，它提供了一种更灵活的方式来组织组件逻辑。通过使用 setup 函数和响应式 API，我们可以更好地复用代码，提高代码的可维护性。本文将从基础概念开始，逐步深入到高级用法，帮助你全面掌握 Composition API 的精髓。我们将探讨 ref、reactive、computed、watch 等核心 API 的使用技巧，以及如何使用组合式函数来抽象和复用逻辑。',
    node: { id: 101, name: 'Composition API 设计理念' },
    views: 1234,
    likes: 156,
    comments: 23,
    publishedAt: '2024-11-01'
  },
  {
    id: 2,
    preview: 'TypeScript 的类型系统是其最核心的特性，也是区别于 JavaScript 的关键所在。本文将系统性地介绍 TypeScript 类型系统的各个方面，包括基础类型、高级类型、泛型、类型推断等内容。我们会通过大量实例来说明如何在实际项目中正确使用类型系统，避免常见的类型错误。同时还会介绍一些高级技巧，如条件类型、映射类型、模板字面量类型等，帮助你写出更加类型安全和优雅的代码。无论你是 TypeScript 初学者还是有一定经验的开发者，都能从本文中获得收获。',
    node: { id: 102, name: '高级类型系统' },
    views: 892,
    likes: 98,
    comments: 15,
    publishedAt: '2024-10-28'
  },
  {
    id: 3,
    preview: '在现代 Web 开发中，性能优化是一个永恒的话题。本文将分享一些在实际项目中总结出的前端性能优化技巧和经验。我们将从多个维度来探讨如何提升 Web 应用的性能，包括资源加载优化、渲染性能优化、JavaScript 执行优化等。文章会介绍一些实用的工具和方法，如使用 Lighthouse 进行性能分析、利用 Chrome DevTools 定位性能瓶颈、使用 Webpack 进行代码分割和懒加载等。同时还会分享一些最佳实践，帮助你在开发过程中就能避免常见的性能问题，打造流畅的用户体验。',
    node: { id: 103, name: '性能分析与优化' },
    views: 2156,
    likes: 234,
    comments: 45,
    publishedAt: '2024-11-05'
  }
])

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

// 编辑文章
const editArticle = (articleId: number) => {
  // TODO: 跳转到编辑页面
  console.log('Edit article:', articleId)
}

// 删除文章
const deleteArticle = (articleId: number) => {
  articleToDelete.value = articleId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = () => {
  if (articleToDelete.value !== null) {
    const index = articles.value.findIndex(a => a.id === articleToDelete.value)
    if (index !== -1) {
      // TODO: 调用 API 删除文章
      articles.value.splice(index, 1)
    }
  }
  articleToDelete.value = null
}
</script>

<style scoped>
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.article-preview {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
}

/* 移动端取消 sticky */
@media (max-width: 960px) {
  .sticky-sidebar {
    position: relative;
    top: 0;
    max-height: none;
    margin-bottom: 16px;
  }
}
</style>
