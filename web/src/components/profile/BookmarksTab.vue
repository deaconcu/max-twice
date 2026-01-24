<script lang="ts">
export default {
  name: 'BookmarksTab',
}
</script>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { bookmarkApi, type ContentType } from '@/api/modules/bookmark'
import type { Profession } from '@/types/profession'
import type { Course } from '@/types/course'
import type { Roadmap } from '@/types/roadmap'
import type { Post } from '@/types/post'
import type { MemoryCardView } from '@/types/memory'

// 当前激活的标签
const activeType = ref<ContentType>('profession')

// 各类型的收藏数据
const professions = ref<Profession[]>([])
const courses = ref<Course[]>([])
const roadmaps = ref<Roadmap[]>([])
const posts = ref<Post[]>([])
const memoryCards = ref<MemoryCardView[]>([])

// 加载状态
const loading = ref(false)
const loadingMore = ref(false)

// 分页参数
const lastIds = ref({
  profession: 0,
  course: 0,
  roadmap: 0,
  post: 0,
  memory_card: 0,
})

const hasMore = ref({
  profession: true,
  course: true,
  roadmap: true,
  post: true,
  memory_card: true,
})

// 统计数量
const counts = ref({
  profession: 0,
  course: 0,
  roadmap: 0,
  post: 0,
  memory_card: 0,
})

// 当前类型的数据
const currentItems = computed(() => {
  switch (activeType.value) {
    case 'profession':
      return professions.value
    case 'course':
      return courses.value
    case 'roadmap':
      return roadmaps.value
    case 'post':
      return posts.value
    case 'memory_card':
      return memoryCards.value
    default:
      return []
  }
})

// 加载收藏列表
async function loadBookmarks(type: ContentType, append = false) {
  if (!append) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const lastId = append ? lastIds.value[type] : 0
    const response = await bookmarkApi.getBookmarks(type, lastId, 20)

    if (response.data) {
      const items = response.data

      // 更新数据
      if (!append) {
        switch (type) {
          case 'profession':
            professions.value = items as unknown as Profession[]
            break
          case 'course':
            courses.value = items as unknown as Course[]
            break
          case 'roadmap':
            roadmaps.value = items as unknown as Roadmap[]
            break
          case 'post':
            posts.value = items as unknown as Post[]
            break
          case 'memory_card':
            memoryCards.value = items as unknown as MemoryCardView[]
            break
        }
      } else {
        // 追加数据
        switch (type) {
          case 'profession':
            professions.value.push(...(items as unknown as Profession[]))
            break
          case 'course':
            courses.value.push(...(items as unknown as Course[]))
            break
          case 'roadmap':
            roadmaps.value.push(...(items as unknown as Roadmap[]))
            break
          case 'post':
            posts.value.push(...(items as unknown as Post[]))
            break
          case 'memory_card':
            memoryCards.value.push(...(items as unknown as MemoryCardView[]))
            break
        }
      }

      // 更新分页参数
      if (items.length > 0) {
        lastIds.value[type] = items[items.length - 1].id
        hasMore.value[type] = items.length === 20
      } else {
        hasMore.value[type] = false
      }

      // 更新数量
      if (!append) {
        counts.value[type] = items.length
      } else {
        counts.value[type] += items.length
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
    if (currentItems.value.length === 0) {
      loadBookmarks(newType)
    }
  },
  { immediate: true }
)

// 取消收藏
async function handleUnbookmark(item: any) {
  try {
    await bookmarkApi.toggle(activeType.value, item.id)
    // 从列表中移除
    switch (activeType.value) {
      case 'profession':
        professions.value = professions.value.filter((p) => p.id !== item.id)
        break
      case 'course':
        courses.value = courses.value.filter((c) => c.id !== item.id)
        break
      case 'roadmap':
        roadmaps.value = roadmaps.value.filter((r) => r.id !== item.id)
        break
      case 'post':
        posts.value = posts.value.filter((p) => p.id !== item.id)
        break
      case 'memory_card':
        memoryCards.value = memoryCards.value.filter((m) => m.id !== item.id)
        break
    }
    counts.value[activeType.value]--
  } catch (error) {
    console.error('取消收藏失败:', error)
  }
}
</script>

<template>
  <div class="bookmarks-tab">
    <!-- 二级标签 -->
    <v-tabs v-model="activeType" color="primary" class="mb-6">
      <v-tab value="profession">
        职业 <span class="ml-1 text-caption">({{ counts.profession }})</span>
      </v-tab>
      <v-tab value="roadmap">
        路线图 <span class="ml-1 text-caption">({{ counts.roadmap }})</span>
      </v-tab>
      <v-tab value="course">
        课程 <span class="ml-1 text-caption">({{ counts.course }})</span>
      </v-tab>
      <v-tab value="post">
        文章 <span class="ml-1 text-caption">({{ counts.post }})</span>
      </v-tab>
      <v-tab value="memory_card">
        卡片 <span class="ml-1 text-caption">({{ counts.memory_card }})</span>
      </v-tab>
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
              {{ item.name || item.content?.substring(0, 50) }}
            </h3>
            <p v-if="item.description" class="text-body-2 text-grey">
              {{ item.description }}
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
