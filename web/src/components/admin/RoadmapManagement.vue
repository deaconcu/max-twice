<template>
  <div class="roadmap-management">
    <!-- 页面头部 -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-blue-lighten-5 mr-3">
          <v-icon icon="mdi-map-marker-path" color="blue-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">路线图管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">管理和审核学习路线图</p>
        </div>
      </div>
      <v-chip variant="flat" color="blue-lighten-4" rounded="lg">
        <v-icon icon="mdi-map-marker-path" color="blue-darken-2" size="16" class="mr-2"></v-icon>
        <span class="text-blue-darken-2 text-caption">{{ roadmapList.length }}个路线图</span>
      </v-chip>
    </div>

    <!-- 筛选区域 -->
    <v-card flat class="pa-4 bg-grey-lighten-5 rounded-lg mb-6">
      <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
        <v-icon icon="mdi-filter-outline" size="16" class="mr-2"></v-icon>
        高级筛选
      </h4>
      <v-row dense>
        <v-col cols="12" sm="4">
          <v-text-field
            v-model.number="professionIdFilter"
            label="职业ID"
            type="number"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="4">
          <v-text-field
            v-model.number="creatorIdFilter"
            label="创建者ID"
            type="number"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="4">
          <div class="d-flex gap-2">
            <v-btn variant="flat" color="primary" rounded="lg" @click="onFilterChange">
              <v-icon icon="mdi-magnify" class="mr-1"></v-icon>
              筛选
            </v-btn>
            <v-btn variant="outlined" color="grey" rounded="lg" @click="onResetFilter">
              <v-icon icon="mdi-refresh" class="mr-1"></v-icon>
              重置
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </v-card>

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
          :color="
            state.value === ContentState.SUBMITTED
              ? 'orange-darken-1'
              : state.value === ContentState.PUBLISHED
                ? 'green-darken-1'
                : state.value === ContentState.REJECTED
                  ? 'red-darken-1'
                  : 'grey-darken-1'
          "
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
      <v-icon icon="mdi-map-marker-path" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无路线图</p>
    </div>

    <!-- 路线图列表 -->
    <div v-else>
      <v-card
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
        flat
        class="border rounded-lg pa-5 mb-4"
        hover
      >
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
                <v-icon :icon="getStateConfig(roadmap.state).icon" size="14" class="mr-1"></v-icon>
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
                  :loading="roadmap.approving"
                  @click="approveRoadmap(roadmap, 'APPROVE')"
                >
                  <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                  通过
                </v-btn>
                <v-btn
                  variant="flat"
                  color="red-lighten-4"
                  rounded="lg"
                  size="small"
                  @click="rejectRoadmap(roadmap)"
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

              <!-- 已通过状态：撤销通过、修改、屏蔽 -->
              <template v-if="roadmap.state === ContentState.PUBLISHED">
                <v-btn
                  variant="flat"
                  color="orange-lighten-4"
                  rounded="lg"
                  size="small"
                  @click="rejectRoadmap(roadmap)"
                >
                  <v-icon icon="mdi-undo" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                  撤销通过
                </v-btn>
                <v-btn
                  variant="flat"
                  color="blue-lighten-4"
                  rounded="lg"
                  size="small"
                  @click="showEditModal(roadmap)"
                >
                  <v-icon icon="mdi-pencil" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                  修改
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

              <!-- 已拒绝状态：恢复、屏蔽 -->
              <template v-if="roadmap.state === ContentState.REJECTED">
                <v-btn
                  variant="flat"
                  color="orange-lighten-4"
                  rounded="lg"
                  size="small"
                  :loading="roadmap.restoring"
                  @click="approveRoadmap(roadmap, 'APPROVE')"
                >
                  <v-icon icon="mdi-check" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                  重新通过
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
                  <v-icon
                    icon="mdi-lock-open"
                    color="blue-darken-2"
                    size="16"
                    class="mr-1"
                  ></v-icon>
                  取消屏蔽
                </v-btn>
                <v-btn
                  variant="flat"
                  color="orange-lighten-4"
                  rounded="lg"
                  size="small"
                  @click="downgradeToRejected(roadmap)"
                >
                  <v-icon
                    icon="mdi-arrow-down"
                    color="orange-darken-2"
                    size="16"
                    class="mr-1"
                  ></v-icon>
                  降级为拒绝
                </v-btn>
              </template>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-3">
              <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                <v-icon icon="mdi-map-marker-path" color="grey-darken-1" size="18"></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-2">
                  路线图 ID: {{ roadmap.id }}
                </div>
                <div class="text-caption text-grey-darken-1">
                  {{ roadmap.createdAt || '未知时间' }}
                </div>
              </div>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <!-- 职业信息 -->
              <div v-if="roadmap.profession" class="mb-3">
                <v-chip variant="tonal" color="purple-lighten-1" size="small" rounded="lg">
                  <v-icon icon="mdi-briefcase" size="14" class="mr-1"></v-icon>
                  {{ roadmap.profession.name }}
                </v-chip>
              </div>

              <!-- 路线图描述 -->
              <div class="mb-3">
                <div class="text-caption text-grey-darken-1 mb-1">路线图描述：</div>
                <div class="text-body-2 text-grey-darken-3">
                  {{ roadmap.description || '暂无描述' }}
                </div>
              </div>

              <!-- 创建者信息 -->
              <div class="d-flex align-center text-caption text-grey-darken-1">
                <v-icon icon="mdi-account-outline" size="14" class="mr-1"></v-icon>
                创建者: {{ roadmap.creator?.name || '未知' }}
                <span class="mx-3">|</span>
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                更新时间: {{ roadmap.updatedAt || '未知' }}
              </div>
            </div>
          </div>
        </div>
      </v-card>

      <!-- 加载更多状态 -->
      <div v-if="loading && roadmapList.length > 0" class="text-center py-4">
        <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
        <p class="mt-2 text-body-2 text-grey-darken-1">加载更多中...</p>
      </div>

      <!-- 没有更多数据提示 -->
      <div v-else-if="!hasMoreData && roadmapList.length > 0" class="text-center py-4">
        <p class="text-body-2 text-grey-darken-1">已加载全部数据</p>
      </div>
    </div>

    <!-- 编辑描述对话框 -->
    <v-dialog v-model="showEditDialog" max-width="700px" persistent>
      <v-card rounded="lg">
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
            color="blue-lighten-4"
            rounded="lg"
            :loading="updating"
            @click="updateRoadmapDescription"
          >
            <v-icon icon="mdi-content-save" color="blue-darken-2" class="mr-2"></v-icon>
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
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { roadmapApi, adminApi } from '@/api'
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
    // 使用统一接口，state 从当前 tab 获取
    const currentState = getCurrentState()
    return adminApi.getContentsByState('roadmap', currentState, params.lastId)
  },
  getNextParams: (lastItem, currentParams) => ({
    ...currentParams,
    lastId: lastItem.id,
  }),
  initialParams: {},
  immediate: true, // 自动初始加载
})

// 状态改变
const onStateChange = (): void => {
  resetList()
  loadMore() // 重新加载数据
}

// 筛选条件变化
const onFilterChange = (): void => {
  resetList()
  loadMore() // 重新加载数据
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
    roadmapApi.approveRoadmap(payload.id, payload.action),
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
    roadmapApi.approveRoadmap(payload.id, payload.action, payload.reason),
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
.roadmap-management {
  padding: 0;
}

.status-actions-area {
  min-width: 130px;
}

:deep(.v-card[hover]:hover) {
  transform: translateY(-2px);
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}
</style>
