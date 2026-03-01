<script setup lang="ts">
import { inject, ref } from 'vue'
import { adminApi } from '@/api'
import { postApi } from '@/api/modules/post'
import { ContentState } from '@/enums'
import type { Node } from '@/types/node.d'
import RejectBanDialog from './RejectBanDialog.vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 当前选中的tab
const currentTab = ref<string>('pending')

// 标签配置
interface TabConfig {
  key: string
  label: string
  state: ContentState
  icon: string
  color: string
}

const tabs: TabConfig[] = [
  {
    key: 'pending',
    label: '待审核',
    state: ContentState.SUBMITTED,
    icon: 'mdi-clock-outline',
    color: 'orange',
  },
  {
    key: 'approved',
    label: '已通过',
    state: ContentState.PUBLISHED,
    icon: 'mdi-check-circle',
    color: 'green',
  },
  {
    key: 'rejected',
    label: '已拒绝',
    state: ContentState.REJECTED,
    icon: 'mdi-close-circle',
    color: 'red',
  },
  {
    key: 'banned',
    label: '已封禁',
    state: ContentState.BANNED,
    icon: 'mdi-cancel',
    color: 'grey',
  },
]

// 筛选条件
const filterNodeId = ref<number | undefined>(undefined)
const filterCourseId = ref<number | undefined>(undefined)
const filterCreatorId = ref<number | undefined>(undefined)
const isFilterMode = ref<boolean>(false)

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const currentNode = ref<Node | null>(null)
const dialogType = ref<'reject' | 'ban'>('reject')

// 使用 useInfiniteScroll 加载节点列表
const {
  items: nodeList,
  loading,
  hasMore,
  loadMore,
  reset: resetNodeList,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const currentTabConfig = tabs.find((tab) => tab.key === currentTab.value)

    if (isFilterMode.value) {
      // 筛选模式：使用筛选接口
      const state = filterNodeId.value ? undefined : currentTabConfig?.state
      const response = await adminApi.getNodesByFilter(
        state,
        filterNodeId.value,
        filterCourseId.value,
        filterCreatorId.value,
        params.lastId
      )
      const pageData = response.data
      return {
        code: response.code,
        data: pageData?.items || [],
        message: response.message || '',
        hasMore: pageData?.hasMore ?? false,
      }
    } else {
      // 普通模式：使用统一的状态筛选接口
      const response = await adminApi.getContentsByState('node', currentTabConfig?.state, params.lastId)
      const pageData = response.data
      return {
        code: response.code,
        data: pageData?.items || [],
        message: response.message || '',
        hasMore: pageData?.hasMore ?? false,
      }
    }
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: {
    lastId: null,
  },
  immediate: true,
})

// 应用筛选
const applyFilter = (): void => {
  if (!filterNodeId.value && !filterCourseId.value && !filterCreatorId.value) {
    showSnackbar?.('请至少输入一个筛选条件', 'warning')
    return
  }
  isFilterMode.value = true
  resetNodeList()
  loadMore()
}

// 清除筛选
const clearFilter = (): void => {
  filterNodeId.value = undefined
  filterCourseId.value = undefined
  filterCreatorId.value = undefined
  isFilterMode.value = false
  resetNodeList()
  loadMore()
}

// 无限滚动回调接口
type InfiniteScrollCallback = (status: 'ok' | 'empty' | 'error') => void

// v-infinite-scroll 加载更多回调
const onLoadMore = async ({ done }: { done: InfiniteScrollCallback }): Promise<void> => {
  if (!hasMore.value || loading.value) {
    done('empty')
    return
  }

  try {
    await loadMore()
    if (hasMore.value) {
      done('ok')
    } else {
      done('empty')
    }
  } catch (error) {
    done('error')
  }
}

// 监听tab切换
const handleTabChange = () => {
  resetNodeList()
  loadMore()
}

const getStateColor = (state: ContentState): string => {
  switch (state) {
    case ContentState.SUBMITTED:
      return 'orange-lighten-4'
    case ContentState.PUBLISHED:
      return 'green-lighten-4'
    case ContentState.REJECTED:
      return 'red-lighten-4'
    case ContentState.BANNED:
      return 'grey-lighten-2'
    default:
      return 'grey-lighten-4'
  }
}

const getStateText = (state: ContentState): string => {
  switch (state) {
    case ContentState.SUBMITTED:
      return '待审核'
    case ContentState.PUBLISHED:
      return '已通过'
    case ContentState.REJECTED:
      return '已拒绝'
    case ContentState.BANNED:
      return '已封禁'
    default:
      return '未知'
  }
}

// 使用 useMutation 更新节点状态
const { execute: executeUpdateNodeState } = useMutation(
  (data: { nodeId: number; newState: number; reason?: string }) =>
    adminApi.updateNodeState(data.nodeId, data.newState, data.reason),
  {
    onSuccess: (response, data) => {
      const updatedNode = response as Node
      showSnackbar?.(`节点状态已更新为${getStateText(data.newState)}`, 'success')

      if (!isFilterMode.value) {
        const index = nodeList.value.findIndex((n) => n.id === data.nodeId)
        if (index !== -1) {
          nodeList.value.splice(index, 1)
        }
      } else {
        const index = nodeList.value.findIndex((n) => n.id === data.nodeId)
        if (index !== -1) {
          nodeList.value[index].state = updatedNode.state
        }
      }
    },
  }
)

const updateNodeState = async (node: Node, newState: number) => {
  await executeUpdateNodeState({ nodeId: node.id, newState })
}

// 打开拒绝对话框
const showRejectDialog = (node: Node) => {
  currentNode.value = node
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 打开屏蔽对话框
const showBanDialog = (node: Node) => {
  currentNode.value = node
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (data: { nodeId: number; targetState: number; reason: string }) =>
    adminApi.updateNodeState(data.nodeId, data.targetState, data.reason),
  {
    onSuccess: (_, data) => {
      const message = data.targetState === ContentState.REJECTED ? '已拒绝' : '已屏蔽'
      showSnackbar?.(message, 'success')

      if (!isFilterMode.value) {
        const index = nodeList.value.findIndex((n) => n.id === data.nodeId)
        if (index !== -1) {
          nodeList.value.splice(index, 1)
        }
      } else {
        const index = nodeList.value.findIndex((n) => n.id === data.nodeId)
        if (index !== -1) {
          nodeList.value[index].state = data.targetState
        }
      }

      showReasonDialog.value = false
      currentNode.value = null
    },
  }
)

// 确认操作
const handleConfirmAction = async (reason: string) => {
  if (!currentNode.value) return

  const targetState = dialogType.value === 'reject' ? ContentState.REJECTED : ContentState.BANNED
  await executeRejectOrBan({
    nodeId: currentNode.value.id,
    targetState,
    reason,
  })
}

// 初始化 Embedding
const initializingEmbeddings = ref(false)
const initEmbeddingResult = ref<{ successCount: number; failCount: number; totalProcessed: number } | null>(null)

const initializeEmbeddings = async () => {
  if (!confirm('确定要初始化所有节点的 embedding 吗？这可能需要几分钟时间。')) {
    return
  }

  initializingEmbeddings.value = true
  initEmbeddingResult.value = null

  try {
    const response = await postApi.initNodeEmbeddings(20)
    initEmbeddingResult.value = response.data
    showSnackbar?.(`初始化完成！成功: ${response.data.successCount}, 失败: ${response.data.failCount}`, 'success')
  } catch (error) {
    console.error('初始化 embedding 失败', error)
    showSnackbar?.('初始化失败', 'error')
  } finally {
    initializingEmbeddings.value = false
  }
}
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-4">
      <h2 class="text-h5 font-weight-bold">节点管理</h2>
      <v-btn
        variant="outlined"
        color="purple"
        rounded="lg"
        :loading="initializingEmbeddings"
        :disabled="initializingEmbeddings"
        @click="initializeEmbeddings"
      >
        <v-icon icon="mdi-vector-polyline" class="mr-1"></v-icon>
        初始化 Embedding
      </v-btn>
    </div>

    <!-- ID查询 -->
    <v-card flat class="border mb-4">
      <v-card-text>
        <v-row align="center">
          <v-col cols="2">
            <v-text-field
              v-model.number="filterNodeId"
              type="number"
              label="节点 ID"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="applyFilter"
            ></v-text-field>
          </v-col>
          <v-col cols="2">
            <v-text-field
              v-model.number="filterCourseId"
              type="number"
              label="课程 ID"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="applyFilter"
            ></v-text-field>
          </v-col>
          <v-col cols="2">
            <v-text-field
              v-model.number="filterCreatorId"
              type="number"
              label="创建者 ID"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="applyFilter"
            ></v-text-field>
          </v-col>
          <v-col cols="auto">
            <v-btn variant="tonal" size="default" @click="applyFilter">
              <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
              查询
            </v-btn>
            <v-btn
              v-if="isFilterMode"
              variant="text"
              size="default"
              @click="clearFilter"
            >
              清除
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 节点列表 -->
    <v-card flat class="border">
      <v-card-text>
        <!-- 状态标签 -->
        <v-tabs
          v-if="!isFilterMode"
          v-model="currentTab"
          color="primary"
          density="compact"
          @update:model-value="handleTabChange"
          class="mb-4"
        >
          <v-tab v-for="tab in tabs" :key="tab.key" :value="tab.key" class="text-none" size="small">
            <v-icon :icon="tab.icon" size="14" class="mr-1"></v-icon>
            {{ tab.label }}
          </v-tab>
        </v-tabs>

        <!-- 首次加载状态 -->
        <div v-if="loading && nodeList.length === 0" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!loading && nodeList.length === 0" class="text-center py-12">
          <v-icon icon="mdi-file-tree-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">
            {{ isFilterMode ? '未找到符合条件的节点' : `暂无${tabs.find((tab) => tab.key === currentTab)?.label}的节点` }}
          </p>
        </div>

        <!-- 列表 -->
        <div v-else>
          <v-infinite-scroll
            :empty="!hasMore"
            @load="onLoadMore"
          >
            <div
              v-for="node in nodeList"
              :key="node.id"
              class="list-item mb-3"
            >
              <div class="d-flex align-start">
                <!-- 操作区 -->
                <div class="action-area mr-4">
                  <!-- 待审核 -->
                  <div v-if="node.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                    <v-btn variant="tonal" color="success" size="small" block @click="updateNodeState(node, ContentState.PUBLISHED)">
                      批准
                    </v-btn>
                    <v-btn variant="tonal" color="error" size="small" block @click="showRejectDialog(node)">
                      拒绝
                    </v-btn>
                    <v-btn variant="tonal" color="grey" size="small" block @click="showBanDialog(node)">
                      屏蔽
                    </v-btn>
                  </div>

                  <!-- 已通过 -->
                  <div v-if="node.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                    <v-btn variant="tonal" color="warning" size="small" block @click="showRejectDialog(node)">
                      撤回
                    </v-btn>
                    <v-btn variant="tonal" color="grey" size="small" block @click="showBanDialog(node)">
                      屏蔽
                    </v-btn>
                  </div>

                  <!-- 已拒绝 -->
                  <div v-if="node.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                    <v-btn variant="tonal" color="success" size="small" block @click="updateNodeState(node, ContentState.PUBLISHED)">
                      通过
                    </v-btn>
                    <v-btn variant="tonal" color="grey" size="small" block @click="showBanDialog(node)">
                      屏蔽
                    </v-btn>
                  </div>

                  <!-- 已屏蔽 -->
                  <div v-if="node.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                    <v-btn variant="tonal" color="info" size="small" block @click="updateNodeState(node, ContentState.PUBLISHED)">
                      解封
                    </v-btn>
                    <v-btn variant="tonal" color="warning" size="small" block @click="updateNodeState(node, ContentState.REJECTED)">
                      降级
                    </v-btn>
                  </div>
                </div>

                <!-- 内容区 -->
                <div class="flex-grow-1">
                  <!-- 标题行 -->
                  <div class="d-flex align-center justify-space-between mb-2">
                    <div class="d-flex align-center">
                      <div class="text-body-1 font-weight-medium text-grey-darken-3">
                        {{ node.name }}
                      </div>
                      <v-chip variant="flat" :color="getStateColor(node.state)" size="x-small" class="ml-2">
                        {{ getStateText(node.state) }}
                      </v-chip>
                    </div>
                    <div class="d-flex align-center text-caption text-grey-darken-1">
                      <span>{{ node.createdAt }}</span>
                      <span class="mx-1">·</span>
                      <span>ID: {{ node.id }}</span>
                      <span class="mx-1">·</span>
                      <span>课程 ID: {{ node.courseId }}</span>
                    </div>
                  </div>

                  <!-- 内容 -->
                  <div class="content-wrapper">
                    <div v-if="node.description" class="text-body-2 text-grey-darken-1">
                      {{ node.description }}
                    </div>
                    <div v-else class="text-body-2 text-grey-darken-1">
                      暂无描述
                    </div>

                    <!-- 拒绝/封禁原因 -->
                    <div v-if="(node.state === ContentState.REJECTED || node.state === ContentState.BANNED) && node.reason" class="mt-2">
                      <span class="text-caption text-red-darken-2">{{ node.state === ContentState.BANNED ? '封禁' : '拒绝' }}原因：{{ node.reason }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <template #empty>
              <div class="text-center py-4 text-caption text-grey">
                没有更多了
              </div>
            </template>

            <template #loading>
              <div class="text-center py-4">
                <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
                <span class="ml-2 text-grey-darken-1">加载中...</span>
              </div>
            </template>
          </v-infinite-scroll>
        </div>
      </v-card-text>
    </v-card>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="currentNode?.name || ''"
      :item-state="currentNode?.state"
      item-type="节点"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />
  </div>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.list-item {
  padding: 16px;
  border-radius: 8px;
  background-color: #fafafa;
}

.action-area {
  width: 70px;
  flex-shrink: 0;
}

.content-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  background-color: white;
}
</style>
