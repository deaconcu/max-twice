<template>
  <div class="read-page">
    <AppHeader />
    <LeftSidebar />

    <!-- 固定顶部横条 - 滚动时显示 -->
    <div class="fixed-top-bar" :class="{ 'show': showFixedBar }">
      <div class="fixed-bar-content">
        <!-- 左侧：返回按钮 + 课程和子课程信息 + 完整节点路径 -->
        <div class="d-flex align-center flex-grow-1" style="gap: 12px;">
          <v-btn
            variant="text"
            color="grey-darken-2"
            size="small"
            @click="$router.back()"
          >
            <v-icon icon="mdi-arrow-left" size="18" class="mr-1"></v-icon>
            返回
          </v-btn>
          <div class="d-flex align-center" style="gap: 6px;">
            <span class="fixed-bar-course-name">{{ courseData.title }}</span>
            <v-icon icon="mdi-chevron-right" size="16" color="grey"></v-icon>
            <span class="fixed-bar-subcourse-name">{{ subCourses[currentSubCourseIndex].name }}</span>
            <v-icon icon="mdi-chevron-right" size="16" color="grey"></v-icon>
            <span class="fixed-bar-path">{{ pathText }}</span>
          </div>
        </div>

        <!-- 右侧：统计信息 + 操作按钮 -->
        <div class="d-flex align-center" style="gap: 32px;">
          <div class="d-flex align-center" style="gap: 20px;">
            <div class="d-flex align-center" style="gap: 4px;">
              <v-icon icon="mdi-format-list-bulleted" size="14" color="grey"></v-icon>
              <span class="fixed-bar-stat">{{ subCourses[currentSubCourseIndex].totalNodes }} 节</span>
            </div>
            <div class="d-flex align-center" style="gap: 4px;">
              <v-icon icon="mdi-check-circle" size="14" color="success"></v-icon>
              <span class="fixed-bar-stat">完成 {{ subCourses[currentSubCourseIndex].completedNodes }} 节</span>
            </div>
            <div class="d-flex align-center" style="gap: 4px;">
              <v-icon icon="mdi-chart-line" size="14" color="primary"></v-icon>
              <span class="fixed-bar-stat">{{ subCourses[currentSubCourseIndex].progress }}%</span>
            </div>
          </div>
          <div class="d-flex align-center" style="gap: 8px;">
            <v-btn
              :color="isLearning ? 'success' : 'primary'"
              :variant="isLearning ? 'outlined' : 'flat'"
              size="x-small"
              rounded="lg"
              class="text-none"
              @click="toggleLearning"
            >
              <v-icon size="14" class="mr-1">{{ isLearning ? 'mdi-check-circle' : 'mdi-play-circle' }}</v-icon>
              {{ isLearning ? '学习中' : '开始学习' }}
            </v-btn>
            <v-btn
              :color="courseData.subscribed ? 'error' : 'grey-darken-2'"
              :variant="courseData.subscribed ? 'flat' : 'outlined'"
              size="x-small"
              rounded="lg"
              class="text-none"
              @click="toggleSubscribe"
            >
              <v-icon size="14" class="mr-1">{{ courseData.subscribed ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>
              {{ courseData.subscribed ? '已订阅' : '订阅' }}
            </v-btn>
          </div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <!-- 返回按钮 -->
      <v-btn
        variant="text"
        color="grey-darken-2"
        class="mb-4"
        @click="$router.back()"
      >
        <v-icon icon="mdi-arrow-left" class="mr-1"></v-icon>
        返回
      </v-btn>

      <!-- 课程信息卡片 -->
      <v-card rounded="lg" class="course-info-card mb-2">
        <v-card-text class="pa-6">
          <!-- 整体课程信息 -->
          <div class="d-flex align-center mb-4">
            <v-avatar color="grey-lighten-3" size="64" rounded="lg" class="mr-4">
              <v-icon icon="mdi-book-open-variant" color="#666666" size="32"></v-icon>
            </v-avatar>
            <div class="flex-grow-1">
              <h1 class="text-h5 font-weight-bold text-grey-darken-4 mb-1">
                {{ courseData.title }}
              </h1>
              <p class="text-body-2 text-grey-darken-2 mb-0">
                {{ courseData.description }}
              </p>
            </div>
          </div>

          <!-- 子课程切换 -->
          <div class="sub-courses-section">
            <div class="d-flex align-center mb-3">
              <v-icon icon="mdi-book-multiple" size="16" color="grey-darken-2" class="mr-2"></v-icon>
              <span class="text-caption font-weight-bold text-grey-darken-3 text-uppercase" style="letter-spacing: 0.05em;">子课程</span>
            </div>
            <div class="d-flex align-center ga-2 flex-wrap mb-4">
              <div
                v-for="(subCourse, index) in subCourses"
                :key="index"
                class="sub-course-chip"
                :class="{ 'sub-course-active': currentSubCourseIndex === index }"
                @click="currentSubCourseIndex = index"
              >
                <span class="sub-course-name">{{ subCourse.name }}</span>
                <span class="sub-course-progress">{{ subCourse.progress }}%</span>
              </div>
            </div>

            <!-- 当前子课程的详细信息 -->
            <div class="current-subcourse-info">
              <!-- 子课程标题和描述 -->
              <div class="d-flex align-center mb-3">
                <v-avatar color="primary" size="32" rounded="lg" class="mr-3">
                  <v-icon icon="mdi-book-open-page-variant" color="white" size="16"></v-icon>
                </v-avatar>
                <div class="flex-grow-1">
                  <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-1">
                    {{ subCourses[currentSubCourseIndex].name }}
                  </h3>
                  <p class="text-caption text-grey-darken-2 mb-0">
                    {{ subCourses[currentSubCourseIndex].description }}
                  </p>
                </div>
              </div>

              <!-- 统计信息和操作 -->
              <div class="d-flex align-center justify-space-between">
                <div class="d-flex align-center flex-wrap" style="gap: 16px;">
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-format-list-bulleted" size="16" color="primary" class="mr-1"></v-icon>
                    <span class="text-caption text-grey-darken-2">
                      共 <span class="font-weight-bold text-grey-darken-4">{{ subCourses[currentSubCourseIndex].totalNodes }}</span> 节
                    </span>
                  </div>
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-check-circle" size="16" color="success" class="mr-1"></v-icon>
                    <span class="text-caption text-grey-darken-2">
                      完成 <span class="font-weight-bold text-grey-darken-4">{{ subCourses[currentSubCourseIndex].completedNodes }}</span> 节
                    </span>
                  </div>
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-chart-line" size="16" color="primary" class="mr-1"></v-icon>
                    <span class="text-caption text-grey-darken-2">
                      进度 <span class="font-weight-bold text-grey-darken-4">{{ subCourses[currentSubCourseIndex].progress }}%</span>
                    </span>
                  </div>
                </div>

                <div class="d-flex align-center" style="gap: 8px;">
                  <v-btn
                    :color="isLearning ? 'success' : 'primary'"
                    :variant="isLearning ? 'outlined' : 'flat'"
                    size="small"
                    rounded="lg"
                    class="text-none"
                    @click="toggleLearning"
                  >
                    <v-icon size="16" class="mr-1">{{ isLearning ? 'mdi-check-circle' : 'mdi-play-circle' }}</v-icon>
                    {{ isLearning ? '学习中' : '开始学习' }}
                  </v-btn>
                  <v-btn
                    :color="courseData.subscribed ? 'error' : 'grey-darken-2'"
                    :variant="courseData.subscribed ? 'flat' : 'outlined'"
                    size="small"
                    rounded="lg"
                    class="text-none"
                    @click="toggleSubscribe"
                  >
                    <v-icon size="16" class="mr-1">{{ courseData.subscribed ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>
                    {{ courseData.subscribed ? '已订阅' : '订阅' }}
                  </v-btn>
                </div>
              </div>
            </div>
          </div>
        </v-card-text>
      </v-card>

      <div class="read-content">
      <!-- 左侧目录 - 靠边固定 -->
      <div class="toc-sidebar">
        <div class="toc-sticky-wrapper">
          <!-- 目录组选择卡片 -->
          <div class="toc-groups-card">
            <div class="toc-header">
              <div class="d-flex align-center">
                <v-icon icon="mdi-view-list" size="14" color="grey" class="mr-2"></v-icon>
                <span class="toc-title">课程目录</span>
              </div>
              <div class="toc-actions">
                <v-btn
                  icon
                  size="x-small"
                  variant="text"
                  :class="{ 'rotate-180': openContentsList }"
                  @click="openContentsList = !openContentsList"
                >
                  <v-icon size="16">mdi-chevron-down</v-icon>
                </v-btn>
                <v-btn
                  icon
                  size="x-small"
                  variant="text"
                  @click="configContents = true"
                >
                  <v-icon size="16">mdi-cog-outline</v-icon>
                </v-btn>
              </div>
            </div>
            <v-expand-transition>
              <div v-if="openContentsList" class="toc-chips">
                <div
                  v-for="(item, index) in tocGroups"
                  :key="index"
                  class="toc-chip"
                  :class="{
                    'chip-active': currentTocGroupIndex === index,
                    'chip-primary': index === 0
                  }"
                  @click="currentTocGroupIndex = index"
                >
                  <div class="chip-inner">
                    <span class="chip-number">{{ index + 1 }}</span>
                  </div>
                  <div v-if="index === 0" class="corner-badge">
                    <v-icon icon="mdi-chart-line-variant" size="8" color="white"></v-icon>
                  </div>
                </div>
              </div>
            </v-expand-transition>
          </div>

          <!-- 目录树 -->
          <div class="toc-card">
            <h3 class="text-h6 font-weight-bold mb-1">目录</h3>
            <div class="toc-tree">
              <TreeNode
                v-for="(item, index) in tocData"
                :key="index"
                :node="item"
                :active-node="activeNode"
                @node-click="handleNodeClick"
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
        <!-- 节点路径（面包屑） -->
        <div class="node-path mb-2">
          <div class="d-flex align-center">
            <span class="text-caption text-medium-emphasis">{{ pathText }}</span>
          </div>
        </div>

        <!-- 节点头部 -->
        <div class="node-header mb-4">
          <div class="d-flex align-center justify-space-between mb-3">
            <div class="d-flex align-center">
              <v-icon icon="mdi-list-box-outline" color="primary" size="24"></v-icon>
              <h2 class="text-h5 font-weight-bold ms-3">{{ currentNode.name }}</h2>
            </div>
            <v-btn
              v-if="isLearning"
              :color="currentNode.isCompleted ? 'grey-lighten-2' : 'success'"
              :variant="currentNode.isCompleted ? 'outlined' : 'flat'"
              rounded="lg"
              size="small"
              class="px-4"
              :prepend-icon="currentNode.isCompleted ? 'mdi-check-circle' : 'mdi-circle-outline'"
              @click="toggleNodeCompletion"
            >
              <span
                class="font-weight-medium"
                :class="currentNode.isCompleted ? 'text-grey-darken-2' : 'text-white'"
              >
                {{ currentNode.isCompleted ? '已完成' : '完成学习' }}
              </span>
            </v-btn>
          </div>
          <p v-if="currentNode.description" class="text-body-2 text-medium-emphasis mb-0">
            {{ currentNode.description }}
          </p>
        </div>

        <!-- Tab栏和操作按钮 -->
        <div class="tabs-actions-bar">
          <div class="tabs-actions-content">
            <v-tabs v-model="activeTab" density="compact" color="primary">
              <v-tab value="list" class="px-3">
                <v-icon icon="mdi-list-box-outline" size="16" class="mr-2"></v-icon>
                <span class="font-weight-medium">文章列表</span>
              </v-tab>
              <v-tab value="comment" class="px-3">
                <v-icon icon="mdi-comment-outline" size="16" class="mr-2"></v-icon>
                <span class="font-weight-medium">{{ currentNode.commentCount }} 评论</span>
              </v-tab>
              <v-tab value="memoryCards" class="px-3">
                <v-icon icon="mdi-cards-outline" size="16" class="mr-2"></v-icon>
                <span class="font-weight-medium">记忆卡片</span>
              </v-tab>
            </v-tabs>

            <div class="d-flex align-center ga-2">
              <v-btn
                variant="flat"
                color="grey-lighten-4"
                rounded="lg"
                density="comfortable"
                class="px-3"
              >
                <v-icon icon="mdi-note-plus-outline" size="14" class="mr-2"></v-icon>
                <span class="font-weight-medium text-grey-darken-3">添加文章</span>
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-4"
                rounded="lg"
                density="comfortable"
                class="px-3"
              >
                <v-icon icon="mdi-format-list-group-plus" size="14" class="mr-2"></v-icon>
                <span class="font-weight-medium text-grey-darken-3">添加目录</span>
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-4"
                rounded="lg"
                density="comfortable"
                class="px-3"
              >
                <v-icon icon="mdi-account-plus-outline" size="14" class="mr-2"></v-icon>
                <span class="font-weight-medium text-grey-darken-3">邀请回答</span>
              </v-btn>
            </div>
          </div>
        </div>

        <!-- 文章内容 -->
        <div class="post-card mb-4">
          <h2 class="text-h5 font-weight-bold mb-4">{{ currentPost.title }}</h2>
          <div class="post-content" v-html="currentPost.content"></div>

          <div class="d-flex align-center mt-6 ga-4">
            <div class="d-flex align-center ga-1">
              <v-btn icon variant="text" size="small">
                <v-icon>mdi-arrow-up-bold</v-icon>
              </v-btn>
              <span class="font-weight-bold">{{ currentPost.votes }}</span>
              <v-btn icon variant="text" size="small">
                <v-icon>mdi-arrow-down-bold</v-icon>
              </v-btn>
            </div>

            <v-btn variant="text" class="text-none">
              <v-icon left>mdi-comment-outline</v-icon>
              {{ currentPost.commentCount }} 评论
            </v-btn>

            <v-btn variant="text" class="text-none">
              <v-icon left>mdi-share-variant</v-icon>
              分享
            </v-btn>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="comments-card">
          <h3 class="text-h6 font-weight-bold mb-4">评论 ({{ currentPost.commentCount }})</h3>

          <!-- 评论输入框 -->
          <v-textarea
            v-model="newComment"
            variant="outlined"
            placeholder="写下你的评论..."
            rows="3"
            class="mb-4"
          />
          <v-btn color="primary" class="text-none">发表评论</v-btn>

          <!-- 评论列表 -->
          <div class="comments-list mt-6">
              <div
                v-for="comment in comments"
                :key="comment.id"
                class="comment-item mb-4 pa-4"
              >
                <div class="d-flex align-start">
                  <v-avatar size="40" class="mr-3">
                    <v-icon>mdi-account-circle</v-icon>
                  </v-avatar>
                  <div class="flex-grow-1">
                    <div class="d-flex align-center mb-1">
                      <span class="font-weight-bold mr-2">{{ comment.author }}</span>
                      <span class="text-caption text-medium-emphasis">{{ comment.time }}</span>
                    </div>
                    <p class="mb-2">{{ comment.content }}</p>
                    <div class="d-flex align-center ga-2">
                      <v-btn icon variant="text" size="x-small">
                        <v-icon size="small">mdi-arrow-up-bold</v-icon>
                      </v-btn>
                      <span class="text-caption">{{ comment.votes }}</span>
                      <v-btn icon variant="text" size="x-small">
                        <v-icon size="small">mdi-arrow-down-bold</v-icon>
                      </v-btn>
                      <v-btn variant="text" size="small" class="text-none ml-2">
                        回复
                      </v-btn>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      <!-- 右侧工具栏 - 靠边固定 -->
      <div class="right-sidebar">
        <div class="sidebar-sticky">
          <!-- AI答疑助手 -->
          <v-card class="sidebar-card mb-4" border rounded="lg">
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

                <div class="d-flex flex-wrap mt-5" style="gap: 8px;">
                  <div class="text-body-2 w-100 text-grey-darken-2 font-weight-bold">常用 AI 引擎：</div>
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
          <v-card class="sidebar-card mb-4" border rounded="lg">
            <v-card-title class="pa-4">
              <v-icon left color="primary">mdi-cards</v-icon>
              记忆卡片组
            </v-card-title>
            <v-card-text class="pa-4 pt-0">
              <v-list>
                <v-list-item
                  v-for="deck in memoryDecks"
                  :key="deck.id"
                  class="px-0"
                >
                  <v-list-item-title class="text-body-2">
                    {{ deck.title }}
                  </v-list-item-title>
                  <v-list-item-subtitle class="text-caption">
                    {{ deck.cardCount }} 张卡片
                  </v-list-item-subtitle>
                </v-list-item>
              </v-list>
              <v-btn block color="primary" variant="outlined" class="text-none mt-2">
                创建卡片组
              </v-btn>
            </v-card-text>
          </v-card>

          <!-- 课程信息 -->
          <v-card class="sidebar-card" border rounded="lg">
            <v-card-title class="pa-4">
              关于本课程
            </v-card-title>
            <v-card-text class="pa-4 pt-0">
              <div class="info-item mb-3">
                <div class="text-caption text-medium-emphasis">总节数</div>
                <div class="text-body-2 font-weight-bold">{{ courseData.totalNodes }}</div>
              </div>
              <div class="info-item mb-3">
                <div class="text-caption text-medium-emphasis">已完成</div>
                <div class="text-body-2 font-weight-bold">{{ courseData.completedNodes }}</div>
              </div>
              <div class="info-item">
                <div class="text-caption text-medium-emphasis">学习进度</div>
                <v-progress-linear
                  :model-value="courseData.progress"
                  color="primary"
                  height="8"
                  rounded
                  class="mt-2"
                />
                <div class="text-caption text-right mt-1">{{ courseData.progress }}%</div>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </div>
        </div>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'
import TreeNode from '@/components/TreeNode.vue'

const isLearning = ref(true)
const newComment = ref('')
const activeNode = ref(null)
const activeTab = ref('list')
const isAssistantExpanded = ref(true)
const showFixedBar = ref(false)

// 滚动监听
const handleScroll = () => {
  // 当滚动超过 380px 时显示固定横条
  showFixedBar.value = window.scrollY > 380
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

// AI 引擎列表
const aiEngines = [
  { name: 'ChatGPT', href: 'https://chatgpt.com', color: 'green-darken-2', icon: 'mdi-robot' },
  { name: 'Claude', href: 'https://claude.ai', color: 'indigo-darken-2', icon: 'mdi-alpha-c-circle-outline' },
  { name: 'Gemini', href: 'https://gemini.google.com', color: 'blue-darken-2', icon: 'mdi-google' },
  { name: 'DeepSeek', href: 'https://chat.deepseek.com', color: 'red-darken-4', icon: 'mdi-radar' }
]

// 子课程相关
const currentSubCourseIndex = ref(0)
const subCourses = ref([
  {
    name: 'Vue 3 基础',
    description: '学习 Vue 3 的核心概念、响应式系统、组件基础和模板语法',
    progress: 80,
    totalNodes: 12,
    completedNodes: 10
  },
  {
    name: 'Vue 3 进阶',
    description: '深入学习组合式 API、自定义指令、插件开发和高级组件模式',
    progress: 60,
    totalNodes: 15,
    completedNodes: 9
  },
  {
    name: 'Vue 3 实战',
    description: '通过完整项目实战掌握 Vue 3 生态系统，包括路由、状态管理和构建部署',
    progress: 30,
    totalNodes: 20,
    completedNodes: 6
  }
])

// 目录组相关
const openContentsList = ref(true)
const configContents = ref(false)
const currentTocGroupIndex = ref(0)
const tocGroups = ref([
  { name: '主线目录' },
  { name: '扩展目录' },
  { name: '实战目录' }
])

// 节点路径
const pathText = ref('Vue 3 完整教程 / 2. 响应式系统 / 2.1 响应式基础')

// 当前节点数据
const currentNode = ref({
  name: '2.1 响应式基础',
  description: '学习 Vue 3 响应式系统的核心API和基本用法',
  isCompleted: false,
  commentCount: 8
})

const handleNodeClick = (node: any) => {
  activeNode.value = node
  console.log('Clicked node:', node.name)
}

const toggleLearning = () => {
  isLearning.value = !isLearning.value
}

const toggleNodeCompletion = () => {
  currentNode.value.isCompleted = !currentNode.value.isCompleted
}

const toggleSubscribe = () => {
  courseData.value.subscribed = !courseData.value.subscribed
}

// Mock 数据
const courseData = ref({
  title: 'Vue 3 完整教程',
  description: '从入门到精通，掌握 Vue 3 的核心概念和最佳实践。本课程涵盖响应式系统、组合式 API、组件开发、路由管理、状态管理等核心内容，通过大量实战案例帮助你深入理解 Vue 3 的设计理念和使用方法，最终能够独立开发完整的 Vue 3 应用程序。课程包含从基础语法到高级特性的完整知识体系，适合零基础学员和有一定经验的开发者。每个章节都配有详细的代码示例和练习题，帮助你巩固所学知识并应用到实际项目中。',
  subscribed: false,
  totalNodes: 20,
  completedNodes: 5,
  progress: 25
})

// 多级树形目录数据
const tocData = ref([
  {
    name: '1. Vue 3 入门',
    completed: true,
    expanded: true,
    children: [
      { name: '1.1 Vue 3 简介', completed: true },
      { name: '1.2 安装和设置', completed: true },
      {
        name: '1.3 基本概念',
        completed: true,
        expanded: true,
        children: [
          { name: '1.3.1 模板语法', completed: true },
          { name: '1.3.2 数据绑定', completed: true },
          { name: '1.3.3 条件渲染', completed: false }
        ]
      },
      { name: '1.4 开发工具', completed: false }
    ]
  },
  {
    name: '2. 响应式系统',
    completed: false,
    expanded: false,
    children: [
      {
        name: '2.1 响应式基础',
        completed: false,
        children: [
          { name: '2.1.1 reactive()', completed: false },
          { name: '2.1.2 ref()', completed: false },
          { name: '2.1.3 toRef() 和 toRefs()', completed: false }
        ]
      },
      {
        name: '2.2 计算属性和侦听器',
        completed: false,
        children: [
          { name: '2.2.1 computed()', completed: false },
          { name: '2.2.2 watch()', completed: false },
          { name: '2.2.3 watchEffect()', completed: false }
        ]
      },
      { name: '2.3 响应式原理', completed: false }
    ]
  },
  {
    name: '3. Composition API',
    completed: false,
    expanded: false,
    children: [
      { name: '3.1 setup() 函数', completed: false },
      {
        name: '3.2 生命周期钩子',
        completed: false,
        children: [
          { name: '3.2.1 onMounted', completed: false },
          { name: '3.2.2 onUpdated', completed: false },
          { name: '3.2.3 onUnmounted', completed: false }
        ]
      },
      { name: '3.3 依赖注入', completed: false },
      { name: '3.4 组合式函数', completed: false }
    ]
  },
  {
    name: '4. 组件开发',
    completed: false,
    expanded: false,
    children: [
      { name: '4.1 组件基础', completed: false },
      { name: '4.2 Props', completed: false },
      { name: '4.3 事件', completed: false },
      { name: '4.4 插槽', completed: false },
      {
        name: '4.5 组件通信',
        completed: false,
        children: [
          { name: '4.5.1 父子组件通信', completed: false },
          { name: '4.5.2 兄弟组件通信', completed: false },
          { name: '4.5.3 跨级组件通信', completed: false }
        ]
      }
    ]
  },
  {
    name: '5. 路由管理',
    completed: false,
    expanded: false,
    children: [
      { name: '5.1 Vue Router 安装', completed: false },
      { name: '5.2 路由配置', completed: false },
      { name: '5.3 动态路由', completed: false },
      { name: '5.4 导航守卫', completed: false },
      { name: '5.5 路由懒加载', completed: false }
    ]
  },
  {
    name: '6. 状态管理',
    completed: false,
    expanded: false,
    children: [
      { name: '6.1 Pinia 简介', completed: false },
      { name: '6.2 定义 Store', completed: false },
      { name: '6.3 State', completed: false },
      { name: '6.4 Getters', completed: false },
      { name: '6.5 Actions', completed: false }
    ]
  },
  {
    name: '7. 高级特性',
    completed: false,
    expanded: false,
    children: [
      { name: '7.1 指令', completed: false },
      { name: '7.2 Teleport', completed: false },
      { name: '7.3 Suspense', completed: false },
      { name: '7.4 性能优化', completed: false }
    ]
  },
  {
    name: '8. 项目实战',
    completed: false,
    expanded: false,
    children: [
      { name: '8.1 项目搭建', completed: false },
      { name: '8.2 UI 框架集成', completed: false },
      { name: '8.3 API 请求封装', completed: false },
      { name: '8.4 权限管理', completed: false },
      { name: '8.5 项目部署', completed: false }
    ]
  }
])

const currentPost = ref({
  title: 'Vue 3 响应式系统深度解析',
  content: `
    <p>Vue 3 的响应式系统是框架的核心特性之一。它基于 Proxy 实现，相比 Vue 2 的 Object.defineProperty 有更好的性能和更完整的功能支持。在本文中，我们将深入探讨 Vue 3 响应式系统的工作原理、核心 API 以及最佳实践。</p>

    <h3>为什么需要响应式系统</h3>
    <p>在现代前端开发中，我们经常需要处理动态数据。当数据发生变化时，界面应该自动更新以反映最新的状态。这就是响应式系统存在的意义——它能够自动追踪数据的变化，并触发相应的视图更新，让开发者无需手动操作 DOM。</p>
    <p>传统的前端开发需要开发者手动监听数据变化，然后更新对应的 DOM 元素。这种方式不仅繁琐，而且容易出错。Vue 的响应式系统通过自动化这个过程，极大地提升了开发效率和代码的可维护性。</p>

    <h3>Vue 2 vs Vue 3 响应式实现</h3>
    <p>Vue 2 使用 Object.defineProperty 来实现响应式。这种方式虽然可行，但存在一些局限性：无法检测对象属性的添加或删除、无法直接监听数组的索引和 length 属性变化、性能开销较大等。</p>
    <p>Vue 3 采用了 ES6 的 Proxy 来重写响应式系统。Proxy 是一个更底层、更强大的 API，它能够拦截对象的所有操作，包括属性的读取、设置、删除等。这使得 Vue 3 的响应式系统更加完善和高效。</p>
    <p>使用 Proxy 的优势包括：可以检测到对象属性的添加和删除、可以直接监听数组的变化、性能更好、代码实现更简洁等。这些改进让 Vue 3 的响应式系统更加强大和可靠。</p>

    <h3>核心 API 详解</h3>

    <h4>reactive() - 创建响应式对象</h4>
    <p>reactive() 是 Vue 3 中用于创建响应式对象的核心 API。它接收一个普通对象，返回该对象的响应式代理。当你修改响应式对象的属性时，所有依赖该属性的地方都会自动更新。</p>
    <p>使用 reactive() 时需要注意：它只能用于对象类型（对象、数组、Map、Set 等），不能用于基本类型；返回的响应式代理会深层递归，即嵌套的对象也会变成响应式的；解构会导致响应式丢失，需要使用 toRefs() 来保持响应式。</p>
    <p>在实际开发中，reactive() 适合用于组合多个相关联的状态。例如，表单数据、用户信息对象等场景都很适合使用 reactive()。</p>

    <h4>ref() - 创建响应式引用</h4>
    <p>ref() 用于创建一个响应式引用，它可以包装任何类型的值，包括基本类型。ref() 返回一个包含 value 属性的对象，通过 .value 来访问和修改实际的值。</p>
    <p>ref() 的特点是：可以用于基本类型和对象类型；在模板中使用时会自动解包，不需要 .value；在 JavaScript 代码中需要通过 .value 访问；可以方便地在不同组件之间传递响应式数据。</p>
    <p>什么时候用 ref() 而不是 reactive()？一般来说，当你需要存储基本类型的值时，必须使用 ref()。对于对象类型，两者都可以使用，但 ref() 更适合需要整体替换的场景，而 reactive() 更适合需要修改内部属性的场景。</p>

    <h4>computed() - 计算属性</h4>
    <p>computed() 用于创建计算属性，它会基于响应式依赖进行缓存。只有当依赖的响应式数据发生变化时，计算属性才会重新计算。这是计算属性相比方法的主要优势——避免不必要的重复计算。</p>
    <p>计算属性默认是只读的，但你也可以提供 getter 和 setter 来创建可写的计算属性。在大多数情况下，只读的计算属性就足够了。</p>
    <p>使用计算属性的最佳场景包括：需要对数据进行复杂的转换或格式化；需要从多个响应式数据源派生出新的值；需要进行开销较大的计算并希望利用缓存机制等。</p>

    <h4>watch() 和 watchEffect() - 侦听器</h4>
    <p>watch() 用于监听一个或多个响应式数据源，当数据变化时执行回调函数。它可以访问到数据变化前后的值，并且支持配置选项如 immediate、deep 等。</p>
    <p>watchEffect() 是一个更加自动化的侦听器。它会自动追踪回调函数中使用的所有响应式依赖，并在依赖变化时重新执行。相比 watch()，watchEffect() 不需要明确指定要监听的数据源，更加简洁。</p>
    <p>选择使用 watch() 还是 watchEffect() 取决于具体场景。如果你需要访问变化前后的值，或者需要更精确地控制监听的数据源，使用 watch()。如果只是想在某些响应式数据变化时执行副作用操作，watchEffect() 更加方便。</p>

    <h3>响应式原理深入理解</h3>
    <p>Vue 3 的响应式系统基于依赖追踪和发布订阅模式。当你访问一个响应式对象的属性时，Vue 会追踪这个依赖关系。当属性被修改时，Vue 会通知所有依赖该属性的订阅者（如组件、计算属性、侦听器等），触发它们的更新。</p>
    <p>这个过程涉及三个核心概念：Proxy 代理对象负责拦截属性的读取和设置操作；track() 函数在属性被读取时收集依赖；trigger() 函数在属性被修改时触发更新。</p>
    <p>了解这些原理可以帮助你更好地理解 Vue 的行为，避免一些常见的陷阱，并写出更高效的代码。</p>

    <h3>最佳实践与注意事项</h3>
    <p>在使用 Vue 3 响应式系统时，有一些最佳实践值得遵循。首先，避免在响应式对象中存储不必要的数据。响应式系统会为每个属性添加额外的开销，因此只应该让真正需要响应式的数据变成响应式。</p>
    <p>其次，注意响应式对象的解构问题。当你解构一个 reactive 对象时，解构出来的值会失去响应式。如果确实需要解构，可以使用 toRefs() 或 toRef() 来保持响应式。</p>
    <p>另外，在处理大型列表或频繁更新的数据时，要注意性能优化。可以考虑使用 shallowReactive() 或 shallowRef() 来创建浅层响应式对象，只监听第一层属性的变化。对于不需要响应式的数据，可以使用 markRaw() 标记为非响应式。</p>
    <p>最后，合理使用计算属性和侦听器。不要在计算属性中进行有副作用的操作，也不要在侦听器中进行过于复杂的逻辑处理。保持代码的清晰和可维护性。</p>

    <h3>总结</h3>
    <p>Vue 3 的响应式系统是一个强大而灵活的工具，它为开发者提供了简洁的 API 来处理动态数据。通过深入理解其工作原理和掌握核心 API 的使用方法，你可以更好地利用 Vue 3 构建高性能、易维护的应用程序。</p>
    <p>无论你是从 Vue 2 迁移到 Vue 3，还是刚开始学习 Vue，掌握响应式系统都是至关重要的。希望本文能帮助你更好地理解和使用 Vue 3 的响应式特性。</p>
  `,
  votes: 42,
  commentCount: 8
})

const comments = ref([
  {
    id: 1,
    author: '用户A',
    time: '2小时前',
    content: '讲解得很清楚，特别是 reactive 和 ref 的区别部分！',
    votes: 12
  },
  {
    id: 2,
    author: '用户B',
    time: '5小时前',
    content: '能否再详细说明一下 computed 的缓存机制？',
    votes: 8
  },
  {
    id: 3,
    author: '用户C',
    time: '1天前',
    content: '非常实用的教程，已经在项目中应用了这些概念。',
    votes: 15
  }
])

const memoryDecks = ref([
  { id: 1, title: 'Vue 3 核心 API', cardCount: 12 },
  { id: 2, title: '响应式原理', cardCount: 8 },
  { id: 3, title: '常见问题', cardCount: 15 }
])
</script>

<style scoped>
.read-page {
  min-height: 100vh;
  background-color: #FFFFFF;
}

/* 固定顶部横条 */
.fixed-top-bar {
  position: fixed;
  top: 56px;
  left: 0;
  right: 0;
  height: 42px;
  background-color: white;
  border-top: 1px solid #E5E5E5;
  z-index: 999;
  transform: translateY(-100%);
  transition: transform 0.3s ease;
  margin-top: -1px;
}

.fixed-top-bar.show {
  transform: translateY(0);
}

.fixed-bar-content {
  height: 100%;
  padding: 0;
  max-width: 1470px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.fixed-bar-course-name {
  font-size: 0.8125rem;
  color: #666;
  line-height: 1;
}

.fixed-bar-subcourse-name {
  font-size: 0.8125rem;
  color: #666;
  line-height: 1;
}

.fixed-bar-path {
  font-size: 0.8125rem;
  color: #666;
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fixed-bar-stat {
  font-size: 0.75rem;
  color: #666;
  font-weight: 500;
  line-height: 1;
}

@media (max-width: 960px) {
  .fixed-top-bar {
    left: 0;
  }

  .fixed-bar-content {
    padding: 0 20px;
  }
}

.main-content {
  margin-left: max(160px, calc((100vw - 1550px) / 2));
  padding: 80px 40px 40px 40px;
  max-width: 1550px;
  width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
}

@media (min-width: 2229px) {
  .main-content {
    margin-left: max(160px, calc((100vw - 1550px) / 2));
    padding: 80px 40px 40px 40px;
    width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
    max-width: 1550px;
  }
}

.course-info-card {
  background-color: white;
  border: 1px solid #E5E5E5;
}

/* 子课程区域 */
.sub-courses-section {
  padding-top: 20px;
  border-top: 1px solid #E5E5E5;
}

.current-subcourse-info {
  padding: 14px 16px;
  background-color: #FAFAFA;
  border-radius: 12px;
  border: 1px solid #E5E5E5;
}

/* 子课程列表 */
.sub-course-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background-color: white;
  border: 1.5px solid #E5E5E5;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sub-course-chip:hover {
  border-color: rgb(var(--v-theme-primary));
  background-color: rgba(var(--v-theme-primary), 0.05);
  transform: translateY(-1px);
}

.sub-course-chip.sub-course-active {
  background-color: rgb(var(--v-theme-primary));
  border-color: rgb(var(--v-theme-primary));
}

.sub-course-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #1A1A1B;
}

.sub-course-active .sub-course-name {
  color: white;
}

.sub-course-progress {
  font-size: 0.6875rem;
  font-weight: 600;
  color: #666;
  padding: 2px 6px;
  background-color: #F5F5F5;
  border-radius: 6px;
}

.sub-course-active .sub-course-progress {
  color: white;
  background-color: rgba(255, 255, 255, 0.25);
}

/* Reddit风格三栏布局 */
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
  padding: 24px 12px 24px 0;
}

.toc-sticky-wrapper {
  position: sticky;
  top: 67px;
  max-height: calc(100vh - 82px);
  display: flex;
  flex-direction: column;
  transition: top 0.3s ease, max-height 0.3s ease;
}

/* 当固定横条显示时，调整目录位置 */
.read-page:has(.fixed-top-bar.show) .toc-sticky-wrapper {
  top: 109px;
  max-height: calc(100vh - 124px);
}

.toc-card {
  background-color: white;
  padding: 14px 0;
  border-radius: 16px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.toc-tree {
  margin-top: 6px;
  margin-right: 0;
  padding-right: 0;
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1;
  min-height: 0;
}

/* 自定义滚动条样式 - Webkit (Chrome, Safari, Edge) */
.toc-tree::-webkit-scrollbar {
  width: 1px;
  height: 1px;
}

.toc-tree::-webkit-scrollbar-track {
  background: transparent;
}

.toc-tree::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 0.5px;
}

.toc-tree::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

/* 目录组选择卡片 */
.toc-groups-card {
  background-color: white;
  padding: 10px 12px;
  border: 1px solid #E5E5E5;
  border-radius: 16px;
  flex-shrink: 0;
  margin-bottom: 4px;
}

.toc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #E5E5E5;
}

.toc-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.toc-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rotate-180 {
  transform: rotate(180deg);
  transition: transform 0.3s ease;
}

.toc-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 10px;
}

.toc-chip {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background-color: #F6F7F8;
  border: 1.5px solid #E5E5E5;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toc-chip:hover {
  border-color: rgb(var(--v-theme-primary));
  background-color: rgba(var(--v-theme-primary), 0.05);
}

.toc-chip.chip-active {
  background-color: rgb(var(--v-theme-primary));
  border-color: rgb(var(--v-theme-primary));
}

.toc-chip.chip-primary {
  border-color: rgb(var(--v-theme-primary));
  border-width: 2px;
}

.chip-inner {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chip-number {
  font-size: 0.875rem;
  font-weight: 600;
  color: #666;
}

.chip-active .chip-number {
  color: white;
}

.corner-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background-color: rgb(var(--v-theme-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
}

/* 中间+右侧容器包装 */
.center-right-container {
  display: flex;
  flex-direction: column;
  flex: 1;
}

/* 中间+右侧容器 - 居中 */
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

/* 节点路径 */
.node-path {
  padding: 8px 0;
}

/* 节点头部 */
.node-header {
  background-color: white;
  padding: 20px 0;
  border-radius: 12px;
}

/* Tab栏和操作按钮 */
.tabs-actions-bar {
  position: sticky;
  top: 90px;
  background-color: white;
  padding: 12px 0;
  border-radius: 0;
  margin-bottom: 16px;
  z-index: 998;
}

.tabs-actions-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.post-card,
.comments-card {
  position: relative;
}

/* 文章内容区域 */
.post-content {
  line-height: 1.8;
  color: #1A1A1B;
  font-size: 1rem;
}

/* 段落样式 */
.post-content p {
  margin-bottom: 1rem;
  line-height: 1.75;
}

/* 标题层级样式 - 使用 Vuetify spacing */
.post-content h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-top: 2rem;
  margin-bottom: 1rem;
  color: #1A1A1B;
  line-height: 1.3;
  letter-spacing: -0.02em;
}

.post-content h2 {
  font-size: 1.75rem;
  font-weight: 700;
  margin-top: 2.5rem;
  margin-bottom: 1rem;
  color: #1A1A1B;
  line-height: 1.3;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #E5E5E5;
  letter-spacing: -0.01em;
}

.post-content h3 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-top: 2rem;
  margin-bottom: 0.875rem;
  color: #1A1A1B;
  line-height: 1.4;
}

.post-content h4 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-top: 1.75rem;
  margin-bottom: 0.75rem;
  color: #1A1A1B;
  line-height: 1.4;
}

.post-content h5 {
  font-size: 1.1rem;
  font-weight: 600;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
  color: #1A1A1B;
  line-height: 1.4;
}

.post-content h6 {
  font-size: 1rem;
  font-weight: 600;
  margin-top: 1.25rem;
  margin-bottom: 0.625rem;
  color: #666;
  line-height: 1.4;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.875rem;
}

/* 第一个元素不需要上边距 */
.post-content > *:first-child {
  margin-top: 0 !important;
}

/* 列表样式 */
.post-content ul,
.post-content ol {
  margin-bottom: 1rem;
  padding-left: 2rem;
  line-height: 1.75;
}

.post-content ul {
  list-style-type: disc;
}

.post-content ol {
  list-style-type: decimal;
}

.post-content li {
  margin-bottom: 0.5rem;
  padding-left: 0.5rem;
}

.post-content ul ul,
.post-content ol ul {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  list-style-type: circle;
}

.post-content ul ol,
.post-content ol ol {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}

/* 链接样式 */
.post-content a {
  color: rgb(var(--v-theme-primary));
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all 0.2s ease;
  font-weight: 500;
}

.post-content a:hover {
  border-bottom-color: rgb(var(--v-theme-primary));
}

/* 代码块样式 */
.post-content code {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 0.875em;
  color: #E91E63;
  font-weight: 500;
}

.post-content pre {
  background-color: #F6F7F8;
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  margin-top: 1rem;
  margin-bottom: 1rem;
  border: 1px solid #E5E5E5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.post-content pre code {
  background-color: transparent;
  padding: 0;
  color: #1A1A1B;
  font-size: 0.875rem;
  font-weight: 400;
}

/* 引用块样式 */
.post-content blockquote {
  margin: 1.5rem 0;
  padding: 0.75rem 1.25rem;
  border-left: 4px solid rgb(var(--v-theme-primary));
  background-color: rgba(var(--v-theme-primary), 0.05);
  color: #666;
  font-style: italic;
  border-radius: 0 0.25rem 0.25rem 0;
}

.post-content blockquote p {
  margin-bottom: 0;
}

.post-content blockquote p:not(:last-child) {
  margin-bottom: 0.5rem;
}

/* 分隔线样式 */
.post-content hr {
  margin: 2rem 0;
  border: none;
  border-top: 1px solid #E5E5E5;
  opacity: 0.7;
}

/* 图片样式 */
.post-content img {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
  margin: 1.5rem 0;
  display: block;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 表格样式 */
.post-content table {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5rem 0;
  font-size: 0.9rem;
  border-radius: 0.5rem;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.post-content th,
.post-content td {
  padding: 0.75rem 1rem;
  text-align: left;
  border-bottom: 1px solid #E5E5E5;
}

.post-content th {
  background-color: #F6F7F8;
  font-weight: 600;
  color: #1A1A1B;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.post-content tr:last-child td {
  border-bottom: none;
}

.post-content tbody tr:hover {
  background-color: #FFFFFF;
  transition: background-color 0.2s ease;
}

/* 强调样式 */
.post-content strong {
  font-weight: 600;
  color: #1A1A1B;
}

.post-content em {
  font-style: italic;
  color: #666;
}

/* 删除线 */
.post-content del {
  text-decoration: line-through;
  color: #999;
  opacity: 0.7;
}

/* 标记文本 */
.post-content mark {
  background-color: rgba(255, 235, 59, 0.4);
  padding: 0.125rem 0.25rem;
  border-radius: 0.125rem;
}

/* 键盘按键样式 */
.post-content kbd {
  background-color: #F6F7F8;
  border: 1px solid #E5E5E5;
  border-radius: 0.25rem;
  padding: 0.125rem 0.375rem;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 0.875em;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.comment-item {
  background-color: #F6F7F8;
  border-radius: 24px;
}

/* 右侧边栏 - 紧贴中间内容 */
/* 右侧边栏 - 紧贴中间内容 */
.right-sidebar {
  flex: 0 1 360px;
  max-width: 360px;
  padding: 24px 0 24px 24px;
}

.sidebar-sticky {
  position: sticky;
  top: 67px;
  max-height: calc(100vh - 82px);
  transition: top 0.3s ease, max-height 0.3s ease;
}

/* 当固定横条显示时，调整右侧栏位置 */
.read-page:has(.fixed-top-bar.show) .sidebar-sticky {
  top: 109px;
  max-height: calc(100vh - 124px);
}

.sidebar-card {
  background-color: white;
  border: 1px solid #E5E5E5;
  position: relative;
}

.sidebar-card .v-card-title {
  font-size: 0.9375rem;
  font-weight: 600;
}

/* AI 引擎链接样式 */
.engine-link {
  text-decoration: none !important;
}

.engine-link:hover,
.engine-link:focus,
.engine-link:active {
  text-decoration: none !important;
}

.info-item {
  padding: 8px 0;
}

/* 按钮样式 */
.v-btn {
  border-radius: 24px;
}

/* 移动端 */
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    max-width: 100%;
    padding: 80px 20px 80px 20px;
  }

  .read-content {
    max-width: 100%;
  }

  .toc-sidebar {
    display: none;
  }

  .center-content {
    width: 100%;
    max-width: 100%;
    padding: 0;
  }

  .right-sidebar {
    display: none;
  }
}

</style>
