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
import { formatRelativeTime } from '@/utils/format'
import { useI18n } from '@/composables/useI18n'
import DynamicIcon from '@/components/common/DynamicIcon.vue'

const { t } = useI18n()
const router = useRouter()

// 当前激活的标签
const activeType = ref<ContentType>('role')

// 收藏数据（统一存储 BookmarkDTO）
const bookmarks = ref<Record<ContentType, Bookmark[]>>({
  role: [],
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
  role: undefined,
  course: undefined,
  roadmap: undefined,
  post: undefined,
  memory_card_deck: undefined,
})

const hasMore = ref<Record<ContentType, boolean>>({
  role: true,
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
    console.error('unfavorite error:', error)
  }
}

// 跳转到详情页
function goToDetail(item: Bookmark) {
  const obj = item.object as Record<string, unknown>
  if (!obj) return

  switch (activeType.value) {
    case 'role':
      router.push(`/role/${item.objectId}`)
      break
    case 'roadmap':
      router.push(`/roadmap/${item.objectId}`)
      break
    case 'course':
      router.push(`/read?courseId=${item.objectId}`)
      break
    case 'post':
      router.push(`/post/${item.objectId}`)
      break
    case 'memory_card_deck':
      router.push(`/deck/${item.objectId}`)
      break
  }
}

// 获取职业/课程的图标（返回实际值，null 由 DynamicIcon 的 default-icon 处理）
function getIcon(item: Bookmark): string | null {
  const obj = item.object as Record<string, unknown>
  if (!obj) return null
  return (obj.icon as string) || (obj.roleIcon as string) || null
}

// 获取名称
function getName(item: Bookmark): string {
  const obj = item.object as Record<string, unknown>
  if (!obj) return t('user.profile.unknown')
  return (obj.name as string) || (obj.roleName as string) || t('user.profile.unknown')
}

// 获取图标颜色
function getIconColor(item: Bookmark): string {
  return getColorByString(getName(item))
}

// 获取文章的节点名称
function getPostNodeName(item: Bookmark): string | null {
  const obj = item.object as Record<string, unknown>
  if (!obj) return null
  const node = obj.node as Record<string, unknown> | undefined
  return (node?.name as string) || null
}

// 获取文章类型标签（文章/目录）
function getPostTypeLabel(item: Bookmark): string {
  const obj = item.object as Record<string, unknown>
  if (!obj) return t('user.profile.article')
  // type: 1 = 目录, 2 = 文章
  return obj.type === 1 ? t('user.profile.myContents') : t('user.profile.article')
}

// 获取文章图标（目录/文章显示不同图标）
function getPostIcon(item: Bookmark): string {
  const obj = item.object as Record<string, unknown>
  if (!obj) return 'mdi-file-document-outline'
  return obj.type === 1 ? 'mdi-format-list-bulleted' : 'mdi-file-document-outline'
}

// 获取文章图标颜色
function getPostIconColor(): string {
  return 'grey'
}
</script>

<template>
  <div class="bookmarks-tab">
    <!-- 二级标签 -->
    <v-tabs v-model="activeType" color="primary" class="mb-6">
      <v-tab value="role">{{ t('nav.role') }}</v-tab>
      <v-tab value="roadmap">{{ t('nav.roadmap') }}</v-tab>
      <v-tab value="course">{{ t('nav.courses') }}</v-tab>
      <v-tab value="post">{{ t('user.profile.article') }}</v-tab>
      <v-tab value="memory_card_deck">{{ t('user.profile.myMemoryDecks') }}</v-tab>
    </v-tabs>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <!-- 空状态 -->
    <div v-else-if="currentItems.length === 0" class="text-center py-12">
      <v-icon icon="mdi-bookmark-outline" size="64" color="grey" class="mb-4" />
      <p class="text-body-1 text-grey">{{ t('common.noCards') }}</p>
    </div>

    <!-- 收藏列表 -->
    <div v-else>
      <!-- 角色 - 网格卡片 -->
      <div v-if="activeType === 'role'" class="bookmark-grid">
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
                  default-icon="mdi-briefcase"
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
                <div class="d-flex align-center justify-end">
                  <span class="text-caption text-grey">{{ formatRelativeTime(item.createdAt) }}</span>
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <!-- 路线图 - 网格卡片 -->
      <div v-else-if="activeType === 'roadmap'" class="bookmark-grid">
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
                  default-icon="mdi-map-marker-path"
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
                <div class="d-flex align-center justify-space-between">
                  <span
                    v-if="(item.object as Record<string, unknown>)?.nodeCount"
                    class="text-caption text-medium-emphasis text-truncate"
                    style="flex: 1; min-width: 0"
                  >
                    {{ (item.object as Record<string, unknown>).nodeCount }} {{ t('rightSidebar.knowledgeNodes') }}
                  </span>
                  <span v-else style="flex: 1"></span>
                  <span class="text-caption text-grey flex-shrink-0 ml-2">{{ formatRelativeTime(item.createdAt) }}</span>
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <!-- 课程 - 网格卡片 -->
      <div v-else-if="activeType === 'course'" class="bookmark-grid">
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
                  default-icon="mdi-book-open-variant"
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
                <div class="d-flex align-center justify-space-between">
                  <span
                    v-if="(item.object as Record<string, unknown>)?.nodeCount"
                    class="text-caption text-medium-emphasis text-truncate"
                    style="flex: 1; min-width: 0"
                  >
                    {{ (item.object as Record<string, unknown>).nodeCount }} {{ t('rightSidebar.knowledgeNodes') }}
                  </span>
                  <span v-else style="flex: 1"></span>
                  <span class="text-caption text-grey flex-shrink-0 ml-2">{{ formatRelativeTime(item.createdAt) }}</span>
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <!-- 文章 - 网格卡片 -->
      <div v-else-if="activeType === 'post'" class="bookmark-grid">
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
                <v-icon
                  size="24"
                  :color="getPostIconColor()"
                >
                  {{ getPostIcon(item) }}
                </v-icon>
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div
                  class="text-body-1 font-weight-bold text-truncate"
                  :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                >
                  {{ getPostNodeName(item) || t('user.profile.unknownNode') }}
                </div>
                <div class="d-flex align-center justify-space-between">
                  <span class="text-caption text-medium-emphasis">{{ getPostTypeLabel(item) }}</span>
                  <span class="text-caption text-grey flex-shrink-0 ml-2">{{ formatRelativeTime(item.createdAt) }}</span>
                </div>
              </div>
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
                  {{ (item.object as Record<string, unknown>)?.name || t('user.profile.unknown') }}
                </div>
                <div class="d-flex align-center justify-space-between">
                  <span
                    v-if="(item.object as Record<string, unknown>)?.cardCount"
                    class="text-caption text-medium-emphasis text-truncate"
                    style="flex: 1; min-width: 0"
                  >
                    {{ (item.object as Record<string, unknown>).cardCount }} {{ t('review.cards') }}
                  </span>
                  <span v-else style="flex: 1"></span>
                  <span class="text-caption text-grey flex-shrink-0 ml-2">{{ formatRelativeTime(item.createdAt) }}</span>
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore[activeType]" class="text-center mt-6">
        <v-btn :loading="loadingMore" variant="outlined" rounded="lg" @click="loadMore">
          {{ t('common.loadMore') }}
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
