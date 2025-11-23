<template>
  <DefaultLayout>
    <div class="read-page">
      <!-- 移动端目录抽屉 -->
      <v-dialog
        v-if="$vuetify.display.mobile"
        v-model="drawerOpen"
        fullscreen
        transition="dialog-left-transition"
        scrollable
      >
        <v-card class="drawer-card">
          <v-card-text class="pa-0 drawer-card-content">
            <div class="drawer-container">
              <!-- 目录组选择和关闭按钮 -->
              <div v-if="data && data.toc" class="toc-chips-row pa-4 pa-md-4 d-flex align-items-center flex-wrap">
                <v-chip
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  rounded="lg"
                  label
                  variant="tonal"
                  color=""
                  class="me-2 px-3 text-body-2 text-md-body-1"
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
                    :size="$vuetify.display.mobile ? 'small' : 'default'"
                    variant="flat"
                    :color="currContentsIndex === index ? 'grey-darken-1' : 'grey-lighten-3'"
                    class="me-2 text-body-2 text-md-body-1"
                    @click="currContentsIndex = index"
                  >
                    {{ index + 1 }}
                  </v-chip>
                  <div v-if="index === 0" class="corner-badge">
                    <v-icon :icon="'mdi-chart-line-variant'" :size="$vuetify.display.mobile ? 6 : 8" color="white" />
                  </div>
                </div>
                <v-btn
                  icon="mdi-close"
                  variant="text"
                  :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                  class="ms-auto"
                  @click="drawerOpen = false"
                />
              </div>

              <!-- 目录树 -->
              <div class="drawer-toc-content pa-4 pt-0">
                <TreeNode
                  v-if="data && data.toc && data.toc[currContentsIndex]"
                  :node-data="data.toc[currContentsIndex]"
                  :node-infos="data.tocNodeInfos"
                  :course-id="data.course?.id"
                  :path="data.path"
                  :curr-path="String(currContentsIndex + 1)"
                  :depth="1"
                  :is-learning="isLearning"
                  @node-click="drawerOpen = false"
                />
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-dialog>

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
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  rounded="lg"
                  label
                  variant="tonal"
                  color=""
                  class="me-0 px-3 text-body-2 text-md-body-1"
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
                    :size="$vuetify.display.mobile ? 'small' : 'default'"
                    variant="flat"
                    :color="currContentsIndex === index ? 'grey-darken-1' : 'grey-lighten-3'"
                    class="text-body-2 text-md-body-1"
                    @click="currContentsIndex = index"
                  >
                    {{ index + 1 }}
                  </v-chip>
                  <div v-if="index === 0" class="corner-badge">
                    <v-icon icon="mdi-chart-line-variant" :size="$vuetify.display.mobile ? 6 : 8" color="white" />
                  </div>
                </div>
                <v-btn
                  icon
                  :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                  variant="text"
                  class="config-btn ms-auto"
                  @click="configContents = true"
                >
                  <v-icon :size="$vuetify.display.mobile ? 16 : 20">mdi-cog-outline</v-icon>
                </v-btn>
              </div>
            </div>

            <!-- 目录树 -->
            <div class="toc-card">
              <div
                class="toc-tree"
                :class="{ 'toc-tree-hover': isTocHovering }"
                @mouseenter="isTocHovering = true"
                @mouseleave="isTocHovering = false"
              >
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
                @view-deck="handleViewDeck"
              />
            </div>

            <!-- 右侧工具栏 -->
            <div v-if="data" class="right-sidebar">
              <div class="sidebar-sticky">
                <!-- 文章详情页：显示答疑助手和记忆卡片组侧边栏 -->
                <template
                  v-if="
                    currentTab !== 'list' &&
                    currentTab !== 'comment' &&
                    currentTab !== 'memoryCards' &&
                    currentPosting
                  "
                >
                  <!-- AI答疑助手 -->
                  <v-card class="sidebar-card mb-4 mb-md-4 no-border" rounded="lg">
                    <v-card-title class="pa-4 pa-md-4">
                      <div class="d-flex align-center justify-space-between w-100">
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-robot-excited" color="primary" :size="$vuetify.display.mobile ? 20 : 24" class="mr-2"></v-icon>
                          <span class="text-body-1 text-md-h6">答疑助手</span>
                        </div>
                        <v-btn
                          :icon="isAssistantExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down'"
                          variant="text"
                          color="grey-darken-1"
                          :size="$vuetify.display.mobile ? 'x-small' : 'x-small'"
                          @click="isAssistantExpanded = !isAssistantExpanded"
                        ></v-btn>
                      </div>
                    </v-card-title>

                    <v-expand-transition>
                      <v-card-text v-show="isAssistantExpanded" class="pa-4 pa-md-4 pt-0">
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
                            :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                            class="engine-link text-caption text-md-body-2"
                            :prepend-icon="e.icon"
                            :text="e.name"
                          />
                        </div>
                      </v-card-text>
                    </v-expand-transition>
                  </v-card>

                  <!-- 记忆卡片组侧边栏 -->
                  <MemoryCardSidebar
                    :post-id="currentPosting.id"
                    class="mb-4 mb-md-4"
                    @create-deck="handleCreateDeck"
                    @view-deck="handleViewDeck"
                  />
                </template>

                <!-- 其他页面：显示课程信息 -->
                <template v-else>
                  <v-card class="sidebar-card no-border" rounded="lg">
                    <v-card-title class="pa-4 pa-md-4 text-body-1 text-md-h6"> 关于本课程 </v-card-title>
                    <v-card-text class="pa-4 pa-md-4 pt-0">
                      <div class="info-item">
                        <div class="text-caption text-medium-emphasis mb-2">课程描述</div>
                        <div class="text-body-2">{{ data?.course?.description }}</div>
                      </div>
                    </v-card-text>
                  </v-card>
                </template>
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
      v-if="currentPosting"
      v-model="showCreateDeckDialog"
      :post-id="currentPosting.id"
      @created="handleDeckCreated"
    />

    <!-- 卡片组详情对话框 -->
    <DeckDetailDialog
      v-model="showDeckDetailDialog"
      :deck="selectedDeck"
      @add-to-study="handleAddDeck"
    />

    <!-- 移动端浮动按钮 - 仅目录按钮 -->
    <v-btn
      v-if="$vuetify.display.mobile"
      icon
      color="primary"
      :size="$vuetify.display.mobile ? 'large' : 'large'"
      elevation="6"
      class="mobile-toc-fab"
      @click="drawerOpen = true"
    >
      <v-icon :size="$vuetify.display.mobile ? 20 : 24">mdi-format-list-bulleted</v-icon>
      <v-tooltip activator="parent" location="left">课程目录</v-tooltip>
    </v-btn>
  </DefaultLayout>
</template>

<script lang="ts">
export default {
  name: 'ContentReadPage',
}
</script>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import CourseHeader from '@/components/features/read/CourseHeader.vue'
import TreeNode from '@/components/common/TreeNode.vue'
import PostingList from '@/components/features/read/PostingList.vue'
import ConfigContentsDialog from '@/components/features/read/ConfigContentsDialog.vue'
import CreateDeckDialog from '@/components/features/read/CreateDeckDialog.vue'
import MemoryCardSidebar from '@/components/features/read/MemoryCardSidebar.vue'
import DeckDetailDialog from '@/components/features/read/DeckDetailDialog.vue'
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
const selectedDeck = ref<MemoryCardDeck | null>(null)
const showDeckDetailDialog = ref(false)
const loading = ref(false)
const error = ref<string | null>(null)
const isTocHovering = ref(false)
const drawerOpen = ref(false)

// 关闭drawer
const closeDrawer = () => {
  drawerOpen.value = false
}

// 数据处理
const nodes = ref<any[]>([])
const currNodeId = ref(0)
const lastPathNode = ref<any>(null)
const pathText = ref('')

// 使用 useFetch 加载页面数据
const {
  data,
  loading: dataLoading,
  execute: loadData,
} = useFetch<ReadResponse>({
  fetchFn: () => {
    if (route.query.commentId) {
      return pageApi.readByComment(Number(route.query.commentId))
    } else if (route.query.postId) {
      return pageApi.readByPost(Number(route.query.postId))
    } else if (route.query.nodeId) {
      return pageApi.readByNode(Number(route.query.nodeId))
    } else if (route.query.courseId && route.query.path) {
      return pageApi.readByCoursePath(Number(route.query.courseId), route.query.path as string)
    } else if (route.query.courseId) {
      return pageApi.readByCoursePath(Number(route.query.courseId), '')
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
  },
})

// 是否为主课程
const isMainCourse = computed(() => {
  if (data.value?.course && data.value.parentCourse) {
    return data.value.course.id === data.value.parentCourse.id
  }
  return true
})

// AI 引擎列表
const aiEngines = [
  { name: 'ChatGPT', href: 'https://chatgpt.com', color: 'grey-darken-1', icon: 'mdi-robot' },
  {
    name: 'Claude',
    href: 'https://claude.ai',
    color: 'grey-darken-1',
    icon: 'mdi-alpha-c-circle-outline',
  },
  { name: 'Gemini', href: 'https://gemini.google.com', color: 'grey-darken-1', icon: 'mdi-google' },
  {
    name: 'DeepSeek',
    href: 'https://chat.deepseek.com',
    color: 'grey-darken-1',
    icon: 'mdi-radar',
  },
]

// 处理创建卡片组成功
const handleDeckCreated = (deck: MemoryCardDeck) => {
  console.log('Deck created:', deck)
}

// 处理数据
const processData = () => {
  console.log('data:', data.value)
  if (!data.value?.path) return

  // 解析路径
  nodes.value = data.value.path.split('-')
  nodes.value[0] = String(Number(nodes.value[0]) - 1)

  // 遍历 toc 获取 lastPathNode
  lastPathNode.value = nodes.value.reduce((acc: any, key: any) => acc?.[key], data.value.toc)

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

// 返回上一页
const goBackToCourse = () => {
  router.back()
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

// 处理创建卡片组
const handleCreateDeck = () => {
  if (currentPosting.value) {
    showCreateDeckDialog.value = true
  }
}

// 处理查看卡片组详情
const handleViewDeck = (deck: MemoryCardDeck) => {
  selectedDeck.value = deck
  showDeckDetailDialog.value = true
}

// 处理添加卡片组到学习计划
const handleAddDeck = async (deck: MemoryCardDeck) => {
  console.log('ContentReadPage received addDeck event:', deck)

  try {
    // 获取当前课程ID
    const courseId = data.value?.course?.id
    if (!courseId) {
      console.error('无法确定课程信息')
      return
    }

    // 调用API添加卡片组到记忆库
    const response = await memoryApi.addDeckToMemoryBank({
      deckId: deck.id,
      courseId: courseId,
    })

    if (response.code === 200) {
      console.log(`已将"${deck.title}"添加到${data.value.course.name}课程的学习计划`)
    } else {
      console.error('添加失败，请重试')
    }
  } catch (error) {
    console.error('Failed to add deck to memory bank:', error)
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
  () => [
    route.params.id,
    route.query.path,
    route.query.nodeId,
    route.query.postId,
    route.query.commentId,
  ],
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

/* 固定课程头部 */
.course-header-sticky {
  position: sticky;
  top: 56px;
  background-color: white;
  z-index: 999;
  padding-bottom: 8px;
  max-width: 1470px;
  margin: 0 auto;
}

/* 三栏布局 */
.read-content {
  display: flex;
  position: relative;
  z-index: 1;
  max-width: 1470px;
  width: 100%;
  margin: 0 auto;
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
  transition:
    top 0.3s ease,
    max-height 0.3s ease;
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
  width: 2px;
}

.toc-tree::-webkit-scrollbar-track {
  background: transparent;
}

.toc-tree::-webkit-scrollbar-thumb {
  background-color: transparent;
  border-radius: 2px;
}

.toc-tree-hover::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
}

.toc-tree-hover::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
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

@media (max-width: 750px) {
  .center-content {
    padding: 16px 20px 32px 20px;
  }
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
  transition:
    top 0.3s ease,
    max-height 0.3s ease;
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

/* 移动端目录抽屉样式 */
.drawer-card {
  overflow: hidden !important;
}

.drawer-card-content {
  overflow: hidden !important;
}

.drawer-card-content::-webkit-scrollbar {
  display: none;
}

.drawer-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: white;
}

.toc-chips-row {
  flex-shrink: 0;
  border-bottom: 1px solid rgb(var(--v-theme-border));
}

.drawer-toc-content {
  flex: 1;
  overflow-y: auto;
}

/* 隐藏drawer滚动条 */
.drawer-toc-content::-webkit-scrollbar {
  width: 0;
  height: 0;
}

/* 移动端浮动按钮 */
.mobile-toc-fab {
  position: fixed;
  bottom: 80px;
  right: 24px;
  z-index: 1000;
}

/* 中等屏幕：隐藏右侧栏，保持左侧目录和内容区 */
@media (max-width: 1700px) {
  .course-header-sticky {
    max-width: 1110px;
  }

  .read-content {
    max-width: 1110px;
  }

  .right-sidebar {
    display: none;
  }

  .center-right-wrapper {
    justify-content: center;
  }
}

/* 小屏幕：隐藏左侧目录，内容区保持最大750px居中 */
@media (max-width: 1280px) and (min-width: 751px) {
  .course-header-sticky {
    max-width: 750px;
  }

  .read-content {
    max-width: 750px;
    overflow-x: hidden !important;
  }

  .toc-sidebar {
    display: none;
  }

  .center-right-wrapper {
    justify-content: center;
  }

  .center-content {
    padding: 0 !important;
  }
}

/* 超小屏幕：内容区可以缩小到屏幕宽度 */
@media (max-width: 750px) {
  .course-header-sticky {
    max-width: none;
  }

  .read-content {
    max-width: none;
    width: 100% !important;
    overflow-x: hidden !important;
  }

  .toc-sidebar {
    display: none;
  }

  .center-right-wrapper {
    width: 100% !important;
    max-width: none !important;
  }

  .center-content {
    flex: 1 !important;
    max-width: none !important;
    min-width: 0 !important;
    padding: 0 4px 40px 4px !important;
    width: 100% !important;
  }
}
</style>
