<template>
  <div class="read-page">
    <AppHeader />
    <AppSidebar />

    <div class="main-content">
      <!-- 固定顶部横条 - 滚动时显示 -->
      <div v-if="showFixedBar" class="fixed-top-bar">
        <div class="fixed-bar-content">
          <div class="d-flex align-center flex-grow-1" style="gap: 12px">
            <v-btn variant="text" color="grey-darken-2" size="small" @click="goBackToCourse">
              <v-icon icon="mdi-arrow-left" size="18" class="mr-1" />
              返回课程
            </v-btn>
            <div class="d-flex align-center" style="gap: 6px">
              <span class="fixed-bar-course-name">{{ courseName }}</span>
              <v-icon icon="mdi-chevron-right" size="16" color="grey" />
              <span class="fixed-bar-path">{{ currentNodeName }}</span>
            </div>
          </div>
          <div class="d-flex align-center" style="gap: 8px">
            <v-btn
              :color="isLearning ? 'success' : 'primary'"
              :variant="isLearning ? 'outlined' : 'flat'"
              size="x-small"
              rounded="lg"
              class="text-none"
              @click="toggleLearning"
            >
              <v-icon size="14" class="mr-1">
                {{ isLearning ? 'mdi-check-circle' : 'mdi-play-circle' }}
              </v-icon>
              {{ isLearning ? '学习中' : '开始学习' }}
            </v-btn>
          </div>
        </div>
      </div>

      <!-- 课程信息卡片 -->
      <div class="course-info-section">
        <div v-if="loadingPage" class="text-center py-4">
          <v-progress-circular indeterminate size="32" />
        </div>
        <div v-else-if="pageError" class="text-center py-4">
          <p class="text-error">加载失败</p>
        </div>
        <div v-else class="d-flex align-center justify-space-between">
          <div class="d-flex align-center course-breadcrumb">
            <v-btn
              icon="mdi-arrow-left"
              variant="flat"
              color="grey-lighten-4"
              size="small"
              class="mr-2"
              @click="goBackToCourse"
            />
            <v-chip size="small" density="comfortable" color="grey-darken-1" variant="tonal">
              课程
            </v-chip>
            <v-btn variant="text" class="course-link-btn px-1" @click="goBackToCourse">
              {{ courseName }}
            </v-btn>
            <span class="text-caption text-grey mx-2">·</span>
            <span class="text-caption text-grey">{{ totalNodes }} 个节点</span>
            <span class="text-caption text-grey mx-2">·</span>
            <span class="text-caption text-grey">{{ learnerCount }} 人学习</span>
          </div>
          <div class="d-flex align-center flex-shrink-0" style="gap: 8px">
            <v-btn
              :color="isLearning ? 'success' : 'primary'"
              :variant="isLearning ? 'tonal' : 'flat'"
              density="comfortable"
              rounded="pill"
              class="text-none px-4"
              elevation="0"
              @click="toggleLearning"
            >
              <v-icon size="16" class="mr-1">
                {{ isLearning ? 'mdi-check-circle' : 'mdi-play-circle' }}
              </v-icon>
              {{ isLearning ? '学习中' : '开始学习' }}
            </v-btn>
            <v-btn
              :icon="isSubscribed ? 'mdi-heart' : 'mdi-heart-outline'"
              :color="isSubscribed ? 'error' : 'grey-lighten-1'"
              :variant="isSubscribed ? 'flat' : 'text'"
              density="comfortable"
              rounded="circle"
              @click="toggleSubscribe"
            />
          </div>
        </div>
      </div>

      <div class="read-content">
        <!-- 左侧目录 -->
        <div class="toc-sidebar">
          <div class="toc-sticky-wrapper">
            <!-- 目录头部 -->
            <div class="toc-card">
              <div class="toc-header">
                <div class="d-flex align-center">
                  <v-icon icon="mdi-view-list" size="14" color="grey" class="mr-2" />
                  <span class="toc-title">课程目录</span>
                </div>
              </div>
              <!-- 目录树 -->
              <div class="toc-tree">
                <div v-for="node in tocData" :key="node.id" class="toc-node">
                  <div
                    class="toc-node-item"
                    :class="{ active: activeNodeId === node.id }"
                    @click="handleNodeClick(node)"
                  >
                    <v-icon
                      :icon="node.completed ? 'mdi-check-circle' : 'mdi-circle-outline'"
                      :color="node.completed ? 'success' : 'grey'"
                      size="16"
                      class="mr-2"
                    />
                    <span>{{ node.name }}</span>
                  </div>
                  <!-- 子节点 -->
                  <div v-if="node.children" class="toc-children">
                    <div
                      v-for="child in node.children"
                      :key="child.id"
                      class="toc-node-item child"
                      :class="{ active: activeNodeId === child.id }"
                      @click="handleNodeClick(child)"
                    >
                      <v-icon
                        :icon="child.completed ? 'mdi-check-circle' : 'mdi-circle-outline'"
                        :color="child.completed ? 'success' : 'grey'"
                        size="14"
                        class="mr-2"
                      />
                      <span>{{ child.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间+右侧容器 -->
        <div class="center-right-container">
          <div class="center-right-wrapper">
            <!-- 中间内容区 -->
            <div class="center-content">
              <!-- 节点路径 -->
              <div class="node-path mb-2">
                <span class="text-caption text-medium-emphasis">{{ nodePath }}</span>
              </div>

              <!-- 节点头部 -->
              <div class="node-header mb-4">
                <div class="d-flex align-center justify-space-between mb-3">
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-list-box-outline" color="primary" size="24" />
                    <h2 class="text-h5 font-weight-bold ms-3">{{ currentNodeName }}</h2>
                  </div>
                  <v-btn
                    v-if="isLearning"
                    :color="currentNodeCompleted ? 'success' : 'primary'"
                    :variant="currentNodeCompleted ? 'tonal' : 'flat'"
                    size="small"
                    rounded="lg"
                    @click="toggleNodeCompletion"
                  >
                    <v-icon size="16" class="mr-1">
                      {{ currentNodeCompleted ? 'mdi-check-circle' : 'mdi-checkbox-blank-circle-outline' }}
                    </v-icon>
                    {{ currentNodeCompleted ? '已完成' : '标记完成' }}
                  </v-btn>
                </div>
                <p v-if="currentNodeDescription" class="text-body-2 text-medium-emphasis">
                  {{ currentNodeDescription }}
                </p>
              </div>

              <!-- 文章列表 -->
              <div class="articles-section">
                <div v-if="loadingArticles" class="text-center py-8">
                  <v-progress-circular indeterminate color="primary" />
                </div>
                <div v-else-if="articles.length === 0" class="text-center py-8">
                  <v-icon icon="mdi-file-document-outline" size="64" color="grey-lighten-1" />
                  <p class="text-body-1 text-medium-emphasis mt-4">暂无文章内容</p>
                </div>
                <div v-else>
                  <v-card
                    v-for="article in articles"
                    :key="article.id"
                    class="article-card mb-4"
                    border
                    @click="handleArticleClick(article)"
                  >
                    <v-card-title>{{ article.title }}</v-card-title>
                    <v-card-subtitle v-if="article.author">
                      {{ article.author.username }} · {{ formatTime(article.createTime) }}
                    </v-card-subtitle>
                    <v-card-text>
                      <div class="article-preview" v-html="article.content"></div>
                    </v-card-text>
                  </v-card>
                </div>
              </div>
            </div>

            <!-- 右侧辅助区 -->
            <div class="right-assistant">
              <div class="assistant-card">
                <h4 class="text-subtitle-1 font-weight-bold mb-2">学习助手</h4>
                <p class="text-caption text-medium-emphasis">辅助功能开发中...</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useFetch } from '@/composables'
import { pageApi, type ReadResponse } from '@/api/modules/page'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'

const router = useRouter()
const route = useRoute()

// ==================== 路由参数 ====================
const courseId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : 0
})

// ==================== 状态 ====================
const showFixedBar = ref(false)
const activeNodeId = ref<number | null>(null)
const currentNodeCompleted = ref(false)
const currentPath = computed(() => (route.query.path as string) || '')

// ==================== 页面数据加载函数 ====================
const loadPageData = () => {
  console.log('Loading page data, courseId:', courseId.value, 'path:', currentPath.value)
  return pageApi.readByCoursePath(courseId.value, currentPath.value)
}

// ==================== 页面数据（使用 pageApi） ====================
const {
  data: pageData,
  loading: loadingPage,
  error: pageError,
  refresh: refreshPage,
} = useFetch<ReadResponse>({
  fetchFn: loadPageData,
  immediate: true,
  defaultValue: null,
})

// ==================== 从 pageData 提取数据 ====================
const courseName = computed(() => pageData.value?.course?.name || '加载中...')
const learnerCount = computed(() => pageData.value?.course?.learnerCount || 0)
const isLearning = computed(() => pageData.value?.learning || false)
const isSubscribed = computed(() => pageData.value?.course?.subscribed || false)
const tocData = computed(() => pageData.value?.toc || [])
const tocNodeInfos = computed(() => pageData.value?.tocNodeInfos || {})
const currentNode = computed(() => pageData.value?.node)
const currentNodeName = computed(() => currentNode.value?.name || '')
const currentNodeDescription = computed(() => currentNode.value?.description || '')
const nodePath = computed(() => pageData.value?.path || '')
const totalNodes = computed(() => Object.keys(tocNodeInfos.value).length)

// ==================== 文章数据 ====================
const articles = computed(() => {
  const fixed = pageData.value?.fixedPostings || []
  const other = pageData.value?.otherPostings || []
  return [...fixed, ...other]
})
const loadingArticles = computed(() => loadingPage.value)

// ==================== 滚动监听 ====================
const handleScroll = () => {
  showFixedBar.value = window.scrollY > 150
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

// 监听路由 query 变化，重新加载数据
watch(
  () => route.query.path,
  () => {
    void refreshPage()
  }
)

// ==================== 事件处理 ====================
const toggleLearning = () => {
  // TODO: 调用学习状态 API
  void refreshPage()
}

const toggleSubscribe = async () => {
  if (!pageData.value?.course) return

  const { subscriptionApi } = await import('@/api')
  const action = isSubscribed.value ? 'unsubscribe' : 'subscribe'

  try {
    if (action === 'subscribe') {
      await subscriptionApi.subscribe(courseId.value)
    } else {
      await subscriptionApi.unsubscribe(courseId.value)
    }
    void refreshPage()
  } catch (error) {
    console.error('Toggle subscribe failed:', error)
  }
}

const toggleNodeCompletion = () => {
  currentNodeCompleted.value = !currentNodeCompleted.value
  // TODO: 调用API更新节点完成状态
}

const goBackToCourse = () => {
  void router.push({
    name: 'course-detail',
    params: { id: String(courseId.value) },
  })
}

const handleNodeClick = (node: any) => {
  activeNodeId.value = node.id

  // 获取节点信息
  const nodeInfo = tocNodeInfos.value[node.id]
  if (nodeInfo && nodeInfo.path) {
    // 通过路由更新 path 参数
    void router.push({
      name: 'content-read',
      params: { id: String(courseId.value) },
      query: { path: nodeInfo.path },
    })
  }
}

const handleArticleClick = (article: any) => {
  // TODO: 展开文章详情
  console.log('Article clicked:', article)
}

/**
 * 格式化时间
 */
const formatTime = (time?: string | number) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.read-page {
  min-height: 100vh;
}

.main-content {
  margin-left: 240px;
  padding-top: 64px;
  min-height: calc(100vh - 64px);
}

/* 固定顶部栏 */
.fixed-top-bar {
  position: fixed;
  top: 64px;
  left: 240px;
  right: 0;
  height: 48px;
  background: rgb(var(--v-theme-surface));
  border-bottom: 1px solid rgb(var(--v-theme-border));
  z-index: 100;
  padding: 0 24px;
}

.fixed-bar-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.fixed-bar-course-name {
  font-weight: 600;
  font-size: 14px;
}

.fixed-bar-path {
  font-size: 13px;
  color: #666;
}

/* 课程信息卡片 */
.course-info-section {
  padding: 20px 32px;
  border-bottom: 1px solid rgb(var(--v-theme-border));
}

.course-breadcrumb {
  gap: 8px;
}

.course-link-btn {
  text-transform: none;
  font-weight: 500;
}

/* 内容区 */
.read-content {
  display: flex;
  gap: 0;
  padding: 24px 0;
}

/* 左侧目录 */
.toc-sidebar {
  width: 280px;
  flex-shrink: 0;
  padding: 0 16px;
  border-right: 1px solid rgb(var(--v-theme-border));
}

.toc-sticky-wrapper {
  position: sticky;
  top: 88px;
}

.toc-card {
  padding: 16px 0;
}

.toc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toc-title {
  font-weight: 600;
  font-size: 14px;
}

.toc-tree {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.toc-node {
  margin-bottom: 4px;
}

.toc-node-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.toc-node-item:hover {
  background: #f5f5f5;
}

.toc-node-item.active {
  background: #e3f2fd;
  color: #1976d2;
  font-weight: 500;
}

.toc-children {
  margin-left: 16px;
}

.toc-node-item.child {
  font-size: 13px;
  padding: 6px 12px;
}

/* 中间+右侧容器 */
.center-right-container {
  flex: 1;
  min-width: 0;
}

.center-right-wrapper {
  display: flex;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 中间内容 */
.center-content {
  flex: 1;
  min-width: 0;
  max-width: 900px;
}

.node-path {
  color: #666;
}

.node-header {
  padding: 20px 0;
  border-bottom: 1px solid rgb(var(--v-theme-border));
}

/* 文章列表 */
.articles-section {
  margin-top: 24px;
}

.article-card {
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 16px;
}

.article-card:hover {
  border-color: rgb(var(--v-theme-primary));
}

.article-preview {
  max-height: 200px;
  overflow: hidden;
  position: relative;
}

/* 右侧辅助 */
.right-assistant {
  width: 280px;
  flex-shrink: 0;
}

.assistant-card {
  padding: 16px;
  border: 1px solid rgb(var(--v-theme-border));
  border-radius: 8px;
  position: sticky;
  top: 88px;
}

/* 响应式 */
@media (max-width: 1400px) {
  .right-assistant {
    display: none;
  }
}

@media (max-width: 1024px) {
  .toc-sidebar {
    display: none;
  }

  .main-content {
    margin-left: 0;
  }

  .fixed-top-bar {
    left: 0;
  }
}
</style>
