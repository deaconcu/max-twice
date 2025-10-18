<script setup lang="ts">
import { inject, onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { postServiceV1 } from '@/services/api/v1/apiServiceV1'
import { PostState, PostType } from '@/types/enums'
import type { Post } from '@/types/post'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

const allPostList = ref<Post[]>([])
const currentTab = ref<string>('pending')
const loading = ref<boolean>(false)
const hasMore = ref<boolean>(true)
const pageSize = 20

// 筛选条件
const filterNodeId = ref<number | undefined>(undefined)
const filterCreatorId = ref<number | undefined>(undefined)
const isFilterMode = ref<boolean>(false)

// 标签配置
interface TabConfig {
  key: string
  label: string
  state: PostState
  icon: string
  color: string
}

const tabs: TabConfig[] = [
  {
    key: 'pending',
    label: t('admin.pending'),
    state: PostState.SUBMITTED,
    icon: 'mdi-clock-outline',
    color: 'orange'
  },
  {
    key: 'approved',
    label: t('admin.approved'),
    state: PostState.APPROVED,
    icon: 'mdi-check-circle',
    color: 'green'
  },
  {
    key: 'rejected',
    label: t('admin.rejected'),
    state: PostState.DELETED,
    icon: 'mdi-close-circle',
    color: 'red'
  }
]

// 直接显示当前tab的数据，不需要过滤
const postList = computed<Post[]>(() => {
  return allPostList.value
})

// 根据当前tab获取对应状态的帖子
const getPostsByTab = async (tabKey: string, isLoadMore: boolean = false): Promise<void> => {
  if (loading.value) return

  loading.value = true

  try {
    const lastId = isLoadMore && allPostList.value.length > 0
      ? allPostList.value[allPostList.value.length - 1].id
      : undefined

    const response = await postServiceV1.getPostsByState(tabKey, lastId, pageSize)

    if (response.code === 401) {
      console.log('not login')
    } else if (response.code === 200) {
      const newPosts = response.data as Post[]

      if (isLoadMore) {
        allPostList.value.push(...newPosts)
      } else {
        allPostList.value = newPosts
      }

      // 如果返回的数据少于页面大小，说明没有更多数据了
      hasMore.value = newPosts.length === pageSize
      console.log('done')
    }
  } catch (error) {
    console.error('Error loading posts:', error)
  } finally {
    loading.value = false
  }
}

// 按筛选条件获取帖子
const getPostsByFilter = async (isLoadMore: boolean = false): Promise<void> => {
  if (loading.value) return

  loading.value = true

  try {
    const lastId = isLoadMore && allPostList.value.length > 0
      ? allPostList.value[allPostList.value.length - 1].id
      : undefined

    const response = await postServiceV1.getPostsByFilter(
      filterNodeId.value,
      filterCreatorId.value,
      lastId
    )

    if (response.code === 401) {
      console.log('not login')
    } else if (response.code === 200) {
      const newPosts = response.data as Post[]

      if (isLoadMore) {
        allPostList.value.push(...newPosts)
      } else {
        allPostList.value = newPosts
      }

      hasMore.value = newPosts.length === pageSize
    }
  } catch (error) {
    console.error('Error loading posts:', error)
  } finally {
    loading.value = false
  }
}

// 应用筛选
const applyFilter = async (): Promise<void> => {
  if (!filterNodeId.value && !filterCreatorId.value) {
    showSnackbar?.('请至少输入一个筛选条件', 'warning')
    return
  }
  isFilterMode.value = true
  hasMore.value = true
  await getPostsByFilter()
}

// 清除筛选
const clearFilter = async (): Promise<void> => {
  filterNodeId.value = undefined
  filterCreatorId.value = undefined
  isFilterMode.value = false
  hasMore.value = true
  await getPostsByTab(currentTab.value)
}

// 加载更多数据
const loadMore = async (): Promise<void> => {
  if (hasMore.value && !loading.value) {
    if (isFilterMode.value) {
      await getPostsByFilter(true)
    } else {
      await getPostsByTab(currentTab.value, true)
    }
  }
}

const getPostSensorList = async (): Promise<void> => {
  await getPostsByTab(currentTab.value)
}

const approvePost = async (post: Post, approve: boolean): Promise<void> => {
  try {
    const response = await postServiceV1.approvePost(post.id, approve)

    if (response.code === 401) {
      console.log('not login')
    } else if (response.code === 200) {
      console.log('done')
      console.log(`post: ${JSON.stringify(response.data)}`)

      // 使用后端返回的数据更新本地状态
      const updatedPost = response.data as Post
      const index = allPostList.value.findIndex(p => p.id === post.id)
      if (index !== -1) {
        allPostList.value[index].state = updatedPost.state
      }

      showSnackbar?.(t('admin.operationSuccess'))
    }
  } catch (error) {
    console.error('Error verifying login status:', error)
  }
}

// 监听tab切换，重新加载数据
const handleTabChange = async (newTab: string) => { if (isFilterMode.value) {
    // 如果在筛选模式下，切换tab时清除筛选
    await clearFilter()
  } else {
    hasMore.value = true
    await getPostsByTab(newTab)
  }
}

// 解析目录内容 JSON
const parseContents = (content: string) => {
  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed)) {
      return parsed
    }
    return []
  } catch (e) {
    // 向后兼容：如果解析失败，尝试按逗号分割
    return content.split(',').map((item, index) => ({
      id: index,
      name: item.trim(),
      description: ''
    }))
  }
}

onMounted(() => {
  getPostSensorList()
})
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-note-check-outline" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">
            {{ t('admin.articleReview') }}
          </h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">{{ t('admin.reviewUserArticles') }}</p>
        </div>
      </div>
      <v-chip variant="tonal" color="teal" rounded="lg">
        <v-icon
          icon="mdi-file-document-multiple"
          size="16"
          class="mr-1"
        ></v-icon>
        <span class="text-caption">{{ postList.length }}</span>
      </v-chip>
    </div>

    <!-- 筛选区域 -->
    <v-card v-if="!isFilterMode" flat class="border rounded-lg pa-4 mb-6">
      <div class="text-subtitle-2 font-weight-bold text-grey-darken-3 mb-3">
        <v-icon icon="mdi-filter-outline" size="18" class="mr-2"></v-icon>
        高级筛选
      </div>
      <v-row dense>
        <v-col cols="12" sm="5">
          <v-text-field
            v-model.number="filterNodeId"
            type="number"
            label="节点 ID"
            variant="outlined"
            density="compact"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="5">
          <v-text-field
            v-model.number="filterCreatorId"
            type="number"
            label="用户 ID"
            variant="outlined"
            density="compact"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="2">
          <v-btn
            variant="flat"
            color="primary"
            block
            @click="applyFilter"
          >
            <v-icon icon="mdi-magnify" class="mr-1"></v-icon>
            筛选
          </v-btn>
        </v-col>
      </v-row>
    </v-card>

    <!-- 筛选结果提示 -->
    <v-alert
      v-if="isFilterMode"
      type="info"
      color="teal"
      variant="outlined"
      class="mb-6"
      border="top"
      rounded="lg"
      closable
      @click:close="clearFilter"
    >
      <div class="d-flex align-center justify-space-between">
        <div>
          <span class="font-weight-medium">筛选条件：</span>
          <v-chip v-if="filterNodeId" size="small" class="mx-1">节点 ID: {{ filterNodeId }}</v-chip>
          <v-chip v-if="filterCreatorId" size="small" class="mx-1">用户 ID: {{ filterCreatorId }}</v-chip>
        </div>
      </div>
    </v-alert>

    <!-- 状态标签 -->
    <v-tabs
      v-if="!isFilterMode"
      v-model="currentTab"
      color="primary"
      class="mb-6"
      show-arrows
      @update:model-value="handleTabChange"
    >
      <v-tab
        v-for="tab in tabs"
        :key="tab.key"
        :value="tab.key"
        class="text-none"
      >
        <v-icon
          :icon="tab.icon"
          :color="`${tab.color}-darken-1`"
          size="18"
          class="mr-2"
        ></v-icon>
        {{ tab.label }}
      </v-tab>
    </v-tabs>

    <div v-if="postList.length === 0" class="text-center py-12">
      <v-icon
        icon="mdi-file-document-outline"
        size="48"
        color="grey-lighten-1"
        class="mb-4"
      ></v-icon>
      <p class="text-body-1 text-grey-darken-1">
        {{ currentTab === 'pending' ? t('admin.noArticlesToReview') : `暂无${tabs.find(tab => tab.key === currentTab)?.label}的文章` }}
      </p>
    </div>

    <div
      v-for="post in postList"
      :key="post.id"
      class="mb-4"
      v-intersect="{
        handler: (isIntersecting) => {
          if (isIntersecting && post === postList[postList.length - 1] && hasMore && !loading) {
            loadMore()
          }
        }
      }"
    >
      <v-card flat class="border rounded-lg pa-5" hover>
        <div class="d-flex align-start">
          <!-- 状态和操作区域 -->
          <div class="mr-4 action-area">
            <div class="mb-3">
              <v-chip
                v-if="post.state == PostState.SUBMITTED"
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                {{ t('admin.pending') }}
              </v-chip>
              <v-chip
                v-if="post.state == PostState.APPROVED"
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.approved') }}
              </v-chip>
              <v-chip
                v-if="post.state == PostState.DELETED"
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.rejected') }}
              </v-chip>
            </div>
            <div v-if="post.state == PostState.SUBMITTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="approvePost(post, true)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.approve') }}
              </v-btn>
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="approvePost(post, false)"
              >
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.reject') }}
              </v-btn>
            </div>

            <!-- 已通过状态下显示屏蔽按钮 -->
            <div v-if="post.state == PostState.APPROVED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="approvePost(post, false)"
              >
                <v-icon icon="mdi-block-helper" color="red-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已拒绝状态下显示通过按钮 -->
            <div v-if="post.state == PostState.DELETED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="approvePost(post, true)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.approve') }}
              </v-btn>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-3">
              <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                <v-icon icon="mdi-account" color="grey-darken-1" size="18"></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-2">
                  {{ t('admin.articleId') }}: {{ post.id }}
                </div>
                <div class="text-caption text-grey-darken-1">{{ post.createdAt }}</div>
              </div>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <div
                v-if="post.type == PostType.ARTICLE"
                class="tiptap post-content"
                v-html="post.content"
              ></div>
              <div v-if="post.type == PostType.CONTENTS">
                <div class="text-caption text-grey-darken-1 mb-3">{{ t('admin.directory') }}</div>
                <div class="contents-list">
                  <div
                    v-for="(item, index) in parseContents(post.content)"
                    :key="index"
                    class="content-item mb-3"
                  >
                    <div class="d-flex align-start">
                      <div class="text-body-2 font-weight-medium text-grey-darken-3 mr-2">{{ index + 1 }}.</div>
                      <div class="flex-grow-1">
                        <div class="text-body-2 font-weight-medium text-grey-darken-3">{{ item.name }}</div>
                        <div v-if="item.description" class="text-caption text-grey-darken-1 mt-1">
                          {{ item.description }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </v-card>
    </div>

    <!-- 加载更多指示器 -->
    <div v-if="loading" class="text-center py-4">
      <v-progress-circular
        indeterminate
        color="primary"
        size="24"
      ></v-progress-circular>
      <span class="ml-2 text-grey-darken-1">加载中...</span>
    </div>

    <!-- 没有更多数据提示 -->
    <div v-if="!hasMore && postList.length > 0" class="text-center py-4">
      <span class="text-grey-darken-1">没有更多数据了</span>
    </div>
  </div>
</template>

<style scoped>
  .tiptap.post-content {
    max-height: 200px;
    overflow-y: auto;
  }

  .action-area {
    min-width: 200px;
  }
</style>