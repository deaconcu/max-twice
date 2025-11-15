<script setup lang="ts">
import { inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { adminApi } from '@/api'
import { ContentState, ApprovalAction } from '@/enums'
import RejectBanDialog from './RejectBanDialog.vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 筛选条件
const filterForm = ref({
  nodeId: null as number | null,
  courseId: null as number | null,
  creatorId: null as number | null
})

const selectedState = ref<number>(ContentState.SUBMITTED)

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const currentNode = ref<any | null>(null)
const dialogType = ref<'reject' | 'ban'>('reject')

// 状态选项
const stateOptions = [
  {
    value: ContentState.SUBMITTED,
    text: '待审核',
    color: 'orange-lighten-4',
    icon: 'mdi-clock-outline'
  },
  {
    value: ContentState.PUBLISHED,
    text: '已批准',
    color: 'green-lighten-4',
    icon: 'mdi-check-circle-outline'
  },
  {
    value: ContentState.REJECTED,
    text: '已拒绝',
    color: 'red-lighten-4',
    icon: 'mdi-close-circle-outline'
  },
  {
    value: ContentState.BANNED,
    text: '已屏蔽',
    color: 'grey-lighten-2',
    icon: 'mdi-cancel'
  }
]

// 获取状态配置
const getStateConfig = (state: number) => {
  return stateOptions.find(option => option.value === state) || stateOptions[0]
}

// 使用 useInfiniteScroll 加载节点列表
const {
  items: nodeList,
  loading,
  hasMore,
  loadMore,
  reset: resetNodeList
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const response = await adminApi.getNodesByFilter(
      filterForm.value.nodeId || undefined,
      filterForm.value.courseId || undefined,
      filterForm.value.creatorId || undefined,
      selectedState.value,
      params.lastId
    )
    return response.data
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id
  }),
  initialParams: {
    lastId: undefined
  }
})

// 应用筛选
const applyFilter = () => {
  resetNodeList()
}

// 重置筛选
const resetFilter = () => {
  filterForm.value = {
    nodeId: null,
    courseId: null,
    creatorId: null
  }
  applyFilter()
}

// 状态变化处理
const onStateChange = (): void => {
  resetNodeList()
}

// 使用 useMutation 批准节点
const { execute: executeApproveNode } = useMutation(
  (nodeId: number) => adminApi.approveNode(nodeId, { action: ApprovalAction.APPROVE }),
  {
    successMessage: '节点已批准',
    onSuccess: (_, nodeId) => {
      const index = nodeList.value.findIndex((n: any) => n.id === nodeId)
      if (index > -1) {
        nodeList.value.splice(index, 1)
      }
    }
  }
)

const approveNode = async (node: any): Promise<void> => {
  await executeApproveNode(node.id)
}

// 显示拒绝对话框
const showRejectModal = (node: any) => {
  currentNode.value = node
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 显示屏蔽对话框
const showBanModal = (node: any) => {
  currentNode.value = node
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (data: { nodeId: number; action: ApprovalAction; reason: string }) =>
    adminApi.approveNode(data.nodeId, { action: data.action, reason: data.reason }),
  {
    onSuccess: (_, data) => {
      const message = data.action === ApprovalAction.BAN ? '已屏蔽' : '已拒绝'
      showSnackbar?.(message, 'success')

      const index = nodeList.value.findIndex((n: any) => n.id === data.nodeId)
      if (index > -1) {
        nodeList.value.splice(index, 1)
      }

      showReasonDialog.value = false
      currentNode.value = null
    }
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string) => {
  if (!currentNode.value) return

  const action = dialogType.value === 'ban' ? ApprovalAction.BAN : ApprovalAction.REJECT
  await executeRejectOrBan({
    nodeId: currentNode.value.id,
    action,
    reason
  })
}

// 使用 useMutation 取消屏蔽节点
const { execute: executeUnbanNode } = useMutation(
  (nodeId: number) => adminApi.approveNode(nodeId, { action: ApprovalAction.APPROVE }),
  {
    successMessage: '已取消屏蔽',
    onSuccess: (_, nodeId) => {
      const index = nodeList.value.findIndex((n: any) => n.id === nodeId)
      if (index > -1) {
        nodeList.value.splice(index, 1)
      }
    }
  }
)

const unbanNode = async (node: any): Promise<void> => {
  await executeUnbanNode(node.id)
}

// 恢复节点（从拒绝状态恢复）
const restoreNode = async (node: any): Promise<void> => {
  await executeApproveNode(node.id)
}
</script>

<template>
  <div class="node-management">
    <!-- 页面头部 -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-indigo-lighten-5 mr-3">
          <v-icon icon="mdi-file-tree" color="indigo-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">节点管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">审核和管理课程节点</p>
        </div>
      </div>
      <v-chip variant="flat" color="indigo-lighten-4" rounded="lg">
        <v-icon icon="mdi-file-document" color="indigo-darken-2" size="16" class="mr-2"></v-icon>
        <span class="text-indigo-darken-2 text-caption">{{ nodeList.length }}个节点</span>
      </v-chip>
    </div>

    <!-- 筛选区域 -->
    <v-card flat class="pa-4 bg-grey-lighten-5 rounded-lg mb-6">
      <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
        <v-icon icon="mdi-filter-outline" size="16" class="mr-2"></v-icon>
        高级筛选
      </h4>
      <v-row dense>
        <v-col cols="12" md="3">
          <v-text-field
            v-model.number="filterForm.nodeId"
            label="节点ID"
            type="number"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="3">
          <v-text-field
            v-model.number="filterForm.courseId"
            label="课程ID"
            type="number"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="3">
          <v-text-field
            v-model.number="filterForm.creatorId"
            label="用户ID"
            type="number"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="3">
          <div class="d-flex gap-2">
            <v-btn
              variant="flat"
              color="primary"
              rounded="lg"
              @click="applyFilter"
            >
              <v-icon icon="mdi-magnify" class="mr-1"></v-icon>
              筛选
            </v-btn>
            <v-btn
              variant="outlined"
              color="grey"
              rounded="lg"
              @click="resetFilter"
            >
              <v-icon icon="mdi-refresh" class="mr-1"></v-icon>
              重置
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </v-card>

    <!-- 状态选择 -->
    <v-select
      v-model="selectedState"
      :items="stateOptions"
      item-title="text"
      item-value="value"
      label="状态筛选"
      variant="outlined"
      density="compact"
      rounded="lg"
      bg-color="white"
      class="mb-6"
      style="max-width: 200px"
      @update:model-value="onStateChange"
    ></v-select>

    <!-- 加载状态 -->
    <div v-if="loading && nodeList.length === 0" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="mt-3 text-grey-darken-1">加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="nodeList.length === 0" class="text-center py-12">
      <v-icon icon="mdi-file-tree-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无符合条件的节点</p>
    </div>

    <!-- 节点列表 -->
    <div v-else>
      <div
        v-for="node in nodeList"
        :key="node.id"
        class="mb-4"
        v-intersect="{
          handler: (isIntersecting: boolean) => {
            if (isIntersecting && node === nodeList[nodeList.length - 1] && hasMore && !loading) {
              loadMore()
            }
          }
        }"
      >
        <v-card flat class="border rounded-lg pa-5" hover>
          <div class="d-flex align-start">
            <!-- 状态和操作区域 -->
            <div class="mr-4 status-actions-area">
              <div class="mb-3">
                <v-chip
                  :color="getStateConfig(node.state).color"
                  variant="flat"
                  rounded="lg"
                  size="small"
                >
                  <v-icon
                    :icon="getStateConfig(node.state).icon"
                    size="14"
                    class="mr-1"
                  ></v-icon>
                  {{ getStateConfig(node.state).text }}
                </v-chip>
              </div>

              <!-- 审核操作按钮 -->
              <div class="d-flex flex-column ga-2">
                <!-- 待审核状态：通过、拒绝、屏蔽 -->
                <template v-if="node.state === ContentState.SUBMITTED">
                  <v-btn
                    variant="flat"
                    color="green-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="approveNode(node)"
                  >
                    <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                    批准
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="red-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="showRejectModal(node)"
                  >
                    <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                    拒绝
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="grey-lighten-2"
                    rounded="lg"
                    size="small"
                    @click="showBanModal(node)"
                  >
                    <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                    屏蔽
                  </v-btn>
                </template>

                <!-- 已发布状态：撤销通过、屏蔽 -->
                <template v-if="node.state === ContentState.PUBLISHED">
                  <v-btn
                    variant="flat"
                    color="orange-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="showRejectModal(node)"
                  >
                    <v-icon icon="mdi-undo" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                    撤销通过
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="grey-lighten-2"
                    rounded="lg"
                    size="small"
                    @click="showBanModal(node)"
                  >
                    <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                    屏蔽
                  </v-btn>
                </template>

                <!-- 已拒绝状态：通过、屏蔽 -->
                <template v-if="node.state === ContentState.REJECTED">
                  <v-btn
                    variant="flat"
                    color="green-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="restoreNode(node)"
                  >
                    <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                    通过
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="grey-lighten-2"
                    rounded="lg"
                    size="small"
                    @click="showBanModal(node)"
                  >
                    <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                    屏蔽
                  </v-btn>
                </template>

                <!-- 已屏蔽状态：取消屏蔽、降级为拒绝 -->
                <template v-if="node.state === ContentState.BANNED">
                  <v-btn
                    variant="flat"
                    color="blue-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="unbanNode(node)"
                  >
                    <v-icon icon="mdi-lock-open" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                    取消屏蔽
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="orange-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="showRejectModal(node)"
                  >
                    <v-icon icon="mdi-arrow-down" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                    降级为拒绝
                  </v-btn>
                </template>
              </div>
            </div>

            <!-- 节点内容区域 -->
            <div class="flex-grow-1">
              <div class="d-flex align-center justify-space-between mb-3">
                <h4 class="text-h6 font-weight-bold text-grey-darken-3">{{ node.title }}</h4>
                <div class="d-flex align-center gap-4">
                  <div class="text-caption text-grey-darken-1">
                    节点ID: {{ node.id }}
                  </div>
                  <div class="text-caption text-grey-darken-1">
                    课程ID: {{ node.courseId }}
                  </div>
                </div>
              </div>

              <div v-if="node.description" class="mb-3">
                <p class="text-body-2 text-grey-darken-1">{{ node.description }}</p>
              </div>

              <!-- 关联课程信息 -->
              <div v-if="node.course" class="mb-3">
                <v-chip variant="outlined" color="blue" size="small" rounded="lg">
                  <v-icon icon="mdi-book-outline" size="14" class="mr-1"></v-icon>
                  {{ node.course.name }}
                </v-chip>
              </div>

              <div class="d-flex align-center justify-space-between">
                <div class="d-flex align-center">
                  <v-avatar size="24" class="mr-2">
                    <v-img v-if="node.creator?.avatar" :src="node.creator.avatar" />
                    <v-icon v-else icon="mdi-account-circle" size="16" color="grey"></v-icon>
                  </v-avatar>
                  <span class="text-body-2 text-grey-darken-2">
                    {{ node.creator?.name || '匿名用户' }}
                  </span>
                </div>
                <div class="text-body-2 text-grey-darken-1">
                  创建时间：{{ new Date(node.createdAt).toLocaleDateString() }}
                </div>
              </div>
            </div>
          </div>
        </v-card>
      </div>
    </div>

    <!-- 加载更多指示器 -->
    <div v-if="loading" class="text-center py-4">
      <v-progress-circular
        indeterminate
        color="primary"
        size="24"
      ></v-progress-circular>
      <span class="ml-2 text-grey-darken-1">加载中...</span>
    </div>

    <!-- 没有更多数据提示 -->
    <div v-if="!hasMore && nodeList.length > 0" class="text-center py-4">
      <span class="text-grey-darken-1">没有更多数据了</span>
    </div>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="currentNode?.title || ''"
      :item-state="currentNode?.state"
      item-type="节点"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />
  </div>
</template>

<style scoped>
.node-management {
  max-width: 100%;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.status-actions-area {
  min-width: 180px;
  background-color: #fafafa;
  border-radius: 8px;
  padding: 18px;
}
</style>