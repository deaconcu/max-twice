<template>
  <div class="read-page">
    <AppHeader />
    <AppSidebar />

    <div class="main-content">
      <!-- 课程头部 - 固定在顶部 -->
      <div class="course-header-sticky">
        <CourseHeader
          v-if="data"
          :parent-course-info="data.parentCourse"
          :current-course="data.course"
          :sub-course-list="data.subCourseList"
          :is-main-course="isMainCourse"
          :is-learning="isLearning"
          @start-learning="isLearning = $event"
        />
      </div>

      <div class="read-content">
        <!-- 左侧目录 -->
        <div class="toc-sidebar">
          <div class="toc-sticky-wrapper">
            <!-- 目录组选择卡片 -->
            <div v-if="data && data.toc" class="toc-groups-card">
              <div class="toc-chips">
                <v-chip
                  size="default"
                  rounded="lg"
                  label
                  variant="tonal"
                  color=""
                  class="me-0 px-3"
                  style="font-weight: 600"
                >
                  目录
                </v-chip>
                <div
                  v-for="(item, index) in data.toc"
                  :key="index"
                  class="position-relative d-inline-block"
                >
                  <v-chip
                    label
                    rounded="lg"
                    size="default"
                    variant="flat"
                    :color="currContentsIndex === index ? 'grey-darken-1' : 'grey-lighten-3'"
                    @click="currContentsIndex = index"
                  >
                    {{ index + 1 }}
                  </v-chip>
                  <div v-if="index === 0" class="corner-badge">
                    <v-icon icon="mdi-chart-line-variant" size="8" color="white" />
                  </div>
                </div>
                <v-btn
                  icon
                  size="small"
                  variant="text"
                  class="config-btn ms-auto"
                  @click="configContents = true"
                >
                  <v-icon size="20">mdi-cog-outline</v-icon>
                </v-btn>
              </div>
            </div>

            <!-- 目录树 -->
            <div class="toc-card">
              <div class="toc-tree">
                <TreeNode
                  v-if="data && data.toc && data.toc[currContentsIndex]"
                  :node-data="data.toc[currContentsIndex]"
                  :node-infos="data.tocNodeInfos"
                  :course-id="data.course?.id"
                  :path="data.path"
                  :curr-path="String(currContentsIndex + 1)"
                  :depth="1"
                  :is-learning="isLearning"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 中间+右侧容器包装 -->
        <div class="center-right-container">
          <!-- 中间+右侧容器 - 居中 -->
          <div class="center-right-wrapper">
            <!-- 中间内容区 - 固定宽度居中 -->
            <div class="center-content">
              <!-- PostingList 组件 -->
              <PostingList
                v-if="data"
                :data="data"
                :nodes="nodes"
                :curr-node-id="currNodeId"
                :curr-node="lastPathNode"
                :path-text="pathText"
                :is-learning="isLearning"
                @switch-tab="handleTabSwitch"
              />
            </div>

            <!-- 右侧工具栏 -->
            <div v-if="data" class="right-sidebar">
              <div class="sidebar-sticky">
                <!-- AI答疑助手 -->
                <v-card class="sidebar-card mb-4 no-border" rounded="lg">
                  <v-card-title class="pa-4">
                    <div class="d-flex align-center justify-space-between w-100">
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-robot-excited" color="primary" class="mr-2"></v-icon>
                        <span>答疑助手</span>
                      </div>
                      <v-btn
                        :icon="isAssistantExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down'"
                        variant="text"
                        color="grey-darken-1"
                        size="x-small"
                        @click="isAssistantExpanded = !isAssistantExpanded"
                      ></v-btn>
                    </div>
                  </v-card-title>

                  <v-expand-transition>
                    <v-card-text v-show="isAssistantExpanded" class="pa-4 pt-0">
                      <div class="text-body-2 text-grey-darken-2 mb-3">
                        <div class="font-weight-bold w-100">用法：</div>
                        <div class="mt-2">1）在文章中选中您不太理解的内容</div>
                        <div>2）在左侧竖线处拖动手柄，调整上下文范围</div>
                        <div>3）点击面板中的"复制"，将引用和问题复制到剪贴板</div>
                        <div>4）用复制的内容询问你常用的 AI 引擎</div>
                      </div>

                      <div class="d-flex flex-wrap mt-5" style="gap: 8px">
                        <div class="text-body-2 w-100 text-grey-darken-2 font-weight-bold">
                          常用 AI 引擎：
                        </div>
                        <v-chip
                          v-for="e in aiEngines"
                          :key="e.name"
                          :href="e.href"
                          target="_blank"
                          rel="noopener"
                          :color="e.color"
                          variant="tonal"
                          rounded="lg"
                          size="small"
                          class="engine-link"
                          :prepend-icon="e.icon"
                          :text="e.name"
                        />
                      </div>
                    </v-card-text>
                  </v-expand-transition>
                </v-card>

                <!-- 记忆卡片组 -->
                <v-card class="sidebar-card mb-4 no-border" rounded="lg">
                  <v-card-title class="pa-4">
                    <v-icon left color="primary">mdi-cards</v-icon>
                    记忆卡片组
                  </v-card-title>
                  <v-card-text class="pa-4 pt-0">
                    <div v-if="loadingDecks" class="text-center py-4">
                      <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
                    </div>
                    <template v-else>
                      <v-list v-if="memoryDecks.length > 0">
                        <v-list-item v-for="deck in memoryDecks" :key="deck.id" class="px-0">
                          <v-list-item-title class="text-body-2">
                            {{ deck.title }}
                          </v-list-item-title>
                          <v-list-item-subtitle class="text-caption">
                            {{ deck.cardCount }} 张卡片
                          </v-list-item-subtitle>
                        </v-list-item>
                      </v-list>
                      <div v-else class="text-body-2 text-grey text-center py-4">
                        暂无卡片组
                      </div>
                      <v-btn
                        block
                        color="primary"
                        variant="outlined"
                        class="text-none mt-2"
                        @click="showCreateDeckDialog = true"
                      >
                        创建卡片组
                      </v-btn>
                    </template>
                  </v-card-text>
                </v-card>

                <!-- 课程信息 -->
                <v-card class="sidebar-card no-border" rounded="lg">
                  <v-card-title class="pa-4"> 关于本课程 </v-card-title>
                  <v-card-text class="pa-4 pt-0">
                    <div class="info-item">
                      <div class="text-caption text-medium-emphasis mb-2">课程描述</div>
                      <div class="text-body-2">{{ data?.course?.description }}</div>
                    </div>
                  </v-card-text>
                </v-card>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 配置目录对话框 -->
    <ConfigContentsDialog
      v-if="data"
      v-model="configContents"
      :course-id="data.course?.id"
      :contents="data.toc || []"
      @load-data="loadData"
    />

    <!-- 创建卡片组对话框 -->
    <CreateDeckDialog
      v-if="currNodeId"
      v-model="showCreateDeckDialog"
      :node-id="currNodeId"
      @created="handleDeckCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import CourseHeader from '@/components/features/read/CourseHeader.vue'
import TreeNode from '@/components/common/TreeNode.vue'
import PostingList from '@/components/features/read/PostingList.vue'
import ConfigContentsDialog from '@/components/features/read/ConfigContentsDialog.vue'
import CreateDeckDialog from '@/components/features/read/CreateDeckDialog.vue'
import { pageApi, memoryApi } from '@/api'
import type { ReadResponse } from '@/api/modules/page'
import type { MemoryCardDeck } from '@/types/memory'
import { useFetch } from '@/composables/useFetch'

const router = useRouter()
const route = useRoute()

// 基本状态
const showFixedBar = ref(false)
const isLearning = ref(false)
const openContentsList = ref(true)
const configContents = ref(false)
const showCreateDeckDialog = ref(false)
const currContentsIndex = ref(0)
const isAssistantExpanded = ref(true)
const currentTab = ref('list')
const currentPosting = ref(null)
const loading = ref(false)
const error = ref<string | null>(null)

// 数据处理
const nodes = ref<any[]>([])
const currNodeId = ref(0)
const lastPathNode = ref<any>(null)
const pathText = ref('')

// 使用 useFetch 加载页面数据
const { data, loading: dataLoading, execute: loadData } = useFetch<ReadResponse>({
  fetchFn: () => {
    if (route.query.commentId) {
      return pageApi.readByComment(Number(route.query.commentId))
    } else if (route.query.postId) {
      return pageApi.readByPost(Number(route.query.postId))
    } else if (route.query.nodeId) {
      return pageApi.readByNode(Number(route.query.nodeId))
    } else if (route.params.id && route.query.path) {
      return pageApi.readByCoursePath(
        Number(route.params.id),
        route.query.path as string
      )
    } else if (route.params.id) {
      return pageApi.readByCoursePath(
        Number(route.params.id),
        ''
      )
    }
    return Promise.reject(new Error('缺少必要参数'))
  },
  immediate: true,
  onDataReady: () => {
    // 处理投票类型
    data.value.otherPostings?.forEach((posting: any) => {
      if (posting.voteType === 0) {
        posting.voteType = null
      }
    })
    // 设置学习状态
    isLearning.value = data.value.learning || false
    // 数据赋值完成后处理数据

    processData()

    // 加载记忆卡片组
    loadMemoryDecks()
  },
})

// 是否为主课程
const isMainCourse = computed(() => {
  if (data.value && data.value.course && data.value.parentCourse) {
    return data.value.course.id === data.value.parentCourse.id
  }
  return true
})

// AI 引擎列表
const aiEngines = [
  { name: 'ChatGPT', href: 'https://chatgpt.com', color: 'green-darken-2', icon: 'mdi-robot' },
  {
    name: 'Claude',
    href: 'https://claude.ai',
    color: 'indigo-darken-2',
    icon: 'mdi-alpha-c-circle-outline',
  },
  { name: 'Gemini', href: 'https://gemini.google.com', color: 'blue-darken-2', icon: 'mdi-google' },
  { name: 'DeepSeek', href: 'https://chat.deepseek.com', color: 'red-darken-4', icon: 'mdi-radar' },
]

// 记忆卡片组
const memoryDecks = ref<MemoryCardDeck[]>([])
const loadingDecks = ref(false)

// 加载记忆卡片组
const loadMemoryDecks = async () => {
  if (!currNodeId.value) return

  loadingDecks.value = true
  try {
    const response = await memoryApi.getDecksByNode(currNodeId.value, { limit: 10 })
    if (response.data) {
      memoryDecks.value = response.data.items || []
    }
  } catch (error) {
    console.error('Failed to load memory decks:', error)
    memoryDecks.value = []
  } finally {
    loadingDecks.value = false
  }
}

// 处理创建卡片组成功
const handleDeckCreated = (deck: MemoryCardDeck) => {
  console.log('Deck created:', deck)
  // 重新加载卡片组列表
  loadMemoryDecks()
}

// 处理数据
const processData = () => {
  console.log('data:', data.value)
  if (!data.value || !data.value.path) return

  // 解析路径
  nodes.value = data.value.path.split('-')
  nodes.value[0] = String(Number(nodes.value[0]) - 1)

  // 遍历 toc 获取 lastPathNode
  lastPathNode.value = nodes.value.reduce(
    (acc: any, key: any) => acc && acc[key],
    data.value.toc,
  )

  // 设置当前目录组索引
  currContentsIndex.value = Number(nodes.value[0])
  nodes.value.shift()

  console.log('currContentsIndex:', currContentsIndex.value)

  // 生成路径文本
  pathText.value = `${data.value.course.name}/`
  nodes.value.forEach((item: any, index: number) => {
    if (index < 1) return
    if (index < nodes.value.length - 1) {
      pathText.value += `${data.value.tocNodeInfos[item]?.name}/`
    } else {
      pathText.value += data.value.tocNodeInfos[item]?.name
    }
  })

  currNodeId.value = data.value.node.id
  isLearning.value = data.value.learning
}

// 滚动监听
const handleScroll = () => {
  showFixedBar.value = window.scrollY > 100
}

// 切换学习状态
const toggleLearning = () => {
  isLearning.value = !isLearning.value
}

// 返回课程详情页
const goBackToCourse = () => {
  const courseId = route.params.id || '1'
  router.push({
    name: 'course-detail',
    params: { id: courseId },
  })
}

// 处理Tab切换
const handleTabSwitch = (tab: string, posting?: any) => {
  currentTab.value = tab
  if (posting && typeof posting === 'object') {
    currentPosting.value = posting
  } else if (tab === 'list') {
    currentPosting.value = null
  }
}

// 跳转到目录组的根目录
const goToRootDirectory = (index: number) => {
  // 获取该目录组的根节点ID
  const tocGroup = data.value?.toc?.[index]
  if (!tocGroup) return

  // 找到第一个有效的根节点ID（排除 + 和 ^ 键）
  const rootNodeId = Object.keys(tocGroup).find((key) => key !== '+' && key !== '^')
  if (!rootNodeId) return

  // 构建根目录路径：{目录组编号}-{根节点ID}
  const rootPath = `${index + 1}-${rootNodeId}`

  router.push({
    name: 'content-read',
    params: { id: route.params.id },
    query: { path: rootPath },
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

// 监听路由变化，重新加载数据
watch(
  () => [route.params.id, route.query.path, route.query.nodeId, route.query.postId, route.query.commentId],
  () => {
    loadData()
  },
  { deep: true }
)

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.read-page {
  min-height: 100vh;
  background-color: #ffffff;
}

.main-content {
  margin-left: 240px;
  max-width: 1550px;
}

/* 固定课程头部 */
.course-header-sticky {
  position: sticky;
  top: 56px;
  background-color: white;
  z-index: 999;
  padding-bottom: 8px;
}

/* 三栏布局 */
.read-content {
  display: flex;
  position: relative;
  z-index: 1;
  max-width: 100%;
  width: 100%;
}

/* 左侧 TOC 目录栏 */
.toc-sidebar {
  flex: 0 1 360px;
  max-width: 360px;
  padding: 24px 42px 24px 0;
  position: relative;
  margin-right: 20px;
}

.toc-sidebar::after {
  content: '';
  position: absolute;
  top: 24px;
  right: 0;
  bottom: 24px;
  width: 1px;
  background-color: rgb(var(--v-theme-border));
}

.toc-sticky-wrapper {
  position: sticky;
  top: 110px;
  max-height: calc(100vh - 125px);
  display: flex;
  flex-direction: column;
  transition: top 0.3s ease, max-height 0.3s ease;
}

.read-page:has(.fixed-top-bar.show) .toc-sticky-wrapper {
  top: 109px;
  max-height: calc(100vh - 124px);
}

.toc-card {
  background-color: white;
  padding: 10px 0;
  border-radius: 16px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.toc-tree {
  margin-top: 6px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.toc-tree::-webkit-scrollbar {
  width: 1px;
}

.toc-tree::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 0.5px;
}

/* 目录标签样式 */
.toc-label {
  display: flex;
  align-items: center;
  margin-right: 2px;
  padding: 6px 12px;
  background-color: #f9f4f1;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

/* 目录组选择卡片 */
.toc-groups-card {
  padding: 0;
}

.toc-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.chip-active .chip-number {
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.corner-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background-color: rgb(var(--v-theme-success));
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
}

.config-btn {
  opacity: 0.5;
  transition: opacity 0.2s ease;
}

.config-btn:hover {
  opacity: 1;
}

/* 中间+右侧容器包装 */
.center-right-container {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.center-right-wrapper {
  display: flex;
  flex: 1;
  justify-content: center;
  max-width: 100%;
}

/* 中间内容区 - 固定宽度 */
.center-content {
  flex: 1 1 750px;
  max-width: 750px;
  padding: 24px 26px 40px 26px;
}

/* 右侧边栏 */
.right-sidebar {
  flex: 0 1 360px;
  max-width: 360px;
  padding: 24px 0 24px 24px;
}

.sidebar-sticky {
  position: sticky;
  top: 110px;
  max-height: calc(100vh - 125px);
  transition: top 0.3s ease, max-height 0.3s ease;
}

.read-page:has(.fixed-top-bar.show) .sidebar-sticky {
  top: 109px;
  max-height: calc(100vh - 124px);
}

.sidebar-card {
  background-color: white;
  border: 1px solid rgb(var(--v-theme-border));
}

.sidebar-card .v-card-title {
  font-size: 0.9375rem;
  font-weight: 600;
}

.engine-link {
  text-decoration: none !important;
}

.info-item {
  padding: 8px 0;
}

@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    padding: 80px 20px 80px 20px;
  }

  .toc-sidebar,
  .right-sidebar {
    display: none;
  }

  .center-content {
    width: 100%;
    max-width: 100%;
    padding: 0;
  }
}
</style>
