<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">创建的文章</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">管理您创作的文章，分享知识和经验。</p>
          <v-divider class="my-3" />
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-pencil" size="14" class="mr-1" />
              编辑发布文章
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-eye" size="14" class="mr-1" />
              查看阅读统计
            </div>
            <div>
              <v-icon icon="mdi-tag" size="14" class="mr-1" />
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
          <v-icon icon="mdi-menu" size="18" color="grey-lighten-1" />
        </div>

        <!-- 文章列表 -->
        <v-infinite-scroll v-if="articles.length > 0" :items="articles" @load="onLoadMore">
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
                <div class="flex-grow-1" style="cursor: pointer">
                  <!-- 所属节点 -->
                  <div v-if="article.node" class="mb-3">
                    <v-chip
                      size="small"
                      variant="tonal"
                      color="grey-darken-2"
                      class="cursor-pointer"
                    >
                      <v-icon icon="mdi-file-document-outline" size="14" class="mr-1" />
                      {{ article.node.name }}
                    </v-chip>
                  </div>

                  <p class="text-body-2 text-grey-darken-2 mb-3 article-preview">
                    {{ article.preview }}
                  </p>

                  <!-- 统计信息 -->
                  <div class="d-flex align-center text-caption text-grey" style="gap: 16px">
                    <div>
                      <v-icon icon="mdi-eye-outline" size="14" class="mr-1" />
                      {{ article.views }} 阅读
                    </div>
                    <div>
                      <v-icon icon="mdi-heart-outline" size="14" class="mr-1" />
                      {{ article.likes }} 点赞
                    </div>
                    <div>
                      <v-icon icon="mdi-comment-outline" size="14" class="mr-1" />
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

          <template #loading>
            <div class="text-center py-4">
              <v-progress-circular indeterminate color="primary" size="32" />
            </div>
          </template>

          <template #empty>
            <div class="text-center py-4">
              <p class="text-body-2 text-grey">已加载所有数据</p>
            </div>
          </template>
        </v-infinite-scroll>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-file-document-multiple" size="64" color="grey-lighten-2" class="mb-4" />
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'
import { userApi, postApi } from '@/api'
import { PostType } from '@/enums'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()

// 删除确认对话框
const showDeleteDialog = ref(false)
const articleToDelete = ref<number | null>(null)

// 使用 useInfiniteScroll 加载文章列表
const {
  items: posts,
  loading,
  hasMore,
  loadMore: loadMorePosts,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const response = await userApi.getCurrentUserAllPosts(params.lastId, PostType.ARTICLE)
    return {
      code: response.code,
      data: response.data?.items || [],
      message: response.message || '',
      hasMore: response.data?.hasMore || false,
    }
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: { lastId: undefined },
})

// 删除文章
const { execute: deletePost } = useMutation((postId: number) => postApi.deletePost(postId), {
  successMessage: '已删除该文章',
  onSuccess: () => {
    // 从列表中移除已删除的项
    posts.value = posts.value.filter((p) => p.id !== articleToDelete.value)
  },
})

// 转换文章数据
const articles = computed(() => {
  if (!posts.value) return []

  return posts.value.map((post) => ({
    id: post.id,
    preview: post.content.substring(0, 150) + (post.content.length > 150 ? '...' : ''),
    node: post.node ? { id: post.nodeId, name: post.node.name } : undefined,
    views: post.viewCount || 0,
    likes: post.helpful || 0,
    comments: post.commentCount || 0,
    publishedAt: post.createdAt || '',
  }))
})

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

// 删除文章
const deleteArticle = (articleId: number) => {
  articleToDelete.value = articleId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (articleToDelete.value !== null) {
    await deletePost(articleToDelete.value)
  }
  articleToDelete.value = null
}

// 适配 v-infinite-scroll 的回调接口
type LoadMoreCallback = (status: 'ok' | 'empty') => void

const onLoadMore = async ({ done }: { done: LoadMoreCallback }): Promise<void> => {
  if (!hasMore.value || loading.value) {
    done('empty')
    return
  }

  await loadMorePosts({ done: () => {} })
  done(hasMore.value ? 'ok' : 'empty')
}

onMounted(() => {
  // 加载第一页数据
  loadMorePosts({ done: () => {} })
})
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
