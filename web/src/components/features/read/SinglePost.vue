<script lang="ts">
// 全局 Mermaid ID 计数器（模块级别，所有组件实例共享）
// 避免不同 SinglePost 实例生成重复的 Mermaid ID
let globalMermaidIdCounter = 0
</script>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import renderMathInElement from 'katex/contrib/auto-render'
import 'katex/dist/katex.min.css'
import mermaid from 'mermaid'
import { upvoteApi, postApi, bookmarkApi } from '@/api'
import { useMutation } from '@/composables'
import { ObjectType, VoteType } from '@/enums'
import type { UpvoteStatusResponse } from '@/types/upvote'
import UserAvatar from '@/components/common/UserAvatar.vue'
import ImageViewer from '@/components/common/ImageViewer.vue'
import ChartViewer from '@/components/common/ChartViewer.vue'

interface NodeInfo {
  id: number
  name: string
  description: string
}

interface Props {
  posting: any
  currNode?: any
  data?: any
  detail?: boolean
  isLearning?: boolean
  showBackButton?: boolean
}

interface Emits {
  (e: 'switch-tab', tab: string, posting?: any): void
  (e: 'load-data', parts: string[]): void
  (e: 'mark-node-completed'): void
}

const props = withDefaults(defineProps<Props>(), {
  detail: false,
  isLearning: false,
  showBackButton: true,
})

const emit = defineEmits<Emits>()
const router = useRouter()
const route = useRoute()

const isOverflow = ref(false)
const contentRef = ref<HTMLElement | null>(null)
let mermaidInitialized = false

// 图片查看器
const imageViewerVisible = ref(false)
const viewerImageSrc = ref('')

// 图表查看器
const chartViewerVisible = ref(false)
const viewerChartSvg = ref('')

// 处理内容点击事件（图片点击放大）
const handleContentClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement

  // 图片点击放大
  if (target.tagName === 'IMG') {
    event.preventDefault()
    event.stopPropagation()
    const img = target as HTMLImageElement
    viewerImageSrc.value = img.src
    imageViewerVisible.value = true
    return
  }

  // Mermaid 图表点击放大
  const mermaidContainer = target.closest('.mermaid[data-processed="true"]')
  if (mermaidContainer) {
    event.preventDefault()
    event.stopPropagation()

    const svg = mermaidContainer.querySelector('svg')
    if (svg) {
      viewerChartSvg.value = svg.outerHTML
      chartViewerVisible.value = true
    }
  }
}

// PostType 枚举
const PostType = {
  INDEX: 1,
  ARTICLE: 2,
}

// 解析目录内容
const contentNodes = computed<NodeInfo[]>(() => {
  if (props.posting.type !== PostType.INDEX) {
    return []
  }

  try {
    const parsed = JSON.parse(props.posting.content)
    if (Array.isArray(parsed)) {
      return parsed
    }
    return []
  } catch (e) {
    // 向后兼容：如果解析失败，尝试按逗号分割
    return props.posting.content.split(',').map((item: string, index: number) => ({
      id: index,
      name: item.trim(),
      description: '',
    }))
  }
})

// 初始化 Mermaid
const ensureMermaidInitialized = () => {
  if (!mermaidInitialized) {
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'loose',
      theme: 'neutral',
      look: 'handDrawn',
      htmlLabels: false,
      fontFamily: 'Arial, sans-serif',
      flowchart: { htmlLabels: true, useMaxWidth: true },
      sequence: { noteFontFamily: 'Arial, sans-serif' },
      suppressErrorRendering: true,
    })
    mermaidInitialized = true
  }
}

// 规范化 Mermaid 定义
const normalizeMermaidDefinition = (raw: string) => {
  let normalized = raw
    .replace(/\r\n?/g, '\n')
    .replace(/\u00a0/g, ' ')
    .trim()

  const firstLineMatch = /^(graph\s+[^\s]+)(.*)$/i.exec(normalized)
  if (firstLineMatch) {
    const graphLine = firstLineMatch[1]
    const rest = firstLineMatch[2]?.trimStart() ?? ''
    normalized = rest ? `${graphLine}\n${rest}` : graphLine
  }

  normalized = normalized
    .split('\n')
    .map((line) => line.trim())
    .join('\n')
  const lines = normalized
    .split(';')
    .map((segment) => segment.trim())
    .filter(Boolean)
  normalized = lines.join('\n')
  normalized = normalized.replace(/\n{2,}/g, '\n')

  const wrapLabelText = (value: string, open: string, close: string) =>
    value.replace(new RegExp(`\\${open}([^\\${close}]*)\\${close}`, 'g'), (match, inner) => {
      const trimmed = inner.trim()
      if (!trimmed) {
        return `${open}${close}`
      }
      if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
        return `${open}${trimmed}${close}`
      }
      const escaped = trimmed.replace(/"/g, '\\"')
      return `${open}"${escaped}"${close}`
    })

  normalized = wrapLabelText(normalized, '[', ']')
  normalized = wrapLabelText(normalized, '{', '}')

  return normalized
}

// 将代码块转换为 Mermaid 容器
const convertToMermaidContainers = (root: HTMLElement) => {
  // 查找 <pre><code class="language-mermaid"> 格式的代码块
  const mermaidCodeBlocks = root.querySelectorAll('pre code.language-mermaid')
  mermaidCodeBlocks.forEach((block) => {
    const parentPre = block.parentElement
    const definition = block.textContent || ''
    const container = document.createElement('div')
    container.className = 'mermaid'
    container.textContent = normalizeMermaidDefinition(definition)
    if (parentPre) {
      parentPre.replaceWith(container)
    } else {
      block.replaceWith(container)
    }
  })

  // 查找包含 mermaid 定义的段落
  const blockCandidates = root.querySelectorAll('p, div')
  blockCandidates.forEach((node) => {
    const element = node as HTMLElement
    if (element.classList.contains('mermaid') || element.dataset.processed) {
      return
    }
    if (element.childNodes.length !== 1 || element.firstChild?.nodeType !== 3) {
      return
    }
    const text = element.textContent?.trim()
    if (!text) return
    if (
      !/^(graph|sequenceDiagram|classDiagram|stateDiagram|erDiagram|journey|gantt|timeline|pie)/i.test(
        text
      )
    ) {
      return
    }
    const container = document.createElement('div')
    container.className = 'mermaid'
    container.textContent = normalizeMermaidDefinition(text)
    element.replaceWith(container)
  })
}

// 渲染 Mermaid 图表
const renderMermaidDiagrams = async (root: HTMLElement) => {
  ensureMermaidInitialized()
  convertToMermaidContainers(root)

  const mermaidElements = Array.from(root.querySelectorAll<HTMLElement>('.mermaid'))
  await Promise.all(
    mermaidElements.map(async (el) => {
      if (el.dataset.processed === 'true') {
        return
      }
      const definition = el.textContent?.trim()
      if (!definition) {
        return
      }
      const normalized = normalizeMermaidDefinition(definition)
      try {
        const { svg } = await mermaid.render(`mermaid-${globalMermaidIdCounter++}`, normalized)
        el.innerHTML = svg
        el.dataset.processed = 'true'
      } catch (error) {
        console.error('Mermaid render failed:', error, definition)
        el.dataset.processed = 'error'
        const escapeHtml = (str: string) =>
          str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        el.innerHTML = `<pre class="mermaid-error">${escapeHtml(normalized)}</pre>`
      }
    })
  )
}

// 渲染数学公式
const renderMath = () => {
  if (!contentRef.value) return

  renderMathInElement(contentRef.value, {
    delimiters: [
      { left: '$$', right: '$$', display: true },
      { left: '\\[', right: '\\]', display: true },
      { left: '\\(', right: '\\)', display: false },
      { left: '$', right: '$', display: false },
    ],
    throwOnError: false,
  })
}

// 应用所有增强功能
const applyEnhancements = async () => {
  await nextTick()
  if (!contentRef.value) return

  renderMath()
  await renderMermaidDiagrams(contentRef.value)
  updateOverflowState()
}

// 检查内容是否溢出
const updateOverflowState = () => {
  const el = contentRef.value
  if (!el || props.detail) {
    isOverflow.value = false
    return
  }
  isOverflow.value = el.scrollHeight > el.clientHeight + 1
}

// 查看完整内容
const handleViewFullContent = () => {
  emit('switch-tab', 'two', props.posting)
}

// 使用 useMutation 处理投票
const { execute: executeUpvote } = useMutation(
  (voteType: VoteType) => upvoteApi.upvote(props.posting.id, ObjectType.POST, voteType),
  {
    showToast: false,
    onSuccess: (response: UpvoteStatusResponse) => {
      // 更新帖子的点赞统计数据
      props.posting.twiceCount = response.twiceCount
      props.posting.likeCount = response.likeCount

      // 根据点赞状态设置 voteType
      if (response.twiced) {
        props.posting.voteType = 'twice'
      } else if (response.liked) {
        props.posting.voteType = 'helpful'
      } else {
        props.posting.voteType = null
      }
    },
  }
)

// 点赞
const handleUpvote = async (type: string) => {
  const voteType = type === 'twice' ? VoteType.TWICE : VoteType.LIKE
  await executeUpvote(voteType)
}

// 切换收藏状态
const { execute: executeToggleBookmark, loading: bookmarking } = useMutation(
  () => bookmarkApi.toggle('post', props.posting.id),
  {
    successMessage: '',
    showToast: false,
  }
)

const handleToggleBookmark = async () => {
  const result = await executeToggleBookmark()
  if (result !== null) {
    props.posting.bookmarked = result
  }
}

// 使用 useMutation 修改内容操作
const { execute: executeModifyContents } = useMutation(
  (data: { postingId: number; action: number }) =>
    postApi.operateContent({
      path: props.data?.path || '',
      nodeId: props.data?.rootNodeId || 0,
      postingId: data.postingId,
      action: data.action,
    }),
  {
    successMessage: '操作成功',
    onSuccess: (_, data) => {
      if (data.action === 1 || data.action === 2) {
        emit('load-data', ['contents', 'chosenPosting'])
      }
    },
  }
)

// 修改内容操作（设置/取消目录、置顶/取消置顶）
const handleModifyContents = async (postingId: number, action: number) => {
  await executeModifyContents({ postingId, action })
}

// 查看评论
const handleViewComments = () => {
  if (props.detail) {
    // 详情页：滚动到评论区
    nextTick(() => {
      setTimeout(() => {
        const commentSection = document.querySelector('.comments-section')
        if (commentSection) {
          commentSection.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
      }, 100)
    })
  } else {
    // 列表页：跳转到详情页
    router.push({
      path: '/read',
      query: { postId: String(props.posting.id) },
    })
  }
}

// 点击文章内容区域，跳转到详情页
const handleClickContent = () => {
  if (!props.detail && props.posting.id) {
    router.push({
      path: '/read',
      query: { postId: String(props.posting.id) },
    })
  }
}

onMounted(() => {
  applyEnhancements()
})

// 监听 content 变化，重新渲染
watch(
  () => props.posting.content,
  () => {
    applyEnhancements()
  }
)
</script>

<template>
  <div class="single-post">
    <!-- 作者信息 -->
    <v-row class="mx-0 my-2 py-1 d-flex align-center" :class="[detail ? 'sticky-top' : '']">
      <v-btn
        v-if="detail && showBackButton"
        variant="flat"
        class="me-2"
        color="grey-lighten-4"
        icon="mdi-arrow-left"
        size="small"
        @click="emit('switch-tab', 'list')"
      ></v-btn>
      <UserAvatar
        :name="posting.creator?.name || '匿名用户'"
        :avatar-url="posting.creator?.avatar"
        size="24"
        rounded="circle"
      />
      <div class="pl-3 d-flex align-center">
        <span class="text-body-2 font-weight-medium text-grey-darken-3">
          {{ posting.creator?.name || '匿名用户' }}
        </span>
        <span class="text-caption text-grey mx-2">·</span>
        <span class="text-body-2 text-grey-darken-2">
          {{ posting.createdAt || '2024-01-15' }}
        </span>
      </div>
    </v-row>

    <!-- 文章内容 -->
    <v-row class="ma-0 pa-0 pt-2">
      <!-- 目录型 Post -->
      <template v-if="posting.type === PostType.INDEX">
        <div class="w-100 d-flex justify-space-between align-end">
          <v-list class="w-100">
            <v-list-item
              v-for="(nodeInfo, index) in contentNodes"
              :key="index"
              class="px-0 py-4 dashed-border"
            >
              <router-link
                :to="{
                  path: '/read',
                  query: { nodeId: nodeInfo.id },
                }"
                target="_blank"
                class="text-decoration-none d-block w-100"
              >
                <v-list-item-title class="pb-1 text-grey-darken-3">
                  {{ index + 1 }}. {{ nodeInfo.name }}
                </v-list-item-title>
                <v-list-item-subtitle
                  v-if="nodeInfo.description"
                  class="text-body-2 text-grey-darken-1 mt-1"
                >
                  {{ nodeInfo.description }}
                </v-list-item-subtitle>
              </router-link>
            </v-list-item>
          </v-list>
        </div>
      </template>

      <!-- 文章型 Post -->
      <template v-else>
        <!-- 列表模式：使用 router-link 包裹，浏览器自动保存滚动位置 -->
        <router-link
          v-if="!detail && posting.id"
          :to="{ path: '/read', query: { ...(route.query.courseId ? { courseId: route.query.courseId } : {}), ...(route.query.nodeId ? { nodeId: route.query.nodeId } : {}), ...(route.query.path ? { path: route.query.path } : {}), postId: String(posting.id) } }"
          class="text-decoration-none d-block"
        >
          <div ref="contentRef" class="text-limited clickable-content cursor-pointer w-100">
            <div class="article-content">
              <div v-html="posting.content"></div>
            </div>

            <!-- 溢出提示（替代原来的"查看完整内容"按钮） -->
            <div v-if="isOverflow" class="overflow-hint">
              <v-icon icon="mdi-chevron-down" size="16" class="mr-1"></v-icon>
              点击查看完整内容
            </div>
          </div>
        </router-link>

        <!-- 详情模式：普通div -->
        <div v-else ref="contentRef" class="w-100" @click="handleContentClick">
          <div class="article-content full-article">
            <div v-html="posting.content"></div>
          </div>
        </div>
      </template>
    </v-row>

    <!-- 交互按钮 -->
    <v-row
      class="ma-0 pt-3 d-flex justify-space-between action-bar"
      :class="{ 'action-bar-sticky': detail }"
      align="center"
    >
      <!-- 目录型 Post：只显示"赞同"按钮 -->
      <template v-if="posting.type === PostType.INDEX">
        <v-btn
          :variant="posting.voteType === 'helpful' ? 'flat' : 'tonal'"
          rounded="pill"
          size="default"
          :color="posting.voteType === 'helpful' ? 'primary' : 'grey-lighten-2'"
          :class="['px-4', posting.voteType === 'helpful' ? 'core-btn-active' : 'core-btn-inactive']"
          @click="handleUpvote('helpful')"
        >
          <v-icon
            :icon="posting.voteType === 'helpful' ? 'mdi-check' : 'mdi-thumb-up-outline'"
            size="18"
            class="mr-2"
            :color="posting.voteType === 'helpful' ? 'white' : 'grey-darken-2'"
          ></v-icon>
          <span
            :class="
              posting.voteType === 'helpful'
                ? 'font-weight-bold text-white'
                : 'font-weight-medium text-grey-darken-2'
            "
          >
            赞同
          </span>
          <v-chip
            v-if="posting.likeCount > 0"
            size="small"
            color="white"
            class="ml-2 font-weight-bold"
          >
            {{ posting.likeCount }}
          </v-chip>
        </v-btn>
      </template>

      <!-- 文章型 Post：显示"两遍秒懂"和"有用"按钮 -->
      <template v-else>
        <div class="d-flex core-actions">
          <!-- 二次理解按钮 -->
          <v-btn
            :variant="posting.voteType === 'twice' ? 'flat' : 'tonal'"
            rounded="pill"
            size="default"
            :color="posting.voteType === 'twice' ? 'primary' : 'grey-lighten-2'"
            :class="['px-4', posting.voteType === 'twice' ? 'core-btn-active' : 'core-btn-inactive']"
            @click="handleUpvote('twice')"
          >
            <v-icon
              :icon="posting.voteType === 'twice' ? 'mdi-check' : 'mdi-lightbulb-outline'"
              size="18"
              class="mr-2"
              :color="posting.voteType === 'twice' ? 'white' : 'grey-darken-2'"
            ></v-icon>
            <span
              :class="
                posting.voteType === 'twice'
                  ? 'font-weight-bold text-white'
                  : 'font-weight-medium text-grey-darken-2'
              "
            >
              两遍秒懂
            </span>
            <span
              v-if="posting.twiceCount > 0"
              :class="[
                'vote-count ml-2',
                posting.voteType === 'twice'
                  ? 'text-white'
                  : 'text-grey-darken-1'
              ]"
            >
              {{ posting.twiceCount }}
            </span>
          </v-btn>

          <!-- 有用按钮 -->
          <v-btn
            :variant="posting.voteType === 'helpful' ? 'flat' : 'tonal'"
            rounded="pill"
            size="default"
            :color="posting.voteType === 'helpful' ? 'primary' : 'grey-lighten-2'"
            :class="[
              'px-4 ms-3',
              posting.voteType === 'helpful' ? 'core-btn-active' : 'core-btn-inactive',
            ]"
            @click="handleUpvote('helpful')"
          >
            <v-icon
              :icon="posting.voteType === 'helpful' ? 'mdi-check' : 'mdi-thumb-up-outline'"
              size="18"
              class="mr-2"
              :color="posting.voteType === 'helpful' ? 'white' : 'grey-darken-2'"
            ></v-icon>
            <span
              :class="
                posting.voteType === 'helpful'
                  ? 'font-weight-bold text-white'
                  : 'font-weight-medium text-grey-darken-2'
              "
            >
              有用
            </span>
            <span
              v-if="posting.likeCount > 0"
              :class="[
                'vote-count ml-2',
                posting.voteType === 'helpful'
                  ? 'text-white'
                  : 'text-grey-darken-1'
              ]"
            >
              {{ posting.likeCount }}
            </span>
          </v-btn>
        </div>
      </template>

      <v-spacer></v-spacer>

      <!-- 目录型 Post 管理按钮：设置/取消设置为目录 -->
      <template v-if="posting.type === PostType.INDEX">
        <v-btn
          variant="text"
          rounded="lg"
          :color="
            currNode && currNode['+'] && currNode['+'] === posting.id
              ? 'red-lighten-4'
              : 'grey-lighten-3'
          "
          size="default"
          class="px-3 ms-2"
          @click="
            handleModifyContents(
              posting.id,
              currNode && currNode['+'] && currNode['+'] === posting.id ? 2 : 1
            )
          "
        >
          <v-icon
            :icon="
              currNode && currNode['+'] && currNode['+'] === posting.id
                ? 'mdi-playlist-remove'
                : 'mdi-playlist-plus'
            "
            size="18"
            class="mr-2"
            :color="
              currNode && currNode['+'] && currNode['+'] === posting.id
                ? 'red-darken-2'
                : 'grey-darken-2'
            "
          ></v-icon>
          <span
            :class="
              currNode && currNode['+'] && currNode['+'] === posting.id
                ? 'font-weight-medium text-red-darken-2'
                : 'font-weight-medium text-grey-darken-2'
            "
          >
            {{
              currNode && currNode['+'] && currNode['+'] === posting.id
                ? '取消设置为目录'
                : '设置为目录'
            }}
          </span>
        </v-btn>
      </template>

      <!-- 评论按钮（列表模式） -->
      <v-btn
        v-if="!detail"
        variant="text"
        rounded="lg"
        size="default"
        class="px-3 ms-2"
        @click="handleViewComments"
      >
        <v-icon icon="mdi-comment-outline" size="18" class="mr-2" color="grey-darken-2"></v-icon>
        <span class="font-weight-medium text-grey-darken-2">
          {{ posting.commentCount || 0 }}
        </span>
      </v-btn>

      <!-- 评论按钮（详情模式） - 不可点击 -->
      <v-btn
        v-if="detail"
        variant="text"
        rounded="lg"
        size="default"
        class="px-3 ms-2"
        :ripple="false"
        @click.stop.prevent="null"
      >
        <v-icon icon="mdi-comment-outline" size="18" class="mr-2" color="grey-darken-2"></v-icon>
        <span class="font-weight-medium text-grey-darken-2">
          {{ posting.commentCount || 0 }}
        </span>
      </v-btn>

      <!-- 记忆卡片数量按钮 -->
      <v-tooltip location="top">
        <template v-slot:activator="{ props: tooltipProps }">
          <v-btn
            v-bind="tooltipProps"
            variant="text"
            color="grey-lighten-3"
            rounded="lg"
            size="default"
            class="px-3 ms-2"
          >
            <v-icon icon="mdi-cards-outline" size="18" class="mr-2" color="purple-darken-2"></v-icon>
            <span class="font-weight-medium text-purple-darken-2">
              {{ posting.deckCount || 0 }}
            </span>
          </v-btn>
        </template>
        <span>查看详情页以浏览和添加记忆卡片组</span>
      </v-tooltip>

      <!-- 收藏按钮 -->
      <v-tooltip location="top">
        <template #activator="{ props: tooltipProps }">
          <v-btn
            v-bind="tooltipProps"
            :icon="posting.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
            :color="posting.bookmarked ? 'primary' : 'grey-darken-1'"
            variant="text"
            size="small"
            rounded="lg"
            class="ms-2"
            @click="handleToggleBookmark"
          />
        </template>
        {{ posting.bookmarked ? '取消收藏' : '收藏文章' }}
      </v-tooltip>
    </v-row>

    <!-- 图片查看器 -->
    <ImageViewer v-model="imageViewerVisible" :src="viewerImageSrc" />

    <!-- 图表查看器 -->
    <ChartViewer v-model="chartViewerVisible" :svg-content="viewerChartSvg" />
  </div>
</template>

<style scoped>
.single-post {
  width: 100%;
}

.sticky-top {
  position: sticky;
  top: 102px;
  background-color: white;
  z-index: 10;
  padding-bottom: 12px;
}

/* 虚线边框 */
.dashed-border {
  border-bottom: 1px dashed rgba(0, 0, 0, 0.12);
}

.dashed-border:last-child {
  border-bottom: none;
}

.text-limited {
  position: relative;
  max-height: 800px;
  overflow: hidden;
}

/* 可点击内容区域 */
.clickable-content {
  transition: background-color 0.2s ease;
  padding: 12px;
  margin: -12px;
  border-radius: 8px;
}

.clickable-content:hover {
  background-color: #fafafa;
}

.cursor-pointer {
  cursor: pointer;
}

/* 溢出提示 */
.overflow-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 0;
  color: rgb(var(--v-theme-primary));
  font-size: 14px;
  font-weight: 500;
  margin-top: 8px;
}

.text-limited::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(to bottom, transparent 0%, white 100%);
}

.article-content {
  line-height: 1.8;
  color: #1a1a1b;
  font-size: 1rem;
}

.article-content :deep(p) {
  margin-bottom: 1.2em;
  line-height: 1.8;
}

.article-content :deep(h1),
.article-content :deep(h2) {
  font-size: 1.75rem;
  font-weight: 600;
  margin-top: 2rem;
  margin-bottom: 1rem;
  color: #1a1a1b;
}

.article-content :deep(h3) {
  font-size: 1.5rem;
  font-weight: 600;
  margin-top: 2rem;
  margin-bottom: 0.875rem;
  color: #1a1a1b;
}

.article-content :deep(h4) {
  font-size: 1.25rem;
  font-weight: 600;
  margin-top: 1.75rem;
  margin-bottom: 0.75rem;
  color: #1a1a1b;
}

.article-content :deep(ul),
.article-content :deep(ol) {
  margin: 1.5rem 0;
  padding-left: 2rem;
  line-height: 1.8;
}

.article-content :deep(li) {
  margin-bottom: 0.5rem;
}

.article-content :deep(code) {
  background-color: #f6f6f6;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.article-content :deep(pre) {
  background-color: transparent;
  padding: 1rem;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1.5rem 0;
  max-width: 680px;
}

.article-content :deep(blockquote) {
  border-left: 4px solid #e0e0e0;
  padding-left: 1rem;
  margin: 1.5rem 0;
  color: #666;
  font-style: italic;
}

.article-content :deep(img) {
  max-width: 100%;
  height: auto;
  margin: 1.5rem 0;
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.article-content :deep(img:hover) {
  opacity: 0.8;
}

.article-content :deep(a) {
  color: rgb(var(--v-theme-primary));
  text-decoration: none;
}

.article-content :deep(a:hover) {
  text-decoration: underline;
}

/* 表格样式 */
.article-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5rem 0;
  font-size: 0.95rem;
}

.article-content :deep(table th),
.article-content :deep(table td) {
  border: 1px solid #e0e0e0;
  padding: 0.75rem;
  text-align: left;
}

.article-content :deep(table th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

/* Mermaid 图表和错误内容宽度限制 */
.article-content :deep(.mermaid) {
  max-width: 100%;
  overflow: auto;
  cursor: pointer;
  transition: opacity 0.2s;
}

.article-content :deep(.mermaid:hover) {
  opacity: 0.8;
}

.article-content :deep(.mermaid-error) {
  max-width: 100%;
  overflow-x: auto;
  word-wrap: break-word;
  white-space: pre-wrap;
  background-color: #fff3cd;
  border: 1px solid #ffc107;
  padding: 1rem;
  border-radius: 8px;
  color: #856404;
  font-size: 0.875rem;
}

/* 移动端：限制公式宽度 */
@media (max-width: 750px) {
  .article-content :deep(.katex-display) {
    max-width: calc(100vw - 32px);
    overflow-x: auto;
    overflow-y: hidden;
  }
}

/* 核心操作按钮样式和动画 */
.core-actions .core-btn-inactive {
  transition: all 0.2s ease;
}

.core-actions .core-btn-inactive:hover {
  transform: scale(1.05);
}

.core-actions .core-btn-active {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

/* 详情页：点赞栏粘性固定到底部 */
.action-bar-sticky {
  position: sticky;
  bottom: 0;
  background-color: white;
  padding-bottom: 12px;
}

@media (max-width: 960px) {
  .action-bar-sticky {
    bottom: 60px;
  }
}

@media (max-width: 750px) {
  .action-bar-sticky {
    bottom: 60px;
    margin-left: -4px !important;
    margin-right: -4px !important;
    padding-left: 4px !important;
    padding-right: 4px !important;
  }
}

.full-article {
  max-height: none;
}

.vote-count {
  font-size: 1rem;
  font-weight: bold;
}
</style>
