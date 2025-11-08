<template>
  <div class="read-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
      <!-- 固定顶部横条 - 滚动时显示 -->
      <div class="fixed-top-bar" :class="{ 'show': showFixedBar }">
        <div class="fixed-bar-content">
          <!-- 左侧：返回按钮 + 课程和子课程信息 + 完整节点路径 -->
          <div class="d-flex align-center flex-grow-1" style="gap: 12px;">
            <v-btn
              variant="text"
              color="grey-darken-2"
              size="small"
              @click="goBackToCourse"
            >
              <v-icon icon="mdi-arrow-left" size="18" class="mr-1"></v-icon>
              返回课程
            </v-btn>
            <div class="d-flex align-center" style="gap: 6px;">
              <span class="fixed-bar-course-name">{{ courseData.title }}</span>
              <v-icon icon="mdi-chevron-right" size="16" color="grey"></v-icon>
              <span class="fixed-bar-subcourse-name">{{ currentSubCourse.name }}</span>
              <v-icon icon="mdi-chevron-right" size="16" color="grey"></v-icon>
              <span class="fixed-bar-path">{{ pathText }}</span>
            </div>
          </div>

          <!-- 右侧：操作按钮 -->
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
          </div>
        </div>
      </div>

      <!-- 子课程信息卡片 -->
      <div class="subcourse-info-section">
        <div class="d-flex align-center justify-space-between">
          <!-- 左侧：返回按钮 + 课程路径 -->
          <div class="d-flex align-center course-breadcrumb">
            <v-btn
              icon="mdi-arrow-left"
              variant="flat"
              color="grey-lighten-4"
              size="small"
              class="mr-2"
              @click="goBackToCourse"
            ></v-btn>
            <v-chip size="small" density="comfortable" color="grey-darken-1" variant="tonal">课程</v-chip>
            <v-btn
              variant="text"
              class="course-link-btn px-1"
              @click="goBackToCourse"
            >
              {{ courseData.title }}
            </v-btn>
            <v-icon icon="mdi-chevron-right" size="18" color="grey-darken-1" class="mx-1"></v-icon>
            <v-chip size="small" density="comfortable" color="grey-darken-1" variant="tonal">子课程</v-chip>
            <v-btn
              variant="text"
              class="course-link-btn px-1"
              @click="goBackToCourse"
            >
              {{ currentSubCourse.name }}
            </v-btn>
            <span class="text-caption text-grey mx-2">·</span>
            <span class="text-caption text-grey">{{ currentSubCourse.totalNodes }} 个节点</span>
            <span class="text-caption text-grey mx-2">·</span>
            <span class="text-caption text-grey">1,234 人学习</span>
          </div>

          <!-- 右侧按钮 -->
          <div class="d-flex align-center flex-shrink-0" style="gap: 8px;">
            <v-btn
              :color="isLearning ? 'success' : 'primary'"
              :variant="isLearning ? 'tonal' : 'flat'"
              density="comfortable"
              rounded="pill"
              class="text-none px-4"
              elevation="0"
              @click="toggleLearning"
            >
              <v-icon size="16" class="mr-1">{{ isLearning ? 'mdi-check-circle' : 'mdi-play-circle' }}</v-icon>
              {{ isLearning ? '学习中' : '开始学习' }}
            </v-btn>
            <v-btn
              :icon="currentSubCourse.subscribed ? 'mdi-heart' : 'mdi-heart-outline'"
              :color="currentSubCourse.subscribed ? 'error' : 'grey-lighten-1'"
              :variant="currentSubCourse.subscribed ? 'flat' : 'text'"
              density="comfortable"
              rounded="circle"
              @click="toggleSubscribe"
            ></v-btn>
          </div>
        </div>
      </div>

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
              <div class="comments-section">
                <h3 class="section-title">评论 {{ currentPost.commentCount }}</h3>

                <!-- 评论输入 -->
                <div class="comment-input-section">
                  <v-textarea
                    v-model="newComment"
                    placeholder="写下你的评论..."
                    variant="outlined"
                    rows="3"
                    hide-details
                    class="mb-3"
                  ></v-textarea>
                  <v-btn
                    color="primary"
                    variant="tonal"
                    density="comfortable"
                    :disabled="!newComment.trim()"
                  >
                    <v-icon icon="mdi-send" size="18" class="mr-1"></v-icon>
                    发表评论
                  </v-btn>
                </div>

                <!-- 评论列表 -->
                <div class="comment-list">
                  <div v-for="comment in comments" :key="comment.id" class="comment-item mb-4">
                    <div class="d-flex">
                      <v-avatar size="36" color="grey-lighten-2" class="mr-3 mt-1">
                        <v-icon icon="mdi-account" color="grey" size="20"></v-icon>
                      </v-avatar>
                      <div class="flex-grow-1">
                        <div class="d-flex align-center justify-space-between mb-1">
                          <span class="text-body-2 font-weight-medium text-grey-darken-3">
                            {{ comment.author }}
                          </span>
                          <span class="text-caption text-grey">
                            {{ comment.time }}
                          </span>
                        </div>
                        <p class="text-body-2 text-grey-darken-2 mb-2">
                          {{ comment.content }}
                        </p>
                        <v-btn
                          size="x-small"
                          variant="text"
                          color="grey-darken-2"
                        >
                          <v-icon icon="mdi-thumb-up-outline" size="14" class="mr-1"></v-icon>
                          {{ comment.votes }}
                        </v-btn>
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

                <!-- 子课程信息 -->
                <v-card class="sidebar-card" border rounded="lg">
                  <v-card-title class="pa-4">
                    关于本子课程
                  </v-card-title>
                  <v-card-text class="pa-4 pt-0">
                    <div class="info-item">
                      <div class="text-caption text-medium-emphasis mb-2">课程描述</div>
                      <div class="text-body-2">{{ currentSubCourse.description }}</div>
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
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'
import TreeNode from '@/components/TreeNode.vue'

const router = useRouter()
const route = useRoute()

const isLearning = ref(true)
const newComment = ref('')
const activeNode = ref(null)
const activeTab = ref('list')
const isAssistantExpanded = ref(true)
const showFixedBar = ref(false)

// 滚动监听
const handleScroll = () => {
  showFixedBar.value = window.scrollY > 150
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

// 课程数据
const courseData = ref({
  title: 'Vue 3 完整教程'
})

// 子课程数据
const subCourses = [
  {
    name: 'Vue 3 基础',
    description: '学习 Vue 3 的核心概念、响应式系统、组件基础和模板语法',
    progress: 80,
    totalNodes: 12,
    completedNodes: 10,
    subscribed: false
  },
  {
    name: 'Vue 3 进阶',
    description: '深入学习组合式 API、自定义指令、插件开发和高级组件模式',
    progress: 60,
    totalNodes: 15,
    completedNodes: 9,
    subscribed: false
  },
  {
    name: 'Vue 3 实战',
    description: '通过完整项目实战掌握 Vue 3 生态系统，包括路由、状态管理和构建部署',
    progress: 30,
    totalNodes: 20,
    completedNodes: 6,
    subscribed: false
  }
]

// 当前子课程 - 使用 ref 让它可以响应式更新
const currentSubCourse = computed(() => {
  const subCourseId = parseInt(route.params.subCourseId as string) || 0
  return subCourses[subCourseId] || subCourses[0]
})

// AI 引擎列表
const aiEngines = [
  { name: 'ChatGPT', href: 'https://chatgpt.com', color: 'green-darken-2', icon: 'mdi-robot' },
  { name: 'Claude', href: 'https://claude.ai', color: 'indigo-darken-2', icon: 'mdi-alpha-c-circle-outline' },
  { name: 'Gemini', href: 'https://gemini.google.com', color: 'blue-darken-2', icon: 'mdi-google' },
  { name: 'DeepSeek', href: 'https://chat.deepseek.com', color: 'red-darken-4', icon: 'mdi-radar' }
]

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

const toggleSubscribe = () => {
  const subCourseId = parseInt(route.params.subCourseId as string) || 0
  subCourses[subCourseId].subscribed = !subCourses[subCourseId].subscribed
}

const toggleNodeCompletion = () => {
  currentNode.value.isCompleted = !currentNode.value.isCompleted
}

const goBackToCourse = () => {
  const courseId = route.params.courseId || '1'
  router.push({
    name: 'course-detail',
    params: { courseId }
  })
}

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
          { name: '1.3.3 条件渲染', completed: false },
          { name: '1.3.4 列表渲染', completed: false }
        ]
      },
      { name: '1.4 开发工具', completed: false },
      { name: '1.5 项目结构', completed: false }
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
          { name: '2.1.3 toRef() 和 toRefs()', completed: false },
          { name: '2.1.4 shallowReactive()', completed: false }
        ]
      },
      {
        name: '2.2 计算属性和侦听器',
        completed: false,
        children: [
          { name: '2.2.1 computed()', completed: false },
          { name: '2.2.2 watch()', completed: false },
          { name: '2.2.3 watchEffect()', completed: false },
          { name: '2.2.4 watchPostEffect()', completed: false }
        ]
      },
      { name: '2.3 响应式原理', completed: false },
      { name: '2.4 响应式工具函数', completed: false }
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
          { name: '3.2.3 onUnmounted', completed: false },
          { name: '3.2.4 onBeforeMount', completed: false },
          { name: '3.2.5 onBeforeUpdate', completed: false }
        ]
      },
      { name: '3.3 依赖注入', completed: false },
      { name: '3.4 组合式函数', completed: false },
      { name: '3.5 script setup 语法', completed: false }
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
      },
      { name: '4.6 动态组件', completed: false },
      { name: '4.7 异步组件', completed: false }
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
      { name: '5.5 路由懒加载', completed: false },
      { name: '5.6 路由元信息', completed: false },
      { name: '5.7 命名路由和命名视图', completed: false }
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
      { name: '6.5 Actions', completed: false },
      { name: '6.6 插件', completed: false },
      { name: '6.7 组合式 Store', completed: false }
    ]
  },
  {
    name: '7. 高级特性',
    completed: false,
    expanded: false,
    children: [
      { name: '7.1 自定义指令', completed: false },
      { name: '7.2 Teleport', completed: false },
      { name: '7.3 Suspense', completed: false },
      { name: '7.4 性能优化', completed: false },
      { name: '7.5 Transition', completed: false },
      { name: '7.6 KeepAlive', completed: false }
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
      { name: '8.5 项目部署', completed: false },
      { name: '8.6 性能监控', completed: false },
      { name: '8.7 错误处理', completed: false }
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
    <p>了解这些原理可以帮助你更好地理解 Vue 的行为，避免一些常见的陷阱，并写出更高效的代码。例如，理解为什么解构会丢失响应式、为什么在某些情况下需要使用 nextTick 等。</p>

    <h3>最佳实践与注意事项</h3>
    <p>在使用 Vue 3 响应式系统时，有一些最佳实践值得遵循。首先，避免在响应式对象中存储不必要的数据。响应式系统会为每个属性添加额外的开销，因此只应该让真正需要响应式的数据变成响应式。</p>
    <p>其次，注意响应式对象的解构问题。当你解构一个 reactive 对象时，解构出来的值会失去响应式。如果确实需要解构，可以使用 toRefs() 或 toRef() 来保持响应式。</p>
    <p>另外，在处理大型列表或频繁更新的数据时，要注意性能优化。可以考虑使用 shallowReactive() 或 shallowRef() 来创建浅层响应式对象，只监听第一层属性的变化。对于不需要响应式的数据，可以使用 markRaw() 标记为非响应式。</p>
    <p>最后，合理使用计算属性和侦听器。不要在计算属性中进行有副作用的操作，也不要在侦听器中进行过于复杂的逻辑处理。保持代码的清晰和可维护性。</p>

    <h3>常见问题和解决方案</h3>
    <p>在使用 Vue 3 响应式系统时，开发者经常会遇到一些问题。例如，为什么修改了数据但视图没有更新？这通常是因为直接修改了非响应式的数据，或者在响应式对象外部添加了新属性。</p>
    <p>另一个常见问题是响应式数据在异步操作后丢失。这可能是因为在异步回调中错误地使用了 this，或者没有正确处理响应式引用。解决方法是使用箭头函数保持 this 指向，或者提前保存响应式引用。</p>

    <h3>总结</h3>
    <p>Vue 3 的响应式系统是一个强大而灵活的工具，它为开发者提供了简洁的 API 来处理动态数据。通过深入理解其工作原理和掌握核心 API 的使用方法，你可以更好地利用 Vue 3 构建高性能、易维护的应用程序。</p>
    <p>无论你是从 Vue 2 迁移到 Vue 3，还是刚开始学习 Vue，掌握响应式系统都是至关重要的。希望本文能帮助你更好地理解和使用 Vue 3 的响应式特性。</p>
    <p>在实际项目中，建议多实践、多总结，逐步建立起对响应式系统的直觉认知。随着经验的积累，你会发现 Vue 3 的响应式系统设计得非常优雅和实用。</p>
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
  position: sticky;
  top: 56px;
  left: 0;
  right: 0;
  height: 42px;
  background-color: white;
  z-index: 998;
  transform: translateY(calc(-100% - 80px));
  margin-top: -1px;
  margin-bottom: -42px;
  margin-left: -40px;
  margin-right: -40px;
  padding: 0 40px;
}

.fixed-top-bar.show {
  transform: translateY(0);
}

.fixed-bar-content {
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.fixed-bar-course-name,
.fixed-bar-subcourse-name,
.fixed-bar-path {
  font-size: 0.8125rem;
  color: #666;
  line-height: 1;
}

.fixed-bar-stat {
  font-size: 0.75rem;
  color: #666;
  font-weight: 500;
  line-height: 1;
}

.main-content {
  margin-left: max(160px, calc((100vw - 1550px) / 2));
  padding: 80px 40px 40px 40px;
  max-width: 1550px;
  width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
}

/* 子课程信息区域 */
.subcourse-info-section {
  padding: 0;
}

.course-breadcrumb {
  display: flex;
  align-items: center;
}

.course-link-btn {
  font-size: 16px;
  font-weight: 600;
  color: #666;
  text-transform: none;
  letter-spacing: normal;
  height: auto;
  min-width: auto;
}

.course-link-btn:hover {
  color: rgb(var(--v-theme-primary));
}

.course-main-name {
  font-size: 15px;
  font-weight: 600;
  color: #666;
}

.course-sub-name {
  font-size: 15px;
  font-weight: 600;
  color: #666;
}

.subcourse-description {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0;
  max-width: 800px;
}

.subcourse-info-card {
  background-color: white;
  border: 1px solid #E5E5E5;
}

.progress-section {
  padding-top: 20px;
  border-top: 1px solid #E5E5E5;
  margin-top: 20px;
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

/* 目录组选择卡片 */
.toc-groups-card {
  background-color: white;
  padding: 10px 12px;
  border: 1px solid #E5E5E5;
  border-radius: 16px;
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
  margin-bottom: 16px;
  z-index: 998;
}

.tabs-actions-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 文章内容区域 */
.post-content {
  line-height: 1.8;
  color: #1A1A1B;
  font-size: 1rem;
}

.post-content p {
  margin-bottom: 1rem;
  line-height: 1.75;
}

.post-content h3 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-top: 2rem;
  margin-bottom: 0.875rem;
  color: #1A1A1B;
}

.post-content h4 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-top: 1.75rem;
  margin-bottom: 0.75rem;
  color: #1A1A1B;
}

/* 评论区 */
.comments-section {
  margin-top: 40px;
  padding-top: 40px;
  border-top: 1px solid #E5E5E5;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #1A1A1B;
  margin-bottom: 24px;
}

.comment-input-section {
  margin-bottom: 32px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  padding-top: 10px;
}

.comment-item {
  padding-bottom: 16px;
  border-bottom: 1px solid #F0F0F0;
}

.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.comment-item .v-avatar {
  padding-top: 0;
}

/* 右侧边栏 */
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

.read-page:has(.fixed-top-bar.show) .sidebar-sticky {
  top: 109px;
  max-height: calc(100vh - 124px);
}

.sidebar-card {
  background-color: white;
  border: 1px solid #E5E5E5;
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
