<template>
  <DefaultLayout>
    <div class="read-page">
      <!-- 课程头部 -->
      <div class="course-header-sticky">
        <CourseHeader
          v-if="data"
          :parent-course-info="data.parentCourse"
          :current-course="data.course"
          :sub-course-list="data.subCourseList"
          :is-main-course="isMainCourse"
          :is-learning="false"
        />
      </div>

      <div class="read-content">
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
                :is-learning="false"
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

                  <!-- 记忆卡片组侧边栏 -->
                  <MemoryCardSidebar
                    :post-id="currentPosting.id"
                    class="mb-4"
                    @create-deck="handleCreateDeck"
                    @view-deck="handleViewDeck"
                  />
                </template>

                <!-- 其他页面：显示课程信息 -->
                <template v-else>
                  <v-card class="sidebar-card no-border" rounded="lg">
                    <v-card-title class="pa-4"> 关于本课程 </v-card-title>
                    <v-card-text class="pa-4 pt-0">
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
  </DefaultLayout>
</template>
<script lang="ts">
export default {
  name: 'NodePostsPage',
}
</script>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import CourseHeader from '@/components/features/read/CourseHeader.vue'
import PostingList from '@/components/features/read/PostingList.vue'
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
const showCreateDeckDialog = ref(false)
const isAssistantExpanded = ref(true)
const currentTab = ref('list')
const currentPosting = ref(null)
const selectedDeck = ref<MemoryCardDeck | null>(null)
const showDeckDetailDialog = ref(false)

// 是否为主课程
const isMainCourse = computed(() => {
  if (data.value?.course && data.value.parentCourse) {
    return data.value.course.id === data.value.parentCourse.id
  }
  return true
})

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
    const nodeId = Number(route.query.nodeId)
    return pageApi.readByNode(nodeId)
  },
  immediate: true,
  onDataReady: () => {
    // 处理投票类型
    data.value.otherPostings?.forEach((posting: any) => {
      if (posting.voteType === 0) {
        posting.voteType = null
      }
    })
    // 数据赋值完成后处理数据
    processData()
  },
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
  showCreateDeckDialog.value = false
}

// 处理数据
const processData = () => {
  if (!data.value) return

  // 设置当前节点ID
  currNodeId.value = data.value.node?.id || 0

  // 设置路径文本（如果有的话）
  if (data.value.path) {
    nodes.value = data.value.path.split('-')
    pathText.value = `${data.value.course?.name || ''}/`
  }
}

// 滚动监听
const handleScroll = () => {
  showFixedBar.value = window.scrollY > 100
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
  console.log('NodePostsPage received addDeck event:', deck)

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

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

// 监听路由变化，重新加载数据
watch(
  () => route.query.nodeId,
  () => {
    loadData()
  }
)

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.read-page {
  min-height: 100vh;
  background-color: #ffffff;
  max-width: 1110px;
  margin: 0 auto;
}

/* 固定课程头部 */
.course-header-sticky {
  position: sticky;
  top: 56px;
  background-color: white;
  z-index: 999;
  padding-bottom: 8px;
}

/* 布局 - 无左侧目录 */
.read-content {
  display: flex;
  position: relative;
  z-index: 1;
  max-width: 100%;
  width: 100%;
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

@media (max-width: 960px) {
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
