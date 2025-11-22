<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-4">
          <div class="mb-4">
            <h4 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">创建的目录</h4>
            <p class="text-body-2 text-grey mb-0">
              组织和管理您的学习内容集合。
            </p>
          </div>
          <v-divider class="my-4" />
          <div class="text-body-2 text-grey">
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-folder-plus" size="18" color="grey" class="mr-2 mt-1" />
              <span>创建内容目录</span>
            </div>
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-lock" size="18" color="grey" class="mr-2 mt-1" />
              <span>公开/私密设置</span>
            </div>
            <div class="d-flex align-start">
              <v-icon icon="mdi-tag-multiple" size="18" color="grey" class="mr-2 mt-1" />
              <span>内容分类管理</span>
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-6">
          <div></div>

          <!-- 搜索框 -->
          <v-text-field
            v-model="searchQuery"
            placeholder="搜索课程目录..."
            prepend-inner-icon="mdi-magnify"
            variant="outlined"
            density="compact"
            rounded="lg"
            clearable
            hide-details
            max-width="400"
          />
        </div>

        <!-- 目录列表 -->
        <v-infinite-scroll
          v-if="filteredCatalogs.length > 0"
          :items="filteredCatalogs"
          @load="onLoadMore"
        >
          <div v-for="(catalog, index) in filteredCatalogs" :key="catalog.id">
            <v-card rounded="xl" border class="catalog-card mb-6 hoverable" @click="goToCatalog(catalog.id)">
              <v-card-text class="pa-6 pb-1">
                <!-- 所属课程和节点 -->
                <div v-if="catalog.node || catalog.course" class="mb-4">
                  <div class="d-flex align-center ga-1 flex-wrap">
                    <template v-if="catalog.course">
                      <v-chip
                        size="small"
                        density="comfortable"
                        color="grey-darken-1"
                        variant="tonal"
                      >
                        课程
                      </v-chip>
                      <v-btn
                        variant="text"
                        class="course-link-btn px-2 text-body-1"
                        @click.stop="goToCourse(catalog.course.id)"
                      >
                        {{ catalog.course.name }}
                      </v-btn>
                    </template>
                    <template v-if="catalog.node">
                      <v-icon icon="mdi-chevron-right" size="18" color="grey-darken-1" class="mx-1" />
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
                        @click.stop="goToCatalog(catalog.id)"
                      >
                        {{ catalog.node }}
                      </v-btn>
                    </template>
                  </div>
                </div>

                <!-- 主要内容：目录章节列表 -->
                <div v-if="catalog.contentNodes.length > 0" class="catalog-nodes mb-4">
                  <div
                    v-for="(node, idx) in catalog.contentNodes"
                    :key="idx"
                    class="catalog-node-item py-3 px-4"
                  >
                    <div class="text-body-1 text-grey-darken-3 mb-1">
                      {{ idx + 1 }}. {{ node.name }}
                    </div>
                    <div v-if="node.description" class="text-body-2 text-grey-darken-1">
                      {{ node.description }}
                    </div>
                  </div>
                </div>

                <div class="d-flex align-start justify-space-between">
                  <!-- 统计信息 -->
                  <div class="d-flex align-center text-body-2 text-grey" style="gap: 16px">
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-comment-text-outline" size="16" color="grey" class="mr-1" />
                      {{ catalog.commentCount }} 评论
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-cards-outline" size="16" color="grey" class="mr-1" />
                      {{ catalog.deckCount }} 卡片组
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-file-document-multiple-outline" size="16" color="grey" class="mr-1" />
                      {{ catalog.contentNodes.length }} 章节
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-calendar-outline" size="16" color="grey" class="mr-1" />
                      {{ formatDate(catalog.createdAt) }}
                    </div>
                  </div>

                  <!-- 删除按钮 -->
                  <v-btn
                    color="grey"
                    variant="text"
                    size="small"
                    icon="mdi-delete"
                    @click.stop="deleteCatalog(catalog.id)"
                  >
                    <v-icon>mdi-delete</v-icon>
                    <v-tooltip activator="parent" location="top">删除目录</v-tooltip>
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
            <div v-if="!searchQuery" class="text-center py-4">
              <p class="text-body-2 text-grey">已加载所有数据</p>
            </div>
          </template>
        </v-infinite-scroll>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-folder-multiple" size="64" color="grey-lighten-2" class="mb-4" />
          <p class="text-body-1 text-grey-darken-2">
            {{ searchQuery ? '未找到匹配的目录' : '暂无创建的目录' }}
          </p>
          <p class="text-body-2 text-grey">
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
import { parseContentNodes } from '@/utils/postUtils'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()

// 搜索关键词
const searchQuery = ref('')

// 删除确认对话框
const showDeleteDialog = ref(false)
const postToDelete = ref<number | null>(null)

// 使用 useInfiniteScroll 加载目录列表
const {
  items: posts,
  loading,
  hasMore,
  loadMore: loadMorePosts,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const response = await userApi.getCurrentUserAllPosts(params.lastId, PostType.CONTENTS)
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
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 140px;
  align-self: flex-start;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.catalog-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: #ffffff;
  border: 1px solid #e9ecef !important;
}

/* 目录章节列表容器 */
.catalog-nodes {
  border-radius: 12px;
  overflow: hidden;
  min-height: 100px;
}

/* 章节项 */
.catalog-node-item {
  transition: all 0.2s ease;
}

.catalog-node-item:hover {
  background-color: #fafafa;
}

.catalog-node-item:not(:last-child) {
  border-bottom: 1px dashed #eeeeee;
}

/* 课程链接按钮样式 */
.course-link-btn {
  font-weight: 600;
  text-transform: none;
  letter-spacing: normal;
  height: auto;
  min-height: 0;
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
