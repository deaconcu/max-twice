<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-4">
          <div class="mb-4">
            <h4 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">创建的文章</h4>
            <p class="text-body-2 text-grey mb-0">管理您创作的文章，分享知识和经验。</p>
          </div>
          <v-divider class="my-4" />
          <div class="text-body-2 text-grey">
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-pencil" size="18" color="grey" class="mr-2 mt-1" />
              <span>编辑发布文章</span>
            </div>
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-eye" size="18" color="grey" class="mr-2 mt-1" />
              <span>查看阅读统计</span>
            </div>
            <div class="d-flex align-start">
              <v-icon icon="mdi-tag" size="18" color="grey" class="mr-2 mt-1" />
              <span>添加标签分类</span>
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="py-2">
        <div class="d-flex align-center justify-space-between mb-6">
          <div></div>
        </div>

        <!-- 文章列表 -->
        <v-infinite-scroll v-if="articles.length > 0" :items="articles" @load="onLoadMore">
          <div v-for="(article, index) in articles" :key="article.id">
            <v-card
              rounded="xl"
              hover
              border
              elevation="0"
              class="article-card mb-6 hoverable"
              @click="goToArticle(article)"
            >
              <v-card-text class="pa-6 pb-1">
                <!-- 所属课程和节点 -->
                <div v-if="article.node || article.course" class="mb-4">
                  <div class="d-flex align-center ga-1 flex-wrap">
                    <template v-if="article.course">
                      <v-chip
                        size="small"
                        density="comfortable"
                        color="grey-darken-1"
                        variant="tonal"
                      >
                        课程
                      </v-chip>
                      <v-btn
                        v-if="article.course"
                        variant="text"
                        class="course-link-btn px-2 text-body-1"
                        @click.stop="goToCourse(article.courseId)"
                      >
                        {{ article.course }}
                      </v-btn>
                    </template>
                    <template v-if="article.node">
                      <v-icon
                        icon="mdi-chevron-right"
                        size="18"
                        color="grey-darken-1"
                        class="mx-1"
                      />
                      <v-chip
                        size="small"
                        density="comfortable"
                        color="grey-darken-1"
                        variant="tonal"
                      >
                        节点
                      </v-chip>
                      <v-btn
                        variant="text"
                        class="course-link-btn px-2 text-body-1"
                        @click.stop="goToNode(article.nodeId)"
                      >
                        {{ article.node.name }}
                      </v-btn>
                    </template>
                  </div>
                </div>

                <!-- 文章内容缩略 -->
                <div
                  :ref="(el) => setContentRef(el, index)"
                  class="article-content-preview mb-4"
                  :class="{ 'has-overflow': article.hasOverflow }"
                >
                  <div v-html="article.preview"></div>
                </div>

                <div class="d-flex align-start justify-space-between">
                  <!-- 统计信息 -->
                  <div class="d-flex align-center text-body-2 text-grey" style="gap: 16px">
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-eye-outline" size="16" color="grey" class="mr-1" />
                      {{ article.views }} 阅读
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-heart-outline" size="16" color="grey" class="mr-1" />
                      {{ article.likes }} 点赞
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-comment-outline" size="16" color="grey" class="mr-1" />
                      {{ article.comments }} 评论
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-calendar-outline" size="16" color="grey" class="mr-1" />
                      {{ formatDate(article.publishedAt) }}
                    </div>
                  </div>

                  <!-- 删除按钮 -->
                  <v-btn
                    color="grey"
                    variant="text"
                    size="small"
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
  top: 140px;
  align-self: flex-start;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.article-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: #ffffff;
  border: 1px solid #e9ecef !important;
}

/* 文章内容缩略显示 */
.article-content-preview {
  position: relative;
  max-height: 300px;
  min-height: 100px;
  overflow: hidden;
  line-height: 1.8;
  color: #1a1a1b;
  font-size: 1rem;
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

/* 课程链接按钮样式 */
.course-link-btn {
  font-weight: 600;
  text-transform: none;
  letter-spacing: normal;
  height: auto;
  min-height: 0;
}
</style>
