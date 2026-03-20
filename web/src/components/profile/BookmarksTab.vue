<script lang="ts">
export default {
  name: 'BookmarksTab',
}
</script>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { bookmarkApi, type ContentType, type Bookmark } from '@/api/modules/bookmark'

// 当前激活的标签
const activeType = ref<ContentType>('profession')

// 收藏数据（统一存储 BookmarkDTO）
const bookmarks = ref<Record<ContentType, Bookmark[]>>({
  profession: [],
  course: [],
  roadmap: [],
  post: [],
  memory_card: [],
})

// 加载状态
const loading = ref(false)
const loadingMore = ref(false)

// 分页参数
const lastIds = ref<Record<ContentType, number | undefined>>({
  profession: undefined,
  course: undefined,
  roadmap: undefined,
  post: undefined,
  memory_card: undefined,
})

const hasMore = ref<Record<ContentType, boolean>>({
  profession: true,
  course: true,
  roadmap: true,
  post: true,
  memory_card: true,
})

// 当前类型的数据
const currentItems = computed(() => {
  return bookmarks.value[activeType.value] || []
})

// 加载收藏列表
async function loadBookmarks(type: ContentType, append = false) {
  if (!append) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const lastId = append ? lastIds.value[type] : undefined
    const response = await bookmarkApi.getBookmarks(type, lastId, 20)

    if (response.data) {
      const items = response.data

      // 更新数据
      if (!append) {
        bookmarks.value[type] = items
      } else {
        bookmarks.value[type].push(...items)
      }

      // 更新分页参数
      if (items.length > 0) {
        lastIds.value[type] = items[items.length - 1].id
        hasMore.value[type] = items.length === 20
      } else {
        hasMore.value[type] = false
      }
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 加载更多
function loadMore() {
  if (!loadingMore.value && hasMore.value[activeType.value]) {
    loadBookmarks(activeType.value, true)
  }
}

// 切换标签时加载数据
watch(
  activeType,
  (newType) => {
    if (bookmarks.value[newType].length === 0) {
      loadBookmarks(newType)
    }
  },
  { immediate: true }
)

// 取消收藏
async function handleUnbookmark(item: Bookmark) {
  try {
    await bookmarkApi.toggle(activeType.value, item.objectId)
    // 从列表中移除
    bookmarks.value[activeType.value] = bookmarks.value[activeType.value].filter(
      (b) => b.id !== item.id
    )
  } catch (error) {
    console.error('取消收藏失败:', error)
  }
}

// 获取显示名称
function getItemName(item: Bookmark): string {
  const obj = item.object as Record<string, unknown>
  if (!obj) return '未知'
  // profession, course, roadmap 用 name
  if (obj.name) return obj.name as string
  // roadmap 用 professionName
  if (obj.professionName) return obj.professionName as string
  // post 用 content 截取
  if (obj.content) return (obj.content as string).substring(0, 50)
  // card 用 front 截取
  if (obj.front) return (obj.front as string).substring(0, 50)
  return '未知'
}

// 获取描述
function getItemDescription(item: Bookmark): string | undefined {
  const obj = item.object as Record<string, unknown>
  if (!obj) return undefined
  return obj.description as string | undefined
}
</script>

<template>
  <div class="bookmarks-tab">
    <!-- 二级标签 -->
    <v-tabs v-model="activeType" color="primary" class="mb-6">
      <v-tab value="profession">职业</v-tab>
      <v-tab value="roadmap">路线图</v-tab>
      <v-tab value="course">课程</v-tab>
      <v-tab value="post">文章</v-tab>
      <v-tab value="memory_card">卡片</v-tab>
    </v-tabs>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <!-- 空状态 -->
    <div v-else-if="currentItems.length === 0" class="text-center py-12">
      <v-icon icon="mdi-bookmark-outline" size="64" color="grey" class="mb-4" />
      <p class="text-body-1 text-grey">暂无收藏</p>
    </div>

    <!-- 收藏列表 -->
    <div v-else>
      <v-card
        v-for="item in currentItems"
        :key="item.id"
        class="mb-4"
        rounded="lg"
        elevation="0"
        outlined
      >
        <v-card-text class="d-flex align-center">
          <div class="flex-grow-1">
            <h3 class="text-h6 mb-1">
              {{ getItemName(item) }}
            </h3>
            <p v-if="getItemDescription(item)" class="text-body-2 text-grey">
              {{ getItemDescription(item) }}
            </p>
          </div>
          <v-btn
            icon="mdi-bookmark"
            color="primary"
            variant="text"
            @click="handleUnbookmark(item)"
          />
        </v-card-text>
      </v-card>

      <!-- 加载更多 -->
      <div v-if="hasMore[activeType]" class="text-center mt-6">
        <v-btn
          :loading="loadingMore"
          variant="outlined"
          rounded="lg"
          @click="loadMore"
        >
          加载更多
        </v-btn>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bookmarks-tab {
  max-width: 900px;
}
</style>
