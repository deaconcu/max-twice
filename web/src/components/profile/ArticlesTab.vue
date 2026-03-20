<template>
  <div class="pa-0 pa-sm-2">
        <!-- 加载状态 -->
        <LoadingSpinner v-if="loading && articles.length === 0" />

        <!-- 文章列表 -->
        <v-infinite-scroll v-if="articles.length > 0" :items="articles" @load="onLoadMore">
          <div v-for="(article, index) in articles" :key="article.id">
            <v-card
              rounded="lg"
              hover
              border
              class="article-card mb-5"
              @click="goToArticle(article)"
            >
              <v-card-text class="pa-4 pb-2">
                <!-- 顶部：课程 > 节点 + 状态 -->
                <div class="d-flex align-center justify-space-between mb-3">
                  <!-- 左侧：课程和节点路径 -->
                  <div class="d-flex align-center text-body-2 text-medium-emphasis" style="min-width: 0">
                    <span v-if="article.course" class="text-truncate" @click.stop="goToCourse(article.courseId)" style="cursor: pointer">
                      {{ article.course }}
                    </span>
                    <v-icon v-if="article.course && article.node" icon="mdi-chevron-right" size="16" class="mx-1 flex-shrink-0" />
                    <span v-if="article.node" class="text-truncate" @click.stop="goToNode(article.nodeId)" style="cursor: pointer">
                      {{ article.node.name }}
                    </span>
                  </div>

                  <!-- 右侧：状态标签 -->
                  <v-chip
                    v-if="article.state === 0"
                    size="x-small"
                    color="grey"
                    variant="tonal"
                    class="flex-shrink-0 ml-2"
                  >
                    草稿
                  </v-chip>
                  <v-chip
                    v-else-if="article.state === 1"
                    size="x-small"
                    color="warning"
                    variant="tonal"
                    class="flex-shrink-0 ml-2"
                  >
                    待审核
                  </v-chip>
                  <v-chip
                    v-else-if="article.state === 2"
                    size="x-small"
                    color="success"
                    variant="tonal"
                    class="flex-shrink-0 ml-2"
                  >
                    已发布
                  </v-chip>
                  <v-chip
                    v-else-if="article.state === 3"
                    size="x-small"
                    color="error"
                    variant="tonal"
                    class="flex-shrink-0 ml-2"
                  >
                    已拒绝
                  </v-chip>
                  <v-chip
                    v-else-if="article.state === 4"
                    size="x-small"
                    color="error"
                    variant="tonal"
                    class="flex-shrink-0 ml-2"
                  >
                    已屏蔽
                  </v-chip>
                </div>

                <!-- 文章内容缩略 -->
                <div
                  :ref="(el) => setContentRef(el, index)"
                  class="article-content-preview mb-3"
                  :class="{ 'has-overflow': article.hasOverflow }"
                >
                  <div v-html="article.preview"></div>
                </div>

                <!-- 底部：统计信息 + 操作按钮 -->
                <div class="d-flex align-center justify-space-between">
                  <div class="d-flex align-center text-caption text-grey" style="gap: 12px">
                    <span>{{ article.viewCount }} 阅读</span>
                    <span>{{ article.likeCount }} 点赞</span>
                    <span>{{ article.commentCount }} 评论</span>
                    <span class="d-none d-sm-inline">{{ formatDate(article.publishedAt) }}</span>
                  </div>
                  <div v-if="isOwnProfile" class="d-flex align-center">
                    <v-btn
                      color="primary"
                      variant="text"
                      size="x-small"
                      icon="mdi-pencil"
                      @click.stop="editArticle(article)"
                    />
                    <v-btn
                      color="grey"
                      variant="text"
                      size="x-small"
                      icon="mdi-delete"
                      @click.stop="deleteArticle(article.id)"
                    />
                  </div>
                </div>
              </v-card-text>
            </v-card>
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
        <div v-else class="text-center py-8 py-md-12">
          <v-icon
            icon="mdi-file-document-multiple"
            :size="$vuetify.display.mobile ? 48 : 64"
            color="grey-lighten-2"
            class="mb-3 mb-md-4"
          />
          <p class="text-body-2 text-md-body-1 text-grey-darken-2">暂无创建的文章</p>
          <p class="text-caption text-md-body-2 text-grey">分享您的学习心得和经验</p>
        </div>

        <!-- 文章编辑对话框 -->
        <ArticleEditModal
          v-model="showEditModal"
          :article="editingArticle"
          @success="handleArticleSuccess"
          @cancel="handleCancelEdit"
        />

    <ConfirmDialog
      v-model="showDeleteDialog"
      title="确认删除"
      message="确定要删除该文章吗？此操作不可恢复。"
      confirm-text="确认删除"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'
import { userApi, postApi } from '@/api'
import { PostType, ContentState } from '@/enums'

interface Props {
  userId?: number | null
  isOwnProfile?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  userId: null,
  isOwnProfile: false,
})
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import ArticleEditModal from '@/components/profile/ArticleEditModal.vue'

const router = useRouter()

// 编辑功能状态
const showEditModal = ref(false)
const editingArticle = ref<any>(null)

// 删除确认对话框
const showDeleteDialog = ref(false)
const articleToDelete = ref<number | null>(null)

// 使用 useInfiniteScroll 加载文章列表
const {
  items: posts,
  loading,
  hasMore,
  loadMore: loadMorePosts,
  reset: resetPosts,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    if (props.isOwnProfile || props.userId === null) {
      // 获取当前用户的文章
      const response = await userApi.getCurrentUserAllPosts(params.lastId, PostType.ARTICLE)
      return {
        code: response.code,
        data: response.data?.items || [],
        message: response.message || '',
        hasMore: response.data?.hasMore || false,
      }
    } else {
      // 获取指定用户的文章
      const response = await userApi.getUserPosts(props.userId, params.lastId, PostType.ARTICLE)
      return {
        code: response.code,
        data: response.data || [],
        message: response.message || '',
        hasMore: response.data.length === 20, // 假设每页20条，实际应该从后端返回
      }
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

// 文章编辑/创建成功后的处理
const handleArticleSuccess = (updatedArticle: any) => {
  console.log('handleArticleSuccess called')
  console.log('updatedArticle:', updatedArticle)
  console.log('editingArticle.value:', editingArticle.value)

  if (!updatedArticle) {
    console.log('No updatedArticle, just closing dialog')
    // 创建并直接发布：文章待审核，列表不变，只关闭对话框
    showEditModal.value = false
    editingArticle.value = null
    return
  }

  if (editingArticle.value) {
    // 编辑模式：只更新内容和状态，保留原有的 node、course 等信息
    const index = posts.value.findIndex((p) => p.id === updatedArticle.id)
    console.log('Editing mode, found index:', index)
    if (index !== -1) {
      // 只更新变化的字段，保留原有的嵌套结构
      posts.value[index].content = updatedArticle.content
      posts.value[index].state = updatedArticle.state
      posts.value[index].updatedAt = updatedArticle.updatedAt
      console.log('After update, state:', posts.value[index].state)
    }
  } else {
    // 创建模式保存草稿：添加到列表顶部
    console.log('Creating new draft, adding to top')
    posts.value.unshift(updatedArticle)
  }

  // 关闭编辑对话框
  showEditModal.value = false
  editingArticle.value = null
}

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
    viewCount: post.viewCount || 0,
    likeCount: post.likeCount || 0,
    commentCount: post.commentCount || 0,
    publishedAt: post.createdAt || '',
    hasOverflow: overflowStates.value[post.id] || false,
    state: post.state, // 添加状态字段
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
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
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

// 编辑文章
const editArticle = (article: any) => {
  editingArticle.value = {
    id: article.id,
    preview: article.preview,
    state: article.state,
    node: article.node,
    course: article.course,
    courseId: article.courseId,
  }
  showEditModal.value = true
}

// 取消编辑
const handleCancelEdit = () => {
  showEditModal.value = false
  editingArticle.value = null
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
.article-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline)) !important;
}

/* 文章内容缩略显示 */
.article-content-preview {
  position: relative;
  max-height: 300px;
  min-height: 80px;
  overflow: hidden;
  line-height: 1.8;
  color: rgb(var(--v-theme-on-surface));
  font-size: 0.875rem;
}

@media (min-width: 600px) {
  .article-content-preview {
    min-height: 100px;
    font-size: 1rem;
  }
}

/* 只有溢出时才显示渐变和省略号 */
.article-content-preview.has-overflow {
  padding-bottom: 48px;
}

.article-content-preview.has-overflow::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(
    to bottom,
    transparent 0%,
    rgba(var(--v-theme-surface-rgb), 0.8) 50%,
    rgb(var(--v-theme-surface)) 100%
  );
  pointer-events: none;
}

@media (min-width: 600px) {
  .article-content-preview.has-overflow::before {
    height: 80px;
  }
}

.article-content-preview.has-overflow::after {
  content: '...';
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  color: rgb(var(--v-theme-on-surface-variant));
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 3px;
}

@media (min-width: 600px) {
  .article-content-preview.has-overflow::after {
    font-size: 20px;
  }
}
</style>
