<template>
  <div class="pa-0 pa-sm-1">
    <!-- 顶部筛选栏 -->
    <div class="d-flex align-center justify-space-between flex-wrap ga-3">
      <!-- 左侧：状态筛选 -->
      <div class="d-flex align-center">
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
          :color="statusFilter === 'published' ? 'primary' : 'default'"
          @click="statusFilter = 'published'"
        >
          {{ t('user.profile.published') }}
        </v-btn>
      </div>

      <!-- 右侧：搜索框 -->
      <v-text-field
        v-model="searchQuery"
        :placeholder="t('user.profile.searchPlaceholder')"
        prepend-inner-icon="mdi-magnify"
        variant="outlined"
        density="compact"
        rounded="lg"
        clearable
        hide-details
        style="max-width: 200px"
      />
    </div>

    <!-- 加载状态 -->
    <LoadingSpinner v-if="loading && filteredCatalogs.length === 0" />

    <!-- 目录列表 -->
    <v-infinite-scroll
      v-else-if="filteredCatalogs.length > 0"
      :items="filteredCatalogs"
      @load="onLoadMore"
    >
      <div v-for="(catalog, index) in filteredCatalogs" :key="catalog.id">
        <v-card
          rounded="lg"
          border
          hover
          class="catalog-card mb-5"
          @click="goToCatalog(catalog.id)"
        >
          <v-card-text class="pa-4 pb-2">
            <!-- 顶部：课程 > 节点 + 状态 -->
            <div class="d-flex align-center justify-space-between mb-3">
              <!-- 左侧：课程和节点路径 -->
              <div
                class="d-flex align-center text-body-2 text-medium-emphasis"
                style="min-width: 0"
              >
                <span
                  v-if="catalog.course"
                  class="text-truncate"
                  style="cursor: pointer"
                  @click.stop="goToCourse(catalog.course.id)"
                >
                  {{ catalog.course.name }}
                </span>
                <v-icon
                  v-if="catalog.course && catalog.node"
                  icon="mdi-chevron-right"
                  size="16"
                  class="mx-1 flex-shrink-0"
                />
                <span v-if="catalog.node" class="text-truncate">{{ catalog.node }}</span>
              </div>

              <!-- 右侧：状态标签 -->
              <v-chip
                v-if="catalog.state === 0"
                size="x-small"
                color="grey"
                variant="tonal"
                class="flex-shrink-0 ml-2"
              >
                {{ t('user.profile.draft') }}
              </v-chip>
              <v-chip
                v-else-if="catalog.state === 1"
                size="x-small"
                color="warning"
                variant="tonal"
                class="flex-shrink-0 ml-2"
              >
                {{ t('admin.pending') }}
              </v-chip>
              <v-chip
                v-else-if="catalog.state === 2"
                size="x-small"
                color="success"
                variant="tonal"
                class="flex-shrink-0 ml-2"
              >
                {{ t('user.profile.published') }}
              </v-chip>
              <v-chip
                v-else-if="catalog.state === 3"
                size="x-small"
                color="error"
                variant="tonal"
                class="flex-shrink-0 ml-2"
              >
                {{ t('admin.rejected') }}
              </v-chip>
              <v-chip
                v-else-if="catalog.state === 4"
                size="x-small"
                color="error"
                variant="tonal"
                class="flex-shrink-0 ml-2"
              >
                {{ t('admin.banned') }}
              </v-chip>
            </div>

            <!-- 主要内容：目录章节列表 -->
            <div v-if="catalog.contentNodes.length > 0" class="catalog-nodes mb-3">
              <div
                v-for="(node, idx) in catalog.contentNodes"
                :key="idx"
                class="catalog-node-item py-2 px-3"
              >
                <div class="text-body-1 text-grey-darken-3">{{ idx + 1 }}. {{ node.name }}</div>
                <div v-if="node.description" class="text-body-2 text-medium-emphasis">
                  {{ node.description }}
                </div>
              </div>
            </div>

            <!-- 底部：统计信息 + 操作按钮 -->
            <div class="d-flex align-center justify-space-between">
              <div class="d-flex align-center text-caption text-grey" style="gap: 12px">
                <span>{{ catalog.commentCount }} {{ t('notification.comment') }}</span>
                <span>{{ catalog.deckCount }} {{ t('review.cards') }}</span>
                <span>{{ catalog.contentNodes.length }} {{ t('learning.nodes') }}</span>
                <span class="d-none d-sm-inline">{{ formatDate(catalog.createdAt) }}</span>
              </div>
              <div class="d-flex align-center">
                <v-btn
                  v-if="catalog.state === 0"
                  color="primary"
                  variant="text"
                  size="x-small"
                  icon="mdi-pencil"
                  @click.stop="editCatalog(catalog)"
                />
                <v-btn
                  color="grey"
                  variant="text"
                  size="x-small"
                  icon="mdi-delete"
                  @click.stop="deleteCatalog(catalog.id)"
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
        <div v-if="!searchQuery" class="text-center py-4">
          <p class="text-body-2 text-grey">{{ t('postingList.reachedEnd') }}</p>
        </div>
      </template>
    </v-infinite-scroll>

    <!-- 空状态 -->
    <div v-else class="text-center py-8 py-md-12">
      <v-icon
        icon="mdi-folder-multiple"
        :size="$vuetify.display.mobile ? 48 : 64"
        color="grey-lighten-2"
        class="mb-3 mb-md-4"
      />
      <p class="text-body-2 text-md-body-1 text-grey-darken-2">
        {{ searchQuery ? t('user.profile.noCatalogsFound') : t('user.profile.noCatalogsCreated') }}
      </p>
      <p class="text-caption text-md-body-2 text-grey">
        {{ searchQuery ? t('user.profile.tryOtherKeywords') : t('user.profile.createCatalogHint') }}
      </p>
    </div>

    <!-- 删除确认对话框 -->
    <ConfirmDialog
      v-model="showDeleteDialog"
      :title="t('common.confirm')"
      :message="t('common.delete') + '?'"
      :confirm-text="t('common.confirm')"
      @confirm="confirmDelete"
    />

    <NodeSelectorDialog
      v-if="editingCatalog"
      ref="nodeSelectorDialog"
      :course-id="editingCatalog.node?.course?.id"
      :node-id="editingCatalog.nodeId"
      :draft-post="editingCatalog"
      @load-data="handleCatalogUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'
import { useI18n } from '@/composables/useI18n'
import { userApi, postApi } from '@/api'
import { PostType } from '@/enums'
import { parseContentNodes } from '@/utils/postUtils'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import NodeSelectorDialog from '@/components/features/read/NodeSelectorDialog.vue'

const { t, locale } = useI18n()
const router = useRouter()

// 搜索关键词
const searchQuery = ref('')

// 状态筛选
const statusFilter = ref('all')

// 删除确认对话框
const showDeleteDialog = ref(false)
const postToDelete = ref<number | null>(null)

// 编辑目录对话框
const nodeSelectorDialog = ref()
const editingCatalog = ref<any>(null)

// 使用 useInfiniteScroll 加载目录列表
const {
  items: posts,
  loading,
  hasMore,
  loadMore: loadMorePosts,
  reset: resetPosts,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const response = await userApi.getCurrentUserAllPosts(params.lastId, PostType.INDEX)
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

// 删除帖子
const { execute: deletePost } = useMutation((postId: number) => postApi.deletePost(postId), {
  successMessage: t('user.profile.catalogDeleted'),
  onSuccess: () => {
    // 从列表中移除已删除的项
    posts.value = posts.value.filter((p) => p.id !== postToDelete.value)
  },
})

// 转换帖子数据为目录格式
const catalogs = computed(() => {
  if (!posts.value) return []

  return posts.value.map((post) => {
    const contentNodes = parseContentNodes(post.content)

    return {
      id: post.id,
      node: post.node?.name, // 节点名称（附加信息）
      contentNodes, // 章节列表（主要内容）
      commentCount: post.commentCount || 0,
      deckCount: post.deckCount || 0,
      createdAt: post.createdAt || '',
      state: post.state, // 添加状态字段
      course: post.node?.course
        ? { id: post.node.course.id, name: post.node.course.name }
        : undefined,
    }
  })
})

// 根据搜索关键词和状态过滤目录
const filteredCatalogs = computed(() => {
  let result = catalogs.value

  // 状态筛选
  if (statusFilter.value === 'draft') {
    result = result.filter((catalog) => catalog.state === 0)
  } else if (statusFilter.value === 'published') {
    result = result.filter((catalog) => catalog.state === 2)
  }

  // 搜索筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter((catalog) => {
      const matchNode = catalog.node?.toLowerCase().includes(query)
      const matchCourse = catalog.course?.name.toLowerCase().includes(query)
      const matchContent = catalog.contentNodes.some(
        (node: { name: string; description?: string }) =>
          node.name.toLowerCase().includes(query) || node.description?.toLowerCase().includes(query)
      )
      return matchNode || matchCourse || matchContent
    })
  }

  return result
})

// 跳转到目录详情 (跳转到节点页面)
const goToCatalog = (postId: number) => {
  const post = posts.value?.find((p) => p.id === postId)
  if (post?.nodeId) {
    router.push(`/node/${post.nodeId}`)
  }
}

// 跳转到课程详情
const goToCourse = (courseId: number) => {
  router.push(`/course/${courseId}`)
}

// 删除目录
const deleteCatalog = (postId: number) => {
  postToDelete.value = postId
  showDeleteDialog.value = true
}

// 编辑目录（只能编辑草稿）
const editCatalog = (catalog: any) => {
  const post = posts.value?.find((p) => p.id === catalog.id)
  if (post?.state === 0) {
    // DRAFT
    editingCatalog.value = post
    nodeSelectorDialog.value?.open()
  }
}

// 目录编辑成功后刷新列表
const handleCatalogUpdated = () => {
  resetPosts()
  loadMorePosts({ done: () => {} })
  editingCatalog.value = null
}

// 确认删除
const confirmDelete = async () => {
  if (postToDelete.value !== null) {
    await deletePost(postToDelete.value)
  }
  postToDelete.value = null
}

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString(locale.value === 'zh' ? 'zh-CN' : 'en-US')
}

// 适配 v-infinite-scroll 的回调接口
type LoadMoreCallback = (status: 'ok' | 'empty') => void

const onLoadMore = async ({ done }: { done: LoadMoreCallback }): Promise<void> => {
  if (!hasMore.value || loading.value || searchQuery.value) {
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
.catalog-card {
  cursor: pointer;
  transition: all 0.2s;
  background-color: rgb(var(--v-theme-surface));
}

.catalog-nodes {
  border-radius: 8px;
  background-color: rgba(var(--v-theme-on-surface), 0.02);
}

.catalog-node-item:not(:last-child) {
  border-bottom: 1px dashed rgb(var(--v-theme-outline));
}

:deep(.v-field__outline) {
  --v-field-border-opacity: 1;
  color: rgb(var(--v-theme-outline));
}
</style>
