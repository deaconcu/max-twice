<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi } from '@/api'
import { useI18n } from '@/composables/useI18n'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import type { SearchResultItem } from '@/api/modules/search'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

// 加载触发器
const loadMoreTrigger = ref<HTMLElement | null>(null)

// 搜索关键词
const searchQuery = ref<string>((route.query.q as string) || '')
const activeTab = ref<string>('roles')

// 搜索结果
const courses = ref<SearchResultItem[]>([])
const nodes = ref<SearchResultItem[]>([])
const users = ref<SearchResultItem[]>([])
const roles = ref<SearchResultItem[]>([])

// 加载状态
const searching = ref(false)
const loadingMore = ref(false)

// 分页状态
const coursesOffset = ref(0)
const nodesOffset = ref(0)
const usersOffset = ref(0)
const rolesOffset = ref(0)

const hasMoreCourses = ref(true)
const hasMoreNodes = ref(true)
const hasMoreUsers = ref(true)
const hasMoreRoles = ref(true)

// 执行搜索
const performSearch = async () => {
  if (!searchQuery.value.trim()) {
    courses.value = []
    nodes.value = []
    users.value = []
    roles.value = []
    return
  }

  searching.value = true

  // 重置状态
  coursesOffset.value = 0
  nodesOffset.value = 0
  usersOffset.value = 0
  rolesOffset.value = 0
  hasMoreCourses.value = true
  hasMoreNodes.value = true
  hasMoreUsers.value = true
  hasMoreRoles.value = true

  try {
    // 初始加载每个分类20条
    const [coursesData, nodesData, usersData, rolesData] = await Promise.all([
      searchApi.searchCourses(searchQuery.value, 20, 0),
      searchApi.searchNodes(searchQuery.value, 20, 0),
      searchApi.searchUsers(searchQuery.value, 20, 0),
      searchApi.searchRoles(searchQuery.value, 20, 0),
    ])

    courses.value = coursesData || []
    coursesOffset.value = courses.value.length
    hasMoreCourses.value = courses.value.length >= 20

    nodes.value = nodesData || []
    nodesOffset.value = nodes.value.length
    hasMoreNodes.value = nodes.value.length >= 20

    users.value = usersData || []
    usersOffset.value = users.value.length
    hasMoreUsers.value = users.value.length >= 20

    roles.value = rolesData || []
    rolesOffset.value = roles.value.length
    hasMoreRoles.value = roles.value.length >= 20
  } catch (error) {
    console.error('搜索失败:', error)
  } finally {
    searching.value = false
    // 自动切换到第一个有结果的标签
    const tabs = ['roles', 'courses', 'nodes', 'users']
    for (const tab of tabs) {
      if (
        (tab === 'roles' && roles.value.length > 0) ||
        (tab === 'courses' && courses.value.length > 0) ||
        (tab === 'nodes' && nodes.value.length > 0) ||
        (tab === 'users' && users.value.length > 0)
      ) {
        activeTab.value = tab
        break
      }
    }
    // 搜索完成后重新设置滚动监听
    cleanupInfiniteScroll()
    setTimeout(setupInfiniteScroll, 100)
  }
}

// 加载更多结果
const loadMoreResults = async () => {
  if (!searchQuery.value.trim() || loadingMore.value) return

  let hasMore = false
  let offset = 0

  switch (activeTab.value) {
    case 'roles':
      hasMore = hasMoreRoles.value
      offset = rolesOffset.value
      break
    case 'courses':
      hasMore = hasMoreCourses.value
      offset = coursesOffset.value
      break
    case 'nodes':
      hasMore = hasMoreNodes.value
      offset = nodesOffset.value
      break
    case 'users':
      hasMore = hasMoreUsers.value
      offset = usersOffset.value
      break
  }

  if (!hasMore) return

  loadingMore.value = true
  try {
    switch (activeTab.value) {
      case 'roles': {
        const data = await searchApi.searchRoles(searchQuery.value, 20, rolesOffset.value)
        if (data) {
          roles.value = [...roles.value, ...data]
          rolesOffset.value += data.length
          hasMoreRoles.value = data.length >= 20
        }
        break
      }
      case 'courses': {
        const data = await searchApi.searchCourses(searchQuery.value, 20, coursesOffset.value)
        if (data) {
          courses.value = [...courses.value, ...data]
          coursesOffset.value += data.length
          hasMoreCourses.value = data.length >= 20
        }
        break
      }
      case 'nodes': {
        const data = await searchApi.searchNodes(searchQuery.value, 20, nodesOffset.value)
        if (data) {
          nodes.value = [...nodes.value, ...data]
          nodesOffset.value += data.length
          hasMoreNodes.value = data.length >= 20
        }
        break
      }
      case 'users': {
        const data = await searchApi.searchUsers(searchQuery.value, 20, usersOffset.value)
        if (data) {
          users.value = [...users.value, ...data]
          usersOffset.value += data.length
          hasMoreUsers.value = data.length >= 20
        }
        break
      }
    }
  } catch (error) {
    console.error('加载更多失败:', error)
  } finally {
    loadingMore.value = false
  }
}

// 监听 URL 查询参数变化
watch(
  () => route.query.q,
  (newQuery) => {
    searchQuery.value = (newQuery as string) || ''
    if (searchQuery.value) {
      performSearch()
    } else {
      // 清空搜索结果
      courses.value = []
      nodes.value = []
      users.value = []
      roles.value = []
    }
  },
  { immediate: true }
)

// 结果总数
const totalResults = computed(() => {
  return courses.value.length + nodes.value.length + users.value.length + roles.value.length
})

// 是否有任何分类还有更多数据
const hasAnyMore = computed(() => {
  return hasMoreCourses.value || hasMoreNodes.value || hasMoreUsers.value || hasMoreRoles.value
})

// 当前显示的结果
const currentResults = computed(() => {
  switch (activeTab.value) {
    case 'roles':
      return roles.value
    case 'courses':
      return courses.value
    case 'nodes':
      return nodes.value
    case 'users':
      return users.value
    default:
      return []
  }
})

// 导航到详情
const navigateTo = (type: string, item: { id: number; name: string }) => {
  switch (type) {
    case 'roles':
      router.push(`/role/${item.id}`)
      break
    case 'courses':
      router.push(`/courses/${item.id}`)
      break
    case 'nodes':
      router.push(`/read?nodeId=${item.id}`)
      break
    case 'users':
      router.push(`/users/${item.name}`)
      break
  }
}

// IntersectionObserver 实例
let observer: IntersectionObserver | null = null

// 设置无限滚动
const setupInfiniteScroll = () => {
  if (!loadMoreTrigger.value) return

  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry?.isIntersecting && !loadingMore.value) {
        const hasMore =
          (activeTab.value === 'roles' && hasMoreRoles.value) ||
          (activeTab.value === 'courses' && hasMoreCourses.value) ||
          (activeTab.value === 'nodes' && hasMoreNodes.value) ||
          (activeTab.value === 'users' && hasMoreUsers.value)

        if (hasMore) {
          loadMoreResults()
        }
      }
    },
    {
      root: null,
      rootMargin: '100px',
      threshold: 0.1,
    }
  )

  observer.observe(loadMoreTrigger.value)
}

// 清理 IntersectionObserver
const cleanupInfiniteScroll = () => {
  if (observer && loadMoreTrigger.value) {
    observer.unobserve(loadMoreTrigger.value)
    observer.disconnect()
    observer = null
  }
}

// 组件挂载
onMounted(() => {
  setTimeout(setupInfiniteScroll, 100)
})

// 组件卸载
onBeforeUnmount(() => {
  cleanupInfiniteScroll()
})

// 监听标签切换，重新设置监听器
watch(activeTab, () => {
  cleanupInfiniteScroll()
  setTimeout(setupInfiniteScroll, 100)
})
</script>

<template>
  <DefaultLayout>
    <v-container class="search-page">
      <!-- 加载状态 -->
      <div v-if="searching" class="text-center py-12">
        <v-progress-circular indeterminate color="primary"></v-progress-circular>
        <div class="mt-4 text-body-2 text-grey">{{ t('common.searching') }}</div>
      </div>

      <!-- 无结果 -->
      <div v-else-if="!searching && searchQuery && totalResults === 0" class="text-center py-12">
        <v-icon icon="mdi-magnify" size="80" color="grey-lighten-2"></v-icon>
        <div class="text-h6 mt-4 text-grey-darken-1">{{ t('common.noResults') }}</div>
        <div class="text-body-2 text-grey mt-2">{{ t('common.tryOtherKeywords') }}</div>
      </div>

      <!-- 搜索结果 -->
      <div v-else-if="!searching && searchQuery && totalResults > 0">
        <!-- 标签页 + 结果统计 -->
        <div class="d-flex align-center mb-6">
          <span class="text-body-2 text-grey flex-shrink-0">
            {{ t('common.foundResults', { count: totalResults }) }}{{ hasAnyMore ? '+' : '' }}
          </span>
          <v-spacer />
          <v-tabs v-model="activeTab" color="primary">
            <v-tab value="roles" :disabled="roles.length === 0">
              {{ t('common.roles') }} ({{ roles.length }}{{ hasMoreRoles ? '+' : '' }})
            </v-tab>
            <v-tab value="courses" :disabled="courses.length === 0">
              {{ t('common.courses') }} ({{ courses.length }}{{ hasMoreCourses ? '+' : '' }})
            </v-tab>
            <v-tab value="nodes" :disabled="nodes.length === 0">
              {{ t('common.nodes') }} ({{ nodes.length }}{{ hasMoreNodes ? '+' : '' }})
            </v-tab>
            <v-tab value="users" :disabled="users.length === 0">
              {{ t('common.users') }} ({{ users.length }}{{ hasMoreUsers ? '+' : '' }})
            </v-tab>
          </v-tabs>
        </div>

        <!-- 分类结果 -->
        <div>
          <div
            v-for="item in currentResults"
            :key="item.id"
            class="result-item"
            @click="navigateTo(activeTab, item)"
          >
            <div class="result-title">{{ item.name }}</div>
            <div v-if="item.description" class="result-description">
              {{ item.description }}
            </div>
          </div>

          <!-- 加载触发器 -->
          <div
            v-if="
              (activeTab === 'roles' && hasMoreRoles) ||
              (activeTab === 'courses' && hasMoreCourses) ||
              (activeTab === 'nodes' && hasMoreNodes) ||
              (activeTab === 'users' && hasMoreUsers)
            "
            ref="loadMoreTrigger"
            class="text-center py-6"
          >
            <v-progress-circular v-if="loadingMore" indeterminate size="32" color="primary" />
          </div>

          <!-- 没有更多 -->
          <div v-else-if="currentResults.length > 0" class="text-center py-6 text-body-2 text-grey">
            {{ t('common.noMoreResults') }}
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="text-center py-12">
        <v-icon icon="mdi-magnify" size="80" color="grey-lighten-2"></v-icon>
        <div class="text-h6 mt-4 text-grey-darken-1">{{ t('common.enterKeywords') }}</div>
        <div class="text-body-2 text-grey mt-2">{{ t('common.searchHint') }}</div>
      </div>
    </v-container>
  </DefaultLayout>
</template>

<style scoped>
.search-page {
  max-width: 900px;
  padding-top: 20px;
}

.result-item {
  padding: 24px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
}

.result-item:hover {
  background: rgba(var(--v-theme-primary), 0.02);
}

.result-item:last-child {
  border-bottom: none;
}

.result-title {
  font-size: 17px;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface));
  margin-bottom: 4px;
}

.result-description {
  font-size: 15px;
  color: rgba(var(--v-theme-on-surface), 0.6);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
