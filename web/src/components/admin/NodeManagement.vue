<script setup lang="ts">
import { inject, ref } from 'vue'
import { adminApi } from '@/api'
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
  fetchFn: (params) => {
    const currentTabConfig = tabs.find((tab) => tab.key === currentTab.value)

    if (isFilterMode.value) {
      // 筛选模式：如果使用节点 ID 筛选则不传递状态（节点 ID 唯一）
      const state = filterNodeId.value ? null : currentTabConfig?.state || null
      return adminApi.getAdminNodes(
        state,
        filterNodeId.value || null,
        filterCourseId.value || null,
        filterCreatorId.value || null,
        params.lastId
      )
    } else {
      return adminApi.getAdminNodes(
        currentTabConfig?.state || null,
        null,
        null,
        null,
        params.lastId
      )
    }
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: {
    lastId: null,
  },
})

// 应用筛选
const applyFilter = (): void => {
  if (!filterNodeId.value && !filterCourseId.value && !filterCreatorId.value) {
    showSnackbar?.('请至少输入一个筛选条件', 'warning')
    return
  }
  isFilterMode.value = true
  resetNodeList()
}

// 清除筛选
const clearFilter = (): void => {
  filterNodeId.value = undefined
  filterCourseId.value = undefined
  filterCreatorId.value = undefined
  isFilterMode.value = false
  resetNodeList()
}

// 监听tab切换
const handleTabChange = (newTab: string) => {
  resetNodeList()
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
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-file-tree-outline" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">节点管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">查询和管理课程节点</p>
        </div>
      </div>
      <v-chip variant="flat" color="blue-lighten-4" rounded="lg">
        <v-icon icon="mdi-file-tree" color="blue-darken-2" size="16" class="mr-1"></v-icon>
        <span class="text-blue-darken-2 text-caption">{{ nodeList.length }}</span>
      </v-chip>
    </div>

    <!-- 筛选区域 -->
    <v-card v-if="!isFilterMode" flat class="pa-4 bg-grey-lighten-5 rounded-lg mb-6">
      <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
        <v-icon icon="mdi-filter-outline" size="16" class="mr-2"></v-icon>
        高级筛选
      </h4>
      <v-row dense align="center">
        <v-col cols="12" sm="3">
          <v-text-field
            v-model.number="filterNodeId"
            type="number"
            label="节点 ID"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="3">
          <v-text-field
            v-model.number="filterCourseId"
            type="number"
            label="课程 ID"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="3">
          <v-text-field
            v-model.number="filterCreatorId"
            type="number"
            label="创建者 ID"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="3">
          <v-btn variant="flat" color="primary" rounded="lg" block @click="applyFilter">
            <v-icon icon="mdi-magnify" class="mr-1"></v-icon>
            筛选
          </v-btn>
        </v-col>
      </v-row>
    </v-card>

    <!-- 筛选结果提示 -->
    <v-alert
      v-if="isFilterMode"
      type="info"
      color="primary"
      variant="tonal"
      class="mb-6"
      rounded="lg"
      closable
      @click:close="clearFilter"
    >
      <div class="d-flex align-center justify-space-between">
        <div>
          <span class="font-weight-medium">筛选条件：</span>
          <v-chip v-if="filterNodeId" size="small" class="mx-1">节点 ID: {{ filterNodeId }}</v-chip>
          <v-chip v-if="filterCourseId" size="small" class="mx-1"
            >课程 ID: {{ filterCourseId }}</v-chip
          >
          <v-chip v-if="filterCreatorId" size="small" class="mx-1"
            >创建者 ID: {{ filterCreatorId }}</v-chip
          >
        </div>
      </div>
    </v-alert>

    <!-- 状态标签 -->
    <v-tabs
      v-if="!filterNodeId"
      v-model="currentTab"
      color="primary"
      class="mb-6"
      show-arrows
      @update:model-value="handleTabChange"
    >
      <v-tab v-for="tab in tabs" :key="tab.key" :value="tab.key" class="text-none">
        <v-icon :icon="tab.icon" :color="`${tab.color}-darken-1`" size="18" class="mr-2"></v-icon>
        {{ tab.label }}
      </v-tab>
    </v-tabs>

    <!-- 空状态 -->
    <div v-if="nodeList.length === 0 && !loading" class="text-center py-12">
      <v-icon icon="mdi-file-tree-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">
        {{
          isFilterMode
            ? '未找到符合条件的节点'
            : `暂无${tabs.find((tab) => tab.key === currentTab)?.label}的节点`
        }}
      </p>
    </div>

    <!-- 节点列表 -->
    <div
      v-for="node in nodeList"
      :key="node.id"
      v-intersect="{
        handler: (isIntersecting) => {
          if (isIntersecting && node === nodeList[nodeList.length - 1] && hasMore && !loading) {
            loadMore()
          }
        },
      }"
      class="mb-4"
    >
      <v-card flat class="border rounded-lg pa-5" hover>
        <div class="d-flex align-start">
          <!-- 状态和操作区域 -->
          <div class="mr-4 status-actions-area">
            <div class="mb-3">
              <v-chip
                v-if="node.state === ContentState.SUBMITTED"
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                待审核
              </v-chip>
              <v-chip
                v-if="node.state === ContentState.PUBLISHED"
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                已通过
              </v-chip>
              <v-chip
                v-if="node.state === ContentState.REJECTED"
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                已拒绝
              </v-chip>
              <v-chip
                v-if="node.state === ContentState.BANNED"
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-cancel" size="14" class="mr-1"></v-icon>
                已封禁
              </v-chip>
            </div>

            <!-- 待审核状态：通过、拒绝、屏蔽 -->
            <div v-if="node.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="updateNodeState(node, ContentState.PUBLISHED)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                通过
              </v-btn>
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="showRejectDialog(node)"
              >
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                拒绝
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                @click="showBanDialog(node)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已通过状态：撤销通过、屏蔽 -->
            <div v-if="node.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                @click="showRejectDialog(node)"
              >
                <v-icon icon="mdi-undo" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                撤销通过
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                @click="showBanDialog(node)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已拒绝状态：通过、屏蔽 -->
            <div v-if="node.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="updateNodeState(node, ContentState.PUBLISHED)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                通过
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                @click="showBanDialog(node)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已屏蔽状态：取消屏蔽、降级为拒绝 -->
            <div v-if="node.state === ContentState.BANNED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="blue-lighten-4"
                rounded="lg"
                size="small"
                @click="updateNodeState(node, ContentState.PUBLISHED)"
              >
                <v-icon icon="mdi-lock-open" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                取消屏蔽
              </v-btn>
              <v-btn
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                @click="updateNodeState(node, ContentState.REJECTED)"
              >
                <v-icon
                  icon="mdi-arrow-down"
                  color="orange-darken-2"
                  size="16"
                  class="mr-1"
                ></v-icon>
                降级为拒绝
              </v-btn>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-3">
              <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                <v-icon icon="mdi-file-tree" color="grey-darken-1" size="18"></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-2">
                  节点ID: {{ node.id }} | 课程ID: {{ node.courseId }}
                </div>
                <div class="text-caption text-grey-darken-1">{{ node.createdAt }}</div>
              </div>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <div class="text-h6 font-weight-bold text-grey-darken-3 mb-2">
                {{ node.name }}
              </div>
              <div v-if="node.description" class="text-body-2 text-grey-darken-1">
                {{ node.description }}
              </div>
            </div>
          </div>
        </div>
      </v-card>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="text-center py-4">
      <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
      <span class="ml-2 text-grey-darken-1">搜索中...</span>
    </div>

    <!-- 没有更多数据提示 -->
    <div v-if="!hasMore && nodeList.length > 0" class="text-center py-4">
      <span class="text-grey-darken-1">没有更多数据了</span>
    </div>

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
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.status-actions-area {
  min-width: 200px;
}
</style>
