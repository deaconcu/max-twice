<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">路线图管理</h2>

    <!-- 筛选与状态 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-filter-variant" size="18" class="mr-2"></v-icon>
        筛选与状态
      </v-card-title>
      <v-card-text>
        <!-- 筛选条件 -->
        <div class="d-flex align-center ga-3 mb-4 mt-2">
          <v-text-field
            v-model.number="professionIdFilter"
            label="职业ID"
            type="number"
            variant="outlined"
            density="compact"
            hide-details
            clearable
            style="max-width: 180px"
          ></v-text-field>
          <v-text-field
            v-model.number="creatorIdFilter"
            label="创建者ID"
            type="number"
            variant="outlined"
            density="compact"
            hide-details
            clearable
            style="max-width: 180px"
          ></v-text-field>
          <v-btn variant="tonal" size="default" @click="onFilterChange">
            <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
            筛选
          </v-btn>
          <v-btn variant="text" size="default" @click="onResetFilter">
            重置
          </v-btn>
        </div>

        <!-- 状态标签 -->
        <v-tabs
          v-model="selectedStateIndex"
          color="primary"
          show-arrows
          @update:model-value="onStateChange"
        >
          <v-tab
            v-for="(state, index) in stateOptions"
            :key="state.value"
            :value="index"
            class="text-none"
          >
            <v-icon :icon="state.icon" size="16" class="mr-2"></v-icon>
            {{ state.text }}
          </v-tab>
        </v-tabs>
      </v-card-text>
    </v-card>

    <!-- 路线图列表 -->
    <v-card flat class="border">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-map-marker-path" size="18" class="mr-2"></v-icon>
        路线图列表
      </v-card-title>
      <v-card-text>
        <!-- 空状态 -->
        <div v-if="!loading && roadmapList.length === 0" class="text-center py-12">
          <v-icon icon="mdi-map-marker-path" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">暂无路线图</p>
        </div>

        <!-- 列表 -->
        <div v-if="roadmapList.length > 0">
          <div
            v-for="roadmap in roadmapList"
            :key="roadmap.id"
            v-intersect="{
              handler: (isIntersecting) => {
                if (
                  isIntersecting &&
                  roadmap === roadmapList[roadmapList.length - 1] &&
                  hasMoreData &&
                  !loading
                ) {
                  loadMore()
                }
              },
            }"
            class="list-item mb-3"
          >
            <div class="d-flex align-start">
              <!-- 操作区 -->
              <div class="action-area mr-4">
                <v-chip variant="flat" :color="getStateConfig(roadmap.state).color" size="small" class="mb-4 d-flex justify-center">
                  {{ getStateConfig(roadmap.state).text }}
                </v-chip>

                <!-- 待审核 -->
                <div v-if="roadmap.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block :loading="roadmap.approving" @click="approveRoadmap(roadmap, 'APPROVE')">
                    通过
                  </v-btn>
                  <v-btn variant="tonal" color="error" size="small" block @click="rejectRoadmap(roadmap)">
                    拒绝
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(roadmap)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已通过 -->
                <div v-if="roadmap.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="warning" size="small" block @click="rejectRoadmap(roadmap)">
                    撤销通过
                  </v-btn>
                  <v-btn variant="tonal" color="info" size="small" block @click="showEditModal(roadmap)">
                    修改
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(roadmap)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已拒绝 -->
                <div v-if="roadmap.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block :loading="roadmap.restoring" @click="approveRoadmap(roadmap, 'APPROVE')">
                    重新通过
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(roadmap)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已屏蔽 -->
                <div v-if="roadmap.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="info" size="small" block @click="unbanRoadmap(roadmap)">
                    取消屏蔽
                  </v-btn>
                  <v-btn variant="tonal" color="warning" size="small" block @click="downgradeToRejected(roadmap)">
                    降级为拒绝
                  </v-btn>
                </div>
              </div>

              <!-- 内容区 -->
              <div class="flex-grow-1">
                <!-- 标题行 -->
                <div class="d-flex align-center justify-space-between mb-2">
                  <div class="text-body-1 font-weight-medium text-grey-darken-3">
                    路线图 ID: {{ roadmap.id }}
                  </div>
                  <div class="text-caption text-grey-darken-1">
                    {{ roadmap.createdAt || '未知时间' }}
                  </div>
                </div>

                <!-- 元信息 -->
                <div class="d-flex align-center mb-2 text-caption text-grey-darken-1">
                  <v-icon icon="mdi-account-outline" size="14" class="mr-1"></v-icon>
                  <span>{{ roadmap.creator?.name || '未知' }}</span>
                  <span class="ml-2">更新: {{ roadmap.updatedAt || '未知' }}</span>
                </div>

                <!-- 职业信息 -->
                <div v-if="roadmap.profession" class="mb-2">
                  <v-chip variant="tonal" color="purple" size="small">
                    <v-icon icon="mdi-briefcase" size="14" class="mr-1"></v-icon>
                    {{ roadmap.profession.name }}
                  </v-chip>
                </div>

                <!-- 描述 -->
                <div v-if="roadmap.description" class="text-body-2 text-grey-darken-1">
                  {{ roadmap.description }}
                </div>
                <div v-else class="text-body-2 text-grey">暂无描述</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载指示器 -->
        <div v-if="loading" class="text-center py-4">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 没有更多 -->
        <div v-if="!hasMoreData && roadmapList.length > 0" class="text-center py-4 text-caption text-grey">
          没有更多了
        </div>
      </v-card-text>
    </v-card>

    <!-- 编辑描述对话框 -->
    <v-dialog v-model="showEditDialog" max-width="700px" persistent>
      <v-card rounded="lg" variant="flat">
        <v-card-title class="text-h6 font-weight-bold pa-6 pb-4">
          <v-icon icon="mdi-pencil-outline" color="blue-darken-2" class="mr-3"></v-icon>
          编辑路线图描述
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <v-form ref="editForm" v-model="editFormValid">
            <v-textarea
              v-model="editDescription"
              label="路线图描述"
              :rules="roadmapContentRules"
              :counter="roadmapContentMaxLength"
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              rows="6"
              clearable
              placeholder="请输入路线图描述..."
            ></v-textarea>
          </v-form>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn variant="outlined" color="grey" rounded="lg" @click="closeEditDialog">
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="primary"
            rounded="lg"
            :loading="updating"
            @click="updateRoadmapDescription"
          >
            <v-icon icon="mdi-content-save" class="mr-2"></v-icon>
            保存修改
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="`路线图 ID: ${currentRoadmap?.id || ''}`"
      :item-state="currentRoadmap?.state"
      item-type="路线图"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { adminApi } from '@/api'
import { ContentState } from '@/enums'
import type { Roadmap } from '@/types/roadmap.d'
import type { StateOption } from '@/types/common.d'
import RejectBanDialog from './RejectBanDialog.vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'

const professionIdFilter = ref<number | null>(null)
const creatorIdFilter = ref<number | null>(null)
const selectedStateIndex = ref<number>(0)

// 验证规则
const roadmapContentRules = useValidationRules('roadmap-content')
const roadmapContentMaxLength = useMaxLength('roadmap-content')

const showEditDialog = ref<boolean>(false)
const editFormValid = ref<boolean>(false)
const currentRoadmap = ref<Roadmap | null>(null)
const editDescription = ref<string>('')
const editForm = ref(null)

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const dialogType = ref<'reject' | 'ban'>('reject')

// 状态选项
const stateOptions: StateOption[] = [
  {
    value: ContentState.SUBMITTED,
    text: '待审核',
    color: 'orange-lighten-4',
    icon: 'mdi-clock-outline',
  },
  {
    value: ContentState.PUBLISHED,
    text: '已通过',
    color: 'green-lighten-4',
    icon: 'mdi-check-circle',
  },
  {
    value: ContentState.REJECTED,
    text: '已拒绝',
    color: 'red-lighten-4',
    icon: 'mdi-close-circle',
  },
  {
    value: ContentState.BANNED,
    text: '已封禁',
    color: 'grey-lighten-2',
    icon: 'mdi-cancel',
  },
]

// 获取当前选中的状态
const getCurrentState = (): number =>
  stateOptions[selectedStateIndex.value]?.value || ContentState.SUBMITTED

// 根据状态获取配置
const getStateConfig = (state?: number): StateOption => {
  return stateOptions.find((option) => option.value === state) || stateOptions[0]
}

// 使用 useInfiniteScroll 进行列表加载
const {
  items: roadmapList,
  loading,
  hasMore: hasMoreData,
  loadMore,
  reset: resetList,
} = useInfiniteScroll<Roadmap>({
  fetchFn: (params) => {
    const currentState = getCurrentState()
    return adminApi.getContentsByState('roadmap', currentState, params.lastId)
  },
  getNextParams: (lastItem, currentParams) => ({
    ...currentParams,
    lastId: lastItem.id,
  }),
  initialParams: {},
  immediate: true,
})

// 状态改变
const onStateChange = (): void => {
  resetList()
  loadMore()
}

// 筛选条件变化
const onFilterChange = (): void => {
  resetList()
  loadMore()
}

// 重置筛选
const onResetFilter = (): void => {
  professionIdFilter.value = null
  creatorIdFilter.value = null
  resetList()
}

// 显示编辑对话框
const showEditModal = (roadmap: Roadmap): void => {
  currentRoadmap.value = roadmap
  editDescription.value = roadmap.description || ''
  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = (): void => {
  showEditDialog.value = false
  currentRoadmap.value = null
  editDescription.value = ''
  editFormValid.value = false
}

// 使用 useMutation 更新路线图描述
const { execute: updateRoadmapDescription, loading: updating } = useMutation(
  () => adminApi.updateRoadmap(currentRoadmap.value!.id, editDescription.value || ''),
  {
    successMessage: '更新成功',
    onSuccess: (result) => {
      const index = roadmapList.value.findIndex((r) => r.id === currentRoadmap.value!.id)
      if (index !== -1) {
        roadmapList.value[index] = result
      }
      closeEditDialog()
    },
  }
)

// 使用 useMutation 批准路线图
const { execute: executeApprove } = useMutation(
  (payload: { id: number; action: string }) =>
    adminApi.operateContent('roadmap', payload.id, { action: payload.action }),
  {
    successMessage: '操作成功',
    onSuccess: (result, payload) => {
      const index = roadmapList.value.findIndex((r) => r.id === payload.id)
      if (index !== -1) {
        const currentState = getCurrentState()
        if (currentState !== result.state) {
          roadmapList.value.splice(index, 1)
        } else {
          roadmapList.value[index] = result
        }
      }
    },
  }
)

// 批准路线图
const approveRoadmap = async (roadmap: Roadmap, action: string): Promise<void> => {
  await executeApprove({ id: roadmap.id, action })
}

// 显示拒绝对话框
const showRejectDialog = (roadmap: Roadmap): void => {
  currentRoadmap.value = roadmap
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 显示屏蔽对话框
const showBanDialog = (roadmap: Roadmap): void => {
  currentRoadmap.value = roadmap
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽操作
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (payload: { id: number; action: string; reason: string }) =>
    adminApi.operateContent('roadmap', payload.id, { action: payload.action, reason: payload.reason }),
  {
    successMessage: '操作成功',
    onSuccess: (result) => {
      const index = roadmapList.value.findIndex((r) => r.id === currentRoadmap.value!.id)
      if (index !== -1) {
        const currentState = getCurrentState()
        if (currentState !== result.state) {
          roadmapList.value.splice(index, 1)
        } else {
          roadmapList.value[index] = result
        }
      }
      showReasonDialog.value = false
      currentRoadmap.value = null
    },
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string): Promise<void> => {
  if (!currentRoadmap.value) return

  const action = dialogType.value === 'reject' ? 'REJECT' : 'BAN'
  await executeRejectOrBan({
    id: currentRoadmap.value.id,
    action,
    reason,
  })
}

// 拒绝路线图（供按钮调用）
const rejectRoadmap = (roadmap: Roadmap): void => {
  showRejectDialog(roadmap)
}

// 取消屏蔽路线图
const unbanRoadmap = async (roadmap: Roadmap): Promise<void> => {
  await executeApprove({ id: roadmap.id, action: 'APPROVE' })
}

// 降级为拒绝
const downgradeToRejected = (roadmap: Roadmap): void => {
  showRejectDialog(roadmap)
}

// 屏蔽路线图（供按钮调用）
const showBanModal = (roadmap: Roadmap): void => {
  showBanDialog(roadmap)
}
</script>

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
</style>
