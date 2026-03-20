<script lang="ts">
export default {
  name: 'BookmarksTab',
}
</script>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { bookmarkApi, type ContentType, type Bookmark } from '@/api/modules/bookmark'
import { getColorByString } from '@/utils/color'
import DynamicIcon from '@/components/common/DynamicIcon.vue'

const router = useRouter()

// 当前激活的标签
const activeType = ref<ContentType>('profession')

// 收藏数据（统一存储 BookmarkDTO）
const bookmarks = ref<Record<ContentType, Bookmark[]>>({
  profession: [],
  course: [],
  roadmap: [],
  post: [],
  memory_card_deck: [],
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
  memory_card_deck: undefined,
})

const hasMore = ref<Record<ContentType, boolean>>({
  profession: true,
  course: true,
  roadmap: true,
  post: true,
  memory_card_deck: true,
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

// 跳转到详情页
function goToDetail(item: Bookmark) {
  const obj = item.object as Record<string, unknown>
  if (!obj) return

  switch (activeType.value) {
    case 'profession':
      router.push(`/profession/${item.objectId}`)
      break
    case 'roadmap':
      router.push(`/role/${item.objectId}`)
      break
    case 'course':
      router.push(`/course/${item.objectId}`)
      break
    case 'post':
      router.push(`/post/${item.objectId}`)
      break
    case 'memory_card_deck':
      router.push(`/deck/${item.objectId}`)
      break
  }
}

// 获取职业/课程的图标
function getIcon(item: Bookmark): string {
  const obj = item.object as Record<string, unknown>
  if (!obj) return 'mdi-bookmark'
  return (obj.icon as string) || (obj.professionIcon as string) || 'mdi-bookmark'
}

// 获取名称
function getName(item: Bookmark): string {
  const obj = item.object as Record<string, unknown>
  if (!obj) return '未知'
  return (obj.name as string) || (obj.professionName as string) || '未知'
}

// 获取图标颜色
function getIconColor(item: Bookmark): string {
  return getColorByString(getName(item))
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
      <v-tab value="memory_card_deck">卡片组</v-tab>
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
      <!-- 职业/路线图/课程 - 网格卡片 -->
      <div
        v-if="activeType === 'profession' || activeType === 'roadmap' || activeType === 'course'"
        class="bookmark-grid"
      >
        <v-card
          v-for="item in currentItems"
          :key="item.id"
          rounded="lg"
          border
          hover
          class="bookmark-card"
          @click="goToDetail(item)"
        >
          <v-card-text class="pa-4 position-relative">
            <v-btn
              color="grey"
              variant="text"
              size="x-small"
              icon="mdi-close"
              class="bookmark-btn"
              @click.stop="handleUnbookmark(item)"
            />
            <div class="d-flex align-center ga-3">
              <div class="icon-container flex-shrink-0">
                <DynamicIcon
                  :icon="getIcon(item)"
                  :default-icon="
                    activeType === 'profession'
                      ? 'mdi-briefcase'
                      : activeType === 'roadmap'
                        ? 'mdi-map-marker-path'
                        : 'mdi-book-open-variant'
                  "
                  :size="24"
                  :color="getIconColor(item)"
                />
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div
                  class="text-body-1 font-weight-bold text-truncate"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ getName(item) }}
                </div>
                <div
                  v-if="(item.object as Record<string, unknown>)?.nodeCount"
                  class="text-caption text-medium-emphasis"
                >
                  {{ (item.object as Record<string, unknown>).nodeCount }} 个知识节点
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <!-- 文章 - 列表卡片 -->
      <div v-else-if="activeType === 'post'" class="post-list">
        <v-card
          v-for="item in currentItems"
          :key="item.id"
          rounded="lg"
          border
          hover
          class="mb-4 post-card"
          @click="goToDetail(item)"
        >
          <v-card-text class="pa-4 position-relative">
            <v-btn
              color="grey"
              variant="text"
              size="x-small"
              icon="mdi-close"
              class="bookmark-btn"
              @click.stop="handleUnbookmark(item)"
            />
            <div
              class="text-body-1 post-content"
              :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
            >
              {{ (item.object as Record<string, unknown>)?.content || '无内容' }}
            </div>
            <div class="d-flex align-center ga-4 mt-3 text-caption text-medium-emphasis">
              <span v-if="(item.object as Record<string, unknown>)?.twiceCount">
                <v-icon size="14" class="mr-1">mdi-fire</v-icon>
                {{ (item.object as Record<string, unknown>).twiceCount }}
              </span>
              <span v-if="(item.object as Record<string, unknown>)?.likeCount">
                <v-icon size="14" class="mr-1">mdi-thumb-up</v-icon>
                {{ (item.object as Record<string, unknown>).likeCount }}
              </span>
              <span v-if="(item.object as Record<string, unknown>)?.commentCount">
                <v-icon size="14" class="mr-1">mdi-comment</v-icon>
                {{ (item.object as Record<string, unknown>).commentCount }}
              </span>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <!-- 卡片组 - 网格卡片 -->
      <div v-else-if="activeType === 'memory_card_deck'" class="bookmark-grid">
        <v-card
          v-for="item in currentItems"
          :key="item.id"
          rounded="lg"
          border
          hover
          class="bookmark-card"
          @click="goToDetail(item)"
        >
          <v-card-text class="pa-4 position-relative">
            <v-btn
              color="grey"
              variant="text"
              size="x-small"
              icon="mdi-close"
              class="bookmark-btn"
              @click.stop="handleUnbookmark(item)"
            />
            <div class="d-flex align-center ga-3">
              <div class="icon-container flex-shrink-0">
                <v-icon size="24" :color="getIconColor(item)">mdi-cards</v-icon>
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div
                  class="text-body-1 font-weight-bold text-truncate"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ (item.object as Record<string, unknown>)?.name || '未知卡片组' }}
                </div>
                <div
                  v-if="(item.object as Record<string, unknown>)?.cardCount"
                  class="text-caption text-medium-emphasis"
                >
                  {{ (item.object as Record<string, unknown>).cardCount }} 张卡片
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore[activeType]" class="text-center mt-6">
        <v-btn :loading="loadingMore" variant="outlined" rounded="lg" @click="loadMore">
          加载更多
        </v-btn>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bookmarks-tab {
  container-type: inline-size;
}

.bookmark-card {
  cursor: pointer;
  transition: all 0.2s;
  background-color: rgb(var(--v-theme-surface));
}

.bookmark-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.bookmark-btn {
  position: absolute;
  top: 8px;
  right: 8px;
}

.icon-container {
  width: 48px;
  height: 48px;
  border: 1px solid rgb(var(--v-theme-outline));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 网格布局 */
.bookmark-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@container (max-width: 1200px) {
  .bookmark-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@container (max-width: 750px) {
  .bookmark-grid {
    grid-template-columns: 1fr;
  }
}

/* 文章卡片 */
.post-card {
  cursor: pointer;
  transition: all 0.2s;
  background-color: rgb(var(--v-theme-surface));
}

.post-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.post-content {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
  padding-right: 32px;
}
</style>
