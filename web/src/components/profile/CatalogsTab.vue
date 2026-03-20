<template>
  <div class="pa-0 pa-sm-2">
    <!-- 顶部搜索栏 -->
        <div class="mb-2">
          <!-- 搜索框 -->
          <v-text-field
            v-model="searchQuery"
            placeholder="搜索目录..."
            prepend-inner-icon="mdi-magnify"
            variant="outlined"
            density="compact"
            rounded="lg"
            clearable
            hide-details
            style="max-width: 300px"
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
                  <div class="d-flex align-center text-body-2 text-medium-emphasis" style="min-width: 0">
                    <span v-if="catalog.course" class="text-truncate" @click.stop="goToCourse(catalog.course.id)" style="cursor: pointer">
                      {{ catalog.course.name }}
                    </span>
                    <v-icon v-if="catalog.course && catalog.node" icon="mdi-chevron-right" size="16" class="mx-1 flex-shrink-0" />
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
                    草稿
                  </v-chip>
                  <v-chip
                    v-else-if="catalog.state === 1"
                    size="x-small"
                    color="warning"
                    variant="tonal"
                    class="flex-shrink-0 ml-2"
                  >
                    待审核
                  </v-chip>
                  <v-chip
                    v-else-if="catalog.state === 2"
                    size="x-small"
                    color="success"
                    variant="tonal"
                    class="flex-shrink-0 ml-2"
                  >
                    已发布
                  </v-chip>
                  <v-chip
                    v-else-if="catalog.state === 3"
                    size="x-small"
                    color="error"
                    variant="tonal"
                    class="flex-shrink-0 ml-2"
                  >
                    已拒绝
                  </v-chip>
                  <v-chip
                    v-else-if="catalog.state === 4"
                    size="x-small"
                    color="error"
                    variant="tonal"
                    class="flex-shrink-0 ml-2"
                  >
                    已屏蔽
                  </v-chip>
                </div>

                <!-- 主要内容：目录章节列表 -->
                <div v-if="catalog.contentNodes.length > 0" class="catalog-nodes mb-3">
                  <div
                    v-for="(node, idx) in catalog.contentNodes"
                    :key="idx"
                    class="catalog-node-item py-2 px-3"
                  >
                    <div class="text-body-1 text-grey-darken-3">
                      {{ idx + 1 }}. {{ node.name }}
                    </div>
                    <div
                      v-if="node.description"
                      class="text-body-2 text-medium-emphasis"
                    >
                      {{ node.description }}
                    </div>
                  </div>
                </div>

                <!-- 底部：统计信息 + 操作按钮 -->
                <div class="d-flex align-center justify-space-between">
                  <div class="d-flex align-center text-caption text-grey" style="gap: 12px">
                    <span>{{ catalog.commentCount }} 评论</span>
                    <span>{{ catalog.deckCount }} 卡片</span>
                    <span>{{ catalog.contentNodes.length }} 章节</span>
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
              <p class="text-body-2 text-grey">已加载所有数据</p>
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
            {{ searchQuery ? '未找到匹配的目录' : '暂无创建的目录' }}
          </p>
          <p class="text-caption text-md-body-2 text-grey">
            {{ searchQuery ? '尝试使用其他关键词搜索' : '创建目录来组织您的学习内容' }}
          </p>
        </div>

        <!-- 删除确认对话框 -->
        <ConfirmDialog
          v-model="showDeleteDialog"
          title="确认删除"
          message="确定要删除该目录吗？此操作不可恢复。"
          confirm-text="确认删除"
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
import { userApi, postApi } from '@/api'
import { PostType } from '@/enums'
import { parseContentNodes } from '@/utils/postUtils'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import NodeSelectorDialog from '@/components/features/read/NodeSelectorDialog.vue'

const router = useRouter()

// 搜索关键词
const searchQuery = ref('')

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
  successMessage: '已删除该目录',
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

// 根据搜索关键词过滤目录
const filteredCatalogs = computed(() => {
  if (!searchQuery.value) {
    return catalogs.value
  }

  const query = searchQuery.value.toLowerCase()
  return catalogs.value.filter((catalog) => {
    const matchName = catalog.name.toLowerCase().includes(query)
    const matchDescription = catalog.description.toLowerCase().includes(query)
    const matchCourse = catalog.course?.name.toLowerCase().includes(query)

    return matchName || matchDescription || matchCourse
  })
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
  if (post && post.state === 0) {
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
  return new Date(date).toLocaleDateString('zh-CN')
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

.catalog-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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
