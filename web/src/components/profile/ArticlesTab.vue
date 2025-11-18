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
      <div class="py-2">
        <div class="d-flex align-center justify-space-between mb-4">
          <v-icon icon="mdi-menu" size="18" color="grey-lighten-1" />
        </div>

        <!-- 文章列表 -->
        <v-infinite-scroll v-if="articles.length > 0" :items="articles" @load="onLoadMore">
          <div v-for="(article, index) in articles" :key="article.id">
            <v-divider v-if="index > 0" class="mb-8" />
            <div class="article-item pb-8" :class="index === 0 ? 'pt-1' : 'pt-0'">
              <div class="d-flex align-start justify-space-between mb-3">
                <div class="flex-grow-1">
                  <!-- 所属课程和节点 -->
                  <div v-if="article.node || article.course" class="mb-3 d-flex align-center ga-2">
                    <v-chip
                      v-if="article.course"
                      size="small"
                      variant="tonal"
                      color="primary"
                      class="cursor-pointer"
                      @click.stop="goToCourse(article.courseId)"
                    >
                      <v-icon icon="mdi-book-outline" size="14" class="mr-1" />
                      {{ article.course }}
                    </v-chip>
                    <v-chip
                      v-if="article.node"
                      size="small"
                      variant="tonal"
                      color="grey-darken-2"
                      class="cursor-pointer"
                      @click.stop="goToNode(article.nodeId)"
                    >
                      <v-icon icon="mdi-file-document-outline" size="14" class="mr-1" />
                      {{ article.node.name }}
                    </v-chip>
                  </div>

                  <!-- 文章内容缩略 -->
                  <router-link
                    v-if="article.id"
                    :to="{ path: '/read', query: { postId: article.id } }"
                    class="text-decoration-none d-block"
                  >
                    <div :ref="(el) => setContentRef(el, index)" class="article-content-preview mb-3" :class="{ 'has-overflow': article.hasOverflow }">
                      <div v-html="article.preview"></div>
                    </div>
                  </router-link>
                  <div v-else :ref="(el) => setContentRef(el, index)" class="article-content-preview mb-3" :class="{ 'has-overflow': article.hasOverflow }">
                    <div v-html="article.preview"></div>
                  </div>

                  <!-- 统计信息 -->
                  <div class="d-flex align-center text-body-2 text-grey" style="gap: 16px">
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
                  color="error"
                  variant="tonal"
                  size="small"
                  icon="mdi-delete"
                  density="comfortable"
                  @click.stop="deleteArticle(article.id)"
                >
                  <v-icon>mdi-delete</v-icon>
                  <v-tooltip activator="parent" location="top">删除文章</v-tooltip>
                </v-btn>
              </div>
            </div>
          </div>

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
import { ref, computed, onMounted, nextTick, watch } from 'vue'
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

// 存储每个文章的溢出状态
const overflowStates = ref<Record<number, boolean>>({})

// 转换文章数据
const articles = computed(() => {
  if (!posts.value) return []

  return posts.value.map((post) => ({
    id: post.id,
    nodeId: post.nodeId,
    preview: post.content, // 使用完整 HTML 内容
    node: post.node ? { id: post.nodeId, name: post.node.name } : undefined,
    course: post.node?.course?.name || undefined,
    courseId: post.node?.course?.id || undefined,
    views: post.viewCount || 0,
    likes: post.helpful || 0,
    comments: post.commentCount || 0,
    publishedAt: post.createdAt || '',
    hasOverflow: overflowStates.value[post.id] || false,
  }))
})

// 检测内容是否溢出
const setContentRef = (el: any, index: number) => {
  if (!el || !articles.value[index]) return

  nextTick(() => {
    // 检测整个容器的 scrollHeight 是否超过 max-height (300px)
    if (el.scrollHeight > 300) {
      const articleId = articles.value[index].id
      overflowStates.value[articleId] = true
    }
  })
}

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

// 跳转到课程
const goToCourse = (courseId: number) => {
  if (courseId) {
    router.push({ path: '/read', query: { courseId } })
  }
}

// 跳转到节点
const goToNode = (nodeId: number) => {
  if (nodeId) {
    router.push({ path: '/read', query: { nodeId } })
  }
}

// 跳转到文章详情（使用 postId）
const goToArticle = (article: any) => {
  if (article.id) {
    router.push({ path: '/read', query: { postId: article.id } })
  }
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
  top: 56px;
  max-height: calc(100vh - 76px);
  overflow-y: auto;
}

/* 文章内容缩略显示 */
.article-content-preview {
  position: relative;
  max-height: 300px;
  overflow: hidden;
  line-height: 1.8;
  color: #1a1a1b;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.article-content-preview:hover {
  background-color: #fafafa;
}

/* 只有溢出时才显示渐变和省略号 */
.article-content-preview.has-overflow {
  padding-bottom: 40px;
}

.article-content-preview.has-overflow::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80px;
  background: linear-gradient(to bottom, transparent 0%, rgba(255, 255, 255, 0.8) 50%, white 100%);
  pointer-events: none;
}

.article-content-preview.has-overflow::after {
  content: '...';
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  color: #666;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 3px;
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
