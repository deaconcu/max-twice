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

const selectedStateIndex = ref<number>(0)

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const currentRoadmap = ref<any | null>(null)
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

// 获取当前选中的状态
const getCurrentState = (): number => stateOptions[selectedStateIndex.value].value
const getCurrentStateText = (): string => stateOptions[selectedStateIndex.value].text

// 获取状态配置
const getStateConfig = (state: number) => {
  return stateOptions.find(option => option.value === state) || stateOptions[0]
}

// 使用 useInfiniteScroll 加载路线图列表
const {
  items: roadmapList,
  loading,
  hasMore,
  loadMore,
  reset: resetRoadmapList
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const currentState = getCurrentState()
    let stateKey = 'pending'

    switch (currentState) {
      case ContentState.SUBMITTED:
        stateKey = 'pending'
        break
      case ContentState.PUBLISHED:
        stateKey = 'approved'
        break
      case ContentState.REJECTED:
        stateKey = 'rejected'
        break
      case ContentState.BANNED:
        stateKey = 'banned'
        break
    }

    const response = await adminApi.getRoadmapsByState(stateKey, params.lastId)
    return response.data
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id
  }),
  initialParams: {
    lastId: undefined
  }
})

// 状态变化处理
const onStateChange = (): void => {
  resetRoadmapList()
}

// 使用 useMutation 批准路线图
const { execute: executeApproveRoadmap } = useMutation(
  (roadmapId: number) => adminApi.approveRoadmap(roadmapId, { action: ApprovalAction.APPROVE }),
  {
    successMessage: '路线图已批准',
    onSuccess: (_, roadmapId) => {
      const index = roadmapList.value.findIndex((r: any) => r.id === roadmapId)
      if (index > -1) {
        roadmapList.value.splice(index, 1)
      }
    }
  }
)

const approveRoadmap = async (roadmap: any): Promise<void> => {
  await executeApproveRoadmap(roadmap.id)
}

// 显示拒绝对话框
const showRejectModal = (roadmap: any) => {
  currentRoadmap.value = roadmap
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 显示屏蔽对话框
const showBanModal = (roadmap: any) => {
  currentRoadmap.value = roadmap
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (data: { roadmapId: number; action: ApprovalAction; reason: string }) =>
    adminApi.approveRoadmap(data.roadmapId, { action: data.action, reason: data.reason }),
  {
    onSuccess: (_, data) => {
      const message = data.action === ApprovalAction.BAN ? '已屏蔽' : '已拒绝'
      showSnackbar?.(message, 'success')

      const index = roadmapList.value.findIndex((r: any) => r.id === data.roadmapId)
      if (index > -1) {
        roadmapList.value.splice(index, 1)
      }

      showReasonDialog.value = false
      currentRoadmap.value = null
    }
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string) => {
  if (!currentRoadmap.value) return

  const action = dialogType.value === 'ban' ? ApprovalAction.BAN : ApprovalAction.REJECT
  await executeRejectOrBan({
    roadmapId: currentRoadmap.value.id,
    action,
    reason
  })
}

// 使用 useMutation 取消屏蔽路线图
const { execute: executeUnbanRoadmap } = useMutation(
  (roadmapId: number) => adminApi.approveRoadmap(roadmapId, { action: ApprovalAction.APPROVE }),
  {
    successMessage: '已取消屏蔽',
    onSuccess: (_, roadmapId) => {
      const index = roadmapList.value.findIndex((r: any) => r.id === roadmapId)
      if (index > -1) {
        roadmapList.value.splice(index, 1)
      }
    }
  }
)

const unbanRoadmap = async (roadmap: any): Promise<void> => {
  await executeUnbanRoadmap(roadmap.id)
}

// 恢复路线图（从拒绝状态恢复）
const restoreRoadmap = async (roadmap: any): Promise<void> => {
  await executeApproveRoadmap(roadmap.id)
}
</script>

<template>
  <div class="roadmap-management">
    <!-- 页面头部 -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-green-lighten-5 mr-3">
          <v-icon icon="mdi-map-marker-path" color="green-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">路线图管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">审核和管理用户提交的学习路线图</p>
        </div>
      </div>
      <v-chip variant="flat" color="green-lighten-4" rounded="lg">
        <v-icon icon="mdi-map" color="green-darken-2" size="16" class="mr-2"></v-icon>
        <span class="text-green-darken-2 text-caption">{{ roadmapList.length }}个路线图</span>
      </v-chip>
    </div>

    <!-- 状态标签 -->
    <v-tabs
      v-model="selectedStateIndex"
      color="primary"
      class="mb-6"
      show-arrows
      @update:model-value="onStateChange"
    >
      <v-tab
        v-for="(state, index) in stateOptions"
        :key="state.value"
        :value="index"
        class="text-none"
      >
        <v-icon
          :icon="state.icon"
          :color="state.value === ContentState.SUBMITTED ? 'orange-darken-1' : state.value === ContentState.PUBLISHED ? 'green-darken-1' : state.value === ContentState.REJECTED ? 'red-darken-1' : 'grey-darken-1'"
          size="18"
          class="mr-2"
        ></v-icon>
        {{ state.text }}
      </v-tab>
    </v-tabs>

    <!-- 加载状态 -->
    <div v-if="loading && roadmapList.length === 0" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="mt-3 text-grey-darken-1">加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="roadmapList.length === 0" class="text-center py-12">
      <v-icon icon="mdi-map-marker-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无{{ getCurrentStateText() }}的路线图申请</p>
    </div>

    <!-- 路线图申请列表 -->
    <div v-else>
      <div
        v-for="roadmap in roadmapList"
        :key="roadmap.id"
        class="mb-4"
        v-intersect="{
          handler: (isIntersecting: boolean) => {
            if (isIntersecting && roadmap === roadmapList[roadmapList.length - 1] && hasMore && !loading) {
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
                  :color="getStateConfig(roadmap.state).color"
                  variant="flat"
                  rounded="lg"
                  size="small"
                >
                  <v-icon
                    :icon="getStateConfig(roadmap.state).icon"
                    size="14"
                    class="mr-1"
                  ></v-icon>
                  {{ getStateConfig(roadmap.state).text }}
                </v-chip>
              </div>

              <!-- 审核操作按钮 -->
              <div class="d-flex flex-column ga-2">
                <!-- 待审核状态：通过、拒绝、屏蔽 -->
                <template v-if="roadmap.state === ContentState.SUBMITTED">
                  <v-btn
                    variant="flat"
                    color="green-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="approveRoadmap(roadmap)"
                  >
                    <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                    批准
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="red-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="showRejectModal(roadmap)"
                  >
                    <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                    拒绝
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="grey-lighten-2"
                    rounded="lg"
                    size="small"
                    @click="showBanModal(roadmap)"
                  >
                    <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                    屏蔽
                  </v-btn>
                </template>

                <!-- 已发布状态：撤销通过、屏蔽 -->
                <template v-if="roadmap.state === ContentState.PUBLISHED">
                  <v-btn
                    variant="flat"
                    color="orange-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="showRejectModal(roadmap)"
                  >
                    <v-icon icon="mdi-undo" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                    撤销通过
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="grey-lighten-2"
                    rounded="lg"
                    size="small"
                    @click="showBanModal(roadmap)"
                  >
                    <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                    屏蔽
                  </v-btn>
                </template>

                <!-- 已拒绝状态：通过、屏蔽 -->
                <template v-if="roadmap.state === ContentState.REJECTED">
                  <v-btn
                    variant="flat"
                    color="green-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="restoreRoadmap(roadmap)"
                  >
                    <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                    通过
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="grey-lighten-2"
                    rounded="lg"
                    size="small"
                    @click="showBanModal(roadmap)"
                  >
                    <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                    屏蔽
                  </v-btn>
                </template>

                <!-- 已屏蔽状态：取消屏蔽、降级为拒绝 -->
                <template v-if="roadmap.state === ContentState.BANNED">
                  <v-btn
                    variant="flat"
                    color="blue-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="unbanRoadmap(roadmap)"
                  >
                    <v-icon icon="mdi-lock-open" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                    取消屏蔽
                  </v-btn>
                  <v-btn
                    variant="flat"
                    color="orange-lighten-4"
                    rounded="lg"
                    size="small"
                    @click="showRejectModal(roadmap)"
                  >
                    <v-icon icon="mdi-arrow-down" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                    降级为拒绝
                  </v-btn>
                </template>
              </div>
            </div>

            <!-- 路线图内容区域 -->
            <div class="flex-grow-1">
              <div class="d-flex align-center justify-space-between mb-3">
                <h4 class="text-h6 font-weight-bold text-grey-darken-3">{{ roadmap.title }}</h4>
                <div class="text-caption text-grey-darken-1">
                  ID: {{ roadmap.id }}
                </div>
              </div>

              <div v-if="roadmap.description" class="mb-3">
                <p class="text-body-2 text-grey-darken-1">{{ roadmap.description }}</p>
              </div>

              <!-- 关联职业信息 -->
              <div v-if="roadmap.profession" class="mb-3">
                <v-chip variant="outlined" color="blue" size="small" rounded="lg">
                  <v-icon icon="mdi-briefcase-outline" size="14" class="mr-1"></v-icon>
                  {{ roadmap.profession.name }}
                </v-chip>
              </div>

              <div class="d-flex align-center justify-space-between">
                <div class="d-flex align-center">
                  <v-avatar size="24" class="mr-2">
                    <v-img v-if="roadmap.creator?.avatar" :src="roadmap.creator.avatar" />
                    <v-icon v-else icon="mdi-account-circle" size="16" color="grey"></v-icon>
                  </v-avatar>
                  <span class="text-body-2 text-grey-darken-2">
                    {{ roadmap.creator?.name || '匿名用户' }}
                  </span>
                </div>
                <div class="text-body-2 text-grey-darken-1">
                  创建时间：{{ new Date(roadmap.createdAt).toLocaleDateString() }}
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
    <div v-if="!hasMore && roadmapList.length > 0" class="text-center py-4">
      <span class="text-grey-darken-1">没有更多数据了</span>
    </div>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="currentRoadmap?.title || ''"
      :item-state="currentRoadmap?.state"
      item-type="路线图"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />
  </div>
</template>

<style scoped>
.roadmap-management {
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