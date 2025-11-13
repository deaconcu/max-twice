<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">创建的目录</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">组织和管理您的学习内容集合。</p>
          <v-divider class="my-3" />
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-folder-plus" size="14" class="mr-1" />
              创建内容目录
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-lock" size="14" class="mr-1" />
              公开/私密设置
            </div>
            <div>
              <v-icon icon="mdi-tag-multiple" size="14" class="mr-1" />
              内容分类管理
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

          <!-- 搜索框 -->
          <v-text-field
            v-model="searchQuery"
            placeholder="搜索课程目录..."
            prepend-inner-icon="mdi-magnify"
            variant="outlined"
            density="compact"
            rounded="md"
            clearable
            hide-details
            max-width="400"
          />
        </div>

        <!-- 目录列表 -->
        <div v-if="filteredCatalogs.length > 0">
          <v-row>
            <v-col v-for="catalog in filteredCatalogs" :key="catalog.id" cols="12" md="6">
              <v-card border rounded="lg" hover class="hoverable">
                <v-card-text class="pa-4">
                  <div class="d-flex align-start justify-space-between mb-3">
                    <div
                      class="flex-grow-1"
                      @click="goToCatalog(catalog.id)"
                      style="cursor: pointer"
                    >
                      <h4 class="text-body-1 font-weight-bold mb-1">{{ catalog.name }}</h4>
                      <p class="text-body-2 text-grey mb-0">{{ catalog.description }}</p>
                    </div>
                    <v-btn
                      color="grey"
                      variant="tonal"
                      size="x-small"
                      icon="mdi-delete"
                      @click.stop="deleteCatalog(catalog.id)"
                    >
                      <v-icon>mdi-delete</v-icon>
                      <v-tooltip activator="parent" location="top">删除目录</v-tooltip>
                    </v-btn>
                  </div>

                  <!-- 所属课程 -->
                  <div v-if="catalog.course" class="mb-3">
                    <v-chip
                      size="small"
                      variant="tonal"
                      color="grey-darken-2"
                      @click.stop="goToCourse(catalog.course.id)"
                      class="cursor-pointer"
                    >
                      <v-icon icon="mdi-book-outline" size="14" class="mr-1" />
                      {{ catalog.course.name }}
                    </v-chip>
                  </div>

                  <div class="d-flex align-center justify-space-between text-caption text-grey">
                    <div class="d-flex align-center ga-3">
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-comment-text-outline" size="14" class="mr-1" />
                        {{ catalog.commentCount }} 评论
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-cards-outline" size="14" class="mr-1" />
                        {{ catalog.deckCount }} 卡片组
                      </div>
                    </div>
                    <div>{{ formatDate(catalog.createdAt) }}</div>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { userApi, postApi } from '@/api'
import { PostType } from '@/enums'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()

// 搜索关键词
const searchQuery = ref('')

// 删除确认对话框
const showDeleteDialog = ref(false)
const postToDelete = ref<number | null>(null)

// 获取用户创建的目录（type=CONTENTS）
const {
  data: posts,
  loading,
  execute: fetchPosts,
} = useFetch({
  fetchFn: () => userApi.getCurrentUserAllPosts(undefined, PostType.CONTENTS),
  immediate: true,
  defaultValue: [],
})

// 删除帖子
const { execute: deletePost } = useMutation(
  (postId: number) => postApi.deletePost(postId),
  {
    successMessage: '已删除该目录',
    onSuccess: () => {
      fetchPosts()
    },
  }
)

// 转换帖子数据为目录格式
const catalogs = computed(() => {
  if (!posts.value) return []

  return posts.value.map((post) => ({
    id: post.id,
    name: post.node?.name || '未知节点',
    description: post.content.substring(0, 100) + (post.content.length > 100 ? '...' : ''),
    commentCount: post.commentCount || 0,
    deckCount: post.deckCount || 0,
    createdAt: post.createdAt || '',
    course: post.node?.course
      ? { id: post.node.course.id, name: post.node.course.name }
      : undefined,
  }))
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
</script>

<style scoped>
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
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
