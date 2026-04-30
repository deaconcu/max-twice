<template>
  <div class="pa-0 pa-sm-1">
    <!-- 顶部筛选栏（仅自己的 profile 显示）-->
    <div v-if="isOwnProfile" class="d-flex align-center mb-4">
      <v-btn
        variant="text"
        size="small"
        rounded="lg"
        :color="statusFilter === 'all' ? 'primary' : 'default'"
        @click="statusFilter = 'all'"
      >
        {{ t('user.profile.all') }}
      </v-btn>
      <v-btn
        variant="text"
        size="small"
        rounded="lg"
        :color="statusFilter === 'draft' ? 'primary' : 'default'"
        @click="statusFilter = 'draft'"
      >
        {{ t('user.profile.draft') }}
      </v-btn>
      <v-btn
        variant="text"
        size="small"
        rounded="lg"
        :color="statusFilter === 'pending' ? 'primary' : 'default'"
        @click="statusFilter = 'pending'"
      >
        {{ t('user.profile.pending') }}
      </v-btn>
      <v-btn
        variant="text"
        size="small"
        rounded="lg"
        :color="statusFilter === 'published' ? 'primary' : 'default'"
        @click="statusFilter = 'published'"
      >
        {{ t('user.profile.published') }}
      </v-btn>
      <v-btn
        variant="text"
        size="small"
        rounded="lg"
        :color="statusFilter === 'rejected' ? 'primary' : 'default'"
        @click="statusFilter = 'rejected'"
      >
        {{ t('user.profile.rejected') }}
      </v-btn>
    </div>

    <!-- 加载状态 -->
    <LoadingSpinner v-if="loading && articles.length === 0" />

    <!-- 文章列表 -->
    <v-infinite-scroll v-if="articles.length > 0" :items="articles" @load="onLoadMore">
      <div v-for="(article, index) in articles" :key="article.id">
        <v-card rounded="lg" hover border class="article-card mb-5" @click="goToArticle(article)">
          <v-card-text class="pa-4 pb-2">
            <!-- 顶部：课程 > 节点 + 状态 -->
            <div class="d-flex align-center justify-space-between mb-3">
              <!-- 左侧：课程和节点路径 -->
              <div
                class="d-flex align-center text-body-2 text-medium-emphasis"
                style="min-width: 0"
              >
                <span
                  v-if="article.course"
                  class="text-truncate"
                  style="cursor: pointer"
                  @click.stop="goToCourse(article.courseId)"
                >
                  {{ article.course }}
                </span>
                <v-icon
                  v-if="article.course && article.node"
                  icon="mdi-chevron-right"
                  size="16"
                  class="mx-1 flex-shrink-0"
                />
                <span
                  v-if="article.node"
                  class="text-truncate"
                  style="cursor: pointer"
                  @click.stop="goToNode(article.nodeId)"
                >
                  {{ article.node.name }}
                </span>
              </div>

              <!-- 右侧：状态标签（仅自己的 profile 显示）-->
              <template v-if="isOwnProfile">
                <v-chip
                  v-if="article.state === 0"
                  size="x-small"
                  color="grey"
                  variant="tonal"
                  class="flex-shrink-0 ml-2"
                >
                  {{ t('user.profile.draft') }}
                </v-chip>
                <v-chip
                  v-else-if="article.state === 1"
                  size="x-small"
                  color="warning"
                  variant="tonal"
                  class="flex-shrink-0 ml-2"
                >
                  {{ t('admin.pending') }}
                </v-chip>
                <v-chip
                  v-else-if="article.state === 2"
                  size="x-small"
                  color="success"
                  variant="tonal"
                  class="flex-shrink-0 ml-2"
                >
                  {{ t('user.profile.published') }}
                </v-chip>
                <v-chip
                  v-else-if="article.state === 3"
                  size="x-small"
                  color="error"
                  variant="tonal"
                  class="flex-shrink-0 ml-2"
                >
                  {{ t('admin.rejected') }}
                </v-chip>
                <v-chip
                  v-else-if="article.state === 4"
                  size="x-small"
                  color="error"
                  variant="tonal"
                  class="flex-shrink-0 ml-2"
                >
                  {{ t('admin.banned') }}
                </v-chip>
              </template>
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
                <span>{{ article.viewCount }} {{ t('userStats.views') }}</span>
                <span>{{ article.likeCount }} {{ t('comment.upvote') }}</span>
                <span>{{ article.commentCount }} {{ t('notification.comment') }}</span>
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
          <p class="text-body-2 text-grey">{{ t('postingList.reachedEnd') }}</p>
        </div>
      </template>
    </v-infinite-scroll>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="text-center py-8 py-md-12">
      <v-icon
        icon="mdi-file-document-multiple"
        :size="$vuetify.display.mobile ? 48 : 64"
        color="grey-lighten-2"
        class="mb-3 mb-md-4"
      />
      <p class="text-body-2 text-md-body-1 text-grey-darken-2">
        {{
          statusFilter !== 'all'
            ? t('user.profile.noArticlesFound')
            : t('user.profile.noArticlesCreated')
        }}
      </p>
      <p class="text-caption text-md-body-2 text-grey">
        {{
          statusFilter !== 'all'
            ? t('user.profile.adjustFilters')
            : t('user.profile.shareExperience')
        }}
      </p>
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
      :title="t('common.confirm')"
      :message="t('common.delete') + '?'"
      :confirm-text="t('common.confirm')"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import { useMyPostsQuery, useUserPostsQuery } from '@/queries/user'
import { useDeletePostMutation } from '@/queries/post'
import { userKeys } from '@/queries/keys'
import { useI18n } from '@/composables/useI18n'
import { getGlobalSnackbar } from '@/composables/config'
import { PostType, ContentState } from '@/enums'

const props = withDefaults(defineProps<Props>(), {
  userId: null,
  isOwnProfile: false,
})

const { t, locale } = useI18n()

interface Props {
  userId?: number | null
  isOwnProfile?: boolean
}

import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import ArticleEditModal from '@/components/profile/ArticleEditModal.vue'

const router = useRouter()
const queryClient = useQueryClient()

// 状态筛选
const statusFilter = ref<'all' | 'draft' | 'pending' | 'published' | 'rejected'>('all')

// 将 statusFilter 转换为后端 state 值
const stateValue = computed((): number | undefined => {
  switch (statusFilter.value) {
    case 'draft':
      return ContentState.DRAFT
    case 'pending':
      return ContentState.SUBMITTED
    case 'published':
      return ContentState.PUBLISHED
    case 'rejected':
      return ContentState.REJECTED
    default:
      return undefined
  }
})

const isOwn = computed(() => props.isOwnProfile || props.userId === null)

// 自己的文章
const {
  data: myPostsData,
  isLoading: myPostsLoading,
  hasNextPage: myHasMore,
  fetchNextPage: myFetchNext,
  isFetchingNextPage: myFetchingNext,
} = useMyPostsQuery(
  computed(() => PostType.ARTICLE),
  stateValue,
  isOwn
)

// 他人的文章
const {
  data: userPostsData,
  isLoading: userPostsLoading,
  hasNextPage: userHasMore,
  fetchNextPage: userFetchNext,
  isFetchingNextPage: userFetchingNext,
} = useUserPostsQuery(
  computed(() => props.userId ?? 0),
  PostType.ARTICLE,
  computed(() => !isOwn.value && !!props.userId)
)

const posts = computed(() => {
  if (isOwn.value) {
    return myPostsData.value?.pages.flatMap((p) => p.items) ?? []
  }
  return userPostsData.value?.pages.flatMap((p) => p.items) ?? []
})

const loading = computed(() => (isOwn.value ? myPostsLoading.value : userPostsLoading.value))
const hasMore = computed(() => (isOwn.value ? !!myHasMore.value : !!userHasMore.value))

// 加载更多（适配 v-infinite-scroll）
type LoadMoreCallback = (status: 'ok' | 'empty') => void
const onLoadMore = async ({ done }: { done: LoadMoreCallback }): Promise<void> => {
  if (!hasMore.value) {
    done('empty')
    return
  }
  if (isOwn.value) await myFetchNext()
  else await userFetchNext()
  done(hasMore.value ? 'ok' : 'empty')
}

// 编辑功能状态
const showEditModal = ref(false)
const editingArticle = ref<any>(null)

// 删除确认对话框
const showDeleteDialog = ref(false)
const articleToDelete = ref<number | null>(null)

// 删除文章
const { mutate: deletePostMutate } = useDeletePostMutation()

const deletePost = (postId: number) => {
  deletePostMutate(postId, {
    onSuccess: () => {
      getGlobalSnackbar()?.(t('user.profile.articleDeleted'), 'success')
      void queryClient.invalidateQueries({ queryKey: userKeys.myPosts() })
    },
  })
}

// 文章编辑/创建成功后的处理
const handleArticleSuccess = (_updatedArticle: unknown) => {
  void queryClient.invalidateQueries({ queryKey: userKeys.myPosts() })
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
  return d.toLocaleString(locale.value === 'zh' ? 'zh-CN' : 'en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

// 跳转到课程
const goToCourse = (courseId: number | undefined) => {
  if (courseId) {
    router.push({ path: '/read', query: { courseId } })
  }
}

// 跳转到节点
const goToNode = (nodeId: number | undefined) => {
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
const confirmDelete = () => {
  if (articleToDelete.value !== null) {
    deletePost(articleToDelete.value)
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
</script>

<style scoped>
.article-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: rgb(var(--v-theme-surface));
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
  padding: 12px;
  border-radius: 8px;
  background-color: rgba(var(--v-theme-on-surface), 0.02);
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
  height: 100px;
  background: linear-gradient(to bottom, transparent 0%, rgb(var(--v-theme-surface)) 100%);
  pointer-events: none;
}

@media (min-width: 600px) {
  .article-content-preview.has-overflow::before {
    height: 120px;
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

:deep(.v-infinite-scroll__side) {
  padding: 0;
}

:deep(.v-field__outline) {
  --v-field-border-opacity: 1;
  color: rgb(var(--v-theme-outline));
}
</style>
