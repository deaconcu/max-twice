<template>
  <DefaultLayout>
    <!-- 首次加载 - 全页 loading -->
    <LoadingSpinner v-if="dataLoading && isInitialLoad" />

    <!-- 内容区 -->
    <div v-else-if="data || dataLoading" class="read-page">
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
              <!-- 目录为空的提示 -->
              <div
                v-if="data && (!data.toc || data.toc.length === 0)"
                class="pa-8 text-center"
              >
                <v-icon icon="mdi-compass-outline" size="56" color="primary" class="mb-4"></v-icon>
                <div class="text-h6 text-medium-emphasis mb-3">开启目录导航</div>
                <div class="text-body-2 text-medium-emphasis mb-4">
                  在文章列表中选择一篇目录帖子
                  <br />
                  即可开启目录树导航功能
                </div>
                <v-btn
                  variant="tonal"
                  color="primary"
                  @click="drawerOpen = false"
                >
                  知道了
                </v-btn>
              </div>

              <!-- 目录组选择和关闭按钮 -->
              <div
                v-if="data && data.toc && data.toc.length > 0"
                class="toc-chips-row pa-4 pa-md-4 d-flex align-items-center flex-wrap"
              >
                <v-chip
                  size="default"
                  rounded="lg"
                  label
                  variant="tonal"
                  color=""
                  class="me-2 px-3 text-body-2 text-md-body-1 text-medium-emphasis"
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
                    :color="currContentsIndex === index ? 'grey' : 'surface-variant'"
                    class="me-2 text-body-2 text-md-body-1 font-weight-bold"
                    @click="currContentsIndex = index"
                  >
                    {{ index + 1 }}
                  </v-chip>
                  <div v-if="index === 0 && isLearning" class="corner-badge">
                    <v-icon
                      icon="mdi-check"
                      :size="$vuetify.display.mobile ? 8 : 10"
                      color="white"
                    />
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
                  :course-id="route.query.courseId ? data.course?.id : undefined"
                  :node-id="route.query.nodeId ? data.rootNodeId : undefined"
                  :path="data.path"
                  :curr-path="String(currContentsIndex + 1)"
                  :depth="1"
                  :is-learning="isLearning"
                  :toc-index="currContentsIndex"
                  @node-click="drawerOpen = false"
                />
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-dialog>

      <div class="read-content">
        <!-- 左侧目录 -->
        <div class="toc-sidebar">
          <div class="toc-sticky-wrapper">
            <!-- 课程头部 - 放到目录上方 -->
            <CourseHeader
              v-if="data"
              :parent-course-info="data.parentCourse"
              :current-course="data.course"
              :sub-course-list="data.subCourseList"
              :is-main-course="isMainCourse"
              :is-learning="isLearning"
              :course-progress="data.course?.progressPercent || 0"
              @start-learning="handleToggleLearning"
            />

            <!-- 目录为空的占位 -->
            <div v-if="data && (!data.toc || data.toc.length === 0)" class="toc-placeholder">
              <!-- 目录组占位 -->
              <div class="placeholder-chips mb-4">
                <div class="placeholder-chip"></div>
                <div class="placeholder-chip"></div>
                <div class="placeholder-chip"></div>
                <div class="placeholder-chip-btn"></div>
              </div>
              <!-- 目录树占位 -->
              <div class="placeholder-item mb-2"></div>
              <div class="placeholder-item mb-2" style="width: 85%"></div>
              <div class="placeholder-item mb-2" style="width: 70%"></div>
              <div class="placeholder-item mb-2"></div>
              <div class="placeholder-item mb-2" style="width: 90%"></div>
              <div class="placeholder-hint mt-5 text-center text-caption text-medium-emphasis">
                选择目录帖子开启导航
              </div>
            </div>

            <!-- 目录组选择卡片 -->
            <div v-if="data && data.toc && data.toc.length > 0" class="toc-groups-card">
              <div class="toc-chips">
                <v-chip
                  size="default"
                  rounded="lg"
                  label
                  variant="tonal"
                  color=""
                  class="me-0 px-3 text-body-2 text-md-body-1 text-medium-emphasis"
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
                    :color="currContentsIndex === index ? 'grey' : 'surface-variant'"
                    class="text-body-2 text-md-body-1 font-weight-bold"
                    @click="currContentsIndex = index"
                  >
                    {{ index + 1 }}
                  </v-chip>
                  <div v-if="index === 0 && isLearning" class="corner-badge">
                    <v-icon
                      icon="mdi-check"
                      :size="$vuetify.display.mobile ? 8 : 10"
                      color="white"
                    />
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
                  :course-id="route.query.courseId ? data.course?.id : undefined"
                  :node-id="route.query.nodeId ? data.rootNodeId : undefined"
                  :path="data.path"
                  :curr-path="String(currContentsIndex + 1)"
                  :depth="1"
                  :is-learning="isLearning"
                  :toc-index="currContentsIndex"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 中间+右侧容器包装 -->
        <div class="center-right-container">
          <!-- 中间+右侧容器 - 居中 -->
          <div class="center-right-wrapper">
            <!-- 中间内容区 -->
            <div class="center-content">
              <!-- 详情模式：有 postId 时显示 PostDetail -->
              <PostDetail
                v-if="route.query.postId"
                ref="postDetailRef"
                :show-node-header="true"
                :show-right-sidebar="true"
              />

              <!-- 列表模式：用 v-show 保持组件状态和滚动位置 -->
              <div v-show="!route.query.postId">
                <!-- 加载状态 - 只在非首次加载时显示在内容区 -->
                <LoadingSpinner v-if="dataLoading && !isInitialLoad" />

                <!-- PostingList 组件 -->
                <PostingList
                  v-else-if="data"
                  :data="data"
                  :nodes="nodes"
                  :curr-node-id="currNodeId"
                  :curr-node="lastPathNode"
                  :path-text="pathText"
                  :is-learning="isLearning"
                  :loading-more="loadingMore"
                  :has-more="hasMore"
                  :target-comment-id="targetCommentId"
                  :target-sub-comment-id="targetSubCommentId"
                  @view-deck="handleViewDeck"
                  @load-data="loadData"
                  @mark-node-completed="handleNodeCompleted"
                />
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
      :node-id="data.rootNodeId"
      :contents="data.toc || []"
      @load-data="loadData"
    />

    <!-- 卡片组详情对话框 -->
    <DeckDetailDialog
      v-model="showDeckDetailDialog"
      :deck="selectedDeck"
      :course-id="data?.course?.id"
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
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import CourseHeader from '@/components/features/read/CourseHeader.vue'
import TocSidebar from '@/components/features/read/TocSidebar.vue'
import TreeNode from '@/components/common/TreeNode.vue'
import PostingList from '@/components/features/read/PostingList.vue'
import PostDetail from '@/components/features/read/PostDetail.vue'
import ConfigContentsDialog from '@/components/features/read/ConfigContentsDialog.vue'
import DeckDetailDialog from '@/components/features/read/DeckDetailDialog.vue'
import { pageApi, memoryApi, postApi, progressApi, statsApi } from '@/api'
import type { ReadResponse } from '@/api/modules/page'
import type { MemoryCardDeck } from '@/types/memory'
import type { KeysetPageResponse } from '@/types/api'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { VoteType } from '@/enums'
import { convertVoteType } from '@/utils/postUtils'

// 将后端返回的数字类型转换为前端使用的字符串类型
const convertVoteType = (voteType: number | null | undefined): string | null => {
  if (!voteType || voteType === VoteType.NONE) return null
  if (voteType === VoteType.TWICE) return 'twice'
  if (voteType === VoteType.LIKE) return 'helpful'
  return null
}

const router = useRouter()
const route = useRoute()

// 基本状态
const showFixedBar = ref(false)
const isLearning = ref(false)
const openContentsList = ref(true)
const configContents = ref(false)
const currContentsIndex = ref(0)
const isAssistantExpanded = ref(true)
const postDetailRef = ref<InstanceType<typeof PostDetail> | null>(null)
const selectedDeck = ref<MemoryCardDeck | null>(null)
const showDeckDetailDialog = ref(false)
const loading = ref(false)
const error = ref<string | null>(null)
const isTocHovering = ref(false)
const drawerOpen = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)

// 获取课程ID
const courseId = computed(() => data.value?.course?.id)

// 目标评论ID（从 URL 获取）
const targetCommentId = computed(() => {
  if (route.query.commentId) {
    return Number(route.query.commentId)
  }
  return null
})

// 目标子评论ID（从 URL 获取）
const targetSubCommentId = computed(() => {
  if (route.query.subCommentId) {
    return Number(route.query.subCommentId)
  }
  return null
})

// 开始学习课程
const { execute: startLearning, loading: startingLearning } = useMutation(
  () => progressApi.startCourse(courseId.value!),
  {
    onSuccess: () => {
      isLearning.value = true
    },
  }
)

// 取消学习课程
const { execute: cancelLearning, loading: cancelingLearning } = useMutation(
  () => progressApi.cancelCourse(courseId.value!),
  {
    onSuccess: () => {
      isLearning.value = false
    },
  }
)

// 处理开始/取消学习
const handleToggleLearning = async (shouldStart: boolean) => {
  if (!courseId.value) return

  if (shouldStart) {
    await startLearning()
  } else {
    await cancelLearning()
  }
}

// 标记节点完成
const { execute: markNodeCompleted, loading: markingNode } = useMutation(
  () => {
    const nodeId = data.value?.node?.id
    const rootNodeId = data.value?.rootNodeId
    if (!nodeId || !rootNodeId) {
      throw new Error('节点ID或根节点ID不存在')
    }
    return progressApi.markNodeComplete(nodeId, rootNodeId)
  },
  {
    successMessage: '已标记节点完成',
    onSuccess: (response) => {
      // 更新当前节点的完成状态
      if (data.value && data.value.node) {
        data.value.node.isCompleted = response.completed
      }

      // 更新课程进度
      if (data.value && data.value.course && response.courseProgressPercent !== undefined) {
        data.value.course.progressPercent = response.courseProgressPercent
      }

      // 更新目录树中该节点的完成状态
      if (data.value && data.value.tocNodeInfos && response.nodeId) {
        const nodeInfo = data.value.tocNodeInfos[response.nodeId]
        if (nodeInfo) {
          nodeInfo.isCompleted = response.completed
        }
      }

      // 更新可完成节点标识
      if (data.value && data.value.tocNodeInfos && response.completableNodeIds) {
        // 先清除所有节点的 canComplete 标识
        Object.values(data.value.tocNodeInfos).forEach((info: any) => {
          info.canComplete = false
        })
        // 设置新的可完成节点
        response.completableNodeIds.forEach((nodeId: number) => {
          const nodeInfo = data.value!.tocNodeInfos[nodeId]
          if (nodeInfo) {
            nodeInfo.canComplete = true
          }
        })

        // 同时更新当前节点的 canComplete 状态
        if (data.value.node) {
          const currentNodeInfo = data.value.tocNodeInfos[data.value.node.id]
          if (currentNodeInfo) {
            data.value.node.canComplete = currentNodeInfo.canComplete
          }
        }
      }
    },
  }
)

// 取消节点完成
const { execute: unmarkNodeCompleted, loading: unmarkingNode } = useMutation(
  () => {
    const nodeId = data.value?.node?.id
    const rootNodeId = data.value?.rootNodeId
    if (!nodeId || !rootNodeId) {
      throw new Error('节点ID或根节点ID不存在')
    }
    return progressApi.unmarkNodeComplete(nodeId, rootNodeId)
  },
  {
    successMessage: '已取消节点完成',
    onSuccess: (response) => {
      // 更新当前节点的完成状态
      if (data.value && data.value.node) {
        data.value.node.isCompleted = response.completed
      }

      // 更新课程进度
      if (data.value && data.value.course && response.courseProgressPercent !== undefined) {
        data.value.course.progressPercent = response.courseProgressPercent
      }

      // 更新目录树中该节点的完成状态
      if (data.value && data.value.tocNodeInfos && response.nodeId) {
        const nodeInfo = data.value.tocNodeInfos[response.nodeId]
        if (nodeInfo) {
          nodeInfo.isCompleted = response.completed
        }
      }

      // 更新可完成节点标识
      if (data.value && data.value.tocNodeInfos && response.completableNodeIds) {
        // 先清除所有节点的 canComplete 标识
        Object.values(data.value.tocNodeInfos).forEach((info: any) => {
          info.canComplete = false
        })
        // 设置新的可完成节点
        response.completableNodeIds.forEach((nodeId: number) => {
          const nodeInfo = data.value!.tocNodeInfos[nodeId]
          if (nodeInfo) {
            nodeInfo.canComplete = true
          }
        })
      }
    },
  }
)

// 处理节点完成
const handleNodeCompleted = async () => {
  console.log('ContentReadPage: handleNodeCompleted 被调用', {
    nodeId: data.value?.node?.id,
    courseId: courseId.value,
    currentCompleted: data.value?.node?.isCompleted
  })

  // 如果已完成，则取消完成；如果未完成，则标记完成
  if (data.value?.node?.isCompleted) {
    await unmarkNodeCompleted()
  } else {
    await markNodeCompleted()
  }
}

// 关闭drawer
const closeDrawer = () => {
  drawerOpen.value = false
}

// 数据处理
const nodes = ref<any[]>([])
const currNodeId = ref(0)
const lastPathNode = ref<any>(null)
const pathText = ref('')

// 标记是否为首次加载
const isInitialLoad = ref(true)

// 记录上次加载的参数
const lastLoadedParams = ref({
  nodeId: route.query.nodeId,
  path: route.query.path,
  courseId: route.query.courseId,
})

// 使用 useFetch 加载页面数据
const {
  data,
  loading: dataLoading,
  execute: loadData,
} = useFetch<ReadResponse>({
  fetchFn: () => {
    if (route.query.nodeId && route.query.path) {
      return pageApi.readByNode(Number(route.query.nodeId), route.query.path as string)
    } else if (route.query.nodeId) {
      return pageApi.readByNode(Number(route.query.nodeId), '')
    } else if (route.query.courseId && route.query.path) {
      return pageApi.readByCoursePath(Number(route.query.courseId), route.query.path as string)
    } else if (route.query.courseId) {
      return pageApi.readByCoursePath(Number(route.query.courseId), '')
    }
    return Promise.reject(new Error('缺少必要参数'))
  },
  immediate: true,
  onDataReady: () => {
    // 首次加载完成后，标记为非首次
    isInitialLoad.value = false

    // 更新上次加载的参数
    lastLoadedParams.value = {
      nodeId: route.query.nodeId,
      path: route.query.path,
      courseId: route.query.courseId,
    }

    // 处理 toc 为 null 的情况，转换为空数组
    if (!data.value.toc) {
      data.value.toc = []
    }
    // 处理投票类型
    data.value.otherPostings?.forEach((posting: any) => {
      posting.voteType = convertVoteType(posting.voteType)
    })
    // 设置学习状态
    isLearning.value = data.value.learning || false

    // 处理可完成节点标识
    if (data.value.completableNodeIds && data.value.tocNodeInfos) {
      // 先清除所有节点的 canComplete 标识
      Object.values(data.value.tocNodeInfos).forEach((info: any) => {
        info.canComplete = false
      })
      // 设置新的可完成节点
      data.value.completableNodeIds.forEach((nodeId: number) => {
        const nodeInfo = data.value.tocNodeInfos[nodeId]
        if (nodeInfo) {
          nodeInfo.canComplete = true
        }
      })

      // 同时更新当前节点的 canComplete 状态
      if (data.value.node) {
        const currentNodeInfo = data.value.tocNodeInfos[data.value.node.id]
        if (currentNodeInfo) {
          data.value.node.canComplete = currentNodeInfo.canComplete
        }
      }
    }

    // 数据赋值完成后处理数据
    processData()
    // 检查是否有更多数据
    hasMore.value = data.value.otherPostings && data.value.otherPostings.length > 0
  },
})

// 使用第二个 useFetch 加载更多帖子
const {
  data: morePosts,
  loading: loadingMorePosts,
  execute: loadMorePosts,
} = useFetch<KeysetPageResponse<any>>({
  fetchFn: () => {
    if (!data.value || !data.value.otherPostings || !data.value.node) {
      return Promise.reject(new Error('No data'))
    }
    const lastPosting = data.value.otherPostings[data.value.otherPostings.length - 1]
    const nodeId = data.value.node.id
    return postApi.getNodePosts(nodeId, lastPosting.score, lastPosting.id)
  },
  immediate: false,
  onDataReady: () => {
    if (morePosts.value && morePosts.value.items && morePosts.value.items.length > 0) {
      // 处理投票类型
      morePosts.value.items.forEach((posting: any) => {
        posting.voteType = convertVoteType(posting.voteType)
      })
      // 追加到现有列表
      data.value.otherPostings = [...(data.value.otherPostings || []), ...morePosts.value.items]
      hasMore.value = morePosts.value.hasMore
    } else {
      hasMore.value = false
    }
  },
})

// 加载更多数据
const loadMore = async () => {
  if (loadingMore.value || !hasMore.value || !data.value || !data.value.otherPostings) return

  const lastPosting = data.value.otherPostings[data.value.otherPostings.length - 1]
  if (!lastPosting) return

  loadingMore.value = true
  try {
    await loadMorePosts()
    // hasMore 已在 onDataReady 中更新
  } catch (error) {
    console.error('Failed to load more posts:', error)
    hasMore.value = false
  } finally {
    loadingMore.value = false
  }
}

// 是否为主课程
const isMainCourse = computed(() => {
  if (data.value?.course && data.value.parentCourse) {
    return data.value.course.id === data.value.parentCourse.id
  }
  return true
})

// 处理数据
const processData = () => {
  console.log('data:', data.value)
  if (!data.value?.path) return

  // 如果目录为空，跳过目录相关处理
  if (!data.value.toc || data.value.toc.length === 0) {
    currNodeId.value = data.value.node?.id || 0
    isLearning.value = data.value.learning || false
    return
  }

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

  // 检查是否接近页面底部
  const scrollPosition = window.scrollY + window.innerHeight
  const pageHeight = document.documentElement.scrollHeight
  const threshold = 200 // 距离底部200px时触发加载

  if (scrollPosition >= pageHeight - threshold) {
    loadMore()
  }
}

// 切换学习状态
const toggleLearning = () => {
  isLearning.value = !isLearning.value
}

// 返回上一页
const goBackToCourse = () => {
  router.back()
}

// 处理查看卡片组详情
const handleViewDeck = (deck: MemoryCardDeck) => {
  selectedDeck.value = deck
  showDeckDetailDialog.value = true
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
    route.query.courseId,
  ],
  () => {
    // 检查参数是否与上次加载的相同
    const nodeIdSame = lastLoadedParams.value.nodeId === route.query.nodeId
    const pathSame = lastLoadedParams.value.path === route.query.path
    const courseIdSame = lastLoadedParams.value.courseId === route.query.courseId

    const isSame = nodeIdSame && pathSame && courseIdSame

    // 参数相同，不重新加载（使用缓存）
    if (isSame) {
      return
    }

    // 参数变化了，重新加载数据
    loadData()
    // 注意：参数会在 onDataReady 回调中更新
  },
  { deep: true }
)

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.read-page {
  background-color: #ffffff;
}

/* 课程头部 */
.course-header-sticky {
  background-color: white;
  padding-bottom: 0px;
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
  padding-top: 24px;
}

/* 左侧 TOC 目录栏 */
.toc-sidebar {
  flex: 0 1 360px;
  max-width: 360px;
  padding: 0 0 24px 0;
  position: relative;
  margin-right: 20px;
  min-height: calc(100vh - 125px);
}

.toc-sidebar::after {
  content: '';
  position: absolute;
  top: 6px;
  right: 0;
  bottom: 24px;
  width: 1px;
  background-color: rgb(var(--v-theme-border));
}

.toc-sticky-wrapper {
  position: sticky;
  top: 56px;
  max-height: calc(100vh - 71px);
  display: flex;
  flex-direction: column;
}

.toc-card {
  background-color: white;
  padding: 4px 0;
  border-radius: 16px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.toc-tree {
  margin-top: 6px;
  padding-right: 42px;
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
  background-color: rgb(var(--v-theme-surface-variant));
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

/* 目录组选择卡片 */
.toc-groups-card {
  padding: 0;
  margin-right: 42px;
  margin-bottom: 0;
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
  border: 1px solid white;
}

.config-btn {
  opacity: 0.5;
  transition: opacity 0.2s ease;
}

.config-btn:hover {
  opacity: 1;
}

/* 目录占位符 */
.toc-placeholder {
  padding: 0 42px 0 0;
}

.placeholder-chips {
  display: flex;
  gap: 8px;
  align-items: center;
}

.placeholder-chip {
  width: 32px;
  height: 32px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.placeholder-chip-btn {
  width: 24px;
  height: 24px;
  background-color: #f5f5f5;
  border-radius: 50%;
  margin-left: auto;
}

.placeholder-item {
  height: 16px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.placeholder-hint {
  opacity: 0.6;
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

/* 中间内容区 - 子组件自己管理布局 */
.center-content {
  flex: 1;
  min-width: 0;
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
  }

  .toc-sidebar {
    display: none;
  }

  .center-right-wrapper {
    justify-content: center;
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
  }

  .toc-sidebar {
    display: none;
  }

  .center-right-container {
    width: 100% !important;
  }

  .center-right-wrapper {
    width: 100% !important;
    max-width: none !important;
  }
}
</style>
